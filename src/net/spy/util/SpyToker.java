/*
 * Copyright (c) 1999 Dustin Sallings
 *
 * arch-tag: 424C5378-1110-11D9-B04F-000A957659CC
 */

package net.spy.util;

import java.io.File;
import java.io.IOException;

import java.util.Map;

/**
 * A simple token in-plugger.
 * <p>
 * Input files are in any textual format, with tokens in the format of
 * %TOKEN% where TOKEN is a valid token that can be found in the hash
 * passed in to the tokenizer.
 */
public class SpyToker extends Object {

	/** 
	 * Construct a SpyToker.
	 */
	public SpyToker() {
		super();
	}

	/** 
	 * Tokenize a String, return the tokenized results.
	 * 
	 * @param input the input string
	 * @param m the map of tokens (without percents)
	 * @return the tokenized string
	 */
	public String tokenizeString(String input, Map m) {
		StringBuffer output=new StringBuffer(input.length() + 256);
		int which;

		while((which=input.indexOf('%')) >= 0) {
			output.append(input.substring(0, which));
			input=input.substring(which+1);

			which=input.indexOf('%');
			if(which>=0) {
				String v=null;
				String tmp = input.substring(0, which);
				input = input.substring(which+1);
				if((v=(String)m.get(tmp)) != null) {
					output.append(v);
				} else {
					output.append("%" + tmp + "%");
				}
			} else {
				output.append("%");
			}
		}
		output.append(input);
		return(output.toString());
	}

	/**
	 * Tokenize a file.
	 *
	 * @param file file to tokenize
	 * @param p Map whose entries will be looked up to fill in the
	 * tokens in the text file.
	 *
	 * @return tokenized data from the file.
	 */
	public String tokenize(File file, Map m) {
		String input=null;

		// Get our mofo data.
		try {
			input=SpyUtil.getFileData(file);
		} catch (IOException e) {
			return(null);
		}
		return(tokenizeString(input, m));
	}
}
