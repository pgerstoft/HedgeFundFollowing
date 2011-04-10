
public class Cusip{

	private String cusip;
	
	public Cusip(String c){
		if(c.length() == 7)
			c = c  + "0";
		
		if(c.length() == 8){
			try {
				c = c + getCusipCheckDigit(c);
			} catch (IllegalArgumentException e) {
				Lib.assertNotReached();
			}
		}
		
		if(cusipIsValid(c))
			cusip = c;
		else
			throw new IllegalArgumentException(); 
	}
	
	private static boolean cusipIsValid(String cusip)
	{
	    if(!hasNumber(cusip))
	    	return false;
	    if (cusip.length() != 9 || !Character.isDigit(cusip.charAt(8)))
	        return false;
	    if (getCusipCheckDigit(cusip.substring(0,8)) != cusip.charAt(8) )
	        return false;
	    return true;
	}
	
	private static boolean hasNumber(String s){
		for(char c: s.toCharArray()){
			if(Character.isDigit(c))
				return true;
		}
		return false;
	}
	
	//Determines the CUSIP check digit, algorithm from Wikipedia
	private static char getCusipCheckDigit(String cusip) throws IllegalArgumentException{
	  int sum = 0, v =0, p, result;
	  char c;
	   if(cusip.length() != 8){
	       throw new IllegalArgumentException("Error input for checkCusipDigit not correct length (8), Length:" + cusip.length()+ " Cusip: " + cusip);
	   }
	   
	   for(int ii = 0; ii < cusip.length(); ii++){
		   
		  c = cusip.charAt(ii);
	      if(Character.isDigit(c))
	          v = Character.getNumericValue(c);
	      else if(Character.isLetter(c)){
	          p = c-96;
	          if( p < 0)
	              p = p+32;
	          v= p+9;
	      }else if( c == '*')
	          v = 36;
	      else if( c == '@')
	          v = 37;
	      else if( c =='#')
	          v = 38;
	      
	      if(((ii+1)%2) == 0){
	         v = v * 2;
	      }
	      sum = sum + v/10 + (v % 10);
	   }
	   
	   result = (10-(sum%10)) % 10;
	   return Character.forDigit(result, 10);
	}  

	public String toString(){
		return cusip;
	}
	
	public int compareTo(Cusip arg0){
		return cusip.compareTo(arg0.toString());
	}
	
	public boolean equals(Cusip arg0){
		return cusip.equalsIgnoreCase(arg0.toString());
	}
}
