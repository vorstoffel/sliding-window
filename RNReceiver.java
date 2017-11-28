import rn.*;

public class RNReceiver implements Receiver
{
	private SlidingWindow sw;

	public static void main(String[] args)
	{
		short sourceadr = 0;
		short destadr = 0;
		short empfangsfenster = 0;

		try
		{
			sourceadr = Short.parseShort(args[0]);
			destadr = Short.parseShort(args[1]);
			empfangsfenster = Short.parseShort(args[2]);
		} catch (NumberFormatException exc)
		{
			exc.printStackTrace();
		}

		new RNReceiver(sourceadr, destadr, empfangsfenster);
	}

	public RNReceiver(short sourceadr, short destadr, short empfangsfenster)
	{
		try
		{
			// Netzwerkkarte instanziieren, Empfang über receive(byte[] frame)
			NetworkCard nwcard = new NetworkCard(this);
			this.sw = new SlidingWindow(nwcard, sourceadr, destadr, empfangsfenster);

		} catch (Exception e)
		{
			System.out.println(e);
		}
	}

	public void receive(byte[] frame)
	{
		this.sw.receive(frame);
	}
}