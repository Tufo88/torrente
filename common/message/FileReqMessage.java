package message;

import java.util.ArrayList;
import java.util.UUID;

import utils.tData;

public class FileReqMessage extends FileMessage {

	public FileReqMessage(FileOperation op, UUID header, boolean valid) {
		super(MessageType.FILE_REQ, op, header, valid);
	}

	public FileReqMessage(FileOperation op, UUID header) {
		super(MessageType.FILE_REQ, op, header, true);

	}

	public FileReqMessage(FileOperation op, UUID header, ArrayList<tData> data) {
		super(MessageType.FILE_REQ, op, header, data); // MessageHeader(header, true)

	}

}
