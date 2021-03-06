// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>

package net.spy.cache;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import net.spy.SpyThread;

/**
 * Listen for multicast request to clear cache for a given prefix.
 */
public class CacheClearRequestListener extends SpyThread {

	private final MulticastSocket s;
	private final InetAddress group;
	private final int port;
	private int requests=0;
	private volatile boolean running=true;

	/**
	 * Get an instance of CacheClearRequestListener.
	 */
	public CacheClearRequestListener(InetAddress g, int p)
		throws IOException {

		super();

		getLogger().info("Starting multicast cache listener on %s:%d", g, p);
		setDaemon(true);
		setName("SpyCacheClearRequestListener");

		group=g;
		port=p;

		s=makeMCastSocket(port);
		s.joinGroup(g);

		start();
	}

	protected MulticastSocket makeMCastSocket(int p) throws IOException {
		return new MulticastSocket(p);
	}

	/**
	 * String me.
	 */
	@Override
	public String toString() {
		return super.toString()
			+ " on " + group.getHostAddress() + ":" + port
			+ " processed " + requests + " requests";
	}

	/**
	 * Tell the thing to stop running.
	 */
	public void stopRunning() {
		running=false;
		try {
			s.leaveGroup(group);
			s.close();
		} catch(IOException ioe) {
			getLogger().error("IOException when leaving group", ioe);
		}
	}

	/**
	 * Do that crazy flush thing.
	 */
	public void flush(DatagramPacket recv) {
		byte[] data=recv.getData();
		byte[] tmp=new byte[recv.getLength()];
		System.arraycopy(data, 0, tmp, 0, tmp.length);
		String prefix=new String(tmp);
		getLogger().info("CacheClearRequestListener flushing ``%s''"
					+ " per mcast req from %s", prefix, recv.getAddress());
		requests++;

		// Do it.
		SpyCache cache=SpyCache.getInstance();
		cache.uncacheLike(prefix);
	}

	/**
	 * Run.
	 */
	@Override
	public void run() {
		while(running) {
			try {
				byte[] data=new byte[1500];
				DatagramPacket recv = new DatagramPacket(data, data.length);
				s.receive(recv);
				flush(recv);
			} catch(IOException e) {
				getLogger().error("IOException processing packet.", e);
			}
		}
	}

}
