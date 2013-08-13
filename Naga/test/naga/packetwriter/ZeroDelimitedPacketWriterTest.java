package naga.packetwriter;
/**
 * Undocumented Class
 *
 * @author Christoffer Lerno
 */

import junit.framework.TestCase;

import java.nio.ByteBuffer;

public class ZeroDelimitedPacketWriterTest extends TestCase
{
    public void testZeroDelimitedPacketWriter() throws Exception
    {
        final ZeroDelimitedPacketWriter writer = new ZeroDelimitedPacketWriter();
        final ByteBuffer part1 = ByteBuffer.wrap("FOO".getBytes());
        final ByteBuffer part2 = ByteBuffer.wrap("bar".getBytes());
        final ByteBuffer[] result = writer.write(new ByteBuffer[]{part1, part2});
        final ByteBuffer buffer = ByteBuffer.allocate(100);
        for (final ByteBuffer b : result)
        {
            buffer.put(b);
        }
        buffer.flip();

        final byte[] resultByte = new byte[buffer.limit()];
        buffer.get(resultByte);
        assertEquals("FOObar\0", new String(resultByte));
        final ByteBuffer part3 = ByteBuffer.wrap("BAZ".getBytes());
        final ByteBuffer part4 = ByteBuffer.wrap("fooo".getBytes());
        final ByteBuffer[] result2 = writer.write(new ByteBuffer[]{part3, part4});
        final ByteBuffer buffer2 = ByteBuffer.allocate(100);
        for (final ByteBuffer b : result2)
        {
            buffer2.put(b);
        }
        buffer2.flip();

        final byte[] resultByte2 = new byte[buffer2.limit()];
        buffer2.get(resultByte2);
        assertEquals("BAZfooo\0", new String(resultByte2));
    }
}
