package model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import concurrency.RWLock;
import concurrency.Ticket;
import message.AccessReplyMessage;
import message.AccessReqMessage;
import message.AccessType;
import message.FileOperation;
import message.FileReplyMessage;
import message.FileReqMessage;
import message.Message;
import message.P2POperation;
import message.P2PReplyMessage;
import message.P2PReqMessage;
import message.UsersReplyMessage;
import message.UsersReqMessage;
import utils.Pair;
import utils.tData;
import view.Controller;
import view.Evento;

public class Client {

	private final InetAddress _connectionIp;
	private final int _connectionPort;

	private Socket _socket;
	private OyenteServidor _oyente;
	// private ObjectOutputStream _os;

	private UUID _key;
	private String _id;

	// monitor para el manejo de recibir mensajes desde el oyente servidor
	private MessageReceiver _mr;

	private List<tData> _uploadedFiles;

	// monitor para enviar los mensajes secuencialmente
	private MessageSender _ms;

	// read/write lock para uploadedFiles
	private RWLock filesLock;

	// usamos Ticket como lock de los archivos que recibimos

	public Client() throws UnknownHostException {
		// init conexion con server
		this._connectionIp = InetAddress.getByName("localhost");
		this._connectionPort = 8080;

		_uploadedFiles = new LinkedList<>();

		filesLock = new RWLock();

	}

	public void pushMessage(Message m) {
		this._mr.pushMessage(m);
	}

