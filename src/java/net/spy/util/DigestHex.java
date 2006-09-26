// Copyright (c)  2006  Dustin Sallings <dustin@spy.net>
// arch-tag: 3B7EC947-2630-463E-96C8-A986ECCAF70E

package net.spy.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import net.spy.SpyObject;

/**
 * Hex digester.
 */
public class DigestHex extends SpyObject {

	private String digestAlg=null;

	public DigestHex(String alg) {
		super();
		digestAlg=alg;
		// Make sure the digest will load
		getDigest();
	}

	/**
	 * Get the type of digest to use for this digest authenticator.
	 */
	protected MessageDigest getDigest() {
		MessageDigest rv=null;
		try {
			rv=MessageDigest.getInstance(digestAlg);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Requested digest not supported", e);
		}
		return rv;
	}

	/**
	 * Get the hex digest of the given string.
	 * 
	 * @param in a string
	 * @return the hex digest of this string
	 */
	public String getHexDigest(String in) {
		MessageDigest dig=getDigest();
		dig.update(in.getBytes());
		return SpyUtil.byteAToHexString(dig.digest());
	}

}