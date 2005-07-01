// Copyright (c) 1999  Dustin Sallings <dustin@spy.net>
// arch-tag: D100E4D0-1110-11D9-A14B-000A957659CC

package net.spy.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.spy.util.SpyUtil;

/**
 * Oversimplified HTTP document fetcher.
 */

// Fetch the contents of a URL
public class HTTPFetch extends Object {
	private URL url;

	private String contents=null;
	private String stripped=null;

	private Map<String, List<String>> headers=null;
	private long ifModifiedSince=0;

	private int status=0;
	private long lastModified=0;
	private Map<String, List<String>> responseHeaders=null;

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
	public HTTPFetch(URL u, Map<String, List<String>> head) {
		super();
		url=u;
		headers=head;
	}

	/** 
	 * Get the response headers from the request (will force a content fetch).
	 */
	public Map<String, List<String>> getResponseHeaders() throws IOException {
		getData();
		return(responseHeaders);
	}

	/** 
	 * Set the ifModifiedSince value for the request.
	 */
	public void setIfModifiedSince(long to) {
		ifModifiedSince=to;
	}

	/** 
	 * Get the HTTP status from this request.
	 */
	public int getStatus() throws IOException {
		getData();
		return(status);
	}

	/** 
	 * Get the last modified date of this response.
	 */
	public long getLastModified() throws IOException {
		getData();
		return(lastModified);
	}

	/**
	 * Get a vector containing the individual lines of the document
	 * returned from the URL.
	 *
	 * @exception Exception thrown when something fails.
	 */
	public List<String> getLines() throws IOException {
		ArrayList<String> a = new ArrayList<String>();

		StringTokenizer st=new StringTokenizer(getData(), "\r\n");
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
		HttpURLConnection uc = (HttpURLConnection)url.openConnection();
		if(headers!=null) {
			for(Map.Entry<String, List<String>> me: headers.entrySet()) {
				for(String val : me.getValue()) {
					uc.setRequestProperty(me.getKey(), val);
				}
			}
		}
		// Set the ifModifiedSince if we have one
		if(ifModifiedSince > 0) {
			uc.setIfModifiedSince(ifModifiedSince);
		}
		InputStream i = uc.getInputStream();
		// Collect some data about this request
		status=uc.getResponseCode();
		responseHeaders=new HashMap<String, List<String>>(uc.getHeaderFields());
		lastModified=uc.getLastModified();

		BufferedReader br =
			new BufferedReader( new InputStreamReader(i));
		return(br);
	}

	public static void main(String args[]) throws Exception {
		HTTPFetch hf=new HTTPFetch(new URL(args[0]));
		System.out.println(hf.getStrippedData());
	}
}
