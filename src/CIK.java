
public class CIK  implements Comparable<CIK>{
	//Central Index Key used by SEC to identify individuals and companies
	private String cik;
	
	public CIK(String c){

		if(stringIsNumber(c) && c.length() == 10)
			cik = c;
		else
			throw new IllegalArgumentException(); 
	}
	
	
	private boolean stringIsNumber(String in) {
		try {
			Double.parseDouble(in);
		} catch (NumberFormatException ex) {
			return false;
		}
		return true;
	}
	
	public String toString(){
		return cik;
	}
	
	public int compareTo(CIK arg0){
		return cik.compareTo(arg0.toString());
	}
	
	
	public boolean equals(Object arg0){
		if(arg0 instanceof CIK){
			CIK x = (CIK) arg0;
			return cik.equalsIgnoreCase(x.toString());
		}
		return false;
	}

	
	public int hashCode(){
		return cik.hashCode();
	}
}
