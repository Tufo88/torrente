package message;

import java.util.ArrayList;
import java.util.UUID;

import utils.Pair;

public class UsersReqMessage extends UsersMessage {

	public UsersReqMessage(UUID header, boolean valid) {
		super(MessageType.USERS_REQ, header, valid);
	}

	public UsersReqMessage(UUID header) {
		super(MessageType.USERS_REQ, header, true);

	}

	public UsersReqMessage(UUID header, ArrayList<Pair<String, Boolean>> data) {
		super(MessageType.USERS_REQ, header, data); // MessageHeader(header, true)

	}

}