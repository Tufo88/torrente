package model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import concurrency.Ticket;
import message.Message;
import message.MessageType;
import message.P2POperation;
import message.P2PReplyMessage;
import utils.tData;

public class P2PReceiver extends Thread {

	private Socket _socket;
	private ObjectOutputStream _os;
	private ObjectInputStream _is;

	private Ticket _lock;
	private List<tData> _received;

	public P2PReceiver(Socket sSocket, Ticket t, List<tData> received) throws IOException {
		_socket = sSocket;

		_is = new ObjectInputStream(_socket.getInputStream());
		_os = new ObjectOutputStream(_socket.getOutputStream());

		_lock = t;
		_received = received;

	}

	@Override
	public void run() {
		Message m;
		try {
			do {

				m = (Message) _is.readObject();

			} while (m.getType() != MessageType.P2P_REPLY
					|| ((P2PReplyMessage) m).getP2POperation() != P2POperation.SEND_FILES);

			P2PReplyMessage pm = (P2PReplyMessage) m;

			P2PReplyMessage om;
			ArrayList<tData> files = pm.getFiles();

			boolean valid = files != null;

			if (valid) {
				for (tData file : files) {
					valid = valid && file != null;
					if (!valid)
						break;
				}
			}

			om = new P2PReplyMessage(pm.getP2POperation(), valid);
			_os.writeObject(om);

			if (valid) {
				_lock.getLock();
				_received.addAll(files);
				_lock.releaseLock();
			}

		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}

		try {
			disconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void disconnect() throws IOException {
		_is.close();
		_os.close();
		_socket.close();
	}

}
