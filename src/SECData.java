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
//TODO how do you stock split? Look at adjusted and unadjusted yahoo

//TOMORROW: Save cusips to file
//GET WRDS AND CRSP tomorrow

//WRITE CODE TO USE WEKA!

public class SECData extends Data{

	public static final String ftpSEC = "ftp.sec.gov";
	public static final String ftpSECUser = "anonymous";
	public static final String ftpSECPassword = "pgerstoft@berkeley.edu";
	public static final String sec13FHardDrive = "/Volumes/gerstoftbackup/Stocks/";
	// public static final String sec13FsLocalDir = sec13FHardDrive+
	// "filings/13Fs/";
	public static final String sec13FsLocalDir = "filings/13Fs/";
	public static final String sec13FsFilingDir = "data/";
	public static final String secRemoteFullDir = "edgar/full-index/";
	public static final String secRemoteDataDir = "edgar/data/";
	private static String tempFolder = "temp/";
	public static String companyIdx = "company.idx";
	private static String companyIdx13F = "company.idx.13F";

	private Quarter quarter;
	
	public SECData() {
		this(getMostRecentFinishedQuarter());
	}
	
	public SECData(Quarter q) {
		createFolders(sec13FsLocalDir);
		createFolders(tempFolder);
		setQuarter(q);
	}
		
	public void setQuarter(Quarter quarter) {
		Lib.assertTrue(isValidQuarter(quarter));
		this.quarter = quarter;
	}

	public Quarter getQuarter() {
		return quarter;
	}
	
	public boolean isValidQuarter(Quarter quarter) {
		return quarter.compareTo(getMostRecentFinishedQuarter()) <= 0;
	}

	public int downloadAndStoreSEC13Fs(boolean overRide) {
		// check if the 13Fs have been downloaded
		// by looking at the company.idx folder

		
		if (!overRide) {
			System.out.println("Get Company.Idx");
			getCompanyIdx(); // DO THIS EVERYTIME?
			System.out.println("Create Company.Idx13F");
			createCompanyIdx13F(); // QUICK
			get13FsFromSEC();
			resetnumFilesRead();
		}
		
		parseSEC13Fs();
		resetnumFilesRead();
		return 1;
	}

	private void resetnumFilesRead() {
		saveData(tempFolder + "formatSEC13FsIndex.data", 0);
	}

	// Downloads company.idx from the SEC for inputted quarter
	private void getCompanyIdx() {

		// "ftp.sec.gov anonymous pgerstoft@berkeley.edu edgar/full-index/2010/QTR1/company.idx compay.idx";
		createFolders(sec13FsLocalDir + quarter);
		File companyIdxFile = new File(sec13FsLocalDir + quarter
				+ companyIdx);
		int minCompanyIdxFile = 10000000;
		if (companyIdxFile.exists()
				&& companyIdxFile.length() > minCompanyIdxFile)
			return;
		String[] ftpCommand = { ftpSEC, ftpSECUser, ftpSECPassword,
				secRemoteFullDir + quarter + companyIdx,
				sec13FsLocalDir + quarter + companyIdx };
		Ftp.ftpSingleFile(ftpCommand);
	}

