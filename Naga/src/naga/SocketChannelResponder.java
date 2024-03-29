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

import naga.packetreader.RawPacketReader;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Christoffer Lerno
 */
class SocketChannelResponder extends ChannelResponder implements NIOSocket
{
	private int m_maxQueueSize;
	private long m_timeOpened;
	private final AtomicLong m_bytesInQueue;
	private ConcurrentLinkedQueue<Object> m_packetQueue;
	private PacketReader m_packetReader;
	private volatile SocketObserver m_socketObserver;
    private final SocketReader m_socketReader;
    private final SocketWriter m_socketWriter;

	public SocketChannelResponder(final NIOService service, final SocketChannel socketChannel, final InetSocketAddress address)
	{
		super(service, socketChannel, address);
		m_socketObserver = null;
		m_maxQueueSize = -1;
		m_timeOpened = -1;
		m_packetReader = RawPacketReader.INSTANCE;
		m_bytesInQueue = new AtomicLong(0L);
		m_packetQueue = new ConcurrentLinkedQueue<Object>();
        m_socketReader = new SocketReader(service);
        m_socketWriter = new SocketWriter();
	}

	@Override
    void keyInitialized()
	{
		if (!isConnected())
		{
			addInterest(SelectionKey.OP_CONNECT);
		}
	}

	@Override
    public void closeAfterWrite()
	{
        queue(new Runnable() {
            @Override
            public void run()
            {
                m_packetQueue.clear();
                close(null);
            }
        });
	}

    @Override
    public void queue(final Runnable runnable)
    {
        m_packetQueue.offer(runnable);
        getNIOService().queue(new AddInterestEvent(SelectionKey.OP_WRITE));
    }

    @Override
    public boolean write(final byte[] packet, final Object tag)
    {
        final long currentQueueSize = m_bytesInQueue.addAndGet(packet.length);
        if (m_maxQueueSize > 0 && currentQueueSize > m_maxQueueSize)
        {
            m_bytesInQueue.addAndGet(-packet.length);
            return false;
        }

        // Add the packet.
        m_packetQueue.offer(tag == null ? packet : new Object[] { packet, tag });
        getNIOService().queue(new AddInterestEvent(SelectionKey.OP_WRITE));

        return true;
    }

	@Override
    public boolean write(final byte[] packet)
	{
        return write(packet, null);
	}

	public boolean isConnected()
	{
		return getChannel().isConnected();
	}

    /**
     * Notify the observer that the packet is received, will log to the exception observer on NIOService if an error occurs.
     *
     * @param packet the packet received.
     */
    private void notifyPacketReceived(final byte[] packet)
    {
        try
        {
            if (m_socketObserver != null) m_socketObserver.packetReceived(this, packet);
        }
        catch (Exception e)
        {
            getNIOService().notifyException(e);
        }
    }


    /**
     * Notify the observer that the packet was sent. Will log to the exception observer on NIOService if an error occurs.
     *
     * @param tag the optional tag associated with the packet.
     */
    private void notifyPacketSent(final Object tag)
    {
        try
        {
            if (m_socketObserver != null) m_socketObserver.packetSent(this, tag);
        }
        catch (Exception e)
        {
            getNIOService().notifyException(e);
        }
    }

	@Override
    public void socketReadyForRead()
    {
		if (!isOpen()) return;
		try
		{
			if (!isConnected()) throw new IOException("Channel not connected.");
            while (m_socketReader.read(getChannel()) > 0)
            {
                byte[] packet;
                final ByteBuffer buffer = m_socketReader.getBuffer();
				while (buffer.remaining() > 0
                       && (packet = m_packetReader.nextPacket(buffer)) != null)
				{
                    if (packet == PacketReader.SKIP_PACKET) continue;
                    notifyPacketReceived(packet);
				}
                m_socketReader.compact();
			}
		}
		catch (Exception e)
		{
			close(e);
		}
	}

	private void fillCurrentOutgoingBuffer() {
        if (m_socketWriter.isEmpty())
        {
            // Retrieve next packet from the queue.
            Object nextPacket = m_packetQueue.poll();
            while (nextPacket != null && nextPacket instanceof Runnable)
            {
                ((Runnable) nextPacket).run();
                nextPacket = m_packetQueue.poll();
            }
            if (nextPacket == null) return;
            final byte[] data;
            Object tag = null;
            if (nextPacket instanceof byte[])
            {
                data = (byte[]) nextPacket;
            }
            else
            {
                data = (byte[])((Object[])nextPacket)[0];
                tag = ((Object[])nextPacket)[1];
            }
            m_socketWriter.setPacket(data, tag);
            // Remove the space reserved in the queue.
            m_bytesInQueue.addAndGet(-data.length);
        }
	}

