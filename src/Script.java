import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;


public class Script {

	public static void main(String[] args){
		//Download SEC data
		SECData inst = SECData.getInstance();
		String quarterDir = "2009/QTR2/";
		WRDSFileRead freader = new WRDSFileRead();
		String file = "93dbde77c0e270ba.csv";
	//	freader.parseFile(file);
		try {
			inst.downloadAndStoreSEC13Fs(quarterDir, true);
		} catch (IOException e) {
			e.printStackTrace();
		}	
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void getMostConcentrated(String quarter){
		ArrayList<String> cusips = DB.getCusips(quarter);
		//get Shares Held
		Hashtable<String, Double> cusipToSharesHeld = DB.getNumSharesHeldByFunds(quarter);
		String previousQuarter = getPreviousQuarter(quarter);
		Hashtable<String, Double> cusipsToSharesOutStanding = DB.getSharesOustandingAllCusips(previousQuarter);
		Hashtable<String, Double> cusipsHedgeConcentration = new Hashtable<String, Double>();
		double concentration;
		for(String cusip: cusips ){
			concentration = cusipToSharesHeld.get(cusip)/cusipsToSharesOutStanding.get(cusip);
			cusipsHedgeConcentration.put(cusip, concentration);
		}
 		//
		try{	
		BufferedWriter out = new BufferedWriter(new FileWriter("concentration"+quarter.replace("/","")+".csv"));
		for(String cusip:  cusipsHedgeConcentration.keySet())
			out.write(DB.getTickerFromCusip(cusip, quarter) + ", "+  cusipsHedgeConcentration.get(cusip) + "\n");
		out.close();	
		}catch(Exception e){
			e.printStackTrace();
		}
			
	}
	
	public static String getPreviousQuarter(String quarter){
		String[] quarterSplit = quarter.split("/");
		
		int year = new Integer(quarterSplit[0]);
		int quart = new Integer(quarterSplit[1].substring(quarterSplit[1].length()-1));
		if(quart == 1){
			quart = 4;
			year--;
		}else
			quart--;
		
		return year+"/QRT"+quart+"/";
	}
	
	
}
/*
 * Download Data X
 * Get Data from Files
 * Store Data - in  a .csv First Name, Year Quarter, CUSIP, Ticker, 
 */


