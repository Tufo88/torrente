package message;

import java.util.UUID;

public abstract class AccessMessage extends Message {

	private AccessType _at;

	public AccessMessage(MessageType type, AccessType at) {
		super(type);
		_at = at;
	}

	public AccessMessage(MessageType type, AccessType at, UUID header) {
		super(type, header);
		_at = at;
	}

	public AccessMessage(MessageType type, AccessType at, UUID header, boolean valid) {
		super(type, header, valid);
		_at = at;
	}

	public AccessType getAccessType() {
		return _at;
	}

}
