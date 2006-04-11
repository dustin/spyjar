// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 5FE1F489-1110-11D9-B52F-000A957659CC

package net.spy.cache;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import net.spy.SpyThread;

/**
 * Listen for multicast request to clear cache for a given prefix.
 */
public final class CacheClearRequestListener extends SpyThread {

	private MulticastSocket s=null;
	private InetAddress group=null;
	private int port=-1;
	private int requests=0;
	private boolean running=true;

	/**
	 * Get an instance of CacheClearRequestListener.
	 */
	public CacheClearRequestListener(InetAddress g, int p)
		throws IOException {

		super();

		getLogger().info("Starting multicast cache listener on %s:%d", g, p);
		setDaemon(true);
		setName("SpyCacheClearRequestListener");

		this.group=g;
		this.port=p;

		s=new MulticastSocket(p);
		s.joinGroup(g);

		start();
	}

	/**
	 * String me.
	 */
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
		byte data[]=recv.getData();
		byte tmp[]=new byte[recv.getLength()];
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
	public void run() {
		while(running) {
			try {
				byte data[]=new byte[1500];
				DatagramPacket recv = new DatagramPacket(data, data.length);
				s.receive(recv);
				flush(recv);
			} catch(IOException e) {
				getLogger().error("IOException processing packet.", e);
			}
		}
	}

}
