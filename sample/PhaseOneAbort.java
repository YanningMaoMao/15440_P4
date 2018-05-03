
/**
 * 
 * @file PhaseOneAbort.java
 * 
 * This class is a subclass of CommitProcess, and represents a partial Two Phase Commit process
 * that aborts the commit process which failed during Phase I. The server distributes the commit
 * abort messages to all UserNodes, and waits for all the ACKs.
 * 
 * @author YanningMao <yanningm@andrew.cmu.edu>
 * @date April 27, 2018
 *
 */
public class PhaseOneAbort extends CommitProcess {
	
	/**
	 * This constructor constructs a PhaseOneAbort object.
	 * 
	 * @param PL - the PrjectLib object
	 * @param commitInfo - the commit information
	 */
	public PhaseOneAbort(ProjectLib PL, CommitInfo commitInfo) {
		super(PL, commitInfo);
	}
	
	/**
	 * Runs the process from Phase II, distributes commit abort to all UerNodes,
	 * and waits for all the ACKs.
	 */
	@Override
	public void run() {

		/* ------------------------   Phase II   ------------------------ */
		
		// log the start of Phase II
		IOHelper.logPrintln(commitInfo, PHASE_TWO_STR + COLON + CommitDecision.ABORT);
		PL.fsync();
		
		// perform Phase II
		phaseTwo(CommitDecision.ABORT);
		
		// log the end of the commit
		IOHelper.logPrintln(commitInfo, DONE_STR);
		PL.fsync();
	}

}


