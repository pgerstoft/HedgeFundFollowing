package fundfollowing;

import org.junit.Test;



import static org.junit.Assert.assertEquals;


public class HoldingTest {

	@Test
	public void testCompareTo(){
		Holding h1 = new Holding(4, 2);
		Holding h2 = new Holding(4, 2);
		Holding h3 = new Holding(5, 3);
		Holding h4 = new Holding(1, 4);
		
		assertEquals("Result", 0, h1.compareTo(h2));
		assertEquals("Result", -1, h1.compareTo(h3));
		assertEquals("Result", 1, h1.compareTo(h4));
	}

}
