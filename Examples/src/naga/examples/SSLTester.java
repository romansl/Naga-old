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
package naga.examples;

import naga.NIOService;
import naga.NIOSocket;
import naga.NIOSocketSSL;
import naga.SocketObserver;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;

/**
 * Undocumented Class
 *
 * @author Christoffer Lerno
 */
public class SSLTester
{
    public static void main(final String... args)
    {
        try
        {
            final NIOService service = new NIOService();
            final SSLEngine engine = SSLContext.getDefault().createSSLEngine();
            final NIOSocket socket = service.openSSLSocket(engine, "www.sslshopper.com", 443);
            socket.listen(new SocketObserver() {
                @Override
                public void packetSent(final NIOSocket socket, final Object tag)
                {
                    System.out.println("Packet sent");
                }

                @Override
                public void connectionOpened(final NIOSocket nioSocket)
                {
                    try
                    {
                        ((NIOSocketSSL)nioSocket).beginHandshake();
                    }
                    catch (SSLException e)
                    {
                        e.printStackTrace();
                    }
                    System.out.println("*Connection opened");
                    nioSocket.write("GET /ssl-converter.html HTTP/1.0\r\n\r\n".getBytes());
                }

                @Override
                public void connectionBroken(final NIOSocket nioSocket, final Exception exception)
                {
                    System.out.println("*Connection broken");
                    if (exception != null) exception.printStackTrace();
                    System.exit(9);
                }

                @Override
                public void packetReceived(final NIOSocket socket, final byte[] packet)
                {
                    System.out.println("*Unencrypted Packet received " + packet.length);
                    System.out.println(new String(packet));
                }
            });
           //         https://www.sslshopper.com/ssl-converter.html
            while (true)
            {
                service.selectBlocking();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
