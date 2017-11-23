import java.nio.ByteBuffer;

public class Frame
{
	// 16 Bit: 2 Byte groß:
	private short sourceAdr;
	private short destAdr;
	private short sequNr;
	private short flags;
	private short checksum;
	private short payloadLength;
	// quasi beliebig viele Bits:
	private byte[] payload;

	private byte[] rawFrame; // besteht aus allen obigen Daten
	boolean ack; // zeigt an, ob Frame ein ACK ist
	boolean term; // zeigt an, ob Frame ein Terminierungsframe ist

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

	// RawFrame == Byte array wie es im System uebertragen wird
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
		this.sourceAdr = sourceadr;
		this.destAdr = destadr;
		this.sequNr = sequNr;

		if (term == true) // ack fuer ACK-Rahmen immer true
		{
			this.flags = 3;
		} else
		{
			this.flags = 1;
		}

		// TODO: payload + payloadLength einfuegen
		this.checksum = createChecksum();
		this.rawFrame = createFrame();
	}

	// Konstruktor fuer Daten-Rahmen
	public Frame(short sourceadr, short destadr, short sequNr, byte[] payload, boolean term)
	{
		this.sourceAdr = sourceadr;
		this.destAdr = destadr;
		this.sequNr = sequNr;

		if (term == true) // ack fuer ACK-Rahmen immer false
		{
			this.flags = 2;
		} else
		{
			this.flags = 0;
		}

		this.payloadLength = (short) payload.length;
		this.payload = payload;
		// immer zuletzt, da greift auf Datenfelder der Klasse zurueck
		this.checksum = createChecksum();
		this.rawFrame = createFrame();
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

	public byte[] shortToBytes(short s)
	{
		byte[] b = ByteBuffer.allocate(2).putShort(s).array();
		return b;
	}

	// adds one byte array to another
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
		ByteBuffer bb = ByteBuffer.allocate(payload.length % 2 == 0 ? payload.length : payload.length + 1);
		bb.put(payload); // Bufferposition ist am Ende

		int iPayload = 0;
		if (payload.length % 2 != 0)
		{
			bb.put((byte) 0);
		}
		bb.position(0);
		for (int i = 0; i < payload.length / 2; i++)
		{
			iPayload += 0xffff & bb.getShort(); // durch Verundung mit 0xffff wird der short zu int
		}

		int sum = sourceAdr + destAdr + sequNr + flags + payloadLength + iPayload;

		int max = 65536; // Maxwert von 16 Bit + 1
		int div = 0;
		int rest = sum;
		while (max <= rest)
		{
			rest = rest - max;
			div++;
		}

		// Einerkomplementsumme (Ueberlauf addieren)
		int einerkomp = rest + div;
		// Einerkomplement (Bits umdrehen)
		max--;
		int checksum = max - einerkomp;
		return (short) checksum;
	}

	public boolean isChecksumCorrekt()
	{
		if (createChecksum() == getChecksum())
		{
			return true;
		} else
		{
			return false;
		}
	}
}