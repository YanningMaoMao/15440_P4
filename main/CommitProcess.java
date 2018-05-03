import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 
 * @file CommitProcess.java
 * 
 * This class represents a Two Phase Commit process.
 * The class has three subclasses that represent different commit processes that
 * start from different states.
 * 
 * @author YanningMao <yanningm@andrew.cmu.edu>
 * @date April 27, 2018
 *
 */
public class CommitProcess implements Runnable {


	/* -------------------------------------------------------------------- */
	/* --------------------------   Constants   --------------------------- */
	/* -------------------------------------------------------------------- */
	
	protected static final long TIME_OUT_MILLIS = 6000;
	protected static final String COMMA = ",";
	protected static final String COLON = ":";
	protected static final String CURR_DIR = ".";
	protected static final String LOG_DIR = "log";

	protected static final String PHASE_ONE_STR = "Phase One";
	protected static final String PHASE_TWO_STR = "Phase Two";
	protected static final String DONE_STR = "DONE";


	/* -------------------------------------------------------------------- */
	/* ---------------------   Instance Variables   ----------------------- */
	/* -------------------------------------------------------------------- */
	
	protected ProjectLib PL;
	// the commit information
	protected CommitInfo commitInfo;
	// the commit image
	protected byte[] imgBytes;
	
	// contains blocked commit agreement messages from the User Nodes
	protected BlockingQueue<MessageContent> agreementMessages;
	// contains blocked commit ACK messages from the User Nodes
	protected BlockingQueue<MessageContent> ackMessages;

	
	/* -------------------------------------------------------------------- */
	/* -------------------------   Constructor   -------------------------- */
	/* -------------------------------------------------------------------- */
	
	/**
	 * Constructs the CommitProcess.
	 * 
	 * @param PL - the ProjectLib object
	 * @param commitInfo - the commit information
	 */
	public CommitProcess(ProjectLib PL, CommitInfo commitInfo) {
		this.PL = PL;
		this.commitInfo = commitInfo;
		
		agreementMessages = new LinkedBlockingQueue<>();
		ackMessages = new LinkedBlockingQueue<>();
		
	}
	
	/**
	 * This method is called by the Server to redirect messages to the commit processes they belong.
	 * It takes in a message, and puts it into the corresponding message queue.
	 * 
	 * @param msgContent - message from user
	 * @throws InterruptedException
	 */
	public synchronized void receiveMessage(MessageContent msgContent) throws InterruptedException {
		
		if (msgContent.getMessageType().equals(MessageType.COMMIT_AGREEMENT)) {
			agreementMessages.put(msgContent);
		}
		else if (msgContent.getMessageType().equals(MessageType.COMMIT_ACK)) {
			ackMessages.add(msgContent);
		}
		else {
			System.err.println(">>> Server received unrecognized message : ");
			System.err.println(msgContent);
		}
	}
	
	/**
	 * Receives the commit agreement messages from the User Nodes.
	 * This method polls the commit agreement messages added to the agreement queue.
	 * It waits for all the commit agreement messages to arrive, until time out.
	 * 
	 * @return commitDecision - the decision for the commit request
	 */
	protected CommitDecision receiveAgreementMessage() {

		int numUsers = commitInfo.getUsers().size();
		Set<String> approvals = new HashSet<>();
		Set<String> denials = new HashSet<>();
		
		// keep track of whether time out
		long initTime = System.currentTimeMillis();
		long currTime = System.currentTimeMillis();
		
		// poll agreement messages
		while (approvals.size() + denials.size() < numUsers) {
			MessageContent msgContent;
			// poll the message
			try {
				synchronized (agreementMessages) {
					msgContent = agreementMessages.poll(TIME_OUT_MILLIS,
									    TimeUnit.MILLISECONDS);
				}
				currTime = System.currentTimeMillis();
			} catch (InterruptedException e) {
				msgContent = null;
				currTime = System.currentTimeMillis();
			}
			
			// message times out
			if (msgContent == null) {
				return CommitDecision.ABORT;
			}
			else if (currTime - initTime > TIME_OUT_MILLIS) {
				return CommitDecision.ABORT;
			}
			else {
				if (msgContent.getAgreement()) {
					approvals.add(msgContent.getSender());
				}
				else {
					denials.add(msgContent.getSender());
				}
			}
		}
		
		// return commit decision
		if (denials.size() == 0) {
			return CommitDecision.YES;
		}
		else {
			return CommitDecision.NO;
		}
	}
	
	/**
	 * Receives the commit ACK messages from the User Nodes.
	 * This method polls the commit ACK messages added to the ACK queue.
	 * It waits for all the commit ACK messages to arrive, until time out.
	 * 
	 * @return set - the set of User Nodes that replied ACK
	 */
	protected Set<String> receiveACKMessages() {
		
		int numUsers = commitInfo.getUsers().size();
		Set<String> usersReplied = new HashSet<>();
		
		// track the time
		long initTime = System.currentTimeMillis();
		long currTime = System.currentTimeMillis();
		
		while (usersReplied.size() < numUsers) {

			MessageContent msgContent;
			// poll the message
			try {
				synchronized (ackMessages) {
					msgContent = ackMessages.poll(TIME_OUT_MILLIS,
								      TimeUnit.MILLISECONDS);
				}
				currTime = System.currentTimeMillis();
			} catch (InterruptedException e) {
				msgContent = null;
				currTime = System.currentTimeMillis();
			}
			
			// if times out
			if (msgContent == null || (currTime - initTime) > TIME_OUT_MILLIS) {
				break;
			}
			else {
				usersReplied.add(msgContent.getSender());
			}
		}
		
		return usersReplied;
	}
	
