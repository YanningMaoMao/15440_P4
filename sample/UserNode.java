import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @file UserNode.java
 * 
 * This file represents a User Node in the Two Phase Commit Process.
 * 
 * In Phase I, the User Node receives commit query message from the Server, and votes for whether
 * to commit.
 * 
 * In Phase II, the User Node receives commit decision message from the Server. The decision can
 * be a commit abort message, a commit confirm message, or a commit cancel message. The User Node
 * performs accordingly.
 * 
 * The User Node only listens to the command from the Server and replies to the command. It never
 * the initiative to send a message to the Server.
 * 
 * The User Node has a recover mechanism using log that records its local file status and restores
 * it after restart.
 * 
 * @author YanningMao <yanningm@andrew.cmu.edu>
 * @date April 18, 2018
 *
 */
public class UserNode implements ProjectLib.MessageHandling {
	
	/* -------------------------------------------------------------------- */
	/* --------------------------   Constants   --------------------------- */
	/* -------------------------------------------------------------------- */
	
	private static final int NUM_CMD_ARGUMENTS = 2;
	private static final String CMD_LINE_FORMAT = "<port><id>";
	
	// the time to wait between checking if recover has ended 
	private static final long RECOVER_MILLIS = 50;
	private static final String COLON = ":";
	private static final String SLASH = "/";
	private static final String LOG_DIR = "log";
	private static final String LOG_FNAME = "log.txt";
	private static final String LOG_FPATH = LOG_DIR + SLASH + LOG_FNAME;

	/* -------------------------------------------------------------------- */
	/* -----------------------   Class Variables   ------------------------ */
	/* -------------------------------------------------------------------- */
	
	private static int port;
	private static ProjectLib PL;
	
	// indicator for whether recovery has finished
	private static AtomicBoolean recoverFinished = new AtomicBoolean(false);
	
	// stores the source files waiting for the Server's decision for whether to commit
	private static ConcurrentMap<String, String> filesPrepared = new ConcurrentHashMap<>();

	/* -------------------------------------------------------------------- */
	/* ----------------------   Instance Variables   ---------------------- */
	/* -------------------------------------------------------------------- */
	
	private final String id;
	
	
	/* -------------------------------------------------------------------- */
	/* -------------------------   Constructor   -------------------------- */
	/* -------------------------------------------------------------------- */

	
	/**
	 * The constructor of the User Node. Takes in the ID assigned to this User Node.
	 * 
	 * @param id - the user ID
	 */
	public UserNode(String id) {
		this.id = id;
	}

	
	/* -------------------------------------------------------------------- */
	/* -----------------------------   Main   ----------------------------- */
	/* -------------------------------------------------------------------- */
	
	/**
	 * 
	 * Starts the UserNode so that it is ready to participate in the Two Phase Commit process.
	 * Before it starts, recovers the environment from its last failure if necessary.
	 * 
	 * @param args - Command line arguments. The first argument is the port used by the User
	 * 		 Node to communicate with ProjectLib. The second argument is the ID of the
	 * 		 User Node.
	 */
	public static void main(String[] args) {
		
		// read command-line arguments
		if (args.length != NUM_CMD_ARGUMENTS) {
			System.err.println("Please enter correct command line argument : " + CMD_LINE_FORMAT);
		}
		
		port = Integer.parseInt(args[0]);
		String userID = args[1];
		
		// construct ProjectLib object
		ProjectLib.MessageHandling node = new UserNode(userID);
		PL = new ProjectLib(port, userID, node);
		
		// create log directory
		File logDir = new File(LOG_DIR);
		if (!logDir.exists()) {
			logDir.mkdir();
			PL.fsync();
		}
		
		// recovery
		recoverFromLog();
		recoverFinished.set(true);

	}
	

