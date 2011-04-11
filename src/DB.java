import java.io.BufferedWriter;
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

public class DB {

	private static String dbURL = "jdbc:mysql://localhost/myDB";

	private static String cusipTicker = "CUSIPTICKER";
	private static String cusipTickerFields = "CUSIP char(9), "
			+ "TICKER char(10), " + // Ticker length is longer than 4 to accommodate unusually
									// long tickers
			"QUARTER char(10), "+
			"UNIQUE(CUSIP,QUARTER)";

	private static String hedgeFund = "HEDGEFUND";
	private static String hedgeFundFields = "CIK char(10), "
			+ "FundName varchar(255), " 
			+ "NumHoldings int, "
			+ "FILE varchar(255), "
			+ "QUARTER char(10), "
			+ "PRIMARY KEY(CIK), "
			+ "UNIQUE(FundName, QUARTER)";

	private static String hedgeFundHoldings = "HEDGEFUNDHOLDINGS";
	private static String hedgeFundHoldingsFields = "CUSIP char(9), "
			+ "CIK char(10), " + "VALUE float, " + "SHARES float, "
			+ "QUARTER char(10), "
			+ "UNIQUE(CIK,CUSIP, QUARTER), "
			+ "FOREIGN KEY(CIK) REFERENCES HEDGEFUND(CIK)";

	private static String stockPrice = "STOCKPRICE";
	private static String stockPriceFields = "CUSIP char(9), "
		+ "PRICE float, "
		+ "RET float, "
		+ "QUARTER char(10), "
		+ "UNIQUE(CUSIP, QUARTER)";
	
	private static Hashtable<String, String> tableNameToFields = new Hashtable<String, String>();
	// Keep track of whether or not the table exists
	private static Hashtable<String, Boolean> tableExists = new Hashtable<String, Boolean>();

	static {
		tableNameToFields.put(hedgeFund, hedgeFundFields);
		tableNameToFields.put(hedgeFundHoldings, hedgeFundHoldingsFields);
		tableNameToFields.put(cusipTicker, cusipTickerFields);
		tableNameToFields.put(stockPrice, stockPriceFields);
		
		tableExists.put(cusipTicker, false);
		tableExists.put(hedgeFund, false);
		tableExists.put(hedgeFundHoldings, false);
		tableExists.put(stockPrice, false);
	}

	// jdbc Connection
	private static Connection conn = null;
	private static Statement stmt = null;

	private static final DB INSTANCE = new DB();

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

