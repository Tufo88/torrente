package model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import concurrency.RWLock;
import message.AccessReplyMessage;
import message.AccessReqMessage;
import message.AccessType;
import message.FileOperation;
import message.FileReplyMessage;
import message.FileReqMessage;
import message.P2PReqMessage;
import message.UsersReplyMessage;
import message.UsersReqMessage;
import utils.Pair;
import utils.User;
import utils.tData;

public class Server {

	private static final int _serverPort = 8080;

	private Set<String> _activeUsers;
	private List<User> _users;
	private Map<String, User> _idToUser;
	private Map<User, OyenteCliente> _userToOyente;

	private Map<String, Set<String>> _filenameToIds; // para que funcione con los fileHash deberiamos hacer un mapa de
														// tData a List

	// usamos especificamente arraylist para que sea serializable
	private Map<String, ArrayList<tData>> _idToInfo; // tiene que ser tipo tDatapara poder acceder al fileSize y hash

	private Map<UUID, String> _uuidToId;

	private Map<UUID, DownloadManager> _tIdToDM;

	private ServerSocket _ss;

	private final String DB_NAME = "db.json";

	/*
	 * sincronizacion
	 */
	// para limitar el acceso de escritura al archivo que actua como BD
	// solo es necesario para el populateDatabase, porque populateVariables ocurre
	// una unica vez
	private Semaphore dbFileLock;

	// semaforo que funciona como mutex para la seccion critica del mapa tId a DM
	private Semaphore tIdDMLock;

	// read/write lock para idToInfo y idToUser
	private RWLock idsLock;

	// read/write lock para uuidToId usamos este tipo de lock porque
	// hay muchisimos accesos de lectura que se pueden paralelizar.
	private RWLock uuidsLock;

	// read/write lock para activeUsers, userToOyente y users
	private RWLock usersLock;

	// read/write lock para filenameToIds, usamos este para facilitar
	// las descargas simultaneas.
	private RWLock filenamesLock;

	public Server() {
		_users = new LinkedList<>();
		_idToUser = new HashMap<>();
		_userToOyente = new HashMap<>();
		_filenameToIds = new HashMap<>();
		_idToInfo = new HashMap<>();
		_activeUsers = new HashSet<>();

		_uuidToId = new HashMap<>();
		this._tIdToDM = new HashMap<>();

		dbFileLock = new Semaphore(1);
		idsLock = new RWLock();
		uuidsLock = new RWLock();
		usersLock = new RWLock();
		filenamesLock = new RWLock();
		tIdDMLock = new Semaphore(1);

		populateVariables();
	}

	private JSONObject getJSON() throws JSONException {
		try {
			FileInputStream fos = new FileInputStream(new File(DB_NAME));
			JSONObject obj = new JSONObject(new JSONTokener(fos));
			fos.close();
			return obj;
		} catch (IOException e) {
			return new JSONObject();
		}

	}

	// en populateVariables no necesitamos coger ningun Lock,
	// porque solo ocurren operaciones de manera secuencial en el momento que
	// ejecuta
	private void populateVariables() {

		JSONObject jo;
		try {
			jo = getJSON();
			if (jo.isEmpty())
				return;

			JSONArray users = jo.getJSONArray("users");

			for (int i = 0; i < users.length(); i++) {
				JSONObject obj = users.getJSONObject(i);

				User user = new User(obj.getString("id"), InetAddress.getByName(obj.getString("host")),
						obj.getString("hash"), obj.getString("salt"));

				// init de cada user (debe hacerse aqui y en el register unicamente, no en el
				// login)
				_users.add(user);
				_idToUser.put(user.get_id(), user);
				_idToInfo.put(user.get_id(), new ArrayList<>());

				JSONArray data = obj.getJSONArray("user_data"); // son tDatos separados
				for (int j = 0; j < data.length(); j++) {
					JSONObject d = data.getJSONObject(j);

					tData userData = new tData(d.getString("name"), d.getLong("size"), d.getString("hash"),
							d.getString("path"));

					// instanciando/update a accesibleInfo
					if (_filenameToIds.containsKey(userData.getFileName())) {
						_filenameToIds.get(userData.getFileName()).add(user.get_id());
					} else {
						HashSet<String> l = new HashSet<>();
						l.add(user.get_id());
						_filenameToIds.put(d.getString("name"), l);
					}

					_idToInfo.get(user.get_id()).add(userData);

				}

			}
		} catch (JSONException | UnknownHostException e) {
			// el json esta mal construido.

		}
	}

