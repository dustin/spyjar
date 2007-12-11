// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.stat;

import net.spy.SpyObject;

/**
 * Base class for all stats.
 */
public abstract class Stat extends SpyObject {

    private String name=null;

    /**
     * Get an instance of Stat.
     */
    public Stat() {
        super();
    }

    /**
     * Get the name of this stat.
     */
    public String getName() {
        return(name);
    }

    /**
     * Set the name of this stat.
     *
     * @param to
     */
    public void setName(String to) {
        this.name=to;
    }

    /**
     * getName()=getStat()
     */
    @Override
	public String toString() {
        return getName() + "=" + getStat();
    }

    /**
     * Get the current value of this stat as a String.
     */
    public abstract String getStat();

}
