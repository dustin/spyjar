//
// arch-tag: 7E61AA4A-1110-11D9-B9A1-000A957659CC

package net.spy.pool;

import net.spy.net.SNPP;
import net.spy.util.SpyConfig;

/**
 * PoolFiller object to fill a pool with SNPP PoolAbles
 */
public class SNPPPoolFiller extends PoolFiller {

	public SNPPPoolFiller(String name, SpyConfig conf) {
		super(name, conf);
	}

	/**
	 * get a new object for the pool.
	 *
	 * The following config entries are required:
	 * <ul>
	 *  <li>snppHost - SNPP server hostname</li>
	 * </ul>
	 *
	 * The following config entries are optional:
	 * <ul>
	 *  <li>snppPort - Alternate SNPP server port.  Default is 444</li>
	 *  <li>max_age - The maximum amount of time (in milliseconds) that the
	 *      connection can live.  Default is forever</li>
	 * </ul>
	 *
	 * @exception PoolException if a new connection could not be made.
	 */
	@Override
	public PoolAble getObject() throws PoolException {
		SNPPPoolAble sp = null;
		try {
			String hostname=null;
			int port=SNPP.SNPP_PORT;

			hostname=getProperty("snppHost");
			if(hostname==null) {
				throw new Exception("No snppHost property given");
			}

			port=getPropertyInt("snppPort", SNPP.SNPP_PORT);

			int timeout=getPropertyInt("snppTimeout", 0);

			long maxAge=getPropertyInt("max_age", 0);

			// Grab a connection.
			SNPP snpp = new SNPP(hostname, port, timeout);
			// Create the PoolAble object
			sp=new SNPPPoolAble(snpp, maxAge, getPoolHash());
		} catch(Exception e) {
			throw new PoolException(
				"Error getting new SNPP object for the "
					+ debugName() + " pool:  " + e
				);
		}

		return(sp);
	}
}
