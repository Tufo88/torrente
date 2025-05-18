package utils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class tData implements Serializable {

	private static final long serialVersionUID = 1L;

	private String _filePath;
	private String _fileName;
	private long _fileSize;
	private String _fileHash;

	private byte[] _fileContent;

	public tData(File file) throws IOException {

		_fileName = file.getName();
		_fileSize = file.length();
		_filePath = file.getAbsolutePath();

		_fileContent = Files.readAllBytes(file.toPath());
		_fileHash = tData.generateHash(_fileContent);
	}

	// TODO: constructor temporal, habria que hacer un tClientData que contuviese el
	// fileContent y se construyese con el file, y este ser para guardar el resto
	public tData(String name, long size, String hash, String path) {
		this._fileName = name;
		this._fileSize = size;
		this._fileHash = hash;
		this._filePath = path;
	}

	private tData(tData clientFile) {
		_fileName = clientFile._fileName;
		_fileSize = clientFile._fileSize;
		_fileHash = clientFile._fileHash;
		_fileContent = null;
		_filePath = null;

	}

	public static tData removeContent(tData clientFile) {
		tData md = new tData(clientFile);
		md._filePath = clientFile.getFilePath();
		return md;
	}

	public static tData removePath(tData clientFile) {
		tData md = new tData(clientFile);
		md._fileContent = clientFile.getFileContent();
		return md;
	}

	public void setPath(String path) {
		this._filePath = path;
	}

	public static String generateHash(byte[] content) {
		try {

			byte[] digested = MessageDigest.getInstance("MD5").digest(content);

			StringBuilder sb = new StringBuilder();
			for (byte b : digested) {
				sb.append(String.format("%02x", b)); // Convertir a hex
			}

			String hash = sb.toString();

			return hash;

		} catch (NoSuchAlgorithmException e) {

			return null;

		}

	}

	public String getFileName() {
		return _fileName;
	}

	public long getFileSize() {
		return _fileSize;
	}

	public byte[] getFileContent() {
		return _fileContent;
	}

	public String getFileHash() {
		return _fileHash;
	}

	public String getFilePath() {
		return _filePath;
	}

	@Override
	public int hashCode() {
		return Objects.hash(_fileHash, _fileName, _fileSize);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		tData other = (tData) obj;
		return Objects.equals(_fileHash, other._fileHash) && Objects.equals(_fileName, other._fileName)
				&& _fileSize == other._fileSize;
	}

	@Override
	public String toString() {
		return this._fileName + " " + this._fileSize + " " + this._fileHash;
	}

}
