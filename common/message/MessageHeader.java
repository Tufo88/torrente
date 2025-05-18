package message;

import java.io.Serializable;
import java.util.UUID;

public class MessageHeader implements Serializable {

	private static final long serialVersionUID = 1L;

	private UUID _key;
	private boolean _status;

	public MessageHeader() {

	}

	public MessageHeader(UUID key, boolean valid) {
		_key = key;
		_status = valid;
	}

	public UUID getKey() {
		return _key;
	}

	public boolean getStatus() {
		return _status;
	}

	public void setStatus(boolean status) {
		_status = status;
	}

	public static boolean validateHeader(Message msg, UUID key) {

		if (msg.getType() == MessageType.ACCESS_REQ && ((AccessMessage) msg).getAccessType() != AccessType.LOGOUT) // si
																													// estamos
																													// haciendo
																													// request
																													// de
																													// login
																													// o
																													// register
			return true;
		if (msg.getType() == MessageType.ACCESS_REPLY && ((AccessMessage) msg).getAccessType() != AccessType.LOGOUT) // si
																														// estamos
																														// recibiendo
																														// reply
																														// de
																														// login
																														// o
																														// register
			return true;

		return msg.getHeader().getKey().equals(key);
	}

}
