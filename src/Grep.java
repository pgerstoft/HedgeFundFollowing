/*
 * @(#)Grep.java	1.3 01/12/13
 * Search a list of files for lines that match a given regular-expression
 * pattern.  Demonstrates NIO mapped byte buffers, charsets, and regular
 * expressions.
 *
 * Copyright 2001-2002 Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or 
 * without modification, are permitted provided that the following 
 * conditions are met:
 * 
 * -Redistributions of source code must retain the above copyright  
 * notice, this  list of conditions and the following disclaimer.
 * 
 * -Redistribution in binary form must reproduct the above copyright 
 * notice, this list of conditions and the following disclaimer in 
 * the documentation and/or other materials provided with the 
 * distribution.
 * 
 * Neither the name of Oracle and/or its affiliates. or the names of 
 * contributors may be used to endorse or promote products derived 
 * from this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any 
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND 
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY 
 * EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY 
 * DAMAGES OR LIABILITIES  SUFFERED BY LICENSEE AS A RESULT OF  OR 
 * RELATING TO USE, MODIFICATION OR DISTRIBUTION OF THE SOFTWARE OR 
 * ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE 
 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, 
 * SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER 
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF 
 * THE USE OF OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that Software is not designed, licensed or 
 * intended for use in the design, construction, operation or 
 * maintenance of any nuclear facility. 
 */

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.ArrayList;
import java.util.regex.*;

public class Grep {

	// Charset and decoder for ISO-8859-15
	private static Charset charset = Charset.forName("ISO-8859-15");
	private static CharsetDecoder decoder = charset.newDecoder();

	// Pattern used to parse lines
	private static Pattern linePattern = Pattern.compile(".*\r?\n");

	// The input pattern that we're looking for
	private static Pattern pattern;

	// Compile the pattern from the command line
	//
	private static void compile(String pat) {
		try {
			pattern = Pattern.compile(pat);
		} catch (PatternSyntaxException x) {
			System.err.println(x.getMessage());
			System.exit(1);
		}
	}

	// Use the linePattern to break the given CharBuffer into lines, applying
	// the input pattern to each line to see if we have a match
	//
	private static ArrayList<String> grep(CharBuffer cb, File writeTo)
			throws IOException {
		ArrayList<String> foundInstances = null;
		boolean writeOut;
		FileWriter fstream;

		BufferedWriter out = null;
		if (writeTo == null) {
			writeOut = false;
			foundInstances = new ArrayList<String>();
		} else {
			writeOut = true;
			// open file for writing
			fstream = new FileWriter(writeTo.getPath());
			out = new BufferedWriter(fstream);
			System.out.println("System written");
		}

		Matcher lm = linePattern.matcher(cb); // Line matcher
		Matcher pm = null; // Pattern matcher
		int lines = 0;
		while (lm.find()) {
			lines++;
			CharSequence cs = lm.group(); // The current line
			if (pm == null)
				pm = pattern.matcher(cs);
			else
				pm.reset(cs);
			if (pm.find()) {
				if (writeOut)
					out.write(cs + "");
				else
					foundInstances.add(cs + "");
			}

			if (lm.end() == cb.limit())
				break;
		}

		if (writeOut)
			out.close();

		return foundInstances;

		// close file

	}

	// Search for occurrences of the input pattern in the given file
	//
	public static ArrayList<String> grep(File f, File writeTo, String pattern)
			throws IOException {

		compile(pattern);

		// Open the file and then get a channel from the stream
		FileInputStream fis = new FileInputStream(f);
		FileChannel fc = fis.getChannel();

		// Get the file's size and then map it into memory
		int sz = (int) fc.size();
		MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, sz);

		// Decode the file into a char buffer
		CharBuffer cb = decoder.decode(bb);

		// Perform the search
		ArrayList<String> foundInstances = grep(cb, writeTo);

		// Close the channel and the stream
		fc.close();

		return foundInstances;
	}
	
	public static ArrayList<String> grep(String str, File writeTo, String pattern) throws IOException{
		compile(pattern);
		CharBuffer cb = CharBuffer.allocate(str.length());
		cb.put(str);
		System.out.println(cb.toString());
		ArrayList<String> foundInstances = grep(cb, writeTo);
		return foundInstances;
	}
	

}