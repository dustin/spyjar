// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>

package net.spy.util;

import java.util.HashSet;

import junit.framework.TestCase;

/**
 * Test the digest imlementation and password generator.
 */
public class DigestTest extends TestCase {

	/**
	 * A basic test of the password generator.  Ensure the password
	 * generator won't generate the same password if called several times.
	 */
	public void testPasswordGenerator() {
		HashSet<String> words=new HashSet<String>();

		for(int i=0; i<1000; i++) {
			String pw=PwGen.getPass(8);
			assertTrue("Generated a duplicate password on attempt " + i,
				(!words.contains(pw)));
			words.add(pw);
		}
	}

	/**
	 * Test the password hashing.  Do a couple rounds of passwords and make
	 * sure the hashing consistently works.
	 */
	public void testPasswordHashSHA() {
		Digest d=new Digest();
		assertEquals("SHA", d.getHashAlg());

		for(int i=0; i<10; i++) {
			String pw=PwGen.getPass(8);
			String hpw=d.getHash(pw);
			assertTrue("Password checking failed", d.checkPassword(pw, hpw));
		}
	}

	/**
	 * Test the password hashing.  Do a couple rounds of passwords and make
	 * sure the hashing consistently works.
	 */
	public void testPasswordHashMD5() {
		Digest d=new Digest("MD5");
		assertEquals("MD5", d.getHashAlg());

		for(int i=0; i<10; i++) {
			String pw=PwGen.getPass(8);
			String hpw=d.getHash(pw);
			assertTrue("Password checking failed", d.checkPassword(pw, hpw));
		}
	}

	/**
	 * Test salt-free hashes.
	 */
	public void testSaltFree() throws Exception {
		Digest d=new Digest();
		assertEquals("{SHA}qUqP5cyxm6YcTAhz05Hph5gvu9M=",
			d.getSaltFreeHash("test"));
		d.prefixHash(false);
		assertEquals("qUqP5cyxm6YcTAhz05Hph5gvu9M=", d.getSaltFreeHash("test"));
	}

	public void testUnknownHash() throws Exception {
		String pw="DR4TQS96";
		String noprefixSalted="d0f4wGSDm5EHbLcbyPUEbKwrxj9bHFvM1dAzrw==";
		String noprefixUnsalted="ErIKWQQHaOgcy1G+qpuoMUshEEo=";
		assertFalse("Default digest shouldn't work (salted)",
				new Digest().checkPassword(pw, noprefixSalted));
		assertFalse("Default digest shouldn't work (unsalted)",
				new Digest().checkPassword(pw, noprefixUnsalted));
	}

	public void testBadHash() throws Exception {
		try {
			Digest d=new Digest("BadAssHash");
			fail("Didn't expect to be able to make a BadAssHash:  " + d);
		} catch(RuntimeException e) {
			assertEquals("No such digest:  BadAssHash", e.getMessage());
		}
	}
}
