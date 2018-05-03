/**
 * 
 * @file FullCommitProcess.java
 * 
 * Represents a complete commit process that starts from the beginning.
 * The process starts from logging the information of the commit, to Phase I,
 * then to Phase II.
 * 
 * This class extends the super class CommitProcess that implements the Runnable interface,
 * and overrides the run() method to perform a full commit process.
 * 
 * @author YanningMao <yanningm@andrew.cmu.edu>
 * @date April 27, 2018
 *
 */
public class FullCommitProcess extends CommitProcess {

	/**
	 * Constructs a full Two Phase Commit process.
	 * 
	 * @param PL - the ProjectLib object
	 * @param commitInfo - the commit information
	 */
	public FullCommitProcess(ProjectLib PL, CommitInfo commitInfo, byte[] imgBytes) {
		super(PL, commitInfo);
		this.imgBytes = imgBytes;
	}

	/**
	 * Performs the full commit process from Phase I to Phase II.
	 * 
	 * Before commit starts : Logs the commit information on the disk.
	 * In Phase I : Sends out the commit queries to all UserNodes, then makes a commit decision
	 * according to the UserNode' replies.
	 * In Phase II : Distributes the commit decision to all UserNodes, and waits for all the ACKs.
	 * If any ACK times out, redistributes the commit decision to all UserNodes.
	 * 
	 */
	@Override
	public void run() {
		
		// log commit information
		IOHelper.logCommitInfo(commitInfo);
		PL.fsync();
		
		/* ------------------------   Phase I   ------------------------ */
		
		// log the start of Phase I 
		IOHelper.logPrintln(commitInfo, PHASE_ONE_STR);
		PL.fsync();
		
		// perform phase I
		CommitDecision commitDecision = phaseOne();

		/* ------------------------   Phase II   ------------------------ */
		
		// commit and save the image if commit approved
		if (commitDecision.equals(CommitDecision.YES)) {
			IOHelper.commitImage(commitInfo.getFileName(), imgBytes);
		}
		
		// log the start of Phase II
		IOHelper.logPrintln(commitInfo, PHASE_TWO_STR + COLON + commitDecision);
		PL.fsync();
		
		// perform Phase II
		phaseTwo(commitDecision);
		
		// log the end of the commit
		IOHelper.logPrintln(commitInfo, DONE_STR);
		PL.fsync();
	}
}



