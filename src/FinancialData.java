import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//TODO: Decide what financial data we want from WRDS
//	1.Financial Sector
//	2.
//TODO: Get historical data from WRDS

//scrape the financial data from online
public class FinancialData extends Data{
	
	private final String FINANCIAL_DIRECTORY = "/Volumes/gerstoftbackup/Stocks/";
	private Quarter quarter;
	
	public FinancialData(){
		quarter = getMostRecentFinishedQuarter();
		createFolders(FINANCIAL_DIRECTORY);
	}
	
	public static Quarter getMostRecentFinishedQuarter() {
		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH);
		return new Quarter(year, month).getPreviousQuarter();
	}
	
	public String urlread(String url, Double sleepTime){
		StringBuffer bf = new StringBuffer();
		try {
			URL conn = new URL(url);
			try {
				Thread.sleep(convertSecondToMillis(sleepTime));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					conn.openStream()));
			String line;

			while ((line = reader.readLine()) != null) {
				bf.append(line + "\n");
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bf.toString();
	}
	
	private long convertSecondToMillis(double d) {
		return (long) (1000 * d);
	}
	
	//get financial data for all stocks in database
	public void downloadFinancialDataForMostRecentQuarter(){
		
		//BATCH MODE! How do we get all cusips?
		//Get all cusips into file load 30,000 at a time
		
		//Store financial data every 500 cusips
 		
		//save cusip into financialCusipsTemp
		
		//delete financialCusipsTemp
	}

	
	//gets the financial statements and stores them
	public void downloadFinancialData(Cusip cusip) throws IOException{

		String tick = getTicker(cusip);
		
		if(tick.isEmpty()){
			//write to file!
			return;
		}
		String finacialStatement[] = { "CAS", "INC", "BAL" };
		String dataFrequency[] = { "ANN", "INT" };
		String financialFile = "";
		String stockDataDirectory= "";
		String url;
		String str;
		BufferedWriter out;
		int startFile, endFile;
		for (String statement : finacialStatement) {
			for (String freq : dataFrequency) {
				url = "http://www.reuters.com/finance/stocks/incomeStatement?symbol="
					+ tick + "&stmtType=" + statement + "&perType=" + freq;
				str = urlread(url, .25);

				stockDataDirectory = FINANCIAL_DIRECTORY + tick + "/";
				financialFile = stockDataDirectory + tick + "_" + 
						statement + "_" + freq + ".csv";
				createFolders(stockDataDirectory);
				
				out = new BufferedWriter(new FileWriter(financialFile));
				
				startFile = str.indexOf("In Millions of U.S. Dollars");
				endFile = str.indexOf("<div class=\"column2 gridPanel grid4\">");

				if (startFile == -1) {
					out.write("NOT US DOLLARS");
					out.close();
					continue;
				}

				str = str.substring(startFile - 1, endFile - 2);

				// FORMAT FILE
				str.replaceAll("<[/a-z=:;!?\" ]*>", "");
				str.replaceAll("<.*>", "");
				str.replaceAll("^[  ]*", "");
				str.replaceAll("[   ]*$", "");
				// str.replaceAll("[(<[/a-z=:;!?\" ]*>)(<.*>)(^[  ]*)([   ]*$)", "");
				str.replaceAll("\n[ \t]*\n", "\n"); // remove blank lines
				str.replaceAll("--", "0");
				// change losses denoted by by parentheses to negative sign
				// (189) to -189
				str.replaceAll("(\\([0-9.]*\\))", "-$1"); 
				// !sed	 's/(\([0-9.]*\))/-\1/' temp > temp2
				str.replaceAll(",", "");
				/*
				 * %MAY OR MAY NOT KEEP THIS !sed '/Period/d' temp > temp2 !sed
				 * '/Months/d' temp2 > temp !sed '/Weeks/d' temp > temp2
				 */
				
				String[] strByLine = str.split("\n");
				String newLine = "";
				int numQuarters = 0;
				int strByLineIndx = 2;
				String line = strByLine[strByLineIndx];
				StringTokenizer lineTokenized = new StringTokenizer(line, "-");
				int year;
				int month;
				int day;
				while ((line.substring(0, 2).equals("20") && line.length() == 4)
						|| line.equals("Restated")
						|| line.equals("Reclassified")) {
					line = strByLine[strByLineIndx++];
					year = new Integer(lineTokenized.nextToken());
					month = new Integer(lineTokenized.nextToken());
					
					day = new Integer(lineTokenized.nextToken());

					if (year < 2000
							|| year > Calendar.getInstance().get(
									(Calendar.YEAR))) {
						System.err.println("Unexpected value " + year);
						System.exit(1);
					}
					
					numQuarters++;
					newLine = newLine + ", " + new Date(year, month, day);

					line = strByLine[strByLineIndx++];
					if (line.equals("Restated") || line.equals("Reclassified")) {
						newLine = newLine + " " + line + ":";
						line = strByLine[strByLineIndx++];
						year = new Integer(lineTokenized.nextToken());
						month = new Integer(lineTokenized.nextToken());
						;
						day = new Integer(lineTokenized.nextToken());
						line = strByLine[strByLineIndx++];
					}
				}

				if (line.substring(0, 2).equals("20")) {
					line = strByLine[strByLineIndx++];
				}

				// if( numQuarters < 2 )
				// deleteFD{end+1,1} = tickers(ii).name;
				// tickers(ii).name
				// 'DELETED!!!'
				// return
				// end

				// if strncmp(line, '19', 2)
				// deleteFD{end+1,1} = tickers(ii).name;
				// tickers(ii).name
				// 'DELETED!!!'
				// return
				// end
				out.write(newLine + "\n");

				while (strByLineIndx < strByLine.length) {
					newLine = line;
					for (int lines = 0; lines < numQuarters; lines++) {
						line = strByLine[strByLineIndx++];
						newLine = newLine + ", " + line;
					}
					out.write(newLine + "\n");
					line = strByLine[strByLineIndx++];
				}
				out.close();

			}
		}

	}

	public String getTicker(Cusip cusip) {

		String url = "http://activequote.fidelity.com/mmnet/SymLookup.phtml?" +
				"QUOTE_TYPE=&scCode=E&searchBy=C&searchFor=" + cusip;
		
		String wholeFile = urlread(url, .25);
		String startString = "SID_VALUE_ID=";
		int start = wholeFile.indexOf(startString);
		int end = wholeFile.indexOf("\"", start);

		if(start != -1 && end != -1) 
			return wholeFile.substring(start+startString.length(), end);
		
		return "";
	}
	
//	private setStockPriceReturn(String tick){
//		String[] priceData = getPriceData(tick, quarter.getPreviousQuarter().toCalendar(), quarter.toCalendar());
//		
//	}
	
	public String[] getPriceData(String tick, Calendar start, Calendar end){
		//download from Yahoo
		//get the close and the adj close get it weekly
		
		char frequency = 'w';
		String url = constructURL(tick, start, end, frequency);
		return url.split(",");
	}	
	
	private String constructURL(String ticker, Calendar start, Calendar end, char freq){
		/**
		 * s - ticker symbol 
		 * a - start month 
		 * b - start day 
		 * c - start year 
		 * d - end month 
		 * e - end day 
		 * f - end year 
		 * g - resolution (e.g. 'd' is daily, 'w' is weekly, 'm' is monthly)
		 */
		
		Lib.assertTrue(freq == 'm' || freq=='w' || freq=='d');
		return "http://ichart.finance.yahoo.com/table.csv?s=" + ticker + 
		"&a=" + Integer.toString(start.get(Calendar.MONTH) ) +
		"&b=" + start.get(Calendar.DAY_OF_MONTH) + 
		"&c=" + Integer.toString(start.get(Calendar.YEAR)) +		
		"&d=" + Integer.toString(end.get(Calendar.MONTH) ) +
		"&e=" + Integer.toString(end.get(Calendar.DAY_OF_MONTH)) + 
		"&f=" + Integer.toString(end.get(Calendar.YEAR)) +
		"&g=" + freq +"&ignore=.csv";
		}
	
	
	
	//Extract information and store it in a Stock Object
	public void storeFinancialDataInDB(){
		
	}
	
		
	//compute Relevant ratios
	public void computeFinancialValues(){
		
	}
	
	
}



//Financial Data
//Get it then store Ratios in the DB?