import java.util.Calendar;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.TreeSet;


public class Fund {

	private Hashtable<Cusip, Holding> holdings;
	private Hashtable<Holding, LinkedList<Cusip>> reverseHoldings;
	private String fundName;
	private CIK cik;
	private Quarter quarter;
	private double fundValue;
		
	public Fund(){
		this(null, null, null);
	}
	
	public Fund(String fName, CIK fundCIK, Quarter quarter){
		fundName = fName;
		this.quarter = quarter;
		cik = fundCIK;
		holdings = new Hashtable<Cusip, Holding>();
		reverseHoldings = new Hashtable<Holding, LinkedList<Cusip>>();
		
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
	
	public CIK getCIK(){
		return cik;
	}
	
	public void setCIK(CIK fundCIK){
		this.cik = fundCIK;
	}
	
	public void setQuarter(Quarter q){
		quarter = q;
	}

	public Quarter getQuarter(){
		return quarter;
	}
	

	//return whether the fundName and cik are properly set.
	public boolean isValidFund(){
		return cik != null || !fundName.isEmpty() || quarter != null;

	}	
	
	public Hashtable<Cusip, Holding> getHoldings(){
		return holdings;
	}
	
	public void addHoldings(Cusip cusip, Holding newHolding){
		
		LinkedList<Cusip> newList = new LinkedList<Cusip>();
		if(holdings.containsKey(cusip)){
			Holding oldHolding = holdings.get(cusip);
			
			//remove oldHolding from reverseHoldings
			LinkedList<Cusip> oldList = new LinkedList<Cusip>();
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
			for(Cusip cusip: reverseHoldings.get(hold)){
				fundHoldings.append(numHoldingsAddedToBuffer + ": "+cusip + " " + hold+ "\n");
				numHoldingsAddedToBuffer++;
			}
		}
		//TODO ADD TICKER INFORMATION!!!!
		
		return fundHoldings.toString();
	}
	
	
	
}
