import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.TreeSet;

public class Script {

	public static void main(String[] args) {
		// setFundReturn(new Quarter(2011, 1, true));
		// CRSPFileRead.parseFile("9a9bfff938ec8b45.csv");
		// setPortionOfFund(new CIK("0000009631"), new Quarter(2011, 1, true));
		// Download SEC data
		SECData inst = new SECData();
		// String quarterDir = "2009/QTR2/";
		// WRDSFileRead wrdsFreader = new WRDSFileRead();
		// String file = "93dbde77c0e270ba.csv";
		// //wrdsFreader.parseFile(file);

		DataAnalysis x = new DataAnalysis();
		x.makeDesignMatrix();

//		for (int year = 2004; year <= 2010; year++) {
//			for (int quarter = 1; quarter <= 4; quarter++) {
//				if (year == 2004 && quarter < 3)
//					//1Z E89 84X 90 5892 1274
//					continue;
//				inst.setQuarter(new Quarter(year, quarter, true));
//				inst.downloadAndStoreSEC13Fs(true);
//			}
//		}

		// try{
		// inst.downloadAndStoreSEC13Fs(new Quarter(2009,2,true), false);
		// }catch(Exception e){e.printStackTrace();}
		// try{
		// makeDesignMatrix();
		// }catch(Exception e){
		// e.printStackTrace();
		// }

		// DB.selectSharesOutstanding();
		// DB.outputNumSharesHeldByFunds(quarterDir);
		// getMostConcentrated(quarterDir);
		// DB.writeToFile("09255X100", quarterDir);
	}

	// public static void getMostConcentrated(Quarter quarter){
	// // ArrayList<String> cusips = DB.getCusips(quarter);
	// // System.out.println(cusips.size());
	// //get Shares Held
	// Hashtable<String, Double> cusipToSharesHeld =
	// DB.getNumSharesHeldByFunds(quarter);
	// System.out.println(cusipToSharesHeld.size());
	// Quarter previousQuarter = quarter.getPreviousQuarter();
	// Hashtable<String, Double> cusipsToSharesOutStanding =
	// DB.getSharesOustandingAllCusips(previousQuarter);
	// System.out.println(cusipsToSharesOutStanding.size());
	//
	// Hashtable<String, Double> cusipsHedgeConcentration = new
	// Hashtable<String, Double>();
	// double concentration;
	// for(String cusip: cusipsToSharesOutStanding.keySet() ){
	// if(cusipToSharesHeld.get(cusip) == null)
	// continue;
	// System.out.println(cusip +" " +cusipToSharesHeld.get(cusip) + " "+
	// cusipsToSharesOutStanding.get(cusip));
	//
	// concentration =
	// cusipToSharesHeld.get(cusip)/cusipsToSharesOutStanding.get(cusip);
	// cusipsHedgeConcentration.put(cusip, concentration);
	// }
	// //
	// try{
	// BufferedWriter out = new BufferedWriter(new
	// FileWriter("concentration"+quarter.toString().replace("/","")+".csv"));
	// System.out.println(cusipsHedgeConcentration.size());
	// for(String cusip: cusipsHedgeConcentration.keySet()){
	// out.write(DB.getTickerFromCusip(cusip, quarter) + ", "+
	// cusipsHedgeConcentration.get(cusip) + "\n");
	// System.out.print(DB.getTickerFromCusip(cusip, quarter) + ", "+
	// cusipsHedgeConcentration.get(cusip) + "\n");
	// }
	// out.close();
	// }catch(Exception e){
	// e.printStackTrace();
	// }
	//			
	// }

	// TODO Need faster way to create design matrix
	// Convert Ticker portionOfFund CIK Quarter to Ticker Quarter portionOfFund1
	// ... portionOfFundM

	// 
	// 2.Get list of all CIKS from file, create a hashtable where each CIKS maps
	// to Array value
	// 3.open file while same quarter add to array[hash.get(CIK)]
	// 4.when new quarter write line repeat

