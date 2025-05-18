package message;

import java.util.ArrayList;
import java.util.UUID;

import utils.tData;

public class FileReplyMessage extends FileMessage {

	private int _expectedConnections = 0;
	private UUID tID;

	public FileReplyMessage(boolean valid, FileOperation op, UUID header) {
		super(MessageType.FILE_REPLY, op, header, valid);

	}

	public FileReplyMessage(UUID header, FileOperation op, ArrayList<tData> data) {
		super(MessageType.FILE_REPLY, op, header, data);

	}

	public FileReplyMessage(boolean valid, FileOperation op, UUID header, int size, UUID transactionID) {
		this(valid, op, header);
		this._expectedConnections = size;
		this.tID = transactionID;
	}

	public int getExpectedConnections() {
		return this._expectedConnections;
	}

	public UUID getTId() {
		return this.tID;
	}

}
