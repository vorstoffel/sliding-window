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

	public void send(String sourceadr, String destadr, short sendefenster, TestData testData)
	{
		try
		{
			byte[] payload = testData.getTestData(); // Testdaten beschaffen

			while (payload != null) // ganzen Testsatz senden
			{
				boolean term = false; // terminierungs frames senden?
				Frame myFrame = new Frame(sourceadr, destadr, sequNr, testData, term); // Frame instanziieren
				byte[] frame = myFrame.CreateFrame(payload);

				System.out.print("Send Frame (" + frame.length + " bytes: ");

				for (int j = 0; j < Math.min(15, frame.length); j++) // 10
				// for (int j = 0; j < frame.length; j++) // print full frame
				{
					System.out.print(String.format("%02x ", frame[j]));
				}
				System.out.println("...)");

				nwcard.send(frame);
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
		// Empfangene Frames ausgeben
		System.out.print("Received frame (" + frame.length + " bytes: ");
		for (int i = 0; i < Math.min(10, frame.length); i++)
			System.out.print(String.format("%02x ", frame[i]));
		System.out.println("...)");
	}
}
