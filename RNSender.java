import rn.*;

public class RNSender implements Receiver
{
	private SlidingWindow sw;

	// Auch der Sender muss empfangen koennen (minds. die Quittungen)
	public static void main(String[] args)
	{
		short sourceadr = 0;
		short destadr = 0;
		short sendefenster = 0;
		short testdatenNr = 0;

		try
		{
			sourceadr = Short.parseShort(args[0]);
			destadr = Short.parseShort(args[1]);
			sendefenster = Short.parseShort(args[2]);
			testdatenNr = Short.parseShort(args[3]);
		} catch (NumberFormatException exc)
		{
			exc.printStackTrace();
		}

		new RNSender(sourceadr, destadr, sendefenster, testdatenNr);
	}

	public RNSender(short sourceadr, short destadr, short sendefenster, short testdatenNr)
	{
		try
		{
			NetworkCard nwcard = new NetworkCard(this); // Netzwerkkarte instanziieren
			TestData testData = TestData.createTestData(testdatenNr); // Testdaten instanziieren
			this.sw = new SlidingWindow(nwcard, sourceadr, destadr, sendefenster, testData);
		} catch (Exception e)
		{
			System.out.println(e);
		}
	}

	public void receive(byte[] frame)
	{
		// RNSender muss ACKs empfangen koennen
		sw.receive(frame);
	}

}