import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * 
 * @file IOHelper.java
 * 
 * This class is an I/O helper class.
 * The main responsibilities are saving log files and images to the disk.
 * 
 * @author YanningMao <yanningm@andrew.cmu.edu>
 * @date April 26, 2018
 *
 */
public class IOHelper {


	/* -------------------------------------------------------------------- */
	/* --------------------------   Constants   --------------------------- */
	/* -------------------------------------------------------------------- */
	
	private static final String COMMA = ",";
	private static final String DOT = ".";
	private static final String COLON = ":";
	private static final String SLASH = "/";
	private static final String LOG_DIR = "log";
	private static final String LOG_FILE_PREFIX = "log_";
	private static final String TXT_FILE_SUFFIX = "txt";
	private static final String FILE_NAME_STR = "File Name";
	private static final String SOURCES_STR = "Sources";
			
	/* -------------------------------------------------------------------- */
	/* ---------------------------   Methods   ---------------------------- */
	/* -------------------------------------------------------------------- */
	
	
	/**
	 * Generate the path to the log file given the name of the commit file.
	 * 
	 * @param commitFName - commit file name
	 * @return path - path to the log file
	 */
	public static String getServerLogFilePath(String commitFName) {
		return LOG_DIR + SLASH + getServerLogFileName(commitFName);
	}
	
	/**
	 * Generates the full name of the log file given the commit file name.
	 * 
	 * @param commitFName - commit file name
	 * @return logFileName - log file name
	 */
	public static String getServerLogFileName(String commitFName) {
		return LOG_FILE_PREFIX + commitFName.split("\\" + DOT)[0] + DOT + TXT_FILE_SUFFIX;
	}
	
	/**
	 * Logs the commit information to the disk.
	 * The commit information includes the commit file name, the list of sources, and the image
	 * to be committed.
	 * 
	 * @param commitInfo - the commit information
	 */
	public static synchronized void logCommitInfo(CommitInfo commitInfo) {
				
		String fileName = commitInfo.getFileName();
		
		// log file name
		IOHelper.logPrintln(commitInfo, FILE_NAME_STR + COLON + fileName);
		
		// log the sources
		logPrint(commitInfo, SOURCES_STR + COLON);
		for (int i = 0; i < commitInfo.getNumSources(); i ++) {
			if (i != 0) {
				logPrint(commitInfo, COMMA);
			}
			logPrint(commitInfo, commitInfo.getSource(i));
		}
		logPrintln(commitInfo, "");
		
	}	
	
	/**
	 * Logs the given string for the specified commit.
	 * 
	 * @param commitInfo - the commit information
	 * @param str - the string to be logged
	 */
	public static synchronized void logPrint(CommitInfo commitInfo, String str) {
				
		String filePath = getServerLogFilePath(commitInfo.getFileName());
		logPrint(filePath, str);
		
	}
	
	/**
	 * Logs the given string to the specified file path.
	 * 
	 * @param filePath - the file path
	 * @param str - the string to be logged
	 */
	public static synchronized void logPrint(String filePath, String str) {

		try {
			PrintWriter wr = new PrintWriter(new FileWriter(filePath, true));
			wr.print(str);
			wr.close();
		} catch (Exception e) {
			System.err.println("Cannot open commit log file : " + filePath);
			e.printStackTrace();
			return;
		}
	}
	
	/**
	 * Logs the given string for the specified commit, with a new line appended.
	 * 
	 * @param commitInfo - the commit information
	 * @param str - the string to be logged
	 */
	public static synchronized void logPrintln(CommitInfo commitInfo, String str) {
		
		String filePath = getServerLogFilePath(commitInfo.getFileName());
		logPrintln(filePath, str);
	}
	
	/**
	 * Logs the given string to the specified file path, with a new line appended.
	 * 
	 * @param filePath - the file path
	 * @param str - the string to be logged
	 */
	public static synchronized void logPrintln(String filePath, String str) {
		
		try {
			PrintWriter wr = new PrintWriter(new FileWriter(filePath, true));
			wr.println(str);
			wr.close();
		} catch (Exception e) {
			System.err.println("Cannot open commit log file : " + filePath);
			e.printStackTrace();
			return;
		}
		
	}
	
	/**
	 * Commit and save the image to working directory.
	 * 
	 * @param fileName - commit file name
	 * @param imgBytes - byte array of the image to be committed
	 */
	public static synchronized void commitImage(String fileName, byte[] imgBytes) {
		
		try (FileOutputStream fos = new FileOutputStream(fileName)) {
			fos.write(imgBytes);
		} catch (Exception e) {
			System.err.println("Error when committing image " + fileName + " to working directory.");
			System.err.println("Error : " + e.getMessage());
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Deletes the image from the disk.
	 * 
	 * @param commitFName - the name of the commit image
	 */
	public static synchronized void deleteLogImage(String commitFName) {
		
		String filePath = LOG_DIR + SLASH + commitFName;
		
		File loggedImg = new File(filePath);
		if (loggedImg.exists()) {
			loggedImg.delete();
		}
	}
	
	/**
	 * Deletes the log file of the specified commit.
	 * 
	 * @param commitFName - the name of the commit image
	 */
	public static synchronized void deleteLogFile(String commitFName) {
		String filePath = getServerLogFilePath(commitFName);
		File logFile = new File(filePath);
		// delete the file if it exists
		if (logFile.exists()) {
			logFile.delete();
		}
	}

}



