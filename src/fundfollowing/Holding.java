package fundfollowing;
public class Holding implements Comparable<Holding>, java.io.Serializable {

	private static final long serialVersionUID = 1L;
	private double shares;
	private double value;

	public Holding(double v, double s) {
		shares = s;
		value = v;
	}

	public double getShares() {
		return shares;
	}

	public void setShares(double newShares) {
		shares = newShares;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double newVal) {
		value = newVal;
	}

	public void addToHolding(Holding newHolding) {
		shares += newHolding.getShares();
		value += newHolding.getValue();
	}

	public int compareTo(Holding arg0) {
		if (value < arg0.getValue())
			return -1;
		else if (value > arg0.getValue())
			return 1;
		return 0;
	}

	@Override
	public String toString() {
		return "Value: " + value + " Shares: " + shares;
	}

	@Override
	public boolean equals(Object arg0) {
		if (arg0 instanceof Holding) {
			Holding x = (Holding) arg0;
			return this.toString().equalsIgnoreCase(x.toString());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

}
