import java.io.File;
import java.util.Calendar;


public abstract class Data {
	
	public static Quarter getMostRecentFinishedQuarter() {
		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH);
		return new Quarter(year, month).getPreviousQuarter();
	}
	
	protected static void createFolders(String dir) {
		File sec13FFileDir = new File(dir);
		if (!sec13FFileDir.exists())
			sec13FFileDir.mkdirs();
	}
	
}
