// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 6283078E-1110-11D9-9516-000A957659CC

package net.spy.cron;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import net.spy.util.CloseUtil;
import net.spy.util.SpyUtil;

/**
 * Get a job queue as defined in a file.  The file will be in one the following
 * formats:
 *
 * <p>
 *
 * <ul>
 *   <li>YYYYMMDD-HHMMSS calfield calincrement classname args ...</li>
 *   <li>HHMMSS calfield calincrement classname args ...</li>
 *   <li>MMSS calfield calincrement classname args ...</li>
 * </ul>
 *
 * There are also special times that may be provided for less specific start
 * times:
 *
 * <ul>
 *   <li>NOW - start immediately (or close to it) after startup, cycle from
 *   	that time</li>
 *   <li>NEXT - similar to NOW, but skip the current startup.  Each cycle
 *   	will be based on the startup time, but the first run will happen after
 *   	the delay finishes.</li>
 * </ul>
 *
 * In a case where fields are not provided, the current time will be
 * substituted.
 *
 * <p>
 *
 * For example:
 *
 * <pre>
 * 20010403-090000 DAY 1 net.spy.pagermusic.RunSubs
 * </pre>
 */
public class FileJobQueue extends JobQueue<MainJob> {

	private ClassLoader classLoader;

	/**
	 * Get a new FileJobQueue based on what's in the given file.
	 * @param cl the class loader
	 * @param f the file to read
	 * @throws IOException if there's a problem reading the jobs
	 */
	public FileJobQueue(ClassLoader cl, File f) throws IOException {
		super();

		classLoader=cl;
		FileReader fr=null;
		try {
			fr=new FileReader(f);
			initQueue(fr);
		} finally {
			CloseUtil.close(fr);
		}
	}

	/** 
	 * Get a FileJobQueue from a file using the current classloader.
	 * @param f the file
	 */
	public FileJobQueue(File f) throws IOException {
		this(null, f);
		classLoader=getClass().getClassLoader();
	}

	/** 
	 * Get a new FileJobQueue from a Reader.
	 * 
	 * @param cl the classloader
	 * @param r the reader
	 * @throws IOException if there's a problem reading the jobs
	 */
	public FileJobQueue(ClassLoader cl, Reader r) throws IOException {
		super();

		classLoader=cl;
		initQueue(r);
	}

	/** 
	 * Get a FileJobQueue from a reader using the current classloader.
	 * @param r the reader
	 */
	public FileJobQueue(Reader r) throws IOException {
		this(null, r);
		classLoader = getClass().getClassLoader();
	}

	// Init the job queue.
	private void initQueue(Reader r) throws IOException {
		LineNumberReader lnr=new LineNumberReader(r);

		String line=lnr.readLine();
		while(line!=null) {

			try {
				MainJob j=parseJob(line, lnr.getLineNumber());
				if(j!=null) {
					addJob(j);
					getLogger().info("Added job %s to start at %s",
							j, j.getStartTime());
				}
			} catch(Exception e) {
				getLogger().warn("Error parsing line %s",
						lnr.getLineNumber(), e);
			}

			line=lnr.readLine();
		}

		lnr.close();
	}

	// Parse an individual line from the job file.
	private MainJob parseJob(String line, int lineNum) throws ParseException {
		MainJob rv=null;

		line=line.trim();

		// Ignore comments.
		if(line.startsWith("#")) {
			return(null);
		}

		// Ignore empty lines.
		if(line.length() < 1) {
			return(null);
		}

		String[] stuff=SpyUtil.split(" ", line);
		String dateS=stuff[0];
		String fieldS=stuff[1];
		String incrS=stuff[2];
		String classS=stuff[3];

		String[] args=new String[stuff.length-4];
		// If there were args, copy them in instead.
		if(stuff.length>4) {
			System.arraycopy(stuff, 4, args, 0, args.length);
		}

		Date startDate=parseStartDate(dateS);

		int cf=parseCalendarField(fieldS);
		if(cf>=0) {
			TimeIncrement ti=new TimeIncrement();
			ti.setField(cf);
			ti.setIncrement(Integer.parseInt(incrS));
			// Get the next start date using the given increment, otherwise
			// the job will run *right now*.
			startDate=ti.nextDate(startDate);

			rv=new MainJob(classLoader, classS, args, startDate, ti);
		} else {
			if(startDate.getTime() < System.currentTimeMillis()) {
				getLogger().warn("At job on line %d is in the past.", lineNum);
			} else {
				rv=new MainJob(classLoader, classS, args, startDate);
			}
		}

		return(rv);
	}

