package fundfollowing.db;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;

import fundfollowing.CIK;
import fundfollowing.Cusip;
import fundfollowing.Date;
import fundfollowing.Lib;
import fundfollowing.Quarter;


public class DB {

	// TODO CREATE INDEX cikHFH ON MYDB.HEDGEFUNDHOLDINGS (CIK);
	// TODO CREATE INDEX quarterHFH ON MYDB.HEDGEFUNDHOLDINGS (QUARTER);

	private static String dbURL = "jdbc:mysql://localhost/myDB";

	private static String cusipTicker = "CUSIPTICKER";
	private static String cusipTickerFields = "CUSIP char(9), "
			+ "TICKER char(10), " + // Ticker length is longer than 4 to
									// accommodate unusually
									// long tickers
			"QUARTER char(10), " + "UNIQUE(CUSIP,QUARTER)";

	private static String hedgeFund = "HEDGEFUND";
	private static String hedgeFundFields = "CIK char(10), "
			+ "FundName varchar(255), " + "NumHoldings int, "
			+ "FILE varchar(255), " + "QUARTER char(10), "
			+ "PRIMARY KEY(CIK, QUARTER) ";

	private static String hedgeFundHoldings = "HEDGEFUNDHOLDINGS";
	private static String hedgeFundHoldingsFields = "CUSIP char(9), "
			+ "CIK char(10), " + "VALUE float, " + "SHARES float, "
			+ "PORTIONOFFUND float, " + "QUARTER char(10), "
			+ "UNIQUE(CIK,CUSIP, QUARTER)";
	// + "FOREIGN KEY(CIK) REFERENCES HEDGEFUND(CIK)";

	private static String stockPrice = "STOCKPRICE";
	private static String stockPriceFields = "CUSIP char(9), "
			+ "PRICE float, " + "MONTHLYRET float, " + "DATE char(8), "
			+ "UNIQUE(CUSIP, DATE)";

	private static String cusipReturn = "CUSIPRETURN";
	private static String cusipReturnFields = "CUSIP char(9), "
			+ "EndQuarterPrice float, " + "RET float, " + "QUARTER char(10), "
			+ "UNIQUE(CUSIP, QUARTER)";

	private static String cikReturn = "CIKRETURN";
	private static String cikReturnFields = "CIK char(10), " + "RET float, "
			+ "QUARTER char(10), " + "UNIQUE(CIK, QUARTER)";

	private static String tempShares = "TEMP_SHARES";
	private static String tempSharesFields = "CUSIP char(9), "
			+ "CSHOQ float, " + "QUARTER char(10), " + "UNIQUE(CUSIP, QUARTER)";

	private static Hashtable<String, String> tableNameToFields = new Hashtable<String, String>();
	// Keep track of whether or not the table exists
	private static Hashtable<String, Boolean> tableExists = new Hashtable<String, Boolean>();

	static {
		tableNameToFields.put(hedgeFund, hedgeFundFields);
		tableNameToFields.put(hedgeFundHoldings, hedgeFundHoldingsFields);
		tableNameToFields.put(cusipTicker, cusipTickerFields);
		tableNameToFields.put(stockPrice, stockPriceFields);
		tableNameToFields.put(cusipReturn, cusipReturnFields);
		tableNameToFields.put(cikReturn, cikReturnFields);
		tableNameToFields.put(tempShares, tempSharesFields);

		tableExists.put(cusipTicker, false);
		tableExists.put(hedgeFund, false);
		tableExists.put(hedgeFundHoldings, false);
		tableExists.put(stockPrice, false);
		tableExists.put(cusipReturn, false);
		tableExists.put(cikReturn, false);
	}

	// jdbc Connection
	private static Connection conn = null;
	private static Statement stmt = null;

	private static final DB INSTANCE = new DB();
	private static boolean multipleQueries = false;
	private static int numQueries = 1;

	public static DB getInstance() {
		return INSTANCE;
	}

	private DB() {
		// try {
		// Class.forName("com.mysql.jdbc.Driver").newInstance();
		// DriverManager.getConnection("jdbc:mysql:;shutdown=true");
		// } catch (Exception e) {
		// }
	}

	public static void turnMultipleQueriesOn() {
		numQueries = 1;
		createConnection();
		multipleQueries = true;
	}

	public static void turnMultipleQueriesOff() {
		multipleQueries = false;
		closeConnection();
	}

	private static void createConnection() {
		if (multipleQueries && conn != null && numQueries % 100 != 0) {
			numQueries++;
			return;
		} else if (numQueries % 100 == 0) {
			turnMultipleQueriesOff();
			turnMultipleQueriesOn();
		}
		try {
			Class.forName("com.mysql.jdbc.Driver");
			// Get a connection
			conn = DriverManager.getConnection(dbURL, "root", "");
		} catch (Exception except) {
			except.printStackTrace();
			System.exit(1);
		}
	}

