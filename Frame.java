import java.nio.ByteBuffer;
import rn.TestData;

public class Frame
{
	// 16 Bit: sourceadr, destadr, sequenceNumber, Flags, Checksum, PayloadLength,
	// Checksum
	// Beliebige Bit: Payload
	private short sourceAdr; // 2 Byte groß
	private short destAdr; // 2 Byte groß
	private short sequNr; // 2 Byte groß
	private short flags; // 2 Byte groß
	private short checksum; // 2 Byte groß
	private short payloadLength; // 2 Byte groß
	private byte[] payload; // beliebig viele Bits

	private byte[] rawFrame; // besteht aus allen obigen Daten
	boolean ack;
	boolean term;

	public short getSourceAdr()
	{
		return sourceAdr;
	}

	public short getDestAdr()
	{
		return destAdr;
	}

	public short getSequNr()
	{
		return sequNr;
	}

	public short getFlags()
	{
		return flags;
	}

	public short getChecksum()
	{
		return checksum;
	}

	public short getPayloadLength()
	{
		return payloadLength;
	}

	public byte[] getPayload()
	{
		return payload;
	}

	// RawFrame == ByteArray wie es im Netzwerk übertragen wird
	public byte[] getRawFrame()
	{
		return rawFrame;
	}

	// Konstruktor fuer empfangene byte[] frames
	public Frame(byte[] rawFrame)
	{
		ByteBuffer bb = ByteBuffer.allocate(rawFrame.length).put(rawFrame);
		bb.position(0);
		this.sourceAdr = bb.getShort();
		this.destAdr = bb.getShort();
		this.sequNr = bb.getShort();
		this.flags = bb.getShort();
		this.checksum = bb.getShort();
		this.payloadLength = bb.getShort();

		// TODO: restliche Positionen im ByteBuffer sind der Payload
		// ~ this.payload = bb.getBytes.array();
	}

	// Konstruktor fuer ACK-Rahmen
	public Frame(short sourceadr, short destadr, short sequNr, boolean term)
	{
		// TODO: restlichen frame Inhalt einfuegen

		if (term == true)
		{
			this.flags = 3;
		} else
		{
			this.flags = 1;
		}
	}

	// Konstruktor fuer Daten-Rahmen
	public Frame(short sourceadr, short destadr, short sequNr, byte[] payload, boolean term)
	{
		this.sourceAdr = sourceadr;
		this.destAdr = destadr;
		this.sequNr = sequNr;

		if (term == true)
		{
			this.flags = 2;
		} else
		{
			this.flags = 0;
		}
		this.checksum = createChecksum();
		this.payloadLength = (short) payload.length;
		this.payload = payload;

		this.rawFrame = createFrame();
	}

	public byte[] shortToBytes(short s)
	{
		byte[] b = ByteBuffer.allocate(2).putShort(s).array();
		return b;
	}

	// erzeugt das byte array fuer rawFrame
	public byte[] createFrame()
	{
		byte[] frame = add(shortToBytes(sourceAdr), shortToBytes(destAdr));
		frame = add(frame, shortToBytes(sequNr));
		frame = add(frame, shortToBytes(flags));
		frame = add(frame, shortToBytes(checksum));
		frame = add(frame, shortToBytes(payloadLength));
		frame = add(frame, payload);
		return frame;
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

	public short createChecksum()
	{
		// TODO: checksum berechnen

		// alle shorts addieren

		// short = 65536 bit

		short checksum = 0;
		return checksum;
	}

	public boolean isChecksumCorrekt()
	{
		// TODO: Checksum ueberpruefen
		Boolean b = true;
		return b;
	}
}