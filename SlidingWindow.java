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

	public void send(short sourceadr, short destadr, short sendefenster, TestData testData)
	{
		try
		{
			byte[] payload = testData.getTestData(); // Testdaten beschaffen

			while (payload != null) // ganzen Testsatz senden
			{
				boolean term = false;
				Frame myFrame = new Frame(sourceadr, destadr, sequNr, payload, term); // Frame instanziieren
				sequNr++;

				System.out.println("Sended frame " + sequNr);

				/*
				 * System.out .print("Send Frame " + myFrame.getSequNr() + " (" +
				 * myFrame.getRawFrame().length + " bytes: ");
				 * 
				 * for (int j = 0; j < Math.min(15, myFrame.getRawFrame().length); j++) // 10 //
				 * for (int j = 0; j < frame.length; j++) // print full frame {
				 * System.out.print(String.format("%02x ", myFrame.getPayload()[j])); }
				 * System.out.println("...)");
				 */
				nwcard.send(myFrame.getRawFrame());
				frameBuffer.add(myFrame); // add frame to buffer

				Thread.sleep(500); // etwas warten
				payload = testData.getTestData(); // Testdaten beschaffen
			}
			System.exit(0);
		} catch (Exception e)
		{
			System.out.println(e);
		}
	}

	public void receive(byte[] frame)
	{
		// TODO: FIRST: Frame(byte[] frame); SECOND: for each frame -> send ACK
		Frame recFrame = new Frame(frame);

		// if (recFrame.getRawFrame().length > 12 && recFrame.isChecksumCorrekt() ==
		// true)
		System.out.println("Received frame " + recFrame.getSequNr());
		// else
		// System.out.println("Frame error");

		/*
		 * System.out.print("Received frame " + myFrame.getSequNr() + " (" +
		 * frame.length + " bytes: "); for (int i = 0; i < Math.min(10, frame.length);
		 * i++) System.out.print(String.format("%02x ", frame[i]));
		 * System.out.println("...)");
		 */
	}
}