	// Store all lines with 13F-HR in company.idx from the inputted quarter
	private void createCompanyIdx13F() {

		String file2Read = "company.idx";
		File compIdx = new File(sec13FsLocalDir + quarter + file2Read);
		Lib.assertTrue(compIdx.exists(), "Must download company.idx for: "
				+ quarter);

		File compIdx13F = new File(sec13FsLocalDir + quarter + file2Read
				+ ".13F");

		System.gc();

		try {
			compIdx13F.createNewFile();
			Grep.grep(compIdx, compIdx13F, "13F-HR[^/A]");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// downloads 13Fs from SEC
	private void get13FsFromSEC() {

		File companyIdx13FFile = new File(sec13FsLocalDir + quarter
				+ companyIdx13F);
		Lib.assertTrue(companyIdx13FFile.exists());

		ArrayList<String> filesRemote13F = get13FsFilesFromCompanyIdx(companyIdx13FFile);
		Lib.assertTrue(filesRemote13F.size() != 0);

		String localFilingDir = sec13FsLocalDir + quarter + sec13FsFilingDir;

		// if we have already downloaded all the files, check the number
		// downloaded,
		String localDir = sec13FsLocalDir + quarter;
		File dir = new File(localDir);
		int numFiles = 0;
		if (dir.exists()) {
			File[] localDirFiles = dir.listFiles();
			for (File d : localDirFiles) {
				if (d.isDirectory())
					numFiles += d.list().length;
			}
		}
		System.out.println(Math.abs(numFiles - filesRemote13F.size()));

		if (Math.abs(numFiles - filesRemote13F.size()) <= 100)
			return;

		System.out.println(Math.abs(numFiles - filesRemote13F.size()));
		ArrayList<String> filesLocal13F = new ArrayList<String>();
		String[] split;
		for (String path : filesRemote13F) {
			split = path.split("/");
			filesLocal13F.add(split[split.length - 1]);
		}

		createFolders(localFilingDir);
		String[] ftpCommand = { ftpSEC, ftpSECUser, ftpSECPassword,
				secRemoteDataDir, localFilingDir };
		Ftp.ftpMultipleFiles(ftpCommand, filesRemote13F, filesLocal13F);
	}

	// returns a list of File names in companyIdx13F
	private ArrayList<String> get13FsFilesFromCompanyIdx(File companyIdx13FFile) {

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
				// /edgar/data/CIK/0123456789ABCDEFGH/0123456789-AB-CDEFGH.txt
				middleVal = forsplitting[forsplitting.length - 1].split("-")[1];
				noTxtNoDashes = forsplitting[forsplitting.length - 1]
						.replaceAll("-", "").replace(".txt", "");
				parentAndFile = forsplitting[forsplitting.length - 2] + "/"
						// + middleVal + "/"
						+ noTxtNoDashes + "/"
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

	private void parseSEC13Fs() {

		File[] allFiles = new File(sec13FsLocalDir + quarter
				+ sec13FsFilingDir).listFiles();
		
		File13F f13F = null;
		Set<Cusip> cusips = new TreeSet<Cusip>();

		// For WRDS
		// DB.deleteTempTable();
		// DB.createTempCusipTable();

		Integer numFilesRead = null;
		try {
			numFilesRead = (Integer) loadData(tempFolder
					+ "formatSEC13FsIndex.data");
		} catch (IOException e) {
			e.printStackTrace();
		}


		
		if (numFilesRead == null || numFilesRead == allFiles.length)
			numFilesRead = 0;
		
		
		Lib.assertTrue(allFiles != null && allFiles.length != 0,
				"No files for: " + quarter);

		for (int ii = numFilesRead; ii < allFiles.length; ii++) {
			System.out.println(allFiles[ii].getPath() + " " + (ii + 1) + " / "
					+ allFiles.length);
			try {
				f13F = new File13F(allFiles[ii]);
			} catch (Exception e) {
				System.out.println("Exception");
				continue;
			}

			// 0000950123-11-012552.txt
			// f13F = new File13F(new
			// File("filings/13Fs/2009/QTR2/data/0000312069-09-000043.txt"));
			Hashtable<Cusip, Holding> newHoldings = f13F.getFund()
					.getHoldings();

			cusips = newHoldings.keySet();

			// if is not current quarter, for wrds
			for (Cusip c : cusips) {
				DB.insertTempCusipTable(c);
			}

			if (cusips.size() == 0 && f13F.isFileSmall()) {
				createFolders(sec13FsLocalDir + quarter + "NoHoldings/");
				allFiles[ii].renameTo(new File(new File(sec13FsLocalDir
						+ quarter + "NoHoldings/"), allFiles[ii].getName()));
				// update allFiles to reflect file removal
				allFiles = new File(sec13FsLocalDir + quarter
						+ sec13FsFilingDir).listFiles();
				ii = ii - 1;
			} else if (cusips.size() == 0) {
				createFolders(sec13FsLocalDir + quarter + "MaybeNoHoldings/");
				allFiles[ii].renameTo(new File(new File(sec13FsLocalDir
						+ quarter + "MaybeNoHoldings/"), allFiles[ii]
						.getName()));
				System.out.println("Dont think this is a bad file, "
						+ f13F.getMatchType() + " " + allFiles[ii].getPath());
				try {
					Runtime.getRuntime().exec(
							"open " + allFiles[ii].getCanonicalPath());
				} catch (IOException e) {
					e.printStackTrace();
				}
				// update allFiles to reflect file removal
				allFiles = new File(sec13FsLocalDir + quarter
						+ sec13FsFilingDir).listFiles();
				ii = ii - 1;
			} else {
				// if is current holding quarter
				// storeCusips(cusips, quarterDir);
				storeFundInDB(f13F.getFund(), allFiles[ii].getPath());
			}

			saveData(tempFolder + "formatSEC13FsIndex.data", ii + 1);
		}

		// verifyDatabase(quarterDir);
		// DB.getInstance().updateNumHoldings(quarterDir);
		System.out.println("DONE");
		// DB.writeTempVals();
	}

	private void correctDatabase() throws IOException {
		// calculate price for all stocks
		BufferedWriter out = new BufferedWriter(new FileWriter("BADCIKS.txt"));
		ArrayList<CIK> ciks = DB.getCIKS(quarter);
		ArrayList<Cusip> cusipsHeldByAtLeast10 = DB.getCusipsHeldByAtLeast(10,
				quarter);
		Cusip matchedCusip = null;
		double correctPrice;
		double foundPrice;
		double lowerBound = 1.0 - .2;
		double upperBound = 1.0 + .2;
		double valueMultiplier = 1000;
		String file = "";
		for (CIK cik : ciks) {
			matchedCusip = null;
			ArrayList<Cusip> cusipsForCik = DB.getCusipsHeldBy(cik, quarter);
			for (Cusip c : cusipsForCik) {
				if (cusipsHeldByAtLeast10.contains(c)) {
					matchedCusip = c;
					break;
				}
			}
			if (matchedCusip == null)
				continue;

			correctPrice = DB.getPrice(matchedCusip, quarter);
			foundPrice = DB.getFundValueDividedByShare(matchedCusip, cik,
					quarter);

			// TODO if 1000 off divide all values by 1000

			if (upperBound * (correctPrice / valueMultiplier) > foundPrice
					&& lowerBound * (correctPrice / valueMultiplier) < foundPrice)
				continue;

			if (upperBound * correctPrice > foundPrice
					&& lowerBound * correctPrice < foundPrice)
				continue;
			if (upperBound * (correctPrice / valueMultiplier) > foundPrice
					* valueMultiplier
					&& lowerBound * (correctPrice / valueMultiplier) < foundPrice
							* valueMultiplier)
				continue;
			// System.out.println("CIK: " + cik + "  " + matchedCusip + " " +
			// foundPrice + " correct: "+ correctPrice);
			// System.exit(1);
			file = DB.getFileName(cik, quarter);
			DB.removeFund(cik, quarter);
			// THIS IS DUMB JUST SWITCH IT YOURSELF!
			if (upperBound * correctPrice > 1 / foundPrice
					&& lowerBound * correctPrice < 1 / foundPrice) {
				// file has value shares switched
				storeFundInDB(new File13F(file, true).getFund(), file);
				System.out.println("1: CIK: " + cik);
				continue;
			}
			if (upperBound * (correctPrice / valueMultiplier) > (1 / foundPrice)
					&& lowerBound * (correctPrice / valueMultiplier) < 1 / foundPrice) {
				// file has value shares switched
				storeFundInDB(new File13F(file, true).getFund(), file);
				System.out.println("2: CIK: " + cik);
				continue;
			}
			if (upperBound * (correctPrice / valueMultiplier) > (1 / foundPrice)
					* valueMultiplier
					&& lowerBound * (correctPrice / valueMultiplier) < 1
							/ foundPrice * valueMultiplier) {
				// file has value shares switched
				storeFundInDB(new File13F(file, true).getFund(), file);
				System.out.println("3: CIK: " + cik);
				continue;
			}
			// System.out.println("BAD");
			// Add to list of bad cusips move on
			out.write(file + "\n");
		}

		out.close();
	}

	private void storeFundInDB(Fund f, String fileName) {
		Lib.assertTrue(f.isValidFund());

		ArrayList<Cusip> cusips = DB.getCusips(f.getQuarter());

		DB database = DB.getInstance();
		database.insertHedgeFund(f.getCIK(), f.getFundName(), f.getQuarter(),
				fileName);
		double value;
		double shares;
		BufferedWriter out;
		try {
			out = new BufferedWriter(new FileWriter("temp/temp.csv"));
			for (Cusip cusip : f.getHoldings().keySet()) {
				// if(!cusips.contains(cusip))
				// continue;
				value = f.getHoldings().get(cusip).getValue();
				shares = f.getHoldings().get(cusip).getShares();
				out.write(cusip + "," + f.getCIK() + "," + value + "," + shares
						+ "," + 0.0 + "," + f.getQuarter() + "\n");
				// database.insertHedgeFundHoldings(cusip, f.getCIK(), value,
				// shares, 0.0, f.getQuarter());
			}
			out.close();
			DB.batchLoadHedgeFundHoldings(System.getProperty("user.dir")
					+ "/temp/temp.csv");
			// setPortionOfFund(f.getCIK(), f.getQuarter());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setPortionOfFund(CIK cik) {
		BufferedWriter out;
		try {
			out = new BufferedWriter(new FileWriter("temp/temp.csv"));
			Hashtable<Cusip, Double> cusipToShares = DB.getShares(cik, quarter);
			double quarterToFundValue = DB.getFundValue(cik, quarter);
			double fundConcentration;
			System.out.println(cusipToShares);
			for (Cusip cusip : cusipToShares.keySet()) {
				fundConcentration = cusipToShares.get(cusip)
						* DB.getFundValueDividedByShare(cusip, cik, quarter)
						/ quarterToFundValue;
				out.write(cusip + "," + fundConcentration + "," + quarter
						+ " \n");
			}
			out.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		DB.batchSetPortionOfFund(cik, System.getProperty("user.dir")
				+ "/temp/temp.csv");
	}

	private void storeCUSIPTickerInDB(Cusip cusip, String tick, Quarter quarter) {
		Lib.assertTrue(isValidQuarter(quarter));

		DB databaseDb = DB.getInstance();
		databaseDb.insertCusipTicker(cusip, tick, quarter);
	}



//	@SuppressWarnings("unchecked")
//	private void storeCusips(Set<Cusip> cusips, Quarter quarterDir) {
//
//		Hashtable<Cusip, String> badCusipTicker = null;
//
//		try {
//			badCusipTicker = (Hashtable<Cusip, String>) loadData(tempFolder
//					+ "badCusipTicker.data");
//		} catch (IOException e) {
//			try {
//				badCusipTicker = (Hashtable<Cusip, String>) loadData(tempFolder
//						+ "badCusipTicker.data.temp");
//			} catch (IOException e1) {
//				e.printStackTrace();
//			}
//		}
//
//		if (badCusipTicker == null)
//			badCusipTicker = new Hashtable<Cusip, String>();
//
//		System.out.println("Number stored badCusip " + badCusipTicker.size());
//		// TODO had a check to see if there are a lot of badCusips
//
//		String tick;
//		for (Cusip cusip : cusips) {
//			if (DB.getTickerFromCusip(cusip, quarterDir) == null
//					&& !badCusipTicker.containsKey(cusip)) {
//				tick = getTicker(cusip);
//				System.out.println(tick + " Cusip:" + cusip);
//				if (!tick.equals("")) {
//					storeCUSIPTickerInDB(cusip, tick, quarterDir);
//				} else
//					badCusipTicker.put(cusip, tick);
//			}
//		}
//
//		// save the data twice in case determination causes a EOFException
//		saveData(tempFolder + "badCusipTicker.data.temp", badCusipTicker);
//
//		saveData(tempFolder + "badCusipTicker.data", badCusipTicker);
//
//	}
	
	private void saveData(String filename, Object obj) {
		try {
			File n = new File(filename);
			if (!n.exists())
				n.createNewFile();
			FileOutputStream f_out = new FileOutputStream(filename);

			// Write object with ObjectOutputStream
			ObjectOutputStream obj_out = new ObjectOutputStream(f_out);

			// Write object out to disk
			obj_out.writeObject(obj);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Object loadData(String filename) throws IOException {
		Object obj = null;
		File n = new File(filename);
		if (!n.exists())
			return null;
		// Read from disk using FileInputStream
		FileInputStream f_in = new FileInputStream(filename);

		// Read object using ObjectInputStream
		ObjectInputStream obj_in = new ObjectInputStream(f_in);

		// Read an object
		try {
			obj = obj_in.readObject();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return obj;

	}

}
