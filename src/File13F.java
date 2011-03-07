import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Set;
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

//		//Berkshire Hathaway case:
//		//One line is standard
//		//next lines until the next holding do not have Company or CUSIP
//		type2 = false;
//		[start2 finish2] = regexp(str, '\n[\t\$ ]+[0-9,.]{1,8}[\t ]+[0-9,.]+ |');
//
//			type2_ind=1;
//			while type2_ind < length(start2) && start2(type2_ind) < start(1)
//			    type2_ind = type2_ind+1;
//			end
//
//			if type2_ind == length(start2) && start2(type2_ind) < start(1) 
//			    type2 = false;
//			end
		
		ArrayList<String> bestMatch = getListMatches(wholeFile);
		addHoldingsFromMatches(bestMatch);
//	    return fund13F.getHoldings().keySet();
	}
	
	
	public static boolean isCusipValid(String cusip)
	{
	    int d, sum, multiply, i;

	    if (cusip.length() != 9 || !Character.isDigit(cusip.charAt(8)))
	        return false;

	    for (sum = 0, multiply = 1, i = 7; i > -1; --i) {
	        if (i < 3) {
	            if (Character.isDigit(cusip.charAt(i)))
	                d = cusip.charAt(i) - '0';
	            else
	                return false;
	        } else {
	            if (Character.isUpperCase(cusip.charAt(i)))
	                d = cusip.charAt(i) - 'A' + 10;
	            else if (Character.isDigit(cusip.charAt(i)))
	                d = cusip.charAt(i) - '0';
	            else
	                return false;
	        }

	        if((i+1)%2 == 0)
	            d *= 2;

	        sum += (d / 10) + (d % 10); 
	    }

	    sum %= 10;
	    sum = 10 - sum;
	    sum %= 10;

	    if (sum != cusip.charAt(8) - '0')
	        return false;

	    return true;
	}
	
	private void addHoldingsFromMatches(ArrayList<String> bestMatch) {
		StringTokenizer tempToken;
		String cusipString;
		String valueTemp;
		String sharesTemp;
		
		for(String line: bestMatch){
			
			if(line.replaceAll(",","").split(" ").length == 1)
				line = line.replaceAll(",", " ");
			else
				line = line.replaceAll(",","");
			
		   	tempToken = new StringTokenizer(line);
		   	
		    cusipString = tempToken.nextToken();

		    if (matchType.contains("Eight Digit")) 
		    	cusipString = cusipString +  getCusipDigit(cusipString);
		    
		    //Case where 5-2-1
		    if ( cusipString.length() < 9 )
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
		    
		    if(matchType.contains("Name Cusip Switched"))
			    valueTemp = tempToken.nextToken();
		    
		    if(matchType.contains("Name Ticker Cusip Switched")){
		    	valueTemp = tempToken.nextToken();
		    	valueTemp = tempToken.nextToken();
		    }
		    	
//		    System.out.println(matchType+ " " +line+cusipString);
		    sharesTemp = tempToken.nextToken();
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
	    
//		    
//		    %if the file follows a Berkshire Hathaway format and
//		    %if there is a value of start2 between start(ii) and start(ii+1)
//		    while type2 && type2_ind<=length(start2) && start2(type2_ind)>start(ii) && (ii==length(start) || start2(type2_ind)<start(ii+1)) 
//		        %Add the Value and Shares
//		        temp = str(start2(type2_ind):finish2(type2_ind));
//		        temp = strrep(temp, '"', '');
//		        temp = strrep(temp, ',', '');
//		        temp = strrep(temp, '$', '');
//		        [t temp] = strtok(temp);
//		        value_temp = t;
//		        [t temp] = strtok(temp);
//		        shares_temp = t;
//		        %get rid of commas and convert to numbers
//		        value_temp  = strrep(value_temp ,',', '');
//		        Value(ii) = Value(ii)+str2double(value_temp );
//		        shares_temp = strrep(shares_temp,',', '');
//		        Shares(ii) = Shares(ii)+str2double(shares_temp);
//		        if isnan(Value(ii)) || isnan(Shares(ii))
//		            str(start2(type2_ind):finish2(type2_ind))
//		            file
//		            erroorrr
//		        end
//		        type2_ind = type2_ind+1;    
//		    end
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
	
	
	public boolean hasNumber(String s){
		for(char c: s.toCharArray()){
			if(Character.isDigit(c))
				return true;
		}
		return false;
	}
	
	//Determines the CUSIP check digit, algorithm from Wikipedia
	public char  getCusipDigit(String cusip){
	  int sum = 0, v =0, p, result;
	  char c;
	   if(cusip.length() != 8){
	       System.err.println("Error input for checkCusipDigit not correct length (8), Length:" + cusip.length()+ " " + cusip);
	       System.exit(1);
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
	
	public ArrayList<String> getListMatchesDefault(String wholeFile){
		 Pattern cusipPattern = Pattern.compile("[^A-Za-z][0-9A-Za-z]{9}[\t\n$=, ]+[0-9,.]+[\t\n\\$=, ]+[0-9,.]+"); 
	     Pattern cusipPatternTest = Pattern.compile("[^A-Za-z][A-Za-z]{9}[\t\n$=, ]+[0-9,.]+[\t\n\\$=, ]+[0-9,.]+");
	     return getMatches(wholeFile, cusipPattern, cusipPatternTest);
	}
	
	public ArrayList<String> getListMatchesNoValue(String wholeFile){
		 Pattern cusipPattern = Pattern.compile("[0-9A-Za-z]{9}[\t\n$=, ]+[0-9,.]+"); 
	     Pattern cusipPatternTest = Pattern.compile("[A-Za-z]{9}[\t\n$=, ]+[0-9,.]+");
	     return getMatches(wholeFile, cusipPattern, cusipPatternTest);
	}
	
	
	public ArrayList<String> getListMatchesSoleShareSwitched(String wholeFile){
		Pattern cusipPattern= Pattern.compile("[0-9A-Za-z]{9}[\t\n\\$=, ]+[0-9,.]+[\t\n\\$=, ]+[a-zA-Z]+[\t\n\\$=, ]+[0-9,.]+");
		Pattern cusipPatternTest = Pattern.compile("[A-Za-z]{9}[\t\n\\$=, ]+[0-9,.]+[\t\n\\$=, ]+[a-zA-Z]+[\t\n\\$=, ]+[0-9,.]+");
		return getMatches(wholeFile, cusipPattern, cusipPatternTest);
	}
	
	public ArrayList<String> getListMatchesSoleShareSwitchedEight(String wholeFile){
		Pattern cusipPattern= Pattern.compile("[0-9A-Za-z]{8}[\t\n\\$=, ]+[0-9,.]+[\t\n\\$=, ]+[a-zA-Z]+[\t\n\\$=, ]+[0-9,.]+");
		Pattern cusipPatternTest = Pattern.compile("[A-Za-z]{8}[\t\n\\$=, ]+[0-9,.]+[\t\n\\$=, ]+[a-zA-Z]+[\t\n\\$=, ]+[0-9,.]+");
		return getMatches(wholeFile, cusipPattern, cusipPatternTest);
	}

	public ArrayList<String> getListMatchesSixTwoOne(String wholeFile){
		Pattern cusipPattern= Pattern.compile("[0-9A-Za-z]{6} [0-9A-Za-z]{2} [0-9A-Za-z]{1}[\t\n\\$=, ]+[0-9,.]+[\t\n\\$=, ]+[0-9,.]+");
		Pattern cusipPatternTest = Pattern.compile("[A-Za-z]{6} [A-Za-z]{2} [A-Za-z]{1}[\t\n\\$=, ]+[0-9,.]+[\t\n\\$=, ]+[0-9,.]+");
		return getMatches(wholeFile, cusipPattern, cusipPatternTest);
	}
	
	public ArrayList<String> getListMatchesEightDigit(String wholeFile){
		Pattern cusipPattern= Pattern.compile("[0-9A-Za-z]{8}[\t\n\\$=, ]+[0-9,.]+[\t\n\\$=, ]+[0-9,.]+");
		Pattern cusipPatternTest = Pattern.compile("[A-Za-z]{8}[\t\n\\$=, ]+[0-9,.]+[\t\n\\$=, ]+[0-9,.]+");
		return getMatches(wholeFile, cusipPattern, cusipPatternTest);
	}
	
	public ArrayList<String> getListMatchesNameCusipSwitched(String wholeFile){
		Pattern cusipPattern= Pattern.compile("[0-9A-Za-z]{9}[\t\n\\$=, ]+[A-Za-z0-9]+[\t\n\\$=, ]+[0-9,.]+[\t\n\\$=, ]+[0-9,.]+");
		Pattern cusipPatternTest = Pattern.compile("[A-Za-z]{9}[\t\n\\$=, ]+[A-Za-z0-9]+[\t\n\\$=, ]+[0-9,.]+[\t\n\\$=, ]+[0-9,.]+");
		return getMatches(wholeFile, cusipPattern, cusipPatternTest);
	
	}
	
	public ArrayList<String> getListMatchesNameCusipSwitchedEight(String wholeFile){
		Pattern cusipPattern= Pattern.compile("[0-9A-Za-z]{8}[\t\n\\$=, ]+[A-Za-z0-9]+[\t\n\\$=, ]+[0-9,.]+[\t\n\\$=, ]+[0-9,.]+");
		Pattern cusipPatternTest = Pattern.compile("[A-Za-z]{8}[\t\n\\$=, ]+[A-Za-z0-9]+[\t\n\\$=, ]+[0-9,.]+[\t\n\\$=, ]+[0-9,.]+");
		return getMatches(wholeFile, cusipPattern, cusipPatternTest);
	
	}
	
	public ArrayList<String> getListMatchesNameTickerCusipSwitched(String wholeFile){
		Pattern cusipPattern= Pattern.compile("[0-9A-Za-z]{9}[\t\n\\$=, ]+[A-Za-z]+[\t\n\\$=, ]+[A-Za-z0-9]+[\t\n\\$=, ]+[0-9,.]+[\t\n\\$=, ]+[0-9,.]+");
		Pattern cusipPatternTest = Pattern.compile("[A-Za-z]{9}[\t\n\\$=, ]+[A-Za-z]+[\t\n\\$=, ]+[A-Za-z0-9]+[\t\n\\$=, ]+[0-9,.]+[\t\n\\$=, ]+[0-9,.]+");
		return getMatches(wholeFile, cusipPattern, cusipPatternTest);
	}
	
	public ArrayList<String> getListMatchesNameTickerCusipSwitchedEight(String wholeFile){
		Pattern cusipPattern= Pattern.compile("[0-9A-Za-z]{8}[\t\n\\$=, ]+[A-Za-z]+[\t\n\\$=, ]+[A-Za-z0-9]+[\t\n\\$=, ]+[0-9,.]+[\t\n\\$=, ]+[0-9,.]+");
		Pattern cusipPatternTest = Pattern.compile("[A-Za-z]{8}[\t\n\\$=, ]+[A-Za-z]+[\t\n\\$=, ]+[A-Za-z0-9]+[\t\n\\$=, ]+[0-9,.]+[\t\n\\$=, ]+[0-9,.]+");
		return getMatches(wholeFile, cusipPattern, cusipPatternTest);
	}
	
	
	public ArrayList<String> getListMatches(String wholeFile) {

		ArrayList<String> bestMatch = getListMatchesDefault(wholeFile);
		matchType = "Default";

//		System.out.println(bestMatch.size()+ " " + bestMatch);
		
		ArrayList<String> bestMatchTemp = getListMatchesSoleShareSwitched(wholeFile);
		if (bestMatchTemp != null && bestMatchTemp.size() > bestMatch.size()) {
			bestMatch = bestMatchTemp;
			matchType = "Sole Shares Switched ";
		}
		
		bestMatchTemp = getListMatchesSoleShareSwitchedEight(wholeFile);
		if (bestMatchTemp != null && bestMatchTemp.size() > bestMatch.size()) {
			bestMatch = bestMatchTemp;
			matchType = "Sole Shares Switched "+ "Eight Digit ";
		}

		bestMatchTemp = getListMatchesSixTwoOne(wholeFile);
		if (bestMatchTemp != null && bestMatchTemp.size() > bestMatch.size()) {
			bestMatch = bestMatchTemp;
			matchType = "Six Two One ";
		}

		bestMatchTemp = getListMatchesEightDigit(wholeFile);
		if (bestMatchTemp != null && bestMatchTemp.size() > bestMatch.size()) {
			bestMatch = bestMatchTemp;
			matchType = "Eight Digit ";
		}

		bestMatchTemp = getListMatchesNameCusipSwitched(wholeFile);
		if (bestMatchTemp != null && bestMatchTemp.size() > bestMatch.size()) {
			bestMatch = bestMatchTemp;
			matchType = "Name Cusip Switched ";
		}

		bestMatchTemp = getListMatchesNameCusipSwitchedEight(wholeFile);
		if (bestMatchTemp != null && bestMatchTemp.size() > bestMatch.size()) {
			bestMatch = bestMatchTemp;
			matchType = "Name Cusip Switched "+ "Eight Digit ";
		}
		
		bestMatchTemp = getListMatchesNameTickerCusipSwitched(wholeFile);
		if (bestMatchTemp != null && bestMatchTemp.size() > bestMatch.size()) {
			bestMatch = bestMatchTemp;
			matchType = "Name Ticker Cusip Switched ";
		}
		
		bestMatchTemp = getListMatchesNameTickerCusipSwitchedEight(wholeFile);
		if (bestMatchTemp != null && bestMatchTemp.size() > bestMatch.size()) {
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
	

