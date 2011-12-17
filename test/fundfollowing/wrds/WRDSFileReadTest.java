package fundfollowing.wrds;

	import org.junit.Test;


import static org.junit.Assert.assertEquals;

public class WRDSFileReadTest {


		@Test
		public void testwrdsQTRFormatToqtrDirFormat(){
			System.out.println(WRDSFileRead.wrdsQTRFormatToqtrDirFormat("2009Q1"));
			assertEquals("Result", WRDSFileRead.wrdsQTRFormatToqtrDirFormat("2009Q1"), "2009/QTR1/");
			
		}
}
