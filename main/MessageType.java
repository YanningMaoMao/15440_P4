/**
 * @file MessageType.java
 * 
 * @author YanningMao <yanningm@andrew.cmu.edu>
 * @date April 18, 2018
 *
 */
public enum MessageType {

	// query from server to node asking for approval of commit
	COMMIT_QUERY,
	
	// agreement message from node to server indicating whether agree to commit
	COMMIT_AGREEMENT,
	
	// commit message from server to node indicating commit started
	COMMIT_MSG,
	
	// acknowledgement from node to server
	COMMIT_ACK,
	
	// aborts the commit (only when commit queries sent but no commit messages sent)
	COMMIT_ABORT;
	
}

