// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: F11444EF-C0B6-498B-B2DD-6797E23EB4FA

package net.spy.stat;

import java.util.concurrent.Callable;

import junit.framework.TestCase;
import net.spy.test.SyncThread;

public class StatTest extends TestCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Stats.setInstance(null);
	}

	@Override
	protected void tearDown() throws Exception {
		Stats.setInstance(null);
		super.tearDown();
	}

	public void testBumRushSingletonian() throws Throwable {
		int n=SyncThread.getDistinctResultCount(50, new Callable<Stats>() {
			public Stats call() throws Exception {
				return Stats.getInstance();
			}});
		assertEquals(1, n);
	}

	public void testBumRushStatFetch() throws Throwable {
		int n=SyncThread.getDistinctResultCount(50, new Callable<Stat>() {
			public Stat call() throws Exception {
				return Stats.getCounterStat("x");
			}});
		assertEquals(1, n);
	}

    public void testCounterStat() {
        CounterStat cs=new CounterStat();
        cs.setName("st.test");
        assertEquals(0, cs.getCount());
        assertEquals("0", cs.getStat());
        assertEquals("st.test=0", String.valueOf(cs));

        cs.setValue(100);
        assertEquals(100, cs.getCount());
        assertEquals("100", cs.getStat());
        assertEquals("st.test=100", String.valueOf(cs));

        cs.increment();
        assertEquals(101, cs.getCount());
        assertEquals("101", cs.getStat());
        assertEquals("st.test=101", String.valueOf(cs));

        cs.increment(3);
        assertEquals(104, cs.getCount());
        assertEquals("104", cs.getStat());
        assertEquals("st.test=104", String.valueOf(cs));

        cs.increment(-3);
        assertEquals(101, cs.getCount());
        assertEquals("101", cs.getStat());
        assertEquals("st.test=101", String.valueOf(cs));
    }

    private void assertComputingStat(ComputingStat cs, long count, double sum,
            double min, double avg, double davg, double max, double stddev) {
        assertEquals(count, cs.getCount());
        assertEquals(sum, cs.getSum());
        assertEquals(min, cs.getMin());
        assertEquals(avg, cs.getMean());
        assertEquals(davg, cs.getDecayAvg());
        assertEquals(max, cs.getMax());
        if(Double.isNaN(stddev)) {
            assertEquals(stddev, cs.getStddev());
        } else {
            assertEquals(stddev, cs.getStddev(), 0.0001);
        }
    }

    public void testComputingStat() {
        ComputingStat cs=new ComputingStat();
        cs.setName("st.test2");
        assertComputingStat(cs, 0, 0, Double.NaN, Double.NaN,
                Double.NaN, Double.NaN, Double.NaN);
        assertEquals("st.test2=compstat: count=0 sum=0.000000 min=NaN "
                + "avg=NaN davg=NaN max=NaN stddev=NaN", cs.toString());

        cs.add(1);
        cs.add(2);
        cs.add(3);
        cs.add(4);
        assertComputingStat(cs, 4, 10d, 1d, 2.5d, 2.2d, 4d, 1.290994d);
        assertEquals("st.test2=compstat: count=4 sum=10.000000 min=1.000000 "
                + "avg=2.500000 davg=2.200000 max=4.000000 stddev=1.290994",
                cs.toString());

        // clear it and do it again
        cs.clear();
        assertComputingStat(cs, 0, 0, Double.NaN, Double.NaN,
                Double.NaN, Double.NaN, Double.NaN);
        // Going to do this one in the other direction, which changes the
        // the decaying average
        cs.add(4);
        cs.add(2);
        cs.add(3);
        cs.add(1);
        assertComputingStat(cs, 4, 10d, 1d, 2.5d, 2.8d, 4d, 1.290994d);
        assertEquals("st.test2=compstat: count=4 sum=10.000000 min=1.000000 "
                + "avg=2.500000 davg=2.800000 max=4.000000 stddev=1.290994",
                cs.toString());
    }

    public void testStats() {
        CounterStat cs=Stats.getCounterStat("st.test1");
        assertEquals("st.test1", cs.getName());
        assertEquals("st.test1=0", String.valueOf(cs));
        assertSame(cs, Stats.getStat("st.test1"));
        assertSame(cs, Stats.getInstance().getStats().get("st.test1"));

        // Can't instantiate broken stats.
        try {
        	Stat x = Stats.getStat("st.broken1", BrokenStat1.class);
        	fail("Expected exception, got " + x);
        } catch(Exception e) {
        	assertEquals("Couldn't create a " + BrokenStat1.class,
        			e.getMessage());
        	assertEquals(Exception.class, e.getCause().getClass());
        	assertEquals("Can't instantiate me", e.getCause().getMessage());
        }
        try {
        	Stat x = Stats.getStat("st.broken2", BrokenStat2.class);
        	fail("Expected exception, got " + x);
        } catch(Exception e) {
        	assertEquals("Couldn't create a " + BrokenStat2.class,
        			e.getMessage());
        	assertSame(IllegalAccessException.class, e.getCause().getClass());
        }
        try {
        	Stat x = Stats.getStat("st.broken3", BrokenStat3.class);
        	fail("Expected exception, got " + x);
        } catch(Exception e) {
        	assertEquals("Objection!", e.getMessage());
        	assertEquals(IllegalArgumentException.class, e.getClass());
        }

        // None of that should be there.
        assertNull(Stats.getStat("st.broken1"));
        assertNull(Stats.getStat("st.broken2"));
        assertNull(Stats.getStat("st.string"));

        try {
            ComputingStat comp=Stats.getComputingStat("st.test1");
            fail("Expected failure to get counter stat as comp stat, got "
                    + comp);
        } catch(AssertionError e) {
            assertEquals("st.test1=0 is not an instance of "
                    + ComputingStat.class, e.getMessage());
        }

        ComputingStat comp=Stats.getComputingStat("compstat.test1");
        assertNotNull(comp);
        assertSame(comp, Stats.getComputingStat("compstat.test1"));
    }

    public static class BrokenStat1 extends Stat {
        public BrokenStat1() throws Exception{
            super();
            throw new Exception("Can't instantiate me");
        }
        @Override
		public String getStat() {
            return(getClass().getName());
        }
    }

    private static class BrokenStat2 extends Stat {
        @Override
		public String getStat() {
            return(getClass().getName());
        }
    }

    public static class BrokenStat3 extends Stat {
        public BrokenStat3() throws Exception{
            super();
            throw new IllegalArgumentException("Objection!");
        }
        @Override
		public String getStat() {
            return(getClass().getName());
        }
    }

}
