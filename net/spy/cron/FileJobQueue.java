// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: FileJobQueue.java,v 1.2 2002/10/08 21:44:47 dustin Exp $

package net.spy.cron;

import java.io.File;
import java.io.Reader;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;

import net.spy.SpyUtil;

/**
 * Get a job queue as defined in a file.  The file will be in the following
 * format:
 *
 * <p>
 *
 * <pre>
 * YYYYMMDD-HHMMSS calfield calincrement classname args ...
 * </pre>
 *
 * <p>
 *
 * For example:
 *
 * <pre>
 * 20010403-090000 DAY 1 net.spy.pagermusic.RunSubs
 * </pre>
 */
public class FileJobQueue extends JobQueue {

	/**
	 * Get a new FileJobQueue based on what's in the given file.
	 * @param f the file to read
	 * @throws IOException if there's a problem reading the jobs
	 */
	public FileJobQueue(File f) throws IOException {
		super();

		FileReader fr=new FileReader(f);
		initQueue(fr);
		fr.close();
	}

	/** 
	 * Get a new FileJobQueue from a Reader.
	 * 
	 * @param r the reader
	 * @throws IOException if there's a problem reading the jobs
	 */
	public FileJobQueue(Reader r) throws IOException {
		super();

		initQueue(r);
	}

	// Init the job queue.
	private void initQueue(Reader r) throws IOException {
		LineNumberReader lnr=new LineNumberReader(r);

		String line=lnr.readLine();
		while(line!=null) {

			try {
				Job j=parseJob(line, lnr.getLineNumber());
				if(j!=null) {
					addJob(j);
				}
			} catch(Exception e) {
				System.err.println("Error parsing line "
					+ lnr.getLineNumber() + ":  " + e);
				e.printStackTrace();
			}

			line=lnr.readLine();
		}

		lnr.close();
	}

	// Parse an individual line from the job file.
	private Job parseJob(String line, int lineNum) throws ParseException {
		Job rv=null;

		line=line.trim();

		// Ignore comments.
		if(line.startsWith("#")) {
			return(null);
		}

		// Ignore empty lines.
		if(line.length() < 1) {
			return(null);
		}

		String stuff[]=SpyUtil.split(" ", line);
		String dateS=stuff[0];
		String fieldS=stuff[1];
		String incrS=stuff[2];
		String classS=stuff[3];

		String args[]=new String[stuff.length-4];
		// If there were args, copy them in instead.
		if(stuff.length>4) {
			System.arraycopy(stuff, 4, args, 0, args.length);
		}

		SimpleDateFormat df=new SimpleDateFormat("yyyyMMdd-hhmmss");
		Date startDate=df.parse(dateS);

		int cf=parseCalendarField(fieldS);
		if(cf>=0) {
			TimeIncrement ti=new TimeIncrement();
			ti.setField(cf);
			ti.setIncrement(Integer.parseInt(incrS));
			// Get the next start date using the given increment, otherwise
			// the job will run *right now*.
			startDate=ti.nextDate(startDate);

			rv=new MainJob(classS, args, startDate, ti);
		} else {
			if(startDate.getTime() < System.currentTimeMillis()) {
				System.err.println("At job on line " + lineNum
					+ " is in the past.");
			} else {
				rv=new MainJob(classS, args, startDate);
			}
		}

		return(rv);
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
			System.err.println("WARNING!  " + fieldName
				+ " is not a valid Calendar field.");
		}

		return(rv);
	}

}
