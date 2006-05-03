/*
 * Created on 02.05.2006
 */
package org.knowceans.util;

import java.util.Hashtable;
import java.util.Vector;

/**
 * StopWatch allows to time a java program by simply starting lap-timing,
 * stopping and resetting a stop watch "channel".
 * 
 * @author gregor
 */
public class StopWatch {

    public static void main(String[] args) throws InterruptedException {
        long a = 234345243630300L;
        System.out.println(format(a));
        start();
        Thread.sleep(1200);
        System.out.println(format(read()));
        start("my");
        System.out.println(format(read("my")));
        for (int i = 0; i < 6; i++) {

            Thread.sleep(1400);
            System.out.println(format(lap("my")));
            System.out.println(format(lap()));
            System.out.println(format(read()));
            System.out.println();
        }
        stop("my");
        System.out.println(get("my").debug());

    }

    /**
     * name of the default stopwatch (used with convenience methods)
     */
    private static final String DEFAULT = "_default_";
    // TODO: set invalid to out-of-scope value
    private static final int INVALID = -1;

    /**
     * Starting system time
     */
    private long tstart = INVALID;

    /**
     * keeps the time that this stop watch has been paused.
     */
    private long tpaused = 0;

    /**
     * start of the last lap time
     */
    private long tlapstart = INVALID;

    /**
     * Lap times (stored relative to last start or lap).
     */
    private Vector<Long> laps = null;

    /**
     * Stopping relative time relative to starting time
     */
    private long tstop = INVALID;

    /**
     * Running status (set by start, unset by stop).
     */
    private boolean running = false;

    /**
     * Name of this watch
     */
    private String name;

    /**
     * Manages all stop watches in the current java process.
     */
    protected static Hashtable<String, StopWatch> watches = new Hashtable<String, StopWatch>();

    /**
     * Creates a stop watch with the name. Constructor is protected because
     * direct creation is not part of the concept; subclasses can open up
     * functionality, though.
     * 
     * @param name
     */
    protected StopWatch(String name) {
        laps = new Vector<Long>();
        this.name = name;
    }

    /**
     * Starts the named watch. If the watch existed, the times are kept and the
     * interval that it was paused is subtracted from every interval involving
     * the absolute start time. If it was running, the starting time is simply
     * updated.
     * 
     * @param watch string identifier for that watch.
     * @return current time used as reference for the watch.
     */
    public static synchronized long start(String watch) {

        StopWatch w = watches.get(watch);
        if (w == null) {
            w = new StopWatch(watch);
        }
        w.running = true;
        long t = time();
        w.tstart = t;
        w.tlapstart = t;
        // subtract paused interval
        if (w.tstop > 0) {
            w.tpaused += t - w.tstop;
        }
        watches.put(watch, w);
        return t;
    }

    /**
     * Start the default stop watch.
     * 
     * @return
     */
    public static long start() {
        return start(DEFAULT);
    }

    /**
     * Removes the named stop watch, i.e., gets rid of the data and entry in the
     * watches table. Returns the stop watch just removed to save its data.
     * 
     * @param watch
     */
    public static synchronized StopWatch clear(String watch) {
        return watches.remove(watch);
    }

    /**
     * Reset the default named stop watch.
     */
    public static StopWatch clear() {
        return clear(DEFAULT);
    }

    /**
     * Get the named watch object.
     * 
     * @param watch
     * @return
     */
    public static StopWatch get(String watch) {
        return watches.get(watch);
    }

    /**
     * Get the default watch object.
     * 
     * @param watch
     * @return
     */
    public static StopWatch get() {
        return watches.get(DEFAULT);
    }

    /**
     * Get the time of the named stop watch, relative to the last call to lap or
     * start.
     * 
     * @param watch
     * @return relative time of last lap (or start), or INVALID if unknown or
     *         not running.
     */
    public static synchronized long lap(String watch) {
        long tabs = time();
        long t = tabs;

        StopWatch w = watches.get(watch);
        if (w == null || !w.running) {
            return INVALID;
        }
        t -= w.tlapstart;
        w.tlapstart = tabs;
        w.laps.add(t);
        return t;
    }

