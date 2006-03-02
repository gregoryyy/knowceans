/*
 * Created on 02.03.2006
 */
package org.knowceans.util;

import java.util.Calendar;
import java.util.Date;

/**
 * TimeIntervals provides methods to find the time intervals (week, month, year)
 * surrounding a given date. Intervals are expressed as 2-arrays of Date object,
 * corresponding to the start and end times. By convention, intervals include
 * their bounds.
 * 
 * @author gregor
 */
public class TimeIntervals {

    public static void main(String[] args) {
        Date now = new Date();
        // this week
        Date[] interval = TimeIntervals.weekOf(now);
        System.out.println("this week:\t" + interval[0] + " -- " + interval[1]);
        // next week
        interval = TimeIntervals.weeksAway(now, 1);
        System.out.println("next week:\t" + interval[0] + " -- " + interval[1]);

        // this month
        interval = TimeIntervals.monthOf(now);
        System.out
            .println("this month:\t" + interval[0] + " -- " + interval[1]);
        // next month
        interval = TimeIntervals.monthsAway(now, 1);
        System.out
            .println("next month:\t" + interval[0] + " -- " + interval[1]);

        // this year
        interval = TimeIntervals.yearOf(now);
        System.out.println("this year:\t" + interval[0] + " -- " + interval[1]);
        // next year
        Date[] interval2 = TimeIntervals.yearsAway(now, 1);
        System.out.println("next year:\t" + interval[0] + " -- " + interval[1]);
        
        interval = TimeIntervals.join(interval, interval2);
        // this and next year
        System.out.println("this+next year:\t" + interval[0] + " -- " + interval[1]);

    }

    /**
     * returns the start and end times of the week surrounding the date d.
     * 
     * @param d
     * @return
     */
    public static Date[] weekOf(Date d) {
        return weeksAway(d, 0);

    }

    /**
     * returns the start and end times of the month surrounding the date d.
     * 
     * @param d
     * @return
     */
    public static Date[] monthOf(Date d) {
        return monthsAway(d, 0);
    }

    /**
     * returns the start and end times of the year surrounding the date d.
     * 
     * @param d
     * @return
     */
    public static Date[] yearOf(Date d) {
        return yearsAway(d, 0);
    }

    /**
     * returns the start and end times of the week before / after the start of
     * the week surrounding the date d.
     * 
     * @param d
     * @param away
     * @return
     */
    public static Date[] weeksAway(Date d, int away) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        Calendar start = Calendar.getInstance();
        start.clear();
        start.setLenient(true);
        start.set(Calendar.YEAR, c.get(Calendar.YEAR));
        start.set(Calendar.WEEK_OF_YEAR, c.get(Calendar.WEEK_OF_YEAR) + away);
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.WEEK_OF_YEAR, 1);
        end.add(Calendar.MILLISECOND, -1);
        return new Date[] {start.getTime(), end.getTime()};
    }

    /**
     * returns the start and end times of the week before / after the start of
     * the week surrounding the date d.
     * 
     * @param d
     * @param away
     * @return
     */
    public static Date[] monthsAway(Date d, int away) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        Calendar start = Calendar.getInstance();
        start.clear();
        start.setLenient(true);
        start.set(Calendar.YEAR, c.get(Calendar.YEAR));
        start.set(Calendar.MONTH, c.get(Calendar.MONTH) + away);
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.MONTH, 1);
        end.add(Calendar.MILLISECOND, -1);
        return new Date[] {start.getTime(), end.getTime()};

    }

    /**
     * returns the start and end times of the week before / after the start of
     * the week surrounding the date d.
     * 
     * @param d
     * @param away
     * @return
     */
    public static Date[] yearsAway(Date d, int away) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        Calendar start = Calendar.getInstance();
        start.clear();
        start.setLenient(true);
        start.set(Calendar.YEAR, c.get(Calendar.YEAR) + away);
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.YEAR, 1);
        end.add(Calendar.MILLISECOND, -1);
        return new Date[] {start.getTime(), end.getTime()};

    }

    /**
     * check whether time is within the interval.
     * 
     * @param time
     * @param interval
     * @return
     */
    public static boolean isIn(Date time, Date[] interval) {
        return !time.before(interval[0]) && !time.after(interval[1]);
    }

    /**
     * joins the limits of the two intervals by taking the start of before and
     * the end of after.
     * 
     * @param before
     * @param after
     * @return
     */
    public static Date[] join(Date[] before, Date[] after) {
        return new Date[] {before[0], after[1]};
    }

}
