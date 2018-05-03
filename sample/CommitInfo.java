import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @file CommitInfo
 * 
 * This class represents information about a commit process.
 * This class has variables that store the commit file name, the sources, and the contributors,
 * and has getters that gets the relevant information.
 * 
 * @author YanningMao <yanningm@andrew.cmu.edu>
 * @date April 19, 2018
 *
 */
public class CommitInfo {
	
	/* -------------------------------------------------------------------- */
	/* --------------------------   Constants   --------------------------- */
	/* -------------------------------------------------------------------- */
	
	private static final String COLON = ":";
	
	/* -------------------------------------------------------------------- */
	/* ---------------------   Instance Variables   ----------------------- */
	/* -------------------------------------------------------------------- */
	
	// commit file name
	private String fileName;
	// list of sources in the format <contributor>:<sourceFile>
	private String[] sources;
	// map from contributor to its sources
	private Map<String, List<String>> files;
	

	/* -------------------------------------------------------------------- */
	/* -------------------------   Constructor   -------------------------- */
	/* -------------------------------------------------------------------- */

	/**
	 * Constructs a CommitInfo object from the commit file name and the sources.
	 * 
	 * @param fileName - commit file name
	 * @param sources - list of sources
	 */
	public CommitInfo(String fileName, String[] sources) {
		this.fileName = fileName;
		this.sources = sources.clone();
		// parse the sources by their contributors
		parseSources(sources);
	}

	
	/* ---------------------------------------------------------------- */
	/* --------------------------   Getters   ------------------------- */
	/* ---------------------------------------------------------------- */
	
	/**
	 * Gets the commit file name.
	 * 
	 * @return fileName - commit file name
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * Gets the number of total sources.
	 * 
	 * @return numSources - number of sources
	 */
	public int getNumSources() {
		return sources.length;
	}

	/**
	 * Gets the source at specified index.
	 * 
	 * @param i - the source index
	 * @return source - the specified source
	 */
	public String getSource(int i) {
		return sources[i];
	}

	/**
	 * Gets the list of all sources.
	 * 
	 * @return sources - the list of all sources
	 */
	public String[] getSources() {
		return sources.clone();
	}

	/**
	 * Gets the source files contributed by the specified user (UserNode).
	 * 
	 * @param user - the user node
	 * @return files - source files from the user node
	 */
	public List<String> getFilesFromUser(String user) {

		if (!files.containsKey(user)) {
			return null;
		}

		else {
			return files.get(user);
		}
	}

	/**
	 * Gets the list of contributors.
	 * 
	 * @return user - all the contributors
	 */
	public Set<String> getUsers() {
		return files.keySet();
	}

	/**
	 * Parses the list of all sources by their contributors, and build a map that maps from
	 * the user node to its source files.
	 * 
	 * @param sources - all the sources
	 */
	private void parseSources(String[] sources) {

		// initialize the map
		files = new HashMap<>();

		// initialize the map to store the contributors and sources
		if (sources == null || sources.length == 0) {
			return;
		}

		// parse the sources
		for (String source : sources) {

			// get contributor and file name
			String[] nodeAndFile = source.split(COLON);
			String nodeName = nodeAndFile[0];
			String fileName = nodeAndFile[1];

			// add the file to the node
			if (!files.containsKey(nodeName)) {
				files.put(nodeName, new ArrayList<>());
			}
			files.get(nodeName).add(fileName);
		}
	}


}


