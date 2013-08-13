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
package naga;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * SSL-implementation on top of NIOSocket, wrapping all calls to the socket.
 *
 * @author Christoffer Lerno
 */
class SSLSocketChannelResponder implements NIOSocketSSL, SocketObserver
{
    private final NIOSocket m_wrappedSocket;
    private final SSLPacketHandler m_packetHandler;
    private final NIOService m_nioService;
    private SocketObserver m_observer;

    public SSLSocketChannelResponder(final NIOService nioService, final NIOSocket wrappedSocket, final SSLEngine engine, final boolean client) {
        m_nioService = nioService;
        m_wrappedSocket = wrappedSocket;
        m_packetHandler = new SSLPacketHandler(engine, m_wrappedSocket, this);
        m_wrappedSocket.setPacketReader(m_packetHandler);
        m_wrappedSocket.setPacketWriter(m_packetHandler);
        engine.setUseClientMode(client);
    }

    @Override
    public void beginHandshake() throws SSLException
    {
        if (getSSLEngine().getHandshakeStatus() != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) throw new IllegalStateException("Tried to start handshake during handshake.");
        m_packetHandler.begin();
    }

    @Override
    public boolean isEncrypted()
    {
        return m_packetHandler.isEncrypted();
    }

    @Override
    public SSLEngine getSSLEngine()
    {
        return m_packetHandler.getSSLEngine();
    }


    @Override
    public boolean write(final byte[] packet)
    {
        return m_wrappedSocket.write(packet);
    }

    @Override
    public boolean write(final byte[] packet, final Object tag)
    {
        return m_wrappedSocket.write(packet, tag);
    }

    @Override
    public void queue(final Runnable runnable)
    {
        m_wrappedSocket.queue(runnable);
    }

    @Override
    public long getBytesRead()
    {
        return m_wrappedSocket.getBytesRead();
    }

    @Override
    public long getBytesWritten()
    {
        return m_wrappedSocket.getBytesWritten();
    }

    @Override
    public long getTimeOpen()
    {
        return m_wrappedSocket.getTimeOpen();
    }

    @Override
    public long getWriteQueueSize()
    {
        return m_wrappedSocket.getWriteQueueSize();
    }

    @Override
    public int getMaxQueueSize()
    {
        return m_wrappedSocket.getMaxQueueSize();
    }

    @Override
    public void setMaxQueueSize(final int maxQueueSize)
    {
        m_wrappedSocket.setMaxQueueSize(maxQueueSize);
    }

    @Override
    public void setPacketReader(final PacketReader packetReader)
    {
        m_packetHandler.setReader(packetReader);
    }

    @Override
    public void setPacketWriter(final PacketWriter packetWriter)
    {
        m_wrappedSocket.queue(new Runnable()
        {
            @Override
            public void run()
            {
                m_packetHandler.setWriter(packetWriter);
            }
        });
    }

    @Override
    public void listen(final SocketObserver socketObserver)
    {
        m_observer = socketObserver;
        m_wrappedSocket.listen(this);
    }

    @Override
    public void closeAfterWrite()
    {
        m_packetHandler.closeEngine();
        m_wrappedSocket.closeAfterWrite();
    }

    @Override
    public Socket socket()
    {
        return m_wrappedSocket.socket();
    }

    @Override
    public void close()
    {
        m_wrappedSocket.close();
    }

    @Override
    public InetSocketAddress getAddress()
    {
        return m_wrappedSocket.getAddress();
    }

    @Override
    public boolean isOpen()
    {
        return m_wrappedSocket.isOpen();
    }

    @Override
    public String getIp()
    {
        return m_wrappedSocket.getIp();
    }

    @Override
    public int getPort()
    {
        return m_wrappedSocket.getPort();
    }

    @Override
    public Object getTag()
    {
        return m_wrappedSocket.getTag();
    }

    @Override
    public void setTag(final Object tag)
    {
        m_wrappedSocket.setTag(tag);
    }

    void closeDueToSSLException(final SSLException e)
    {
        try
        {
            if (m_observer != null) m_observer.connectionBroken(this, e);
        }
        catch (Exception ex)
        {
            m_nioService.notifyException(e);
        }
        m_wrappedSocket.close();
    }

    @Override
    public void connectionOpened(final NIOSocket nioSocket)
    {
        try
        {
            if (m_observer != null) m_observer.connectionOpened(this);
        }
        catch (Exception e)
        {
            m_nioService.notifyException(e);
        }
    }

    @Override
    public void connectionBroken(final NIOSocket nioSocket, final Exception exception)
    {
        try
        {
            if (m_observer != null) m_observer.connectionBroken(this, exception);
        }
        catch (Exception e)
        {
            m_nioService.notifyException(e);
        }
    }

    @Override
    public void packetReceived(final NIOSocket socket, final byte[] packet)
    {
        try
        {
            if (m_observer != null) m_observer.packetReceived(this, packet);
        }
        catch (Exception e)
        {
            m_nioService.notifyException(e);
        }
    }

    @Override
    public void packetSent(final NIOSocket socket, final Object tag)
    {
        try
        {
            if (m_observer != null) m_observer.packetSent(this, tag);
        }
        catch (Exception e)
        {
            m_nioService.notifyException(e);
        }
    }

}
