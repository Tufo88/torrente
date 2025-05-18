package message;

import java.net.InetAddress;
import java.util.UUID;

public class AccessReqMessage extends AccessMessage {

	private String _id;
	private InetAddress _ip;
	private String _password;

	public AccessReqMessage(String id, InetAddress ip, String password, AccessType at) {
		super(MessageType.ACCESS_REQ, at);
		_id = id;
		_ip = ip;
		_password = password;

	}

	public AccessReqMessage(UUID key, String id, InetAddress ip, AccessType at) {
		super(MessageType.ACCESS_REQ, at, key, true); // MessageHeader(key, true) los requests siempre no pueden
														// devolver que algo ha ido mal
		_id = id;
		_ip = ip;

	}

	public AccessReqMessage(UUID key, InetAddress ip, AccessType at) {
		super(MessageType.ACCESS_REQ, at, key, true); // MessageHeader(key, true) los requests siempre no pueden
														// devolver que algo ha ido mal
		_ip = ip;

	}

	public String getUserId() {
		return _id;
	}

	public InetAddress getIp() {

		return _ip;
	}

	public String getPassword() {
		return _password;
	}

}
