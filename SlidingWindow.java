import java.util.LinkedList;
import rn.*;

public class SlidingWindow
{
	private NetworkCard nwcard;

	// enthaelt alle relevanten Frames fuers Sendefenster (FIFO)
	private LinkedList<Frame> frameBuffer = new LinkedList<>();
	private short sequNr = 0; // sequence number

	public SlidingWindow(NetworkCard nwcard)
	{
		this.nwcard = nwcard;
	}

	// Sendemethode fuer Datenrahmen
	public void send(short sourceadr, short destadr, short sendefenster, TestData testData)
	{
		// TODO: sendefenster beachten, auf ACK warten
		try
		{
			byte[] payload = testData.getTestData(); // Testdaten beschaffen

			while (payload != null) // ganzen Testsatz senden
			{
				Frame senFrame = new Frame(sourceadr, destadr, sequNr, payload); // Frame instanziieren
				sequNr++;
				System.out.println("Sended frame " + senFrame.getSequNr());

				// System.out.println(senFrame);

				// for (int j = 0; j < senFrame.getPayloadLength(); j++)
				// System.out.print(String.format("%02x ", senFrame.getRawFrame()[j]));

				/*
				 * System.out .print("Send Frame " + myFrame.getSequNr() + " (" +
				 * myFrame.getRawFrame().length + " bytes: ");
				 * 
				 * for (int j = 0; j < Math.min(15, myFrame.getRawFrame().length); j++) // 10 //
				 * for (int j = 0; j < frame.length; j++) // print full frame {
				 * System.out.print(String.format("%02x ", myFrame.getPayload()[j])); }
				 * System.out.println("...)");
				 */

				nwcard.send(senFrame.getRawFrame());
				frameBuffer.add(senFrame); // add frame to buffer

				Thread.sleep(10); // 500); // etwas warten
				payload = testData.getTestData(); // Testdaten beschaffen
			}
			Frame lastFrame = new Frame(sourceadr, destadr, sequNr, true, false);
			nwcard.send(lastFrame.getRawFrame());
			System.out.println("Sended Term frame " + lastFrame.getSequNr());

			System.exit(0);
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
			Frame myFrame = new Frame(sourceadr, destadr, sequNr, true, false);
			nwcard.send(myFrame.getRawFrame());
		} catch (Exception e)
		{
			System.out.println(e);
		}
	}

	public void receive(byte[] frame)
	{
		// TODO: for each data frame -> send ACK

		if (frame.length >= 12)
		{
			Frame recFrame = new Frame(frame);
			// System.out.println(recFrame);

			if (recFrame.isChecksumCorrekt() == true)
			{
				if (recFrame.getAck() == false)
				{
					if (recFrame.getTerm() == false)
					{
						System.out.println("Received dataframe " + recFrame.getSequNr());
						send(recFrame.getSourceAdr(), recFrame.getDestAdr(), recFrame.getSequNr(), recFrame.getFlags());
						System.out.println("Send ACK " + recFrame.getSequNr());
					}
				} else if (recFrame.getAck() == true)
				{
					if (recFrame.getTerm() == false)
						System.out.println("Received ACK " + recFrame.getSequNr());
					else
						System.out.println("Received term ACK " + recFrame.getSequNr());
				}
			} else
			{
				System.out.println("Received frame " + recFrame.getSequNr() + " has incorrect checksum");
			}
		}
	}
}