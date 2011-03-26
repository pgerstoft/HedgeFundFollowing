import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;

public class DB {

	private static String dbURL = "jdbc:derby:myDB;create=true";
	
	private static String cusipTicker = "CUSIPTICKER";
	private static String cusipTickerFields = "CUSIP char(9), " +
			"TICKER char(10), " + //Ticker length is to accommodate unusually long tickers
			"QUARTER char(10)";
	
	private static String hedgeFund = "HEDGEFUND";
	private static String hedgeFundFields = "CIK char(10), " +
			"FundName char(128), " +
			"PRIMARY KEY(CIK), " +
			"UNIQUE(FundName)";
	
	private static String hedgeFundHoldings = "HEDGEFUNDHOLDINGS";
	private static String hedgeFundHoldingsFields = "CUSIP char(9), " +
			"CIK char(10), " +
			"VALUE float, " +
			"SHARES float, " +
			"QUARTER char(10), " +
			"PRIMARY KEY(CUSIP), " +
			"UNIQUE(CIK,CUSIP, quarter), " +
			"FOREIGN KEY(CIK) REFERENCES HEDGEFUND(CIK)";
		
	private static Hashtable<String, String> tableNameToFields = new Hashtable<String, String>();
	//Keep track of whether or not the table exists
	private static Hashtable<String, Boolean> tableExists = new Hashtable<String, Boolean>();
	
	static{
		tableNameToFields.put(hedgeFund,hedgeFundFields);
		tableNameToFields.put(hedgeFundHoldings, hedgeFundHoldingsFields);
		tableNameToFields.put(cusipTicker, cusipTickerFields);
		
		tableExists.put(cusipTicker, false);
		tableExists.put(hedgeFund, false);
		tableExists.put(hedgeFundHoldings, false);
	}
	
	// jdbc Connection
	private static Connection conn = null;
	private static Statement stmt = null;

	private static final DB INSTANCE = new DB();
	
	public static DB getInstance(){
		return INSTANCE;
	}
	
	private DB(){
		createConnection();
	}
	
	private void createConnection() {
		try {
			Class.forName("org.apache.derby.jdbc.ClientDriver").newInstance();
			// Get a connection
			conn = DriverManager.getConnection(dbURL);
		} catch (Exception except) {
			except.printStackTrace();
			System.exit(1);
		}
	}

	private void shutdown() {
		try {
			if (stmt != null) {
				stmt.close();
			}
			if (conn != null) {
				DriverManager.getConnection(dbURL + ";shutdown=true");
				conn.close();
			}
		} catch (SQLException sqlExcept) {
		}
	}

	private static void createTable(String table) {
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate("CREATE TABLE " + table+ "("+ tableNameToFields.get(table) +")");
		} catch (SQLException sqlExcept) {
			sqlExcept.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void createTempSharesTable(){
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate("CREATE TABLE TEMP_SHARES(CUSIP char(9), CSHOQ float, " +
					"QUARTER char(10),  UNIQUE(CUSIP, QUARTER))");
		} catch (SQLException sqlExcept) {
			sqlExcept.printStackTrace();
			System.exit(1);
		}
	}
	
	public void insertTempSharesTable(String cusip, double cshoq, String quarter){
		try {
			if(!tableExists("TEMP_SHARES"))
				createTempSharesTable();
			stmt = conn.createStatement();
			stmt.execute("INSERT INTO TEMP_SHARES values ('" + cusip + "', " + cshoq+ " ," +
					"'"+ quarter + "')");
			stmt.close();
		} catch (SQLException sqlExcept) {
			if(sqlExcept.getSQLState().equals("23505"))//Found duplicate from database view
				 return;//ignore duplicate and continue with the insert statement
			sqlExcept.printStackTrace();
			System.exit(1);
		}
	}
	
	public static Hashtable<String, Double> getSharesOustandingAllCusips(String quarter){
		Hashtable<String, Double> cusipToCSHOQ = new Hashtable<String, Double>();
		try {
			stmt = conn.createStatement();
			ResultSet results = stmt.executeQuery("select CUSIP, TICKER from " + "TEMP_SHARES " +
					"WHERE QUARTER = '" + quarter + "'");
			
			String cusip;
			double cshoq;
			while (results.next()) {
				cusip = results.getString(1);
				cshoq = results.getDouble(2);
				cusipToCSHOQ.put(cusip, cshoq);
			}
			
			results.close();
			stmt.close();
		} catch (SQLException sqlExcept) {
			sqlExcept.printStackTrace();
			System.exit(1);
		}
		
		return cusipToCSHOQ;
	}
	
