/*
Copyright (c) 2008-2011 Christoffer Lernö

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package naga.packetwriter;

import naga.PacketWriter;

import java.nio.ByteBuffer;

/**
 * Writes a byte packet to the stream without doing any changes to it.
 * <p>
 * This is the commonly case when one wants to output text or similarly
 * delimited data.
 *
 * @author Christoffer Lerno
 */
public class RawPacketWriter implements PacketWriter
{
    public static RawPacketWriter INSTANCE = new RawPacketWriter();

    private RawPacketWriter()
    {
    }

    @Override
    public ByteBuffer[] write(final ByteBuffer[] byteBuffers)
    {
        return byteBuffers;
    }
}
