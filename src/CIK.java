
public class CIK  implements Comparable<CIK>{
	//Central Index Key used by SEC to identify individuals and companies
	private String cik;
	
	public CIK(String c){

		if(stringIsNumber(c) && c.length() == 10)
			cik = c;
		else
			throw new IllegalArgumentException(); 
	}
	
	public String toString(){
		return cik;
	}
	
	public int compareTo(CIK arg0){
		return cik.compareTo(arg0.toString());
	}
	
	public boolean equals(CIK arg0){
		return cik.equalsIgnoreCase(arg0.toString());
	}
	
	private boolean stringIsNumber(String in) {
		try {
			Double.parseDouble(in);
		} catch (NumberFormatException ex) {
			return false;
		}
		return true;
	}
	
	public int hashCode(){
		return cik.hashCode();
	}
}
