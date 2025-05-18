package message;

import java.util.ArrayList;
import java.util.UUID;

import utils.Pair;

public class UsersReplyMessage extends UsersMessage {

	public UsersReplyMessage(boolean valid, UUID header) {
		super(MessageType.USERS_REPLY, header, valid);

	}

	public UsersReplyMessage(UUID header, ArrayList<Pair<String, Boolean>> data) {
		super(MessageType.USERS_REPLY, header, data);

	}
}