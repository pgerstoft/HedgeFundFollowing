import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//TODO Run through getMaches for each 13F, fix any problems...
//TODO getSharesOutsanding and do Concentration
//TODO Set up Stock
//TODO Set up Financial Data
//TODO check out if there are a lot of bad cusips and eight Digit
//TODO Set up a system for storing vars, and writing variables twice

public class SECData {

	public static final String ftpSEC = "ftp.sec.gov";
	public static final String ftpSECUser = "anonymous";
	public static final String ftpSECPassword = "pgerstoft@berkeley.edu";
	public static final String sec13FsLocalDir = "filings/13Fs/";
	public static final String sec13FsFilingDir = "data/";
	public static final String secRemoteFullDir = "edgar/full-index/";
	public static final String secRemoteDataDir = "edgar/data/";
	private static String tempFolder ="temp/";
	public static String companyIdx = "company.idx";
	private static String companyIdx13F = "company.idx.13F";

	private static final SECData INSTANCE = new SECData();

	// Private constructor prevents instantiation from other classes
	private SECData() {
		createFolders(sec13FsLocalDir);
		createFolders(tempFolder);
		// TODO Verify that all files that should be downloaded are
	}

	public static SECData getInstance() {
		return INSTANCE;
	}

	private static void createFolders(String dir) {
		File sec13FFileDir = new File(dir);
		if (!sec13FFileDir.exists())
			sec13FFileDir.mkdirs();
	}

	public int downloadSEC13Fs() {
		// check if the 13Fs have been downloaded
		// by looking at the company.idx folder
		String quarterDir = "2010/QTR1/";
		getCompanyIdx(quarterDir);

		return 1;
	}

	public void get13Fs(String quarterDir) {
		File companyIdx13FFile = new File(sec13FsLocalDir + quarterDir
				+ companyIdx13F);
		if (!companyIdx13FFile.exists())
			printStatementError("Download " + companyIdx
					+ " before getting 13Fs");

		ArrayList<String> filesRemote13F = get13FsFilesFromCompanyIdx(companyIdx13FFile);
		ArrayList<String> filesLocal13F = new ArrayList<String>();
		String[] split;
		for (String path : filesRemote13F) {
			split = path.split("/");
			filesLocal13F.add(split[split.length - 1]);
		}
		String localDir = sec13FsLocalDir + quarterDir + sec13FsFilingDir;
		createFolders(localDir);
		String[] ftpCommand = { ftpSEC, ftpSECUser, ftpSECPassword,
				secRemoteDataDir, localDir };
		Ftp.ftpMultipleFiles(ftpCommand, filesRemote13F, filesLocal13F);
	}

	public static ArrayList<String> get13FsFilesFromCompanyIdx(
			File companyIdx13FFile) {

		if (!companyIdx13FFile.getPath().endsWith(companyIdx13F))
			printStatementError("Bad Input: File path need to end with: "
					+ companyIdx13F);

		ArrayList<String> filesRemote13F = new ArrayList<String>();

		// get the file and the parent directory for each of the paths from
		// company.idx.13F

		BufferedReader reader = null;
		try {

			reader = new BufferedReader(new FileReader(companyIdx13FFile));
			String line = null;
			String parentAndFile = null;
			String[] forsplitting;
			String middleVal;
			String noTxtNoDashes;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				forsplitting = line.split("/");
				middleVal = forsplitting[forsplitting.length - 1].split("-")[1];
				noTxtNoDashes = forsplitting[forsplitting.length - 1]
						.replaceAll("-", "").replace(".txt", "");
				parentAndFile = forsplitting[forsplitting.length - 2] + "/"
						+ middleVal + "/" + noTxtNoDashes + "/"
						+ forsplitting[forsplitting.length - 1];

				if (!parentAndFile.endsWith(".txt"))
					printStatementError("Needs to end with .txt"
							+ parentAndFile);

				// System.out.println(parentAndFile);
				filesRemote13F.add(parentAndFile);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} finally {
			try {
				if (reader != null)
					reader.close();
			} catch (IOException e2) {
				e2.printStackTrace();
			}
		}

		return filesRemote13F;
	}

