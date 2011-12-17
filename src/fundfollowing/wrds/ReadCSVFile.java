package fundfollowing.wrds;

public abstract class ReadCSVFile {

	//functions useful for when reading csv files
	
	public static int getColNum(String header, String var){
		int wordCount = 0;
		for(String word : header.split(",")){
			if(word.equalsIgnoreCase(var))
				break;
			wordCount++;
		}
		return wordCount;
	}
	
	public static String getValue(String line, int colNumTicker) {
		String[] lineSplit = line.split(",");
		if(lineSplit.length > colNumTicker)
			return lineSplit[colNumTicker];
		else
			return null;
		
	}
	
}
