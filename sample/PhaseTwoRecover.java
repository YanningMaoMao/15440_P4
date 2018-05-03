
/**
 * 
 * @file PhaseTwoRecover.java
 * 
 * This class is a subclass of CommitProcess, and represents a partial Two Phase Commit process
 * that restarts from Phase II. The server redistributes the commit decision recovered from last
 * time, and wait for ACKs from all the UserNodes.
 * 
 * @author YanningMao <yanningm@andrew.cmu.edu>
 * @date April 27, 2018
 *
 */
public class PhaseTwoRecover extends CommitProcess {
	
	private CommitDecision commitDecision;
	
	/**
	 * The constructor constructs a PhaseTwoRecover commit process.
	 * 
	 * @param PL - the ProjectLib object
	 * @param commitInfo - the commit information
	 * @param commitDecision - the commit decision
	 */
	public PhaseTwoRecover(ProjectLib PL, CommitInfo commitInfo, CommitDecision commitDecision) {
		super(PL, commitInfo);
		this.commitDecision = commitDecision;
	}
	
	/**
	 * Runs the process from Phase II, redistributes the commit decision to all UserNodes,
	 * and waits for all the ACKs.
	 */
	@Override
	public void run() {

		/* ------------------------   Phase II   ------------------------ */
		
		// perform Phase II
		phaseTwo(commitDecision);
		
		// log the end of the commit
		IOHelper.logPrintln(commitInfo, DONE_STR);
		PL.fsync();
	}
}


