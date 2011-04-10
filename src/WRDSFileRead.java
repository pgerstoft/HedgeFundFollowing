import java.io.BufferedReader;
import java.io.FileReader;




public class WRDSFileRead {
	private DB database;
	
	public WRDSFileRead(){
		database = DB.getInstance();
	}
	
	public  void parseFile(String file){
		System.out.println("WRDSFileRead: Parsing File...");
		int colNumDataFQTR,colNumTic,colNumCusip, colNumCSHOQ = -1;
		try{
			BufferedReader in = new BufferedReader(new FileReader(file));
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
				System.out.println(line);
				quarterDir = wrdsQTRFormatToqtrDirFormat(lineSplit[colNumDataFQTR]);
				cusip = new Cusip(lineSplit[colNumCusip]);
				storeTickersCusips(cusip, lineSplit[colNumTic], quarterDir);
				sharesOutstanding = lineSplit[colNumCSHOQ];
				if(sharesOutstanding.isEmpty())
					saveSharesOutstanding(cusip, 0, quarterDir);
				else
					saveSharesOutstanding(cusip, new Double(sharesOutstanding), quarterDir);
			}
			
		}catch(Exception e){ e.printStackTrace();}
	}
	
	private int getColNumDataFQTR(String header){ return getColNum(header, "datafqtr");}
	private int getColNumTic(String header){ return getColNum(header,"tic"); }
	private int getColNumCusip(String header) { return getColNum(header, "cusip"); } 
	private int getColNumCSHOQ(String header) { return getColNum(header, "cshoq"); } 
	
	private int getColNum(String header, String var){
		int wordCount = 0;
		for(String word : header.split(",")){
			if(word.equalsIgnoreCase(var))
				break;
			wordCount++;
		}
		return wordCount;
	}
	
	private void storeTickersCusips(Cusip cusip, String tick, Quarter quarter){ 
		database.insertCusipTicker(cusip, tick, quarter);
	}
	
	public  void saveSharesOutstanding(Cusip cusip, double sharesOutstanding, Quarter quarter){
		database.insertTempSharesTable(cusip, sharesOutstanding, quarter);
	}
	
	public static Quarter wrdsQTRFormatToqtrDirFormat(String wrdsQTR){
		//wrdsQTR looks like 2009Q1
		//returns 2009/QTR1/
		int year = new Integer(wrdsQTR.substring(0, 4));
		int quarter = new Integer(wrdsQTR.substring(5));
		return new Quarter(year, quarter, true);
	}

}