	/**
	 * Sends the commit message to the specified user with the agreement information.
	 * 
	 * @param agreement - whether to commit
	 * @param userAddr - the user ID
	 */
	protected void sendCommitMessageToUser(boolean agreement, String userAddr) {
		
		// set content of the commit message
		MessageContent msgContent = new MessageContent(commitInfo.getFileName(),
								MessageType.COMMIT_MSG,
								"Server", userAddr);
		msgContent.setFiles(commitInfo.getFilesFromUser(userAddr));
		msgContent.setAgreement(agreement);
		
		// convert the message
		byte[] msgBytes = MessageConvert.packMessage(msgContent);
		ProjectLib.Message msg = new ProjectLib.Message(userAddr, msgBytes);
		
		// send the message
		PL.sendMessage(msg);
	}
	
	/**
	 * Sends the commit message to all users.
	 * 
	 * @param agreement - whether to commit
	 */
	protected void sendCommitMessageToAll(boolean agreement) {
		
		for (String userAddr : commitInfo.getUsers()) {
			sendCommitMessageToUser(agreement, userAddr);
		}

	}
	
	/**
	 * Sends the commit abort message to the specified user.
	 * 
	 * @param userAddr - the user ID
	 */
	protected void sendCommitAbortToUser(String userAddr) {
		
		// set content of the commit message
		MessageContent msgContent = new MessageContent(commitInfo.getFileName(),
								MessageType.COMMIT_ABORT,
								"Server", userAddr);
		msgContent.setFiles(commitInfo.getFilesFromUser(userAddr));
		
		// convert the message
		byte[] msgBytes = MessageConvert.packMessage(msgContent);
		ProjectLib.Message msg = new ProjectLib.Message(userAddr, msgBytes);
		
		// send the message
		PL.sendMessage(msg);
		
	}
	
	/**
	 * Sends the commit abort message to all User Nodes.
	 */
	protected void sendCommitAbortToAll() {
		
		for (String userAddr : commitInfo.getUsers()) {
			sendCommitAbortToUser(userAddr);
		}
	}
	
	/**
	 * Sends the commit query to the specified User Node.
	 * 
	 * @param userAddr - the user ID
	 */
	protected void sendCommitQueryToUser(String userAddr) {
		
		// set the content of the message
		MessageContent msgContent = new MessageContent(commitInfo.getFileName(),
								MessageType.COMMIT_QUERY,
								"Server", userAddr);
		
		List<String> files = commitInfo.getFilesFromUser(userAddr);
		msgContent.setFiles(files.toArray(new String[files.size()]));
							
		msgContent.setImg(imgBytes);
							
		// convert the message
		byte[] msgBytes = MessageConvert.packMessage(msgContent);
		ProjectLib.Message msg = new ProjectLib.Message(userAddr, msgBytes);
							
		// send the message
		PL.sendMessage(msg);
		
	}
	
	/**
	 * Sends the commit queries to all User Nodes.
	 */
	protected void sendCommitQueriesToAll() {
		
		// send commit queries
		for (String userAddr : commitInfo.getUsers()) {
			sendCommitQueryToUser(userAddr);
		}
	}
	
	/**
	 * Performs Phase I and returns the commit decision.
	 * 
	 * @return commitDecision - the commit decision
	 */
	protected CommitDecision phaseOne() {

		/* ------------------   Distribute Commit Queries   ------------------ */
		
		sendCommitQueriesToAll();

		/* ------------------   Collect Commit Agreements   ------------------ */
		
		CommitDecision commitDecision = receiveAgreementMessage();
		
		return commitDecision;
	}
	
	/**
	 * Performs Phase II.
	 * Sends the commit decision (Yes / No / Abort) the the User Nodes, and continuously
	 * send until all users reply with ACK.
	 * 
	 * @param commitDecision - the commit decision
	 */
	protected void phaseTwo(CommitDecision commitDecision) {
		
		/* ------------------   Distribute Commit Decisions   ------------------ */

		// if times out, send commit abort
		if (commitDecision.equals(CommitDecision.ABORT)) {
			sendCommitAbortToAll();
		}
		// if commit approved
		else if (commitDecision.equals(CommitDecision.YES)) {
			sendCommitMessageToAll(true);
		}
		// if commit denied
		else {
			sendCommitMessageToAll(false);
		}
		
		/* ------------------------   Collect All ACKs   ------------------------ */
		
		// receive ACK messages
		Set<String> ackUsers = receiveACKMessages();
		
		if (ackUsers.size() == commitInfo.getUsers().size()) {
			// delete log files and logged image
			return;
		}
		
		// get users that time out
		Set<String> timeoutUsers = new HashSet<>();
		for (String userAddr : commitInfo.getUsers()) {
			timeoutUsers.add(userAddr);
		}
		timeoutUsers.removeAll(ackUsers);

		// continuously send commit decision to time out users
		while (timeoutUsers.size() != 0) {
			for (String userAddr : timeoutUsers) {

				// if times out
				if (commitDecision.equals(CommitDecision.ABORT)) {
					sendCommitAbortToUser(userAddr);
				}
				// if commit approved
				else if (commitDecision.equals(CommitDecision.YES)) {
					sendCommitMessageToUser(true, userAddr);
				}
				// if commit denied
				else {
					sendCommitMessageToUser(false, userAddr);
				}
			}
			timeoutUsers.removeAll(receiveACKMessages());
		}
	}

	/**
	 * Runs the commit process. This method is overridden by the subclasses.
	 */
	@Override
	public void run() {
		// overridden by subclasses
	}

}