	/* -------------------------------------------------------------------- */
	/* ----------------------------   Helpers   --------------------------- */
	/* -------------------------------------------------------------------- */

	
	/**
	 * Recovers from failure of the UserNode. Restores the running environment and the
	 * status of the UserNode. Specifically, reads the files on the UserNode that had
	 * been agreed to contribute to a commit but is still waiting.
	 * Each line in the log file contains a pair of the source file name and the commit
	 * file name, separated by a colon.
	 */
	private static void recoverFromLog() {

		// the log file
		File logFile = new File(LOG_FPATH);
		
		// nothing to recover
		if (!logFile.exists()) {
			return;
		}
		
		// map to keep track of log
		Map<String, Map<String, Integer>> recoverMap = new HashMap<>();
		
		// recover the status
		Scanner sc;
		try {
			sc = new Scanner(logFile);
		} catch (FileNotFoundException e) {
			return;
		}
		
		// track the addition to and removal from the prepared list of source files 
		while (sc.hasNextLine()) {
			
			String line = sc.nextLine().trim();
				
			// ignore empty line
			if (line.length() == 0) {
				continue;
			}
			else if (!line.contains(COLON)) {
				continue;
			}
				
			// extract source file name and commit file name
			String sourceFileName = line.split(COLON)[0];
			String commitFileName = line.split(COLON)[1];
			SourceFileStatus status = SourceFileStatus.valueOf(line.split(COLON)[2]);
			
			if (!recoverMap.containsKey(sourceFileName)) {
				recoverMap.put(sourceFileName, new HashMap<>());
			}
			if (!recoverMap.get(sourceFileName).containsKey(commitFileName)) {
				recoverMap.get(sourceFileName).put(commitFileName, 0);
			}
			
			int oldVal = recoverMap.get(sourceFileName).get(commitFileName);
			if (status.equals(SourceFileStatus.PREPARED)) {
				recoverMap.get(sourceFileName).put(commitFileName, oldVal + 1);
			}
			else {
				recoverMap.get(sourceFileName).put(commitFileName, oldVal - 1);
			}
		}
		
		// decide the prepared list of source files before restart
		for (String sourceFileName : recoverMap.keySet()) {
			
			// if the file does not exist or already committed
			File sourceFile = new File(sourceFileName);
			if (!sourceFile.exists()) {
				continue;
			}
			
			// check if the source file is prepared for any commit
			for (String commitFileName : recoverMap.get(sourceFileName).keySet()) {
				if (recoverMap.get(sourceFileName).get(commitFileName) > 0) {
					filesPrepared.put(sourceFileName, commitFileName);
					break;
				}
			}
		}
		
		sc.close();
	}
	
	/**
	 * Sends the commit agreement message to the server. If the user node approves to
	 * commit, it sends a message indicating approval; If the user refuses to
	 * commit, it sends a message indicating refuse.
	 * 
	 * @param addr - the address of the server of the commit query
	 * @param rcvMsg - the commit query received by the user node
	 */
	private synchronized void handleCommitQuery(String addr, MessageContent rcvMsg) {
		
		// get file name
		String commitFName = rcvMsg.getFileName();
		
		// ask the user for agreement
		boolean ok = PL.askUser(rcvMsg.getImg(), rcvMsg.getFiles());
		
		// check if any file is waiting for commit or already committed
		for (String sourceFName : rcvMsg.getFiles()) {
			File sourceFile = new File(sourceFName);
			
			// if the file does not exist or already committed
			if (!sourceFile.exists()) {
				ok = false;
				break;
			}
			
			// if the file is waiting for a commit decision
			else if (filesPrepared.containsKey(sourceFName)) {
				if (filesPrepared.get(sourceFName) != commitFName) {
					ok = false;
					break;
				}
			}
			
			// otherwise add the file as prepared
			else {
				String str = sourceFName + COLON + commitFName + COLON + SourceFileStatus.PREPARED;
				IOHelper.logPrintln(LOG_FPATH, str);
				PL.fsync();
				filesPrepared.put(sourceFName, commitFName);
			}
		}
		
		// update the files waiting for commit based on UserNode's decision
		if (ok) {
			
			// mark all source files as prepared
			for (String sourceFName : rcvMsg.getFiles()) {
				if (!filesPrepared.containsKey(sourceFName)) {
					
					// log the change
					String str = sourceFName + COLON + commitFName;
					str += COLON + SourceFileStatus.PREPARED;
					IOHelper.logPrintln(LOG_FPATH, str);
					PL.fsync();
					
					// add the file
					filesPrepared.put(sourceFName, commitFName);
				}
			}
		}
		
		else {
			// release all source files from prepared
			for (String sourceFName : rcvMsg.getFiles()) {
				
				if (filesPrepared.containsKey(sourceFName)
				&& filesPrepared.get(sourceFName).equals(commitFName)) {
					
					// log the change of the source file status
					String str = sourceFName + COLON + commitFName;
					str += COLON + SourceFileStatus.ABORTED;
					IOHelper.logPrintln(LOG_FPATH, str);
					PL.fsync();
					
					// remove the source file from prepared list
					filesPrepared.remove(sourceFName);
				}
			}
		}
		
		// build reply message
		MessageContent rplMsg = new MessageContent(commitFName, MessageType.COMMIT_AGREEMENT, id, addr);
		rplMsg.setAgreement(ok);
		
		// serialize the message
		byte[] msgBytes = MessageConvert.packMessage(rplMsg);
		
		// send the message
		ProjectLib.Message msg = new ProjectLib.Message(addr, msgBytes);
		PL.sendMessage(msg);
	}
	
