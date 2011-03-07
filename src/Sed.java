 import java.io.BufferedReader;
 import java.io.BufferedWriter;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.io.InputStreamReader;
 
 public class Sed {
     String _searchPattern = "";
     String _replacementPattern = "";
 
     protected void readReplace(String filenameFrom, String filenameTo, String searchPattern,String replacementPattern) throws IOException {
         String line;
         StringBuffer buffer = new StringBuffer();
         FileInputStream fileInputStream = new FileInputStream(filenameFrom);
         BufferedReader reader = new BufferedReader(
                 new InputStreamReader(fileInputStream));
         while((line = reader.readLine()) != null) {
             String newline = line.replaceAll(searchPattern, replacementPattern);
             /*
             if (!newline.equals(line)) {
                 System.out.println("Found pattern in " + filename
                         + ". New line: " + newline);
             }
             */
             buffer.append(newline + "\n");
         }
         reader.close();
         BufferedWriter out;
         if(filenameTo == null)
        	 out = new BufferedWriter(new FileWriter(filenameFrom));
         else
        	 out = new BufferedWriter(new FileWriter(filenameTo));
         out.write(buffer.toString());
         out.close();
     }
 
     protected void browseDirectoryTree(String path)
             throws IOException {
         File file = new File(path);
 
         if (!file.exists()) return;
         if (!file.isDirectory()) return;
 
         //loop through files in directory
         String[] files = file.list();
 
         for (int k = 0; k < files.length; k++) {
             File newfile = new File(file.getPath(), files[k]);
             if (newfile.isFile())
                 readReplace(path + System.getProperty("file.separator") + files[k],  null, _searchPattern, _replacementPattern);
             else if (newfile.isDirectory()) {
                 browseDirectoryTree(file.getPath()
                         + System.getProperty("file.separator")
                         + files[k]);
             }
         }
     }
 
     public void sed(String path, String fileTo, String searchPattern, String replacementPattern) throws IOException {
         File file = new File(path);
         
         _searchPattern = searchPattern;
         _replacementPattern = replacementPattern;
         if (!file.exists()) return;
         if (fileTo != null){
        	 File fileToo = new File(fileTo);
        	 if(!fileToo.exists())
        		 fileToo.createNewFile();
         }
         if (file.isFile()) {
             readReplace(path, fileTo, searchPattern, replacementPattern);
         }
         else if (file.isDirectory()) {
             browseDirectoryTree(path);
         }
     }
 
//     public static void main(String args[]) {
//         if (args.length == 3) {
//             try {
//                 new Sed().sed(args[0], args[1], args[2]);
//             }
//             catch (IOException e) {
//                 System.out.println("Sed: An error occured:");
//                 System.out.println(e.getMessage());
//             }
//         }
//         else {
//             System.out.println("usage: java Sed directory|file "
//                     + "searchPattern replacementPattern");
//         }
//     }
// 
 }