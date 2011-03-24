import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;

public class DB {

	private static String dbURL = "jdbc:derby:myDB;create=true";
	private static String tableName = "restaurants";
	
	private static String hedgeFund = "HEDGEFUND";
	private static String hedgeFundFields = "CIK char(10), " +
			"FundName char(50), " +
			"PRIMARY KEY(CIK), " +
			"UNIQUE(FundName)";
	
	private static String hegdeFundHoldings = "HEDGEFUNDHOLDINGS";
	private static String hedgeFundHoldingsFields = "CUSIP char(9), " +
			"CIK char(10), " +
			"VALUE float, " +
			"SHARES float, " +
			"QUARTER char(10), " +
			"PRIMARY KEY(CUSIP), " +
			"UNIQUE(CIK,CUSIP, quarter), " +
			"FOREIGN KEY(CIK) REFERENCES HEDGEFUND(CIK)";
		
	private static Hashtable<String, String> tableNameToFields = new Hashtable<String, String>();
	
	
	static{
		tableNameToFields.put(hedgeFund,hedgeFundFields);
		tableNameToFields.put(hegdeFundHoldings, hedgeFundHoldingsFields);
	}
	
	// jdbc Connection
	private static Connection conn = null;
	private static Statement stmt = null;


	public DB(){
	}
	
	public void createConnection() {
		try {
			Class.forName("org.apache.derby.jdbc.ClientDriver").newInstance();
			// Get a connection
			conn = DriverManager.getConnection(dbURL);
		} catch (Exception except) {
			except.printStackTrace();
			System.exit(1);
		}
	}

	public void shutdown() {
		try {
			if (stmt != null) {
				stmt.close();
			}
			if (conn != null) {
				DriverManager.getConnection(dbURL + ";shutdown=true");
				conn.close();
			}
		} catch (SQLException sqlExcept) {
			//sqlExcept.printStackTrace();
			//System.exit(1);
		}
	}

	private static void createTable(String table) {
		try {
			stmt = conn.createStatement();
			System.out.println(new String("CREATE TABLE " + table+ "("+ tableNameToFields.get(table) +")").substring(140));
			stmt.executeUpdate("CREATE TABLE " + table+ "("+ tableNameToFields.get(table) +")");
		} catch (SQLException sqlExcept) {
			sqlExcept.printStackTrace();
			System.exit(1);
		}
	}

	private void outputAllTables() {
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

	private static boolean tableExists(String table) {
		try {
			DatabaseMetaData dbm = conn.getMetaData();
			ResultSet rs = dbm.getTables(null, null, table.toUpperCase(), null);
			if (!rs.next()) {
				return false;
			} else {
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
			if(!tableExists(hegdeFundHoldings))
				createTable(hegdeFundHoldings);
			
			stmt = conn.createStatement();
			stmt.execute("INSERT INTO " + hegdeFundHoldings + " values ('" + cusip + "','"
					+ cik + "'," + value + "," + shares+",'" + quarter +"' )");
			stmt.close();
		} catch (SQLException sqlExcept) {
			if(sqlExcept.getSQLState().equals("23505"))//Found duplicate from database view
				 return;//ignore duplicate and continue with the insert statement
			sqlExcept.printStackTrace();
			System.exit(1);
		}
	}
	

	private static void select50HedgeFunds() {
		try {
			stmt = conn.createStatement();
			ResultSet results = stmt.executeQuery("select * from " + hedgeFund + " LIMIT 50");
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
	
	private static void select50HedgeFundsHoldings() {
		try {
			stmt = conn.createStatement();
			ResultSet results = stmt.executeQuery("select * from " + hedgeFund +" LIMIT 50");
			ResultSetMetaData rsmd = results.getMetaData();
			int numberCols = rsmd.getColumnCount();
			for (int i = 1; i <= numberCols; i++) {
				// print Column Names
				System.out.print(rsmd.getColumnLabel(i) + "\t\t");
			}

			System.out
					.println("\n-------------------------------------------------");

			while (results.next()) {
				String cusip = results.getString(1);
				String cik = results.getString(2);
				int value = results.getInt(3);
				int shares = results.getInt(4);
				String quarterDir = results.getString(5);
				System.out.println(cusip + "\t\t" + cik+ "\t\t" + value + "\t\t" + shares + "\t\t" + quarterDir);
			}
			
			results.close();
			stmt.close();
		} catch (SQLException sqlExcept) {
			sqlExcept.printStackTrace();
			System.exit(1);
		}
	}


}
