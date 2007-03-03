// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: 4881FD35-A7DF-4702-A622-9A2102112089

package net.spy.cache;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import junit.framework.TestCase;

public class CacheClearListenerTest extends TestCase {

	@Override
	protected void tearDown() {
		SpyCache.shutdown();
	}

	public void testCacheClear() throws Exception {
		String key="cleartest";
		SpyCache sc=SpyCache.getInstance();
		assertNull(sc.get(key));
		sc.store(key, "X", 10000);
		assertNotNull(sc.get(key));

		final BlockingQueue<DatagramPacket> q=
			new ArrayBlockingQueue<DatagramPacket>(8);

		InetAddress addr=InetAddress.getByAddress(
				new byte[]{(byte) 224, 0, 0, 1});
		int port=1984;

		CacheClearRequestListener c=new CacheClearRequestListener(addr, port) {

			@Override
			protected MulticastSocket makeMCastSocket(int p)
				throws IOException {
				return new LoopbackMulticastSocket(q);
			}
		};

		assertTrue(c.toString().endsWith("processed 0 requests"));

		byte[] data="cleartest".getBytes();
		q.put(new DatagramPacket(data, data.length, addr, port));
		Thread.sleep(100);

		// Should be gone now.
		assertNull(sc.get(key));

		assertTrue(c.toString().endsWith("processed 1 requests"));

		c.stopRunning();
	}

    // This is a multicast socket that doesn't use the network.
    private static class LoopbackMulticastSocket extends MulticastSocket {

        private BlockingQueue<DatagramPacket> queue = null;
        private boolean isBound = false;

        public LoopbackMulticastSocket(BlockingQueue<DatagramPacket> q)
            throws IOException {
            super();
            queue = q;
        }

        @Override
		public void send(DatagramPacket p) throws IOException {
        	try {
				queue.put(p);
			} catch (InterruptedException e) {
				throw new IOException("Interrupted");
			}
        }

        @Override
		public void receive(DatagramPacket p) throws IOException {
                try {
                    DatagramPacket rcvPkt = queue.remove();
                    assert p.getOffset() == 0
                        : "unexpected p offset " + p.getOffset();
                    assert rcvPkt.getOffset() == 0
                        : "unexpected rcv offset " + p.getOffset();
                    assert rcvPkt.getLength() > 0 : "too short";
                    assert rcvPkt.getLength() <= p.getData().length
                        : "too long " + rcvPkt.getLength() + " longer than "
                            + p.getData().length;
                    System.arraycopy(rcvPkt.getData(), rcvPkt.getOffset(),
                        p.getData(), 0, rcvPkt.getLength());
                    p.setLength(rcvPkt.getLength());
                    p.setPort(rcvPkt.getPort());
                    p.setSocketAddress(rcvPkt.getSocketAddress());
                } catch(NoSuchElementException e) {
                    IOException toThrow = new IOException("No packet!");
                    toThrow.initCause(e);
                    throw toThrow;
                }
        }

        @Override
		public boolean isBound() {
            return (isBound);
        }

        @Override
		public void joinGroup(InetAddress sa) throws IOException {
            isBound = true;
        }

        @Override
		public void leaveGroup(InetAddress sa) throws IOException {
            isBound = false;
        }

        @Override
		public void close() {
            queue.clear();
            super.close();
        }
    }

}
