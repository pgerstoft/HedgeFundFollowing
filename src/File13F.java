import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//Process the file at opening of the file!
//TODO VERIFY THAT MARKET VALUE COMES BEFORE SHARES
//TODO VERIFY THAT PRICE DOES NOT COME BEFORE EITHER

//TODO Need to check name cusip for 3M like Stocks
public class File13F {

	private File f13F;
	private Fund fund13F;
	private String matchType;
	private int numClaimedHoldings;
	
	public File13F(String filePath){
		
		this( new File(filePath));
		
	}
	
	public File13F(File f){
		//Check input
		f13F = f;
		if(!f13F.exists()){
			System.err.println("File does not exist");
			System.exit(1);
		}
		fund13F = new Fund();
		matchType ="";
		setCompanyName();
		try {
			initFund();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private void setCompanyName(){
		String name = null;
		ArrayList<String> matchingVals;
		try {
			matchingVals = Grep.grep(f13F, null,"COMPANY CONFORMED NAME:");
			name = matchingVals.get(0).split(":")[1];
			if(name == null){
				System.err.println(f13F.getPath());
				System.exit(1);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}

		fund13F.setFundName(name.trim());
	}
	
	public String getCompanyName(){
		return fund13F.getFundName();
	}
	
	public Fund getFund(){
		return fund13F;
	}
	
	public String getMatchType(){
		return matchType;
	}
	
	//gets All the holdings and stores them in a Fund object
	private void initFund() throws IOException{

		//load file into wholeFile
		String line;
        StringBuffer buffer = new StringBuffer();
        String wholeFile;
        FileInputStream fileInputStream = new FileInputStream(f13F);
        BufferedReader reader = new BufferedReader(new InputStreamReader(fileInputStream));
        while((line = reader.readLine()) != null) {
            buffer.append(line + "\n");
        }
        reader.close();
        wholeFile = buffer.toString();
        
        
        wholeFile = wholeFile.replace( '$', ' ');
        wholeFile = wholeFile.replaceAll("-", "");
        wholeFile = wholeFile.replaceAll( "\"", " ");
        wholeFile = wholeFile.replaceAll( "=", " ");

        setNumClaimedHoldings(wholeFile);
		
		ArrayList<String> bestMatch = getListMatches(wholeFile);
		if(!matchedHoldingsCloseToClaimed(wholeFile, bestMatch.size())){
			System.err.println("This file's number of matches doesnt match number of claimed holdings: " + f13F.getPath()
					+"\nNumber of found: "+ bestMatch.size()+" Number Claimed in File:"+ numClaimedHoldings);
			System.exit(1);
		}
		addHoldingsFromMatches(bestMatch);
		
//	    return fund13F.getHoldings().keySet();
	}
	
	private void setNumClaimedHoldings(String wholeFile){
		numClaimedHoldings = getTableEntryTotal(wholeFile);
	}
	
	public int getNumClaimedHoldings(){
		return numClaimedHoldings;
	}
	
	public int getTableEntryTotal(String wholeFile){
		
		Pattern p = Pattern.compile("TABLE *ENTRY *TOTAL:+ *[0-9,]*");
		Pattern p1 = Pattern.compile("Table *Entry *Total:+ *[0-9,]*");
		Pattern p2 = Pattern.compile("table *entry *total:+ *[0-9,]*");
		Matcher m = p.matcher(wholeFile);
		Matcher m1 = p1.matcher(wholeFile);
		Matcher m2 = p2.matcher(wholeFile);

		if(m.find()){
			StringTokenizer k = new StringTokenizer(m.group().replaceAll(",",""));
			String tok;
			while(k.hasMoreTokens()){
				tok = k.nextToken();
				if(isStringNumber(tok))
					return Integer.parseInt(tok);
			}
		}else if(m1.find()){
			StringTokenizer k = new StringTokenizer(m1.group().replaceAll(",",""));
			String tok;
			while(k.hasMoreTokens()){
				tok = k.nextToken();
				
				if(isStringNumber(tok))
					return Integer.parseInt(tok);
			}
		}else if(m2.find()){
			StringTokenizer k = new StringTokenizer(m2.group().replaceAll(",",""));
			String tok;
			while(k.hasMoreTokens()){
				tok = k.nextToken();
				if(isStringNumber(tok))
					return Integer.parseInt(tok);
			}
		}
			
			
		return 0;
	}
	
	
	/* Functions to verify that number of matches is close to what is expected given the file*/
	
	public boolean matchedHoldingsCloseToClaimed(String wholeFile, int numFound){
		//return false only if numClaimedHoldings not zero OR 
		if(numClaimedHoldings == 0)
			return true;
		return (numFound-numClaimedHoldings)/(numClaimedHoldings+1) < .25 ; //within twenty-five percent
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



	public boolean isStringNumber(String in) {
		try {
			Double.parseDouble(in);
		} catch (NumberFormatException ex) {
			return false;
		}
		return true;
	}
	
	private void addHoldingsFromMatches(ArrayList<String> bestMatch) {
		StringTokenizer tempToken;
		String cusipString;
		String valueTemp;
		String sharesTemp;
		int numNotEnoughVals= 0;
		
		String newline;
				
		for(String line: bestMatch){
			
			if(line.replaceAll(",","").split(" ").length == 1)
				newline = line.replaceAll(",", " ");
			else
				newline = line.replaceAll(",","");
			
		   	tempToken = new StringTokenizer(newline);
		   	
		    cusipString = tempToken.nextToken();

		    if (matchType.contains("Eight Digit")) {
		    	//System.out.println(cusipString);
		    	if(!hasNumber(cusipString))
		    		continue;
		    	try{
		    	cusipString = cusipString +  getCusipCheckDigit(cusipString);
		    	}catch(IllegalArgumentException e){
		    		System.err.println(matchType);
		    		System.err.println(bestMatch);
		    		e.printStackTrace();
		    		System.exit(1);
		    	}
		    }
		    
		    //Case where 5-2-1
		    if (matchType.contains("Six Two One"))
		    	cusipString = cusipString + tempToken.nextToken() + tempToken.nextToken();

		    if(!isCusipValid(cusipString)){
		    	
		    	 //System.err.println("Something Wrong with cusip "+ cusipString + " "+ checkCusipDigit(cusipString.substring(0,8)) + f13F.getPath() );
		    	continue;
		    	//System.exit(1);
		    }
		    
		    if(!matchType.contains("No Value"))
		    	valueTemp = tempToken.nextToken();
		    else
		    	valueTemp = "0";
		    
		    if(matchType.contains("Name Cusip Switched")){
		    	while(!isStringNumber(valueTemp))
		    		valueTemp = tempToken.nextToken();
		    }
		    
		    if(matchType.contains("Name Ticker Cusip Switched")){
		    	System.out.println(line);
		    	valueTemp = tempToken.nextToken();
		    	valueTemp = tempToken.nextToken();
		    }
		    			    
		    if(tempToken.hasMoreTokens()){
		    	sharesTemp = tempToken.nextToken();
		    }else if(bestMatch.indexOf(line)>1 && (1.0*numNotEnoughVals)/(1.0*bestMatch.indexOf(line)) < .5  ){ //Just a Freak incident
		    	numNotEnoughVals += numNotEnoughVals;
		    	continue;
		    }
		    else{
		    	//get an ERROR
		    	System.out.println(matchType+ " " +line+ " "+ cusipString+" value: "+ valueTemp + " " +newline);
		    	sharesTemp = tempToken.nextToken();
		    }
		    	
		    if(matchType.contains("Sole Shares Switched")){ //This means that "Sole" came before the number of shares owned
		    	sharesTemp = tempToken.nextToken();
		    }
		    
		    if(!matchType.contains("Sole Shares Switched") && tempToken.hasMoreTokens()){
		    	valueTemp += sharesTemp;
		    	sharesTemp = tempToken.nextToken();
		    }
		    
		    if(!hasNumber(sharesTemp) || !hasNumber(valueTemp)){
		    	System.out.println(cusipString);
		    	System.out.println(line);
		    }
		    
		    
//		    System.out.println(sharesTemp +  " " + valueTemp+ " " + line);
		    fund13F.addHoldings(cusipString, new Holding(new Double(sharesTemp), new Double(valueTemp)));
		    
			}
		

			if(fund13F.getHoldings().keySet().size() == 0 ){
				if (bestMatch.size() != 0 ){
					System.err.println("There are no holdings for: " + f13F.getPath());
					System.err.println("Moving File");
					System.err.println(bestMatch.size()+ " " + bestMatch.toString());
					System.err.println(matchType);
				}
				//System.exit(1);
			}
			//System.out.println(fund13F.getFundValue());			
	}
	
	
	public static boolean hasNumber(String s){
		for(char c: s.toCharArray()){
			if(Character.isDigit(c))
				return true;
		}
		return false;
	}
	
	public static boolean isCusipValid(String cusip)
	{
	    if(!hasNumber(cusip))
	    	return false;
	    	    
	    if (cusip.length() != 9 || !Character.isDigit(cusip.charAt(8)))
	        return false;
	    if (getCusipCheckDigit(cusip.substring(0,8)) != cusip.charAt(8) )
	        return false;

	    return true;
	}
	
	//Determines the CUSIP check digit, algorithm from Wikipedia
	public static char getCusipCheckDigit(String cusip) throws IllegalArgumentException{
	  int sum = 0, v =0, p, result;
	  char c;
	   if(cusip.length() != 8){
	       throw new IllegalArgumentException("Error input for checkCusipDigit not correct length (8), Length:" + cusip.length()+ " " + cusip);
	   }
	   
	   for(int ii = 0; ii < cusip.length(); ii++){
		   
		  c = cusip.charAt(ii);
	      if(Character.isDigit(c))
	          v = Character.getNumericValue(c);
	      else if(Character.isLetter(c)){
	          p = c-96;
	          if( p < 0)
	              p = p+32;
	          v= p+9;
	      }else if( c == '*')
	          v = 36;
	      else if( c == '@')
	          v = 37;
	      else if( c =='#')
	          v = 38;
	      
	      if(((ii+1)%2) == 0){
	         v = v * 2;
	      }
	      sum = sum + v/10 + (v % 10);
	   }
	   
	   result = (10-(sum%10)) % 10;
	   return Character.forDigit(result, 10);
	}  
	
	public boolean matchesAreEqual(Matcher m1, Matcher m2){
		boolean matchesEqual = false; 
		m1.reset();
		m2.reset();
		
		if(m1.matches()){
			while(m1.find()){
				if(m2.find()){
					if(!m1.group().equals(m2.group()) )
						break;	
				}else
					break;
				if(m1.end() == m2.end())
					matchesEqual = true;
			}
		}
		return matchesEqual;	
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
		Matcher pmTest = cusipPatternTest.matcher(wholeFile);
		
		ArrayList<String> withNumbers = getArrayListOfMatches(pm);
		ArrayList<String> withoutNumbers = getArrayListOfMatches(pmTest);

		if (withNumbers.size() > withoutNumbers.size())
			return withNumbers;
		else
			return new ArrayList<String>();
	}
	
	private String notLetter = "[^A-Za-z]";
	private String notDigit = "[^0-9]";
	private String lettersAndDigits = "[0-9A-Za-z]";
	private String letters = "[A-Za-z]";
	private String nineLettersAndDigits = notLetter+lettersAndDigits + "{9}";
	private String nineLetters = notLetter + letters+ "{9}";
	private String eightLettersAndDigits = notLetter+ notDigit+lettersAndDigits + "{8}";
	private String eightLetters = notLetter+notDigit+letters+ "{8}";
	private String manyLetters = letters +"+";
	private String name = "[A-Za-z ]+";
	private String space = "[\t\n$=, ]+";
	private String number = "[0-9,.]+";
	private String sixTwoOneLettersAndDigits = lettersAndDigits+"{6}" +" "+lettersAndDigits+"{2}"+ " "+ lettersAndDigits+"{1} ";
	private String sixTwoOneLetters = letters+"{6}" +" "+letters+"{2}"+ " "+ letters+"{1}";
	
	private double eightDigitFactor = 1.1; //Want to use eight digit matching only if it is significantly better than 
	
	public ArrayList<String> getListMatchesDefault(String wholeFile){
		 Pattern cusipPattern = Pattern.compile(nineLettersAndDigits+ space+number+space+number); //"[^A-Za-z][0-9A-Za-z]{9}[\t\n$=, ]+[0-9,.]+[\t\n\\$=, ]+[0-9,.]+"); 
	     Pattern cusipPatternTest = Pattern.compile(nineLetters+ space+number+space+number);// "[^A-Za-z][A-Za-z]{9}[\t\n$=, ]+[0-9,.]+[\t\n\\$=, ]+[0-9,.]+");
	     return getMatches(wholeFile, cusipPattern, cusipPatternTest);
	}
	
	public ArrayList<String> getListMatchesNoValue(String wholeFile){
		 Pattern cusipPattern = Pattern.compile(nineLettersAndDigits+space+number);//"[0-9A-Za-z]{9}[\t\n$=, ]+[0-9,.]+"); 
	     Pattern cusipPatternTest = Pattern.compile(nineLetters+space+number);//"[A-Za-z]{9}[\t\n$=, ]+[0-9,.]+");
	     return getMatches(wholeFile, cusipPattern, cusipPatternTest);
	}
	
	
	public ArrayList<String> getListMatchesSoleShareSwitched(String wholeFile){
		Pattern cusipPattern= Pattern.compile(nineLettersAndDigits+ space+number+space+manyLetters+space+number);// "[0-9A-Za-z]{9}[\t\n\\$=, ]+[0-9,.]+[\t\n\\$=, ]+[a-zA-Z]+[\t\n\\$=, ]+[0-9,.]+");
		Pattern cusipPatternTest = Pattern.compile(nineLetters+ space+number+space+manyLetters+space+number);//"[A-Za-z]{9}[\t\n\\$=, ]+[0-9,.]+[\t\n\\$=, ]+[a-zA-Z]+[\t\n\\$=, ]+[0-9,.]+"
		return getMatches(wholeFile, cusipPattern, cusipPatternTest);
	}
	
	public ArrayList<String> getListMatchesSoleShareSwitchedEight(String wholeFile){
		Pattern cusipPattern= Pattern.compile(eightLettersAndDigits+space+number+space+manyLetters+space+number);//"[0-9A-Za-z]{8}[\t\n\\$=, ]+[0-9,.]+[\t\n\\$=, ]+[a-zA-Z]+[\t\n\\$=, ]+[0-9,.]+");
		Pattern cusipPatternTest = Pattern.compile(eightLetters+space+number+space+manyLetters+space+number);//"[A-Za-z]{8}[\t\n\\$=, ]+[0-9,.]+[\t\n\\$=, ]+[a-zA-Z]+[\t\n\\$=, ]+[0-9,.]+");
		return getMatches(wholeFile, cusipPattern, cusipPatternTest);
	}

	public ArrayList<String> getListMatchesSixTwoOne(String wholeFile){
		Pattern cusipPattern= Pattern.compile(sixTwoOneLettersAndDigits+ space+number+space+number);
		Pattern cusipPatternTest = Pattern.compile(sixTwoOneLetters+ space+number+space+number);//[A-Za-z]{6} [A-Za-z]{2} [A-Za-z]{1}[\t\n\\$=, ]+[0-9,.]+[\t\n\\$=, ]+[0-9,.]+
		return getMatches(wholeFile, cusipPattern, cusipPatternTest);
	}
	
	public ArrayList<String> getListMatchesEightDigit(String wholeFile){
		Pattern cusipPattern= Pattern.compile(eightLettersAndDigits+space+number+space+number);//"[^0-9][0-9A-Za-z]{8}[\t\n\\$=, ]+[0-9,.]+[\t\n\\$=, ]+[0-9,.]+");
		Pattern cusipPatternTest = Pattern.compile(eightLetters+space+number+space+number);//"[^0-9][A-Za-z]{8}[\t\n\\$=, ]+[0-9,.]+[\t\n\\$=, ]+[0-9,.]+");
		return getMatches(wholeFile, cusipPattern, cusipPatternTest);
	}
	
	public ArrayList<String> getListMatchesNameCusipSwitched(String wholeFile){
		Pattern cusipPattern= Pattern.compile(nineLettersAndDigits+space+name+space+number+space+number);//"[0-9A-Za-z]{9}[\t\n\\$=, ]+[A-Za-z]+[\t\n\\$=, ]+[0-9,.]+[\t\n\\$=, ]+[0-9,.]+");
		Pattern cusipPatternTest = Pattern.compile(nineLetters+space+name+space+number+space+number);//"[A-Za-z]{9}[\t\n\\$=, ]+[A-Za-z0]+[\t\n\\$=, ]+[0-9,.]+[\t\n\\$=, ]+[0-9,.]+");
		return getMatches(wholeFile, cusipPattern, cusipPatternTest);
	
	}
	
	public ArrayList<String> getListMatchesNameCusipSwitchedEight(String wholeFile){
		Pattern cusipPattern= Pattern.compile(eightLettersAndDigits+space+name+space+number+space+number);//"[^0-9][0-9A-Za-z]{8}[\t\n\\$=, ]+[A-Za-z]+[\t\n\\$=, ]+[0-9,.]+[\t\n\\$=, ]+[0-9,.]+");
		Pattern cusipPatternTest = Pattern.compile(eightLetters+space+name+space+number+space+number);//"[^0-9][A-Za-z]{8}[\t\n\\$=, ]+[A-Za-z]+[\t\n\\$=, ]+[0-9,.]+[\t\n\\$=, ]+[0-9,.]+");
		return getMatches(wholeFile, cusipPattern, cusipPatternTest);
	
	}
	
	public ArrayList<String> getListMatchesNameTickerCusipSwitched(String wholeFile){
		Pattern cusipPattern= Pattern.compile(nineLettersAndDigits+space+name+space+name+space+number+space+number);//"[0-9A-Za-z]{9}[\t\n\\$=, ]+[A-Za-z]+[\t\n\\$=, ]+[A-Za-z0-9]+[\t\n\\$=, ]+[0-9,.]+[\t\n\\$=, ]+[0-9,.]+");
		Pattern cusipPatternTest = Pattern.compile(nineLetters+space+name+space+name+space+number+space+number);//"[A-Za-z]{9}[\t\n\\$=, ]+[A-Za-z]+[\t\n\\$=, ]+[A-Za-z0-9]+[\t\n\\$=, ]+[0-9,.]+[\t\n\\$=, ]+[0-9,.]+");
		return getMatches(wholeFile, cusipPattern, cusipPatternTest);
	}
	
	public ArrayList<String> getListMatchesNameTickerCusipSwitchedEight(String wholeFile){
		Pattern cusipPattern= Pattern.compile(eightLettersAndDigits+space+name+space+name+space+number+space+number);//"[^0-9][0-9A-Za-z]{8}[\t\n\\$=, ]+[A-Za-z]+[\t\n\\$=, ]+[A-Za-z0-9]+[\t\n\\$=, ]+[0-9,.]+[\t\n\\$=, ]+[0-9,.]+");
		Pattern cusipPatternTest = Pattern.compile(eightLetters+space+name+space+name+space+number+space+number);//"[^0-9][A-Za-z]{8}[\t\n\\$=, ]+[A-Za-z]+[\t\n\\$=, ]+[A-Za-z0-9]+[\t\n\\$=, ]+[0-9,.]+[\t\n\\$=, ]+[0-9,.]+");
		return getMatches(wholeFile, cusipPattern, cusipPatternTest);
	}
	
	
	public ArrayList<String> getListMatches(String wholeFile) {
		
//		wholeFile.replaceAll("[A-Za-z]{8}", "");
		ArrayList<String> bestMatch = getListMatchesDefault(wholeFile);
		
		//Since Default covers most 13F filings check if it is within 1% of the number reported
		//If it is then default is likely as good as it gets. 
		//Otherwise run through all the other possibilities
		if(numClaimedHoldings != 0 && (bestMatch.size() + numClaimedHoldings*.01 >= numClaimedHoldings))
			return bestMatch;
		
//		System.out.println(bestMatch.size()+ " " + bestMatch + " " + numClaimedHoldings);
		
		ArrayList<String> bestMatchTemp = getListMatchesSoleShareSwitched(wholeFile);
		if (bestMatchTemp != null && bestMatchTemp.size() > bestMatch.size()) {
			bestMatch = bestMatchTemp;
			matchType = "Sole Shares Switched ";
		}

		bestMatchTemp = getListMatchesSoleShareSwitchedEight(wholeFile);
		if (bestMatchTemp != null && bestMatchTemp.size() > bestMatch.size()*eightDigitFactor) {

			bestMatch = bestMatchTemp;
			matchType = "Sole Shares Switched "+ "Eight Digit ";
		}

		bestMatchTemp = getListMatchesSixTwoOne(wholeFile);
		if (bestMatchTemp != null && bestMatchTemp.size() > bestMatch.size()) {
			bestMatch = bestMatchTemp;
			matchType = "Six Two One ";
		}

		bestMatchTemp = getListMatchesEightDigit(wholeFile);
		if (bestMatchTemp != null && bestMatchTemp.size() > bestMatch.size()*eightDigitFactor) {
//			System.out.println(bestMatch);
//			System.out.println(bestMatchTemp);
//			System.out.println(bestMatchTemp.size());
//			System.out.println(bestMatch.size());
			bestMatch = bestMatchTemp;
			matchType = "Eight Digit ";
		}

		bestMatchTemp = getListMatchesNameCusipSwitched(wholeFile);
		if (bestMatchTemp != null && bestMatchTemp.size() > bestMatch.size()*eightDigitFactor) {
			bestMatch = bestMatchTemp;
			matchType = "Name Cusip Switched ";
		}

		bestMatchTemp = getListMatchesNameCusipSwitchedEight(wholeFile);
		if (bestMatchTemp != null && bestMatchTemp.size() > bestMatch.size()*eightDigitFactor) {
			bestMatch = bestMatchTemp;
			matchType = "Name Cusip Switched "+ "Eight Digit ";
		}

		bestMatchTemp = getListMatchesNameTickerCusipSwitched(wholeFile);
		if (bestMatchTemp != null && bestMatchTemp.size() > bestMatch.size()*eightDigitFactor) {
			bestMatch = bestMatchTemp;
			matchType = "Name Ticker Cusip Switched ";
		}

		bestMatchTemp = getListMatchesNameTickerCusipSwitchedEight(wholeFile);
		if (bestMatchTemp != null && bestMatchTemp.size() > bestMatch.size()*eightDigitFactor) {
			bestMatch = bestMatchTemp;
			matchType = "Name Ticker Cusip Switched " + "Eight Digit ";
		}

		bestMatchTemp = getListMatchesNoValue(wholeFile);
		if ( bestMatchTemp != null && bestMatchTemp.size() > bestMatch.size()) {
			if(bestMatch == null || bestMatch.size() < 10){
				bestMatch = bestMatchTemp;
				matchType = "No Value ";
			}	
		}
		
		return bestMatch;
	}
	
	
}
	

