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
			String quarterDir;
			String sharesOutstanding;
			
			while((line = in.readLine()) != null){
				lineSplit = line.split(",");
				System.out.println(line);
				quarterDir = wrdsQTRFormatToqtrDirFormat(lineSplit[colNumDataFQTR]);
				storeTickersCusips(lineSplit[colNumCusip], lineSplit[colNumTic], quarterDir);
				sharesOutstanding = lineSplit[colNumCSHOQ];
				if(sharesOutstanding.isEmpty())
					saveSharesOutstanding(lineSplit[colNumCusip], 0, quarterDir);
				else
					saveSharesOutstanding(lineSplit[colNumCusip], new Double(sharesOutstanding), quarterDir);
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
			if(word.equals(var))
				break;
			wordCount++;
		}
		return wordCount;
	}
	
	private void storeTickersCusips(String cusip, String tick, String quarter){ 
		database.insertCusipTicker(cusip, tick, quarter);
	}
	
	public  void saveSharesOutstanding(String cusip, double sharesOutstanding, String quarter){
		database.insertTempSharesTable(cusip, sharesOutstanding, quarter);
	}
	
	public static String wrdsQTRFormatToqtrDirFormat(String wrdsQTR){
		//wrdsQTR looks like 2009Q1
		//returns 2009/QTR1/
		return wrdsQTR.replaceAll("([0-9]{4})Q([1-4])", "$1/QTR$2/");
	}
}
