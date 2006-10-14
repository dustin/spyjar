// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: D00F3608-DAC5-4E2B-8211-6A218895EE83

package net.spy.stat;

import static java.lang.Double.NaN;

/**
 * Stat implementation that keeps averages and standard deviation and stuff.
 * 
 * Basically stole from Scott Lamb's Axomol library.
 */
public class ComputingStat extends Stat {

    private int n=0;
    private double mean=NaN, variance=NaN, min=NaN, max=NaN, decayAvg=NaN;
    private double sum=0;

    public ComputingStat() {
        super();
    }   

    /**
     * Clear the stats that have been accumulated so far.
     */ 
    public synchronized void clear() {
        n=0;
        mean = variance = min = max = decayAvg = NaN;
        sum=0;
    }

    /**
     * Add the given value to the current stat.
     *  
     * @param value the value
     */
    public synchronized void add(double value) {
        if (++n == 1) {
            // Special case the first sample.
            mean = decayAvg = min = max = value;
            variance = 0;
        } else {
            double newmean = mean + (value - mean) / n;
            variance += (value - mean) * (value - newmean);
            mean = newmean;
            decayAvg = (decayAvg * n + value) / (n + 1);
            if (value < min) {
                min = value;
            }
            if (value > max) {
                max = value;
            }
        }
        sum += value;
    }   

    /**
     * Compute the standard deviation of the current series.
     * 
     * @return the stddev or NaN if there aren't enough samples yet
     */
    public synchronized double getStddev() {
        if (n < 2) {
            return Double.NaN;
        }
        return Math.sqrt(variance / (n-1));
    }

    /**
     * Get the decaying average of this series.
     * 
     * @return the decaying average, or NaN if there aren't enough samples yet
     */
    public synchronized double getDecayAvg() {
        return decayAvg;
    }

    public synchronized String getStat() {
        return String.format(
            "compstat: count=%d sum=%f min=%f avg=%f davg=%f max=%f stddev=%f",
            n, sum, min, mean, decayAvg, max, getStddev());
    }

    /**
     * Get the maximum value seen in this stat.
     */
    public synchronized double getMax() {
        return max;
    }

    /**
     * Get the average value seen in this stat.
     */
    public synchronized double getMean() {
        return mean;
    }

    /**
     * Get the minimum value seen in this stat.
     */
    public synchronized double getMin() {
        return min;
    }

    /**
     * Get the number of times a value has been added to this stat.
     */
    public synchronized int getCount() {
        return n;
    }

    /**
     * Get the sum of all values added to this stat.
     */
    public synchronized double getSum() {
        return sum;
    }

}
