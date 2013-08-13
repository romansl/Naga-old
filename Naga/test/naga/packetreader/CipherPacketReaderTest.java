package naga.packetreader;
/**
 * Undocumented Class
 *
 * @author Christoffer Lerno
 */

import junit.framework.TestCase;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.Key;

public class CipherPacketReaderTest extends TestCase
{
    CipherPacketReader m_cipherPacketReader;

    public void testNextPacketStreamCipher() throws Exception
    {
        final Key key = new SecretKeySpec("FOOBAR".getBytes(), "RC4");
        final Cipher encrypt = Cipher.getInstance("RC4");
        encrypt.init(Cipher.ENCRYPT_MODE, key);
        final byte[] bytes = encrypt.doFinal("ABCDEFGHIJK\n Testing\n Testing!\n ABC".getBytes());
        final Cipher decrypt = Cipher.getInstance("RC4");
        decrypt.init(Cipher.DECRYPT_MODE, key);
        m_cipherPacketReader = new CipherPacketReader(decrypt, new AsciiLinePacketReader());
        final ByteBuffer buffer1 = ByteBuffer.wrap(bytes, 0, 5);
        final ByteBuffer buffer2 = ByteBuffer.wrap(bytes, 5, 10);
        final ByteBuffer buffer3 = ByteBuffer.wrap(bytes, 15, bytes.length - 15);
        assertEquals(null, m_cipherPacketReader.nextPacket(buffer1));
        assertEquals(0, buffer1.remaining());
        assertEquals("ABCDEFGHIJK", new String(m_cipherPacketReader.nextPacket(buffer2)));
        assertEquals(0, buffer2.remaining());
        assertEquals(null, m_cipherPacketReader.nextPacket(buffer2));
        assertEquals(" Testing", new String(m_cipherPacketReader.nextPacket(buffer3)));
        assertEquals(0, buffer3.remaining());
        assertEquals(" Testing!", new String(m_cipherPacketReader.nextPacket(buffer3)));
        assertEquals(null, m_cipherPacketReader.nextPacket(buffer3));
    }

    public void testNextPacketBlockCipher() throws Exception
    {
        final Key key = new SecretKeySpec("1234567890ABCDEF".getBytes(), "AES");
        final Cipher encrypt = Cipher.getInstance("AES");
        encrypt.init(Cipher.ENCRYPT_MODE, key);
        final byte[] bytes = encrypt.doFinal("ABCDEFGHIJK\n Testing\n Testing!\n ABC".getBytes());
        final Cipher decrypt = Cipher.getInstance("AES");
        decrypt.init(Cipher.DECRYPT_MODE, key);
        m_cipherPacketReader = new CipherPacketReader(decrypt, new AsciiLinePacketReader());
        final ByteBuffer buffer1 = ByteBuffer.wrap(bytes, 0, 5);
        final ByteBuffer buffer2 = ByteBuffer.wrap(bytes, 5, 10);
        final ByteBuffer buffer3 = ByteBuffer.wrap(bytes, 15, bytes.length - 15);
        assertEquals(null, m_cipherPacketReader.nextPacket(buffer1));
        assertEquals(0, buffer1.remaining());
        assertEquals(null, m_cipherPacketReader.nextPacket(buffer2));
        assertEquals(0, buffer2.remaining());
        assertEquals("ABCDEFGHIJK", new String(m_cipherPacketReader.nextPacket(buffer3)));
        assertEquals(" Testing", new String(m_cipherPacketReader.nextPacket(buffer3)));
        assertEquals(" Testing!", new String(m_cipherPacketReader.nextPacket(buffer3)));
        assertEquals(null, m_cipherPacketReader.nextPacket(buffer3));
    }

}