	private static void closeConnection() {
		if (multipleQueries)
			return;
		try {
			if (stmt != null) {
				stmt.close();
			}
			if (conn != null) {
				conn.close();
			}
		} catch (Exception e) {
		}
	}

	private static void createTable(String table) {
		try {
			stmt = conn.createStatement();

			stmt.executeUpdate("CREATE TABLE " + table + "("
					+ tableNameToFields.get(table) + ")");

		} catch (Exception e) {
			handleException(e);
		}
	}

	private static void createTempSharesTable() {
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate("CREATE TABLE TEMP_SHARES(CUSIP char(9), CSHOQ float, "
					+ "QUARTER char(10),  UNIQUE(CUSIP, QUARTER))");
		} catch (Exception e) {
			handleException(e);
		}
	}

	private static void insertValues(String table, String values) {
		try {
			createConnection();
			if (!tableExists(table))
				createTable(table);
			stmt = conn.createStatement();
			stmt.execute("INSERT IGNORE INTO " + table + " values (" + values
					+ ")");
			stmt.close();
		} catch (Exception e) {
			handleException(e);
		}
		closeConnection();
	}

	private static void handleException(Exception e) {
		closeConnection();
		e.printStackTrace();
		System.exit(1);
	}

	private static boolean tableExists(String table) {
		if (tableExists.contains(table) && tableExists.get(table))
			return true;

		try {
			DatabaseMetaData dbm = conn.getMetaData();
			ResultSet rs = dbm.getTables(null, null, table.toUpperCase(), null);
			if (!rs.next()) {
				return false;
			} else {
				tableExists.put(table, true);
				return true;
			}
		} catch (Exception e) {
			handleException(e);
		}
		return true;
	}

	public void insertTempSharesTable(Cusip cusip, double cshoq, Quarter quarter) {
		insertValues("TEMP_SHARES", "'" + cusip + "', " + cshoq + " ," + "'"
				+ quarter + "'");
	}

	public static Hashtable<Cusip, Double> getSharesOustandingAllCusips(
			Quarter quarter) {
		Hashtable<Cusip, Double> cusipToCSHOQ = new Hashtable<Cusip, Double>();
		try {
			createConnection();
			stmt = conn.createStatement();
			ResultSet results = stmt.executeQuery("select * from "
					+ "TEMP_SHARES " + "WHERE QUARTER = '" + quarter + "'");

			Cusip cusip;
			double cshoq;
			while (results.next()) {
				cusip = new Cusip(results.getString(1));
				cshoq = results.getDouble(2);
				cusipToCSHOQ.put(cusip, cshoq);
			}

			results.close();
			stmt.close();
		} catch (Exception e) {
			handleException(e);
		}
		closeConnection();
		return cusipToCSHOQ;
	}

	public static double getSharesOustanding(Cusip cusip, Quarter quarter) {
		double cshoq = 0.0;

		try {
			createConnection();
			stmt = conn.createStatement();
			ResultSet results = stmt.executeQuery("select CSHOQ from "
					+ "TEMP_SHARES WHERE QUARTER = '" + quarter + "' "
					+ "AND CUSIP = '" + cusip + "'");

			while (results.next()) {
				cshoq = results.getDouble(1);
			}

			results.close();
			stmt.close();
		} catch (Exception e) {
			handleException(e);
		}
		closeConnection();
		return cshoq;
	}

	public static void createTempCusipTable() {
		try {
			createConnection();
			stmt = conn.createStatement();
			stmt.executeUpdate("CREATE TABLE TEMP(CUSIP char(9), UNIQUE(CUSIP))");
		} catch (Exception e) {
			handleException(e);
		}
		closeConnection();
	}

	public static void insertTempCusipTable(Cusip cusip) {
		try {
			createConnection();
			stmt = conn.createStatement();
			stmt.execute("INSERT IGNORE INTO TEMP values ('" + cusip + "' )");
		} catch (Exception e) {
			handleException(e);
		}
		closeConnection();
	}

	// returns all CUSIPs from that quarter
	public static void writeTempVals() {
		try {
			createConnection();
			stmt = conn.createStatement();
			new File("tempCusips2.txt").delete();
			stmt.executeQuery("select DISTINCT CUSIP from TEMP_SHARES INTO OUTFILE '"
					+ System.getProperty("user.dir") + "/tempCusips2.txt'");
			// stmt.executeQuery("select * from TEMP INTO OUTFILE '" +
			// System.getProperty("user.dir")+ "/tempCusips2.txt'");

			// BufferedWriter out = new BufferedWriter(new FileWriter(
			// "tempCusip.txt"));
			// while (results.next())
			// out.write(results.getString(1) + "\n");
			// out.close();
		} catch (Exception e) {
			handleException(e);
		}
		closeConnection();
	}

	public static void deleteTempTable() {
		try {
			createConnection();
			if (tableExists("TEMP")) {
				stmt = conn.createStatement();
				stmt.executeUpdate("DROP TABLE TEMP");
			}
		} catch (Exception e) {
			handleException(e);
		}
		closeConnection();
	}

	public void insertHedgeFund(CIK cik, String fundName, Quarter quarter,
			String fileName) {
		fundName = fundName.replaceAll("'", "");
		fundName = fundName.replace("\\", "");
		insertValues(hedgeFund, "'" + cik + "'," + "'" + fundName + "', "
				+ "0, " + "'" + fileName + "', " + "'" + quarter + "'");
	}

	public static void updateNumHoldings(Quarter quarter) {
		try {
			createConnection();
			if (!tableExists(hedgeFund))
				return;
			stmt = conn.createStatement();
			stmt.execute("UPDATE " + hedgeFund + " HF " + "SET NUMHOLDINGS =  "
					+ "(SELECT COUNT(CUSIP) " + "FROM " + hedgeFundHoldings
					+ " HFH " + "WHERE HF.CIK = HFH.CIK AND HFH.QUARTER = '"
					+ quarter + "')" + "WHERE HF.QUARTER = '" + quarter + "'");
			stmt.close();
		} catch (Exception e) {
			handleException(e);
		}
		closeConnection();
	}

	public static void updateFundReturns(Quarter quarter) {
		try {
			createConnection();
			if (!tableExists(cikReturn))
				createTable(cikReturn);
			System.out.println("HERE");
			stmt = conn.createStatement();
			stmt.execute("INSERT IGNORE INTO "
					+ cikReturn
					+ " "
					+ "SELECT CIK, SUM(PORTIONOFFUND*RET), HFH.Quarter "
					+ "FROM "
					+ hedgeFundHoldings
					+ " HFH "
					+ "JOIN CUSIPRETURN C ON (HFH.CUSIP = C.CUSIP AND HFH.QUARTER = C.QUARTER) "
					+ "WHERE HFH.QUARTER = '" + quarter + "' " + "GROUP BY CIK");
			stmt.close();
		} catch (Exception e) {
			handleException(e);
		}
		closeConnection();
	}

	public static double getFundReturn(CIK cik, Quarter quarter) {
		double ret = -999;
		try {
			createConnection();
			stmt = conn.createStatement();

			ResultSet results = stmt.executeQuery("SELECT RET  " + "FROM "
					+ cikReturn + " " + "WHERE QUARTER = '" + quarter + "' "
					+ "AND CIK = '" + cik + "'");

			if (results.next())
				ret = results.getDouble(1);

			Lib.assertTrue(!results.next());
			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}

		return ret;
	}

	public void insertHedgeFundHoldings(Cusip cusip, CIK cik, double value,
			double shares, double portionOfFund, Quarter quarter) {

		insertValues(hedgeFundHoldings, cusip + "','" + cik + "'," + value
				+ "," + shares + "," + portionOfFund + "'" + quarter);
	}

	public void insertCusipTicker(Cusip cusip, String tick, Quarter quarter) {
		insertValues(cusipTicker, "'" + cusip + "','" + tick + "','" + quarter
				+ "'");
	}

	public static String getTickerFromCusip(Cusip cusip, Quarter quarter) {
		try {
			createConnection();
			if (!tableExists(cusipTicker))
				return null;
			stmt = conn.createStatement();
			ResultSet results = stmt.executeQuery("select Ticker from "
					+ cusipTicker + " WHERE CUSIP = '" + cusip + "'"
					+ " AND QUARTER = '" + quarter + "'");
			if (results.next())
				return results.getString(1);
			;

			closeConnection();

			return null;
		} catch (Exception e) {
			handleException(e);
		}
		return null;

	}

	public static Cusip getCusipFromTicker(String tick, Quarter quarter) {
		Cusip cusip = null;
		try {
			createConnection();
			stmt = conn.createStatement();

			ResultSet results = stmt.executeQuery("SELECT CUSIP " + "FROM "
					+ cusipTicker + " " + "WHERE TICKER = '" + tick
					+ "' AND QUARTER = '" + quarter + "'");

			if (results.next()) {
				cusip = new Cusip(results.getString(1));
			}
			Lib.assertTrue(!results.next());
			closeConnection();
		} catch (Exception e) {
			handleException(e);
		}
		return cusip;
	}

	// returns all CUSIPs from that quarter
	public static ArrayList<Cusip> getCusips(Quarter quarter) {
		ArrayList<Cusip> cusips = new ArrayList<Cusip>();
		try {

			createConnection();
			if (!tableExists(cusipTicker))
				return cusips;
			stmt = conn.createStatement();
			ResultSet results = stmt.executeQuery("select CUSIP from "
					+ cusipTicker + " WHERE QUARTER = '" + quarter + "'");
			while (results.next())
				cusips.add(new Cusip(results.getString(1)));

			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}
		return cusips;
	}

	// returns all CUSIPs from that quarter
	public static ArrayList<Cusip> getCusipsFromCusipReturn(Quarter quarter) {
		ArrayList<Cusip> cusips = new ArrayList<Cusip>();
		try {

			createConnection();
			if (!tableExists(cusipTicker))
				return cusips;
			stmt = conn.createStatement();
			ResultSet results = stmt.executeQuery("select CUSIP from "
					+ cusipReturn + " WHERE QUARTER = '" + quarter + "'");
			while (results.next())
				cusips.add(new Cusip(results.getString(1)));

			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}
		return cusips;
	}

	public static Hashtable<String, Double> getNumSharesHeldByFunds(
			Quarter quarter) {
		Hashtable<String, Double> tickerToSharesHeld = new Hashtable<String, Double>();

		try {
			createConnection();
			if (!tableExists(cusipTicker))
				return tickerToSharesHeld;

			stmt = conn.createStatement();
			ResultSet results = stmt
					.executeQuery("select C.CUSIP, SUM(SHARES) from "
							+ cusipTicker + " C, " + hedgeFundHoldings
							+ " H WHERE C.QUARTER = '" + quarter + "' AND "
							+ "C.CUSIP = H.CUSIP " + "Group By C.CUSIP");
			String tick = "";
			double summedShares = 0;
			while (results.next()) {
				tick = results.getString(1);
				summedShares = results.getDouble(2);
				tickerToSharesHeld.put(tick, summedShares);
			}
			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}

		return tickerToSharesHeld;
	}

	public static void writeToFile(Cusip cusip, Quarter quarter) {
		try {
			ArrayList<String> topRow = new ArrayList<String>();
			ArrayList<String> bottomRow = new ArrayList<String>();
			createConnection();
			stmt = conn.createStatement();
			ResultSet results = stmt.executeQuery("select CIK, SHARES from "
					+ hedgeFundHoldings + "  WHERE QUARTER = '" + quarter
					+ "' AND CUSIP = '" + cusip + "'");
			topRow.add("TICKER");
			topRow.add("CUSIP");
			bottomRow.add(getTickerFromCusip(cusip, quarter));
			bottomRow.add(cusip.toString());
			String fundName;
			Double shares;
			Double totalShares = 0.0;
			while (results.next()) {
				fundName = results.getString(1);
				shares = results.getDouble(2);
				topRow.add(fundName);
				totalShares += shares;
				bottomRow.add("" + shares);
			}
			results = stmt.executeQuery("select CSHOQ from " + "TEMP_SHARES "
					+ "WHERE QUARTER = '" + quarter + "' AND Cusip = '" + cusip
					+ "'");
			topRow.add("SUMTotal");
			bottomRow.add("" + totalShares);
			while (results.next()) {
				topRow.add("TOTAL");
				bottomRow.add("" + results.getDouble(1));
			}

			BufferedWriter out = new BufferedWriter(new FileWriter(
					"sharesCusip.csv"));
			for (String top : topRow)
				out.write(top.replaceAll(",", "") + ", ");
			out.write("\n");
			for (String bottom : bottomRow)
				out.write(bottom + ", ");
			out.write("\n");
			out.close();
			closeConnection();
		} catch (Exception e) {
			handleException(e);
		}

	}

	public void getNumberOfHedgeFundHoldingTicker(String tick,
			int minNumHoldings, int maxNumHoldings, Quarter quarter) {
		try {
			createConnection();
			stmt = conn.createStatement();

			System.out.println("SELECT CIK " + "FROM MYDB.HEDGEFUNDHOLDINGS H "
					+ "WHERE CUSIP = '" + getCusipFromTicker(tick, quarter)
					+ "' AND " + "CIK IN " + "(SELECT CIK  "
					+ "FROM MYDB.HEDGEFUNDHOLDINGS " + "GROUP BY CIK "
					+ "HAVING count(CUSIP) >=  " + minNumHoldings + " "
					+ "AND count(CUSIP) <= " + maxNumHoldings + ")");

			ResultSet results = stmt.executeQuery("SELECT CIK "
					+ "FROM HEDGEFUNDHOLDINGS H " + "WHERE CUSIP = '"
					+ getCusipFromTicker(tick, quarter) + "' AND " + "CIK IN "
					+ "(SELECT CIK  " + "FROM HEDGEFUNDHOLDINGS "
					+ "GROUP BY CIK " + "HAVING count(CUSIP) >=  "
					+ minNumHoldings + " " + "AND count(CUSIP) <= "
					+ maxNumHoldings + ")");

			while (results.next()) {

				System.out.println(results.getString(1));
			}
			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}
	}

	public static Hashtable<CIK, Double> getFundValues(Quarter quarter) {
		Hashtable<CIK, Double> cikToFundValue = new Hashtable<CIK, Double>();
		try {

			createConnection();
			stmt = conn.createStatement();

			ResultSet results = stmt.executeQuery("SELECT CIK, SUM(VALUE)  "
					+ "FROM HEDGEFUNDHOLDINGS H " + "WHERE QUARTER = '"
					+ quarter + "' " + "GROUP BY CIK");
			while (results.next()) {

				cikToFundValue.put(new CIK(results.getString(1)),
						results.getDouble(2));
			}
			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}

		return cikToFundValue;
	}

	public static ArrayList<CIK> getCIKS() {
		ArrayList<CIK> ciks = new ArrayList<CIK>();
		try {

			createConnection();
			stmt = conn.createStatement();

			ResultSet results = stmt.executeQuery("SELECT DISTINCT CIK "
					+ "FROM HEDGEFUND ");

			while (results.next()) {
				ciks.add(new CIK(results.getString(1)));
			}
			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}

		return ciks;
	}

	public static Hashtable<CIK, Double> getFundsToShares(Cusip cusip,
			Quarter quarter) {
		Hashtable<CIK, Double> cikToShares = new Hashtable<CIK, Double>();
		try {

			createConnection();
			stmt = conn.createStatement();

			ResultSet results = stmt.executeQuery("SELECT CIK, SHARES  "
					+ "FROM HEDGEFUNDHOLDINGS H " + "WHERE QUARTER = '"
					+ quarter + "' " + "AND CUSIP = '" + cusip + "'");
			while (results.next()) {

				cikToShares.put(new CIK(results.getString(1)),
						results.getDouble(2));
			}
			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}

		return cikToShares;
	}

	public static void getFundsWithNumberOfHoldings(int minNumHoldings,
			int maxNumHoldings, Quarter quarter) {
		try {

			createConnection();
			stmt = conn.createStatement();

			ResultSet results = stmt.executeQuery("SELECT FUNDNAME  "
					+ "FROM HEDGEFUNDHOLDINGS H JOIN HEDGEFUND H1 "
					+ "ON (H.CIK = H1.CIK)  " + "" + "WHERE QUARTER = '"
					+ quarter + "' " + "GROUP BY H.CIK "
					+ "HAVING numHoldings >=  " + minNumHoldings + " "
					+ "AND numHoldings <= " + maxNumHoldings);

			while (results.next()) {

				System.out.println(results.getString(1));
			}
			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}
	}

	public static ArrayList<Cusip> getCusipsHeldByAtLeast(int numHeldBy,
			Quarter quarter) {
		ArrayList<Cusip> x = new ArrayList<Cusip>();
		try {
			createConnection();
			stmt = conn.createStatement();

			ResultSet results = stmt
					.executeQuery("SELECT CUSIP, count(*) As HELDBY  "
							+ "FROM HEDGEFUNDHOLDINGS " + "WHERE QUARTER = '"
							+ quarter + "' " + "GROUP BY CUSIP "
							+ "HAVING HELDBY >= " + numHeldBy + " "
							+ "ORDER BY HELDBY");

			while (results.next()) {

				x.add(new Cusip(results.getString(1)));
			}
			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}
		return x;
	}

	public static ArrayList<Cusip> getCusipsHeldBy(CIK cik, Quarter quarter) {
		ArrayList<Cusip> x = new ArrayList<Cusip>();
		try {
			createConnection();
			stmt = conn.createStatement();

			ResultSet results = stmt.executeQuery("SELECT CUSIP  "
					+ "FROM HEDGEFUNDHOLDINGS H " + "WHERE QUARTER = '"
					+ quarter + "' " + "AND CIK = '" + cik + "' ");

			while (results.next()) {

				x.add(new Cusip(results.getString(1)));
			}
			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}

		return x;
	}

	public static String getFileName(CIK cik, Quarter quarter) {
		String x = "";
		try {
			createConnection();
			stmt = conn.createStatement();

			ResultSet results = stmt.executeQuery("SELECT  FILE "
					+ "FROM HEDGEFUND  " + "WHERE QUARTER = '" + quarter + "' "
					+ "AND CIK = '" + cik + "'");

			if (results.next()) {

				x = results.getString(1);
			}
			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}
		return x;
	}

	public static void removeFund(CIK cik, Quarter quarter) {
		try {
			createConnection();
			stmt = conn.createStatement();

			stmt.executeUpdate("DELETE  " + "FROM HEDGEFUNDHOLDINGS  "
					+ "WHERE QUARTER = '" + quarter + "' " + "AND CIK = '"
					+ cik + "'");

			stmt.executeUpdate("DELETE  " + "FROM HEDGEFUND  "
					+ "WHERE QUARTER = '" + quarter + "' " + "AND CIK = '"
					+ cik + "'");

			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}
	}

	public static double getFundValueDividedByShare(Cusip cusip, CIK cik,
			Quarter quarter) {
		double x = 0.0;
		try {
			createConnection();
			stmt = conn.createStatement();

			ResultSet results = stmt.executeQuery("SELECT  VALUE, SHARES "
					+ "FROM HEDGEFUNDHOLDINGS H " + "WHERE H.QUARTER = '"
					+ quarter + "' " + "AND CUSIP = '" + cusip + "'"
					+ "AND CIK = '" + cik + "'");

			if (results.next()) {

				x = results.getDouble(1) / results.getDouble(2);
			}
			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}
		return x;
	}

	public static double getPrice(Cusip cusip, Quarter quarter) {
		double x = 0.0;
		try {
			createConnection();
			stmt = conn.createStatement();

			ResultSet results = stmt.executeQuery("SELECT EndQuarterPrice  "
					+ "FROM " + cusipReturn + " " + "WHERE QUARTER = '"
					+ quarter + "' " + "AND CUSIP = '" + cusip + "'");

			if (results.next()) {

				x = results.getDouble(1);
			}
			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}

		return x;
	}

	public static ArrayList<CIK> getCIKS(Quarter quarter) {
		ArrayList<CIK> x = new ArrayList<CIK>();
		try {
			createConnection();
			stmt = conn.createStatement();

			ResultSet results = stmt.executeQuery("SELECT CIK  "
					+ "FROM HEDGEFUND " + "WHERE QUARTER = '" + quarter + "'");

			while (results.next()) {

				x.add(new CIK(results.getString(1)));
			}
			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}

		return x;
	}

	public static ArrayList<Double> getValueDividedByShares(Cusip cusip,
			Quarter quarter) {

		ArrayList<Double> x = new ArrayList<Double>();
		try {
			createConnection();
			stmt = conn.createStatement();

			ResultSet results = stmt.executeQuery("SELECT  VALUE, SHARES "
					+ "FROM HEDGEFUNDHOLDINGS H " + "WHERE H.QUARTER = '"
					+ quarter + "' " + "AND CUSIP = '" + cusip + "'");

			while (results.next()) {

				x.add(results.getDouble(1) / results.getDouble(2));
			}
			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}

		Collections.sort(x);

		return x;
	}

	public static void insertStockPriceReturn(Cusip cusip, double price,
			double ret, Date date) {
		insertValues(stockPrice, cusip + "'," + price + "," + ret + ",'" + date);
	}

	public static int numFundsHolding(Cusip cusip, int numFundHoldingsMin,
			int numFundHoldingsMax, Quarter quarter) {
		int numFundsHolding = 0;
		try {
			createConnection();
			stmt = conn.createStatement();
			ResultSet results = stmt
					.executeQuery("SELECT count(*) AS NumHeldBy "
							+ "FROM  MYDB.HEDGEFUNDHOLDINGS H JOIN  "
							+ "MYDB.HEDGEFUND HF "
							+ "ON (H.CIK = HF.CIK AND H.QUARTER = HF.QUARTER) "
							+ "WHERE H.CUSIP = '" + cusip + "' "
							+ "AND H.QUARTER = '" + quarter + "' "
							+ "AND NUMHOLDINGS >= " + numFundHoldingsMin + " "
							+ "AND NUMHOLDINGS <= " + numFundHoldingsMax);

			if (results.next())
				numFundsHolding = results.getInt(1);

			Lib.assertTrue(!results.next());
			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}

		return numFundsHolding;
	}

	public static double numSharesHeld(Cusip cusip, int numFundHoldingsMin,
			int numFundHoldingsMax, Quarter quarter) {

		double numFundsHolding = 0;
		try {
			createConnection();
			stmt = conn.createStatement();
			ResultSet results = stmt
					.executeQuery("SELECT SUM(SHARES) AS NumHeldBy "
							+ "FROM  MYDB.HEDGEFUNDHOLDINGS H JOIN  "
							+ "MYDB.HEDGEFUND HF "
							+ "ON (H.CIK = HF.CIK AND H.QUARTER = HF.QUARTER) "
							+ "WHERE H.CUSIP = '" + cusip + "' "
							+ "AND H.QUARTER = '" + quarter + "' "
							+ "AND NUMHOLDINGS >= " + numFundHoldingsMin + " "
							+ "AND NUMHOLDINGS <= " + numFundHoldingsMax);

			if (results.next())
				numFundsHolding = results.getDouble(1);

			Lib.assertTrue(!results.next());
			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}

		return numFundsHolding;
	}

	public static Hashtable<Date, Double> getDateToReturn(Cusip cusip) {
		Hashtable<Date, Double> dateToReturn = new Hashtable<Date, Double>();
		try {
			createConnection();
			stmt = conn.createStatement();
			ResultSet results = stmt.executeQuery("SELECT DATE, MONTHLYRET"
					+ "FROM " + stockPrice + " " + "WHERE CUSIP = '" + cusip
					+ "' ");

			while (results.next()) {
				dateToReturn.put(new Date(results.getString(1)),
						results.getDouble(2));
			}

			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}

		return dateToReturn;
	}

	public static void setThreeMonthStockReturn(Cusip cusip,
			Hashtable<Quarter, Double> quarterToFutureReturn) {
		for (Quarter q : quarterToFutureReturn.keySet()) {
			setThreeMonthStockReturn(cusip, quarterToFutureReturn.get(q), q);
		}

	}

	public static void setThreeMonthStockReturn(Cusip cusip, double ret,
			Quarter quarter) {
		insertValues(cusipReturn, "'" + cusip + "', " + ret + ", '" + quarter
				+ "'");
	}

	public static Hashtable<Quarter, Double> getThreeMonthStockReturn(
			Cusip cusip) {
		Hashtable<Quarter, Double> quarterToReturn = new Hashtable<Quarter, Double>();
		try {
			createConnection();
			stmt = conn.createStatement();
			ResultSet results = stmt.executeQuery("SELECT Quarter, RET "
					+ "FROM " + cusipReturn + " " + "WHERE CUSIP = '" + cusip
					+ "' ");

			while (results.next()) {
				quarterToReturn.put(new Quarter(results.getString(1)),
						results.getDouble(2));
			}

			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}

		return quarterToReturn;

	}

	public static double getThreeMonthStockReturn(Cusip cusip, Quarter quarter) {
		double ret = 0.0;
		try {
			createConnection();
			stmt = conn.createStatement();
			ResultSet results = stmt.executeQuery("SELECT Quarter, RET "
					+ "FROM " + cusipReturn + " " + "WHERE CUSIP = '" + cusip
					+ "' " + "AND QUARTER = '" + quarter + "'");

			if (results.next()) {
				ret = results.getDouble(2);
			}
			Lib.assertTrue(!results.next());

			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}

		return ret;

	}

	private static void batchLoad(String table, String filename) {
		try {
			createConnection();
			if (!tableExists(table))
				createTable(table);

			Statement stmt = conn.createStatement();

			// Load the data
			System.out
					.println("LOAD DATA local INFILE '" + filename
							+ "' IGNORE INTO TABLE " + table
							+ " FIELDS TERMINATED BY ','"
							+ " LINES TERMINATED BY '\n'");
			stmt.executeUpdate("LOAD DATA local INFILE '" + filename
					+ "' IGNORE INTO TABLE " + table
					+ " FIELDS TERMINATED BY ','" + " LINES TERMINATED BY '\n'");

			closeConnection();
		} catch (Exception e) {
			handleException(e);
		}
	}

	public static void batchLoadStockPrice(String filename) {
		batchLoad(stockPrice, filename);
	}

	public static void batchLoadCusipReturn(String filename) {
		batchLoad(cusipReturn, filename);
	}

	public static void batchLoadCIKReturn(String filename) {
		batchLoad(cikReturn, filename);
	}

	public static Hashtable<Cusip, Double> getShares(CIK cik, Quarter quarter) {
		Hashtable<Cusip, Double> cusipToShares = new Hashtable<Cusip, Double>();
		try {

			createConnection();
			stmt = conn.createStatement();

			ResultSet results = stmt.executeQuery("SELECT CUSIP, SHARES  "
					+ "FROM HEDGEFUNDHOLDINGS H " + "WHERE QUARTER = '"
					+ quarter + "' " + "AND CIK = '" + cik + "'");
			while (results.next()) {

				cusipToShares.put(new Cusip(results.getString(1)),
						results.getDouble(2));
			}
			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}

		return cusipToShares;
	}

	public static double getFundValue(CIK cik, Quarter quarter) {
		double value = 0.0;
		try {

			createConnection();
			stmt = conn.createStatement();

			ResultSet results = stmt.executeQuery("SELECT SUM(VALUE) "
					+ "FROM HEDGEFUNDHOLDINGS H " + "WHERE QUARTER = '"
					+ quarter + "' " + "AND CIK = '" + cik + "'");
			if (results.next()) {

				value = results.getDouble(1);
			}
			Lib.assertTrue(!results.next());
			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}

		return value;
	}

	public static void batchSetPortionOfFund(CIK cik, String filename) {
		try {
			createConnection();
			Statement stmt = conn.createStatement();

			stmt.execute("CREATE TEMPORARY TABLE temp_data(CUSIP char(9), PORTIONOFFUND float, "
					+ "QUARTER char(10)) ;");
			// Load the data
			stmt.execute("LOAD DATA local INFILE '" + filename + "' "
					+ "INTO TABLE temp_data " + " FIELDS TERMINATED BY ','"
					+ " LINES TERMINATED BY '\n'");

			stmt.execute("UPDATE " + hedgeFundHoldings + " H , temp_data T "
					+ "SET H.PORTIONOFFUND = T.PORTIONOFFUND "
					+ "WHERE H.CUSIP = T.CUSIP AND " + "H.CIK = '" + cik
					+ "' AND " + "H.QUARTER = T.QUARTER");
			stmt.execute("DROP TABLE temp_data");
			closeConnection();
		} catch (Exception e) {
			handleException(e);
		}
	}

	public static void batchSetFundReturn(String filename) {
		try {
			createConnection();

			if (!tableExists(cikReturn))
				createTable(cikReturn);
			Statement stmt = conn.createStatement();
			stmt.execute("DROP TABLE temp_data");
			stmt.execute("CREATE TEMPORARY TABLE temp_data(CIK char(10), RET float, "
					+ "QUARTER char(10)) ;");
			// Load the data
			stmt.execute("LOAD DATA local INFILE '" + filename + "' "
					+ "INTO TABLE temp_data " + " FIELDS TERMINATED BY ','"
					+ " LINES TERMINATED BY '\n'");

			stmt.execute("UPDATE " + cikReturn + " H , temp_data T"
					+ "SET H.RET = T.RET "
					+ "WHERE H.CIK = T.CIK AND H.QUARTER = T.QUARTER");
			closeConnection();
		} catch (Exception e) {
			handleException(e);
		}
	}

	public static double getPortionOfFund(CIK cik, Cusip cusip, Quarter quarter) {
		double portionOfFund = 0.0;
		try {

			createConnection();
			stmt = conn.createStatement();

			ResultSet results = stmt.executeQuery("SELECT PORTIONOFFUND "
					+ "FROM HEDGEFUNDHOLDINGS " + "WHERE QUARTER = '" + quarter
					+ "' " + "AND CUSIP = '" + cusip + "' " + "AND CIK = '"
					+ cik + "'");
			if (results.next())
				portionOfFund = results.getDouble(1);

			Lib.assertTrue(!results.next());
			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}

		return portionOfFund;

	}

	public static void batchLoadHedgeFundHoldings(String filename) {
		batchLoad(hedgeFundHoldings, filename);
	}

	public static void makeDesignTemp(String file,
			Quarter quarterB4FirstQuarter, Quarter quarterAfterLastQuarter) {
		try {

			createConnection();
			stmt = conn.createStatement();

			stmt = conn.createStatement();

			System.out
					.println("SELECT TICKER, H.QUARTER, CIK, PORTIONOFFUND "
							+ ", RET "
							+ "FROM HEDGEFUNDHOLDINGS H "
							+ "JOIN CUSIPTICKER C ON (C.CUSIP = H.CUSIP AND C.QUARTER = H.QUARTER) "
							+ "JOIN CUSIPRETURN S ON (H.CUSIP = S.CUSIP AND S.QUARTER = H.QUARTER) "
							+ " WHERE STRCMP(H.QUARTER, '"
							+ quarterB4FirstQuarter + "') = 1"
							+ " AND STRCMP(H.QUARTER, '"
							+ quarterAfterLastQuarter + "') = -1 "
							+ " ORDER BY  TICKER, QUARTER " + " INTO OUTFILE '"
							+ file + "'" + "  FIELDS TERMINATED BY ','");
			stmt.executeQuery("SELECT C.TICKER, H.QUARTER, H.CIK, H.PORTIONOFFUND "
					+ ", S.RET "
					+ "FROM HEDGEFUNDHOLDINGS H "
					+ "JOIN CUSIPTICKER C ON (C.CUSIP = H.CUSIP AND C.QUARTER = H.QUARTER) "
					+ " JOIN CUSIPRETURN S ON (H.CUSIP = S.CUSIP AND S.QUARTER = H.QUARTER) "
					+ " JOIN HEDGEFUND HF ON (H.CIK = HF.CIK AND HF.QUARTER = H.QUARTER) "
					+ " WHERE STRCMP(H.QUARTER, '"
					+ quarterB4FirstQuarter
					+ "') = 1"
					+ " AND STRCMP(H.QUARTER, '"
					+ quarterAfterLastQuarter
					+ "') = -1 "
					+ " AND NUMHOLDINGS >= 10 AND NUMHOLDINGS <= 200 "
					+ " ORDER BY  TICKER, QUARTER "
					+ " INTO OUTFILE '"
					+ file
					+ "'" + "  FIELDS TERMINATED BY ','");

			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}
	}

	public static void batchLoadSharesOutstanding(String file) {
		batchLoad(tempShares, file);
	}

	public static void batchLoadCusipTickers(String file) {
		batchLoad(cusipTicker, file);
	}

	public static void deleteRowsIfCusipNotIncusipReturn(Quarter quarter) {
		try {
			createConnection();
			stmt = conn.createStatement();
			stmt.executeUpdate("DELETE FROM " + hedgeFundHoldings
					+ " WHERE QUARTER = '" + quarter + "' AND CUSIP NOT IN"
					+ " (SELECT CUSIP FROM " + cusipReturn
					+ " WHERE QUARTER = '" + quarter.getPreviousQuarter()
					+ "' )");
		} catch (Exception e) {
			handleException(e);
		}
		closeConnection();
	}

	public static void setTurnover(Quarter quarter) {
		// count(CUSIP) quarter.getPreviousQuarter() INNER JOIN quarter Group by
		// CIK / count(Cusip) quarter Group by CIK
	}
}
