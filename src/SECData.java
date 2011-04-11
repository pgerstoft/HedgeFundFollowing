import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//TODO Run through getMaches for each 13F, fix any problems...
//TODO getSharesOutsanding and do Concentration
//TODO Set up Stock
//TODO Set up Financial Data
//TODO check that you are connected to the internet!
//TODO compute 13F one quarter return
//TODO how do you stock split? 


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
		return downloadAndStoreSEC13Fs(getMostRecentFinishedQuarter(), overRide);
	}
	
	public static Quarter getMostRecentFinishedQuarter(){ 
		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH);
		return new Quarter(year, month).getPreviousQuarter();
	  }
	
	
	public int downloadAndStoreSEC13Fs(Quarter quarterDir, boolean overRide) throws IOException, ClassNotFoundException {
		// check if the 13Fs have been downloaded
		// by looking at the company.idx folder
		
		Lib.assertTrue(isValidQuarter(quarterDir));
		
		if(!overRide){
			getCompanyIdx(quarterDir); //DO THIS EVERYTIME?
			createCompanyIdx13F(quarterDir); //QUICK
			resetnumFilesRead();
			get13FsFromSEC(quarterDir); 
		}
		parseSEC13Fs(quarterDir);
		return 1;
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
	private void getCompanyIdx(Quarter quarterDir) {
		Lib.assertTrue(isValidQuarter(quarterDir));
		
		// "ftp.sec.gov anonymous pgerstoft@berkeley.edu edgar/full-index/2010/QTR1/company.idx compay.idx";
		createFolders(sec13FsLocalDir + quarterDir);
		String[] ftpCommand = { ftpSEC, ftpSECUser, ftpSECPassword,
				secRemoteFullDir + quarterDir + companyIdx,
				sec13FsLocalDir + quarterDir + companyIdx };
		Ftp.ftpSingleFile(ftpCommand);
	}

	//Store all lines with 13F-HR in company.idx from the inputted quarter
	private void createCompanyIdx13F(Quarter quarterDir) {
		Lib.assertTrue(isValidQuarter(quarterDir));
		
		String file2Read = "company.idx";
		File compIdx = new File(sec13FsLocalDir + quarterDir + file2Read);
		Lib.assertTrue(compIdx.exists(), "Must download company.idx for: "+ quarterDir);

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
	private void get13FsFromSEC(Quarter quarterDir) {
		
		File companyIdx13FFile = new File(sec13FsLocalDir + quarterDir+ companyIdx13F);
		Lib.assertTrue(companyIdx13FFile.exists());
			
		ArrayList<String> filesRemote13F = get13FsFilesFromCompanyIdx(companyIdx13FFile);
		Lib.assertTrue(filesRemote13F.size() != 0);
		
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

		Lib.assertTrue(companyIdx13FFile.getPath().endsWith(companyIdx13F));

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

				Lib.assertTrue(parentAndFile.endsWith(".txt"));

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


	private void parseSEC13Fs(Quarter quarterDir) throws IOException, ClassNotFoundException{
		
		Lib.assertTrue(isValidQuarter(quarterDir));
		
		File[] allFiles = new File(sec13FsLocalDir + quarterDir + sec13FsFilingDir).listFiles();
		File13F f13F = null;
		Set<Cusip> cusips = new TreeSet<Cusip>();
		
		//For WRDS
//		DB.deleteTempTable();
//		DB.createTempCusipTable();
		
		Integer numFilesRead = (Integer) loadData(tempFolder+"formatSEC13FsIndex.data");		
		if(numFilesRead == null || numFilesRead == allFiles.length)
			numFilesRead = 0;
		
		Lib.assertTrue(allFiles == null || allFiles.length == 0, "No files for: "+ quarterDir);
		
		for(int ii = numFilesRead; ii< allFiles.length; ii++){
			System.out.println(allFiles[ii].getPath() + " " + (ii + 1) + " / " + allFiles.length);
			try{
				f13F = new File13F(allFiles[ii]);
			}catch(Exception e){ continue;}
			//0000950123-11-012552.txt
			//f13F = new File13F(new File("filings/13Fs/2009/QTR2/data/0000312069-09-000043.txt"));
			Hashtable<Cusip, Holding> newHoldings = f13F.getFund().getHoldings();

			cusips = newHoldings.keySet();	
			
			//if is not current quarter, for wrds
//			for(String c:cusips){
//				DB.insertTempCusipTable(c);
//			}
			
			
			
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
		    }else{
		    	//if is current holding quarter
		    	//storeCusips(cusips, quarterDir);
		    	//TODO allHoldings should also keep track of which Funds have already been added
		    	storeFundInDB(f13F.getFund(), allFiles[ii].getPath());
		    }
			
			saveData(tempFolder+"formatSEC13FsIndex.data", ii+1);
		}
//		setStockPrices(quarterDir);
		verifyDatabase(quarterDir);
		DB.getInstance().updateNumHoldings(quarterDir);
		System.out.println("DONE");
//		DB.writeTempVals();
	}
	
//	private void setStockPrices(Quarter quarter){
//		ArrayList<String> cusips = DB.getCusipsHeldByAtLeast(10, quarter);
//		for(String cusip :  cusips){
//			ArrayList<Double> prices = DB.getValueDividedByShares(cusip, quarter);
//			
//			DB.setStockPrice(cusip, quarter, prices.get(prices.size()/2)*1000);
//		}
//	}
	
	private void verifyDatabase(Quarter quarter) throws IOException{
		//calculate price for all stocks
		BufferedWriter out = new BufferedWriter(new FileWriter("BADCIKS.txt"));
		ArrayList<String> ciks = DB.getCIKS(quarter);
		ArrayList<Cusip> cusipsHeldByAtLeast10 = DB.getCusipsHeldByAtLeast(10, quarter);
		Cusip matchedCusip = null;
		double correctPrice;
		double foundPrice;
		double lowerBound = 1.0 - .2;
		double upperBound = 1.0 + .2;
		double valueMultiplier = 1000;
		String file = "";
		for(String cik :  ciks){
			matchedCusip = null;
			ArrayList<Cusip> cusipsForCik = DB.getCusipsHeldBy(cik, quarter);
			for(Cusip c : cusipsForCik){
				if(cusipsHeldByAtLeast10.contains(c)){
					matchedCusip = c;
					break;
				}
			}
			if(matchedCusip ==  null)
				continue;
			
			correctPrice = DB.getPrice(matchedCusip, quarter);
			foundPrice = DB.getFundValueDividedByShare(matchedCusip, cik, quarter);
			
			if(upperBound*(correctPrice/valueMultiplier) > foundPrice && lowerBound*(correctPrice/valueMultiplier) < foundPrice)
				continue;
			
			if(upperBound*correctPrice > foundPrice && lowerBound*correctPrice < foundPrice)
				continue;
			if(upperBound*(correctPrice/valueMultiplier) > foundPrice *valueMultiplier && lowerBound*(correctPrice/valueMultiplier) < foundPrice* valueMultiplier)
				continue;
//			System.out.println("CIK: " + cik + "  " + matchedCusip + " " + foundPrice + " correct: "+ correctPrice);
//			System.exit(1);
			file = DB.getFileName(cik, quarter);
			DB.removeFund(cik, quarter);
			
			if(upperBound*correctPrice > 1/foundPrice && lowerBound*correctPrice < 1/foundPrice){
				//file has value shares switched
				storeFundInDB(new File13F(file, true).getFund(), file);
				System.out.println("1: CIK: " + cik);
				continue;
			}
			if(upperBound*(correctPrice/valueMultiplier) > (1/foundPrice) && lowerBound*(correctPrice/valueMultiplier) < 1/foundPrice){
				//file has value shares switched
				storeFundInDB(new File13F(file, true).getFund(), file);
				System.out.println("2: CIK: " + cik);
				continue;
			}
			if(upperBound*(correctPrice/valueMultiplier) > (1/foundPrice) * valueMultiplier && lowerBound*(correctPrice/valueMultiplier) < 1/foundPrice *valueMultiplier){
				//file has value shares switched
				storeFundInDB(new File13F(file, true).getFund(), file);
				System.out.println("3: CIK: " + cik);
				continue;
			}
//			System.out.println("BAD");
			//Add to list of bad cusips move on
			out.write(file + "\n");
		}
		
		out.close();
	}
	
	private void storeFundInDB(Fund f, String fileName){
		Lib.assertTrue(f.isValidFund());
		
		ArrayList<Cusip> cusips  = DB.getCusips(f.getQuarter());
		
		DB database = DB.getInstance();
		database.insertHedgeFund(f.getCIK(), f.getFundName(), f.getQuarter(), fileName);
		double value;
		double shares;
		for(Cusip cusip: f.getHoldings().keySet()){
			if(!cusips.contains(cusip))
				continue;
			value = f.getHoldings().get(cusip).getValue();
			shares = f.getHoldings().get(cusip).getShares();
			database.insertHedgeFundHoldings(cusip, f.getCIK(), value, shares, f.getQuarter());
		}
	}

	
	private void storeCUSIPTickerInDB(Cusip cusip, String tick, Quarter quarter){
		Lib.assertTrue(isValidQuarter(quarter));
		
		DB databaseDb = DB.getInstance();
		databaseDb.insertCusipTicker(cusip, tick, quarter);
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
				Thread.sleep(Lib.convertSecondToMillis(.25));
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
		Lib.assertNotReached("No match in Shares Outstanding");
		return 0;
	}
	
	private String getTicker(Cusip cusip) {

		StringBuffer bf = new StringBuffer();
		try {
			URL url = new URL(
					"http://activequote.fidelity.com/mmnet/SymLookup.phtml?QUOTE_TYPE=&scCode=E&searchBy=C&searchFor="
							+ cusip);
			try {
				Thread.sleep(Lib.convertSecondToMillis(.25));
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
	private void storeCusips(Set<Cusip> cusips, Quarter quarterDir){
		
		Hashtable<Cusip, String> badCusipTicker = null;
		
		try{
			badCusipTicker = (Hashtable<Cusip, String>) loadData(tempFolder+"badCusipTicker.data");
		} catch (IOException e) {
			try {
				badCusipTicker = (Hashtable<Cusip, String>) loadData(tempFolder+"badCusipTicker.data.temp");
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

		if(badCusipTicker == null)
			badCusipTicker = new Hashtable<Cusip, String>();
		
		System.out.println("Number stored badCusip " + badCusipTicker.size());
		//TODO had a check to see if there are a lot of badCusips
		
		String tick;
		for(Cusip cusip: cusips){
			if(DB.getTickerFromCusip(cusip, quarterDir) == null && !badCusipTicker.containsKey(cusip)){
				tick = getTicker(cusip);
				System.out.println(tick+" Cusip:"+cusip);
				if(!tick.equals("")){
					storeCUSIPTickerInDB(cusip, tick, quarterDir);
				}else
					badCusipTicker.put(cusip, tick);
			}
		}
		
		//save the data twice in case determination causes a EOFException
		try {
			saveData(tempFolder+"badCusipTicker.data.temp", badCusipTicker);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
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
	
	
	public boolean isValidQuarter(Quarter quarter){
		return quarter.compareTo(getMostRecentFinishedQuarter()) >= 0;
	}

}