	public static void createTempCusipTable(){
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate("CREATE TABLE TEMP(CUSIP char(9), UNIQUE(CUSIP))");
		} catch (SQLException sqlExcept) {
			sqlExcept.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void insertTempCusipTable(String cusip){
		try {
			stmt = conn.createStatement();
			stmt.execute("INSERT INTO TEMP values ('" + cusip + "' )");
			stmt.close();
		} catch (SQLException sqlExcept) {
			if(sqlExcept.getSQLState().equals("23505"))//Found duplicate from database view
				 return;//ignore duplicate and continue with the insert statement
			sqlExcept.printStackTrace();
			System.exit(1);
		}
	}
	
	//returns all CUSIPs from that quarter
	public static void writeTempVals(){
		try {
			stmt = conn.createStatement();
			ResultSet results = stmt.executeQuery("select * from TEMP");
					
			BufferedWriter out = new BufferedWriter(new FileWriter("tempCusip.txt"));
			while (results.next()) 
				out.write(results.getString(1) + "\n");
			out.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void deleteTempTable(){
		try {
			if(tableExists("TEMP")){
				stmt = conn.createStatement();
				stmt.executeUpdate("DROP TABLE TEMP");
			}
		} catch (SQLException sqlExcept) {
			sqlExcept.printStackTrace();
			System.exit(1);
		}
	}

	private static boolean tableExists(String table) {
		if(tableExists.contains(table) &&tableExists.get(table)) 
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
		} catch (SQLException sqlExcept) {
			sqlExcept.printStackTrace();
			System.exit(1);
		}
		
		return true;
	}

	public void insertHedgeFund(String cik, String fundName){
		try {
			if(!tableExists(hedgeFund))
				createTable(hedgeFund);

			fundName = fundName.replaceAll("'", "");
			
			stmt = conn.createStatement();
			System.out.println("INSERT INTO " + hedgeFund + " values ('" + cik + "','" + fundName + "')");
			stmt.execute("INSERT INTO " + hedgeFund + " values ('" + cik + "','"
					+ fundName + "')");
			stmt.close();
		} catch (SQLException sqlExcept) {
			 if(sqlExcept.getSQLState().equals("23505"))//Found duplicate from database view
				 return;//ignore duplicate and continue with the insert statement
			sqlExcept.printStackTrace();
			System.exit(1);
		}
	}
	
	public void insertHedgeFundHoldings(String cusip, String cik, double value, double shares, String quarter){
		try {
			if(!tableExists(hedgeFundHoldings))
				createTable(hedgeFundHoldings);
			
			stmt = conn.createStatement();
			stmt.execute("INSERT INTO " + hedgeFundHoldings + " values ('" + cusip + "','"
					+ cik + "'," + value + "," + shares+",'" + quarter +"' )");
			stmt.close();
		} catch (SQLException sqlExcept) {
			if(sqlExcept.getSQLState().equals("23505"))//Found duplicate from database view
				 return;//ignore duplicate and continue with the insert statement
			sqlExcept.printStackTrace();
			System.exit(1);
		}
	}
	
	public void insertCusipTicker(String cusip, String tick, String quarter){
		try {
			if(!tableExists(cusipTicker))
				createTable(cusipTicker);
			
			stmt = conn.createStatement();
			stmt.execute("INSERT INTO " + cusipTicker + " values ('" + cusip + "','"
					+ tick + "','" + quarter +"' )");
			stmt.close();
		} catch (SQLException sqlExcept) {
			if(sqlExcept.getSQLState().equals("23505"))//Found duplicate from database view
				 return;//ignore duplicate and continue with the insert statement
			sqlExcept.printStackTrace();
			System.exit(1);
		}

	}

	public static void select50HedgeFunds() {
		try {
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
				String cik = results.getString(1);
				String fundName = results.getString(2);
				System.out.println(cik + "\t\t" + fundName);
			}
			
			results.close();
			stmt.close();
		} catch (SQLException sqlExcept) {
			sqlExcept.printStackTrace();
			System.exit(1);
		}
	}	
	
	

	public static void select50HedgeFundsHoldings() {
		try {
			stmt = conn.createStatement();
			stmt.setMaxRows(50);
			ResultSet results = stmt.executeQuery("select * from " + hedgeFundHoldings);
			ResultSetMetaData rsmd = results.getMetaData();
			int numberCols = rsmd.getColumnCount();
			for (int i = 1; i <= numberCols; i++) {
				// print Column Names
				System.out.print(rsmd.getColumnLabel(i) + "\t\t\t");
			}

			System.out
					.println("\n-------------------------------------------------");

			while (results.next()) {
				String cusip = results.getString(1);
				String cik = results.getString(2);
				double value = results.getDouble(3);
				double shares = results.getDouble(4);
				String quarterDir = results.getString(5);
				System.out.println(cusip + "\t\t" + cik+ "\t\t" + value + "\t\t" + shares + "\t\t\t" + quarterDir);
			}
			
			results.close();
			stmt.close();
		} catch (SQLException sqlExcept) {
			sqlExcept.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void select50CusipTicker() {
		try {
			stmt = conn.createStatement();
			stmt.setMaxRows(50);
			ResultSet results = stmt.executeQuery("select * from " + cusipTicker);
			ResultSetMetaData rsmd = results.getMetaData();
			int numberCols = rsmd.getColumnCount();
			for (int i = 1; i <= numberCols; i++) {
				// print Column Names
				System.out.print(rsmd.getColumnLabel(i) + "\t\t\t");
			}

			System.out
					.println("\n-------------------------------------------------");

			while (results.next()) {
				String cusip = results.getString(1);
				String tick = results.getString(2);
				String quarterDir = results.getString(5);
				System.out.println(cusip + "\t\t" + tick+ "\t\t\t" + quarterDir);
			}
			
			results.close();
			stmt.close();
		} catch (SQLException sqlExcept) {
			sqlExcept.printStackTrace();
			System.exit(1);
		}
	}

	public static void outputAllTables() {
		try {
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
		} catch (SQLException sqlExcept) {
			sqlExcept.printStackTrace();
		}
	}
	
	public static String getTickerFromCusip(String cusip, String quarter){
		try {
			if(!tableExists(cusipTicker))
				return null;
			stmt = conn.createStatement();
			ResultSet results = stmt.executeQuery("select Ticker from " + cusipTicker
					+" WHERE CUSIP = '" + cusip+"'"
					+" AND QUARTER = '" + quarter+"'");
					
			if (results.next()) 
				return results.getString(1);;
			
			
			return null;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
		return null;
		
	}
	
	//returns all CUSIPs from that quarter
	public static ArrayList<String> getCusips(String quarter){
		ArrayList<String> cusips = new ArrayList<String>();
		try {
			if(!tableExists(cusipTicker))
				return cusips;
			stmt = conn.createStatement();
			ResultSet results = stmt.executeQuery("select CUSIP from " + cusipTicker
					+" WHERE QUARTER = '" + quarter+"'");
					
			while (results.next()) 
				cusips.add(results.getString(1));
			
			
			return cusips;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
		return cusips;
	}
	
	public static Hashtable<String, Double> getNumSharesHeldByFunds(String quarter){
		Hashtable<String, Double> tickerToSharesHeld =  new Hashtable<String, Double>(); 
		
		try{
			if(!tableExists(cusipTicker))
				return tickerToSharesHeld;
			stmt = conn.createStatement();
			ResultSet results = stmt.executeQuery("select TICKER, SUM(SHARES) from " + cusipTicker + " AND " + hedgeFundHoldings 
					+" WHERE QUARTER = '" + quarter+"' AND " + cusipTicker + ".CUSIP = "+ hedgeFundHoldings+ ".CUSIP " +
					"Group By TICKER");
			String tick ="";
			double summedShares = 0;
			while(results.next()){
				tick = results.getString(0);
				summedShares = results.getDouble(1);
				tickerToSharesHeld.put(tick, summedShares);
			}
			
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
		
		
		return tickerToSharesHeld;
	}
}
