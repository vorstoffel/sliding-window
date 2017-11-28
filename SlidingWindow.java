import java.io.FileOutputStream;
import java.util.LinkedList;
import rn.*;

public class SlidingWindow
{
	private NetworkCard nwcard;

	// enthaelt alle relevanten Frames fuers Sendefenster (FIFO)
	private LinkedList<Frame> frameBuffer = new LinkedList<>();
	private short sequNr = 0; // sequence number
	private boolean firstRec = true; // test if first frame (sequNr = 0) is correct
	private short sourceAdr;
	private short destAdr;
	private short sendefenster;
	private TestData testData;

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

		// TODO: HIER Thread anlegen und starten
		SendThread senThread = new SendThread();
	}

	// Sendemethode fuer Datenrahmen
	public void send()
	{
		// TODO: sendefenster beachten, auf ACK warten, Timeout, in frameBuffer rein und
		// raus tun
		try
		{
			byte[] payload = testData.getTestData(); // Testdaten beschaffen

			while (payload != null) // ganzen Testsatz senden
			{
				Frame senFrame = new Frame(sourceAdr, destAdr, sequNr, payload); // Frame instanziieren
				sequNr++;
				System.out.println("Sended frame " + senFrame.getSequNr());

				nwcard.send(senFrame.getRawFrame()); // HIER wird gesendet
				frameBuffer.add(senFrame); // add frame to buffer

				Thread.sleep(10); // 500);
				payload = testData.getTestData(); // Testdaten beschaffen
			}
			Frame lastFrame = new Frame(sourceAdr, destAdr, sequNr, true, false);
			nwcard.send(lastFrame.getRawFrame()); // HIER wird gesendet
			System.out.println("Sended term frame " + lastFrame.getSequNr());

		} catch (Exception e)
		{
			System.out.println(e);
		}
	}

	// Sendemethode fuer ACK-Rahmen
	public void send(short sourceadr, short destadr, short sequNr, short flags)
	{
		try
		{
			Frame myFrame = new Frame(sourceadr, destadr, sequNr, false, true);
			nwcard.send(myFrame.getRawFrame()); // HIER wird gesendet
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
				// prueft ob source und destination adresse richtig sind
				if (recFrame.getSourceAdr() == this.sourceAdr && recFrame.getDestAdr() == this.destAdr)
				{
					// check if first frame is correct
					if ((firstRec == true && recFrame.getSequNr() == 0) || firstRec == false)
					{
						firstRec = false;
						if (recFrame.getFlags() == 0) // data, not terminating
						{
							System.out.println("Received dataframe " + recFrame.getSequNr());

							FileOutputStream out;
							try
							{
								// haengt Daten am Ende dran (Achtung bei schon existierendem data.out!)
								out = new FileOutputStream("data.out", true);
								out.write(frame);
								out.close();
							} catch (Exception e)
							{
								e.printStackTrace();
							}

							send(recFrame.getSourceAdr(), recFrame.getDestAdr(), recFrame.getSequNr(),
									recFrame.getFlags());
							System.out.println("Sended ack " + recFrame.getSequNr());
						} else if (recFrame.getFlags() == 1) // ack, not terminating
						{
							System.out.println("Received ack " + recFrame.getSequNr());
						} else if (recFrame.getFlags() == 2) // data, terminating
						{
							System.out.println("Received terminating dataframe " + recFrame.getSequNr());

						} else if (recFrame.getFlags() == 3) // ack, terminating
						{
							System.out.println("Received terminating ack " + recFrame.getSequNr());
							System.exit(0);
						}
					} else if (firstRec == true && recFrame.getSequNr() != 0)
					{
						System.out.println("First frame incorrect");
						send(recFrame.getSourceAdr(), recFrame.getDestAdr(), (short) -1, recFrame.getFlags());
						System.out.println("Sended ack " + recFrame.getSequNr());
					}
				}
			} else if (recFrame.isChecksumCorrekt() == false)
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

		}
	}
}