package model;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import message.AccessMessage;
import message.AccessReplyMessage;
import message.FileMessage;
import message.FileReplyMessage;
import message.Message;
import message.MessageType;
import message.UsersMessage;

public class MessageReceiver {

	private Queue<Message> _queue;

	private Lock _l;
	private Condition _q;

	public MessageReceiver() {
		this._queue = new LinkedList<>();
		this._l = new ReentrantLock();
		this._q = _l.newCondition();
	}

	public Message getFileMessage(FileMessage lr) {
		_l.lock();
		try {

			boolean firstTimeIn = true;
			while (_queue.isEmpty() || _queue.peek().getType() != MessageType.FILE_REPLY
					|| ((FileReplyMessage) _queue.peek()).getFileOperation() != lr.getFileOperation()) {

				try {
					if (firstTimeIn)
						firstTimeIn = false;
					else if (!_queue.isEmpty())// deberia siempre ser cierto
						_q.signal(); // despertamos porque hay otro thread que le toca despertar
					_q.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			return _queue.poll();
		} finally {
			_l.unlock();
		}
	}

	public Message getAccessMessage(AccessMessage lr) {
		_l.lock();
		try {

			boolean firstTimeIn = true;
			while (_queue.isEmpty() || _queue.peek().getType() != MessageType.ACCESS_REPLY
					|| ((AccessReplyMessage) _queue.peek()).getAccessType() != lr.getAccessType()) {
				try {
					if (firstTimeIn)
						firstTimeIn = false;
					else if (!_queue.isEmpty())// deberia siempre ser cierto
						_q.signal(); // despertamos porque hay otro thread que le toca despertar
					_q.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			return _queue.poll();
		} finally {
			_l.unlock();
		}
	}

	public Message getUsersMessage(UsersMessage lr) {
		_l.lock();
		try {

			boolean firstTimeIn = true;
			while (_queue.isEmpty() || _queue.peek().getType() != MessageType.USERS_REPLY) {
				try {
					if (firstTimeIn)
						firstTimeIn = false;
					else if (!_queue.isEmpty())// deberia siempre ser cierto
						_q.signal(); // despertamos porque hay otro thread que le toca despertar
					_q.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			return _queue.poll();
		} finally {
			_l.unlock();
		}
	}

	public void pushMessage(Message m) {
		_l.lock();
		try {
			_queue.add(m);
			_q.signal();
		} finally {
			_l.unlock();
		}
	}

}
