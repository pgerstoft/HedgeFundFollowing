import java.util.Hashtable;
import java.util.LinkedList;
import java.util.TreeSet;


public class Fund {

	private Hashtable<String, Holding> holdings;
	private Hashtable<Holding, LinkedList<String>> reverseHoldings;
	private String fundName;
	private String cik;
	private double fundValue;
	
	//TODO need a date
	
	public Fund(){
		this(null, null);
	}
	
	public Fund(String fName, String fundCIK){
		fundName = fName;
		holdings = new Hashtable<String, Holding>();
		reverseHoldings = new Hashtable<Holding, LinkedList<String>>();
		cik = fundCIK;
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
	
	public void setCIK(String fundCIK){
		if(fundCIK.length() != 10){
			System.err.println("Inputted CIK is not ten digits.");
			System.exit(1);
		}
		this.cik = fundCIK;
	}
	
	//return whether the fundName and cik are properly set.
	public boolean isValidFund(){
		if(cik.length() != 10 || fundName.isEmpty())
			return false;
		try{
			Double.parseDouble(cik);
		}catch (Exception e) {
			return false;
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
