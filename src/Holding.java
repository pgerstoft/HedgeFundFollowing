
public class Holding implements Comparable<Holding>, java.io.Serializable{

	private double shares;
	private double value;
	
	public Holding(double v, double s){
		shares = s;
		value = v;
	}
	
	public double getShares(){
		return shares;
	}
	
	public void setShares(double newShares){
		shares = newShares;
	}
	
	public double getValue(){
		return value;
	}
	
	public void setValue(double newVal){
		value = newVal;
	}
	
	public void addToHolding(Holding newHolding){
		shares += newHolding.getShares();
		value += newHolding.getValue();
	}

	public int compareTo(Holding arg0) {			
		if( value < arg0.getValue())
			return -1;
		else if (value > arg0.getValue())
			return 1;
		return 0;
	}
	
	public String toString(){
		return " Value: "+ value + " Shares: " + shares;
	}
	
	
	
	
}
