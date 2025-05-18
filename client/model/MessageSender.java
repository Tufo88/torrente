package model;

import java.io.IOException;
import java.io.ObjectOutputStream;

import message.Message;

//monitor para enviar los mensajes de uno en uno
public class MessageSender {

	private ObjectOutputStream _os;

	public MessageSender(ObjectOutputStream s) {
		_os = s;
	}

	public synchronized void sendMsg(Message m) throws IOException {
		_os.writeObject(m);
	}

	public synchronized void close() throws IOException {
		_os.close();
	}

}
