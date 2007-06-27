// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>

package net.spy.util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

/**
 * Test a weak hash set.
 */
public class ShortestPathTest extends TestCase {

	Map<String, StringNode> nodes=null;
	private StringNode nodeA=null;
	private StringNode nodeB=null;
	private StringNode nodeC=null;
	private StringNode nodeD=null;
	private StringNode nodeE=null;
	private StringNode nodeF=null;
	private StringNode nodeG=null;

	/** 
	 * Create the collection of nodes.
	 */
	@Override
	protected void setUp() {
		nodes=new java.util.TreeMap<String, StringNode>();

		nodeA=new StringNode("A");
		nodeB=new StringNode("B");
		nodeC=new StringNode("C");
		nodeD=new StringNode("D");
		nodeE=new StringNode("E");
		nodeF=new StringNode("F");
		nodeG=new StringNode("G");

		// Add the nodes to the collection
		nodes.put("A", nodeA);
		nodes.put("B", nodeB);
		nodes.put("C", nodeC);
		nodes.put("D", nodeD);
		nodes.put("E", nodeE);
		nodes.put("F", nodeF);
		nodes.put("G", nodeG);

		// A -> B    A -> C (cost 15)
		nodeA.linkTo(nodeB);
		nodeA.linkTo(nodeC, 15);

		// B -> C
		nodeB.linkTo(nodeC);

		// C -> D   C -> E  C -> F
		nodeC.linkTo(nodeD);
		nodeC.linkTo(nodeE);
		nodeC.linkTo(nodeF);
		nodeC.linkTo(nodeG, 100);

		// D -> E
		// d.linkTo(e);
		// D -> C at a higher cost, giving it a path to E
		nodeD.linkTo(nodeC, 100);

		// Link e to itself
		nodeE.linkTo(nodeE, 10);

		// And f links to g and b
		nodeF.linkTo(nodeG, 10);
		nodeF.linkTo(nodeB, 200);

		// calculate the paths
		ShortestPathFinder spf=new ShortestPathFinder();
		spf.calculatePaths(nodes.values());
	}

