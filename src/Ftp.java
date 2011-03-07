import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;


public class Ftp {

	 public static final String USAGE =
	        "Usage: ftp [-s] [-b] [-l] [-k secs [-w msec]] [-#] <hostname> <username> <password> <remote file> <local file>\n" +
	        "\nDefault behavior is to download a file and use ASCII transfer mode.\n" +
	        "\t-s store file on server (upload)\n" +
	        "\t-l list files (local file is ignored)\n" +
	        "\t-# add hash display during transfers\n" +
	        "\t-k secs use keep-alive timer (setControlKeepAliveTimeout)\n" +
	        "\t-w msec wait time for keep-alive reply (setControlKeepAliveReplyTimeout)\n" +
	        "\t-b use binary transfer mode\n";
		
	    public static final String USAGE_MULT =
	        "Usage: ftp [-s] [-b] [-l] [-k secs [-w msec]] [-#] <hostname> <username> <password> <remote dir> <local dir> <remote files> \n" +
	        "\nDefault behavior is to download a file and use ASCII transfer mode.\n" +
	        "\t-s store file on server (upload)\n" +
	        "\t-l list files (local file is ignored)\n" +
	        "\t-# add hash display during transfers\n" +
	        "\t-k secs use keep-alive timer (setControlKeepAliveTimeout)\n" +
	        "\t-w msec wait time for keep-alive reply (setControlKeepAliveReplyTimeout)\n" +
	        "\t-b use binary transfer mode\n";
	    
	    public static void ftpMultipleFiles(String[] args, ArrayList<String> filesFrom, ArrayList<String> filesTo){

			int base = 0;
	        boolean storeFile = false, binaryTransfer = false, error = false, listFiles = false;
	        String server, username, password, remote, local;
	        final FTPClient ftp = new FTPClient();

	        for (base = 0; base < args.length; base++)
	        {
	            if (args[base].startsWith("-s")) {
	                storeFile = true;
	            }
	            else if (args[base].startsWith("-b")) {
	                binaryTransfer = true;
	            }
	            else if (args[base].equals("-l")) {
	                listFiles = true;
	            }
//	            else if (args[base].equals("-#")) {
//	                ftp.setCopyStreamListener(createListener());
//	            }
//	            else if (args[base].equals("-k")) {
//	                ftp.setControlKeepAliveTimeout(Long.parseLong(args[++base]));
//	            } 
//	            else if (args[base].equals("-w")) {
//	                ftp.setControlKeepAliveReplyTimeout(Integer.parseInt(args[++base]));
//	            } 
	            else {
	                break;
	            }
	        }

	        if ((args.length - base) < 5) // server, user, pass, remote dir, local dir, remote files...
	        {
	        	System.err.println("Number of arguments:" + args.length);
	        	printStatementError(USAGE_MULT);
	        }

	        if(filesFrom.size() != filesTo.size())
	        	printStatementError("Input ArrayLists must of the same type");
	        
	        
	        server = args[base++];
	        int port = 21;
	        String parts[] = server.split(":");
	        if (parts.length == 2){
	            server=parts[0];
	            port=Integer.parseInt(parts[1]);
	        }
	        username = args[base++];
	        password = args[base++];
	        remote = args[base++];
	        local = args[base];

	        if(local.charAt(local.length()-1) != '/' || remote.charAt(remote.length()-1) != '/')
	        	printStatementError("Need to end directories with /"+ local + " " + remote);
	        
	        
	        ftp.addProtocolCommandListener(new PrintCommandListener(
	                                           new PrintWriter(System.out)));

	        try
	        {
	            int reply;
	            ftp.connect(server, port);
	            System.out.println("Connected to " + server + ".");

	            // After connection attempt, you should check the reply code to verify
	            // success.
	            reply = ftp.getReplyCode();

	            if (!FTPReply.isPositiveCompletion(reply))
	            {
	                ftp.disconnect();
	                printStatementError("FTP server refused connection.");
	            }
	        }
	        catch (IOException e)
	        {
	            if (ftp.isConnected())
	            {
	                try
	                {
	                    ftp.disconnect();
	                }
	                catch (IOException f)
	                {
	                    // do nothing
	                }
	            }
	            e.printStackTrace();
	            printStatementError("Could not connect to server.");
	        }

	__main:
	        try
	        {
	            if (!ftp.login(username, password))
	            {
	                ftp.logout();
	                error = true;
	                break __main;
	            }

	            System.out.println("Remote system is " + ftp.getSystemType());

	            if (binaryTransfer)
	                ftp.setFileType(FTP.BINARY_FILE_TYPE);

	            // Use passive mode as default because most of us are
	            // behind firewalls these days.
	            ftp.enterLocalPassiveMode();

	            if (storeFile)
	            {
	                InputStream input;

	                input = new FileInputStream(local);

	                ftp.storeFile(remote, input);

	                input.close();
	            }
	            else if (listFiles)
	            {
	                for (FTPFile f : ftp.listFiles(remote)) {
	                    System.out.println(f);
	                }
	                    
	            }
	            else
	            {
	                OutputStream output = null;
	                String remoteFile;
	                
	                for(int i = 0; i < filesFrom.size(); i++){
	                	output = new FileOutputStream(local + filesTo.get(i)); 
	                	remoteFile = remote + filesFrom.get(i);
	                	System.out.println(i + "/" + filesFrom.size());
	                	System.out.println(local + filesTo.get(i));
	                	ftp.retrieveFile(remoteFile, output);
	                	output.close();
	                }
	                
	            }

	            ftp.syst();
	            ftp.feat();
	            ftp.logout();
	        }
	        catch (FTPConnectionClosedException e)
	        {
	            error = true;
	            System.err.println("Server closed connection.");
	            e.printStackTrace();
	        }
	        catch (IOException e)
	        {
	            error = true;
	            e.printStackTrace();
	        }
	        finally
	        {
	            if (ftp.isConnected())
	            {
	                try
	                {
	                    ftp.disconnect();
	                }
	                catch (IOException f)
	                {
	                    // do nothing
	                }
	            }
	        }
		}
	    
