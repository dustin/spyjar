// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: F7443815-4B5C-4258-A714-8F89F6E516F2

package net.spy.stat;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple counter stat.
 */
public class CounterStat extends Stat {

    private final AtomicLong count;

    /**
     * Get an instance of CounterStat.
     */
    public CounterStat() {
        super();
        count=new AtomicLong();
    }

    /** 
     * Increment the counter.
     */
    public void increment() {
        count.incrementAndGet();
    }

    /** 
     * Increment the counter a specific amount.
     * 
     * @param howmuch how much to increment the counter
     */
    public void increment(long howmuch) {
        count.addAndGet(howmuch);
    }

    /** 
     * Get the current count for this counter.
     */
    public long getCount() {
        return(count.longValue());
    }

    /** 
     * Get the count as a string.
     */
    @Override
	public String getStat() {
        return(String.valueOf(count));
    }

    /** 
     * Set the absolute value of this counter.
     */
    public void setValue(long to) {
        count.set(to);
    }

}
