package fundfollowing.wrds;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import fundfollowing.Cusip;
import fundfollowing.Quarter;
import fundfollowing.db.DB;



public class WRDSFileRead {
	
	public WRDSFileRead(){
	}
	
	public  void parseFile(String file){
		System.out.println("WRDSFileRead: Parsing File...");
		int colNumDataFQTR,colNumTic,colNumCusip, colNumCSHOQ = -1;
		BufferedReader in =  null;
		BufferedWriter outTickers = null;
		BufferedWriter outShares = null;
		String tempTickersFile = "tempTickers.csv";
		String tempSharesFile = "tempShares.csv";
		try{
			outTickers = new BufferedWriter(new FileWriter(tempTickersFile));
			outShares = new BufferedWriter(new FileWriter(tempSharesFile));
			in = new BufferedReader(new FileReader(file));
			String line = in.readLine();
			
			
		//get column # for datafqtr - data financial qtr
			colNumDataFQTR = getColNumDataFQTR(line);
		//get column # for tic
			colNumTic = getColNumTic(line);
		//get column # for cusip
			colNumCusip = getColNumCusip(line);
		//get column # for cshoq - common shares outstanding
			colNumCSHOQ = getColNumCSHOQ(line);
			
			String[] lineSplit;
			Quarter quarterDir;
			String sharesOutstanding;
			Cusip cusip;
			while((line = in.readLine()) != null){
				lineSplit = line.split(",");
//				System.out.println(line);
				quarterDir = wrdsQTRFormatToqtrDirFormat(lineSplit[colNumDataFQTR]);
				cusip = new Cusip(lineSplit[colNumCusip]);
				outTickers.write(cusip + ","+lineSplit[colNumTic]+","+quarterDir+"\n");//storeTickersCusips(cusip, lineSplit[colNumTic], quarterDir);
				sharesOutstanding = lineSplit[colNumCSHOQ];
				if(sharesOutstanding.isEmpty())
					outShares.write(cusip+","+0+","+quarterDir + "\n" );//saveSharesOutstanding(cusip, 0, quarterDir);
				else
					outShares.write(cusip+","+new Double(sharesOutstanding)+","+quarterDir + "\n" );//saveSharesOutstanding(cusip, new Double(sharesOutstanding), quarterDir);
			}
			
		}catch(Exception e){ e.printStackTrace();}
		finally{
		    if (in != null) {
		        try {
		            in.close();
		        } catch (IOException e) {
		        }
		    }
		    if(outShares != null){
		    	try {
		            outShares.close();
		        } catch (IOException e) {
		        }
		    }
		    if(outTickers != null){
		    	try {
		    		outTickers.close();
		        } catch (IOException e) {
		        }
		    }
		}
		
		System.out.println("BatchLoading.....");
		DB.batchLoadSharesOutstanding(System.getProperty("user.dir") + "/"+ tempSharesFile);
		DB.batchLoadCusipTickers(System.getProperty("user.dir") + "/" + tempTickersFile);
		System.out.println("DONE");
		
		new File(tempSharesFile).delete();
		new File(tempTickersFile).delete();
	}
	
	private int getColNumDataFQTR(String header){ return ReadCSVFile.getColNum(header, "datafqtr");}
	private int getColNumTic(String header){ return ReadCSVFile.getColNum(header,"tic"); }
	private int getColNumCusip(String header) { return ReadCSVFile.getColNum(header, "cusip"); } 
	private int getColNumCSHOQ(String header) { return ReadCSVFile.getColNum(header, "cshoq"); } 
	
	
	public static Quarter wrdsQTRFormatToqtrDirFormat(String wrdsQTR){
		//wrdsQTR looks like 2009Q1
		//returns 2009/QTR1/
		int year = new Integer(wrdsQTR.substring(0, 4));
		int quarter = new Integer(wrdsQTR.substring(5));
		return new Quarter(year, quarter, true);
	}

}
