import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * Server.java
 * 
 * This class represents a Server in the Two Phase Commit.
 * 
 * The server starts a commit, sends commit queries to the User Nodes, collects their
 * commit agreement messages, then distributes the commit decision, and collects all the
 * ACKs from the User Nodes.
 * 
 * The server has a time out mechanism that controls the time it takes for a User Node to
 * reply. If a User Node times out to reply, the Server either send commit abort if the
 * time out happened during Phase I, or re-send the commit decision to the User Node
 * if the time out happened during Phase II.
 * 
 * The server also has a self-recovery mechanism. After the Server restarts, it restores
 * the unfinished commits. It either sends commit abort if the failure happened during
 * Phase I, or re-send the commit decision to the User Node if the failure happened
 * during Phase II.
 * 
 * 
 * @author YanningMao <yanningm@andrew.cmu.edu>
 * @date April 18, 2018
 *
 */
public class Server implements ProjectLib.CommitServing {
	

	/* -------------------------------------------------------------------- */
	/* --------------------------   Constants   --------------------------- */
	/* -------------------------------------------------------------------- */
	
	private static final int NUM_CMD_ARGUMENTS = 1;
	private static final String CMD_LINE_FORMAT = "<port>";

	// the time to wait between checking if recover has ended 
	private static final long RECOVER_MILLIS = 50;
	private static final String COLON = ":";
	private static final String COMMA = ",";
	private static final String LOG_DIR = "log";
	private static final String TXT_FILE_SUFFIX = ".txt";
	private static final String FILE_NAME_STR = "File Name";
	private static final String SOURCES_STR = "Sources";
	private static final String PHASE_ONE_STR = "Phase One";
	private static final String PHASE_TWO_STR = "Phase Two";
	private static final String DONE_STR = "DONE";
	

	/* -------------------------------------------------------------------- */
	/* -----------------------   Class Variables   ------------------------ */
	/* -------------------------------------------------------------------- */
	
	private static int port;
	private static AtomicBoolean recoveryFinished = new AtomicBoolean(false);
	
	private static Server server;
	// the message handler to receive messages
	private static ProjectLib.MessageHandling msgHandler;
	// the ProjectLib object
	private static ProjectLib PL;
	
	// stores information about each commit
	private static ConcurrentMap<String, CommitInfo> commitRecords = new ConcurrentHashMap<>();
	// stores runnable thread for each commit
	private static ConcurrentMap<String, Thread> commitThreads = new ConcurrentHashMap<>();
	private static ConcurrentMap<String, CommitProcess> commitProcesses = new ConcurrentHashMap<>();


	/* -------------------------------------------------------------------- */
	/* -------------------------   Constructor   -------------------------- */
	/* -------------------------------------------------------------------- */
	
	
	public Server() {
		super();
	}

	
	/* -------------------------------------------------------------------- */
	/* -----------------------------   Main   ----------------------------- */
	/* -------------------------------------------------------------------- */
	
	
	public static void main(String[] args) {

		// read command-line arguments
		if (args.length != NUM_CMD_ARGUMENTS) {
			System.err.println("Please enter correct command line argument : "
									+ CMD_LINE_FORMAT);
		}
		
		port = Integer.parseInt(args[0]);
		server = new Server();
		msgHandler = new ServerMessageReceiver();
		
		// create ProjectLib object
		PL = new ProjectLib(port, server, msgHandler);
		
		// create log directory
		File logDir = new File(LOG_DIR);
		if (!logDir.exists()) {
			logDir.mkdir();
			PL.fsync();
		}
		
		// recover from failure
		try {
			recoverFromLog();
			recoveryFinished.set(true);
		} catch (Exception e) {
			System.err.println("Error when recovering from log files : " + e.getMessage());
			e.printStackTrace();
		}
		
		// continuously receive message
		while (true) {
			ProjectLib.Message msg = PL.getMessage();
			processMessage(msg);
		}

	}
	
	
	/* -------------------------------------------------------------------- */
	/* ---------------------------   Helpers   ---------------------------- */
	/* -------------------------------------------------------------------- */
	
	
	/**
	 * Processes the messaged missed by the MessageHandler.
	 * This method currently does nothing.
	 * 
	 * @param msg - message from user
	 */
	private static void processMessage(ProjectLib.Message msg) {
		// dummy method and does nothing
		return;
	}
	
	/**
	 * Gets the extension of the file.
	 * 
	 * @param fileName - the file name
	 * @return extension - the file extension
	 */
	private static String getFileExtension(String fileName) {
		if (fileName.contains("\\.")) {
			return null;
		}
		return fileName.split("\\.")[1];
	}
	
	/**
	 * Reads the image file and converts into byte array.
	 * 
	 * @param fileName - the file name
	 * @return imgBytes - the byte array representing the image
	 * @throws Exception - exception when reading
	 */
	public static byte[] readImage(String fileName) throws Exception {
		
		BufferedImage bufferedImg = ImageIO.read(new File(fileName));
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		
		ImageIO.write(bufferedImg, getFileExtension(fileName), bos);
		byte[] imgBytes = bos.toByteArray();
		
		return imgBytes;
	}
	
