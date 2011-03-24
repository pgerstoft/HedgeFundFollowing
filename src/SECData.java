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
//TODO check that you are connected to the internet!



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

	private static final SECData INSTANCE = new SECData(); //UNSURE I WANT TO DO SINGLETON

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

	public int downloadCurrentSEC13Fs(boolean overRide) throws IOException, ClassNotFoundException{
		return downloadSEC13Fs(getCurrent13FQuarter(), overRide);
	}
	
	public int downloadSEC13Fs(String quarterDir, boolean overRide) throws IOException, ClassNotFoundException {
		// check if the 13Fs have been downloaded
		// by looking at the company.idx folder
		
		verifyQuarterDir(quarterDir);
		
		if(!overRide){
			getCompanyIdx(quarterDir); //DO THIS EVERYTIME?
			createCompanyIdx13F(quarterDir); //QUICK
			resetnumFilesRead();
			get13FsFromSEC(quarterDir); 
		}
		formatSEC13Fs(quarterDir);
		return 1;
	}
	
	private static void verifyQuarterDir(String quarterDir){
		String[] fields = quarterDir.split("/");
		Calendar cal = Calendar.getInstance();
		int currentYear = cal.get(Calendar.YEAR);
		try{
			if(fields.length != 2 || !fields[1].matches("QTR[1-4]")
					|| (new Double(fields[0]) < 1990 || new Double(fields[0]) > currentYear)
					|| !quarterDir.endsWith("/")){
				printStatementError("inputted quarter does not follow format /Year/QRT{quarter number} "+ quarterDir);
			}
		}catch(Exception e){
			printStatementError("inputted quarter does not follow format /Year/QRT{quarter number} "+ quarterDir);
		}
		
		int month = cal.get(Calendar.MONTH);
		  //Get Time
		int currentQuarter;
		
		  if(month < Calendar.MARCH) 
			  currentQuarter = 4;
		  else if(month  <Calendar.JUNE) 
			  currentQuarter = 1; 
		  else if(month < Calendar.SEPTEMBER) 
			  currentQuarter =2;
		  else currentQuarter = 3; 
		  
		  if(new Double(fields[0]) == currentYear &&  new Double(fields[1].substring(3))  > currentQuarter ){
			  printStatementError("inputted quarter is in the future current quarter: " 
					  + currentQuarter + " input: " +quarterDir);
		  }
	}

	private void resetnumFilesRead(){
		try {
			saveData(tempFolder+"formatSEC13FsIndex.data", 0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); 
		}
	}
	
	//Downloads company.idx from the SEC for inputted quarter	
	private void getCompanyIdx(String quarterDir) {
		verifyQuarterDir(quarterDir);
		// "ftp.sec.gov anonymous pgerstoft@berkeley.edu edgar/full-index/2010/QTR1/company.idx compay.idx";
		createFolders(sec13FsLocalDir + quarterDir);
		String[] ftpCommand = { ftpSEC, ftpSECUser, ftpSECPassword,
				secRemoteFullDir + quarterDir + companyIdx,
				sec13FsLocalDir + quarterDir + companyIdx };
		Ftp.ftpSingleFile(ftpCommand);
	}

	//Store all lines with 13F-HR in company.idx from the inputted quarter
	private void createCompanyIdx13F(String quarterDir) {
		verifyQuarterDir(quarterDir);
		String file2Read = "company.idx";
		File compIdx = new File(sec13FsLocalDir + quarterDir + file2Read);
		if(!compIdx.exists())
			printStatementError("Must download company.idx for: "+ quarterDir);
		File compIdx13F = new File(sec13FsLocalDir + quarterDir + file2Read
				+ ".13F");

		try {
			compIdx13F.createNewFile();
			Grep.grep(compIdx, compIdx13F, "13F-HR[^/A]");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	//downloads 13Fs from SEC
	private void get13FsFromSEC(String quarterDir) {
		
		File companyIdx13FFile = new File(sec13FsLocalDir + quarterDir+ companyIdx13F);
		if (!companyIdx13FFile.exists()){
			printStatementError("Download " + companyIdx + " before getting 13Fs");
		}
			
		ArrayList<String> filesRemote13F = get13FsFilesFromCompanyIdx(companyIdx13FFile);
		if(filesRemote13F.size() == 0){
			printStatementError("No 13Fs in CompanyIdx for quarter:" + quarterDir);
		}
		
		String localFilingDir = sec13FsLocalDir + quarterDir + sec13FsFilingDir;
		
		//if we have already downloaded all the files, check the number downloaded,
		String localDir = sec13FsLocalDir + quarterDir;
		File dir = new File(localDir);
		int numFiles = 0;
		if(dir.exists()){		
			File[] localDirFiles = dir.listFiles();
			for(File d : localDirFiles){
				if(d.isDirectory())
					numFiles += d.list().length;
			}
		}
		
		if(numFiles == filesRemote13F.size())
			return;
		System.out.println(numFiles);
		System.out.println(filesRemote13F.size());
		ArrayList<String> filesLocal13F = new ArrayList<String>();
		String[] split;
		for (String path : filesRemote13F) {
			split = path.split("/");
			filesLocal13F.add(split[split.length - 1]);
		}
		
		
		createFolders(localFilingDir);
		String[] ftpCommand = { ftpSEC, ftpSECUser, ftpSECPassword, secRemoteDataDir, localFilingDir };
		Ftp.ftpMultipleFiles(ftpCommand, filesRemote13F, filesLocal13F);
	}

	//returns a list of File names in companyIdx13F
	private ArrayList<String> get13FsFilesFromCompanyIdx(
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


	@SuppressWarnings("unchecked")
	public void formatSEC13Fs(String quarterDir) throws IOException, ClassNotFoundException{
		
		verifyQuarterDir(quarterDir);
		
		File[] allFiles = new File(sec13FsLocalDir + quarterDir + sec13FsFilingDir).listFiles();
		File13F f13F;
		Set<String> cusips;
		
		Hashtable<String, Holding> allHoldings = null;
		allHoldings = (Hashtable<String, Holding>) loadData(tempFolder+"allHoldings.data");		
		if(allHoldings == null)
			allHoldings = new Hashtable<String, Holding>();
		
		Integer numFilesRead = 0;		
		numFilesRead = (Integer) loadData(tempFolder+"formatSEC13FsIndex.data");		
		if(numFilesRead == null)
			numFilesRead = 0;
		
		if(allFiles == null || allFiles.length == 0){
			printStatementError("No files for: "+ quarterDir);
		}
		
		for(int ii = numFilesRead; ii< allFiles.length; ii++){
			System.out.println(allFiles[ii].getPath() + " " + ii);
//			if(!File13F.isValueBeforePrice(allFiles[ii])){
//				System.out.println("VALUE ERROR");
//				Runtime.getRuntime().exec("open "+ allFiles[ii].getCanonicalPath());
//				System.exit(1);
//			}
			f13F = new File13F(allFiles[ii]);
			//f13F = new File13F(new File("filings/13Fs/2010/QTR1/data/0000950123-10-013284.txt"));
//			System.out.println(f13F.getFund());
//			System.exit(1);
			Hashtable<String, Holding> newHoldings = f13F.getFund().getHoldings();
			
			//TODO save fund and holdings
			
			cusips = newHoldings.keySet();			
			
			if(cusips.size() == 0 && f13F.isFileSmall()){
				createFolders(sec13FsLocalDir+quarterDir+"NoHoldings/");
				allFiles[ii].renameTo(new File(new File(sec13FsLocalDir+quarterDir+"NoHoldings/"), allFiles[ii].getName()));
				
				//update allFiles to reflect file removal
				allFiles = new File(sec13FsLocalDir + quarterDir + sec13FsFilingDir).listFiles(); 
				ii = ii - 1;
			}else if( cusips.size() == 0){
				createFolders(sec13FsLocalDir+quarterDir+"MaybeNoHoldings/");
				allFiles[ii].renameTo(new File(new File(sec13FsLocalDir+quarterDir+"MaybeNoHoldings/"), allFiles[ii].getName()));
				System.out.println("Dont think this is a bad file, " +f13F.getMatchType() + " " + allFiles[ii].getPath());
				Runtime.getRuntime().exec("open "+ allFiles[ii].getCanonicalPath());

				//update allFiles to reflect file removal
				allFiles = new File(sec13FsLocalDir + quarterDir + sec13FsFilingDir).listFiles(); 
				ii = ii - 1;
//				System.exit(1);
		    }else{	
		    	storeCusips(cusips);
		    	allHoldings = combineTableEntries(allHoldings, newHoldings);
		    	//TODO allHoldings should also keep track of which Funds have already been added
		    	saveData(tempFolder+"allHoldings.data", allHoldings);
		    	storeFundInDB(f13F.getFund(), quarterDir);
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
	
	public void storeFundInDB(Fund f, String quarter){
		if(!f.isValidFund()){
			System.err.println("Fund is not valid cannot store");
			System.exit(1);
		}
		
		DB database = new DB();
		database.createConnection();
		database.insertHedgeFund(f.getCIK(), f.getFundName());
		double value;
		double shares;
		for(String cusip: f.getHoldings().keySet()){
			value = f.getHoldings().get(cusip).getValue();
			shares = f.getHoldings().get(cusip).getShares();
			database.insertHedgeFundHoldings(cusip, f.getCIK(), value, shares, quarter);
		}
		database.shutdown();
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
		

	public Hashtable<String, Double> getSharesOuststanding(Set<String> tickers){
		Hashtable<String,Double> tickersToShares = new Hashtable<String, Double>();
		
		double shares;
		for(String tick: tickers){
			shares = getSharesOutstanding(tick);
			tickersToShares.put(tick, shares);
			System.out.println(tick + " " +  shares);
		}
		
		return tickersToShares;
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
	
	private static void printStatementError(String statement) {
		System.err.println(statement);
		System.exit(1);
	}
	
	
	
	public static String getCurrent13FQuarter(){ 
		String r;
		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH);
		  //Get Time 
		  if(month < Calendar.MARCH) 
			   r = (year-1) + "/QTR" + 4+ "/"; 
		  else if(month  <Calendar.JUNE) 
			  r = year + "/QTR" + 1 + "/"; 
		  else if(month < Calendar.SEPTEMBER) 
			  r =  year + "/QTR" + 2 + "/";
		  else r = year + "/QTR" + 3 + "/"; 
		  
		  return r;
	  }
	  
	  
	 /* 
	 * private static int getNum13FsOnline(int quarter){
	 * 
	 * }
	 * 
	 * public static int getNum13FsStored(int quarter){
	 * 
	 * }
	 */
}
