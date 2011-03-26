import java.util.Calendar;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.TreeSet;


public class Fund {

	private Hashtable<String, Holding> holdings;
	private Hashtable<Holding, LinkedList<String>> reverseHoldings;
	private String fundName;
	private String cik;
	private String quarter;
	private double fundValue;
	
	//TODO need a date
	
	public Fund(){
		this(null, null, null);
	}
	
	public Fund(String fName, String fundCIK, String quarter){
		fundName = fName;
		this.quarter = quarter;
		cik = fundCIK;
		holdings = new Hashtable<String, Holding>();
		reverseHoldings = new Hashtable<Holding, LinkedList<String>>();
		
		fundValue = 0;
		
	}
	
	public String getFundName(){
		return fundName;
	}
	
	public double getFundValue(){
		return fundValue;
	}
	
	public void setFundName(String name){
		fundName = name;
	}
	
	public String getCIK(){
		return cik;
	}
	
	//input is 20110214, YEAR{4}Month{2}DAY{2}
	public void setQuarter(String date){
		this.quarter = formatToDirFormat(date);
	}
	
	private String formatToDirFormat(String date) {
		if (date.length() != 8) {
			System.err.println("Date is not length 8");
			System.exit(1);
		}
		String r;
		int year = new Integer(date.substring(0, 4));
		int month = new Integer(date.substring(4, 6));
		if (month < Calendar.MARCH)
			r = (year - 1) + "/QTR" + 4 + "/";
		else if (month < Calendar.JUNE)
			r = year + "/QTR" + 1 + "/";
		else if (month < Calendar.SEPTEMBER)
			r = year + "/QTR" + 2 + "/";
		else
			r = year + "/QTR" + 3 + "/";

		return r;
	}

	public String getQuarter(){
		return quarter;
	}
	
	public void setCIK(String fundCIK){
		if(fundCIK.length() != 10){
			System.err.println("Inputted CIK is not ten digits.");
			System.exit(1);
		}
		this.cik = fundCIK;
	}
	
	//return whether the fundName and cik are properly set.
	public boolean isValidFund(){
		if(cik.length() != 10 || fundName.isEmpty() || quarter.isEmpty())
			return false;
		try{
			Double.parseDouble(cik);
		}catch (Exception e) {
			return false;
		}
		
		String[] fields = quarter.split("/");
		Calendar cal = Calendar.getInstance();
		int currentYear = cal.get(Calendar.YEAR);
		if(fields.length != 2 || !fields[1].matches("QTR[1-4]")
				|| (new Double(fields[0]) < 1990 || new Double(fields[0]) > currentYear)
				|| !quarter.endsWith("/")){
			System.err.println("inputted quarter does not follow format /Year/QRT{quarter number} "+ quarter);
			System.exit(1);
		}
		
		return true;
	}	
	
	public Hashtable<String, Holding> getHoldings(){
		return holdings;
	}
	
	public void addHoldings(String cusip, Holding newHolding){
		if(!File13F.isCusipValid(cusip))
			throw new IllegalArgumentException("Cusip is not valid");
		LinkedList<String> newList = new LinkedList<String>();
		if(holdings.containsKey(cusip)){
			Holding oldHolding = holdings.get(cusip);
			
			//remove oldHolding from reverseHoldings
			LinkedList<String> oldList = new LinkedList<String>();
			oldList = reverseHoldings.get(oldHolding);
			oldList.remove(cusip);
			reverseHoldings.put(oldHolding, newList);
			
			oldHolding.addToHolding(newHolding);
			
			//add newHolding and oldHolding to reverseHoldings
			if(reverseHoldings.contains(oldHolding)){
				//add the new cusip to the the linked list
				newList = reverseHoldings.get(oldHolding);
				newList.add(cusip);
				reverseHoldings.put(oldHolding, newList);
			}else{
				newList.add(cusip);
				reverseHoldings.put(oldHolding, newList);
			}
			
			holdings.put(cusip,oldHolding);
		}else{
			holdings.put(cusip, newHolding);
			if(reverseHoldings.contains(newHolding)){
				//add the new cusip to the the linked list
				newList = reverseHoldings.get(newHolding);
				newList.add(cusip);
				reverseHoldings.put(newHolding, newList);
			}else{
				newList.add(cusip);
				reverseHoldings.put(newHolding, newList);
			}
				
		}
		
		fundValue += newHolding.getValue();
	}
	
	public String toString(){
		return topHoldings(holdings.size());
	}
	
	public String topHoldings(int n){
		//output holdings by biggest to smallest positions
		StringBuffer fundHoldings = new StringBuffer();
		TreeSet<Holding> sortedHoldings =  new TreeSet<Holding>(reverseHoldings.keySet());
		sortedHoldings = new TreeSet<Holding>(sortedHoldings.descendingSet());
		int numHoldingsAddedToBuffer = 1;
		
		for(Holding hold: sortedHoldings)
		{
			if( numHoldingsAddedToBuffer > n)
				break;
			for(String cusip: reverseHoldings.get(hold)){
				fundHoldings.append(numHoldingsAddedToBuffer + ": "+cusip + " " + hold+ "\n");
				numHoldingsAddedToBuffer++;
			}
		}
		//TODO ADD TICKER INFORMATION!!!!
		
		return fundHoldings.toString();
	}
	
}