	private static void printStatementError(String statement) {
		System.err.println(statement);
		System.exit(1);
	}

	public void getCompanyIdx(String quarterDir) {
		// "ftp.sec.gov anonymous pgerstoft@berkeley.edu edgar/full-index/2010/QTR1/company.idx compay.idx";

		createFolders(sec13FsLocalDir + quarterDir);
		String[] ftpCommand = { ftpSEC, ftpSECUser, ftpSECPassword,
				secRemoteFullDir + quarterDir + companyIdx,
				sec13FsLocalDir + quarterDir + companyIdx };
		Ftp.ftpSingleFile(ftpCommand);
	}

	public static void createCompanyIdx13F() {
		String quarterDir = "2010/QTR1/";
		String file2Read = "company.idx";
		File compIdx = new File(sec13FsLocalDir + quarterDir + file2Read);
		File compIdx13F = new File(sec13FsLocalDir + quarterDir + file2Read
				+ ".13F");

		try {
			compIdx13F.createNewFile();
			Grep.grep(compIdx, compIdx13F, "13F-HR[^/A]");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int count(String filename) throws IOException {
		InputStream is = new BufferedInputStream(new FileInputStream(filename));
		byte[] c = new byte[1024];
		int count = 0;
		int readChars = 0;
		while ((readChars = is.read(c)) != -1) {
			for (int i = 0; i < readChars; ++i) {
				if (c[i] == '\n')
					++count;
			}
		}
		return count;
	}

	public boolean verifySmallFileSize(File f) throws IOException {
		return count(f.getPath()) < 200;
	}

	@SuppressWarnings("unchecked")
	public Hashtable<String, Holding> combineTableEntries(
			Hashtable<String, Holding> table1, Hashtable<String, Holding> table2) {
		
		Hashtable<String, String> cusipTicker = null;
		Hashtable<String, Holding> newTable = new Hashtable<String, Holding>();

		try {
			cusipTicker = (Hashtable<String, String>) loadData(tempFolder+"cusipTicker.data");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		if (cusipTicker == null) {
			printStatementError("CusipTicker is not stored!");
		}

		TreeSet<String> keys = new TreeSet<String>();
		keys.addAll(table1.keySet());
		keys.addAll(table2.keySet());

		for (String key : keys) {
			if(cusipTicker.containsKey(key)){
				if (table1.containsKey(key) && table2.containsKey(key)) {
					Holding old = table1.get(key);
					old.addToHolding(table2.get(key));
					newTable.put(key, old);
				} else if(table1.containsKey(key)){
					newTable.put(key, table1.get(key));
				} else if(table2.containsKey(key)){
					newTable.put(key, table2.get(key));
				} else 
					printStatementError("Neither Tables Contain Key - Fix IT!");
				
			}
		}
		System.out.println("Table Size "+ newTable.size());
		return newTable;
	}

	public Hashtable<Holding, String> switchMapping(
			Hashtable<String, Holding> table) {
		Hashtable<Holding, String> newTable = new Hashtable<Holding, String>();

		for (String key : table.keySet())
			newTable.put(table.get(key), key);

		return newTable;
	}

	private long convertSecondToMillis(double d) {
		return (long) (1000 * d);
	}

	@SuppressWarnings("unchecked")
	public void formatSEC13Fs(String quarterDir) throws IOException, ClassNotFoundException{
		File[] allFiles = new File(sec13FsLocalDir + quarterDir + sec13FsFilingDir).listFiles();
		File13F f13F;

	//		for(File f : allFiles){
		//761??? 1499
		
		Set<String> cusips;
		
		Hashtable<String, Holding> allHoldings = null;
		
		allHoldings = (Hashtable<String, Holding>) loadData(tempFolder+"allHoldings.data");		
		if(allHoldings == null)
			allHoldings = new Hashtable<String, Holding>();
		
		Integer index = 0;		
		index = (Integer) loadData(tempFolder+"formatSEC13FsIndex.data");		
		if(index == null)
			index = 0;
		
		for(int ii = index; ii< allFiles.length; ii++){
			System.out.println(allFiles[ii].getPath() + " " + ii);
			f13F = new File13F(allFiles[ii]);
//			f13F = new File13F(new File("filings/13Fs/2010/QTR1/data/0000022356-10-000028.txt"));
			
			Hashtable<String, Holding> newHoldings = f13F.getFund().getHoldings();
			
			//TODO save fund and holdings
			
			cusips = newHoldings.keySet();
			System.out.println(cusips.size()); //cusips.iterator().next()));
			System.out.println(f13F.getMatchType());
			
			
			
			if(cusips.size() == 0 && verifySmallFileSize(allFiles[ii])){
				createFolders(sec13FsLocalDir+quarterDir+"NoHoldings/");
				allFiles[ii].renameTo(new File(new File(sec13FsLocalDir+quarterDir+"NoHoldings/"), allFiles[ii].getName()));
				ii = ii - 1;
			}else if( cusips.size() == 0){
				createFolders(sec13FsLocalDir+quarterDir+"NoHoldings/");
				allFiles[ii].renameTo(new File(new File(sec13FsLocalDir+quarterDir+"MaybeNoHoldings/"), allFiles[ii].getName()));
				System.out.println("Dont think this is a bad file, " +f13F.getMatchType() + " " + allFiles[ii].getPath());
				ii = ii - 1;
//				System.exit(1);
		    }else{	
		    	storeCusips(cusips);
		    	allHoldings = combineTableEntries(allHoldings, newHoldings);
		    	//TODO allHoldings should also keep track of which Funds have already been added
		    	saveData(tempFolder+"allHoldings.data", allHoldings);
		    }
			saveData(tempFolder+"formatSEC13FsIndex.data", ii+1);
		}
			
			//System.out.println(f.getPath());

//			ArrayList<String> state = Grep.grep(f, null , "STATE:");
//			TODO LOOK At state of incorporation
			//get Fund name
			//get ticker, CUSIPS, value, shares
		

		
		
		getTopTwentyMostConcentrated();
		
		
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<String> getTopTwentyMostConcentrated() throws IOException, ClassNotFoundException{
		ArrayList<String> mostConcentrated = new ArrayList<String>();
		Hashtable<String, Holding> allHoldings = (Hashtable<String, Holding>) loadData(tempFolder+"allHoldings.data");
		Hashtable<String, Holding> allHoldingsConcentrated = new Hashtable<String, Holding>();
		Hashtable<String, String> cusipTicker = null;
		
		cusipTicker = (Hashtable<String, String>) loadData(tempFolder+"cusipTicker.data");
		if(cusipTicker == null)
			cusipTicker = new Hashtable<String, String>();
		//getSharesOustanding for each ticker
		//getSharesOustanding(cusipTicker)
		//divide allHoldings by sharesOustanding

		for(String c: allHoldings.keySet()){
			if(!cusipTicker.contains(c))
				printStatementError("WHY IS " + c + " in holdings!");
		}
		
		getSharesOuststanding(cusipTicker.keySet());
		
		Double shares;
		for(String ticker: allHoldings.keySet()){
			shares = getSharesOutstanding(ticker);
			Holding concetrationHolding = allHoldings.get(ticker);
			concetrationHolding.setShares(allHoldings.get(ticker).getShares()/shares);
			allHoldingsConcentrated.put(ticker, concetrationHolding);
		}
		
		Hashtable<Holding, String> holdingToString = switchMapping(allHoldingsConcentrated);
		TreeSet<Holding> holdingToStringKeys = new TreeSet<Holding>(holdingToString.keySet());
		Holding val;
		for(int i= 1; i< holdingToStringKeys.size(); i++){
			val = holdingToStringKeys.pollLast();
			System.out.println(val + " " + holdingToString.get(val));
			mostConcentrated.add(holdingToString.get(val));
		}		
		
		return mostConcentrated;
	}
		
	@SuppressWarnings("unchecked")
	private double getSharesOutstanding(String ticker){
		
		//load sharesOutstanding HashTable
		
		Hashtable<String, Double> tickerToSharesOutstanding = null;
		
		try{
			tickerToSharesOutstanding = (Hashtable<String, Double>) loadData(tempFolder+"tickerToSharesOutstanding.data");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		if(tickerToSharesOutstanding == null)
			tickerToSharesOutstanding = new Hashtable<String, Double>();
		else if(tickerToSharesOutstanding.containsKey(ticker))
			return tickerToSharesOutstanding.get(ticker);
		
		StringBuffer bf = new StringBuffer();
		try {
			URL url = new URL("http://www.google.com/finance?fstype=bi&q="+ ticker);
			try {
				Thread.sleep(convertSecondToMillis(.25));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					url.openStream()));
			String line;

			while ((line = reader.readLine()) != null) {
				bf.append(line + "\n");
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String wholeFile = bf.toString();
		// System.out.println(wholeFile);
		Pattern start = Pattern.compile("Shares Outstanding");
		Matcher mStart = start.matcher(wholeFile);
		mStart.find();
		Pattern end = Pattern.compile("<td class=\"r bld\">");
		Pattern endEnd = Pattern.compile("/");
		
		Matcher mEnd = end.matcher(wholeFile.substring(mStart.start()));
		mEnd.find();
		Matcher mEndEnd = endEnd.matcher(wholeFile.substring(mEnd.start()));
//		System.out.println(wholeFile.substring(mStart.start(), mStart.start()+mEnd.end()));
//		System.out.println(mEndEnd.find());
		if (mEndEnd.find()) {
//			System.out.println(wholeFile.substring(mStart.start()));
//			System.out.println(wholeFile.substring(mStart.start()+mEnd.end(),mStart.start()+ mEnd.end()+mEndEnd.end()+1));
			double shares = new Double(wholeFile.substring(mStart.start()+mEnd.end(),mStart.start()+ mEnd.end()+mEndEnd.end()+1));
			//Save sharesOutstanding HashTable
			tickerToSharesOutstanding.put(ticker,shares);
			try {
				saveData(tempFolder +"tickerToSharesOutstanding.data", tickerToSharesOutstanding);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return shares;
			
		}
		printStatementError("No match in Shares Outstanding");
		return 0;
	}
	
	private String getTicker(String cusip) {

		StringBuffer bf = new StringBuffer();
		try {
			URL url = new URL(
					"http://activequote.fidelity.com/mmnet/SymLookup.phtml?QUOTE_TYPE=&scCode=E&searchBy=C&searchFor="
							+ cusip);
			try {
				Thread.sleep(convertSecondToMillis(.25));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					url.openStream()));
			String line;

			while ((line = reader.readLine()) != null) {
				bf.append(line + "\n");
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//System.out.println(cusip);
		String wholeFile = bf.toString();
		// System.out.println(wholeFile);
		Pattern start = Pattern.compile("SID_VALUE_ID=");
		Pattern end = Pattern.compile("SID_VALUE_ID=[^>]*");
		Matcher mStart = start.matcher(wholeFile);
		Matcher mEnd = end.matcher(wholeFile);
		if (mStart.find() && mEnd.find()) {
//			System.out.println(mStart.end() + 1);
//			System.out.println(mEnd.end() - 2);
			return wholeFile.substring(mStart.end(), mEnd.end() - 1);
		}
		return "";
	}
	

	public Hashtable<String, Double> getSharesOuststanding(Set<String> tickers){
		Hashtable<String,Double> tickersToShares = new Hashtable<String, Double>();
		
		double shares;
		for(String tick: tickers){
			shares = getSharesOutstanding(tick);
			tickersToShares.put(tick, shares);
		}
		
		return tickersToShares;
	}

	
	
	@SuppressWarnings("unchecked")
	private void storeCusips(Set<String> cusips){
		
		Hashtable<String, String> cusipTicker = null;
		Hashtable<String, String> badCusipTicker = null;
		
		try{
			cusipTicker = (Hashtable<String, String>) loadData(tempFolder+"cusipTicker.data");
			badCusipTicker = (Hashtable<String, String>) loadData(tempFolder+"badCusipTicker.data");
		} catch (IOException e) {
			try {
				cusipTicker = (Hashtable<String, String>) loadData(tempFolder+"cusipTicker.data.temp");
				badCusipTicker = (Hashtable<String, String>) loadData(tempFolder+"badCusipTicker.data.temp");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (ClassNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		if(cusipTicker == null)
			cusipTicker = new Hashtable<String, String>();
		if(badCusipTicker == null)
			badCusipTicker = new Hashtable<String, String>();
		
		System.out.println("Number stored cusip " + cusipTicker.size());
		System.out.println("Number stored badCusip " + badCusipTicker.size());
		//TODO had a check to see if there are a lot of badCusips
		String tick;
		for(String c: cusips){
			//System.out.println(c + " " + newHoldings.get(c));
			if(!cusipTicker.containsKey(c) && !badCusipTicker.containsKey(c)){
				tick = getTicker(c);
				System.out.println(tick+" Cusip:"+c);
				if(!tick.equals("")){
//					System.out.println(tick);
					cusipTicker.put(c, tick);
				}else
					badCusipTicker.put(c, tick);
			}
		}
		
		//save the data twice in case determination causes a EOFException
		try {
			saveData(tempFolder+"cusipTicker.data.temp", cusipTicker);
			saveData(tempFolder+"badCusipTicker.data.temp", badCusipTicker);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			saveData(tempFolder+"cusipTicker.data", cusipTicker);
			saveData(tempFolder+"badCusipTicker.data", badCusipTicker);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	public void saveData(String filename, Object obj) throws IOException{
		File n = new File(filename);
		if(!n.exists())
			n.createNewFile();
		FileOutputStream f_out = new FileOutputStream(filename);

		// Write object with ObjectOutputStream
		ObjectOutputStream obj_out = new ObjectOutputStream (f_out);

		// Write object out to disk
		obj_out.writeObject (obj);
	}
	
	public Object loadData(String filename) throws IOException, ClassNotFoundException{
		File n = new File(filename);
		if(!n.exists())
			return null;
		// Read from disk using FileInputStream
		FileInputStream f_in = new FileInputStream(filename);

		// Read object using ObjectInputStream
		ObjectInputStream obj_in = new ObjectInputStream (f_in);

		// Read an object
		Object obj = obj_in.readObject();
		
		return obj;

	}
	
	/*
	 * private static int getRecent13FQuarter(){ //Get Time Calendar time = new
	 * GregorianCalendar(); return get13FQuarter(time.getTime()); }
	 * 
	 * private static int get13FQuarter(Date time){ //1st Quarter: October 1,
	 * 2010 - December 31, 2010 //2nd Quarter: January 1, 2011 - March 31, 2011
	 * //3rd Quarter: April 1, 2011 - June 30, 2011 //4th Quarter: July 1, 2011
	 * - September 30, 2011 Calendar day = new GregorianCalendar();
	 * day.setTime(time);
	 * 
	 * 
	 * if(day.MONTH < Calendar.MARCH) return 1; else if(day.MONTH <
	 * Calendar.JUNE) return 2; else if(day.MONTH < Calendar.SEPTEMBER) return
	 * 3; else return 4;
	 * 
	 * }
	 * 
	 * private static int getNum13FsOnline(int quarter){
	 * 
	 * }
	 * 
	 * public static int getNum13FsStored(int quarter){
	 * 
	 * }
	 */
}
