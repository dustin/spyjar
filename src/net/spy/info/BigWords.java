// Copyright (c) 2000  Dustin Sallings <dustin@spy.net>
//
// arch-tag: 6F5A02FF-1110-11D9-BFE9-000A957659CC

package net.spy.info;

import java.net.URL;

import java.util.Hashtable;

import net.spy.SpyUtil;

import net.spy.net.HTTPFetch;

/**
 * Look up a book by ISBN from BigWords.com.
 */
public class BigWords extends Info {

	/**
	 * Get a BigWords object.
	 *
	 * @param isbn The ISBN of the book we want to look at.
	 */
	public BigWords(String isbn) {
		super();
		this.arg = isbn;
	}

	/**
	 * Get an unitialized Traffic object.
	 */
	public BigWords() {
		super();
	}

	public String toString() {
		String ret="";
		try {
			get("ERROR"); // get the juices flowing
			if(error) {
				ret=get("ERROR");
			} else {
				ret=get("title") + " by " + get("author")
					+ " - Format: " + get("format") + " - "
					+ "Used: " + get("used_price")
						+ " (" + get("used_availability") + "), "
					+ "New: " + get("new_price")
						+ " (" + get("new_availability") + ")";
			}
		} catch(Exception e) {
			getLogger().warn("BigWords.toString() exception", e);
		}
		return(ret);
	}

	protected void parseInfo() throws Exception {
		hinfo=new Hashtable();
		hinfo.put("isbn", arg);
		getInfo();
		String lines[]=SpyUtil.split("\n", info);
		int section=0;
		String localInfo = "";
		for(int i=0; i<lines.length; i++) {
			if(lines[i].startsWith("BOOK ZOOM")) {
				i++;
				hinfo.put("title", lines[i]);
				i++;
				hinfo.put("author", lines[i].substring(3));
			} else if(lines[i].startsWith("NEW:")) {
				i++;
				hinfo.put("new_price", lines[i]);
				i++;
				hinfo.put("new_availability", lines[i].substring(11));
			} else if(lines[i].startsWith("USED:")) {
				i++;
				hinfo.put("used_price", lines[i]);
				i++;
				hinfo.put("used_availability", lines[i].substring(11));
			} else if(lines[i].startsWith("FORMAT")) {
				String stuff[]=SpyUtil.split(":", lines[i]);
				hinfo.put("format", stuff[1].trim());
			} else if(lines[i].startsWith("PUBLISHER")) {
				String stuff[]=SpyUtil.split(":", lines[i]);
				hinfo.put("publisher", stuff[1].trim());
			} else if(lines[i].startsWith("PUBLISHED")) {
				String stuff[]=SpyUtil.split(":", lines[i]);
				hinfo.put("publish_date", stuff[1].trim());
			}
		}
		if(get("publish_date")==null) {
			error=true;
		} else {
			error=false;
		}
		if(error) {
			String errorString="Unable to get Book info.  "
				+ "Invalid or unknown ISBN?";
			hinfo.put("ERROR", errorString);
		} else {
			localInfo=localInfo.trim();
			hinfo.put("info", localInfo);
		}
	} // if there's a need to find it at all.


	protected void getInfo() throws Exception {
		if(info==null) {
			String url=
				"http://bigwords.com/search/index.cfm?"
					+ "BIGVERB=book%20zoom&ISBN="
					+ arg;
			hinfo.put("URL", url);
			URL u=new URL(url);
			HTTPFetch f = new HTTPFetch(u);
			info=f.getStrippedData();
		}
	}

	public static void main(String args[]) throws Exception {
		BigWords b = new BigWords(args[0]);
		System.out.println("Info:\n" + b);
		System.out.println("Info (XML):\n" + b.toXML());
	}
}
