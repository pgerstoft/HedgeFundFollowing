package fundfollowing;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.TreeSet;

import fundfollowing.analysis.DataAnalysis;

public class Script {

	private static String cusipTicker = "CUSIPTICKER";
	private static String hedgeFundHoldings = "HEDGEFUNDHOLDINGS";

	public static void main(String[] args) {		

		Quarter startQuarter = new Quarter(2002, 1, true);
		Quarter endQuarter = new Quarter(2011,1, true);
		DataAnalysis x = new DataAnalysis();

		
//		Quarter q = startQuarter;
//		while(q.compareTo(endQuarter) < 0){
//		
//			x.makeDesignMatrixCSV(q, 4);
//			q = q.getNextQuarter();
//		}
		
	try {
		x.compareClassifiers(startQuarter, endQuarter, 4);
	} catch (Exception e) {
		e.printStackTrace();
	}
		
	}


	//
	// for every quarter
	// for every cusip
	// get CIK holding it
	//			
}

// 1.Download AND parse SEC DATA
// 2.Download Financial Data: CSHOQ, PRICE
// 3.Update Relevant Portions of Database

