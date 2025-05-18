package message;

import java.util.ArrayList;
import java.util.UUID;

import utils.tData;

public class AccessReplyMessage extends AccessMessage {

	private UUID _clientKey;
	private ArrayList<tData> _savedData;

	public AccessReplyMessage(boolean success, AccessType at) {
		super(MessageType.ACCESS_REPLY, at);
		_clientKey = null;

		_header.setStatus(success);
	}

	public AccessReplyMessage(boolean success, AccessType at, UUID key) {
		super(MessageType.ACCESS_REPLY, at, key);
		_clientKey = key;

		_header.setStatus(success);
	}

	public AccessReplyMessage(boolean b, AccessType at, UUID userUUID, ArrayList<tData> savedData) {
		this(b, at, userUUID);
		this._savedData = savedData;
	}

	public UUID getClientKey() {
		return _clientKey;
	}

	public void setClientKey(UUID _clientKey) {
		this._clientKey = _clientKey;
	}

	public ArrayList<tData> getSavedData() {
		return this._savedData;
	}

}