	// verify a link matches the way we want
	private void assertLinkMatch(StringNode a, StringNode b,
			SPNode<?> expectedNextHop, int cost) {

		SPVertex<?> nextHop=a.getNextHop(b);
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

	public void testEquality() throws Exception {
		StringNode sn1=new StringNode("Test");
		StringNode sn2=new StringNode("Test");
		assertEquals(sn1, sn2);
		assertEquals(sn2, sn1);
		assertFalse(sn1.equals("Test"));
		assertFalse("Test".equals(sn1));
	}

	public void testSPVertexStuff() {

		StringNode sn1=new StringNode("String1");
		StringNode sn2=new StringNode("String2");

		SPVertex<StringNode> v1=new SPVertex<StringNode>(sn1);
		SPVertex<StringNode> v2=new SPVertex<StringNode>(sn1, 100);
		SPVertex<StringNode> v3=new SPVertex<StringNode>(sn2);
		SPVertex<StringNode> v4=new SPVertex<StringNode>(sn2);

		assertEquals(SPVertex.DEFAULT_COST, v1.getCost());
		String.valueOf(v1);

		assertEquals(0, v1.compareTo(v1));
		assertEquals(-1, v1.compareTo(v2));
		assertEquals(1, v2.compareTo(v1));
		assertEquals(-1, v1.compareTo(v3));
		assertEquals(0, v3.compareTo(v4));

		assertNotSame(v3, v4);

		try {
			SPVertex<StringNode> v=new SPVertex<StringNode>((StringNode)null);
			fail("Allowed me to make a null SPVertex:  " + v);
		} catch(NullPointerException e) {
			assertNotNull(e.getMessage());
		}
	}

	/** 
	 * Test a basic SP find.
	 */
	public void testSPFind() {
		// Print out links...this is kinda big and ugly
		/*
		for(StringNode : nodes) {
			sn.dump();
		}
		*/

		// These are manual tests.  Based on the way I configured the graph
		// above, these are all of the expected values (least costly
		// next-hops).

		// A -> A -- no match
		assertLinkMatch(nodeA, nodeA, null, 0);
		// A -> B == 10 via B
		assertLinkMatch(nodeA, nodeB, nodeB, 10);
		// A -> C == 15 via C
		assertLinkMatch(nodeA, nodeC, nodeC, 15);
		// A -> D == 25 via C
		assertLinkMatch(nodeA, nodeD, nodeC, 25);
		// A -> E == 25 via C
		assertLinkMatch(nodeA, nodeE, nodeC, 25);
		// A -> F == 25 via C
		assertLinkMatch(nodeA, nodeF, nodeC, 25);
		// A -> G == 35 via C
		assertLinkMatch(nodeA, nodeG, nodeC, 35);
		assertEquals(6, nodeA.getNextHops().size());

		// B -> A -- doesn't exist
		assertLinkMatch(nodeB, nodeA, null, 0);
		// B -> B -- via C
		assertLinkMatch(nodeB, nodeB, nodeC, 220);
		// B -> C == 10 via C
		assertLinkMatch(nodeB, nodeC, nodeC, 10);
		// B -> D == 20 via C
		assertLinkMatch(nodeB, nodeD, nodeC, 20);
		// B -> E == 20 via C
		assertLinkMatch(nodeB, nodeE, nodeC, 20);
		// B -> F == 20 via C
		assertLinkMatch(nodeB, nodeF, nodeC, 20);
		// B -> G == 30 via C
		assertLinkMatch(nodeB, nodeG, nodeC, 30);
		assertEquals(6, nodeB.getNextHops().size());

		// C -> A won't go
		assertLinkMatch(nodeC, nodeA, null, 0);
		// C -> B via F
		assertLinkMatch(nodeC, nodeB, nodeF, 210);
		// C -> C via D?
		assertLinkMatch(nodeC, nodeC, nodeD, 110);
		// C -> D == 10 via D
		assertLinkMatch(nodeC, nodeD, nodeD, 10);
		// C -> E == 10 via E
		assertLinkMatch(nodeC, nodeE, nodeE, 10);
		// C -> F == 10 via F
		assertLinkMatch(nodeC, nodeF, nodeF, 10);
		// C -> G == 20 via F
		assertLinkMatch(nodeC, nodeG, nodeF, 20);
		assertEquals(6, nodeC.getNextHops().size());

		// D -> A won't go
		assertLinkMatch(nodeD, nodeA, null, 0);
		// D -> B via C
		assertLinkMatch(nodeD, nodeB, nodeC, 310);
		// D -> C via C
		assertLinkMatch(nodeD, nodeC, nodeC, 100);
		// D -> D via C
		assertLinkMatch(nodeD, nodeD, nodeC, 110);
		// D -> E via C
		assertLinkMatch(nodeD, nodeE, nodeC, 110);
		// D -> F via C
		assertLinkMatch(nodeD, nodeF, nodeC, 110);
		// D -> G via C
		assertLinkMatch(nodeD, nodeG, nodeC, 120);
		assertEquals(6, nodeD.getNextHops().size());

		// E Goes nowhere except E
		assertLinkMatch(nodeE, nodeA, null, 0);
		assertLinkMatch(nodeE, nodeB, null, 0);
		assertLinkMatch(nodeE, nodeC, null, 0);
		assertLinkMatch(nodeE, nodeD, null, 0);
		assertLinkMatch(nodeE, nodeE, nodeE, 10);
		assertLinkMatch(nodeE, nodeF, null, 0);
		assertLinkMatch(nodeE, nodeG, null, 0);
		assertEquals(1, nodeE.getNextHops().size());

		// F Goes to G and B
		assertLinkMatch(nodeF, nodeA, null, 0);
		assertLinkMatch(nodeF, nodeB, nodeB, 200);
		assertLinkMatch(nodeF, nodeC, nodeB, 210);
		assertLinkMatch(nodeF, nodeD, nodeB, 220);
		assertLinkMatch(nodeF, nodeE, nodeB, 220);
		assertLinkMatch(nodeF, nodeF, nodeB, 220);
		assertLinkMatch(nodeF, nodeG, nodeG, 10);
		assertEquals(6, nodeF.getNextHops().size());

		// G Goes to nowhere
		assertLinkMatch(nodeG, nodeA, null, 0);
		assertLinkMatch(nodeG, nodeB, null, 0);
		assertLinkMatch(nodeG, nodeC, null, 0);
		assertLinkMatch(nodeG, nodeD, null, 0);
		assertLinkMatch(nodeG, nodeE, null, 0);
		assertLinkMatch(nodeG, nodeF, null, 0);
		assertLinkMatch(nodeG, nodeG, null, 0);
		assertEquals(0, nodeG.getNextHops().size());
	}

	/** 
	 * Do a couple of quick ShortestPath tests.
	 */
	public void testShortestPath() throws NoPathException {
		ShortestPath<StringNode> sp=new ShortestPath<StringNode>(nodeA, nodeB);
		assertEquals("ShortestPath from A -> B", 1, sp.size());
		try {
			sp=new ShortestPath<StringNode>(nodeA, nodeA);
			fail("Expected to not find a path from A -> A, found " + sp);
		} catch(NoPathException e) {
			// Success
		}

		try {
			sp=new ShortestPath<StringNode>(null, nodeA);
			fail("Expected to not find a path from null -> A, found " + sp);
		} catch(NullPointerException e) {
			assertNotNull(e.getMessage());
		}

		try {
			sp=new ShortestPath<StringNode>(nodeA, null);
			fail("Expected to not find a path from A -> null, found " + sp);
		} catch(NullPointerException e) {
			assertNotNull(e.getMessage());
		}

		sp=new ShortestPath<StringNode>(nodeA, nodeC);
		assertEquals("ShortestPath from A -> C:  " + sp, 1, sp.size());
		assertEquals(15, sp.getCost());
		sp=new ShortestPath<StringNode>(nodeA, nodeD);
		assertEquals("ShortestPath from A -> D:  " + sp, 2, sp.size());
		assertEquals(25, sp.getCost());
		sp=new ShortestPath<StringNode>(nodeA, nodeE);
		assertEquals("ShortestPath from A -> E:  " + sp, 2, sp.size());
		assertEquals(25, sp.getCost());

		sp=new ShortestPath<StringNode>(nodeD, nodeE);
		assertEquals("ShortestPath from D -> E:  " + sp, 2, sp.size());
		assertEquals(110, sp.getCost());

		sp=new ShortestPath<StringNode>(nodeE, nodeE);
		assertEquals("ShortestPath from E -> E:  " + sp, 1, sp.size());
		assertEquals(10, sp.getCost());
	}

	public void testLongShortestPath() throws Exception {
		ArrayList<StringNode> al=new ArrayList<StringNode>(1051);

		StringNode sn=new StringNode("starting node");
		al.add(sn);

		StringNode lastNode=sn;
		for(int i=0; i<1050; i++) {
			StringNode newNode=new StringNode(String.valueOf(i));
			al.add(newNode);
			lastNode.linkTo(newNode);
			lastNode=newNode;
		}

		ShortestPathFinder spf=new ShortestPathFinder();
		spf.calculatePaths(al);

		try {
			ShortestPath<StringNode> sp=new ShortestPath<StringNode>(sn, lastNode);
			fail("Expected path to be too deep from " + sn + " to "
				+ lastNode + ", but found" + sp);
		} catch(NoPathException e) {
			assertEquals("No path from " + sn + " to " + lastNode
				+ " - Too deep!", e.getMessage());
		}
	}

	/** 
	 * Verify garbage collection can clean up the instances.
	 */
	public void testCleanup() {
		WeakReference<StringNode> aref=new WeakReference<StringNode>(nodeA);
		WeakReference<StringNode> bref=new WeakReference<StringNode>(nodeB);
		WeakReference<StringNode> cref=new WeakReference<StringNode>(nodeC);
		WeakReference<StringNode> dref=new WeakReference<StringNode>(nodeD);
		WeakReference<StringNode> eref=new WeakReference<StringNode>(nodeE);

		// Verify the reference is alive
		assertNotNull("Reference to A is broken", aref.get());
		assertNotNull("Reference to B is broken", bref.get());
		assertNotNull("Reference to C is broken", cref.get());
		assertNotNull("Reference to D is broken", dref.get());
		assertNotNull("Reference to E is broken", eref.get());

		// Kill them all.
		nodes=null;
		nodeA=null;
		nodeB=null;
		nodeC=null;
		nodeD=null;
		nodeE=null;

		// Pick up the trash
		System.gc();

		// Verify the reference is dead
		assertNull("Node A is still around", aref.get());
		assertNull("Node B is still around", bref.get());
		assertNull("Node C is still around", cref.get());
		assertNull("Node D is still around", dref.get());
		assertNull("Node E is still around", eref.get());
	}

	/** 
	 * Validate a large graph with a lot of links may be created.
	 */
	public void testBigGraph() {
		List<StringNode> tmpList=new ArrayList<StringNode>(100);

		tmpList.add(new StringNode("0"));
		tmpList.add(new StringNode("1"));
		tmpList.add(new StringNode("2"));
		tmpList.add(new StringNode("3"));
		tmpList.add(new StringNode("4"));
		tmpList.add(new StringNode("5"));
		tmpList.add(new StringNode("6"));
		tmpList.add(new StringNode("7"));
		tmpList.add(new StringNode("8"));
		tmpList.add(new StringNode("9"));
		tmpList.add(new StringNode("10"));
		tmpList.add(new StringNode("11"));
		tmpList.add(new StringNode("12"));
		tmpList.add(new StringNode("13"));
		tmpList.add(new StringNode("14"));
		tmpList.add(new StringNode("15"));
		tmpList.add(new StringNode("16"));
		tmpList.add(new StringNode("17"));
		tmpList.add(new StringNode("18"));
		tmpList.add(new StringNode("19"));
		tmpList.add(new StringNode("20"));
		tmpList.add(new StringNode("21"));
		tmpList.add(new StringNode("22"));
		tmpList.add(new StringNode("23"));
		tmpList.add(new StringNode("24"));
		tmpList.add(new StringNode("25"));
		tmpList.add(new StringNode("26"));
		tmpList.add(new StringNode("27"));
		tmpList.add(new StringNode("28"));
		tmpList.add(new StringNode("29"));
		tmpList.add(new StringNode("30"));
		tmpList.add(new StringNode("31"));
		tmpList.add(new StringNode("32"));
		tmpList.add(new StringNode("33"));
		tmpList.add(new StringNode("34"));
		tmpList.add(new StringNode("35"));
		tmpList.add(new StringNode("36"));
		tmpList.add(new StringNode("37"));
		tmpList.add(new StringNode("38"));
		tmpList.add(new StringNode("39"));
		tmpList.add(new StringNode("40"));
		tmpList.add(new StringNode("41"));
		tmpList.add(new StringNode("42"));
		tmpList.add(new StringNode("43"));
		tmpList.add(new StringNode("44"));
		tmpList.add(new StringNode("45"));
		tmpList.add(new StringNode("46"));
		tmpList.add(new StringNode("47"));
		tmpList.add(new StringNode("48"));
		tmpList.add(new StringNode("49"));
		tmpList.add(new StringNode("50"));
		tmpList.add(new StringNode("51"));
		tmpList.add(new StringNode("52"));
		tmpList.add(new StringNode("53"));
		tmpList.add(new StringNode("54"));
		tmpList.add(new StringNode("55"));
		tmpList.add(new StringNode("56"));
		tmpList.add(new StringNode("57"));
		tmpList.add(new StringNode("58"));
		tmpList.add(new StringNode("59"));
		tmpList.add(new StringNode("60"));
		tmpList.add(new StringNode("61"));
		tmpList.add(new StringNode("62"));
		tmpList.add(new StringNode("63"));
		tmpList.add(new StringNode("64"));
		tmpList.add(new StringNode("65"));
		tmpList.add(new StringNode("66"));
		tmpList.add(new StringNode("67"));
		tmpList.add(new StringNode("68"));
		tmpList.add(new StringNode("69"));
		tmpList.add(new StringNode("70"));
		tmpList.add(new StringNode("71"));
		tmpList.add(new StringNode("72"));
		tmpList.add(new StringNode("73"));
		tmpList.add(new StringNode("74"));
		tmpList.add(new StringNode("75"));
		tmpList.add(new StringNode("76"));
		tmpList.add(new StringNode("77"));
		tmpList.add(new StringNode("78"));
		tmpList.add(new StringNode("79"));
		tmpList.add(new StringNode("80"));
		tmpList.add(new StringNode("81"));
		tmpList.add(new StringNode("82"));
		tmpList.add(new StringNode("83"));
		tmpList.add(new StringNode("84"));
		tmpList.add(new StringNode("85"));
		tmpList.add(new StringNode("86"));
		tmpList.add(new StringNode("87"));
		tmpList.add(new StringNode("88"));
		tmpList.add(new StringNode("89"));
		tmpList.add(new StringNode("90"));
		tmpList.add(new StringNode("91"));
		tmpList.add(new StringNode("92"));
		tmpList.add(new StringNode("93"));
		tmpList.add(new StringNode("94"));
		tmpList.add(new StringNode("95"));
		tmpList.add(new StringNode("96"));
		tmpList.add(new StringNode("97"));
		tmpList.add(new StringNode("98"));
		tmpList.add(new StringNode("99"));

		StringNode tmpNode=null;
		tmpNode=tmpList.get(62);
		tmpNode.linkTo(tmpList.get(10));
		tmpNode.linkTo(tmpList.get(81));
		tmpNode.linkTo(tmpList.get(22));
		tmpNode.linkTo(tmpList.get(34));
		tmpNode.linkTo(tmpList.get(44));
		tmpNode.linkTo(tmpList.get(61));
		tmpNode.linkTo(tmpList.get(97));
		tmpNode.linkTo(tmpList.get(87));
		tmpNode.linkTo(tmpList.get(61));
		tmpNode.linkTo(tmpList.get(27));
		tmpNode.linkTo(tmpList.get(97));
		tmpNode.linkTo(tmpList.get(46));
		tmpNode.linkTo(tmpList.get(79));
		tmpNode.linkTo(tmpList.get(39));
		tmpNode.linkTo(tmpList.get(65));
		tmpNode.linkTo(tmpList.get(95));
		tmpNode.linkTo(tmpList.get(1));
		tmpNode.linkTo(tmpList.get(33));
		tmpNode.linkTo(tmpList.get(3));
		tmpNode.linkTo(tmpList.get(74));
		tmpNode.linkTo(tmpList.get(72));
		tmpNode.linkTo(tmpList.get(80));
		tmpNode.linkTo(tmpList.get(36));
		tmpNode.linkTo(tmpList.get(72));
		tmpNode.linkTo(tmpList.get(1));
		tmpNode=tmpList.get(40);
		tmpNode.linkTo(tmpList.get(27));
		tmpNode.linkTo(tmpList.get(33));
		tmpNode.linkTo(tmpList.get(21));
		tmpNode.linkTo(tmpList.get(33));
		tmpNode.linkTo(tmpList.get(24));
		tmpNode.linkTo(tmpList.get(83));
		tmpNode.linkTo(tmpList.get(18));
		tmpNode.linkTo(tmpList.get(50));
		tmpNode.linkTo(tmpList.get(51));
		tmpNode.linkTo(tmpList.get(11));
		tmpNode.linkTo(tmpList.get(93));
		tmpNode.linkTo(tmpList.get(31));
		tmpNode.linkTo(tmpList.get(56));
		tmpNode.linkTo(tmpList.get(64));
		tmpNode.linkTo(tmpList.get(53));
		tmpNode.linkTo(tmpList.get(79));
		tmpNode.linkTo(tmpList.get(67));
		tmpNode.linkTo(tmpList.get(66));
		tmpNode.linkTo(tmpList.get(63));
		tmpNode.linkTo(tmpList.get(50));
		tmpNode.linkTo(tmpList.get(3));
		tmpNode.linkTo(tmpList.get(63));
		tmpNode.linkTo(tmpList.get(65));
		tmpNode.linkTo(tmpList.get(13));
		tmpNode.linkTo(tmpList.get(69));
		tmpNode=tmpList.get(70);
		tmpNode.linkTo(tmpList.get(78));
		tmpNode.linkTo(tmpList.get(94));
		tmpNode.linkTo(tmpList.get(83));
		tmpNode.linkTo(tmpList.get(10));
		tmpNode.linkTo(tmpList.get(99));
		tmpNode.linkTo(tmpList.get(51));
		tmpNode.linkTo(tmpList.get(63));
		tmpNode.linkTo(tmpList.get(82));
		tmpNode.linkTo(tmpList.get(62));
		tmpNode.linkTo(tmpList.get(58));
		tmpNode.linkTo(tmpList.get(21));
		tmpNode.linkTo(tmpList.get(60));
		tmpNode.linkTo(tmpList.get(2));
		tmpNode.linkTo(tmpList.get(74));
		tmpNode.linkTo(tmpList.get(34));
		tmpNode.linkTo(tmpList.get(25));
		tmpNode.linkTo(tmpList.get(5));
		tmpNode.linkTo(tmpList.get(51));
		tmpNode.linkTo(tmpList.get(64));
		tmpNode.linkTo(tmpList.get(10));
		tmpNode.linkTo(tmpList.get(88));
		tmpNode.linkTo(tmpList.get(29));
		tmpNode.linkTo(tmpList.get(50));
		tmpNode.linkTo(tmpList.get(75));
		tmpNode.linkTo(tmpList.get(26));
		tmpNode=tmpList.get(24);
		tmpNode.linkTo(tmpList.get(18));
		tmpNode.linkTo(tmpList.get(22));
		tmpNode.linkTo(tmpList.get(47));
		tmpNode.linkTo(tmpList.get(8));
		tmpNode.linkTo(tmpList.get(89));
		tmpNode.linkTo(tmpList.get(42));
		tmpNode.linkTo(tmpList.get(17));
		tmpNode.linkTo(tmpList.get(47));
		tmpNode.linkTo(tmpList.get(87));
		tmpNode.linkTo(tmpList.get(69));
		tmpNode.linkTo(tmpList.get(78));
		tmpNode.linkTo(tmpList.get(7));
		tmpNode.linkTo(tmpList.get(95));
		tmpNode.linkTo(tmpList.get(97));
		tmpNode.linkTo(tmpList.get(47));
		tmpNode.linkTo(tmpList.get(22));
		tmpNode.linkTo(tmpList.get(87));
		tmpNode.linkTo(tmpList.get(66));
		tmpNode.linkTo(tmpList.get(60));
		tmpNode.linkTo(tmpList.get(92));
		tmpNode.linkTo(tmpList.get(91));
		tmpNode.linkTo(tmpList.get(40));
		tmpNode.linkTo(tmpList.get(63));
		tmpNode.linkTo(tmpList.get(52));
		tmpNode.linkTo(tmpList.get(85));
		tmpNode=tmpList.get(9);
		tmpNode.linkTo(tmpList.get(62));
		tmpNode.linkTo(tmpList.get(23));
		tmpNode.linkTo(tmpList.get(66));
		tmpNode.linkTo(tmpList.get(52));
		tmpNode.linkTo(tmpList.get(76));
		tmpNode.linkTo(tmpList.get(81));
		tmpNode.linkTo(tmpList.get(53));
		tmpNode.linkTo(tmpList.get(98));
		tmpNode.linkTo(tmpList.get(97));
		tmpNode.linkTo(tmpList.get(32));
		tmpNode.linkTo(tmpList.get(73));
		tmpNode.linkTo(tmpList.get(26));
		tmpNode.linkTo(tmpList.get(33));
		tmpNode.linkTo(tmpList.get(20));
		tmpNode.linkTo(tmpList.get(19));
		tmpNode.linkTo(tmpList.get(4));
		tmpNode.linkTo(tmpList.get(94));
		tmpNode.linkTo(tmpList.get(78));
		tmpNode.linkTo(tmpList.get(99));
		tmpNode.linkTo(tmpList.get(33));
		tmpNode.linkTo(tmpList.get(24));
		tmpNode.linkTo(tmpList.get(45));
		tmpNode.linkTo(tmpList.get(79));
		tmpNode.linkTo(tmpList.get(3));
		tmpNode.linkTo(tmpList.get(22));
		tmpNode=tmpList.get(27);
		tmpNode.linkTo(tmpList.get(41));
		tmpNode.linkTo(tmpList.get(68));
		tmpNode.linkTo(tmpList.get(92));
		tmpNode.linkTo(tmpList.get(88));
		tmpNode.linkTo(tmpList.get(66));
		tmpNode.linkTo(tmpList.get(22));
		tmpNode.linkTo(tmpList.get(8));
		tmpNode.linkTo(tmpList.get(27));
		tmpNode.linkTo(tmpList.get(0));
		tmpNode.linkTo(tmpList.get(4));
		tmpNode.linkTo(tmpList.get(81));
		tmpNode.linkTo(tmpList.get(46));
		tmpNode.linkTo(tmpList.get(34));
		tmpNode.linkTo(tmpList.get(7));
		tmpNode.linkTo(tmpList.get(55));
		tmpNode.linkTo(tmpList.get(86));
		tmpNode.linkTo(tmpList.get(19));
		tmpNode.linkTo(tmpList.get(49));
		tmpNode.linkTo(tmpList.get(57));
		tmpNode.linkTo(tmpList.get(47));
		tmpNode.linkTo(tmpList.get(15));
		tmpNode.linkTo(tmpList.get(65));
		tmpNode.linkTo(tmpList.get(37));
		tmpNode.linkTo(tmpList.get(43));
		tmpNode.linkTo(tmpList.get(2));
		tmpNode=tmpList.get(75);
		tmpNode.linkTo(tmpList.get(82));
		tmpNode.linkTo(tmpList.get(72));
		tmpNode.linkTo(tmpList.get(10));
		tmpNode.linkTo(tmpList.get(76));
		tmpNode.linkTo(tmpList.get(85));
		tmpNode.linkTo(tmpList.get(6));
		tmpNode.linkTo(tmpList.get(88));
		tmpNode.linkTo(tmpList.get(13));
		tmpNode.linkTo(tmpList.get(87));
		tmpNode.linkTo(tmpList.get(47));
		tmpNode.linkTo(tmpList.get(61));
		tmpNode.linkTo(tmpList.get(32));
		tmpNode.linkTo(tmpList.get(80));
		tmpNode.linkTo(tmpList.get(33));
		tmpNode.linkTo(tmpList.get(3));
		tmpNode.linkTo(tmpList.get(79));
		tmpNode.linkTo(tmpList.get(35));
		tmpNode.linkTo(tmpList.get(93));
		tmpNode.linkTo(tmpList.get(94));
		tmpNode.linkTo(tmpList.get(47));
		tmpNode.linkTo(tmpList.get(10));
		tmpNode.linkTo(tmpList.get(96));
		tmpNode.linkTo(tmpList.get(93));
		tmpNode.linkTo(tmpList.get(14));
		tmpNode.linkTo(tmpList.get(88));
		tmpNode=tmpList.get(49);
		tmpNode.linkTo(tmpList.get(25));
		tmpNode.linkTo(tmpList.get(40));
		tmpNode.linkTo(tmpList.get(97));
		tmpNode.linkTo(tmpList.get(72));
		tmpNode.linkTo(tmpList.get(49));
		tmpNode.linkTo(tmpList.get(23));
		tmpNode.linkTo(tmpList.get(60));
		tmpNode.linkTo(tmpList.get(24));
		tmpNode.linkTo(tmpList.get(98));
		tmpNode.linkTo(tmpList.get(71));
		tmpNode.linkTo(tmpList.get(94));
		tmpNode.linkTo(tmpList.get(84));
		tmpNode.linkTo(tmpList.get(89));
		tmpNode.linkTo(tmpList.get(65));
		tmpNode.linkTo(tmpList.get(76));
		tmpNode.linkTo(tmpList.get(87));
		tmpNode.linkTo(tmpList.get(23));
		tmpNode.linkTo(tmpList.get(53));
		tmpNode.linkTo(tmpList.get(3));
		tmpNode.linkTo(tmpList.get(17));
		tmpNode.linkTo(tmpList.get(4));
		tmpNode.linkTo(tmpList.get(16));
		tmpNode.linkTo(tmpList.get(7));
		tmpNode.linkTo(tmpList.get(6));
		tmpNode.linkTo(tmpList.get(48));
		tmpNode=tmpList.get(54);
		tmpNode.linkTo(tmpList.get(38));
		tmpNode.linkTo(tmpList.get(95));
		tmpNode.linkTo(tmpList.get(98));
		tmpNode.linkTo(tmpList.get(51));
		tmpNode.linkTo(tmpList.get(71));
		tmpNode.linkTo(tmpList.get(88));
		tmpNode.linkTo(tmpList.get(2));
		tmpNode.linkTo(tmpList.get(69));
		tmpNode.linkTo(tmpList.get(58));
		tmpNode.linkTo(tmpList.get(49));
		tmpNode.linkTo(tmpList.get(72));
		tmpNode.linkTo(tmpList.get(79));
		tmpNode.linkTo(tmpList.get(24));
		tmpNode.linkTo(tmpList.get(96));
		tmpNode.linkTo(tmpList.get(66));
		tmpNode.linkTo(tmpList.get(16));
		tmpNode.linkTo(tmpList.get(1));
		tmpNode.linkTo(tmpList.get(80));
		tmpNode.linkTo(tmpList.get(22));
		tmpNode.linkTo(tmpList.get(15));
		tmpNode.linkTo(tmpList.get(64));
		tmpNode.linkTo(tmpList.get(96));
		tmpNode.linkTo(tmpList.get(9));
		tmpNode.linkTo(tmpList.get(30));
		tmpNode.linkTo(tmpList.get(2));
		tmpNode=tmpList.get(72);
		tmpNode.linkTo(tmpList.get(99));
		tmpNode.linkTo(tmpList.get(48));
		tmpNode.linkTo(tmpList.get(44));
		tmpNode.linkTo(tmpList.get(82));
		tmpNode.linkTo(tmpList.get(72));
		tmpNode.linkTo(tmpList.get(39));
		tmpNode.linkTo(tmpList.get(44));
		tmpNode.linkTo(tmpList.get(52));
		tmpNode.linkTo(tmpList.get(67));
		tmpNode.linkTo(tmpList.get(0));
		tmpNode.linkTo(tmpList.get(23));
		tmpNode.linkTo(tmpList.get(45));
		tmpNode.linkTo(tmpList.get(82));
		tmpNode.linkTo(tmpList.get(71));
		tmpNode.linkTo(tmpList.get(54));
		tmpNode.linkTo(tmpList.get(51));
		tmpNode.linkTo(tmpList.get(11));
		tmpNode.linkTo(tmpList.get(99));
		tmpNode.linkTo(tmpList.get(82));
		tmpNode.linkTo(tmpList.get(60));
		tmpNode.linkTo(tmpList.get(62));
		tmpNode.linkTo(tmpList.get(37));
		tmpNode.linkTo(tmpList.get(27));
		tmpNode.linkTo(tmpList.get(93));
		tmpNode.linkTo(tmpList.get(64));
		tmpNode=tmpList.get(23);
		tmpNode.linkTo(tmpList.get(74));
		tmpNode.linkTo(tmpList.get(33));
		tmpNode.linkTo(tmpList.get(45));
		tmpNode.linkTo(tmpList.get(51));
		tmpNode.linkTo(tmpList.get(16));
		tmpNode.linkTo(tmpList.get(89));
		tmpNode.linkTo(tmpList.get(84));
		tmpNode.linkTo(tmpList.get(89));
		tmpNode.linkTo(tmpList.get(76));
		tmpNode.linkTo(tmpList.get(8));
		tmpNode.linkTo(tmpList.get(84));
		tmpNode.linkTo(tmpList.get(49));
		tmpNode.linkTo(tmpList.get(46));
		tmpNode.linkTo(tmpList.get(11));
		tmpNode.linkTo(tmpList.get(97));
		tmpNode.linkTo(tmpList.get(71));
		tmpNode.linkTo(tmpList.get(48));
		tmpNode.linkTo(tmpList.get(16));
		tmpNode.linkTo(tmpList.get(69));
		tmpNode.linkTo(tmpList.get(66));
		tmpNode.linkTo(tmpList.get(0));
		tmpNode.linkTo(tmpList.get(4));
		tmpNode.linkTo(tmpList.get(18));
		tmpNode.linkTo(tmpList.get(79));
		tmpNode.linkTo(tmpList.get(30));
		tmpNode=tmpList.get(9);
		tmpNode.linkTo(tmpList.get(40));
		tmpNode.linkTo(tmpList.get(44));
		tmpNode.linkTo(tmpList.get(84));
		tmpNode.linkTo(tmpList.get(69));
		tmpNode.linkTo(tmpList.get(52));
		tmpNode.linkTo(tmpList.get(70));
		tmpNode.linkTo(tmpList.get(25));
		tmpNode.linkTo(tmpList.get(36));
		tmpNode.linkTo(tmpList.get(68));
		tmpNode.linkTo(tmpList.get(60));
		tmpNode.linkTo(tmpList.get(17));
		tmpNode.linkTo(tmpList.get(93));
		tmpNode.linkTo(tmpList.get(44));
		tmpNode.linkTo(tmpList.get(58));
		tmpNode.linkTo(tmpList.get(20));
		tmpNode.linkTo(tmpList.get(20));
		tmpNode.linkTo(tmpList.get(54));
		tmpNode.linkTo(tmpList.get(15));
		tmpNode.linkTo(tmpList.get(75));
		tmpNode.linkTo(tmpList.get(8));
		tmpNode.linkTo(tmpList.get(89));
		tmpNode.linkTo(tmpList.get(5));
		tmpNode.linkTo(tmpList.get(68));
		tmpNode.linkTo(tmpList.get(78));
		tmpNode.linkTo(tmpList.get(39));
		tmpNode=tmpList.get(39);
		tmpNode.linkTo(tmpList.get(37));
		tmpNode.linkTo(tmpList.get(68));
		tmpNode.linkTo(tmpList.get(14));
		tmpNode.linkTo(tmpList.get(25));
		tmpNode.linkTo(tmpList.get(81));
		tmpNode.linkTo(tmpList.get(67));
		tmpNode.linkTo(tmpList.get(63));
		tmpNode.linkTo(tmpList.get(16));
		tmpNode.linkTo(tmpList.get(30));
		tmpNode.linkTo(tmpList.get(31));
		tmpNode.linkTo(tmpList.get(84));
		tmpNode.linkTo(tmpList.get(42));
		tmpNode.linkTo(tmpList.get(9));
		tmpNode.linkTo(tmpList.get(29));
		tmpNode.linkTo(tmpList.get(49));
		tmpNode.linkTo(tmpList.get(70));
		tmpNode.linkTo(tmpList.get(62));
		tmpNode.linkTo(tmpList.get(39));
		tmpNode.linkTo(tmpList.get(31));
		tmpNode.linkTo(tmpList.get(90));
		tmpNode.linkTo(tmpList.get(0));
		tmpNode.linkTo(tmpList.get(56));
		tmpNode.linkTo(tmpList.get(94));
		tmpNode.linkTo(tmpList.get(64));
		tmpNode.linkTo(tmpList.get(45));
		tmpNode=tmpList.get(66);
		tmpNode.linkTo(tmpList.get(95));
		tmpNode.linkTo(tmpList.get(75));
		tmpNode.linkTo(tmpList.get(91));
		tmpNode.linkTo(tmpList.get(26));
		tmpNode.linkTo(tmpList.get(3));
		tmpNode.linkTo(tmpList.get(58));
		tmpNode.linkTo(tmpList.get(85));
		tmpNode.linkTo(tmpList.get(54));
		tmpNode.linkTo(tmpList.get(48));
		tmpNode.linkTo(tmpList.get(98));
		tmpNode.linkTo(tmpList.get(87));
		tmpNode.linkTo(tmpList.get(79));
		tmpNode.linkTo(tmpList.get(62));
		tmpNode.linkTo(tmpList.get(10));
		tmpNode.linkTo(tmpList.get(14));
		tmpNode.linkTo(tmpList.get(96));
		tmpNode.linkTo(tmpList.get(34));
		tmpNode.linkTo(tmpList.get(93));
		tmpNode.linkTo(tmpList.get(83));
		tmpNode.linkTo(tmpList.get(14));
		tmpNode.linkTo(tmpList.get(73));
		tmpNode.linkTo(tmpList.get(72));
		tmpNode.linkTo(tmpList.get(83));
		tmpNode.linkTo(tmpList.get(27));
		tmpNode.linkTo(tmpList.get(56));
		tmpNode=tmpList.get(12);
		tmpNode.linkTo(tmpList.get(86));
		tmpNode.linkTo(tmpList.get(3));
		tmpNode.linkTo(tmpList.get(7));
		tmpNode.linkTo(tmpList.get(10));
		tmpNode.linkTo(tmpList.get(44));
		tmpNode.linkTo(tmpList.get(57));
		tmpNode.linkTo(tmpList.get(27));
		tmpNode.linkTo(tmpList.get(22));
		tmpNode.linkTo(tmpList.get(82));
		tmpNode.linkTo(tmpList.get(31));
		tmpNode.linkTo(tmpList.get(5));
		tmpNode.linkTo(tmpList.get(89));
		tmpNode.linkTo(tmpList.get(39));
		tmpNode.linkTo(tmpList.get(91));
		tmpNode.linkTo(tmpList.get(36));
		tmpNode.linkTo(tmpList.get(40));
		tmpNode.linkTo(tmpList.get(80));
		tmpNode.linkTo(tmpList.get(47));
		tmpNode.linkTo(tmpList.get(65));
		tmpNode.linkTo(tmpList.get(71));
		tmpNode.linkTo(tmpList.get(14));
		tmpNode.linkTo(tmpList.get(26));
		tmpNode.linkTo(tmpList.get(7));
		tmpNode.linkTo(tmpList.get(0));
		tmpNode.linkTo(tmpList.get(30));
		tmpNode=tmpList.get(60);
		tmpNode.linkTo(tmpList.get(99));
		tmpNode.linkTo(tmpList.get(78));
		tmpNode.linkTo(tmpList.get(21));
		tmpNode.linkTo(tmpList.get(21));
		tmpNode.linkTo(tmpList.get(38));
		tmpNode.linkTo(tmpList.get(85));
		tmpNode.linkTo(tmpList.get(7));
		tmpNode.linkTo(tmpList.get(61));
		tmpNode.linkTo(tmpList.get(2));
		tmpNode.linkTo(tmpList.get(3));
		tmpNode.linkTo(tmpList.get(46));
		tmpNode.linkTo(tmpList.get(79));
		tmpNode.linkTo(tmpList.get(19));
		tmpNode.linkTo(tmpList.get(73));
		tmpNode.linkTo(tmpList.get(10));
		tmpNode.linkTo(tmpList.get(10));
		tmpNode.linkTo(tmpList.get(39));
		tmpNode.linkTo(tmpList.get(89));
		tmpNode.linkTo(tmpList.get(19));
		tmpNode.linkTo(tmpList.get(96));
		tmpNode.linkTo(tmpList.get(3));
		tmpNode.linkTo(tmpList.get(16));
		tmpNode.linkTo(tmpList.get(37));
		tmpNode.linkTo(tmpList.get(28));
		tmpNode.linkTo(tmpList.get(88));
		tmpNode=tmpList.get(93);
		tmpNode.linkTo(tmpList.get(86));
		tmpNode.linkTo(tmpList.get(15));
		tmpNode.linkTo(tmpList.get(34));
		tmpNode.linkTo(tmpList.get(68));
		tmpNode.linkTo(tmpList.get(3));
		tmpNode.linkTo(tmpList.get(83));
		tmpNode.linkTo(tmpList.get(23));
		tmpNode.linkTo(tmpList.get(51));
		tmpNode.linkTo(tmpList.get(49));
		tmpNode.linkTo(tmpList.get(65));
		tmpNode.linkTo(tmpList.get(12));
		tmpNode.linkTo(tmpList.get(82));
		tmpNode.linkTo(tmpList.get(82));
		tmpNode.linkTo(tmpList.get(57));
		tmpNode.linkTo(tmpList.get(69));
		tmpNode.linkTo(tmpList.get(97));
		tmpNode.linkTo(tmpList.get(56));
		tmpNode.linkTo(tmpList.get(32));
		tmpNode.linkTo(tmpList.get(32));
		tmpNode.linkTo(tmpList.get(42));
		tmpNode.linkTo(tmpList.get(93));
		tmpNode.linkTo(tmpList.get(45));
		tmpNode.linkTo(tmpList.get(25));
		tmpNode.linkTo(tmpList.get(37));
		tmpNode.linkTo(tmpList.get(84));
		tmpNode=tmpList.get(80);
		tmpNode.linkTo(tmpList.get(61));
		tmpNode.linkTo(tmpList.get(64));
		tmpNode.linkTo(tmpList.get(18));
		tmpNode.linkTo(tmpList.get(88));
		tmpNode.linkTo(tmpList.get(53));
		tmpNode.linkTo(tmpList.get(64));
		tmpNode.linkTo(tmpList.get(94));
		tmpNode.linkTo(tmpList.get(98));
		tmpNode.linkTo(tmpList.get(24));
		tmpNode.linkTo(tmpList.get(42));
		tmpNode.linkTo(tmpList.get(35));
		tmpNode.linkTo(tmpList.get(42));
		tmpNode.linkTo(tmpList.get(51));
		tmpNode.linkTo(tmpList.get(29));
		tmpNode.linkTo(tmpList.get(8));
		tmpNode.linkTo(tmpList.get(59));
		tmpNode.linkTo(tmpList.get(75));
		tmpNode.linkTo(tmpList.get(35));
		tmpNode.linkTo(tmpList.get(26));
		tmpNode.linkTo(tmpList.get(28));
		tmpNode.linkTo(tmpList.get(8));
		tmpNode.linkTo(tmpList.get(13));
		tmpNode.linkTo(tmpList.get(67));
		tmpNode.linkTo(tmpList.get(44));
		tmpNode.linkTo(tmpList.get(3));
		tmpNode=tmpList.get(27);
		tmpNode.linkTo(tmpList.get(53));
		tmpNode.linkTo(tmpList.get(18));
		tmpNode.linkTo(tmpList.get(44));
		tmpNode.linkTo(tmpList.get(85));
		tmpNode.linkTo(tmpList.get(50));
		tmpNode.linkTo(tmpList.get(91));
		tmpNode.linkTo(tmpList.get(15));
		tmpNode.linkTo(tmpList.get(69));
		tmpNode.linkTo(tmpList.get(41));
		tmpNode.linkTo(tmpList.get(33));
		tmpNode.linkTo(tmpList.get(59));
		tmpNode.linkTo(tmpList.get(53));
		tmpNode.linkTo(tmpList.get(88));
		tmpNode.linkTo(tmpList.get(65));
		tmpNode.linkTo(tmpList.get(34));
		tmpNode.linkTo(tmpList.get(60));
		tmpNode.linkTo(tmpList.get(79));
		tmpNode.linkTo(tmpList.get(4));
		tmpNode.linkTo(tmpList.get(90));
		tmpNode.linkTo(tmpList.get(31));
		tmpNode.linkTo(tmpList.get(51));
		tmpNode.linkTo(tmpList.get(98));
		tmpNode.linkTo(tmpList.get(62));
		tmpNode.linkTo(tmpList.get(58));
		tmpNode.linkTo(tmpList.get(91));
		tmpNode=tmpList.get(58);
		tmpNode.linkTo(tmpList.get(50));
		tmpNode.linkTo(tmpList.get(96));
		tmpNode.linkTo(tmpList.get(2));
		tmpNode.linkTo(tmpList.get(67));
		tmpNode.linkTo(tmpList.get(67));
		tmpNode.linkTo(tmpList.get(92));
		tmpNode.linkTo(tmpList.get(37));
		tmpNode.linkTo(tmpList.get(59));
		tmpNode.linkTo(tmpList.get(7));
		tmpNode.linkTo(tmpList.get(30));
		tmpNode.linkTo(tmpList.get(23));
		tmpNode.linkTo(tmpList.get(24));
		tmpNode.linkTo(tmpList.get(43));
		tmpNode.linkTo(tmpList.get(88));
		tmpNode.linkTo(tmpList.get(81));
		tmpNode.linkTo(tmpList.get(14));
		tmpNode.linkTo(tmpList.get(67));
		tmpNode.linkTo(tmpList.get(52));
		tmpNode.linkTo(tmpList.get(54));
		tmpNode.linkTo(tmpList.get(62));
		tmpNode.linkTo(tmpList.get(50));
		tmpNode.linkTo(tmpList.get(71));
		tmpNode.linkTo(tmpList.get(26));
		tmpNode.linkTo(tmpList.get(74));
		tmpNode.linkTo(tmpList.get(70));
		tmpNode=tmpList.get(67);
		tmpNode.linkTo(tmpList.get(98));
		tmpNode.linkTo(tmpList.get(56));
		tmpNode.linkTo(tmpList.get(15));
		tmpNode.linkTo(tmpList.get(13));
		tmpNode.linkTo(tmpList.get(88));
		tmpNode.linkTo(tmpList.get(88));
		tmpNode.linkTo(tmpList.get(99));
		tmpNode.linkTo(tmpList.get(36));
		tmpNode.linkTo(tmpList.get(19));
		tmpNode.linkTo(tmpList.get(20));
		tmpNode.linkTo(tmpList.get(23));
		tmpNode.linkTo(tmpList.get(81));
		tmpNode.linkTo(tmpList.get(70));
		tmpNode.linkTo(tmpList.get(16));
		tmpNode.linkTo(tmpList.get(45));
		tmpNode.linkTo(tmpList.get(88));
		tmpNode.linkTo(tmpList.get(58));
		tmpNode.linkTo(tmpList.get(3));
		tmpNode.linkTo(tmpList.get(71));
		tmpNode.linkTo(tmpList.get(55));
		tmpNode.linkTo(tmpList.get(86));
		tmpNode.linkTo(tmpList.get(57));
		tmpNode.linkTo(tmpList.get(47));
		tmpNode.linkTo(tmpList.get(88));
		tmpNode.linkTo(tmpList.get(61));
		tmpNode=tmpList.get(85);
		tmpNode.linkTo(tmpList.get(19));
		tmpNode.linkTo(tmpList.get(30));
		tmpNode.linkTo(tmpList.get(97));
		tmpNode.linkTo(tmpList.get(14));
		tmpNode.linkTo(tmpList.get(41));
		tmpNode.linkTo(tmpList.get(48));
		tmpNode.linkTo(tmpList.get(95));
		tmpNode.linkTo(tmpList.get(84));
		tmpNode.linkTo(tmpList.get(66));
		tmpNode.linkTo(tmpList.get(79));
		tmpNode.linkTo(tmpList.get(67));
		tmpNode.linkTo(tmpList.get(98));
		tmpNode.linkTo(tmpList.get(6));
		tmpNode.linkTo(tmpList.get(51));
		tmpNode.linkTo(tmpList.get(72));
		tmpNode.linkTo(tmpList.get(5));
		tmpNode.linkTo(tmpList.get(28));
		tmpNode.linkTo(tmpList.get(53));
		tmpNode.linkTo(tmpList.get(6));
		tmpNode.linkTo(tmpList.get(17));
		tmpNode.linkTo(tmpList.get(29));
		tmpNode.linkTo(tmpList.get(36));
		tmpNode.linkTo(tmpList.get(41));
		tmpNode.linkTo(tmpList.get(66));
		tmpNode.linkTo(tmpList.get(89));
		tmpNode=tmpList.get(8);
		tmpNode.linkTo(tmpList.get(28));
		tmpNode.linkTo(tmpList.get(69));
		tmpNode.linkTo(tmpList.get(5));
		tmpNode.linkTo(tmpList.get(39));
		tmpNode.linkTo(tmpList.get(89));
		tmpNode.linkTo(tmpList.get(29));
		tmpNode.linkTo(tmpList.get(87));
		tmpNode.linkTo(tmpList.get(35));
		tmpNode.linkTo(tmpList.get(29));
		tmpNode.linkTo(tmpList.get(32));
		tmpNode.linkTo(tmpList.get(32));
		tmpNode.linkTo(tmpList.get(70));
		tmpNode.linkTo(tmpList.get(21));
		tmpNode.linkTo(tmpList.get(20));
		tmpNode.linkTo(tmpList.get(31));
		tmpNode.linkTo(tmpList.get(81));
		tmpNode.linkTo(tmpList.get(39));
		tmpNode.linkTo(tmpList.get(19));
		tmpNode.linkTo(tmpList.get(16));
		tmpNode.linkTo(tmpList.get(17));
		tmpNode.linkTo(tmpList.get(80));
		tmpNode.linkTo(tmpList.get(60));
		tmpNode.linkTo(tmpList.get(90));
		tmpNode.linkTo(tmpList.get(12));
		tmpNode.linkTo(tmpList.get(10));
		tmpNode=tmpList.get(78);
		tmpNode.linkTo(tmpList.get(59));
		tmpNode.linkTo(tmpList.get(2));
		tmpNode.linkTo(tmpList.get(93));
		tmpNode.linkTo(tmpList.get(3));
		tmpNode.linkTo(tmpList.get(67));
		tmpNode.linkTo(tmpList.get(62));
		tmpNode.linkTo(tmpList.get(65));
		tmpNode.linkTo(tmpList.get(18));
		tmpNode.linkTo(tmpList.get(93));
		tmpNode.linkTo(tmpList.get(22));
		tmpNode.linkTo(tmpList.get(58));
		tmpNode.linkTo(tmpList.get(89));
		tmpNode.linkTo(tmpList.get(7));
		tmpNode.linkTo(tmpList.get(11));
		tmpNode.linkTo(tmpList.get(40));
		tmpNode.linkTo(tmpList.get(48));
		tmpNode.linkTo(tmpList.get(26));
		tmpNode.linkTo(tmpList.get(60));
		tmpNode.linkTo(tmpList.get(32));
		tmpNode.linkTo(tmpList.get(87));
		tmpNode.linkTo(tmpList.get(33));
		tmpNode.linkTo(tmpList.get(69));
		tmpNode.linkTo(tmpList.get(7));
		tmpNode.linkTo(tmpList.get(74));
		tmpNode.linkTo(tmpList.get(11));
		tmpNode=tmpList.get(71);
		tmpNode.linkTo(tmpList.get(78));
		tmpNode.linkTo(tmpList.get(67));
		tmpNode.linkTo(tmpList.get(46));
		tmpNode.linkTo(tmpList.get(79));
		tmpNode.linkTo(tmpList.get(77));
		tmpNode.linkTo(tmpList.get(45));
		tmpNode.linkTo(tmpList.get(39));
		tmpNode.linkTo(tmpList.get(52));
		tmpNode.linkTo(tmpList.get(72));
		tmpNode.linkTo(tmpList.get(90));
		tmpNode.linkTo(tmpList.get(5));
		tmpNode.linkTo(tmpList.get(82));
		tmpNode.linkTo(tmpList.get(55));
		tmpNode.linkTo(tmpList.get(40));
		tmpNode.linkTo(tmpList.get(33));
		tmpNode.linkTo(tmpList.get(79));
		tmpNode.linkTo(tmpList.get(24));
		tmpNode.linkTo(tmpList.get(24));
		tmpNode.linkTo(tmpList.get(6));
		tmpNode.linkTo(tmpList.get(47));
		tmpNode.linkTo(tmpList.get(75));
		tmpNode.linkTo(tmpList.get(83));
		tmpNode.linkTo(tmpList.get(28));
		tmpNode.linkTo(tmpList.get(22));
		tmpNode.linkTo(tmpList.get(91));
		tmpNode=tmpList.get(90);
		tmpNode.linkTo(tmpList.get(86));
		tmpNode.linkTo(tmpList.get(79));
		tmpNode.linkTo(tmpList.get(79));
		tmpNode.linkTo(tmpList.get(67));
		tmpNode.linkTo(tmpList.get(19));
		tmpNode.linkTo(tmpList.get(4));
		tmpNode.linkTo(tmpList.get(39));
		tmpNode.linkTo(tmpList.get(98));
		tmpNode.linkTo(tmpList.get(62));
		tmpNode.linkTo(tmpList.get(32));
		tmpNode.linkTo(tmpList.get(36));
		tmpNode.linkTo(tmpList.get(85));
		tmpNode.linkTo(tmpList.get(28));
		tmpNode.linkTo(tmpList.get(5));
		tmpNode.linkTo(tmpList.get(22));
		tmpNode.linkTo(tmpList.get(63));
		tmpNode.linkTo(tmpList.get(69));
		tmpNode.linkTo(tmpList.get(31));
		tmpNode.linkTo(tmpList.get(49));
		tmpNode.linkTo(tmpList.get(79));
		tmpNode.linkTo(tmpList.get(5));
		tmpNode.linkTo(tmpList.get(69));
		tmpNode.linkTo(tmpList.get(54));
		tmpNode.linkTo(tmpList.get(22));
		tmpNode.linkTo(tmpList.get(19));
		tmpNode=tmpList.get(74);
		tmpNode.linkTo(tmpList.get(73));
		tmpNode.linkTo(tmpList.get(90));
		tmpNode.linkTo(tmpList.get(87));
		tmpNode.linkTo(tmpList.get(0));
		tmpNode.linkTo(tmpList.get(41));
		tmpNode.linkTo(tmpList.get(12));
		tmpNode.linkTo(tmpList.get(92));
		tmpNode.linkTo(tmpList.get(60));
		tmpNode.linkTo(tmpList.get(87));
		tmpNode.linkTo(tmpList.get(82));
		tmpNode.linkTo(tmpList.get(60));
		tmpNode.linkTo(tmpList.get(96));
		tmpNode.linkTo(tmpList.get(19));
		tmpNode.linkTo(tmpList.get(57));
		tmpNode.linkTo(tmpList.get(62));
		tmpNode.linkTo(tmpList.get(69));
		tmpNode.linkTo(tmpList.get(97));
		tmpNode.linkTo(tmpList.get(84));
		tmpNode.linkTo(tmpList.get(25));
		tmpNode.linkTo(tmpList.get(69));
		tmpNode.linkTo(tmpList.get(77));
		tmpNode.linkTo(tmpList.get(5));
		tmpNode.linkTo(tmpList.get(3));
		tmpNode.linkTo(tmpList.get(0));
		tmpNode.linkTo(tmpList.get(22));
		tmpNode=tmpList.get(33);
		tmpNode.linkTo(tmpList.get(8));
		tmpNode.linkTo(tmpList.get(25));
		tmpNode.linkTo(tmpList.get(21));
		tmpNode.linkTo(tmpList.get(86));
		tmpNode.linkTo(tmpList.get(29));
		tmpNode.linkTo(tmpList.get(66));
		tmpNode.linkTo(tmpList.get(73));
		tmpNode.linkTo(tmpList.get(1));
		tmpNode.linkTo(tmpList.get(62));
		tmpNode.linkTo(tmpList.get(19));
		tmpNode.linkTo(tmpList.get(84));
		tmpNode.linkTo(tmpList.get(61));
		tmpNode.linkTo(tmpList.get(61));
		tmpNode.linkTo(tmpList.get(66));
		tmpNode.linkTo(tmpList.get(82));
		tmpNode.linkTo(tmpList.get(27));
		tmpNode.linkTo(tmpList.get(56));
		tmpNode.linkTo(tmpList.get(90));
		tmpNode.linkTo(tmpList.get(4));
		tmpNode.linkTo(tmpList.get(60));
		tmpNode.linkTo(tmpList.get(73));
		tmpNode.linkTo(tmpList.get(84));
		tmpNode.linkTo(tmpList.get(30));
		tmpNode.linkTo(tmpList.get(29));
		tmpNode.linkTo(tmpList.get(32));
		tmpNode=tmpList.get(72);
		tmpNode.linkTo(tmpList.get(65));
		tmpNode.linkTo(tmpList.get(16));
		tmpNode.linkTo(tmpList.get(78));
		tmpNode.linkTo(tmpList.get(55));
		tmpNode.linkTo(tmpList.get(30));
		tmpNode.linkTo(tmpList.get(17));
		tmpNode.linkTo(tmpList.get(4));
		tmpNode.linkTo(tmpList.get(33));
		tmpNode.linkTo(tmpList.get(81));
		tmpNode.linkTo(tmpList.get(29));
		tmpNode.linkTo(tmpList.get(59));
		tmpNode.linkTo(tmpList.get(64));
		tmpNode.linkTo(tmpList.get(8));
		tmpNode.linkTo(tmpList.get(97));
		tmpNode.linkTo(tmpList.get(91));
		tmpNode.linkTo(tmpList.get(5));
		tmpNode.linkTo(tmpList.get(97));
		tmpNode.linkTo(tmpList.get(66));
		tmpNode.linkTo(tmpList.get(43));
		tmpNode.linkTo(tmpList.get(0));
		tmpNode.linkTo(tmpList.get(99));
		tmpNode.linkTo(tmpList.get(96));
		tmpNode.linkTo(tmpList.get(52));
		tmpNode.linkTo(tmpList.get(1));
		tmpNode.linkTo(tmpList.get(43));
		tmpNode=tmpList.get(22);
		tmpNode.linkTo(tmpList.get(57));
		tmpNode.linkTo(tmpList.get(59));
		tmpNode.linkTo(tmpList.get(0));
		tmpNode.linkTo(tmpList.get(53));
		tmpNode.linkTo(tmpList.get(46));
		tmpNode.linkTo(tmpList.get(80));
		tmpNode.linkTo(tmpList.get(72));
		tmpNode.linkTo(tmpList.get(93));
		tmpNode.linkTo(tmpList.get(48));
		tmpNode.linkTo(tmpList.get(5));
		tmpNode.linkTo(tmpList.get(8));
		tmpNode.linkTo(tmpList.get(42));
		tmpNode.linkTo(tmpList.get(36));
		tmpNode.linkTo(tmpList.get(69));
		tmpNode.linkTo(tmpList.get(95));
		tmpNode.linkTo(tmpList.get(44));
		tmpNode.linkTo(tmpList.get(51));
		tmpNode.linkTo(tmpList.get(48));
		tmpNode.linkTo(tmpList.get(92));
		tmpNode.linkTo(tmpList.get(32));
		tmpNode.linkTo(tmpList.get(17));
		tmpNode.linkTo(tmpList.get(47));
		tmpNode.linkTo(tmpList.get(57));
		tmpNode.linkTo(tmpList.get(21));
		tmpNode.linkTo(tmpList.get(38));
		tmpNode=tmpList.get(57);
		tmpNode.linkTo(tmpList.get(25));
		tmpNode.linkTo(tmpList.get(58));
		tmpNode.linkTo(tmpList.get(6));
		tmpNode.linkTo(tmpList.get(2));
		tmpNode.linkTo(tmpList.get(28));
		tmpNode.linkTo(tmpList.get(34));
		tmpNode.linkTo(tmpList.get(5));
		tmpNode.linkTo(tmpList.get(68));
		tmpNode.linkTo(tmpList.get(56));
		tmpNode.linkTo(tmpList.get(3));
		tmpNode.linkTo(tmpList.get(53));
		tmpNode.linkTo(tmpList.get(83));
		tmpNode.linkTo(tmpList.get(25));
		tmpNode.linkTo(tmpList.get(51));
		tmpNode.linkTo(tmpList.get(39));
		tmpNode.linkTo(tmpList.get(22));
		tmpNode.linkTo(tmpList.get(81));
		tmpNode.linkTo(tmpList.get(2));
		tmpNode.linkTo(tmpList.get(77));
		tmpNode.linkTo(tmpList.get(68));
		tmpNode.linkTo(tmpList.get(95));
		tmpNode.linkTo(tmpList.get(60));
		tmpNode.linkTo(tmpList.get(70));
		tmpNode.linkTo(tmpList.get(7));
		tmpNode.linkTo(tmpList.get(26));
		tmpNode=tmpList.get(73);
		tmpNode.linkTo(tmpList.get(59));
		tmpNode.linkTo(tmpList.get(63));
		tmpNode.linkTo(tmpList.get(94));
		tmpNode.linkTo(tmpList.get(37));
		tmpNode.linkTo(tmpList.get(40));
		tmpNode.linkTo(tmpList.get(89));
		tmpNode.linkTo(tmpList.get(99));
		tmpNode.linkTo(tmpList.get(88));
		tmpNode.linkTo(tmpList.get(12));
		tmpNode.linkTo(tmpList.get(49));
		tmpNode.linkTo(tmpList.get(16));
		tmpNode.linkTo(tmpList.get(83));
		tmpNode.linkTo(tmpList.get(3));
		tmpNode.linkTo(tmpList.get(92));
		tmpNode.linkTo(tmpList.get(4));
		tmpNode.linkTo(tmpList.get(16));
		tmpNode.linkTo(tmpList.get(34));
		tmpNode.linkTo(tmpList.get(66));
		tmpNode.linkTo(tmpList.get(79));
		tmpNode.linkTo(tmpList.get(60));
		tmpNode.linkTo(tmpList.get(11));
		tmpNode.linkTo(tmpList.get(58));
		tmpNode.linkTo(tmpList.get(33));
		tmpNode.linkTo(tmpList.get(57));
		tmpNode.linkTo(tmpList.get(10));
		tmpNode=tmpList.get(7);
		tmpNode.linkTo(tmpList.get(0));
		tmpNode.linkTo(tmpList.get(34));
		tmpNode.linkTo(tmpList.get(27));
		tmpNode.linkTo(tmpList.get(35));
		tmpNode.linkTo(tmpList.get(35));
		tmpNode.linkTo(tmpList.get(64));
		tmpNode.linkTo(tmpList.get(32));
		tmpNode.linkTo(tmpList.get(26));
		tmpNode.linkTo(tmpList.get(72));
		tmpNode.linkTo(tmpList.get(36));
		tmpNode.linkTo(tmpList.get(60));
		tmpNode.linkTo(tmpList.get(56));
		tmpNode.linkTo(tmpList.get(90));
		tmpNode.linkTo(tmpList.get(21));
		tmpNode.linkTo(tmpList.get(60));
		tmpNode.linkTo(tmpList.get(92));
		tmpNode.linkTo(tmpList.get(99));
		tmpNode.linkTo(tmpList.get(18));
		tmpNode.linkTo(tmpList.get(85));
		tmpNode.linkTo(tmpList.get(40));
		tmpNode.linkTo(tmpList.get(68));
		tmpNode.linkTo(tmpList.get(33));
		tmpNode.linkTo(tmpList.get(96));
		tmpNode.linkTo(tmpList.get(62));
		tmpNode.linkTo(tmpList.get(19));
		tmpNode=tmpList.get(23);
		tmpNode.linkTo(tmpList.get(43));
		tmpNode.linkTo(tmpList.get(29));
		tmpNode.linkTo(tmpList.get(0));
		tmpNode.linkTo(tmpList.get(46));
		tmpNode.linkTo(tmpList.get(3));
		tmpNode.linkTo(tmpList.get(59));
		tmpNode.linkTo(tmpList.get(16));
		tmpNode.linkTo(tmpList.get(99));
		tmpNode.linkTo(tmpList.get(44));
		tmpNode.linkTo(tmpList.get(2));
		tmpNode.linkTo(tmpList.get(87));
		tmpNode.linkTo(tmpList.get(41));
		tmpNode.linkTo(tmpList.get(38));
		tmpNode.linkTo(tmpList.get(39));
		tmpNode.linkTo(tmpList.get(73));
		tmpNode.linkTo(tmpList.get(5));
		tmpNode.linkTo(tmpList.get(14));
		tmpNode.linkTo(tmpList.get(5));
		tmpNode.linkTo(tmpList.get(74));
		tmpNode.linkTo(tmpList.get(36));
		tmpNode.linkTo(tmpList.get(82));
		tmpNode.linkTo(tmpList.get(99));
		tmpNode.linkTo(tmpList.get(94));
		tmpNode.linkTo(tmpList.get(34));
		tmpNode.linkTo(tmpList.get(83));
		tmpNode=tmpList.get(7);
		tmpNode.linkTo(tmpList.get(41));
		tmpNode.linkTo(tmpList.get(24));
		tmpNode.linkTo(tmpList.get(66));
		tmpNode.linkTo(tmpList.get(92));
		tmpNode.linkTo(tmpList.get(19));
		tmpNode.linkTo(tmpList.get(87));
		tmpNode.linkTo(tmpList.get(72));
		tmpNode.linkTo(tmpList.get(92));
		tmpNode.linkTo(tmpList.get(91));
		tmpNode.linkTo(tmpList.get(4));
		tmpNode.linkTo(tmpList.get(30));
		tmpNode.linkTo(tmpList.get(82));
		tmpNode.linkTo(tmpList.get(7));
		tmpNode.linkTo(tmpList.get(99));
		tmpNode.linkTo(tmpList.get(64));
		tmpNode.linkTo(tmpList.get(75));
		tmpNode.linkTo(tmpList.get(10));
		tmpNode.linkTo(tmpList.get(25));
		tmpNode.linkTo(tmpList.get(83));
		tmpNode.linkTo(tmpList.get(72));
		tmpNode.linkTo(tmpList.get(82));
		tmpNode.linkTo(tmpList.get(59));
		tmpNode.linkTo(tmpList.get(97));
		tmpNode.linkTo(tmpList.get(95));
		tmpNode.linkTo(tmpList.get(77));
		tmpNode=tmpList.get(30);
		tmpNode.linkTo(tmpList.get(82));
		tmpNode.linkTo(tmpList.get(57));
		tmpNode.linkTo(tmpList.get(6));
		tmpNode.linkTo(tmpList.get(84));
		tmpNode.linkTo(tmpList.get(44));
		tmpNode.linkTo(tmpList.get(54));
		tmpNode.linkTo(tmpList.get(3));
		tmpNode.linkTo(tmpList.get(75));
		tmpNode.linkTo(tmpList.get(70));
		tmpNode.linkTo(tmpList.get(64));
		tmpNode.linkTo(tmpList.get(80));
		tmpNode.linkTo(tmpList.get(7));
		tmpNode.linkTo(tmpList.get(18));
		tmpNode.linkTo(tmpList.get(59));
		tmpNode.linkTo(tmpList.get(3));
		tmpNode.linkTo(tmpList.get(38));
		tmpNode.linkTo(tmpList.get(77));
		tmpNode.linkTo(tmpList.get(78));
		tmpNode.linkTo(tmpList.get(97));
		tmpNode.linkTo(tmpList.get(34));
		tmpNode.linkTo(tmpList.get(63));
		tmpNode.linkTo(tmpList.get(32));
		tmpNode.linkTo(tmpList.get(62));
		tmpNode.linkTo(tmpList.get(86));
		tmpNode.linkTo(tmpList.get(7));
		tmpNode=tmpList.get(46);
		tmpNode.linkTo(tmpList.get(78));
		tmpNode.linkTo(tmpList.get(58));
		tmpNode.linkTo(tmpList.get(79));
		tmpNode.linkTo(tmpList.get(26));
		tmpNode.linkTo(tmpList.get(58));
		tmpNode.linkTo(tmpList.get(82));
		tmpNode.linkTo(tmpList.get(16));
		tmpNode.linkTo(tmpList.get(68));
		tmpNode.linkTo(tmpList.get(52));
		tmpNode.linkTo(tmpList.get(46));
		tmpNode.linkTo(tmpList.get(3));
		tmpNode.linkTo(tmpList.get(65));
		tmpNode.linkTo(tmpList.get(3));
		tmpNode.linkTo(tmpList.get(19));
		tmpNode.linkTo(tmpList.get(24));
		tmpNode.linkTo(tmpList.get(87));
		tmpNode.linkTo(tmpList.get(9));
		tmpNode.linkTo(tmpList.get(12));
		tmpNode.linkTo(tmpList.get(42));
		tmpNode.linkTo(tmpList.get(30));
		tmpNode.linkTo(tmpList.get(60));
		tmpNode.linkTo(tmpList.get(77));
		tmpNode.linkTo(tmpList.get(31));
		tmpNode.linkTo(tmpList.get(20));
		tmpNode.linkTo(tmpList.get(45));
		tmpNode=tmpList.get(40);
		tmpNode.linkTo(tmpList.get(81));
		tmpNode.linkTo(tmpList.get(52));
		tmpNode.linkTo(tmpList.get(39));
		tmpNode.linkTo(tmpList.get(98));
		tmpNode.linkTo(tmpList.get(72));
		tmpNode.linkTo(tmpList.get(95));
		tmpNode.linkTo(tmpList.get(27));
		tmpNode.linkTo(tmpList.get(6));
		tmpNode.linkTo(tmpList.get(12));
		tmpNode.linkTo(tmpList.get(35));
		tmpNode.linkTo(tmpList.get(36));
		tmpNode.linkTo(tmpList.get(97));
		tmpNode.linkTo(tmpList.get(7));
		tmpNode.linkTo(tmpList.get(54));
		tmpNode.linkTo(tmpList.get(59));
		tmpNode.linkTo(tmpList.get(14));
		tmpNode.linkTo(tmpList.get(8));
		tmpNode.linkTo(tmpList.get(82));
		tmpNode.linkTo(tmpList.get(98));
		tmpNode.linkTo(tmpList.get(62));
		tmpNode.linkTo(tmpList.get(98));
		tmpNode.linkTo(tmpList.get(57));
		tmpNode.linkTo(tmpList.get(28));
		tmpNode.linkTo(tmpList.get(5));
		tmpNode.linkTo(tmpList.get(24));
		tmpNode=tmpList.get(73);
		tmpNode.linkTo(tmpList.get(36));
		tmpNode.linkTo(tmpList.get(71));
		tmpNode.linkTo(tmpList.get(89));
		tmpNode.linkTo(tmpList.get(0));
		tmpNode.linkTo(tmpList.get(96));
		tmpNode.linkTo(tmpList.get(23));
		tmpNode.linkTo(tmpList.get(92));
		tmpNode.linkTo(tmpList.get(26));
		tmpNode.linkTo(tmpList.get(5));
		tmpNode.linkTo(tmpList.get(90));
		tmpNode.linkTo(tmpList.get(86));
		tmpNode.linkTo(tmpList.get(15));
		tmpNode.linkTo(tmpList.get(49));
		tmpNode.linkTo(tmpList.get(6));
		tmpNode.linkTo(tmpList.get(92));
		tmpNode.linkTo(tmpList.get(93));
		tmpNode.linkTo(tmpList.get(40));
		tmpNode.linkTo(tmpList.get(33));
		tmpNode.linkTo(tmpList.get(69));
		tmpNode.linkTo(tmpList.get(56));
		tmpNode.linkTo(tmpList.get(57));
		tmpNode.linkTo(tmpList.get(97));
		tmpNode.linkTo(tmpList.get(34));
		tmpNode.linkTo(tmpList.get(44));
		tmpNode.linkTo(tmpList.get(6));
		tmpNode=tmpList.get(30);
		tmpNode.linkTo(tmpList.get(58));
		tmpNode.linkTo(tmpList.get(83));
		tmpNode.linkTo(tmpList.get(43));
		tmpNode.linkTo(tmpList.get(21));
		tmpNode.linkTo(tmpList.get(46));
		tmpNode.linkTo(tmpList.get(50));
		tmpNode.linkTo(tmpList.get(75));
		tmpNode.linkTo(tmpList.get(40));
		tmpNode.linkTo(tmpList.get(68));
		tmpNode.linkTo(tmpList.get(75));
		tmpNode.linkTo(tmpList.get(4));
		tmpNode.linkTo(tmpList.get(71));
		tmpNode.linkTo(tmpList.get(9));
		tmpNode.linkTo(tmpList.get(44));
		tmpNode.linkTo(tmpList.get(75));
		tmpNode.linkTo(tmpList.get(30));
		tmpNode.linkTo(tmpList.get(92));
		tmpNode.linkTo(tmpList.get(91));
		tmpNode.linkTo(tmpList.get(0));
		tmpNode.linkTo(tmpList.get(60));
		tmpNode.linkTo(tmpList.get(35));
		tmpNode.linkTo(tmpList.get(23));
		tmpNode.linkTo(tmpList.get(43));
		tmpNode.linkTo(tmpList.get(99));
		tmpNode.linkTo(tmpList.get(84));
		tmpNode=tmpList.get(33);
		tmpNode.linkTo(tmpList.get(50));
		tmpNode.linkTo(tmpList.get(0));
		tmpNode.linkTo(tmpList.get(77));
		tmpNode.linkTo(tmpList.get(84));
		tmpNode.linkTo(tmpList.get(72));
		tmpNode.linkTo(tmpList.get(82));
		tmpNode.linkTo(tmpList.get(46));
		tmpNode.linkTo(tmpList.get(43));
		tmpNode.linkTo(tmpList.get(48));
		tmpNode.linkTo(tmpList.get(46));
		tmpNode.linkTo(tmpList.get(73));
		tmpNode.linkTo(tmpList.get(12));
		tmpNode.linkTo(tmpList.get(57));
		tmpNode.linkTo(tmpList.get(93));
		tmpNode.linkTo(tmpList.get(95));
		tmpNode.linkTo(tmpList.get(19));
		tmpNode.linkTo(tmpList.get(89));
		tmpNode.linkTo(tmpList.get(90));
		tmpNode.linkTo(tmpList.get(46));
		tmpNode.linkTo(tmpList.get(95));
		tmpNode.linkTo(tmpList.get(7));
		tmpNode.linkTo(tmpList.get(59));
		tmpNode.linkTo(tmpList.get(50));
		tmpNode.linkTo(tmpList.get(86));
		tmpNode.linkTo(tmpList.get(73));
		tmpNode=tmpList.get(71);
		tmpNode.linkTo(tmpList.get(36));
		tmpNode.linkTo(tmpList.get(56));
		tmpNode.linkTo(tmpList.get(63));
		tmpNode.linkTo(tmpList.get(29));
		tmpNode.linkTo(tmpList.get(43));
		tmpNode.linkTo(tmpList.get(51));
		tmpNode.linkTo(tmpList.get(14));
		tmpNode.linkTo(tmpList.get(68));
		tmpNode.linkTo(tmpList.get(85));
		tmpNode.linkTo(tmpList.get(8));
		tmpNode.linkTo(tmpList.get(20));
		tmpNode.linkTo(tmpList.get(13));
		tmpNode.linkTo(tmpList.get(48));
		tmpNode.linkTo(tmpList.get(47));
		tmpNode.linkTo(tmpList.get(7));
		tmpNode.linkTo(tmpList.get(25));
		tmpNode.linkTo(tmpList.get(55));
		tmpNode.linkTo(tmpList.get(5));
		tmpNode.linkTo(tmpList.get(36));
		tmpNode.linkTo(tmpList.get(65));
		tmpNode.linkTo(tmpList.get(11));
		tmpNode.linkTo(tmpList.get(29));
		tmpNode.linkTo(tmpList.get(39));
		tmpNode.linkTo(tmpList.get(50));
		tmpNode.linkTo(tmpList.get(19));
		tmpNode=tmpList.get(40);
		tmpNode.linkTo(tmpList.get(52));
		tmpNode.linkTo(tmpList.get(87));
		tmpNode.linkTo(tmpList.get(51));
		tmpNode.linkTo(tmpList.get(41));
		tmpNode.linkTo(tmpList.get(37));
		tmpNode.linkTo(tmpList.get(35));
		tmpNode.linkTo(tmpList.get(93));
		tmpNode.linkTo(tmpList.get(78));
		tmpNode.linkTo(tmpList.get(31));
		tmpNode.linkTo(tmpList.get(13));
		tmpNode.linkTo(tmpList.get(62));
		tmpNode.linkTo(tmpList.get(80));
		tmpNode.linkTo(tmpList.get(17));
		tmpNode.linkTo(tmpList.get(16));
		tmpNode.linkTo(tmpList.get(70));
		tmpNode.linkTo(tmpList.get(28));
		tmpNode.linkTo(tmpList.get(36));
		tmpNode.linkTo(tmpList.get(94));
		tmpNode.linkTo(tmpList.get(6));
		tmpNode.linkTo(tmpList.get(79));
		tmpNode.linkTo(tmpList.get(68));
		tmpNode.linkTo(tmpList.get(7));
		tmpNode.linkTo(tmpList.get(6));
		tmpNode.linkTo(tmpList.get(97));
		tmpNode.linkTo(tmpList.get(38));
		tmpNode=tmpList.get(3);
		tmpNode.linkTo(tmpList.get(83));
		tmpNode.linkTo(tmpList.get(24));
		tmpNode.linkTo(tmpList.get(99));
		tmpNode.linkTo(tmpList.get(55));
		tmpNode.linkTo(tmpList.get(15));
		tmpNode.linkTo(tmpList.get(77));
		tmpNode.linkTo(tmpList.get(11));
		tmpNode.linkTo(tmpList.get(25));
		tmpNode.linkTo(tmpList.get(1));
		tmpNode.linkTo(tmpList.get(69));
		tmpNode.linkTo(tmpList.get(31));
		tmpNode.linkTo(tmpList.get(55));
		tmpNode.linkTo(tmpList.get(57));
		tmpNode.linkTo(tmpList.get(65));
		tmpNode.linkTo(tmpList.get(40));
		tmpNode.linkTo(tmpList.get(36));
		tmpNode.linkTo(tmpList.get(68));
		tmpNode.linkTo(tmpList.get(51));
		tmpNode.linkTo(tmpList.get(28));
		tmpNode.linkTo(tmpList.get(92));
		tmpNode.linkTo(tmpList.get(57));
		tmpNode.linkTo(tmpList.get(16));
		tmpNode.linkTo(tmpList.get(85));
		tmpNode.linkTo(tmpList.get(26));
		tmpNode.linkTo(tmpList.get(39));
		tmpNode=tmpList.get(43);
		tmpNode.linkTo(tmpList.get(58));
		tmpNode.linkTo(tmpList.get(2));
		tmpNode.linkTo(tmpList.get(75));
		tmpNode.linkTo(tmpList.get(35));
		tmpNode.linkTo(tmpList.get(4));
		tmpNode.linkTo(tmpList.get(23));
		tmpNode.linkTo(tmpList.get(30));
		tmpNode.linkTo(tmpList.get(23));
		tmpNode.linkTo(tmpList.get(60));
		tmpNode.linkTo(tmpList.get(14));
		tmpNode.linkTo(tmpList.get(19));
		tmpNode.linkTo(tmpList.get(62));
		tmpNode.linkTo(tmpList.get(31));
		tmpNode.linkTo(tmpList.get(53));
		tmpNode.linkTo(tmpList.get(61));
		tmpNode.linkTo(tmpList.get(22));
		tmpNode.linkTo(tmpList.get(83));
		tmpNode.linkTo(tmpList.get(5));
		tmpNode.linkTo(tmpList.get(95));
		tmpNode.linkTo(tmpList.get(73));
		tmpNode.linkTo(tmpList.get(4));
		tmpNode.linkTo(tmpList.get(83));
		tmpNode.linkTo(tmpList.get(18));
		tmpNode.linkTo(tmpList.get(45));
		tmpNode.linkTo(tmpList.get(20));
		tmpNode=tmpList.get(29);
		tmpNode.linkTo(tmpList.get(2));
		tmpNode.linkTo(tmpList.get(5));
		tmpNode.linkTo(tmpList.get(22));
		tmpNode.linkTo(tmpList.get(96));
		tmpNode.linkTo(tmpList.get(14));
		tmpNode.linkTo(tmpList.get(35));
		tmpNode.linkTo(tmpList.get(73));
		tmpNode.linkTo(tmpList.get(97));
		tmpNode.linkTo(tmpList.get(21));
		tmpNode.linkTo(tmpList.get(41));
		tmpNode.linkTo(tmpList.get(1));
		tmpNode.linkTo(tmpList.get(83));
		tmpNode.linkTo(tmpList.get(3));
		tmpNode.linkTo(tmpList.get(42));
		tmpNode.linkTo(tmpList.get(76));
		tmpNode.linkTo(tmpList.get(35));
		tmpNode.linkTo(tmpList.get(63));
		tmpNode.linkTo(tmpList.get(74));
		tmpNode.linkTo(tmpList.get(72));
		tmpNode.linkTo(tmpList.get(36));
		tmpNode.linkTo(tmpList.get(70));
		tmpNode.linkTo(tmpList.get(39));
		tmpNode.linkTo(tmpList.get(18));
		tmpNode.linkTo(tmpList.get(3));
		tmpNode.linkTo(tmpList.get(40));
		tmpNode=tmpList.get(44);
		tmpNode.linkTo(tmpList.get(82));
		tmpNode.linkTo(tmpList.get(9));
		tmpNode.linkTo(tmpList.get(58));
		tmpNode.linkTo(tmpList.get(59));
		tmpNode.linkTo(tmpList.get(93));
		tmpNode.linkTo(tmpList.get(80));
		tmpNode.linkTo(tmpList.get(72));
		tmpNode.linkTo(tmpList.get(91));
		tmpNode.linkTo(tmpList.get(82));
		tmpNode.linkTo(tmpList.get(73));
		tmpNode.linkTo(tmpList.get(66));
		tmpNode.linkTo(tmpList.get(8));
		tmpNode.linkTo(tmpList.get(18));
		tmpNode.linkTo(tmpList.get(60));
		tmpNode.linkTo(tmpList.get(13));
		tmpNode.linkTo(tmpList.get(62));
		tmpNode.linkTo(tmpList.get(49));
		tmpNode.linkTo(tmpList.get(81));
		tmpNode.linkTo(tmpList.get(35));
		tmpNode.linkTo(tmpList.get(55));
		tmpNode.linkTo(tmpList.get(7));
		tmpNode.linkTo(tmpList.get(19));
		tmpNode.linkTo(tmpList.get(15));
		tmpNode.linkTo(tmpList.get(19));
		tmpNode.linkTo(tmpList.get(77));
		tmpNode=tmpList.get(73);
		tmpNode.linkTo(tmpList.get(28));
		tmpNode.linkTo(tmpList.get(23));
		tmpNode.linkTo(tmpList.get(4));
		tmpNode.linkTo(tmpList.get(85));
		tmpNode.linkTo(tmpList.get(52));
		tmpNode.linkTo(tmpList.get(18));
		tmpNode.linkTo(tmpList.get(40));
		tmpNode.linkTo(tmpList.get(31));
		tmpNode.linkTo(tmpList.get(28));
		tmpNode.linkTo(tmpList.get(42));
		tmpNode.linkTo(tmpList.get(37));
		tmpNode.linkTo(tmpList.get(83));
		tmpNode.linkTo(tmpList.get(99));
		tmpNode.linkTo(tmpList.get(71));
		tmpNode.linkTo(tmpList.get(47));
		tmpNode.linkTo(tmpList.get(77));
		tmpNode.linkTo(tmpList.get(23));
		tmpNode.linkTo(tmpList.get(65));
		tmpNode.linkTo(tmpList.get(81));
		tmpNode.linkTo(tmpList.get(49));
		tmpNode.linkTo(tmpList.get(68));
		tmpNode.linkTo(tmpList.get(72));
		tmpNode.linkTo(tmpList.get(60));
		tmpNode.linkTo(tmpList.get(79));
		tmpNode.linkTo(tmpList.get(32));
		tmpNode=tmpList.get(23);
		tmpNode.linkTo(tmpList.get(44));
		tmpNode.linkTo(tmpList.get(81));
		tmpNode.linkTo(tmpList.get(97));
		tmpNode.linkTo(tmpList.get(60));
		tmpNode.linkTo(tmpList.get(63));
		tmpNode.linkTo(tmpList.get(93));
		tmpNode.linkTo(tmpList.get(32));
		tmpNode.linkTo(tmpList.get(46));
		tmpNode.linkTo(tmpList.get(6));
		tmpNode.linkTo(tmpList.get(8));
		tmpNode.linkTo(tmpList.get(95));
		tmpNode.linkTo(tmpList.get(28));
		tmpNode.linkTo(tmpList.get(49));
		tmpNode.linkTo(tmpList.get(42));
		tmpNode.linkTo(tmpList.get(37));
		tmpNode.linkTo(tmpList.get(51));
		tmpNode.linkTo(tmpList.get(53));
		tmpNode.linkTo(tmpList.get(80));
		tmpNode.linkTo(tmpList.get(64));
		tmpNode.linkTo(tmpList.get(55));
		tmpNode.linkTo(tmpList.get(47));
		tmpNode.linkTo(tmpList.get(27));
		tmpNode.linkTo(tmpList.get(10));
		tmpNode.linkTo(tmpList.get(43));
		tmpNode.linkTo(tmpList.get(93));
		tmpNode=tmpList.get(6);
		tmpNode.linkTo(tmpList.get(75));
		tmpNode.linkTo(tmpList.get(36));
		tmpNode.linkTo(tmpList.get(15));
		tmpNode.linkTo(tmpList.get(97));
		tmpNode.linkTo(tmpList.get(38));
		tmpNode.linkTo(tmpList.get(41));
		tmpNode.linkTo(tmpList.get(97));
		tmpNode.linkTo(tmpList.get(97));
		tmpNode.linkTo(tmpList.get(48));
		tmpNode.linkTo(tmpList.get(27));
		tmpNode.linkTo(tmpList.get(70));
		tmpNode.linkTo(tmpList.get(70));
		tmpNode.linkTo(tmpList.get(75));
		tmpNode.linkTo(tmpList.get(53));
		tmpNode.linkTo(tmpList.get(2));
		tmpNode.linkTo(tmpList.get(65));
		tmpNode.linkTo(tmpList.get(86));
		tmpNode.linkTo(tmpList.get(44));
		tmpNode.linkTo(tmpList.get(20));
		tmpNode.linkTo(tmpList.get(71));
		tmpNode.linkTo(tmpList.get(79));
		tmpNode.linkTo(tmpList.get(88));
		tmpNode.linkTo(tmpList.get(54));
		tmpNode.linkTo(tmpList.get(86));
		tmpNode.linkTo(tmpList.get(69));

		// calculate the paths
		ShortestPathFinder spf=new ShortestPathFinder();
		spf.calculatePaths(tmpList);
	}

	class StringNode extends AbstractSPNode<StringNode> {
		String str=null;

		public StringNode(String s) {
			super();
			if(s == null) {
				throw new NullPointerException("s can't be null");
			}
			str=s;
		}

		@Override
		public String toString() {
			return("{StringNode " + str + "}");
		}

		public String getString() {
			return(str);
		}

		@Override
		public void linkTo(StringNode x, int cost) {
			super.linkTo(x, cost);
		}

		@Override
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

		@SuppressWarnings("unchecked")
		public void dump() {
			Set s=new java.util.HashSet();
			dump(0, s);
		}

		@SuppressWarnings("unchecked")
		public void dump(int indentation, Set s) {
			System.out.println(indent(indentation) + this);
			// Find out what we have maps to
			for(StringNode sn : nodes.values()) {
				SPVertex vert=getNextHop(sn);
				if(vert != null) {
					System.out.println(indent(indentation + 4)
						+ " --> " + sn.getString() + " via "
						+ vert.getTo() + "@" + vert.getCost());
				}
			}
			// Dump out the connections
			for(SPVertex spv : getConnections()) {
				if (! s.contains(spv.getTo())) {
					s.add(spv.getTo());
					StringNode sn=(StringNode)spv.getTo();
					sn.dump(indentation+2, s);
				}
			}
		}

		@Override
		public int hashCode() {
			return(str.hashCode());
		}

		public int compareTo(StringNode sn) {
			return (str.compareTo(sn.getString()));
		}
	}

}
