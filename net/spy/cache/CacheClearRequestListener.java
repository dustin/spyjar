// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: CacheClearRequestListener.java,v 1.4 2003/07/26 07:46:51 dustin Exp $

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
	public CacheClearRequestListener(InetAddress group, int port)
		throws IOException {

		super();

		getLogger().info("Starting multicast cache listener on "
			+ group + ":" + port);
		setDaemon(true);
		setName("SpyCacheClearRequestListener");

		this.group=group;
		this.port=port;

		s=new MulticastSocket(port);
		s.joinGroup(group);

		start();
	}

	/**
	 * String me.
	 */
	public String toString() {
		StringBuffer sb=new StringBuffer(64);

		sb.append(super.toString());

		sb.append(" on ");
		sb.append(group.getHostAddress());
		sb.append(":");
		sb.append(port);
		sb.append(" processed ");
		sb.append(requests);
		sb.append(" requests");

		return(sb.toString());
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
		getLogger().info("CacheClearRequestListener flushing ``"
			+ prefix + "'' per multicast request from " + recv.getAddress());
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
