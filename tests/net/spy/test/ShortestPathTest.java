// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: ShortestPathTest.java,v 1.3 2002/10/19 09:37:16 dustin Exp $

package net.spy.test;

import java.lang.ref.WeakReference;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.Iterator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import net.spy.util.SPNode;
import net.spy.util.AbstractSPNode;
import net.spy.util.SPVertex;
import net.spy.util.ShortestPathFinder;
import net.spy.util.ShortestPath;
import net.spy.util.NoPathException;

/**
 * Test a weak hash set.
 */
public class ShortestPathTest extends TestCase {

	private Map nodes=null;
	private StringNode a=null;
	private StringNode b=null;
	private StringNode c=null;
	private StringNode d=null;
	private StringNode e=null;

	/**
	 * Get an instance of ShortestPathTest.
	 */
	public ShortestPathTest(String name) {
		super(name);
	}

	/** 
	 * Get the suite.
	 */
	public static Test suite() {
		return new TestSuite(ShortestPathTest.class);
	}

	/** 
	 * Run this test.
	 */
	public static void main(String args[]) {
		junit.textui.TestRunner.run(suite());
	}

	/** 
	 * Create the collection of nodes.
	 */
	protected void setUp() {
		nodes=new java.util.TreeMap();

		a=new StringNode("A");
		b=new StringNode("B");
		c=new StringNode("C");
		d=new StringNode("D");
		e=new StringNode("E");

		// Add the nodes to the collection
		nodes.put("A", a);
		nodes.put("B", b);
		nodes.put("C", c);
		nodes.put("D", d);
		nodes.put("E", e);

		// A -> B    A -> C (cost 15)
		a.linkTo(b);
		a.linkTo(c, 15);

		// B -> C
		b.linkTo(c);

		// C -> D   C -> E
		c.linkTo(d);
		c.linkTo(e);

		// D -> E
		// d.linkTo(e);
		// D -> C at a higher cost, giving it a path to E
		d.linkTo(c, 100);

		// calculate the paths
		ShortestPathFinder spf=new ShortestPathFinder();
		spf.calculatePaths(nodes.values());
	}

	// verify a link matches the way we want
	private void assertLinkMatch(SPNode a, SPNode b, SPNode expectedNextHop,
		int cost) {

		SPVertex nextHop=a.getNextHop(b);
		if(expectedNextHop == null) {
			assertNull("Expected no link from " + a + " to " + b, nextHop);
		} else {
			assertNotNull("Expected a link from " + a + " to " + b, nextHop);
			assertSame("Expected link from " + a + " to " + b
				+ " to be via " + expectedNextHop
				+ ", got " + nextHop.getTo(), nextHop.getTo(), expectedNextHop);
			assertEquals("Incorrect cost for " + a + " -> " + b,
				cost, nextHop.getCost());
		}
	}

	/** 
	 * Test a basic SP find.
	 */
	public void testSPFind() {
		// Print out links...this is kinda big and ugly
		/*
		for(Iterator i=nodes.values().iterator(); i.hasNext();) {
			StringNode sn=(StringNode)i.next();
			sn.dump();
		}
		*/

		// These are manual tests.  Based on the way I configured the graph
		// above, these are all of the expected values (least costly
		// next-hops).

		// A -> B == 10 via B
		assertLinkMatch(a, b, b, 10);
		// A -> C == 15 via C
		assertLinkMatch(a, c, c, 15);
		// A -> D == 30 via B
		assertLinkMatch(a, d, c, 25);
		// A -> E == 60 via B
		assertLinkMatch(a, e, c, 25);

		// B -> A -- doesn't exist
		assertLinkMatch(b, a, null, 0);
		// B -> C == 10 via C
		assertLinkMatch(b, c, c, 10);
		// B -> D == 20 via C
		assertLinkMatch(b, d, c, 20);
		// B -> E == 20 via C
		assertLinkMatch(b, e, c, 20);

		// C -> A won't go
		assertLinkMatch(c, a, null, 0);
		// C -> B won't go
		assertLinkMatch(c, b, null, 0);
		// C -> D == 10 via D
		assertLinkMatch(c, d, d, 10);
		// C -> E == 10 via E
		assertLinkMatch(c, e, e, 10);

		// D -> A won't go
		assertLinkMatch(d, a, null, 0);
		// D -> B won't go
		assertLinkMatch(d, b, null, 0);
		// D -> C via C
		assertLinkMatch(d, c, c, 100);
		// D -> E via C
		assertLinkMatch(d, e, c, 110);

		// E Goes nowhere
		assertLinkMatch(e, a, null, 0);
		assertLinkMatch(e, b, null, 0);
		assertLinkMatch(e, c, null, 0);
		assertLinkMatch(e, d, null, 0);
	}

