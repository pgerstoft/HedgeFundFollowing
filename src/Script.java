import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;


public class Script {

	public static void main(String[] args){
		CRSPFileRead.parseFile("9a9bfff938ec8b45.csv");
		//Download SEC data
//		SECData inst = SECData.getInstance();
//		String quarterDir = "2009/QTR2/";
//		WRDSFileRead wrdsFreader = new WRDSFileRead();
//		String file = "93dbde77c0e270ba.csv";
//		//wrdsFreader.parseFile(file);
//		try {
//			inst.downloadAndStoreSEC13Fs("2011/QTR1/", true);
//			inst.downloadAndStoreSEC13Fs(quarterDir, true);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}	
//		catch (ClassNotFoundException e) {
//			e.printStackTrace();
//		}
		
//		try{
//			makeDesignMatrix();
//		}catch(Exception e){
//			e.printStackTrace();
//		}
		
		//DB.selectSharesOutstanding();
		//DB.outputNumSharesHeldByFunds(quarterDir);
		//getMostConcentrated(quarterDir);
//		DB.writeToFile("09255X100", quarterDir);
	}
	
//	public static void getMostConcentrated(Quarter quarter){
//	//	ArrayList<String> cusips = DB.getCusips(quarter);
//	//	System.out.println(cusips.size());
//		//get Shares Held
//		Hashtable<String, Double> cusipToSharesHeld = DB.getNumSharesHeldByFunds(quarter);
//		System.out.println(cusipToSharesHeld.size());
//		Quarter previousQuarter = quarter.getPreviousQuarter();
//		Hashtable<String, Double> cusipsToSharesOutStanding = DB.getSharesOustandingAllCusips(previousQuarter);
//		System.out.println(cusipsToSharesOutStanding.size());
//
//		Hashtable<String, Double> cusipsHedgeConcentration = new Hashtable<String, Double>();
//		double concentration;
//		for(String cusip: cusipsToSharesOutStanding.keySet() ){
//			if(cusipToSharesHeld.get(cusip) == null)
//				continue;
//			System.out.println(cusip +" " +cusipToSharesHeld.get(cusip) + " "+ cusipsToSharesOutStanding.get(cusip));
//
//			concentration = cusipToSharesHeld.get(cusip)/cusipsToSharesOutStanding.get(cusip);
//			cusipsHedgeConcentration.put(cusip, concentration);
//		}
// 		//
//		try{	
//		BufferedWriter out = new BufferedWriter(new FileWriter("concentration"+quarter.toString().replace("/","")+".csv"));
//		System.out.println(cusipsHedgeConcentration.size());
//		for(String cusip:  cusipsHedgeConcentration.keySet()){
//			out.write(DB.getTickerFromCusip(cusip, quarter) + ", "+  cusipsHedgeConcentration.get(cusip) + "\n");
//			System.out.print(DB.getTickerFromCusip(cusip, quarter) + ", "+  cusipsHedgeConcentration.get(cusip) + "\n");
//		}
//		out.close();	
//		}catch(Exception e){
//			e.printStackTrace();
//		}
//			
//	}
	
//	public static String getPreviousQuarter(String quarter){
//		String[] quarterSplit = quarter.split("/");
//		
//		int year = new Integer(quarterSplit[0]);
//		int quart = new Integer(quarterSplit[1].substring(quarterSplit[1].length()-1));
//		if(quart == 1){
//			quart = 4;
//			year--;
//		}else
//			quart--;
//		
//		return year+"/QTR"+quart+"/";
//	}
		
	public static void makeDesignMatrix() throws IOException{
		//Using all data in MYDB

		//Ticker Quarter fundPercentInStock (FPIS), Percent Stock Owned by Fund (PSOF), newFundHolding, changeInHolding ... return  
		BufferedWriter out = new BufferedWriter(new FileWriter("designMatrix.csv"));
		
//		ArrayList<String> quarters;
		
		//get list of CIKs
		ArrayList<CIK> ciks = DB.getCIKS();
		Quarter[] quarters = {new Quarter("2011/QTR1/")}; 
		//ticker, Shares
		Hashtable<CIK, Double> previousQuarter = new Hashtable<CIK, Double>();
		Hashtable<CIK, Double> currentQuarter = new Hashtable<CIK, Double>();
		
		Hashtable<CIK, Double> fundToSharesForOneEquity = new Hashtable<CIK, Double>();
		Hashtable<CIK, Double> fundToFundSize = new Hashtable<CIK, Double>();
		String ticker;
		StringBuffer line;
		double sharesOutstanding;
		double sharePrice;
		double percentChange;
		int factor = 100000; 
		ArrayList<Cusip> cusips = DB.getCusips(quarters[0]);
		
		out.write("Ticker, ");
		out.write("Quarter, ");
		for(CIK cik: ciks){
			out.write("% of " + cik + ", ");
			out.write("% stock " + cik + ", ");
			
		}
	
		
		for(Cusip cusip: cusips){
			line = new StringBuffer();
			for(CIK cik: ciks){
				currentQuarter.put(cik, 0.0);
			}
		
			for(Quarter quarter: quarters){
				ticker = DB.getTickerFromCusip(cusip, quarter);				
				sharesOutstanding = DB.getSharesOustanding(cusip, quarter);
				fundToSharesForOneEquity  = DB.getFundsToShares(cusip, quarter);
				fundToFundSize = DB.getFundValues(quarter);
//				sharePrice = DB.getPriceAtQuarterEnd(cusip, quarter);
				
				for(CIK cik: fundToSharesForOneEquity.keySet()){
					currentQuarter.put(cik, fundToSharesForOneEquity.get(cik));
				}
				
				line.append(ticker+ ", ");
				line.append(quarter + ", ");
				line.append(numHeldBy() + ", ");
				line.append(concentration);
				double fundConcentration = 0.0;
				for(CIK cik: ciks){
					if (fundToFundSize.get(cik)  != null)
						fundConcentration = currentQuarter.get(cik) * DB.getFundValueDividedByShare(cusip, cik, quarter)  / fundToFundSize.get(cik);
					else
						fundConcentration = 0.0;
					line.append(fundConcentration + ", ");
					line.append((currentQuarter.get(cik) / (sharesOutstanding * factor)) + ", ");
					//DO IT SO THAT FIRST QUARTER IS JUST TO INITIALIAZE PREVIOUS QUARTER!
//					if(previousQuarter.size()  != 0 ){
//						if((currentQuarter.get(cik) != 0  && previousQuarter.get(cik) == 0))
//							line.append(1 + ", ");
//						else
//							line.append(0 + ", ");
//						percentChange = (currentQuarter.get(cik) - previousQuarter.get(cik)) / previousQuarter.get(cik);
//						line.append(percentChange + ", ");
//					}else
//						line.append("0, 0, ");
					
					
				}
				System.out.println(cusip);
				out.write(line.toString() + "\n");
				
				previousQuarter = currentQuarter;
			}
		}
		out.close();
	}
		//
		//for every quarter
		//	for every cusip
		//		get CIK holding it
		//			
}