	/**
	 * Sends the commit acknowledgement message to the server.
	 * 
	 * @param addr - the address of the sender of the commit message
	 * @param rcvMsg - the commit messaged received by the user node
	 */
	private synchronized void handleCommitMessage(String addr, MessageContent rcvMsg) {
		
		String commitFName = rcvMsg.getFileName();
		
		// remove local files, if commit approved
		if (rcvMsg.getAgreement()) {
			
			for (String fileName : rcvMsg.getFiles()) {
				// delete the file on the disk
				File file = new File(fileName);
				file.delete();
				PL.fsync();
				
				// remove from waiting
				if (filesPrepared.containsKey(fileName)) {
					// log the change
					String str = fileName + COLON + commitFName;
					str += COLON + SourceFileStatus.COMMITTED;
					IOHelper.logPrintln(LOG_FPATH, str);
					PL.fsync();
					
					// remove from prepared
					filesPrepared.remove(fileName);
				}
			}
		}
		
		// if commit denied
		else {
			for (String fileName : rcvMsg.getFiles()) {
				if (filesPrepared.containsKey(fileName)) {
					
					// log the change
					String str = fileName + COLON + commitFName;
					str += COLON + SourceFileStatus.ABORTED;
					IOHelper.logPrintln(LOG_FPATH, str);
					PL.fsync();
					
					// move file from prepared to committed
					filesPrepared.remove(fileName, commitFName);
				}
			}
		}
		
		// build reply message
		MessageContent rplMsg = new MessageContent(commitFName, MessageType.COMMIT_ACK, id, addr);
		
		// serialize the message
		byte[] msgBytes = MessageConvert.packMessage(rplMsg);
		
		// send the message
		ProjectLib.Message msg = new ProjectLib.Message(addr, msgBytes);
		PL.sendMessage(msg);
	}
	
	/**
	 * Handles the commit abort message from the Server, and sends abort ACK message
	 * back to the Server.
	 * When the Server asks to abort, the user releases all its related source files.
	 * 
	 * @param addr - address of sender
	 * @param rcvMsg - content of the message
	 */
	private synchronized void handleCommitAbort(String addr, MessageContent rcvMsg) {
		
		String commitFileName = rcvMsg.getFileName();
		
		// abort all files prepared for commit
		for (String sourceFileName : rcvMsg.getFiles()) {
			if (filesPrepared.containsKey(sourceFileName)
			&& filesPrepared.get(sourceFileName).equals(commitFileName)) {

				// log the change
				String str = sourceFileName + COLON + commitFileName;
				str += COLON + SourceFileStatus.ABORTED;
				IOHelper.logPrintln(LOG_FPATH, str);
				PL.fsync();
				
				// remove the file
				filesPrepared.remove(sourceFileName);
			}
		}
		
		// build reply message
		MessageContent rplMsg = new MessageContent(commitFileName, MessageType.COMMIT_ACK, id, addr);
		
		// serialize the message
		byte[] msgBytes = MessageConvert.packMessage(rplMsg);
		
		// send the message
		ProjectLib.Message msg = new ProjectLib.Message(addr, msgBytes);
		PL.sendMessage(msg);
	}

	/**
	 * A callback method that receives messages from the Server. This method identifies the
	 * type of the message, and handles the message correspondingly.
	 * 
	 * @param msg - the message received from the Server
	 * @return true - if the message is successfully received and processed
	 * 	   false - otherwise
	 */
	@Override
	public synchronized boolean deliverMessage(ProjectLib.Message msg) {
		
		// wait until recovery has finished
		while (!recoverFinished.get()) {
			try {
				TimeUnit.MILLISECONDS.sleep(RECOVER_MILLIS);
			} catch (InterruptedException e) {
				// ignore
			}
		}
		
		// get sender address and message content
		byte[] msgBytes = msg.body;
		String addr = msg.addr;
		
		// convert message content from bytes to object
		MessageContent msgContent = MessageConvert.unpackMessage(msgBytes);
		
		switch (msgContent.getMessageType()) {
			case COMMIT_QUERY:
				handleCommitQuery(addr, msgContent);
				break;
			case COMMIT_MSG:
				handleCommitMessage(addr, msgContent);
				break;
			case COMMIT_ABORT:
				handleCommitAbort(addr, msgContent);
				break;
			default:
				System.err.print("Server Node " + id + " received unknown type of message ");
				System.err.println("from " + addr + ".");
				return false;
		}
		
		return true;
	}
	
	
	/* ---------------------------------------------------------------- */
	/* --------------------------   Getters   ------------------------- */
	/* ---------------------------------------------------------------- */
	
	/**
	 * Gets the port used by the User Node to communicate with ProjectLib.
	 * 
	 * @return port - the port number
	 */
	public static int getPort() {
		return port;
	}
	
	/**
	 * Gets the user ID of the User Node.
	 * 
	 * @return id - the user ID
	 */
	public String getID() {
		return id;
	}

}



