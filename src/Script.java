import java.io.IOException;


public class Script {

	public static void main(String[] args){
		//Download SEC data
		SECData inst = SECData.getInstance();
//		System.out.println(inst.downloadSEC13Fs());
		String quarterDir = "2010/QTR1/";
//		inst.get13Fs(quarterDir);
		try {
			
		inst.formatSEC13Fs(quarterDir);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
//		inst.createCompanyIdx13F();
		//Format Data
		//get top twenty stocks
 catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}

/*Mathematical Statistics and Data Analysis (Hardcover)
Mathematical Statistics and Data Analysis (Hardcover)
Mathematical Statistics and Data Analysis (Hardcover)
Mathematical Statistics and Data Analysis (Hardcover)
Mathematical Statistics and Data Analysis (Hardcover)
Mathematical Statistics and Data Analysis (Hardcover)
Mathematical Statistics and Data Analysis (Hardcover)
Mathematical Statistics and Data Analysis (Hardcover)
File13F.numClaimedHoldings
 * Download Data Check
 * Get Data from Files
 * Store Data - in  a .csv First Name, Year Quarter, CUSIP, Ticker, 
 */
