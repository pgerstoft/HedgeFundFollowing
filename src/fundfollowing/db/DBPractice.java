package fundfollowing.db;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;

import fundfollowing.CIK;


public class DBPractice {

	public static void main(String[] args) {
		CIK x = new CIK("0000866780");
		Hashtable<CIK, Integer> x1 = new Hashtable<CIK, Integer>();
		x1.put(x, 1);
		CIK y = new CIK("0000866780");
		System.out.println(x1.get(y));
	}

}