	// Parse the date
	private Date parseStartDate(String in) throws ParseException {
		Date rv=null;

		long now=System.currentTimeMillis();

		// Special cases
		if(in.equals("NOW")) {
			// Schedule it for a minute from now, which should make it as
			// close to now as anyone should care about
			rv=new Date(now+60000);
		} else if(in.equals("NEXT")) {
			// Make sure it's in the past.
			rv=new Date(now-1);
		} else {

			// Flip through all of the known formats until we get a match
			for(TimeFormat tf : getFormats()) {
				SimpleDateFormat sdf=tf.getFormat();
				try {
					Date d=sdf.parse(in);
					rv=copyDate(d, tf.getFields());
				} catch(ParseException e) {
					// Nope, try the next
				}
			}
		}

		if(rv==null) {
			throw new ParseException("Could not parse date:  " + in, 0);
		}

		return (rv);
	}

	// These are the supported formats, and the fields we should remember
	// from them.
	private Collection<TimeFormat> getFormats() {
		Collection<TimeFormat> rv=new ArrayList<TimeFormat>();

		int[] f1={Calendar.YEAR, Calendar.MONTH, Calendar.DAY_OF_MONTH,
					Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND};
		rv.add(new TimeFormat("yyyyMMdd-HHmmss", f1));

		int[] f2={Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND};
		rv.add(new TimeFormat("HHmmss", f2));

		int[] f3={Calendar.MINUTE, Calendar.SECOND};
		rv.add(new TimeFormat("mmss", f3));

		return (rv);
	}

	// Create a new Date using the current time, and then copy all of the
	// fields of the passed in date that are specified in the given fields
	// array.
	private Date copyDate(Date d, int fields[]) {
		Calendar oldc=Calendar.getInstance();
		oldc.setTime(d);
		Calendar newc=Calendar.getInstance();

		// First, zero out the milliseconds.
		newc.set(Calendar.MILLISECOND, 0);
		// Copy the defined fields.
		for(int i=0; i<fields.length; i++) {
			newc.set(fields[i], oldc.get(fields[i]));
		}

		// Return the new time
		return(newc.getTime());
	}

	private int parseCalendarField(String fieldName) {
		int rv=-1;

		if(fieldName.equals("YEAR")) {
			rv=Calendar.YEAR;
		} else if(fieldName.equals("MONTH")) {
			rv=Calendar.MONTH;
		} else if(fieldName.equals("DAY")) {
			rv=Calendar.DAY_OF_MONTH;
		} else if(fieldName.equals("HOUR")) {
			rv=Calendar.HOUR;
		} else if(fieldName.equals("MINUTE")) {
			rv=Calendar.MINUTE;
		} else if(fieldName.equals("SECOND")) {
			rv=Calendar.SECOND;
		} else if(fieldName.equals("ONCE")) {
			// This is an ``at'' job
			rv=-1;
		} else {
			getLogger().warn(fieldName + " is not a valid Calendar field.");
		}

		return(rv);
	}

	static final class TimeFormat extends Object {
		private SimpleDateFormat format=null;
		private int[] fields=null;

		TimeFormat(String fmt, int flds[]) {
			super();
			this.format=new SimpleDateFormat(fmt);
			this.format.setLenient(false);
			this.fields=flds;
		}

		/** 
		 * Get the format this thing is holding.
		 */
		SimpleDateFormat getFormat() {
			return(format);
		}
		
		/** 
		 * Get the java.util.Calendar fields that should be preserved.
		 */
		int[] getFields() {
			return(fields);
		}
	}

}
