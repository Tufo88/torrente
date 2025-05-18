package message;

import java.util.ArrayList;
import java.util.UUID;

import utils.tData;

public abstract class FileMessage extends Message {

	private FileOperation _fo;
	protected ArrayList<tData> _data;

	public FileMessage(MessageType type, FileOperation op, UUID header, boolean valid) {

		super(type, header, valid);
		_fo = op;

	}

	public FileMessage(MessageType type, FileOperation op, UUID header, ArrayList<tData> data) {
		// TODO: realmente habria que hacer comprobaciones de que se usan MessageType
		// correctos (relacionados con file) a lo mejor guardando un array o un set
		// y comprobando que el type contenga uno de esos permitidos

		super(type, header);
		_fo = op;
		_data = data;

	}

	public FileMessage(MessageType type, UUID header, ArrayList<tData> data) {
		super(type, header);
		_data = data;
	}

	public ArrayList<tData> getData() {
		return new ArrayList<>(_data);
	}

	public FileOperation getFileOperation() {
		return _fo;
	}

}
