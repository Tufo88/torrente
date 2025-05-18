package message;

import java.io.Serializable;
import java.util.UUID;

public abstract class Message implements Serializable {
	protected static final long serialVersionUID = 1L;
	private MessageType _type;
	protected MessageHeader _header;

	public Message(MessageType type) {
		_type = type;
		_header = new MessageHeader();
	}

	public Message(MessageType type, UUID head, boolean valid) {
		_type = type;
		_header = new MessageHeader(head, valid);
	}

	public Message(MessageType type, UUID header) {
		_type = type;
		_header = new MessageHeader(header, true);
	}

	public MessageType getType() {
		return _type;
	}

	public MessageHeader getHeader() {
		return _header;
	}

}