	public static void main(String[] args) {

		try {
			Client c = new Client();
			c.run();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void run() throws IOException {

		try {
			_socket = new Socket(this._connectionIp, this._connectionPort);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		_mr = new MessageReceiver();
		_oyente = new OyenteServidor(this, _socket);
		_oyente.start();
		_ms = new MessageSender(new ObjectOutputStream(_socket.getOutputStream()));

		Controller.init(this);
	}

	public Object handleEvent(Evento evento, Object datos) throws InterruptedException {
		UUID key;
		Pair<String, String> d;
		switch (evento) {
		// key e id no necesitan locks porque se ejecutan a destiempo
		// del resto de operaciones y son exclusivas register y login
		case REGISTER: {
			d = (Pair<String, String>) datos;
			key = getRegisterKey(d.key(), d.value());
			if (key == null)
				return false;
			_id = d.key();
			_key = key;

			return true;
		}

		case LOGIN: {
			d = (Pair<String, String>) datos;
			Pair<UUID, ArrayList<tData>> ret = logIn(d.key(), d.value());
			if (ret == null)
				return false;
			key = ret.key();
			// no necesita lock porque no se ejecuta a la vez que nada mas
			this._uploadedFiles = ret.value();

			_id = d.key();
			_key = key;

			return true;
		}

		case LOGOUT: {
			logout();
			try {
				_ms.close();
				_oyente.join();
				_socket.close();
			} catch (IOException | InterruptedException e) {

				e.printStackTrace();
			}

			return true;
		}

		case LIST_FILES: {
			List<tData> files;

			files = requestAllFiles();

			return files;
		}

		case UPLOAD_FILES: {
			File[] filesToUpload = (File[]) datos;
			try {
				if (uploadFiles(filesToUpload))
					return this.getData();
				else
					return null;

			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		case DOWNLOAD_FILES: {

			// la idea es que al hacer una descarga, envia el mensaje de los archivos que
			// quiere descargar por nombre, y el servidor le devuelve si es posible.
			// si lo es, este cliente crea un ServerSocket, y espera a la conexion del
			// cliente que tiene los archivos que queremos obtener.
			// cuando se conectan este los envia en un unico mensaje, se envia una
			// confirmacion de recibimiento y se cierra el canal.

			ArrayList<tData> filesToDownload = (ArrayList<tData>) datos;

			Pair<UUID, Integer> ret = requestDownload(filesToDownload);
			// id que reconoce a la transaccion (para permitir multiples simultaneas
			UUID tID = ret.key();
			int connections = ret.value();

			if (tID == null) {
				return false;
				// este indica que el server no ha conseguido encontrar clientes que contengan
				// todos los archivos deseados
			}

			try {

				List<tData> receivedFiles = new LinkedList<>();
				// para la sincronizacion de receivedFiles
				Ticket lock = new Ticket();

				Random r = new Random();
				int port = r.nextInt(1024, 50000);// puerto aleatorio para permitir varias descargas simultaneas
				createServerReceiveFiles(port, connections, filesToDownload.size(), tID, receivedFiles, lock);

				// se ejecutara una vez se hayan terminado de ejecutar todos los Receivers
				// si los archivos que hemos recibido no coinciden con lo que pedimos pues
				// avisamos al server de que ha habido algun problema y nada mas.
				boolean valid = checkAndStoreReceivedFiles(filesToDownload, tID, receivedFiles, lock);

				filesReceived(valid);

				return valid;
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;

		}

		case LIST_USERS: {
			List<Pair<String, Boolean>> users;

			users = requestAllUsers();

			return users;
		}
		default:
			break;
		}
		return null;
	}

	private void filesReceived(boolean valid) {

		P2PReqMessage lr = null;

		lr = new P2PReqMessage(P2POperation.RECEIVED_FILES, _key, valid);

		try {
			_ms.sendMsg(lr);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private boolean checkAndStoreReceivedFiles(List<tData> filesToDownload, UUID tID, List<tData> receivedFiles,
			Ticket lock) {
		boolean valid = true;

		Set<tData> fd = new HashSet<tData>(filesToDownload);

		List<String> hashes = filesToDownload.stream().map(t -> t.getFileHash()).collect(Collectors.toList());

		// usamos hashMap por si hay hashes repetidos (muy muy raro)
		Map<String, Integer> setHashes = new HashMap<>();

		// nos guardamos los hashes de los archivos que buscabamos
		for (String hash : hashes) {
			Integer val = setHashes.getOrDefault(hash, null);
			if (val != null)
				setHashes.replace(hash, val + 1);
			else
				setHashes.put(hash, 1);
		}

		// ya no haria falta, pero si no lo cogemos Java puede no darse cuenta de que es
		// el lock de la variable receivedFiles
		lock.getLock();
		try {

			for (tData f : receivedFiles) {
				// comprobamos los archivos que hemos recibido con los que queriamos descargar
				// (basicamente como si comprobasemos nombre, size y hash)
				valid = valid && fd.contains(tData.removeContent(f));
				if (!valid)
					return false;

			}

			valid = valid && filesToDownload.size() == receivedFiles.size();

			if (!valid)
				return false;

			File parentFolder = new File("").getAbsoluteFile();
			File workingFolder = new File(parentFolder, this._id + "_TorrenteDownloads");

			if (!workingFolder.exists() && !workingFolder.mkdirs())
				return false;

			for (tData f : receivedFiles) {

				// hay que comprobar que el fileContent coincide con el hash
				// del archivo que queriamos
				String generatedHash = tData.generateHash(f.getFileContent());
				Integer val = setHashes.getOrDefault(generatedHash, null);
				if (val == null || val <= 0)
					return false;
				// si han coincidido todos nos quedaremos con un mapa lleno de 0,
				// si hay alguno que no ha coincidido ocurrira el return false porque
				// o hay algun repetido o hay algun archivo con contenido distinto
				// de lo que deseabamos.
				setHashes.replace(generatedHash, val - 1);

				File outFile = new File(workingFolder, f.getFileName());
				try (FileOutputStream fos = new FileOutputStream(outFile)) {
					fos.write(f.getFileContent());
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} finally {
			lock.releaseLock();
		}

		return true;
	}

	private void createServerReceiveFiles(int port, int connections, int amount, UUID tID, List<tData> receivedFiles,
			Ticket lock) throws IOException {

		ServerSocket ss;

		ss = new ServerSocket(port);

		sendServerCreatedMsg(port, ss.getInetAddress(), tID);

		while (connections > 0) {

			Socket s = ss.accept();

			P2PReceiver pr = new P2PReceiver(s, lock, receivedFiles);

			pr.start();

			connections--;

		}

		lock.getLock();
		while (receivedFiles.size() < amount /* and not timeout */) {
			lock.releaseLock();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			lock.getLock();

		}

		// este release da igual porque seremos los unicos que accedamos a nuestra lista
		// pero es para que java no se lie
		lock.releaseLock();

		ss.close();

	}

	// para los senders, añadir algun tipo de retry de la conexion
	// por si estos han recibido mas tiempo de cpu que el clientServer
	// y se han intentado conectar antes de que estuviese disponible.
	// (esto en un entorno real seria practicamnte imposible que pasase pero bueno)
	public void P2PConnectandSend(P2PReqMessage m) throws InterruptedException {
		// add un aviso para la gui de que esta enviando archivos a otro usuario ?

		try {

			int port = m.getPort();
			InetAddress ip = m.getServerIp();

			Set<tData> requestedFiles = new HashSet<>(m.getFiles());

			// TODO: modificarlo para enviar archivo por archivo en cada mensaje
			// para facilitar el envio de archivos mas grandes ?

			ArrayList<tData> files = new ArrayList<>();

			this.filesLock.requestRead();
			for (tData file : this._uploadedFiles) {
				if (files.size() == m.getFiles().size())
					break;
				// si es uno de los archivos que buscamos de este cliente
				if (requestedFiles.contains(tData.removePath(file))) {
					requestedFiles.remove(tData.removePath(file));
					files.add(file);
				}
			}
			this.filesLock.releaseRead();

			ArrayList<tData> finalFiles = new ArrayList<>(files.stream().map((tData data) -> {
				try {
					// tenemos que construir el archivo con el content del archivo
					tData builtFile = new tData(new File(data.getFilePath()));
					// borramos el path porque el receiver no lo necesita
					builtFile.setPath(null);
					return builtFile;
				} catch (IOException e) {
					// e.printStackTrace();
					return null;
				}
			}).collect(Collectors.toList()));

			Socket s = new Socket(ip, port);

			ObjectOutputStream P2Pos = new ObjectOutputStream(s.getOutputStream());
			ObjectInputStream P2Pis = new ObjectInputStream(s.getInputStream());

			P2PReplyMessage pm = new P2PReplyMessage(P2POperation.SEND_FILES, finalFiles);

			P2Pos.writeObject(pm);

			P2PReplyMessage rm = (P2PReplyMessage) P2Pis.readObject();

			// ha recibido los archivos (antes de comprobar que los archivos sean
			// realmente los que se han pedido.
			if (rm.getHeader().getStatus()) {
				P2Pos.close();
				P2Pis.close();
				s.close();
			} else {// no ha recibido correctamente los archivos

			}

		} catch (IOException | ClassNotFoundException e) {

			e.printStackTrace();
		}

	}

	private void sendServerCreatedMsg(int port, InetAddress serverIp, UUID tID) {

		P2PReqMessage lr = null;

		lr = new P2PReqMessage(_key, port, serverIp, tID);

		try {
			_ms.sendMsg(lr);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private Pair<UUID, Integer> requestDownload(ArrayList<tData> filesToDownload) {

		FileReqMessage lr = null;

		lr = new FileReqMessage(FileOperation.PREP_DOWNLOAD, _key, filesToDownload);

		try {
			_ms.sendMsg(lr);
		} catch (IOException e) {
			e.printStackTrace();
		}

		FileReplyMessage m = (FileReplyMessage) _mr.getFileMessage(lr);

		return new Pair<UUID, Integer>(m.getHeader().getStatus() ? m.getTId() : null, m.getExpectedConnections());

	}

	private List<tData> requestAllFiles() {

		FileReqMessage lr = null;

		lr = new FileReqMessage(FileOperation.GET_AVAILABLE_FILES, _key);

		try {
			_ms.sendMsg(lr);
		} catch (IOException e) {
			e.printStackTrace();
		}

		FileReplyMessage m = (FileReplyMessage) this._mr.getFileMessage(lr);

		return m.getHeader().getStatus() ? m.getData() : null;

	}

	private ArrayList<tData> addFiles(File[] files) throws IOException, InterruptedException {
		ArrayList<tData> arr = new ArrayList<tData>();
		for (File f : files) {
			// creamos la data y nos la guardamos sin el content
			// para no llenar nuestro heap
			try {
				tData d = tData.removeContent(new tData(f));
				arr.add(d);
			} catch (IOException e) {
				return null;
			}

		}

		// nos los guardamos en un bucle distinto porque la operacion de new tData(f) es
		// costosa
		this.filesLock.requestWrite();
		for (tData d : arr) {
			_uploadedFiles.add(d);
		}
		this.filesLock.releaseWrite();

		return arr;
	}

	private boolean uploadFiles(File[] filesToUpload) throws IOException, InterruptedException {

		ArrayList<tData> dataToSend = addFiles(filesToUpload);

		if (dataToSend == null)
			return false;

		FileReqMessage lr = null;
		// header (key), ArrayList<tData>
		lr = new FileReqMessage(FileOperation.UPLOAD, _key, dataToSend);

		try {
			_ms.sendMsg(lr);
		} catch (IOException e) {
			e.printStackTrace();
		}

		FileReplyMessage m = (FileReplyMessage) this._mr.getFileMessage(lr);

		return m.getHeader().getStatus(); // getStatus nos devuelve la validez, es decir si se han subido
											// correctamente los archivos.

	}

	private AccessReplyMessage requestAccess(AccessReqMessage lr) {

		try {
			_ms.sendMsg(lr);
		} catch (IOException e) {
			e.printStackTrace();
		}

		AccessReplyMessage m = (AccessReplyMessage) this._mr.getAccessMessage(lr);

		return m;

	}

	private UUID getRegisterKey(String id, String password) {

		AccessReqMessage lr = null;
		try {
			lr = new AccessReqMessage(id, InetAddress.getLocalHost(), password, AccessType.REGISTER);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return null;
		}

		AccessReplyMessage m = this.requestAccess(lr);

		return m.getHeader().getStatus() ? m.getClientKey() : null;

	}

	private Pair<UUID, ArrayList<tData>> logIn(String id, String password) {

		AccessReqMessage lr = null;
		try {
			lr = new AccessReqMessage(id, InetAddress.getLocalHost(), password, AccessType.LOGIN);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return null;
		}

		AccessReplyMessage m = this.requestAccess(lr);

		return m.getHeader().getStatus() ? new Pair<>(m.getClientKey(), m.getSavedData()) : null;

	}

	private boolean logout() {

		AccessReqMessage lr = null;
		try {
			lr = new AccessReqMessage(this._key, InetAddress.getLocalHost(), AccessType.LOGOUT);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		}

		AccessReplyMessage m = requestAccess(lr);

		return m.getHeader().getStatus();

	}

	private List<Pair<String, Boolean>> requestAllUsers() {

		UsersReqMessage lr = null;

		lr = new UsersReqMessage(_key);

		try {
			_ms.sendMsg(lr);
		} catch (IOException e) {
			e.printStackTrace();
		}

		UsersReplyMessage m = (UsersReplyMessage) this._mr.getUsersMessage(lr);

		return m.getHeader().getStatus() ? m.getData() : null;

	}

	public String getId() {
		return _id;
	}

	public UUID getKey() {
		return _key;
	}

	public List<tData> getData() {
		try {
			try {
				this.filesLock.requestRead();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return new ArrayList<>(_uploadedFiles);// enviamos una copia

		} finally {
			try {
				this.filesLock.releaseRead();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

}