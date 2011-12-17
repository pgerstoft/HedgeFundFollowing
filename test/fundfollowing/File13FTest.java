package fundfollowing;

import java.io.File;

import org.junit.Test;


import static org.junit.Assert.assertEquals;

	
public class File13FTest {
	@Test
	public void testMatches(){
		File13F tester;

		//Seven digit cusips
		tester = new File13F(new File("test/0001164508-10-000002.txt"));
		assertEquals("Result", "Default ", tester.getMatchType());
		
		tester = new File13F(new File("test/0000911084-10-000001.txt"));
		assertEquals("Result", "Default with Additional Value Shares on Separate Lines ", tester.getMatchType());
		
		tester = new File13F(new File("test/0000002230-10-000031.txt"));
		assertEquals("Result", "Default ", tester.getMatchType());
		
		tester = new File13F(new File("test/0000893838-10-000008.txt"));
		assertEquals("Result", "Default ", tester.getMatchType());
		
		tester = new File13F(new File("test/0000872162-10-000002.txt"));
		assertEquals("Result", "Sole Shares Switched ", tester.getMatchType());
		
		tester = new File13F(new File("test/0000891092-10-000466.txt"));
		assertEquals("Result", "Name Cusip Switched ", tester.getMatchType());
		
		tester = new File13F(new File("test/0000891092-10-000570.txt"));
		assertEquals("Result", "Six Two One ", tester.getMatchType());
		
		tester = new File13F(new File("test/0000950123-10-013284.txt"));
		assertEquals("Result", "Six Two One with Additional Value Shares on Separate Lines ", tester.getMatchType());
		
		tester = new File13F(new File("test/0000897101-10-000204.txt"));
		assertEquals("Result", "Name Cusip Switched ", tester.getMatchType());
		
		tester = new File13F(new File("test/0000905770-10-000003.txt"));
		assertEquals("Result", "SH Sole Shares Switched ", tester.getMatchType());

		tester = new File13F(new File("test/0001011814-10-000001.txt"));
		assertEquals("Result", "Default with Mashed Nine Digit Cusip ", tester.getMatchType());
		
		tester = new File13F(new File("test/0001002672-11-000001.txt"));
		assertEquals("Result", "Name Ticker Cusip Switched ", tester.getMatchType());
	}
}
