/**
 * 
 * @file SourceFileStatus
 * 
 * This is a type class that represents the status of a source file on the UserNode.
 * A source file can be waiting for the decision from the server, released due to abort
 * command from the Server, or committed and deleted from the UserNode.
 * 
 * @author YanningMao <yanningm@andrew.cmu.edu>
 * @date April 28, 2018
 *
 */
public enum SourceFileStatus {
	
	// a source file is available prepared to be committed, but still waiting for commit decision from the Server
	PREPARED,
	
	// a source file is released from prepared state
	ABORTED,
	
	// a source file is used by the commit
	COMMITTED;

}

