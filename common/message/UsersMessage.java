package message;

import java.util.ArrayList;
import java.util.UUID;

import utils.Pair;

public abstract class UsersMessage extends Message {

	protected ArrayList<Pair<String, Boolean>> _data;

	public UsersMessage(MessageType type, UUID header, boolean valid) {
		super(type, header, valid);

	}

	public UsersMessage(MessageType type, UUID header, ArrayList<Pair<String, Boolean>> data) {
		super(type, header);
		_data = data;

	}

	public ArrayList<Pair<String, Boolean>> getData() {
		return new ArrayList<>(_data);
	}
}