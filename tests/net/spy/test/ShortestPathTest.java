// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: ShortestPathTest.java,v 1.1 2002/10/18 07:11:04 dustin Exp $

package net.spy.test;

import java.util.Map;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.Iterator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import net.spy.util.SPNode;
import net.spy.util.SPVertex;

/**
 * Test a weak hash set.
 */
public class ShortestPathTest extends TestCase {

	private Map nodes=null;

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

		StringNode a=new StringNode("A");
		StringNode b=new StringNode("B");
		StringNode c=new StringNode("C");
		StringNode d=new StringNode("D");
		StringNode e=new StringNode("E");

		// Add the nodes to the collection
		nodes.put("A", a);
		nodes.put("B", b);
		nodes.put("C", c);
		nodes.put("D", d);
		nodes.put("E", e);

		// A -> B    A -> C
		a.linkTo(b);
		a.linkTo(c);

		// B -> C
		b.linkTo(c);

		// C -> D   C -> E
		c.linkTo(d);
		c.linkTo(e);

		// D -> E
		// d.linkTo(e);
	}

	/** 
	 * Test a basic SP find.
	 */
	public void testSPFind() {
		// System.out.println(nodes);

		for(Iterator i=nodes.values().iterator(); i.hasNext();) {
			StringNode sn=(StringNode)i.next();
			sn.dump(0);
		}
	}

	private class StringNode implements SPNode {
		private TreeSet ts=null;
		private String str=null;

		public StringNode(String s) {
			super();
			if(s == null) {
				throw new NullPointerException("s can't be null");
			}
			str=s;
			ts=new TreeSet();
		}

		public SortedSet getConnections() {
			return(ts);
		}

		public String toString() {
			return("{StringNode " + str + "}");
		}

		public String getString() {
			return(str);
		}

		public void linkTo(StringNode x) {
			ts.add(new SPVertex(this, x));
		}

		private String indent(int indentation) {
			StringBuffer sb=new StringBuffer(indentation);
			for(int i=0; i<indentation; i++) {
				sb.append(' ');
			}
			return (sb.toString());
		}

		public void dump(int indentation) {
			System.out.println(indent(indentation) + this);
			for(Iterator i=getConnections().iterator(); i.hasNext(); ) {
				SPVertex spv=(SPVertex)i.next();
				StringNode sn=(StringNode)spv.getTo();
				sn.dump(indentation+2);
			}
		}

		public int hashCode() {
			return(str.hashCode());
		}

		public boolean equals(Object o) {
			boolean rv=false;

			if(o instanceof StringNode) {
				StringNode sn=(StringNode)o;
				rv=str.equals(sn.getString());
			}

			return (rv);
		}

		public int compareTo(Object o) {
			StringNode sn=(StringNode)o;

			return (str.compareTo(sn.getString()));
		}
	}

}
