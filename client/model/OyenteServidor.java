package model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

import message.AccessReplyMessage;
import message.AccessType;
import message.Message;
import message.MessageHeader;
import message.MessageType;
import message.P2POperation;
import message.P2PReqMessage;

public class OyenteServidor extends Thread {

	private Socket _socket;
	private ObjectInputStream _is;
	private Client _client;

	public OyenteServidor(Client client, Socket s) throws IOException {
		_socket = s;
		_is = new ObjectInputStream(_socket.getInputStream());
		_client = client;
	}

	@Override
	public void run() {
		Message m;
		boolean logout = false;
		try {
			while (true) {

				m = (Message) _is.readObject();

				if (!MessageHeader.validateHeader(m, _client.getKey())) {
					// TODO: ADD MSGinvalid request
					continue;
				}

				// debe ejecutar en otro thread
				if (m.getType() == MessageType.P2P_REQ
						&& ((P2PReqMessage) m).getP2POperation() == P2POperation.REQUEST_FILES) {
					final P2PReqMessage p2pm = (P2PReqMessage) m;
					new Thread(() -> {
						try {
							_client.P2PConnectandSend(p2pm);
						} catch (InterruptedException e) {

							e.printStackTrace();
						}
					}).start();
					;
					continue;
				}

				_client.pushMessage(m);

				switch (m.getType()) {
				case ACCESS_REPLY:
					logout = ((AccessReplyMessage) m).getAccessType() == AccessType.LOGOUT;
					break;
				default:
					break;

				}

				if (logout)
					break;
			}

			this.disconnect();

		} catch (Exception e) {

		}

	}

	private void disconnect() throws IOException {
		_is.close();
	}

}
