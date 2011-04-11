import java.io.BufferedReader;
import java.io.FileReader;


public class CRSPFileRead {
	
	public static  void parseFile(String file){
		System.out.println("CRSPFileRead: Parsing File...");
		int colNumRET,colNumPRICE,colNumCusip, colNumDate;
		try{
			BufferedReader in = new BufferedReader(new FileReader(file));
			String line = in.readLine();
			
			
		//get column # for datafqtr - data financial qtr
			colNumRET = getColNumRET(line);
		//get column # for tic
			colNumPRICE = getColNumPRC(line);
		//get column # for cusip
			colNumCusip = getColNumCusip(line);
		//get column # for date
			colNumDate = getColNumDate(line);
			
			String[] lineSplit;
			Quarter quarterDir = null;
			Quarter newQuarterDir;
			Date date;
			double monthlyReturn = 1.0;
			double price = 0.0;
			Cusip cusip = null;
			Cusip newCusip;
			
			while((line = in.readLine()) != null){
				lineSplit = line.split(",");
				System.out.println(line);
				date = new Date(lineSplit[colNumDate]);
				newCusip = new Cusip(lineSplit[colNumCusip]);
				
				if(cusip == null)
					cusip = newCusip;
				else if(!cusip.equals(newCusip)){
					cusip = newCusip;
					quarterDir = null;
				}
				
				
				if(colNumPRICE >= lineSplit.length  || lineSplit[colNumPRICE].trim().isEmpty()){ 
					//Stock has been removed from Market
					continue;
				}else if(lineSplit[colNumRET].equalsIgnoreCase("B"))
					continue;
				else if(lineSplit[colNumRET].equalsIgnoreCase("C"))
					monthlyReturn = 0;
				else
					monthlyReturn = new Double(lineSplit[colNumRET]);
					
					price =  new Double(lineSplit[colNumPRICE]);
					DB.insertStockPriceReturn(cusip ,price, monthlyReturn, date);
			}
		}catch(Exception e){e.printStackTrace();}
	}

	//get column number
	private static int getColNumRET(String header){ return getColNum(header, "ret");}
	private static int getColNumPRC(String header){ return getColNum(header,"prc"); }
	private static int getColNumCusip(String header) { return getColNum(header, "cusip"); } 
	private static int getColNumDate(String header) { return getColNum(header, "date"); }
	
	private static int getColNum(String header, String var){
		int wordCount = 0;
		for(String word : header.split(",")){
			if(word.equalsIgnoreCase(var))
				break;
			wordCount++;
		}
		return wordCount;
	}

}
