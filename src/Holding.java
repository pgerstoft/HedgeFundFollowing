
public class Holding implements Comparable<Holding>{

	private double shares;
	private double value;
	
	public Holding(double s, double v){
		shares = s;
		value = v;
	}
	
	public double getShares(){
		return shares;
	}
	
	public double getValue(){
		return value;
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
		return "Shares: " + shares + " Value: "+ value;
	}
	
	
}
