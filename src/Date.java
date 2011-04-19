
public class Date implements Comparable<Date>{

	private int year;
	private int month;
	private int day;
	
	public Date(String date){
		Lib.assertTrue(date.length() == 8);

		year = new Integer(date.substring(0,4));
		month = new Integer(date.substring(4,6));
		day = new Integer(date.substring(6));
		
		Lib.assertTrue(year > 1900 && year < 2100);
		Lib.assertTrue(month >= 1 && month <= 12);
		Lib.assertTrue(day >= 1 && day <= 31);
		
	}
	
	public Date(int year, int month, int day){
		
		Lib.assertTrue(year > 1900 && year < 2100);
		Lib.assertTrue(month >= 1 && month <= 12);
		Lib.assertTrue(day >= 1 && day <= 31);
		
		this.year = year;
		this.month = month;
		this.day = day;	
	}

	public int getYear() {
		return year;
	}

	public int getMonth() {
		return month;
	}

	public int getDay() {
		return day;
	}
	
	public String toString(){
		String m =  month < 10?  "0"+month: ""+month;
		String d = day < 10? "0" + day: "" + day;
		return year+m+d;
	}
	
	public Quarter toQuarter(){
		return new Quarter(year, month - 1);
	}
	
	public int compareTo(Date arg0) {
		return this.toString().compareTo(arg0.toString());
	}
	
	public boolean equals(Object arg0){
		return this.toString().equals(arg0.toString());
	}
	
	public int hashCode(){
		return toString().hashCode();
	}
	
}