	@Override
    public void socketReadyForWrite()
	{
		try
		{
			deleteInterest(SelectionKey.OP_WRITE);
			if (!isOpen()) return;
			fillCurrentOutgoingBuffer();

			// Return if there is nothing in the buffer to send.
			if (m_socketWriter.isEmpty())
            {
                return;
            }
			while (!m_socketWriter.isEmpty())
			{
                final boolean bytesWereWritten = m_socketWriter.write(getChannel());
				if (!bytesWereWritten)
				{
					// Change the interest ops in case we still have things to write.
					addInterest(SelectionKey.OP_WRITE);
					return;
				}
				if (m_socketWriter.isEmpty())
				{
                    notifyPacketSent(m_socketWriter.getTag());
					fillCurrentOutgoingBuffer();
				}
			}
		}
		catch (Exception e)
		{
			close(e);
		}
	}

	@Override
    public void socketReadyForConnect()
	{
		try
		{
			if (!isOpen()) return;
			if (getChannel().finishConnect())
			{
				deleteInterest(SelectionKey.OP_CONNECT);
				m_timeOpened = System.currentTimeMillis();
				notifyObserverOfConnect();
			}

		}
		catch (Exception e)
		{
			close(e);
		}
	}

	public void notifyWasCancelled()
	{
		close();
	}

	public Socket getSocket()
	{
		return getChannel().socket();
	}

	@Override
    public long getBytesRead()
	{
		return m_socketReader.getBytesRead();
	}

	@Override
    public long getBytesWritten()
	{
		return m_socketWriter.getBytesWritten();
	}

	@Override
    public long getTimeOpen()
	{
		return m_timeOpened > 0 ? System.currentTimeMillis() - m_timeOpened : -1;
	}

	@Override
    public long getWriteQueueSize()
	{
		return m_bytesInQueue.get();
	}

	public String toString()
	{
		try
		{
			return getSocket().toString();
		}
		catch (Exception e)
		{
			return "Closed NIO Socket";
		}
	}

	/**
	 * @return the current maximum queue size.
	 */
	@Override
    public int getMaxQueueSize()
	{
		return m_maxQueueSize;
	}

	/**
	 * Sets the maximum number of bytes allowed in the queue for this socket. If this
	 * number is less than 1, the queue is unbounded.
	 *
	 * @param maxQueueSize the new max queue size. A value less than 1 is an unbounded queue.
	 */
	@Override
    public void setMaxQueueSize(final int maxQueueSize)
	{
		m_maxQueueSize = maxQueueSize;
	}

	@Override
    public void listen(final SocketObserver socketObserver)
	{
		markObserverSet();
		getNIOService().queue(new BeginListenEvent(this, socketObserver == null ? SocketObserver.NULL : socketObserver));
	}

	/**
     * Notify the observer that the socket connected. Will log to the exception observer on NIOService if an error occurs.
     *
     */
	private void notifyObserverOfConnect()
	{
		try
		{
			if (m_socketObserver != null) m_socketObserver.connectionOpened(this);
		}
		catch (Exception e)
		{
            getNIOService().notifyException(e);
		}
	}

	/**
     * Notify the observer of the disconnect. Will log to the exception observer on NIOService if an error occurs.
	 *
	 * @param exception the exception causing the disconnect, or null if this was a clean close.
	 */
	private void notifyObserverOfDisconnect(final Exception exception)
	{
		try
		{
			if (m_socketObserver != null) m_socketObserver.connectionBroken(this, exception);
		}
		catch (Exception e)
		{
            getNIOService().notifyException(e);
		}
	}

	@Override
    public void setPacketReader(final PacketReader packetReader)
	{
		m_packetReader = packetReader;
	}

	@Override
    public void setPacketWriter(final PacketWriter packetWriter)
	{
        if (packetWriter == null) throw new NullPointerException();
        queue(new Runnable() {
            @Override
            public void run()
            {
                m_socketWriter.setPacketWriter(packetWriter);
            }
        });
 	}

	@Override
    public SocketChannel getChannel()
	{
		return (SocketChannel) super.getChannel();
	}

	@Override
    protected void shutdown(final Exception e)
	{
		m_timeOpened = -1;
		m_packetQueue.clear();
		m_bytesInQueue.set(0);
		notifyObserverOfDisconnect(e);
	}


    private class AddInterestEvent implements Runnable
    {
        private final int m_interest;

        private AddInterestEvent(final int interest)
        {
            m_interest = interest;
        }

        @Override
        public void run()
        {
            addInterest(m_interest);
        }
    }

	private class BeginListenEvent implements Runnable
	{
		private final SocketObserver m_newObserver;
		private final SocketChannelResponder m_responder;

		private BeginListenEvent(final SocketChannelResponder responder, final SocketObserver socketObserver)
		{
			m_responder = responder;
			m_newObserver = socketObserver;
		}

		@Override
        public void run()
		{
			m_responder.m_socketObserver =  m_newObserver;
			if (m_responder.isConnected())
			{
				m_responder.notifyObserverOfConnect();
			}
			if (!m_responder.isOpen())
			{
				m_responder.notifyObserverOfDisconnect(null);
			}
			m_responder.addInterest(SelectionKey.OP_READ);
		}

		@Override
		public String toString()
		{
			return "BeginListen[" + m_newObserver + "]";
		}
	}

	@Override
    public Socket socket()
	{
		return getChannel().socket();
	}
}
