package message;

import java.util.ArrayList;

import utils.tData;

public class P2PReplyMessage extends P2PMessage {

	public P2PReplyMessage(P2POperation op, ArrayList<tData> files) {
		super(MessageType.P2P_REPLY, op, null, true);
		// se envia el mensaje sin el UUID, porque es otro tipo de comunicacion
		// que no tiene nada que ver con el UUID del cliente

		this._files = files;

	}

	public P2PReplyMessage(P2POperation op, boolean valid) {
		super(MessageType.P2P_REPLY, op, null, valid);
	}

}
