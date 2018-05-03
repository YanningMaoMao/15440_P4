import java.util.List;
import java.io.Serializable;

/**
 * @file Message.java
 * 
 * Represents the content of a message between the Server and the User Node.
 * The message content includes the type of the message, the sender, the receiver,
 * the name of the commit file, the sources, and relevant information.
 * 
 * @author YanningMao <yanningm@andrew.cmu.edu>
 * @date April 18, 2018
 *
 */
public class MessageContent implements Serializable {

	
	/* -------------------------------------------------------------------- */
	/* --------------------------   Constants   --------------------------- */
	/* -------------------------------------------------------------------- */
	
	private static final long serialVersionUID = 8751573692821119157L;

	/* -------------------------------------------------------------------- */
	/* ----------------------   Instance Variables   ---------------------- */
	/* -------------------------------------------------------------------- */
	
	// the message type
	private final MessageType msgType;

	// commit file name
	private String fileName;
	
	// message sender and message receiver
	private final String sender;
	private final String receiver;
	
	// whether the UserNode agrees to commit
	private boolean agreeMent;
	// the image to be committed
	private byte[] img;
	// the source files
	private String[] files;
	

	/* -------------------------------------------------------------------- */
	/* -------------------------   Constructor   -------------------------- */
	/* -------------------------------------------------------------------- */

	
	/**
	 * Constructs a MessageContent object from the basic commit information.
	 * 
	 * @param fileName - commit file name
	 * @param msgType - the message type
	 * @param sender - sender of message
	 * @param receiver - receiver of message
	 */
	public MessageContent(String fileName, MessageType msgType, String sender, String receiver) {
		this.fileName = fileName;
		this.msgType = msgType;
		this.sender = sender;
		this.receiver = receiver;
	}
	

	/* -------------------------------------------------------------------- */
	/* ---------------------------   Setters   ---------------------------- */
	/* -------------------------------------------------------------------- */

	
	/**
	 * Sets the agreement of the agreement message.
	 * @param agreeMent
	 */
	public void setAgreement(boolean agreeMent) {
		this.agreeMent = agreeMent;
	}
	
	/**
	 * Sets the image to be committed.
	 * @param img
	 */
	public void setImg(byte[] img) {
		this.img = img.clone();
	}
	
	/**
	 * Sets the list of source files from the user.
	 * @param files
	 */
	public void setFiles(String[] files) {
		this.files = files.clone();
	}
	
	/**
	 * Sets the list of source files from the user.
	 * @param files
	 */
	public void setFiles(List<String> files) {
		this.files = files.toArray(new String[files.size()]);
	}
	
	
	/* ---------------------------------------------------------------- */
	/* --------------------------   Getters   ------------------------- */
	/* ---------------------------------------------------------------- */

	
	/**
	 * Gets the message type, which can be a commit query, a commit agreement,
	 * a commit message, an ACK message, or a commit abort message.
	 * 
	 * @return msgType - the message type
	 */
	public MessageType getMessageType() {
		return msgType;
	}
	
	public boolean getAgreement() {
		return this.agreeMent;
	}
	
	/**
	 * Gets the name of the commit file.
	 * @return name - commit file name
	 */
	public String getFileName() {
		return fileName;
	}
	
	/**
	 * Gets commit image.
	 * @return img - the commit image
	 */
	public byte[] getImg() {
		return img.clone();
	}
	
	/**
	 * Gets source files.
	 * @return files - the source files
	 */
	public String[] getFiles() {
		return files.clone();
	}
	
	/**
	 * Gets the sender of the message
	 * @return sender - the message sender
	 */
	public String getSender() {
		return sender;
	}
	
	/**
	 * Gets the receiver of the message.
	 * @return receiver - the message receiver
	 */
	public String getReceiver() {
		return receiver;
	}
}


