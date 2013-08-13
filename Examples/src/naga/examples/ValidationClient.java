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
import naga.SocketObserver;
import naga.packetreader.RegularPacketReader;
import naga.packetwriter.RegularPacketWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * A client for exercising the validation server.
 * <p>
 * Use with {@code java naga.examples.ValidationClient [host] [port] [account] [password]}.
 * @author Christoffer Lerno
 */
public class ValidationClient
{
	ValidationClient()
	{}

	/**
	 * Make a login request to the server.
	 *
	 * @param args assumed to be 4 strings representing host, port, account and password.
	 */
	public static void main(final String... args)
	{
		try
		{
			// Parse arguments.
			final String host = args[0];
			final int port = Integer.parseInt(args[1]);
			final String account = args[2];
			final String password = args[3];

			// Prepare the login packet, packing two UTF strings together
			// using a data output stream.
			final ByteArrayOutputStream stream = new ByteArrayOutputStream();
			final DataOutputStream dataStream = new DataOutputStream(stream);
			dataStream.writeUTF(account);
			dataStream.writeUTF(password);
			dataStream.flush();
			final byte[] content = stream.toByteArray();
			dataStream.close();

			// Start up the service.
			final NIOService service = new NIOService();

			// Open our socket.
			final NIOSocket socket = service.openSocket(host, port);

			// Use regular 1 byte header reader/writer
			socket.setPacketReader(new RegularPacketReader(1, true));
			socket.setPacketWriter(new RegularPacketWriter(1, true));

			// Start listening to the socket.
			socket.listen(new SocketObserver()
			{
				@Override
                public void connectionOpened(final NIOSocket nioSocket)
				{
					System.out.println("Sending login...");
					nioSocket.write(content);
				}

                @Override
                public void packetSent(final NIOSocket socket, final Object tag)
                {
                    System.out.println("Packet sent");
                }

                @Override
                public void packetReceived(final NIOSocket socket, final byte[] packet)
				{
					try
					{
						// Read the UTF-reply and print it.
						final String reply = new DataInputStream(new ByteArrayInputStream(packet)).readUTF();
						System.out.println("Reply was: " + reply);
						// Exit the program.
						System.exit(0);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}

				@Override
                public void connectionBroken(final NIOSocket nioSocket, final Exception exception)
				{
					System.out.println("Connection failed.");
					// Exit the program.
					System.exit(-1);
				}
			});
			// Read IO until process exits.
			while (true)
			{
				service.selectBlocking();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

}
