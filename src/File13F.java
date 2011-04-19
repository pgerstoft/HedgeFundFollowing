import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//Process the file at opening of the file!
//TODO VERIFY THAT MARKET VALUE COMES BEFORE SHARES
//TODO VERIFY THAT PRICE DOES NOT COME BEFORE EITHER


public class File13F {

	private File f13F;
	private Fund fund13F;
	private String matchType;
	private int numClaimedHoldings;
	private int numLines;
	private boolean sharesValueSwitched = false;
	
	public File13F(String filePath){
		
		this( new File(filePath));
		
	}
	
	public File13F(String filePath, boolean shValSwitch){
		this( new File(filePath));
		sharesValueSwitched = shValSwitch;		
	}
	
	
	public File13F(File f){
		Lib.assertTrue(f.exists());
		//Check input
		f13F = f;
		fund13F = new Fund();
		matchType = "";
		setFundName();
		setFundCIK();
		setFundQuarter();
		try {
			numLines = countNumLines(f13F.getPath());
			initFund();
		} catch (IOException e) {
			e.printStackTrace();
			//System.exit(1);
		}
		
		//post condition
		Lib.assertTrue(fund13F.isValidFund());
	}
	
	
	private String getValueAfter(String s){
		String name = null;
		ArrayList<String> matchingVals;
		try {
			matchingVals = Grep.grep(f13F, null,s);
			name = matchingVals.get(0).split(":")[1];
			Lib.assertTrue(name != null, f13F.getPath());
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return name.trim();
	}

	private void setFundName(){
		fund13F.setFundName(getValueAfter("COMPANY CONFORMED NAME:"));
	}
	
	private void setFundCIK(){
		fund13F.setCIK(new CIK(getValueAfter("CENTRAL INDEX KEY:").trim()));
	}

	private void setFundQuarter(){
		//input is 20110214, YEAR{4}Month{2}DAY{2}
		String date = getValueAfter("FILED AS OF DATE:");
		fund13F.setQuarter(new Date(date).toQuarter());
	}
	
	public Fund getFund(){
		return fund13F;
	}
	
	public String getMatchType(){
		return matchType;
	}
	
	public int getNumClaimedHoldings(){
		return numClaimedHoldings;
	}
	
	//gets All the holdings and stores them in a Fund object
	private void initFund() throws IOException{

		//load file into wholeFile
        String wholeFile = removeLinesWithPutOrCall(f13F);

        //FORMAT file
        wholeFile = wholeFile.replaceAll("[-$\\=\\(\\)|]", "");
        //Needed to get rid of $ in front of value
        //Needed to get rid of - between cusips
        
        setNumClaimedHoldings(wholeFile);

      //get from end of <TABLE> 
        int indxTABLE = wholeFile.indexOf("<TABLE>");
        if(indxTABLE>0)
        	wholeFile = wholeFile.substring(indxTABLE);
        
        //Most common case is where Cusip  Number Number and comma is part of number
        //Get rid of commas to identify Cusip Number; Cusip 70,000 is not Cusip 70 000 (, used a space indentifier)
		ArrayList<String> bestMatch = getListMatches(wholeFile.replaceAll(",", ""));
		if(bestMatch.size() == 0)
			bestMatch = getListMatches(wholeFile);
		
		Lib.assertTrue(!matchType.isEmpty());
		
		System.out.println("Number Claimed: " + numClaimedHoldings+ " Number of lines: " + numLines);
		System.out.println("Number of matched lines: " + getNumLinesMatched(bestMatch) + " Number unique: " +bestMatch.size());
		System.out.println(matchType);
		
		//numClaimedHoldings must be less than or equal to the number of lines (consider one line per holding and heading)
		if(numClaimedHoldings > 0 && !matchedHoldingsCloseToClaimed(wholeFile, getNumLinesMatched(bestMatch))){
			System.err.println("This file's number of matches doesnt match number of claimed holdings: " + f13F.getPath() +"\nNumber of found: "+ getNumLinesMatched(bestMatch)+" Number Claimed in File:"+ numClaimedHoldings);
			System.err.println(matchType);
			System.err.println(bestMatch);
			System.err.println("Number of Lines " + numLines);
//			Runtime.getRuntime().exec("open "+f13F.getCanonicalPath());
		}
		addToHoldingsFromMatches(bestMatch);
	}
	
	private String removeLinesWithPutOrCall(File f){
		ArrayList<String> arrayString  = new ArrayList<String>();
		try {
			arrayString = Grep.grep(f, null, "^((?!\\b(CALL|Call|call|PUT|Put|put)\\b).)*$");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return arrayString.toString().replaceAll("[\\[\\],]", "");
	}
	
	
	private int getNumLinesMatched(ArrayList<String> bestMatch){
		int numLines = 0;
		for(String match: bestMatch){
			numLines += match.split("\n").length;
		}
		return numLines;
	}
	
	private void setNumClaimedHoldings(String wholeFile){
		int minNumLinesHeader = 30;
		numClaimedHoldings = getTableEntryTotal(wholeFile);
		if(numClaimedHoldings > numLines-minNumLinesHeader)
			numClaimedHoldings = -1;
	}
	
	public int getTableEntryTotal(String wholeFile){
		
		Pattern p = Pattern.compile("TABLE[\t\n ]*ENTRY[\t\n ]*TOTAL[\t\n ]*:[\t\n ]*[0-9,]*");
		Pattern p1 = Pattern.compile("Table[\t\n ]*Entry[\t\n ]*Total[\t\n ]*:[\t\n ]*[0-9,]*");
		Pattern p2 = Pattern.compile("table[\t\n ]*entry[\t\n ]*total[\t\n ]*:[\t\n ]*[0-9,]*");
		Matcher m = p.matcher(wholeFile);
		Matcher m1 = p1.matcher(wholeFile);
		Matcher m2 = p2.matcher(wholeFile);

		if(m.find()){
			StringTokenizer k = new StringTokenizer(m.group().replaceAll(",",""));
			String tok;
			while(k.hasMoreTokens()){
				tok = k.nextToken();
				if(stringIsNumber(tok))
					return Integer.parseInt(tok);
			}
		}else if(m1.find()){
			StringTokenizer k = new StringTokenizer(m1.group().replaceAll(",",""));
			String tok;
			while(k.hasMoreTokens()){
				tok = k.nextToken();
				
				if(stringIsNumber(tok))
					return Integer.parseInt(tok);
			}
		}else if(m2.find()){
			StringTokenizer k = new StringTokenizer(m2.group().replaceAll(",",""));
			String tok;
			while(k.hasMoreTokens()){
				tok = k.nextToken();
				if(stringIsNumber(tok))
					return Integer.parseInt(tok);
			}
		}
			
		return -1;
	}
	
	
	/* Functions to verify that number of matches is close to what is expected given the file*/
	
	public boolean matchedHoldingsCloseToClaimed(String wholeFile, int numFound){
		//return false only if numClaimedHoldings not zero OR 
		if(numClaimedHoldings == 0)
			return true;
		return numFound >= numClaimedHoldings - .25*numClaimedHoldings ; //within twenty-five percent
	}
	
	public boolean isFileSmall() {
		return numLines < 200;
	}
	
	public int countNumLines(String filename) throws IOException {
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

	
	private void addToHoldingsFromMatches(ArrayList<String> bestMatch) throws IOException {
		StringTokenizer tempToken;
		String cusipString;
		String valueTemp;
		String sharesTemp;
		int numNotEnoughVals = 0;
		double valueTempDouble;
		double sharesTempDouble;
		String newline;
		Cusip cusip;
		
		for (String line : bestMatch) {
			cusipString = "";
			valueTemp = "";
			sharesTemp = "";
			valueTempDouble = 0;
			sharesTempDouble = 0;

			line.replaceAll("-", "");
			newline = line.replaceAll(",", "");
			if (newline.split(" ").length == 1)
				newline = line.replaceAll(",", " ");
			
			tempToken = new StringTokenizer(newline);

			
			if(matchType.contains("Mashed")){
				String nextWord = tempToken.nextToken();
				int cusipLength = matchType.contains("Nine")? 9: 8;
				cusipString = nextWord.substring(0,cusipLength);
				
				if(nextWord.length() == cusipLength)
					valueTemp = nextWord.substring(cusipLength);
				else
					valueTemp = tempToken.nextToken();
			}else
				cusipString = tempToken.nextToken();

			// Case where 6-2-1
			if (matchType.contains("Six Two One"))
				cusipString = cusipString + tempToken.nextToken() + tempToken.nextToken();
			
			
//			Lib.assertTrue(cusipString.length() == 9);
			try{
				cusip = new Cusip(cusipString);
			}catch(Exception e){
				//TODO CHECK ratio OF acceptable cusip's to matched cusips
				continue;
			}
			
			if (!matchType.contains("No Value") && !matchType.contains("Mashed"))
				valueTemp = tempToken.nextToken();
			else if(matchType.contains("No Value"))
				valueTemp = "0";
			
				

			if (matchType.contains("Name Cusip Switched")) {
				while (!stringIsNumber(valueTemp))
					valueTemp = tempToken.nextToken();
			}

			if (matchType.contains("Name Ticker Cusip Switched")) {
				while (!stringIsNumber(valueTemp))
					valueTemp = tempToken.nextToken();
			}
						
			if (tempToken.hasMoreTokens()) {
				sharesTemp = tempToken.nextToken();
			} else if (bestMatch.indexOf(line) > 1 && (1.0 * numNotEnoughVals)/ (1.0 * bestMatch.indexOf(line)) < .25) { 
				// Just a Freak incident
				numNotEnoughVals += numNotEnoughVals;
				continue;
			} else {
				// get an ERROR
				System.out.println(bestMatch);
				System.out.println("MatchType: " + matchType + " Line: " 
						+ line + " cusip:" + cusip + " value: " + valueTemp + " NewLine: " + newline);
//				Runtime.getRuntime().exec("open "+ f13F.getCanonicalPath());
				sharesTemp = tempToken.nextToken();
			}

			if (matchType.contains("Sole Shares Switched")) { // This means that "Sole" came before the number of shares owned
				sharesTemp = tempToken.nextToken();
				while (!stringIsNumber(sharesTemp))
					sharesTemp = tempToken.nextToken();
			} else if (tempToken.hasMoreTokens()) { // !matchType.contains("Sole Shares Switched")
				String temp = tempToken.nextToken();
				if(stringIsNumber(temp)){
					valueTemp += sharesTemp;
					sharesTemp = temp;
				}
			}

			if (!stringIsNumber(removeLettersFromEnd(sharesTemp)) || !stringIsNumber(valueTemp)) { //error output
				System.err.println("Shares or Value dont have a number");
				System.err.println(cusip);
				System.err.println(line);
				System.err.println(matchType);
//				Runtime.getRuntime().exec("open "+f13F.getCanonicalPath());
			}
			
			valueTempDouble += new Double(removeLettersFromEnd(valueTemp));

			try{
			sharesTempDouble += new Double(removeLettersFromEnd(sharesTemp));
			}catch(Exception e ){
				if(e.toString().contains("multiple")){
					System.out.println(removeLettersFromEnd(sharesTemp).split("\\.")[0]);
					sharesTemp = removeLettersFromEnd(sharesTemp).split("\\.")[0]+removeLettersFromEnd(sharesTemp).split("\\.")[1].substring(0, 2);
					
					sharesTempDouble += new Double(sharesTemp);
				}else{
					e.printStackTrace();
					//System.exit(1);
				}
			}
			if (matchType.contains("Additional Value Shares on Separate Lines")) {
				String[] newLineSplit = newline.split("\n");
				StringTokenizer temp;
				for(int indx = 1; indx< newLineSplit.length; indx++ ){
					temp = new StringTokenizer(newLineSplit[indx]);
					valueTempDouble += new Double(removeLettersFromEnd(temp.nextToken()));
					sharesTempDouble += new Double(removeLettersFromEnd(temp.nextToken()));
				}
			}

			
			if(sharesValueSwitched)
				fund13F.addHoldings(cusip, new Holding(sharesTempDouble, valueTempDouble));
			else
				fund13F.addHoldings(cusip, new Holding(valueTempDouble, sharesTempDouble));
		}

		if (fund13F.getHoldings().keySet().size() == 0) {
			if (bestMatch.size() != 0) {
				System.err.println("There are no holdings for: "
						+ f13F.getPath());
				System.err.println("Moving File");
				System.err.println(bestMatch.size() + " "
						+ bestMatch.toString());
				System.err.println(matchType);
			}
		}
	}
	
	public static String removeLettersFromEnd(String s){
		char[] x = s.toCharArray();
		int indx = 0;
		while(indx < x.length && Character.isLetter( x[x.length-indx-1]) ){
			indx++;			
		}
				
		if(indx == x.length)
			return s;
		return s.substring(0, s.length()-indx);
	}
	
	
	private boolean stringIsNumber(String in) {
		try {
			Double.parseDouble(in);
		} catch (NumberFormatException ex) {
			return false;
		}
		return true;
	}
	

	
	
	
	/**************MATCHING**************/
		
	public ArrayList<String> getListMatches(String wholeFile) {
		ArrayList<String> bestMatchTemp;	
		double improvementRatio = 1.1;// for matches that are easy to match a lot.
		
		ArrayList<String> bestMatch = getListMatchesDefault(wholeFile);
		matchType = "Default ";
		//Since Default covers most 13F filings check if it is within 1% of the number reported
		//If it is then default is likely as good as it gets. 
		//Otherwise run through all the other possibilities

		if(numClaimedHoldings > 0 && (bestMatch.size() + numClaimedHoldings*.01 >= numClaimedHoldings))
			return bestMatch;
				
		bestMatchTemp = getListMatchesDefaultAdditionalValueShares(wholeFile);
		//System.out.println(getNumLinesMatched(bestMatchTemp));
		if (bestMatchTemp != null && getNumLinesMatched(bestMatchTemp) > bestMatch.size()) {
			bestMatch = bestMatchTemp;
			matchType = "Default with Additional Value Shares on Separate Lines ";
		}
		
		//TODO if there is shares price market
		//TODO getNumLinesMatched(bestMatch) should return bestMatch.size if its not Additional Value...
//		if(){
		bestMatchTemp = getListMatchesSharesPriceMarket(wholeFile);
	//	System.out.println(bestMatchTemp.size());

			if (bestMatchTemp != null && bestMatchTemp.size() > getNumLinesMatched(bestMatch)) {
				bestMatch = bestMatchTemp;
				matchType = "Price Added ";
			}
//		}
			
		bestMatchTemp = getListMatchesSoleShareSwitched(wholeFile);
		if (bestMatchTemp != null && bestMatchTemp.size() > getNumLinesMatched(bestMatch)) {
			bestMatch = bestMatchTemp;
			matchType = "Sole Shares Switched ";
		}
		
		bestMatchTemp = getListMatchesSHSoleShareSwitched(wholeFile);
		if (bestMatchTemp != null && bestMatchTemp.size() > getNumLinesMatched(bestMatch)) {
			bestMatch = bestMatchTemp;
			matchType = "SH Sole Shares Switched ";
		}

		bestMatchTemp = getListMatchesSixTwoOne(wholeFile);
		if (bestMatchTemp != null && bestMatchTemp.size() > getNumLinesMatched(bestMatch)) {
			bestMatch = bestMatchTemp;
			matchType = "Six Two One ";
		}
		
		bestMatchTemp = getListMatchesSixTwoOneAdditionalValueShares(wholeFile);
		if (bestMatchTemp != null && getNumLinesMatched(bestMatchTemp) > getNumLinesMatched(bestMatch)) {
			bestMatch = bestMatchTemp;
			matchType = "Six Two One with Additional Value Shares on Separate Lines ";
		}

		bestMatchTemp = getListMatchesNameCusipSwitched(wholeFile);
		if (bestMatchTemp != null && bestMatchTemp.size() > getNumLinesMatched(bestMatch)*improvementRatio) {
			bestMatch = bestMatchTemp;
			matchType = "Name Cusip Switched ";
		}

		bestMatchTemp = getListMatchesNameTickerCusipSwitched(wholeFile);
		if (bestMatchTemp != null && bestMatchTemp.size() > getNumLinesMatched(bestMatch)*improvementRatio) {
			bestMatch = bestMatchTemp;
			matchType = "Name Ticker Cusip Switched ";
		}
		
		bestMatchTemp = getListMatchesNoValue(wholeFile);
		if ( bestMatchTemp != null && bestMatchTemp.size() > getNumLinesMatched(bestMatch)) {
			if(bestMatch == null || bestMatch.size() < 10){
				bestMatch = bestMatchTemp;
				matchType = "No Value ";
			}	
		}
		
		bestMatchTemp = getListMatchesMashedNine(wholeFile);
		if (bestMatchTemp != null && bestMatchTemp.size() > getNumLinesMatched(bestMatch)) {
			bestMatch = bestMatchTemp;
			matchType = "Default with Mashed Nine Digit Cusip ";
		}
		
		bestMatchTemp = getListMatchesMashedEight(wholeFile);
		//Want a ten percent increase ove just nine digit
		if (bestMatchTemp != null && bestMatchTemp.size() > getNumLinesMatched(bestMatch)*improvementRatio) {
			bestMatch = bestMatchTemp;
			matchType = "Default with Mashed Eight Digit Cusip ";
		}
		
		//FOR ONE where cusip and value are meshed call a new one with cusip of length 8 and 9 
		//then return the largest of the two
		return bestMatch;
	}
	//Matching problems:
	// 0001055980-10-000001.txt 5 4 cusip split need to combine with default????
	// numbers around cusip, use matching in addHoldingsFromMatches
	
	
	//filings/13Fs/2010/QTR1/data/0001104617-10-000002.txt split only 3 columns at a time!
	//filings/13Fs/2010/QTR1/data/0001140361-10-006694.txt has cusips and in other's instance has tickers in cusip col
	//filings/13Fs/2010/QTR1/data/0001144969-10-000006.txt value and shares mashed together so find instance of it where not 
	//mashed then get get substring length then do match by substring For each line with a valid cusip
	
	
//	private static final String NOT_LETTER = "[^A-Za-z]";
//	private static final String NOT_DIGIT = "[^0-9]";
//	private static final String NOT_DIGIT_OR_LETTER = "[^A-Za-z0-9]";
	private static final String LETTERS_AND_DIGITS = "[0-9A-Za-z]";
	private static final String LETTERS = "[A-Za-z]";
	private static final String NAME = "[A-Za-z. ]+"; //. is needed for INC.
	private static final String SPACE = "[|\t\n$,= ]+"; //space with newline
	private static final String SPACE_STAR = "[|\t\n$,= ]*";
	private static final String SPACE_WON = "[|\t$,= ]+"; //space without newline
	private static final String NUMBER = "[0-9,.]+";
	private static final String SOMETHING_TIL_NEWLINE = "[^\n]+";
	private static final String NEWLINE = "\n";
	
	//I am allowing the 7,8th characters to be alphanumeric to include non-equities to match number claimed and number found
	private static final String CUSIP = LETTERS_AND_DIGITS + "\\d{2}"+LETTERS_AND_DIGITS + "{3}" +LETTERS_AND_DIGITS+"{1,2}"+ "\\d{0,1}"; 
	private static final String CUSIP_NINE = LETTERS_AND_DIGITS + "\\d{2}"+LETTERS_AND_DIGITS + "{3}" +LETTERS_AND_DIGITS+"{2}"+ "\\d{1}"; 
	private static final String CUSIP_EIGHT = LETTERS_AND_DIGITS + "\\d{2}"+LETTERS_AND_DIGITS + "{3}" +LETTERS_AND_DIGITS+"{2}"+ "\\d{0}";
	private static final String MANY_LETTERS = LETTERS +"+";

	private String sixTwoOneLettersAndDigits = "\\d{3}"+LETTERS_AND_DIGITS + "{3}"+" "+LETTERS_AND_DIGITS+"{2}"+ " " + "\\d{1}";
	
	public ArrayList<String> getListMatchesDefault(String wholeFile){
		 Pattern cusipPattern = Pattern.compile(CUSIP+SPACE+NUMBER+SPACE+NUMBER);
		 Matcher pm = cusipPattern.matcher(wholeFile);
		 return getArrayListOfMatches(pm);
	}
	
	public ArrayList<String> getListMatchesMashedNine(String wholeFile){
		//Space_star because there are some where the cusip and value is merged;
		Pattern cusipPattern = Pattern.compile(CUSIP_NINE+SPACE_STAR+NUMBER+SPACE+NUMBER);
		Matcher pm = cusipPattern.matcher(wholeFile);		
		return getArrayListOfMatches(pm);
	} 
	
	public ArrayList<String> getListMatchesMashedEight(String wholeFile){
		//Space_star because there are some where the cusip and value is merged;
		//if mashed it has to be a number greater than length of one
		Pattern cusipPattern = Pattern.compile(CUSIP_EIGHT+SPACE_STAR+NUMBER+SPACE+NUMBER);
		Pattern cusipPatternTest = Pattern.compile(CUSIP_EIGHT+SPACE_STAR+NUMBER);
		return getMatches(wholeFile, cusipPattern, cusipPatternTest);		
	} 
	
	public ArrayList<String> getListMatchesSharesPriceMarket(String wholeFile){
		Pattern cusipPattern = Pattern.compile(CUSIP+ SPACE+NUMBER+SPACE+NUMBER+SPACE+NUMBER); 
		 Matcher pm = cusipPattern.matcher(wholeFile);
		 return getArrayListOfMatches(pm);
	}
	
	public ArrayList<String> getListMatchesNoValue(String wholeFile){
		 Pattern cusipPattern = Pattern.compile(CUSIP+SPACE+NUMBER);
		 Matcher pm = cusipPattern.matcher(wholeFile);
		 return getArrayListOfMatches(pm);
	}
	
	public ArrayList<String> getListMatchesSoleShareSwitched(String wholeFile){
		Pattern cusipPattern= Pattern.compile(CUSIP+ SPACE+NUMBER+SPACE+MANY_LETTERS+SPACE+NUMBER);
		 Matcher pm = cusipPattern.matcher(wholeFile);
		 return getArrayListOfMatches(pm);
	}
	
	public ArrayList<String> getListMatchesSHSoleShareSwitched(String wholeFile){
		Pattern cusipPattern= Pattern.compile(CUSIP+ SPACE+NUMBER+SPACE+MANY_LETTERS+SPACE+MANY_LETTERS+SPACE+NUMBER);
		 Matcher pm = cusipPattern.matcher(wholeFile);
		 return getArrayListOfMatches(pm);
	}

	public ArrayList<String> getListMatchesSixTwoOne(String wholeFile){
		Pattern cusipPattern= Pattern.compile(sixTwoOneLettersAndDigits+ SPACE+NUMBER+SPACE+NUMBER);
		 Matcher pm = cusipPattern.matcher(wholeFile);
		 return getArrayListOfMatches(pm);
	}
	
	public ArrayList<String> getListMatchesNameCusipSwitched(String wholeFile){
		Pattern cusipPattern= Pattern.compile(CUSIP+SPACE+NAME+SPACE+NUMBER+SPACE+NUMBER);
		 Matcher pm = cusipPattern.matcher(wholeFile);
		 return getArrayListOfMatches(pm);
	}
	
	public ArrayList<String> getListMatchesNameTickerCusipSwitched(String wholeFile){
		Pattern cusipPattern= Pattern.compile(CUSIP+SPACE+NAME+SPACE+NAME+SPACE+NUMBER+SPACE+NUMBER);
		 Matcher pm = cusipPattern.matcher(wholeFile);
		 return getArrayListOfMatches(pm);
	}
	
	public ArrayList<String> getListMatchesDefaultAdditionalValueShares(String wholeFile){
		Pattern cusipPattern= Pattern.compile(CUSIP+ "(" + SPACE_WON + NUMBER + SPACE_WON + NUMBER + SOMETHING_TIL_NEWLINE+NEWLINE+")+" );
		 Matcher pm = cusipPattern.matcher(wholeFile);
		 return getArrayListOfMatches(pm);
	}
	
	public ArrayList<String> getListMatchesSixTwoOneAdditionalValueShares(String wholeFile){
		Pattern cusipPattern= Pattern.compile(sixTwoOneLettersAndDigits+ "(" + SPACE_WON + NUMBER + SPACE_WON + NUMBER + SOMETHING_TIL_NEWLINE+NEWLINE+")+");
		 Matcher pm = cusipPattern.matcher(wholeFile);
		 return getArrayListOfMatches(pm);
	}
	
	public ArrayList<String> getArrayListOfMatches(Matcher m){
		ArrayList<String> matchInsts = new ArrayList<String>();
		m.reset();
		while(m.find()){
			matchInsts.add(m.group());
		}
		return matchInsts;
	}
	
	
	private ArrayList<String> getMatches(String wholeFile, Pattern cusipPattern, Pattern cusipPatternTest){
		Matcher pm = cusipPattern.matcher(wholeFile);
		if(!pm.find()) //if nothing is found
			return new ArrayList<String>();
		
		Matcher pmTest = cusipPatternTest.matcher(wholeFile);
		
		ArrayList<String> withNumbers = getArrayListOfMatches(pm);
		ArrayList<String> withoutNumbers = getArrayListOfMatches(pmTest);

		if (withNumbers.size() > withoutNumbers.size()) //verify that there are more cusips with numbers found than without
			return withNumbers;
		else
			return new ArrayList<String>();
	}
	
}
	

