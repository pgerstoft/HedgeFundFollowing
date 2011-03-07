import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

//TODO Set up Stock
//TODO Set up 

public class SECData {

	public static final String ftpSEC = "ftp.sec.gov";
	public static final String ftpSECUser = "anonymous";
	public static final String ftpSECPassword = "pgerstoft@berkeley.edu";
	public static final String sec13FsLocalDir = "filings/13Fs/";
	public static final String sec13FsFilingDir = "data/";
	public static final String secRemoteFullDir = "edgar/full-index/";
	public static final String secRemoteDataDir = "edgar/data/";
	public static String companyIdx = "company.idx";
	private static String companyIdx13F = "company.idx.13F";

	private static final SECData INSTANCE = new SECData();

	// Private constructor prevents instantiation from other classes
	private SECData() {
		createFolders(sec13FsLocalDir);

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
			// TODO Auto-generated catch block
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

	public Hashtable<String, Holding> combineTableEntries(
			Hashtable<String, Holding> table1, Hashtable<String, Holding> table2) {
		if (table1.isEmpty())
			return table2;
		if (table2.isEmpty())
			return table1;
		for (String key2 : table2.keySet()) {
			if (table1.containsKey(key2)) {
				Holding old = table1.get(key2);
				old.addToHolding(table2.get(key2));
				table1.put(key2, old);
			} else {
				table1.put(key2, table2.get(key2));
			}
		}
		return table1;
	}

	public Hashtable<Holding, String> switchMapping(
			Hashtable<String, Holding> table) {
		Hashtable<Holding, String> newTable = new Hashtable<Holding, String>();

		for (String key : table.keySet())
			newTable.put(table.get(key), key);

		return newTable;
	}

	public String getTicker(String cusip) {

		StringBuffer bf = new StringBuffer();
		try {
			URL url = new URL(
					"http://activequote.fidelity.com/mmnet/SymLookup.phtml?QUOTE_TYPE=&scCode=E&searchBy=C&searchFor="
							+ cusip);
			try {
				Thread.sleep(convertSecondToMillis(.5));
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
		System.out.println(cusip);
		String wholeFile = bf.toString();
		// System.out.println(wholeFile);
		Pattern start = Pattern.compile("SID_VALUE_ID=");
		Pattern end = Pattern.compile("SID_VALUE_ID=[^>]*");
		Matcher mStart = start.matcher(wholeFile);
		Matcher mEnd = end.matcher(wholeFile);
		if (mStart.find() && mEnd.find()) {
			System.out.println(mStart.end() + 1);
			System.out.println(mEnd.end() - 2);
			return wholeFile.substring(mStart.end(), mEnd.end() - 1);
		}
		return "";
	}

	public long convertSecondToMillis(double d) {
		return (long) (1000 * d);
	}

	public void formatSEC13Fs(String quarterDir) throws IOException, ClassNotFoundException{
		File[] allFiles = new File(sec13FsLocalDir + quarterDir + sec13FsFilingDir).listFiles();
		File13F f13F;
		int index = 0;
//		for(File f : allFiles){
		//761??? 1499
		
		Set<String> cusips;
		
		Hashtable<String, Holding> allHoldings = new Hashtable<String, Holding>();
		Hashtable<String, String> cusipTicker = new Hashtable<String, String>();
		Hashtable<String, String> badCusipTicker = new Hashtable<String, String>();
		String tick;
		for(int ii = 0; ii< allFiles.length; ii++){
			System.out.println(allFiles[ii].getPath() + " " + ii);
			f13F = new File13F(allFiles[ii]);
//			f13F = new File13F(new File("filings/13Fs/2010/QTR1/data/0000022356-10-000028.txt"));
			
			Hashtable<String, Holding> newHoldings = f13F.getFund().getHoldings();
			
			//TODO save fund and holdings
			
			cusips = newHoldings.keySet();
			System.out.println(); //cusips.iterator().next()));
			
			for(String c: cusips){
				if(!cusipTicker.contains(c) && cusipTicker.contains(c)){
					tick = getTicker(c);
					if(!tick.equals("")){
						cusipTicker.put(c, tick);
					}else
						badCusipTicker.put(c, tick);
				}
			}
			
//			saveData(cusipTicker);
//			Hashtable<String,String> x = loadData("myobject.data");
//			if(x.equals(cusipTicker))
//				printStatementError("AMMMMMMMAAAZINg");
			//		index = 1/0;
//			System.out.println(f13F.getCompanyName() + " " + ii);
			
			if(cusips.size() == 0 && verifySmallFileSize(allFiles[ii])){
				createFolders(sec13FsLocalDir+quarterDir+"NoHoldings/");
				allFiles[ii].renameTo(new File(new File(sec13FsLocalDir+quarterDir+"NoHoldings/"), allFiles[ii].getName()));
			}else if( cusips.size() == 0){
				createFolders(sec13FsLocalDir+quarterDir+"NoHoldings/");
				allFiles[ii].renameTo(new File(new File(sec13FsLocalDir+quarterDir+"MaybeNoHoldings/"), allFiles[ii].getName()));
				System.out.println("Dont think this is a bad file, " +f13F.getMatchType() + " " + allFiles[ii].getPath());
//				System.exit(1);
		    }else
		    	allHoldings = combineTableEntries(allHoldings, newHoldings);
			
			Hashtable<Holding, String> holdingToString = switchMapping(allHoldings);
			TreeSet<Holding> holdingToStringKeys = new TreeSet<Holding>(holdingToString.keySet());
			Holding val;
			for(int i= 1; i< holdingToStringKeys.size(); i++){
				val = holdingToStringKeys.pollLast();
				System.out.println(val + " " + holdingToString.get(val));
			}
			
			
			
			
			//System.out.println(f.getPath());

//			ArrayList<String> state = Grep.grep(f, null , "STATE:");
//			TODO LOOK At state of incorporation
			//get Fund name
			//get ticker, CUSIPS, value, shares
		}
		saveData("cusipToTicker.data",cusipTicker);
		saveData("allHoldings.data", allHoldings);
		
		
	}
	

	
	public double convert2num(String st){
		
	Pattern start = Pattern.compile("Shares Outstanding");
	Matcher mStart = start.matcher(st);
	if(mStart.find())
		return new Double(st.substring(mStart.end()+1));
	else
		return 0;
	
	}
	
	public double getSharesOutstanding(String ticker){
		StringBuffer bf = new StringBuffer();
		try {
			URL url = new URL("'http://www.google.com/finance?fstype=bi&q="+ ticker);
			try {
				Thread.sleep(convertSecondToMillis(.5));
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
		Pattern end = Pattern.compile("<td align=\"right\" bgcolor=\"#FFFFFF\">&nbsp;<b>");
		Pattern endEnd = Pattern.compile("&nbsp;</b></td>");
		
		Matcher mEnd = end.matcher(wholeFile.substring(mStart.start()));
		Matcher mEndEnd = endEnd.matcher(wholeFile.substring(mStart.start()));
		if (mEnd.find() && mEndEnd.find()) {
			return convert2num(wholeFile.substring(mEnd.end(), mEnd.end() - 1));
		}
		return 0;
	}
	
	public void saveData(String filename, Object obj) throws IOException{
		FileOutputStream f_out = new FileOutputStream(filename);

		// Write object with ObjectOutputStream
		ObjectOutputStream obj_out = new ObjectOutputStream (f_out);

		// Write object out to disk
		obj_out.writeObject (obj);
	}
	
	public Object loadData(String fileName) throws IOException, ClassNotFoundException{
		// Read from disk using FileInputStream
		FileInputStream f_in = new FileInputStream("myobject.data");

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
