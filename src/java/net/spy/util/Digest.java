// Copyright (c) 2001  Beyond.com <dustin@beyond.com>
//
// arch-tag: 82074898-1110-11D9-9C0E-000A957659CC

package net.spy.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import net.spy.SpyObject;

/**
 * Digest for getting checksums, hashing passwords, stuff like that.
 */
public class Digest extends SpyObject {

	private static final String DEFAULTHASH="SHA";
	private boolean prefixHash=true;

	private String hashAlg=null;
	private int digLen=0;

	/**
	 * Get a new Digest object.
	 */
	public Digest() {
		this(DEFAULTHASH);
	}

	/**
	 * Get a Digest object with the given hash algorithm.
	 */
	public Digest(String alg) {
		super();
		hashAlg=alg;
		MessageDigest d=getMessageDigest();
		digLen=d.getDigestLength();
		assert digLen != 0 : "Couldn't calculate digest length";
	}

	/**
	 * Get the hash type to be used by this password digest thing.
	 */
	public String getHashAlg() {
		return hashAlg;
	}

	/** 
	 * If set to true, hashes will be prefixed with the type of hash used.
	 * (i.e. {SHA}, {SSHA}, etc...).
	 * 
	 * @param doit whether or not to prefix the hash
	 */
	public void prefixHash(boolean doit) {
		prefixHash=doit;
	}

	// SSHA = 20 bytes of SHA data + a random salt.  Perl for checking can
	// be found here:
	// http://developer.netscape.com/docs/technote/ldap/pass_sha.html

	/**
	 * Check a plaintext password against a hashed password.
	 */
	public boolean checkPassword(String pw, String hash) {
		boolean rv=false;

		String htype=hash.substring(0,
			hash.indexOf('}')+1).toUpperCase();
		if(htype.equals("{" + hashAlg + "}")
				|| htype.equals("{S" + hashAlg + "}")) {
			String data=hash.substring(hash.indexOf('}')+1);

			Base64 base64d=new Base64();
			byte datab[]=base64d.decode(data);
			byte salt[]=new byte[datab.length-digLen];
			System.arraycopy(datab, digLen, salt, 0, salt.length);
			String newhash=getHash(pw, salt);

			rv=hash.equals(newhash);
		} else {
			getLogger().warn("Invalid hash type ``%s'' in %s", htype, hash);
		}

		return(rv);
	}

	private String getPrefix(String p) {
		String rv="";
		if(prefixHash) {
			rv=p;
		}
		return(rv);
	}

	/**
	 * Get a hash for a String with a known salt.  This should only be used
	 * for verification, don't be stupid and start handing out words with
	 * static salts.
	 */
	protected String getHash(String word, byte salt[]) {
		MessageDigest md = getMessageDigest();
		md.update(word.getBytes());
		md.update(salt);
		byte pwhash[]=md.digest();
		String hout = getPrefix("{S" + hashAlg + "}")
			+ Base64.getInstance().encode(cat(pwhash, salt));
		return(hout.trim());
	}

	private MessageDigest getMessageDigest() {
		MessageDigest md=null;
		try {
			md=MessageDigest.getInstance(hashAlg);
		} catch(NoSuchAlgorithmException e) {
			throw new RuntimeException("No such digest:  " + hashAlg, e);
		}
		return md;
	}

	/** 
	 * Get the hash for a given string (with no salt).
	 * 
	 * @param s the thing to hash
	 * @return the hash
	 */
	public byte[] getSaltFreeHashBytes(String s) {
		MessageDigest md = getMessageDigest();
		md.update(s.getBytes());
		byte hash[]=md.digest();
		return(hash);
	}

	/**
	 * Get a hash for a String with no salt.  This should only be used for
	 * checksumming, not passwords.
	 */
	public String getSaltFreeHash(String s) {
		String hout = getPrefix("{" + hashAlg + "}")
			+ Base64.getInstance().encode(getSaltFreeHashBytes(s));
		return(hout);
	}

	/**
	 * Get a hash for a given String.
	 */
	public String getHash(String word) {
		// 8 bytes of salt should be enough sodium for anyone
		byte salt[]=new byte[8];
		SecureRandom sr=new SecureRandom();
		sr.nextBytes(salt);

		return(getHash(word, salt));

	}

	private byte[] cat(byte a[], byte b[]) {
		byte r[]= new byte [a.length + b.length];
		System.arraycopy (a, 0, r, 0, a.length);
		System.arraycopy (b, 0, r, a.length, b.length);
		return(r);
	}

}
