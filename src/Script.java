import java.io.IOException;


public class Script {

	public static void main(String[] args){
		//Download SEC data
		SECData inst = SECData.getInstance();
		String quarterDir = "2010/QTR1/";
		try {
			inst.downloadCurrentSEC13Fs(true);
		} catch (IOException e) {
			e.printStackTrace();
		}	
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
	}
}
/*
 * Download Data X
 * Get Data from Files
 * Store Data - in  a .csv First Name, Year Quarter, CUSIP, Ticker, 
 */