	private static void createConnection() {
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
			stmt
					.executeUpdate("CREATE TABLE TEMP_SHARES(CUSIP char(9), CSHOQ float, "
							+ "QUARTER char(10),  UNIQUE(CUSIP, QUARTER))");
		} catch (Exception e) {
			handleException(e);
		}
	}

	private static void insertValues(String table, String values){
		try {
			createConnection();
			if (!tableExists(table))
				createTable(table);
			stmt = conn.createStatement();
			stmt.execute("INSERT IGNORE INTO "+ table +" values ('" + values + "')");
			stmt.close();
		} catch (Exception e) {
			handleException(e);
		}
		closeConnection();
	}
	
	public void insertTempSharesTable(Cusip cusip, double cshoq, Quarter quarter) {
		insertValues("TEMP_SHARES", cusip + "', " + cshoq + " ," + "'" + quarter);
	}

	public static Hashtable<Cusip, Double> getSharesOustandingAllCusips(Quarter quarter) {
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
					+ "TEMP_SHARES WHERE QUARTER = '" + quarter + "' " +
							"AND CUSIP = '"+ cusip +"'");

			
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
	
	public static void selectSharesOutstanding() {
		try {
			createConnection();
			stmt = conn.createStatement();
			ResultSet results = stmt.executeQuery("select * from "
					+ "TEMP_SHARES");

			Cusip cusip;
			double cshoq;
			Quarter quarter;
			while (results.next()) {
				cusip = new Cusip(results.getString(1));
				cshoq = results.getDouble(2);
				quarter = new Quarter(results.getString(3));
				System.out.println(cusip + " " + cshoq + " " + quarter);
			}

			results.close();
		} catch (Exception e) {
			handleException(e);
		}
		closeConnection();
	}

	public static void createTempCusipTable() {
		try {
			createConnection();
			stmt = conn.createStatement();
			stmt
					.executeUpdate("CREATE TABLE TEMP(CUSIP char(9), UNIQUE(CUSIP))");
		} catch (Exception e) {
			handleException(e);
		}
		closeConnection();
	}

	public static void insertTempCusipTable(Cusip cusip) {
		try {
			createConnection();
			stmt = conn.createStatement();
			stmt.execute("INSERT INTO TEMP values ('" + cusip + "' )");
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
			ResultSet results = stmt.executeQuery("select * from TEMP");

			BufferedWriter out = new BufferedWriter(new FileWriter(
					"tempCusip.txt"));
			while (results.next())
				out.write(results.getString(1) + "\n");
			out.close();
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

	public void insertHedgeFund(CIK cik, String fundName, Quarter quarter, String fileName) {
		fundName = fundName.replaceAll("'", "");
		fundName = fundName.replace("\\", "");
		insertValues(hedgeFund,cik + "'," 
				+ "'" + fundName + "', "
				+ "0, "
				+ "'" + fileName +"', "
				+ "'" + quarter + "'");
	}

	public void updateNumHoldings(Quarter quarter){
		try {
			createConnection();
			if (!tableExists(hedgeFund))
				return;
			stmt = conn.createStatement();
			stmt.execute("UPDATE "+ hedgeFund + " HF "+
					"SET NUMHOLDINGS =  " +
					"(SELECT COUNT(CUSIP) " +
					"FROM "+ hedgeFundHoldings + " HFH " +
					"WHERE HF.CIK = HFH.CIK AND HFH.QUARTER = '"+ quarter +"')" +
					"WHERE HF.QUARTER = '"+ quarter  +"'");
			stmt.close();
		} catch (Exception e) {
			handleException(e);
		}
		closeConnection();
	}
	
	public void insertHedgeFundHoldings(Cusip cusip, CIK cik, double value,
			double shares, Quarter quarter) {
		
		insertValues(hedgeFundHoldings, cusip + "','" + cik + "'," + value + ","
					+ shares + ",'" + quarter);
	}

	public void insertCusipTicker(Cusip cusip, String tick, Quarter quarter) {
		insertValues(cusipTicker, cusip + "','" + tick + "','" + quarter);
	}

	public static void select50HedgeFunds() {
		try {
			createConnection();
			stmt = conn.createStatement();
			stmt.setMaxRows(50);
			ResultSet results = stmt.executeQuery("select * from " + hedgeFund);
			ResultSetMetaData rsmd = results.getMetaData();
			int numberCols = rsmd.getColumnCount();
			for (int i = 1; i <= numberCols; i++) {
				// print Column Names
				System.out.print(rsmd.getColumnLabel(i) + "\t\t");
			}

			System.out
					.println("\n-------------------------------------------------");

			while (results.next()) {
				CIK cik = new CIK(results.getString(1));
				String fundName = results.getString(2);
				System.out.println(cik + "\t\t" + fundName);
			}

			results.close();

		} catch (Exception e) {
			handleException(e);
		}
		closeConnection();
	}

	public static void select50HedgeFundsHoldings() {
		try {
			createConnection();
			stmt = conn.createStatement();
			stmt.setMaxRows(2000);
			ResultSet results = stmt.executeQuery("select * from "
					+ hedgeFundHoldings + " WHERE CUSIP = '002824100'");
			ResultSetMetaData rsmd = results.getMetaData();
			int numberCols = rsmd.getColumnCount();
			for (int i = 1; i <= numberCols; i++) {
				// print Column Names
				System.out.print(rsmd.getColumnLabel(i) + "\t\t\t");
			}

			System.out
					.println("\n-------------------------------------------------");
			Cusip cusip;
			CIK cik;
			double value;
			double shares;
			Quarter quarterDir;
			while (results.next()) {
				cusip = new Cusip(results.getString(1));
				cik = new CIK(results.getString(2));
				value = results.getDouble(3);
				shares = results.getDouble(4);
				quarterDir = new Quarter(results.getString(5));
				System.out.println(cusip + "\t\t" + cik + "\t\t" + value
						+ "\t\t" + shares + "\t\t\t" + quarterDir);
			}

			results.close();
		} catch (Exception e) {
			handleException(e);
		}
		closeConnection();
	}

	public static void select50CusipTicker() {
		try {
			createConnection();
			stmt = conn.createStatement();
			stmt.setMaxRows(50);
			ResultSet results = stmt.executeQuery("select * from "
					+ cusipTicker);
			ResultSetMetaData rsmd = results.getMetaData();
			int numberCols = rsmd.getColumnCount();
			for (int i = 1; i <= numberCols; i++) {
				// print Column Names
				System.out.print(rsmd.getColumnLabel(i) + "\t\t\t");
			}

			System.out
					.println("\n-------------------------------------------------");
			Cusip cusip;
			String tick;
			Quarter quarterDir;
			while (results.next()) {
				cusip = new Cusip(results.getString(1));
				tick = results.getString(2);
				quarterDir = new Quarter(results.getString(5));
				System.out.println(cusip + "\t\t" + tick + "\t\t\t"
						+ quarterDir);
			}

			results.close();
		} catch (Exception e) {
			handleException(e);
		}
	}

	public static void outputAllTables() {
		try {
			createConnection();
			DatabaseMetaData dbmd = conn.getMetaData();

			// Specify the type of object; in this case we want tables
			String[] types = { "TABLE" };
			ResultSet res = dbmd.getTables(null, null, null, types);

			// Get the table names
			while (res.next()) {
				// Get the table name
				System.out.println("   " + res.getString("TABLE_CAT") + ", "
						+ res.getString("TABLE_SCHEM") + ", "
						+ res.getString("TABLE_NAME") + ", "
						+ res.getString("TABLE_TYPE") + ", "
						+ res.getString("REMARKS"));
			}
			res.close();
		} catch (Exception e) {
			closeConnection();
			e.printStackTrace();
		}
		closeConnection();
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

			return cusips;
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

	public static void outputNumSharesHeldByFunds(Quarter quarter) {
		try {

			createConnection();
			stmt = conn.createStatement();
			ResultSet results = stmt.executeQuery("select CUSIP, SHARES from "
					+ cusipTicker + " C, " + hedgeFundHoldings
					+ " H WHERE C.QUARTER = '" + quarter + "' AND "
					+ "C.CUSIP = H.CUSIP ");
			String tick = "";
			double summedShares = 0;
			while (results.next()) {
				tick = results.getString(1);
				summedShares = results.getDouble(2);
				System.out.println(tick + " " + summedShares);
			}
			closeConnection();
		} catch (Exception e) {
			handleException(e);
		}

	}

	public static void getNumberOfHoldings(String CIK, Quarter quarter) {
		try {

			createConnection();
			stmt = conn.createStatement();
			ResultSet results = stmt.executeQuery("SELECT count(*) " + "FROM "
					+ hedgeFundHoldings + " " + "WHERE CIK = '" + CIK + "' "
					+ "AND QUARTER = '" + quarter + "'");

			while (results.next()) {

				System.out.println(results.getString(1));
			}
			closeConnection();
		} catch (Exception e) {
			handleException(e);
		}
	}

	private Cusip getCusipFromTicker(String tick, Quarter quarter) {
		Cusip cusip = null;
		try {
			stmt = conn.createStatement();

			ResultSet results = stmt.executeQuery("SELECT CUSIP "
					+ "FROM "+cusipTicker+" " + "WHERE TICKER = '"
					+ tick + "' AND QUARTER = '" + quarter +"'");

			if (results.next()) {
				cusip =  new Cusip(results.getString(1));
			}
			Lib.assertTrue(!results.next());

		} catch (Exception e) {
			handleException(e);
		}
		return cusip;
	}

	public void getNumberOfHedgeFundHoldingTicker(String tick,
			int minNumHoldings, int maxNumHoldings, Quarter quarter) {
		try {
			createConnection();
			stmt = conn.createStatement();

			System.out.println("SELECT CIK "
					+ "FROM MYDB.HEDGEFUNDHOLDINGS H " + "WHERE CUSIP = '"
					+ getCusipFromTicker(tick, quarter) + "' AND " +
			"CIK IN "+
			 "(SELECT CIK  " +
			 "FROM MYDB.HEDGEFUNDHOLDINGS " +
			 "GROUP BY CIK " +
			 "HAVING count(CUSIP) >=  "+ minNumHoldings + " "+
			 "AND count(CUSIP) <= " + maxNumHoldings + ")");
			
			ResultSet results = stmt.executeQuery("SELECT CIK "
					+ "FROM HEDGEFUNDHOLDINGS H " + "WHERE CUSIP = '"
					+ getCusipFromTicker(tick, quarter) + "' AND " +
			"CIK IN "+
			 "(SELECT CIK  " +
			 "FROM HEDGEFUNDHOLDINGS " +
			 "GROUP BY CIK " +
			 "HAVING count(CUSIP) >=  "+ minNumHoldings + " "+
			 "AND count(CUSIP) <= " + maxNumHoldings + ")");


			
			while (results.next()) {

				System.out.println(results.getString(1));
			}
			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}
	}

	public static Hashtable<CIK, Double> getFundValues(Quarter quarter){
		Hashtable<CIK, Double> cikToFundValue = new Hashtable<CIK, Double>();
		try {

			createConnection();
			stmt = conn.createStatement();

			ResultSet results = stmt.executeQuery("SELECT CIK, SUM(VALUE)  "
					+ "FROM HEDGEFUNDHOLDINGS H "
					+ "WHERE QUARTER = '" + quarter+ "' " 
					+ "GROUP BY CIK");
			while (results.next()) {

				cikToFundValue.put(new CIK(results.getString(1)), results.getDouble(2));
			}
			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}
		
		return cikToFundValue;
	}
	
	public static ArrayList<CIK> getCIKS(){
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
	
	public static Hashtable<CIK, Double> getFundsToShares(Cusip cusip, Quarter quarter){
		Hashtable<CIK, Double> cikToShares = new Hashtable<CIK, Double>();
		try {

			createConnection();
			stmt = conn.createStatement();

			ResultSet results = stmt.executeQuery("SELECT CIK, SHARES  "
					+ "FROM HEDGEFUNDHOLDINGS H "
					+ "WHERE QUARTER = '" + quarter+ "' " +
							"AND CUSIP = '"+ cusip + "'" );
			while (results.next()) {

				cikToShares.put(new CIK(results.getString(1)), results.getDouble(2));
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
					+ "ON (H.CIK = H1.CIK)  " + "" 
					+ "WHERE QUARTER = '" + quarter+ "' " 
					+ "GROUP BY H.CIK "
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
	
	public static ArrayList<Cusip> getCusipsHeldByAtLeast(int numHeldBy, Quarter quarter){
		ArrayList<Cusip> x = new ArrayList<Cusip>();
		try {
			createConnection();
			stmt = conn.createStatement();

			ResultSet results = stmt.executeQuery("SELECT CUSIP, count(*) As HELDBY  "
					+ "FROM HEDGEFUNDHOLDINGS "
					+ "WHERE QUARTER = '" + quarter + "' "
					+ "GROUP BY CUSIP " 
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
	
	public static ArrayList<Cusip> getCusipsHeldBy(CIK cik, Quarter quarter){
		ArrayList<Cusip> x = new ArrayList<Cusip>();
		try {
			createConnection();
			stmt = conn.createStatement();

			ResultSet results = stmt.executeQuery("SELECT CUSIP  "
					+ "FROM HEDGEFUNDHOLDINGS H "
					+ "WHERE QUARTER = '" + quarter + "' " +
							"AND CIK = '" + cik  +"' ");

			while (results.next()) {

				x.add(new Cusip(results.getString(1)));
			}
			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}
		
		return x;
	}
	
	public static String getFileName(CIK cik, Quarter quarter){
		String x = "";
		try {
			createConnection();
			stmt = conn.createStatement();

			ResultSet results = stmt.executeQuery("SELECT  FILE " +
					"FROM HEDGEFUND  " +
					"WHERE QUARTER = '"+ quarter + "' " +
							"AND CIK = '"+ cik  +"'");

			if (results.next()) {

				x = results.getString(1);
			}
			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}
		return x;
	}
	
	public static void removeFund(CIK cik, Quarter quarter){
		try {
			createConnection();
			stmt = conn.createStatement();

			stmt.executeUpdate("DELETE  " +
					"FROM HEDGEFUNDHOLDINGS  " +
					"WHERE QUARTER = '"+ quarter + "' " +
							"AND CIK = '"+ cik  +"'");

			stmt.executeUpdate("DELETE  " +
					"FROM HEDGEFUND  " +
					"WHERE QUARTER = '"+ quarter + "' " +
							"AND CIK = '"+ cik  +"'");
			
			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}
	}
	
	public static double getFundValueDividedByShare(Cusip cusip, CIK cik, Quarter quarter){
		double x = 0.0;
		try {
			createConnection();
			stmt = conn.createStatement();

			ResultSet results = stmt.executeQuery("SELECT  VALUE, SHARES " +
					"FROM HEDGEFUNDHOLDINGS H " +
					"WHERE H.QUARTER = '"+ quarter + "' " +
							"AND CUSIP = '"+ cusip +"'" +
							"AND CIK = '"+ cik  +"'");

			if (results.next()) {

				x = results.getDouble(1)/results.getDouble(2);
			}
			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}
		return x;
	}
	
	public static double getPrice(Cusip cusip, Quarter quarter){
		double x=  0.0;
		try {
			createConnection();
			stmt = conn.createStatement();

			ResultSet results = stmt.executeQuery("SELECT PRICE  "
					+ "FROM " + stockPrice + " "
					+ "WHERE QUARTER = '" + quarter + "'" +
							"AND CUSIP = '" + cusip +  "'");

			if (results.next()) {

				x = results.getDouble(1);
			}
			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}
		
		return x;
	}
	
	public static ArrayList<CIK> getCIKS(Quarter quarter){
		ArrayList<CIK> x = new ArrayList<CIK>();
		try {
			createConnection();
			stmt = conn.createStatement();

			ResultSet results = stmt.executeQuery("SELECT CIK  "
					+ "FROM HEDGEFUND "
					+ "WHERE QUARTER = '" + quarter + "'");

			while (results.next()) {

				x.add(new CIK(results.getString(1)));
			}
			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}
		
		return x;
	}
	
	public static ArrayList<Double> getValueDividedByShares(Cusip cusip, Quarter quarter){
		
		ArrayList<Double> x = new ArrayList<Double>();
		try {
			createConnection();
			stmt = conn.createStatement();

			ResultSet results = stmt.executeQuery("SELECT  VALUE, SHARES " +
					"FROM HEDGEFUNDHOLDINGS H " +
					"WHERE H.QUARTER = '"+ quarter + "' " +
							"AND CUSIP = '"+ cusip +"'");

			while (results.next()) {

				x.add(results.getDouble(1)/results.getDouble(2));
			}
			closeConnection();

		} catch (Exception e) {
			handleException(e);
		}
		
		Collections.sort(x);
		
		return x;
	}
	
	public static void insertStockPriceReturn(Cusip cusip, double price, double ret, Quarter quarter){
		insertValues(stockPrice, cusip + "'," + price + "," + ret +",'" + quarter);
	}
	
}
