package net.spy.concurrent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

/**
 * Test the mapper.
 */
public class MapperTest extends TestCase {

	public void testSimpleTransform() throws Exception {
		Mapper<Integer, Integer> m=new Mapper<Integer, Integer>();
		List<Integer> in=Arrays.asList(1, 2, 3, 4, 5);
		List<Integer> out=m.transform(new Transformer<Integer, Integer>(){
			public Integer transform(Integer num) {
				return num + 1;
			}},
			in);
		assertEquals(new ArrayList<Integer>(Arrays.asList(2, 3, 4, 5, 6)),
				out);
	}
}
