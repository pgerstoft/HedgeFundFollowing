package fundfollowing.wrds;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import fundfollowing.Cusip;
import fundfollowing.Date;
import fundfollowing.Lib;
import fundfollowing.Quarter;
import fundfollowing.db.DB;


public class CRSPFileRead {

	public static void parseFile(String file) {
		System.out.println("CRSPFileRead: Parsing File...");
		int colNumRET, colNumPRICE, colNumCusip, colNumDate;
		BufferedReader in = null;
		BufferedWriter out = null, out2 = null;

		try {
			in = new BufferedReader(new FileReader(file));
			out = new BufferedWriter(new FileWriter("temp/temp.csv"));
			out2 = new BufferedWriter(new FileWriter("temp/temp1.csv"));

			String line = in.readLine();

			// get column # for monthly return
			colNumRET = getColNumRET(line);
			// get column # for price
			colNumPRICE = getColNumPRC(line);
			// get column # for cusip
			colNumCusip = getColNumCusip(line);
			// get column # for date
			colNumDate = getColNumDate(line);

			String[] lineSplit;
			Date date;
			double monthlyReturn = 1.0;
			double threeMonthReturn = 0.0;
			double price = 0.0;
			Cusip newCusip = null;
			Cusip oldCusip = null;
			Quarter newQuarterTwoMonthBack = null;
			Quarter quarterOneMonthAhead = null;
			double priceAtQuarterEnd = 0.0;
			Quarter quarter = null;
			Quarter newQuarter = null;

			while ((line = in.readLine()) != null) {
				lineSplit = line.split(",");
				System.out.println(line);
				date = new Date(lineSplit[colNumDate]);
				newCusip = new Cusip(lineSplit[colNumCusip]);

				if (colNumPRICE >= lineSplit.length
						|| lineSplit[colNumPRICE].trim().isEmpty()) {
					// Stock has been removed from Market
					continue;
				} else if (lineSplit[colNumRET].equalsIgnoreCase("B"))
					continue;
				else if (lineSplit[colNumRET].equalsIgnoreCase("C"))
					monthlyReturn = 0;
				else
					monthlyReturn = new Double(lineSplit[colNumRET]);

				// Map december(12) to october(9), jan(1) to nov(10). feb to
				// dec.
				if (date.getMonth() > 2)
					newQuarterTwoMonthBack = new Quarter(date.getYear(),
							date.getMonth() - 3);
				else
					newQuarterTwoMonthBack = new Quarter(date.getYear() - 1,
							date.getMonth() + 9);

				if (oldCusip == null || !oldCusip.equals(newCusip)) {
					if (quarterOneMonthAhead != null) { // &&
														// !quarterOneMonthAhead.equals(newQuarterOneMonthAhead)
						priceAtQuarterEnd = price;
						out2.write(oldCusip + "," + price + ","
								+ threeMonthReturn + "," + quarterOneMonthAhead
								+ "\n");
					}
					oldCusip = newCusip;
					quarterOneMonthAhead = null;
					quarter = null;
				}

				newQuarter = date.toQuarter();

				if (quarter == null || !newQuarter.equals(quarter)) {
					if (quarter != null)
						priceAtQuarterEnd = price;
					else
						priceAtQuarterEnd = -1;
					quarter = newQuarter;
				}

				price = Math.abs(new Double(lineSplit[colNumPRICE]));

				if (quarterOneMonthAhead == null
						|| !quarterOneMonthAhead.equals(newQuarterTwoMonthBack)) {
					if (quarterOneMonthAhead != null)
						out2.write(oldCusip + "," + priceAtQuarterEnd + ","
								+ threeMonthReturn + "," + quarterOneMonthAhead
								+ "\n");
					quarterOneMonthAhead = newQuarterTwoMonthBack;
					threeMonthReturn = monthlyReturn;
				} else if (quarterOneMonthAhead.equals(newQuarterTwoMonthBack))
					threeMonthReturn = (threeMonthReturn + 1)
							* (monthlyReturn + 1) - 1;
				else
					Lib.assertNotReached();

				out.write(newCusip + "," + price + "," + monthlyReturn + ","
						+ date + " \n");
			}
			out.close();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
				}
			}
			if (out2 != null) {
				try {
					out2.close();
				} catch (IOException e) {
				}
			}
		}

		System.out.println("BatchLoading.....");
		DB.batchLoadStockPrice(System.getProperty("user.dir")
				+ "/temp/temp.csv");
		DB.batchLoadCusipReturn(System.getProperty("user.dir")
				+ "/temp/temp1.csv");

		new File("temp/temp.csv").delete();
		new File("temp/temp1.csv").delete();

		System.out.println("DONE");

	}

	// get column number
	private static int getColNumRET(String header) {
		return ReadCSVFile.getColNum(header, "ret");
	}

	private static int getColNumPRC(String header) {
		return ReadCSVFile.getColNum(header, "prc");
	}

	private static int getColNumCusip(String header) {
		return ReadCSVFile.getColNum(header, "cusip");
	}

	private static int getColNumDate(String header) {
		return ReadCSVFile.getColNum(header, "date");
	}

}
