import rn.*;

public class RNSender implements Receiver
{
	private SlidingWindow sw;

	// Auch der Sender muss empfangen koennen (minds. die Quittungen)
	public static void main(String[] args)
	{
		String sourceadr = args[0];
		String destadr = args[1];
		short sendefenster = 0;
		short testdatenNr = 0;

		try
		{
			sendefenster = Short.parseShort(args[2]);
			testdatenNr = Short.parseShort(args[3]);
		} catch (NumberFormatException exc)
		{
			exc.printStackTrace();
		}

		new RNSender(sourceadr, destadr, sendefenster, testdatenNr);
	}

	public RNSender(String sourceadr, String destadr, short sendefenster, short testdatenNr)
	{
		try
		{
			NetworkCard nwcard = new NetworkCard(this); // Netzwerkkarte instanziieren
			TestData testData = TestData.createTestData(testdatenNr); // Testdaten instanziieren
			this.sw = new SlidingWindow(nwcard); // SlidingWindow instanziieren
			sw.send(sourceadr, destadr, sendefenster, testData);

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