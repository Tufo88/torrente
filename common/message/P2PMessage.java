package message;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.UUID;

import utils.tData;

public abstract class P2PMessage extends Message {

	protected int _port;
	protected InetAddress _clientServerIp;

	// contenido del mensaje
	protected P2POperation _op;
	protected ArrayList<tData> _files;

	public P2PMessage(MessageType type, UUID header) {
		super(type, header);
	}

	protected P2PMessage(MessageType type, P2POperation op, UUID head, boolean valid) {
		super(type, head, valid);
		this._op = op;
	}

	public InetAddress getServerIp() {
		return _clientServerIp;
	}

	public int getPort() {
		return _port;
	}

	public P2POperation getP2POperation() {
		return _op;
	}

	public ArrayList<tData> getFiles() {
		return _files;
	}

}
