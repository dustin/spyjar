// Copyright (c) 1999  Dustin Sallings <dustin@spy.net>
// arch-tag: D100E4D0-1110-11D9-A14B-000A957659CC

package net.spy.net;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.net.URL;
import java.net.URLConnection;

import java.util.Map;
import java.util.List;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.Iterator;

import net.spy.util.SpyUtil;

/**
 * Oversimplified HTTP document fetcher.
 */

// Fetch the contents of a URL
public class HTTPFetch extends Object {
	private URL url;

	private String contents=null;
	private String stripped=null;

	private Map headers=null;

	/** 
	 * Get an HTTPFetch instance for the given URL.
	 * 
	 * @param u the URL to fetch
	 */
	public HTTPFetch(URL u) {
		this(u, null);
	}

	/** 
	 * Get an HTTPFetch instance for the given URL and headers.
	 * 
	 * @param u URL to fetch
	 * @param head Map containing the headers to fetch
	 */
	public HTTPFetch(URL u, Map head) {
		super();
		url=u;
		headers=head;
	}

	/**
	 * Get a vector containing the individual lines of the document
	 * returned from the URL.
	 *
	 * @exception Exception thrown when something fails.
	 */
	public List getLines() throws Exception {
		ArrayList a = new ArrayList();
		getData();

		StringTokenizer st=new StringTokenizer(contents, "\r\n");
		while(st.hasMoreTokens()) {
			a.add(st.nextToken());
		}

		return(a);
	}

	/**
	 * Return the contents of the URL as a whole string.
	 *
	 * @return the contents from the URL as a String
	 * @throws IOException if there is a problem accessing the URL
	 */
	public String getData() throws IOException {
		if(contents==null) {
			StringBuffer sb=new StringBuffer(256);
			BufferedReader br = getReader();
			String line;
			while( (line=br.readLine()) != null) {
				sb.append(line);
				sb.append("\n");
			}
			contents=sb.toString();
		}
		return(contents);
	}

	/**
	 * Return the contents of the URL with the HTML tags stripped out.
	 *
	 * @exception Exception thrown when something fails.
	 */
	public String getStrippedData() throws Exception {
		getData();
		if(stripped==null) {
			stripped=SpyUtil.deHTML(contents);
		}
		return(stripped);
	}

	// Get a reader for the above routines.
	private BufferedReader getReader() throws IOException {
		URLConnection uc = url.openConnection();
		if(headers!=null) {
			for(Iterator i=headers.keySet().iterator(); i.hasNext(); ) {
				String key=(String)i.next();
				String value=(String)headers.get(key);

				uc.setRequestProperty(key, value);
			}
		}
		InputStream i = uc.getInputStream();
		BufferedReader br =
			new BufferedReader( new InputStreamReader(i));
		return(br);
	}

	public static void main(String args[]) throws Exception {
		HTTPFetch hf=new HTTPFetch(new URL(args[0]));
		System.out.println(hf.getStrippedData());
	}
}
