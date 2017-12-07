import java.nio.ByteBuffer;
import java.util.Arrays;

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

	@Override
	public String toString()
	{
		return "Frame [sourceAdr=" + sourceAdr + ", destAdr=" + destAdr + ", sequNr=" + sequNr + ", flags=" + flags
				+ ", checksum=" + checksum + ", payloadLength=" + payloadLength + ", payload="
				+ Arrays.toString(payload) + "]";
	}

	// Konstruktor fuer empfangene byte[] frames
	public Frame(byte[] rawFrame)
	{
		ByteBuffer bb = ByteBuffer.wrap(rawFrame);
		bb.position(0);
		this.sourceAdr = bb.getShort();
		this.destAdr = bb.getShort();
		this.sequNr = bb.getShort();
		this.flags = bb.getShort();
		this.checksum = bb.getShort();
		this.payloadLength = bb.getShort();

		// restliche Positionen im ByteBuffer sind der Payload
		this.payload = new byte[bb.remaining()];

		for (int i = 0; i < payloadLength && i + 12 < rawFrame.length; i++)
		{
			payload[i] = bb.get(12 + i);
		}

		this.rawFrame = createFrame();
	}

	// Konstruktor fuer ACK-Rahmen (receiver) und Terminierungsrahmen (sender)
	public Frame(short sourceadr, short destadr, short sequNr, short flags)
	{
		this.sourceAdr = sourceadr;
		this.destAdr = destadr;
		this.sequNr = sequNr;
		this.flags = flags;
		this.payloadLength = 0;
		this.payload = null;
		this.checksum = createChecksum();
		this.rawFrame = createFrame();
	}

	// Konstruktor fuer Daten-Rahmen (sender)
	public Frame(short sourceadr, short destadr, short sequNr, byte[] payload)
	{
		this.sourceAdr = sourceadr;
		this.destAdr = destadr;
		this.sequNr = sequNr;
		this.flags = 0;

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
		if (payload != null)
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
		int sum = 0;
		if (payload != null) // Bei Terminierungsrahmen und ACKs ist payload null
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

			sum = sourceAdr + destAdr + sequNr + flags + payloadLength + iPayload;
		}
		else
		{
			sum = sourceAdr + destAdr + sequNr + flags + payloadLength;
		}

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
		}
		else
		{
			return false;
		}
	}
}