		public static void ftpSingleFile(String[] args){
			int base = 0;
	        boolean storeFile = false, binaryTransfer = false, error = false, listFiles = false;
	        String server, username, password, remote, local;
	        final FTPClient ftp = new FTPClient();

	        for (base = 0; base < args.length; base++)
	        {
	            if (args[base].startsWith("-s")) {
	                storeFile = true;
	            }
	            else if (args[base].startsWith("-b")) {
	                binaryTransfer = true;
	            }
	            else if (args[base].equals("-l")) {
	                listFiles = true;
	            }
//	            else if (args[base].equals("-#")) {
//	                ftp.setCopyStreamListener(createListener());
//	            }
//	            else if (args[base].equals("-k")) {
//	                ftp.setControlKeepAliveTimeout(Long.parseLong(args[++base]));
//	            } 
//	            else if (args[base].equals("-w")) {
//	                ftp.setControlKeepAliveReplyTimeout(Integer.parseInt(args[++base]));
//	            } 
	            else {
	                break;
	            }
	        }

	        if ((args.length - base) != 5) // server, user, pass, remote, local
	        {
	        	System.err.println("Number of arguments:" + args.length);
	        	printStatementError(USAGE);
	        }

	        server = args[base++];
	        int port = 21;
	        String parts[] = server.split(":");
	        if (parts.length == 2){
	            server=parts[0];
	            port=Integer.parseInt(parts[1]);
	        }
	        username = args[base++];
	        password = args[base++];
	        remote = args[base++];
	        local = args[base];

	        ftp.addProtocolCommandListener(new PrintCommandListener(
	                                           new PrintWriter(System.out)));

	        try
	        {
	            int reply;
	            ftp.connect(server, port);
	            System.out.println("Connected to " + server + ".");

	            // After connection attempt, you should check the reply code to verify
	            // success.
	            reply = ftp.getReplyCode();

	            if (!FTPReply.isPositiveCompletion(reply))
	            {
	                ftp.disconnect();
	                printStatementError("FTP server refused connection.");
	            }
	        }
	        catch (IOException e)
	        {
	            if (ftp.isConnected())
	            {
	                try
	                {
	                    ftp.disconnect();
	                }
	                catch (IOException f)
	                {
	                    // do nothing
	                }
	            }
	            e.printStackTrace();
	            printStatementError("Could not connect to server.");
	        }

	__main:
	        try
	        {
	            if (!ftp.login(username, password))
	            {
	                ftp.logout();
	                error = true;
	                break __main;
	            }

	            System.out.println("Remote system is " + ftp.getSystemType());

	            if (binaryTransfer)
	                ftp.setFileType(FTP.BINARY_FILE_TYPE);

	            // Use passive mode as default because most of us are
	            // behind firewalls these days.
	            ftp.enterLocalPassiveMode();

	            if (storeFile)
	            {
	                InputStream input;

	                input = new FileInputStream(local);

	                ftp.storeFile(remote, input);

	                input.close();
	            }
	            else if (listFiles)
	            {
	                for (FTPFile f : ftp.listFiles(remote)) {
	                    System.out.println(f);
	                }
	                    
	            }
	            else
	            {
	                OutputStream output;

	                output = new FileOutputStream(local);
	                ftp.retrieveFile(remote, output);

	                output.close();
	            }

	            ftp.syst();
	            ftp.feat();
	            ftp.logout();
	        }
	        catch (FTPConnectionClosedException e)
	        {
	            error = true;
	            System.err.println("Server closed connection.");
	            e.printStackTrace();
	        }
	        catch (IOException e)
	        {
	            error = true;
	            e.printStackTrace();
	        }
	        finally
	        {
	            if (ftp.isConnected())
	            {
	                try
	                {
	                    ftp.disconnect();
	                }
	                catch (IOException f)
	                {
	                    // do nothing
	                }
	            }
	        }
		}
		
		private static void printStatementError(String statement) {
			System.err.println(statement);
			System.exit(1);
		}
		
}
