// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.util;

import junit.framework.TestCase;

public class ProgressStatsTest extends TestCase {

	private TimeKeeper tk=null;

	@Override
	protected void setUp() {
		tk=new TimeKeeper();
	}

	public void testSimple() throws Exception {
		tk.t=1146460996000L;
		ProgressStats tps=new TestProgressStats(5, tk);
		String[] msgs={
				"4 remaining.  Avg=15s, Estimate=60s (Sun Apr30 22:24:47)",
				"3 remaining.  Avg=20s, Estimate=60s (Sun Apr30 22:25:17)",
				"2 remaining.  Avg=22.5s, Estimate=45s (Sun Apr30 22:25:32)",
				"1 remaining.  Avg=24s, Estimate=24s (Sun Apr30 22:25:41)",
				"0 remaining.  Avg=25s, Estimate=0s (Sun Apr30 22:25:47)",
		};
		tps.start();
		tk.t+=30000;
		tps.stop();
		tk.t+=1190;
		assertEquals(msgs[0], tps.toString());
		assertEquals(60.0d, tps.getEstimatedTimeRemaining());
		assertEquals(15.0d, tps.getRunningAverage());
		assertEquals(30.0d, tps.getOverallAverage());
		assertEquals(30000L, tps.getLastProcessTime());
		for(int i=1; i<5; i++) {
			tps.start();
			tk.t+=30000;
			tps.stop();
			tk.t+=138;
			assertEquals(msgs[i], tps.toString());
		}
	}

	static class TestProgressStats extends ProgressStats {
		private TimeKeeper tk=null;
		public TestProgressStats(int size, TimeKeeper t) {
			super(size);
			tk=t;
		}
		@Override
		protected long getTime() {
			return tk.t;
		}

	}

	static class TimeKeeper {
		long t=0;
	}
}