    /**
     * Get the time of the default watch, relative to the last call to lap.
     * 
     * @return
     */
    public static long lap() {
        return lap(DEFAULT);
    }

    /**
     * Read the current time the named watch is showing. If it is stopped, it
     * reads the interval stop - start - paused
     * 
     * @param watch
     * @return
     */
    public static synchronized long read(String watch) {
        long t = time();

        StopWatch w = watches.get(watch);
        if (w == null) {
            return INVALID;
        }
        if (w.running) {
            return t - w.tstart - w.tpaused;
        }
        return w.tstop - w.tstart - w.tpaused;
    }

    /**
     * Read the current time the default watch is showing.
     * 
     * @return
     */
    public static long read() {
        return read(DEFAULT);
    }

    /**
     * Get the stopping time of the named watch and reset it.
     * 
     * @param watch
     * @return the relative time since the start.
     */
    public static synchronized long stop(String watch) {
        long t = time();
        StopWatch w = watches.get(watch);
        if (w == null || !w.running) {
            return INVALID;
        }
        w.tstop = t;
        w.running = false;
        return t - w.tstart - w.tpaused;
    }

    /**
     * Get the stopping time of the default watch and reset it.
     * 
     * @return
     */
    public static long stop() {
        return stop(DEFAULT);
    }

    /**
     * Prints a debug string
     * 
     * @return
     */
    public String debug() {
        String s = name + ": ";
        s += running ? " running: " : " stopped: ";
        s += format(read(name));
        s += " lap times: ";
        for (long lap : laps) {
            s += format(lap) + " ";
        }
        return s;
    }

    /**
     * Prints the status of this stop watch, i.e., its name and current reading.
     */
    public String toString() {
        return name + ": " + format(read(name));
    }

    /**
     * Print toString() to stdout.
     * 
     * @param watch
     * @return
     */
    public static void print(String watch) {
        StopWatch w = watches.get(watch);
        if (w == null) {
            System.out.println(watch + " unknown");
        }
        System.out.println(w.toString());
    }

    /**
     * Print toString() of the default watch to stdout.
     * 
     * @param watch
     * @return
     */
    public static void print() {
        print(DEFAULT);
    }

    /**
     * Format a long time string into hh.mm.ss etc.
     * 
     * @param reltime
     * @return
     */
    public synchronized static String format(long reltime) {
        StringBuffer b = new StringBuffer();
        if (reltime == INVALID) {
            return "[invalid]";
        }
        if (reltime < 0) {
            b.append("-");
            reltime = -reltime;
        }
        // TODO: class for this!
        long millis = reltime % 1000;
        reltime -= millis;
        reltime /= 1000;
        long secs = reltime % 60;
        reltime -= secs;
        reltime /= 60;
        long mins = reltime % 60;
        reltime -= mins;
        reltime /= 60;
        if (reltime > 0) {
            b.append(reltime).append("h");
        }
        b.append(digits(mins, 2)).append("'");
        b.append(digits(secs, 2)).append("\"");
        b.append(digits(millis, 3));
        return b.toString();
    }

    /**
     * zero-pad the number on the left to create a string of digits characters.
     * 
     * @param number
     * @param digits
     * @return
     */
    public synchronized static String digits(long number, int digits) {
        String s = Long.toString(number);
        int len = s.length();
        if (len > digits)
            return null;
        if (len < digits) {
            StringBuffer b = new StringBuffer();
            for (int i = 0; i < digits - len; i++) {
                b.append('0');
            }
            b.append(s);
            s = b.toString();
        }
        return s;

    }

    /**
     * Get the system time (might be overwritten by subclasses).
     * <p>
     * TODO: rookie question;-) is static ok for inheritance?
     * 
     * @return
     */
    protected static long time() {
        return System.currentTimeMillis();
    }

    /**
     * Get all watches as a table (can be used to add or remove certain
     * watches).
     * 
     * @return the hashtable of watches
     */
    public static final Hashtable<String, StopWatch> getWatches() {
        return watches;
    }

    public final String getName() {
        return name;
    }

    public final Vector<Long> getLaps() {
        return laps;
    }
}
