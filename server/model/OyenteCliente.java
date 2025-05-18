package model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import message.AccessReqMessage;
import message.AccessType;
import message.FileOperation;
import message.FileReplyMessage;
import message.FileReqMessage;
import message.Message;
import message.MessageHeader;
import message.P2POperation;
import message.P2PReqMessage;
import message.UsersReqMessage;

public class OyenteCliente extends Thread {

	private Server _server;
	private Socket _socket;
	private ObjectOutputStream _os;
	private ObjectInputStream _is;
	private UUID _clientKey;

	private Set<UUID> serversEstablished;

	public OyenteCliente(Server server, Socket s) throws IOException {
		_server = server;
		_socket = s;
		_os = new ObjectOutputStream(_socket.getOutputStream());
		_is = new ObjectInputStream(_socket.getInputStream());
		_clientKey = UUID.randomUUID();

		this.serversEstablished = new HashSet<>();
	}

	@Override
	public void run() {

		Message m = null;
		boolean logout = false;
		try {
			while (true) {

				m = (Message) _is.readObject();

				if (!MessageHeader.validateHeader(m, _clientKey)) {
					continue;
				}

				Message outMessage = null;
				switch (m.getType()) {
				case ACCESS_REQ: {
					AccessReqMessage am = (AccessReqMessage) m;

					if (am.getAccessType() == AccessType.LOGIN)
						outMessage = _server.loginUser(am, this);
					else if (am.getAccessType() == AccessType.REGISTER)
						outMessage = _server.registerUser(am, this);
					else {
						outMessage = _server.logoutUser(am);
						logout = outMessage.getHeader().getStatus();
					}

					break;
				}

				case FILE_REQ: {
					FileReqMessage fm = (FileReqMessage) m;

					if (fm.getFileOperation() == FileOperation.UPLOAD) {
						outMessage = _server.uploadFiles(fm);

					} else if (fm.getFileOperation() == FileOperation.GET_AVAILABLE_FILES) {

						outMessage = _server.listFiles(fm);
					} else if (fm.getFileOperation() == FileOperation.PREP_DOWNLOAD) {

						outMessage = _server.prepareDownloadFiles(fm);
						if (outMessage.getHeader().getStatus()) {
							this.serversEstablished.add(((FileReplyMessage) outMessage).getTId());

						}

					}

					break;
				}

				case P2P_REQ: {

					P2PReqMessage pm = (P2PReqMessage) m;

					if (pm.getP2POperation() == P2POperation.SERVER_CREATED) {
						if (this.serversEstablished.contains(pm.getTId())) {
							_server.clientServerCreated(pm);
							this.serversEstablished.remove(pm.getTId());
						}
					}

					break;
				}
				case USERS_REQ: {
					UsersReqMessage fm = (UsersReqMessage) m;
					outMessage = _server.listUsers(fm);
					break;
				}

				default:
					break;

				}

				// casos en los que no devolvemos nada (en logout si devolvemos)
				if (outMessage == null)
					continue;

				try {
					_os.writeObject(outMessage);
				} catch (IOException e) {
					e.printStackTrace();
				}

				if (logout)
					break;

			}

			this.disconnect();

		} catch (Exception e) {
			// TODO no se
			e.printStackTrace();
		}

	}

	private void disconnect() throws IOException {
		this._is.close();
		this._os.close();
		this._socket.close();

	}

	public UUID getClientKey() {

		return _clientKey;
	}

	public void sendMessage(Message m) {
		try {
			_os.writeObject(m);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
