import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

/**
 * 
 * @file MessageConvert.java
 * 
 * This class contains two helper methods that convert a message sent between a Server
 * and UserNode between MessageContent object and byte array object.
 * 
 * @author YanningMao <yanningm@andrew.cmu.edu>
 * @date April 18, 2018
 *
 */
public class MessageConvert {
	
	/**
	 * Converts the received message from byte array format into a MessageContent object.
	 * 
	 * @param msgBytes - the byte array of the message
	 * @return msgContent - the MessageContent object
	 */
	public static MessageContent unpackMessage(byte[] msgBytes) {
		
		// open read stream
		MessageContent msg = null;
		ByteArrayInputStream byteArrayIn = new ByteArrayInputStream(msgBytes);
		
		// read the bytes and convert them
		try {
			ObjectInput in = new ObjectInputStream(byteArrayIn);
			msg = (MessageContent) in.readObject();
			in.close();
		} catch (Exception e) {
			System.err.println("Error when unserializing message bytes.");
			System.err.println("Error  : " + e.getMessage());
			e.printStackTrace();
		}
		
		// close the stream
		try {
			byteArrayIn.close();
		} catch (IOException e) {
			// ignore close exceptions
		}
		
		return msg;
		
	}
	
	/**
	 * Converts the message to be sent from a MessageContent object to byte array.
	 * 
	 * @param msg - the MessageContent object
	 * @return msgBytes - message byte array
	 */
	public static byte[] packMessage(MessageContent msg) {
		
		byte[] msgBytes = null;
		
		// convert message into byte array
		ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
		try {
			ObjectOutput out = new ObjectOutputStream(byteArrayOut);
			out.writeObject(msg);
			out.flush();
			msgBytes = byteArrayOut.toByteArray();
		} catch (IOException e) {
			System.err.println("Error when serializing message : " + msg);
			System.err.println("Error  : " + e.getMessage());
			e.printStackTrace();
		}
		
		// close the stream
		try {
			byteArrayOut.close();
		} catch (IOException e) {
			// ignore close exceptions
		}
		
		return msgBytes;
	}
}