	// public static void makeDesignMatrix() throws IOException{
	// //Using all data in MYDB
	//
	// //Ticker Quarter fundPercentInStock (FPIS), Percent Stock Owned by Fund
	// (PSOF), newFundHolding, changeInHolding ... return
	// BufferedWriter out = new BufferedWriter(new
	// FileWriter("designMatrix.csv"));
	//		
	// // ArrayList<String> quarters;
	//		
	// //get list of CIKs
	// ArrayList<CIK> ciks = DB.getCIKS();
	// Quarter[] quarters = {new Quarter("2010/QTR3/")};
	// //ticker, Shares
	// Hashtable<CIK, Double> previousQuarter = new Hashtable<CIK, Double>();
	// Hashtable<CIK, Double> currentQuarter = new Hashtable<CIK, Double>();
	//		
	// Hashtable<CIK, Double> fundToSharesForOneEquity = new Hashtable<CIK,
	// Double>();
	// Hashtable<CIK, Double> fundToFundSize = new Hashtable<CIK, Double>();
	// String ticker;
	// StringBuffer line;
	// double sharesOutstanding;
	// double sharePrice;
	// double percentChange;
	// int factor = 100000;
	// int numFundHoldingsMin = 10;
	// int numFundHoldingsMax = 200;
	// ArrayList<Cusip> cusips = DB.getCusipsFromCusipReturn(quarters[0]);
	//		
	// out.write("Ticker, ");
	// out.write("Quarter, ");
	// for(CIK cik: ciks){
	// out.write("% of " + cik + ", ");
	// out.write("% stock " + cik + ", ");
	//			
	// }
	//	
	// Hashtable<Quarter, Double> threeMonthReturns;
	// Hashtable<Quarter, Double> threeMonthSPYReturns =
	// DB.getThreeMonthStockReturn(DB.getCusipFromTicker("SPY", new
	// Quarter(2011, 1)));
	// for(Cusip cusip: cusips){
	// line = new StringBuffer();
	// for(CIK cik: ciks){
	// currentQuarter.put(cik, 0.0);
	// }
	// threeMonthReturns = DB.getThreeMonthStockReturn(cusip);
	//			
	// for(Quarter quarter: quarters){
	// ticker = DB.getTickerFromCusip(cusip, quarter);
	// sharesOutstanding = DB.getSharesOustanding(cusip, quarter);
	// fundToSharesForOneEquity = DB.getFundsToShares(cusip, quarter);
	// fundToFundSize = DB.getFundValues(quarter);
	// // sharePrice = DB.getPriceAtQuarterEnd(cusip, quarter);
	//				
	// for(CIK cik: fundToSharesForOneEquity.keySet()){
	// currentQuarter.put(cik, fundToSharesForOneEquity.get(cik));
	// }
	//				
	// if(ticker == null)
	// continue;
	// line.append(cusip+", ");
	// line.append(ticker+ ", ");
	// line.append(quarter + ", ");
	//
	// double fundConcentration = 0.0;
	// for(CIK cik: ciks){
	// // if (fundToFundSize.get(cik) != null){
	// // fundConcentration = currentQuarter.get(cik) *
	// DB.getFundValueDividedByShare(cusip, cik, quarter) /
	// fundToFundSize.get(cik);
	// // }else
	// // fundConcentration = 0.0;
	// // line.append(fundConcentration + ", ");
	// // line.append((currentQuarter.get(cik) / (sharesOutstanding * factor)) +
	// ", ");
	// //DO IT SO THAT FIRST QUARTER IS JUST TO INITIALIAZE PREVIOUS QUARTER!
	// // if(previousQuarter.size() != 0 ){
	// // if((currentQuarter.get(cik) != 0 && previousQuarter.get(cik) == 0))
	// // line.append(1 + ", ");
	// // else
	// // line.append(0 + ", ");
	// // percentChange = (currentQuarter.get(cik) - previousQuarter.get(cik)) /
	// previousQuarter.get(cik);
	// // line.append(percentChange + ", ");
	// // }else
	// // line.append("0, 0, ");
	//										
	// }
	//				
	// line.append(DB.numFundsHolding(cusip, numFundHoldingsMin,
	// numFundHoldingsMax, quarter) + ", ");
	// line.append((DB.numSharesHeld(cusip, numFundHoldingsMin,
	// numFundHoldingsMax, quarter) / (sharesOutstanding * factor)) + ", ");
	//
	// System.out.println(cusip);
	// System.out.println(threeMonthReturns);
	// line.append((threeMonthReturns.get(quarter) -
	// threeMonthSPYReturns.get(quarter)));
	//				
	//				
	// out.write(line.toString() + "\n");
	//				
	// previousQuarter = currentQuarter;
	// }
	// }
	// out.close();
	// }

	public static void setFundReturn(Quarter quarter) {
		BufferedWriter out;
		try {
			out = new BufferedWriter(new FileWriter("temp/temp.csv"));
			ArrayList<CIK> ciks = DB.getCIKS(quarter);
			ArrayList<Cusip> cusips;
			double threeMonthReturn = 0.0;
			double percentOfFund = 0.0;
			double ret;
			double totalPercent = 0.0;
			for (CIK cik : ciks) {
				// get cusips
				ret = 0.0;
				totalPercent = 0.0;
				cusips = DB.getCusipsHeldBy(cik, quarter);
				for (Cusip cusip : cusips) {
					// get threeMonthReturn
					threeMonthReturn = DB.getThreeMonthStockReturn(cusip,
							quarter);
					// get concentration
					percentOfFund = DB.getPortionOfFund(cik, cusip, quarter);
					System.out.println(cik + " " + cusip + " " + quarter + " "
							+ percentOfFund);
					totalPercent += percentOfFund;
					ret += (1 + threeMonthReturn) * percentOfFund;
				}
				System.out.println(totalPercent);
				System.out.println(cik + "," + (ret - 1) + "," + quarter);
				out.write(cik + "," + (ret - 1) + "," + quarter + " \n");

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		DB
				.batchSetFundReturn(System.getProperty("user.dir")
						+ "/temp/temp.csv");
	}

	//
	// for every quarter
	// for every cusip
	// get CIK holding it
	//			
}

// 1.Download AND parse SEC DATA
// 2.Download Financial Data: CSHOQ, PRICE
// 3.Update Relevant Portions of Database

