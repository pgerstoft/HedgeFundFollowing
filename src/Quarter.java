import java.util.Calendar;


public class Quarter implements Comparable<Quarter>{

	private int year;
	private int quarter;
	
	public Quarter(String quarterDir){
		//parse String
		String[] fields = quarterDir.split("/");
		
		Lib.assertTrue(fields.length == 2);
		Lib.assertTrue(quarterDir.endsWith("/"));
		
		year = new Integer(fields[0]);
		quarter = new Integer(fields[1].replace("QTR",""));
		Lib.assertTrue(year > 1900 && year<2100);
		Lib.assertTrue(quarter >= 1 && quarter <=4 );
	}
	
	public Quarter(int y, int m){
		Lib.assertTrue(y > 1900 && y<2100);
		Lib.assertTrue(m >= 0 && m <= 11);
		
		year = y;
		quarter = getQuarter(y, m);
	}
	
	public Quarter(int y, int q, boolean isQuarter){
		Lib.assertTrue(y > 1900 && y<2100);
		Lib.assertTrue(q >= 1 && q <=4 );
		year = y;
		quarter = q;
	}
	
	private static int getQuarter(int year, int month){
		if (month <= Calendar.MARCH)
			return 1;
		else if (month <= Calendar.JUNE)
			return 2;
		else if (month <= Calendar.SEPTEMBER)
			return 3;
		else
			return 4;
	}
	
	public Quarter getPreviousQuarter(){
		int newQuarter = quarter;
		int newYear = year;
		if(newQuarter == 1){
			newQuarter = 4;
			newYear = 1;
		}
		else
			newQuarter--;
		
		return new Quarter(newYear, newQuarter, true); //maybe bad practice
	}
	
	public String toString(){
		return year + "/QTR" + quarter  +"/";
	}
	
	public int compareTo(Quarter arg0) {
		return this.toString().compareTo(arg0.toString());
	}
	
	@Override
	public boolean equals(Object arg0){
		return this.toString().equals(arg0.toString());
	}
	
	@Override
	public int hashCode(){
		return toString().hashCode();
	}
	
}
