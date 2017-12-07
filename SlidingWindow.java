import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import rn.*;

public class SlidingWindow
{
	private NetworkCard nwcard;
	private LinkedBlockingQueue<Frame> sendBuffer = new LinkedBlockingQueue<>();
	private volatile ArrayList<Frame> receiveBuffer = new ArrayList<>(100);
	// Enthaelt alle relevanten Frames fuers Sendefenster (FIFO)
	private short sequNr = 0; // sequence number
	private volatile boolean firstRec = true; // Zum testen, ob erstes empfangenes Frame richtig ist (sequNr = 0)
	private short sourceAdr;
	private short destAdr;
	private short sendefenster;
	private TestData testData;
	private volatile boolean stop = false;
	private short lastReceivedACK = Short.MIN_VALUE; // relevant im Sender, Speichert SequNr vom letzten empfangenen ACK
	private short lastSentACK = -1; // relevant im Receiver
	private boolean resend = false;
	private StopThread stopThread;

	public SlidingWindow(NetworkCard nwcard, short sourceAdr, short destAdr, short sendefenster, TestData testData)
	{
		this(nwcard, sourceAdr, destAdr, sendefenster);
		this.testData = testData;
		SendThread sendThread = new SendThread();
		sendThread.start();
	}

	public SlidingWindow(NetworkCard nwcard, short sourceAdr, short destAdr, short sendefenster)
	{
		this.nwcard = nwcard;
		this.sourceAdr = sourceAdr;
		this.destAdr = destAdr;
		this.sendefenster = sendefenster;
	}

