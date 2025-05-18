package message;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.UUID;

import utils.tData;

public class P2PReqMessage extends P2PMessage {

	private UUID tId;

	private P2PReqMessage(UUID header, int port, InetAddress ip) {
		super(MessageType.P2P_REQ, header);
		this._port = port;
		this._clientServerIp = ip;
	}

	public P2PReqMessage(UUID header, int port, InetAddress ip, UUID tId) {
		this(header, port, ip);
		this._op = P2POperation.SERVER_CREATED;
		this._clientServerIp = ip;
		this.tId = tId;
	}

	public P2PReqMessage(UUID header, int port, InetAddress ip, ArrayList<tData> files) {
		this(header, port, ip);
		this._files = files;
		this._op = P2POperation.REQUEST_FILES;

	}

	public P2PReqMessage(P2POperation receivedFiles, UUID key, boolean yesOrNo) {
		super(MessageType.P2P_REQ, receivedFiles, key, yesOrNo);

	}

	public UUID getTId() {
		return this.tId;
	}

}