	/** 
	 * Do a couple of quick ShortestPath tests.
	 */
	public void testShortestPath() throws NoPathException {
		ShortestPath sp=new ShortestPath(a, b);
		assertEquals("ShortestPath from A -> B", 1, sp.size());
		sp=new ShortestPath(a, c);
		assertEquals("ShortestPath from A -> C", 1, sp.size());
		sp=new ShortestPath(a, d);
		assertEquals("ShortestPath from A -> D", 2, sp.size());
		sp=new ShortestPath(a, e);
		assertEquals("ShortestPath from A -> E", 2, sp.size());

		sp=new ShortestPath(d, e);
		assertEquals("ShortestPath from D -> E", 2, sp.size());
	}

	/** 
	 * Verify garbage collection can clean up the instances.
	 */
	public void testCleanup() {
		WeakReference aref=new WeakReference(a);
		WeakReference bref=new WeakReference(b);
		WeakReference cref=new WeakReference(c);
		WeakReference dref=new WeakReference(d);
		WeakReference eref=new WeakReference(e);

		// Verify the reference is alive
		assertNotNull("Reference to A is broken", aref.get());
		assertNotNull("Reference to B is broken", bref.get());
		assertNotNull("Reference to C is broken", cref.get());
		assertNotNull("Reference to D is broken", dref.get());
		assertNotNull("Reference to E is broken", eref.get());

		// Kill them all.
		nodes=null;
		a=null;
		b=null;
		c=null;
		d=null;
		e=null;

		// Pick up the trash
		System.gc();

		// Verify the reference is dead
		assertNull("Node A is still around", aref.get());
		assertNull("Node B is still around", bref.get());
		assertNull("Node C is still around", cref.get());
		assertNull("Node D is still around", dref.get());
		assertNull("Node E is still around", eref.get());
	}

	private class StringNode extends AbstractSPNode {
		private String str=null;

		public StringNode(String s) {
			super();
			if(s == null) {
				throw new NullPointerException("s can't be null");
			}
			str=s;
		}

		public String toString() {
			return("{StringNode " + str + "}");
		}

		public String getString() {
			return(str);
		}

		public void linkTo(StringNode x, int cost) {
			super.linkTo(x, cost);
		}

		public void linkTo(StringNode x) {
			super.linkTo(x);
		}

		private String indent(int indentation) {
			StringBuffer sb=new StringBuffer(indentation);
			for(int i=0; i<indentation; i++) {
				sb.append(' ');
			}
			return (sb.toString());
		}

		public void dump() {
			Set s=new java.util.HashSet();
			dump(0, s);
		}

		public void dump(int indentation, Set s) {
			System.out.println(indent(indentation) + this);
			// Find out what we have maps to
			for(Iterator i=nodes.values().iterator(); i.hasNext(); ) {
				StringNode sn=(StringNode)i.next();
				SPVertex vert=getNextHop(sn);
				if(vert != null) {
					System.out.println(indent(indentation + 4)
						+ " --> " + sn.getString() + " via "
						+ vert.getTo() + "@" + vert.getCost());
				}
			}
			// Dump out the connections
			for(Iterator i=getConnections().iterator(); i.hasNext(); ) {
				SPVertex spv=(SPVertex)i.next();
				if (! s.contains(spv.getTo())) {
					s.add(spv.getTo());
					StringNode sn=(StringNode)spv.getTo();
					sn.dump(indentation+2, s);
				}
			}
		}

		public int hashCode() {
			return(str.hashCode());
		}

		public int compareTo(Object o) {
			StringNode sn=(StringNode)o;

			return (str.compareTo(sn.getString()));
		}
	}

}