	/**
	 * Restores the Server environment from last failure.
	 * 
	 * This recovery process reads the commit information of each unfinished commit,
	 * and finds where each commit terminated last time. It then restarts each unfinished
	 * commit process.
	 * 
	 * @throws Exception
	 */
	private static void recoverFromLog() throws Exception {
	
		// the log directory
		File logFolder = new File(LOG_DIR);
		File[] logFiles = logFolder.listFiles();
		
		// nothing to recover
		if (logFiles == null || logFiles.length == 0) {
			return;
		}
		
		// store all recovered commits and wait for finish
		List<Thread> recoverCommits = new ArrayList<>();
		
		// recover each log
		for (File logFile : logFiles) {
			
			// ignore non-TXT files
			if ((!logFile.isFile()) || (!logFile.getName().endsWith(TXT_FILE_SUFFIX))) {
				continue;
			}
			
			// read the content
			Scanner sc;
			try {
				sc = new Scanner(logFile);
			} catch (FileNotFoundException e) {
				// ignore the exception
				e.printStackTrace();
				continue;
			}
			
			// prepare for recovery
			String fileName = null;
			String[] sources = null;
			
			boolean reachedPhaseOne = false;
			boolean reachedPhaseTwo = false;
			boolean isDone = false;
			
			CommitDecision commitDecision = null;
			CommitInfo commitInfo = null;
			
			// reconstruct the environment based on log
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				
				if (line.startsWith(FILE_NAME_STR)) {
					fileName = line.split(COLON)[1];
				}
				else if (line.startsWith(SOURCES_STR)) {
					int fstColonIdx = line.indexOf(COLON);
					sources = line.substring(fstColonIdx + 1).split(COMMA);
				}
				else if (line.startsWith(PHASE_ONE_STR)) {
					reachedPhaseOne = true;
				}
				else if (line.startsWith(PHASE_TWO_STR)) {
					reachedPhaseTwo = true;
					commitDecision = CommitDecision.valueOf(line.split(COLON)[1]);
				}
				else if (line.startsWith(DONE_STR)) {
					isDone = true;
				}
				else {
					continue;
				}
			}
			sc.close();
			
			
			// restart the commit based on log
			if (isDone) {
				continue;
			}
			// restore commit information
			else {
				commitInfo = new CommitInfo(fileName, sources);
			}
			
			// if already started Phase II, recover the commit
			if (reachedPhaseTwo) {

				commitRecords.put(fileName, commitInfo);
				
				// according to decision, restart the commit
				CommitProcess commitProcess = new PhaseTwoRecover(PL, commitInfo, commitDecision);
				Thread commitThread = new Thread(commitProcess);
				
				recoverCommits.add(commitThread);
				commitProcesses.put(fileName, commitProcess);
				commitThreads.put(fileName, commitThread);
				// commitThread.start();
				
			}
			
			// if stopped during Phase I, abort the commit
			else if (reachedPhaseOne) {
				
				commitRecords.put(fileName, commitInfo);
				
				// make sure the image was not saved
				File imgFile = new File(fileName);
				if (imgFile != null && imgFile.exists()) {
					imgFile.delete();
				}
				
				// abort the commit
				CommitProcess commitProcess = new PhaseOneAbort(PL, commitInfo);
				Thread commitThread = new Thread(commitProcess);
				
				recoverCommits.add(commitThread);
				commitProcesses.put(fileName, commitProcess);
				commitThreads.put(fileName, commitThread);
				// commitThread.start();
				
			}

		}
		
		// wait until all recovered commits finish
		for (Thread thread : recoverCommits) {
			thread.start();
		}
		for (Thread thread : recoverCommits) {
			thread.join();
		}
		
		// set the flag to recover finished
		recoveryFinished.set(true);
	}

	/**
	 * Server callback when new collage is to be committed.
	 * A call to this function should start a two-phase commit operation.
	 * 
	 * @param fname - name of candidate image
	 * @img - byte array of candidate image
	 * @sources - string array indicating the contributing files in "source_node:filename" format
	 */

	@Override
	public void startCommit(String fname, byte[] img, String[] sources) {
		
		// wait for recovery to finish
		while (!recoveryFinished.get()) {
			try {
				TimeUnit.MILLISECONDS.sleep(RECOVER_MILLIS);
			} catch (InterruptedException e) {
				// ignore
			}
		}
		
		// store commit information
		CommitInfo commitInfo = new CommitInfo(fname, sources);
		commitRecords.put(fname, commitInfo);
		
		// start a thread to process commit
		CommitProcess commitProcess = new FullCommitProcess(PL, commitInfo, img);
		Thread commitThread = new Thread(commitProcess);
		commitProcesses.put(fname, commitProcess);
		commitThreads.put(fname, commitThread);
		commitThread.start();
		
	}

	/* ----------------------------   Helper Class   ---------------------------- */
	
	/**
	 * A private helper class used by the Server as a message handler.
	 * Has a callback message that is called when a message is delivered to the Server.
	 * 
	 * @author YanningMao
	 *
	 */
	private static class ServerMessageReceiver implements ProjectLib.MessageHandling {

		@Override
		public synchronized boolean deliverMessage(ProjectLib.Message rcvMsg) {

			// get sender address and message content
			byte[] msgBytes = rcvMsg.body;
			
			// convert message content from bytes to object
			MessageContent msgContent = MessageConvert.unpackMessage(msgBytes);

			try {
				commitProcesses.get(msgContent.getFileName()).receiveMessage(msgContent);
				return true;
			} catch (InterruptedException e) {
				e.printStackTrace();
				return false;
			}
			
		}

	}

}




