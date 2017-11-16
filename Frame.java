import java.nio.ByteBuffer;
import rn.TestData;

public class Frame
{
	// 16 Bit: sourceadr, destadr, sequenceNumber, Flags, Checksum, PayloadLength
	// Beliebige Bit: Payload, Checksum
	private byte[] sourceAdr;
	private byte[] destAdr;
	private byte[] sequNr;
	private byte[] flags;
	private byte[] checksum;
	private byte[] payloadLength;
	private TestData testData;
	boolean ack;
	boolean term;

	public byte[] getSourceAdr()
	{
		return sourceAdr;
	}

	public byte[] getDestAdr()
	{
		return destAdr;
	}

	public byte[] getSequNr()
	{
		return sequNr;
	}

	public byte[] getFlags()
	{
		return flags;
	}

	public byte[] getChecksum()
	{
		return checksum;
	}

	public byte[] getPayloadLength()
	{
		return payloadLength;
	}

	public TestData getFrameData()
	{
		return testData;
	}

	// RawFrame == ByteArray wie es im Netzwerk übertragen wird
	public byte[] getRawFrame()
	{
		byte[] rawFrame = new byte[0];
		// gibt das frame als byte array zurück
		return rawFrame;
	}

	// Konstruktor, der ein empfangenes byte[] frame zerlegt
	public Frame(byte[] rawFrame)
	{
		// byte sowieso aus rawFrame = sourceAdr;
		// ...
	}

	// Konstruktor fuer ACK-Rahmen
	public Frame(String sourceadr, String destadr, short sequNr, boolean term)
	{
		// TODO: restlichen frame Inhalt einfuegen

		if (term == true)
		{
			this.flags = ByteBuffer.allocate(2).putShort((short) 3).array();
		} else
		{
			this.flags = ByteBuffer.allocate(2).putShort((short) 1).array();
		}
	}

	// Konstruktor fuer Daten-Rahmen
	public Frame(String sourceadr, String destadr, short sequNr, TestData testData, boolean term)
	{
		this.sourceAdr = sourceadr.getBytes();
		this.destAdr = destadr.getBytes();
		this.sequNr = ByteBuffer.allocate(2).putShort(sequNr).array(); // int to byte[]

		if (term == true) // fuer flags
		{
			this.flags = ByteBuffer.allocate(2).putShort((short) 2).array();
		} else
		{
			this.flags = ByteBuffer.allocate(2).putShort((short) 0).array();
		}

		this.testData = testData;
	}

	// befüllt das byte array frame
	public byte[] CreateFrame(byte[] payload)
	{
		byte[] frame = add(sourceAdr, destAdr);
		frame = add(frame, sequNr);
		frame = add(frame, flags);
		// frame = add(frame, checksum);
		// frame = add(frame, payloadLength);
		frame = add(frame, payload);

		return frame;
		// return payload; // um zu testen ob versch. Saetze gesendet werden
	}

	public byte[] add(byte[] ar1, byte[] ar2)
	{
		int length1 = ar1.length;
		int length2 = ar2.length;

		byte[] all = new byte[length1 + length2];

		for (int i = 0; i < length1; i++)
		{
			all[i] = ar1[i];
		}

		for (int i = length1; i < length1 + length2; i++)
		{
			all[i] = ar2[i - length1];
		}
		return all;
	}

	public byte[] CreateChecksum(byte[] frame)
	{
		// ...
		byte[] checksum = new byte[0];
		// ...

		return checksum;
	}
}