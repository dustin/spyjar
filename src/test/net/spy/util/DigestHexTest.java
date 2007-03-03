// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>
// arch-tag: 03DC7993-75A6-4B48-AB5B-A6E879144DC1

package net.spy.util;

import net.spy.test.BaseMockCase;

/**
 * Test the DigestHex class.
 */
public class DigestHexTest extends BaseMockCase {

	public void testSimpleMD5() throws Exception {
		DigestHex dh=new DigestHex("MD5");
		assertEquals("04d0336a64806a7d94e9055d2bfe409a",
				dh.getHexDigest("dustin"));
	}

	public void testSimpleSha() throws Exception {
		DigestHex dh=new DigestHex("SHA-1");
		assertEquals("1c08efb9b3965701be9d700d9a6f481f1ffec3ea",
				dh.getHexDigest("dustin"));
	}

	public void testBadDigest() throws Exception {
		try {
			DigestHex dh=new DigestHex("DustinHash");
			fail("Expected digest to not exist, got " + dh);
		} catch(RuntimeException e) {
			assertEquals("Requested digest not supported", e.getMessage());
		}
	}
}
