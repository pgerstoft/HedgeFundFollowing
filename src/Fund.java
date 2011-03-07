import java.util.Hashtable;


public class Fund {

	private Hashtable<String, Holding> holdings;
	private String fundName;
	private double fundValue;
	
	public Fund(){
		this(null);
	}
	
	public Fund(String fName){
		fundName = fName;
		holdings = new Hashtable<String, Holding>();
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
	
	public Hashtable<String, Holding> getHoldings(){
		return holdings;
	}
	
	public void addHoldings(String cusip, Holding newHolding){
		if(holdings.containsKey(cusip)){
			Holding old = holdings.get(cusip);
			old.addToHolding(newHolding);
			holdings.put(cusip,old);
		}else{
			holdings.put(cusip, newHolding);
		}
		
		fundValue += newHolding.getValue();
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
	
}
