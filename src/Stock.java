import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Stock implements java.io.Serializable{

	private String cusip;
	private String ticker;
	private double sharesOutstanding;
	
	public Stock getStock(String c){
		String tick = getTicker(c);
		if(!tick.equals("")){
			return new Stock(c, tick);
		}
		return null;
	}
	
	
	private Stock(String c, String tick){
		cusip = c;
		ticker = tick;
		sharesOutstanding = getSharesOutstanding(ticker);
	}
	
	public String getTicker(){
		return ticker;
	}
	
	public double getSharesOutstanding(){
		return sharesOutstanding;
	}
	
	private double getSharesOutstanding(String ticker){
		StringBuffer bf = new StringBuffer();
		try {
			URL url = new URL("http://www.google.com/finance?fstype=bi&q="+ ticker);
			try {
				Thread.sleep(convertSecondToMillis(.25));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					url.openStream()));
			String line;

			while ((line = reader.readLine()) != null) {
				bf.append(line + "\n");
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String wholeFile = bf.toString();
		// System.out.println(wholeFile);
		Pattern start = Pattern.compile("Shares Outstanding");
		Matcher mStart = start.matcher(wholeFile);
		mStart.find();
		Pattern end = Pattern.compile("<td class=\"r bld\">");
		Pattern endEnd = Pattern.compile("/");
		
		Matcher mEnd = end.matcher(wholeFile.substring(mStart.start()));
		mEnd.find();
		Matcher mEndEnd = endEnd.matcher(wholeFile.substring(mEnd.start()));
//		System.out.println(wholeFile.substring(mStart.start(), mStart.start()+mEnd.end()));
//		System.out.println(mEndEnd.find());
		if (mEndEnd.find()) {
//			System.out.println(wholeFile.substring(mStart.start()));
//			System.out.println(wholeFile.substring(mStart.start()+mEnd.end(),mStart.start()+ mEnd.end()+mEndEnd.end()+1));
			return new Double(wholeFile.substring(mStart.start()+mEnd.end(),mStart.start()+ mEnd.end()+mEndEnd.end()+1));
		}
		return 0;
	}
	
	private String getTicker(String cusip) {

		StringBuffer bf = new StringBuffer();
		try {
			URL url = new URL(
					"http://activequote.fidelity.com/mmnet/SymLookup.phtml?QUOTE_TYPE=&scCode=E&searchBy=C&searchFor="
							+ cusip);
			try {
				Thread.sleep(convertSecondToMillis(.25));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					url.openStream()));
			String line;

			while ((line = reader.readLine()) != null) {
				bf.append(line + "\n");
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//System.out.println(cusip);
		String wholeFile = bf.toString();
		// System.out.println(wholeFile);
		Pattern start = Pattern.compile("SID_VALUE_ID=");
		Pattern end = Pattern.compile("SID_VALUE_ID=[^>]*");
		Matcher mStart = start.matcher(wholeFile);
		Matcher mEnd = end.matcher(wholeFile);
		if (mStart.find() && mEnd.find()) {
//			System.out.println(mStart.end() + 1);
//			System.out.println(mEnd.end() - 2);
			return wholeFile.substring(mStart.end(), mEnd.end() - 1);
		}
		return "";
	}
	
	private long convertSecondToMillis(double d) {
		return (long) (1000 * d);
	}
	
}
