import rn.*;

public class RNSender implements Receiver
{
	private SlidingWindow sw;

	// Auch der Sender muss empfangen koennen (minds. die Quittungen)
	public static void main(String[] args)
	{
		String sourceadr = args[0];
		String destadr = args[1];
		String sendefenster = args[2];
		int testdatenNr = 0;

		try
		{
			testdatenNr = Integer.parseInt(args[3]);
		} catch (NumberFormatException exc)
		{
			exc.printStackTrace();
		}

		new RNSender(sourceadr, destadr, sendefenster, testdatenNr);
	}

	public RNSender(String sourceadr, String destadr, String sendefenster, int testdatenNr)
	{
		try
		{
			NetworkCard nwcard = new NetworkCard(this); // Netzwerkkarte instanziieren
			TestData testData = TestData.createTestData(testdatenNr); // Testdaten instanziieren

			this.sw = new SlidingWindow(nwcard); // SlidingWindow instanziieren
			Frame myFrame = new Frame(sourceadr, destadr, testData); // Frame instanziieren

			sw.send(myFrame, testData);

		} catch (Exception e)
		{
			System.out.println(e);
		}
	}

	public void receive(byte[] frame)
	{
		sw.receive(frame);
		// Der Sender bekommt nie eine Antwort, wenn doch, das ausgeben
		System.out.print("Sender unexpectedly received frame (" + frame.length + " bytes)");
	}

}