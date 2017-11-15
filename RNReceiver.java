import rn.*;

public class RNReceiver implements Receiver
{
	private SlidingWindow sw;

	public static void main(String[] args)
	{
		int sourceadr = 0;
		int destadr = 0;
		int empfangsfenster = 0;

		try
		{
			sourceadr = Integer.parseInt(args[0]);
			destadr = Integer.parseInt(args[1]);
			empfangsfenster = Integer.parseInt(args[2]);
		} catch (NumberFormatException exc)
		{
			exc.printStackTrace();
		}

		// System.out.println("Params: " + args[0] + ", " + args[1] + ", " + args[2]);
		new RNReceiver(sourceadr, destadr, empfangsfenster);
	}

	public RNReceiver(int sourceadr, int destadr, int empfangsfenster)
	{
		try
		{
			// Netzwerkkarte instanziieren, Empfang über receive(byte[] frame)
			NetworkCard nwcard = new NetworkCard(this);
			this.sw = new SlidingWindow(nwcard);

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