	// Sendemethode fuer Datenrahmen
	public void send()
	{
		boolean sendTerm = false;
		long timestamp = 0;

		try
		{
			byte[] payload = testData.getTestData(); // Testdaten beschaffen

			while (!stop) // Ganzen Testsatz senden
			{
				if (resend == true)
				{
					System.out.println("Resend all frames in frameBuffer:");

					sendAllInFrameBuffer();
					resend = false;
				}

				if (sendBuffer.size() < sendefenster)
				{
					if (sendTerm)
					{
						if (System.currentTimeMillis() - timestamp >= 200)
						{
							sendTerm = false;
						}
						else
						{
							synchronized (this)
							{
								this.wait(50L);
							}
						}
					}
					else if (payload == null && !sendTerm)
					{
						Frame lastFrame = new Frame(sourceAdr, destAdr, sequNr, (short) 2);
						nwcard.send(lastFrame.getRawFrame());
						sendBuffer.add(lastFrame); // Add frame to buffer
						System.out.println("Send term frame " + lastFrame.getSequNr());
						timestamp = System.currentTimeMillis();
						sendTerm = true;
					}
					else
					{
						Frame senFrame = new Frame(sourceAdr, destAdr, sequNr, payload); // Frame instanziieren
						sequNr++;
						System.out.println("Send frame " + senFrame.getSequNr());
						nwcard.send(senFrame.getRawFrame());
						sendBuffer.add(senFrame); // Add frame to buffer

						payload = testData.getTestData(); // Testdaten beschaffen
					}
				}
				else
				{
					waitAndResendIfNeeded();
				}
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	// Hier geschieht die Fehlerbehandlung, bei Uebertragungsfehler
	public void waitAndResendIfNeeded()
	{
		synchronized (this)
		{
			try
			{
				long timestamp = System.currentTimeMillis();
				this.wait(200L);
				// Hier bei Timeout (200ms)
				if (System.currentTimeMillis() - timestamp >= 200)
				{
					System.out.println("Timeout, send again all frames in frameBuffer:");
					sendAllInFrameBuffer();
				}
			} catch (InterruptedException ie)
			{
				ie.printStackTrace();
			}
		}
	}

	public void sendAllInFrameBuffer()
	{
		for (Frame frame : sendBuffer)
		{
			try
			{
				nwcard.send(frame.getRawFrame());
				System.out.println("Send again frame " + frame.getSequNr());
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	// Sendemethode fuer ACK-Rahmen
	public void send(short sourceadr, short destadr, short sequNr, short flags)
	{
		try
		{
			Frame myFrame = new Frame(sourceadr, destadr, sequNr, flags);
			nwcard.send(myFrame.getRawFrame());
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void receive(byte[] frame)
	{
		if (stopThread != null)
		{
			stopThread.stop();
			stopThread = null;
		}
		if (frame.length >= 12)
		{
			Frame recFrame = new Frame(frame);
			if (recFrame.isChecksumCorrekt() == true)
			{
				// Prueft ob source und destination adresse richtig sind
				if (recFrame.getSourceAdr() == this.destAdr && recFrame.getDestAdr() == this.sourceAdr)
				{
					// Check if first frame is correct
					if ((firstRec == true && recFrame.getSequNr() == 0) || firstRec == false)
					{
						firstRec = false;
						if (recFrame.getFlags() == 0) // Data, not terminating
						{
							System.out.print("Received dataframe " + recFrame.getSequNr() + ", ");

							if ((lastSentACK + 1) == recFrame.getSequNr())
							{
								lastSentACK = recFrame.getSequNr();

								for (Frame f : receiveBuffer)
								{
									if (lastSentACK + 1 == f.getSequNr())
									{
										lastSentACK = f.getSequNr();
									}
									else if (lastSentACK < f.getSequNr())
									{
										break;
									}
								}
							}

							// Hier wird ACK gesendet
							send(recFrame.getDestAdr(), recFrame.getSourceAdr(), lastSentACK, (short) 1);
							System.out.print("send ACK " + lastSentACK);
							System.out.println(" ");

							// jedes Element durchgehen schauen ob es nächstgrößeres ist
							boolean insertIt = false;
							for (int i = 0; i < receiveBuffer.size(); i++)
							{
								if (receiveBuffer.get(i).getSequNr() > recFrame.getSequNr())
								{
									receiveBuffer.add(i, recFrame);
									insertIt = true;
									break;
								}
								else if (receiveBuffer.get(i).getSequNr() == recFrame.getSequNr())
								{
									insertIt = true;
									break;
								}
							}
							if (!insertIt)
							{
								receiveBuffer.add(recFrame);
							}
							if (receiveBuffer.size() > sendefenster)
							{
								Frame f = receiveBuffer.remove(0);
								byteFileOutput(f.getPayload(), "data.out");

								// Testausgabe:
								bytesWritten += f.getPayloadLength();
							}
						}
						else if (recFrame.getFlags() == 1) // Ack, not terminating
						{
							System.out.println("Received ACK " + recFrame.getSequNr());
							// Jeweiliges Element, was das erste ist, aus frameBuffer loeschen
							if (sendBuffer.peek() != null)
							{
								// alles kleiner des empfangenen ACKs rausnehmen
								if (lastReceivedACK > recFrame.getSequNr())
								{
									System.out.println("Error: wrong ACK received");
								}
								else
								{
									while (sendBuffer.peek() != null
											&& sendBuffer.peek().getSequNr() <= recFrame.getSequNr())
									{
										sendBuffer.poll();
									}
								}
							}
							lastReceivedACK = recFrame.getSequNr();

							synchronized (this)
							{
								this.notifyAll();
							}
						}
						else if (recFrame.getFlags() == 2) // Data, terminating
						{
							System.out.print("Received terminating dataframe " + recFrame.getSequNr());

							if ((lastSentACK + 1) == recFrame.getSequNr())
								lastSentACK = recFrame.getSequNr();

							if (lastSentACK == recFrame.getSequNr())
							{
								send(recFrame.getDestAdr(), recFrame.getSourceAdr(), lastSentACK, (short) 3);
								System.out.print(", send terminating ACK " + lastSentACK);
								// start stopThread only when term ack is sent
								StopThread stopThread = new StopThread();
								stopThread.start();
							}
							else
							{
								send(recFrame.getDestAdr(), recFrame.getSourceAdr(), lastSentACK, (short) 1);
								System.out.print(", send ACK " + lastSentACK);
							}
							System.out.println(" ");

						}
						else if (recFrame.getFlags() == 3) // Ack, terminating
						{
							System.out.println("Received terminating ack " + recFrame.getSequNr());
							// Jeweiliges Element, was das erste ist, aus frameBuffer loeschen
							if (sendBuffer.peek() != null)
							{
								if (sendBuffer.peek().getSequNr() == recFrame.getSequNr())
								{
									sendBuffer.poll();
								}
								else
									System.out.println("Error: wrong ACK received");
							}
							stop = true;

							synchronized (this)
							{
								this.notifyAll();
							}
							System.exit(0);
						}
					}
					else if (firstRec == true && recFrame.getSequNr() != 0)
					{
						send(recFrame.getDestAdr(), recFrame.getSourceAdr(), (short) -1, (short) 1);
						System.out.println("First frame: wrong sequence number, sent ACK -1");

					}
				}
			}
			else if (recFrame.isChecksumCorrekt() == false)
			{
				System.out.println("Received frame with incorrect checksum");
			}

		}
	}

	private class SendThread extends Thread
	{

		@Override
		public void run()
		{
			send();
		}
	}

	// Testausgabe:
	int bytesWritten = 0;

	private class StopThread extends Thread
	{
		@Override
		public void run()
		{
			synchronized (this)
			{
				try
				{
					this.wait(1000L); // 1 Sekunde warten vor Beenden
					for (Frame f : receiveBuffer)
					{
						bytesWritten += f.getPayloadLength();
						byteFileOutput(f.getPayload(), "data.out");
					}
					// Testoutput:
					System.out.println("Bytes Written: " + bytesWritten);
					System.exit(0);

				} catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	// Erstellt data.out und speichert jedes frame darin
	public void byteFileOutput(byte[] payload, String filename)
	{
		FileOutputStream out;
		try
		{
			// Fuegt Daten am Ende der Datei an (Achtung: bei schon existierendem
			// data.out fuegt er es auch an)
			out = new FileOutputStream(filename, true);
			out.write(payload);
			out.flush();
			out.close();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}