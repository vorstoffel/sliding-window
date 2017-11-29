import java.io.FileOutputStream;
import java.util.LinkedList;
import rn.*;

public class SlidingWindow
{
	private NetworkCard nwcard;
	private LinkedList<Frame> frameBuffer = new LinkedList<>();
	// Enthaelt alle relevanten Frames fuers Sendefenster (FIFO)
	private short sequNr = 0; // sequence number
	private boolean firstRec = true; // Zum testen, ob erstes empfangenes Frame richtig ist (sequNr = 0)
	private short sourceAdr;
	private short destAdr;
	private short sendefenster;
	private TestData testData;
	private volatile boolean stop = false;
	private short lastACK; // Speichert SequNr vom letzten empfangenen ACK
	private boolean resend = false;

	public SlidingWindow(NetworkCard nwcard, short sourceAdr, short destAdr, short sendefenster, TestData testData)
	{
		this(nwcard, sourceAdr, destAdr, sendefenster);
		this.testData = testData;
	}

	public SlidingWindow(NetworkCard nwcard, short sourceAdr, short destAdr, short sendefenster)
	{
		this.nwcard = nwcard;
		this.sourceAdr = sourceAdr;
		this.destAdr = destAdr;
		this.sendefenster = sendefenster;

		SendThread senThread = new SendThread();
		senThread.start();
	}

	// Sendemethode fuer Datenrahmen
	public void send()
	{
		try
		{
			byte[] payload = testData.getTestData(); // Testdaten beschaffen

			while (!stop) // Ganzen Testsatz senden
			{
				if (resend == true)
				{
					sendAllInFrameBuffer();
					resend = false;
				}

				if (frameBuffer.size() < sendefenster)
				{
					Frame senFrame = new Frame(sourceAdr, destAdr, sequNr, payload); // Frame instanziieren
					sequNr++;
					System.out.println("Sended frame " + senFrame.getSequNr());

					nwcard.send(senFrame.getRawFrame());
					frameBuffer.add(senFrame); // Add frame to buffer

					Thread.sleep(1000); // 500);
					payload = testData.getTestData(); // Testdaten beschaffen

					if (payload == null)
					{
						Frame lastFrame = new Frame(sourceAdr, destAdr, sequNr, true, false);
						nwcard.send(lastFrame.getRawFrame());
						System.out.println("Sended term frame " + lastFrame.getSequNr());
					}
				}
				else
				{
					waitAndResendIfNeeded();
				}
			}
		} catch (Exception e)
		{
			System.out.println(e);
		}
	}

	// Hier geschieht die Fehlerbehandlung, bei Uebertragungsfehler
	public void waitAndResendIfNeeded()
	{
		synchronized (this)
		{
			try
			{
				this.wait(200L);
			} catch (InterruptedException ie)
			{
				// Hier kommt man hin, falls der Timeout stattfand (200ms)
				sendAllInFrameBuffer();
			}
		}
	}

	public void sendAllInFrameBuffer()
	{
		for (Frame frame : frameBuffer)
		{
			try
			{
				nwcard.send(frame.getRawFrame());
			} catch (Exception e)
			{
				System.out.println(e);
			}
		}
	}

	// Sendemethode fuer ACK-Rahmen
	public void send(short sourceadr, short destadr, short sequNr, short flags)
	{
		try
		{
			Frame myFrame = new Frame(sourceadr, destadr, sequNr, false, true);
			nwcard.send(myFrame.getRawFrame());
		} catch (Exception e)
		{
			System.out.println(e);
		}
	}

	public void receive(byte[] frame)
	{
		if (frame.length >= 12)
		{
			Frame recFrame = new Frame(frame);
			if (recFrame.isChecksumCorrekt() == true)
			{
				// Prueft ob source und destination adresse richtig sind
				if (recFrame.getSourceAdr() == this.sourceAdr && recFrame.getDestAdr() == this.destAdr)
				{
					// Wenn neues ACK altem ACK entspricht muss neu gesendet werden
					if (recFrame.getFlags() == 1 && recFrame.getSequNr() == lastACK)
					{
						resend = true;
					}
					else
					{
						// Check if first frame is correct
						if ((firstRec == true && recFrame.getSequNr() == 0) || firstRec == false)
						{
							firstRec = false;
							if (recFrame.getFlags() == 0) // Data, not terminating
							{
								System.out.println("Received dataframe " + recFrame.getSequNr());
								byteFileOutput(frame);
								send(recFrame.getSourceAdr(), recFrame.getDestAdr(), recFrame.getSequNr(),
										recFrame.getFlags());
								System.out.println("Sended ack " + recFrame.getSequNr());
							}
							else if (recFrame.getFlags() == 1) // Ack, not terminating
							{
								System.out.println("Received ack " + recFrame.getSequNr());
								// Jeweiliges Element, was das erste ist, aus frameBuffer loeschen
								if (frameBuffer.peekFirst().getSequNr() == recFrame.getSequNr())
									frameBuffer.removeFirst();
								else
									System.out.println("Error: wrong ACK received");

								lastACK = recFrame.getSequNr();

								synchronized (this)
								{
									this.notifyAll();
								}
							}
							else if (recFrame.getFlags() == 2) // Data, terminating
							{
								System.out.println("Received terminating dataframe " + recFrame.getSequNr());
							}
							else if (recFrame.getFlags() == 3) // Ack, terminating
							{
								System.out.println("Received terminating ack " + recFrame.getSequNr());
								// Jeweiliges Element, was das erste ist, aus frameBuffer loeschen
								if (frameBuffer.peekFirst().getSequNr() == recFrame.getSequNr())
									frameBuffer.removeFirst();
								else
									System.out.println("Error: wrong ACK received");

								stop = true;

								synchronized (this)
								{
									this.notifyAll();
								}
								// System.exit(0);
							}
						}
						else if (firstRec == true && recFrame.getSequNr() != 0)
						{
							System.out.println("First frame: wrong sequence number");
							send(recFrame.getSourceAdr(), recFrame.getDestAdr(), (short) -1, recFrame.getFlags());
							System.out.println("Sended ack " + recFrame.getSequNr());
						}
					}
				}
			}
			else if (recFrame.isChecksumCorrekt() == false)
			{
				System.out.println("Received frame " + recFrame.getSequNr() + " has incorrect checksum");
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

	// Erstellt data.out und speichert jedes frame darin
	public void byteFileOutput(byte[] frame)
	{
		FileOutputStream out;
		try
		{
			// Haengt Daten am Ende dran (Achtung bei schon existierendem data.out!)
			out = new FileOutputStream("data.out", true);
			out.write(frame);
			out.close();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}