	private void populateDatabase() throws IOException, InterruptedException {
		JSONObject fileJSON = new JSONObject();
		JSONArray users = new JSONArray();

		this.idsLock.requestRead();

		for (User value : _idToUser.values()) {
			JSONObject u = new JSONObject();
			u.put("id", value.get_id());
			u.put("hash", value.get_hash());
			u.put("salt", value.get_salt());
			u.put("host", value.get_ip().getHostAddress());
			JSONArray info = new JSONArray();
			for (tData d : _idToInfo.get(value.get_id())) {
				JSONObject djo = new JSONObject();
				djo.put("name", d.getFileName());
				djo.put("size", d.getFileSize());
				djo.put("hash", d.getFileHash());
				djo.put("path", d.getFilePath());
				info.put(djo);

			}
			u.put("user_data", info);
			users.put(u);
		}

		this.idsLock.releaseRead();

		fileJSON.put("users", users);

		// creamos stringJSON para tener el mutex lo minimo posible
		String stringJSON = fileJSON.toString(2);

		// coger mutex para que no escriban varios a la vez en DB_NAME
		this.dbFileLock.acquire();
		FileWriter file = new FileWriter(DB_NAME, false);
		file.write(stringJSON);
		file.close();
		this.dbFileLock.release();

	}

	public void run(int port) throws IOException {

		_ss = new ServerSocket(port);

		// si el servidor fuese a reinciarse muchas veces a lo mejor se podria
		// intentar ejecutar el populateVariables en otro thread, permitiendo a otros
		// usuarios acceder,
		// pero habria que reorganizar la estructura del json para asegurarnos que no
		// crean un usuario ya existente
		// y demas cosas
		// this.populateVariables();

		boolean close = false;
		while (!close) {
			try {
				Socket s = _ss.accept();

				OyenteCliente oc = new OyenteCliente(this, s);
				oc.start();

			} catch (IOException e) {
				// TODO: ADD MENSAJE
				e.printStackTrace();
			}

		}

		// no es posible llegar aqui por como hemos ideado el server, que siempre
		// ejecuta,
		// y puede cerrarse abruptamente porque los cambios se guardan con el logout de
		// cualquier usuario
		disconnect();

		try {
			populateDatabase();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void disconnect() {

		try {
			_ss.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {

		Server server = new Server();
		try {
			server.run(Server._serverPort);
		} catch (IOException e) {
			// no se ha podido ejec el server
			e.printStackTrace();
		}
	}

	public AccessReplyMessage loginUser(AccessReqMessage msg, OyenteCliente oc) throws InterruptedException {

		try {
			this.idsLock.requestRead();

		} catch (InterruptedException e) {
			e.printStackTrace();
			this.idsLock.releaseRead();
			return new AccessReplyMessage(false, AccessType.LOGIN);
		}

		User user = _idToUser.get(msg.getUserId());
		ArrayList<tData> savedData = this._idToInfo.getOrDefault(msg.getUserId(), null);
		this.idsLock.releaseRead();

		this.usersLock.requestWrite();
		if (savedData != null && user != null && !this._activeUsers.contains(user.get_id())
				&& user.checkPassword(msg.getPassword().toCharArray())) {
			// si es un usuario correcto, actualizamos su ultima ip, y lo colocamos en
			// active users, en userToOyente y guardamos su Key

			user.updateIp(msg.getIp());
			_activeUsers.add(user.get_id());
			this._userToOyente.put(user, oc);
			this.usersLock.releaseWrite();

			UUID userUUID = oc.getClientKey();

			this.uuidsLock.requestWrite();
			this._uuidToId.put(userUUID, user.get_id());
			this.uuidsLock.releaseWrite();

			return new AccessReplyMessage(true, AccessType.LOGIN, userUUID, savedData);

		} else {
			this.usersLock.releaseWrite();
			return new AccessReplyMessage(false, AccessType.LOGIN);
		}

	}

	public AccessReplyMessage registerUser(AccessReqMessage msg, OyenteCliente oc) throws InterruptedException {

		try {
			this.idsLock.requestRead();

		} catch (InterruptedException e) {
			e.printStackTrace();
			this.idsLock.releaseRead();
			return new AccessReplyMessage(false, AccessType.REGISTER);
		}

		User user = _idToUser.getOrDefault(msg.getUserId(), null);
		// si existe el id, no se puede crear
		if (user != null) {
			idsLock.releaseRead();
			return new AccessReplyMessage(false, AccessType.REGISTER);
		}

		this.idsLock.releaseRead();

		// fuera de los locks para no holdear el lock en operaciones costosas
		Pair<String, String> s = User.generateCypheredPass(msg.getPassword().toCharArray()); // hash y salt
		user = new User(msg.getUserId(), msg.getIp(), s.key(), s.value()); // nos guardamos el hash y el salt de la
																			// contraseña creada
		this.usersLock.requestWrite();
		_activeUsers.add(user.get_id());
		_users.add(user);
		_userToOyente.put(user, oc);
		this.usersLock.releaseWrite();

		this.idsLock.requestWrite();
		_idToUser.put(user.get_id(), user);
		this._idToInfo.put(user.get_id(), new ArrayList<>());
		this.idsLock.releaseWrite();

		UUID userUUID = oc.getClientKey();

		this.uuidsLock.requestWrite();
		this._uuidToId.put(userUUID, user.get_id());
		this.uuidsLock.releaseWrite();

		return new AccessReplyMessage(true, AccessType.REGISTER, userUUID);

	}

	public AccessReplyMessage logoutUser(AccessReqMessage msg) throws InterruptedException {

		UUID userUUID = msg.getHeader().getKey();
		try {
			this.idsLock.requestRead();

		} catch (InterruptedException e) {
			e.printStackTrace();
			this.idsLock.releaseRead();
			return new AccessReplyMessage(false, AccessType.LOGOUT, userUUID);
		}

		this.uuidsLock.requestRead();
		User user = _idToUser.getOrDefault(this._uuidToId.get(userUUID), null);
		this.uuidsLock.releaseRead();

		this.idsLock.releaseRead();

		// no deberia pasar
		if (user == null)
			return new AccessReplyMessage(false, AccessType.LOGOUT, userUUID);

		this.uuidsLock.requestWrite();
		this._uuidToId.remove(userUUID);
		this.uuidsLock.releaseWrite();

		this.usersLock.requestWrite();
		_activeUsers.remove(user.get_id());
		this._userToOyente.remove(user);
		this.usersLock.releaseWrite();

		try {
			this.populateDatabase();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return new AccessReplyMessage(true, AccessType.LOGOUT, userUUID);

	}

	public FileReplyMessage uploadFiles(FileReqMessage m) throws InterruptedException {

		UUID userUUID = m.getHeader().getKey();

		this.uuidsLock.requestRead();
		String userId = this._uuidToId.get(userUUID);
		this.uuidsLock.releaseRead();

		try {
			this.idsLock.requestWrite();

		} catch (InterruptedException e) {
			e.printStackTrace();
			this.idsLock.releaseWrite();
			return new FileReplyMessage(false, FileOperation.UPLOAD, userUUID);
		}

		User user = _idToUser.getOrDefault(userId, null);
		if (user == null) {
			// no deberia pasar
			idsLock.releaseWrite();
			return new FileReplyMessage(false, FileOperation.UPLOAD, userUUID);
		}

		ArrayList<tData> data = m.getData();

		// tenemos aun el writelock de ids
		List<tData> userFiles = this._idToInfo.getOrDefault(user.get_id(), null);

		if (userFiles == null) { // no deberia fallar
			idsLock.releaseWrite();
			return new FileReplyMessage(false, FileOperation.UPLOAD, userUUID);
		}

		// hemos cogido el writeLock para poder actualizar mapa
		// porque no sabemos la implementacion del mapa, y suponemos que
		// si pueden ocurrir mas de 1 upload a la vez.

		this.filenamesLock.requestWrite();
		for (tData d : data) {
			Set<String> usersD = this._filenameToIds.getOrDefault(d.getFileName(), null);

			if (usersD == null) { // puede no existir porque es un archivo nuevo
				Set<String> l = new HashSet<>();
				l.add(user.get_id());
				this._filenameToIds.put(d.getFileName(), l);
			} else {
				usersD.add(user.get_id());
			}

			userFiles.add(d);

		}
		this.filenamesLock.releaseWrite();

		this.idsLock.releaseWrite();

		return new FileReplyMessage(true, FileOperation.UPLOAD, userUUID);

	}

	// devuelve todos los archivos DISPONIBLES para descargar que no sean los del
	// que ha hecho la request
	public FileReplyMessage listFiles(FileReqMessage m) throws InterruptedException {

		UUID userKey = m.getHeader().getKey();

		this.uuidsLock.requestRead();
		String id = this._uuidToId.getOrDefault(userKey, null);
		this.uuidsLock.releaseRead();

		if (id == null) // no deberia pasar
			return new FileReplyMessage(false, FileOperation.GET_AVAILABLE_FILES, userKey);

		Set<tData> available = new HashSet<>();

		this.idsLock.requestRead();
		this.usersLock.requestRead();
		// hay que hacerlos asi porque es el unico sitio donde guardamos el tData
		// completo
		for (Entry<String, ArrayList<tData>> kv : _idToInfo.entrySet()) {
			String k = kv.getKey();
			List<tData> v = kv.getValue();
			if (!k.equals(id) && _activeUsers.contains(k))
				for (tData data : v) {
					tData rem = tData.removePath(data);
					if (!available.contains(rem))// evitar archivos repetidos
						available.add(rem);
				}
		}
		this.usersLock.releaseRead();
		this.idsLock.releaseRead();

		ArrayList<tData> res = new ArrayList<tData>(available);

		return new FileReplyMessage(userKey, FileOperation.GET_AVAILABLE_FILES, res);

	}

	public FileReplyMessage prepareDownloadFiles(FileReqMessage m) throws InterruptedException {

		UUID userKey = m.getHeader().getKey();

		this.uuidsLock.requestRead();
		String id = this._uuidToId.getOrDefault(userKey, null);
		this.uuidsLock.releaseRead();

		if (id == null) // no deberia pasar
			return new FileReplyMessage(false, FileOperation.PREP_DOWNLOAD, userKey);

		List<tData> wantedFiles = m.getData();
		Map<String, ArrayList<tData>> retrievedUsers = new HashMap<>();

		try {
			this.filenamesLock.requestRead();
			this.usersLock.requestRead();

			for (tData fa : wantedFiles) {
				tData f = tData.removePath(fa);
				Set<String> ids = this._filenameToIds.getOrDefault(f.getFileName(), null);
				if (ids == null) {
					return new FileReplyMessage(false, FileOperation.PREP_DOWNLOAD, userKey);
				}

				// hacemos el clone para no hacer el requestWrite, memoria a cambio de velocidad
				Set<String> idsClone = new HashSet<>(ids);
				// nos quitamos para no tenernos en cuenta para la descarga
				idsClone.remove(id);

				String userId = hasActiveUser(idsClone, retrievedUsers, this._activeUsers);
				if (userId == null) {
					return new FileReplyMessage(false, FileOperation.PREP_DOWNLOAD, userKey);
				}

				ArrayList<tData> l = retrievedUsers.getOrDefault(userId, null);
				if (l == null) {
					l = new ArrayList<>();
					l.add(f);
					retrievedUsers.put(userId, l);
				} else
					l.add(f);

			}

		} finally {
			this.filenamesLock.releaseRead();
			this.usersLock.releaseRead();
		}

		Map<OyenteCliente, ArrayList<tData>> ocs = new HashMap<>();

		this.idsLock.requestRead();
		this.usersLock.requestRead();
		for (Entry<String, ArrayList<tData>> ru : retrievedUsers.entrySet()) {

			User i = this._idToUser.get(ru.getKey());
			ocs.put(this._userToOyente.get(i), ru.getValue());
		}
		this.usersLock.releaseRead();
		this.idsLock.releaseRead();

		DownloadManager dm = new DownloadManager(ocs);
		UUID transactionID = UUID.randomUUID();

		// seccion critica
		tIdDMLock.acquire();
		this._tIdToDM.put(transactionID, dm);
		tIdDMLock.release();

		return new FileReplyMessage(true, FileOperation.PREP_DOWNLOAD, userKey, ocs.size(), transactionID);

	}

	public void clientServerCreated(P2PReqMessage m) throws InterruptedException {

		UUID tId = m.getTId();

		tIdDMLock.acquire();
		DownloadManager dm = this._tIdToDM.remove(tId); // nos olvidamos de la req
		tIdDMLock.release();

		if (dm == null)
			return;

		dm.setServerIp(m.getServerIp());
		dm.setPort(m.getPort());

		dm.start();

	}

	public UsersReplyMessage listUsers(UsersReqMessage m) throws InterruptedException {

		UUID userKey = m.getHeader().getKey();

		this.uuidsLock.requestRead();
		String id = this._uuidToId.getOrDefault(userKey, null);
		this.uuidsLock.releaseRead();

		if (id == null) // no deberia pasar
			return new UsersReplyMessage(false, userKey);

		this.usersLock.requestRead();
		ArrayList<User> u = new ArrayList<>(_users);
		ArrayList<Pair<String, Boolean>> res = new ArrayList<>(
				u.stream().map(user -> new Pair<>(user.get_id(), _activeUsers.contains(user.get_id())))
						.collect(Collectors.toList()));
		this.usersLock.releaseRead();

		return new UsersReplyMessage(userKey, res);

	}

	// realmente para hacer esto optimo habria que o hacer un algoritmo voraz o dp
	// pero nos sirve este approach
	private String hasActiveUser(Collection<String> idsFromFile, Map<String, ArrayList<tData>> users, Set<String> au) {
		String secondOption = null;
		for (String s : idsFromFile) {
			User u = this._idToUser.getOrDefault(s, null);
			if (u == null)
				continue;

			if (users.containsKey(s)) // intentamos repetir siempre que podamos, para conectarnos al menor numero de
										// clientes
				return s;

			if (au.contains(s)) // añadir 1 nuevo que este activo
				secondOption = s;
		}

		return secondOption;

	}
}
