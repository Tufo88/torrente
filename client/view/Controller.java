package view;

import model.Client;

public class Controller {
	private static Controller instance;
	private IGUI gui;
	private Client _client;

	private Controller(Client client) {
		gui = new Torrente(client);
		_client = client;
	}

	public void accion(Evento evento, Object datos) throws Exception {

		switch (evento) {
		case LOGIN: {
			boolean res = (boolean) _client.handleEvent(evento, datos);
			if (res) {
				gui.actualizar(Evento.RES_LOGIN_OK, _client);
			} else {
				gui.actualizar(Evento.RES_LOGIN_KO, null);
			}
			break;
		}

		case REGISTER: {
			boolean res = (boolean) _client.handleEvent(evento, datos);

			if (res) {
				gui.actualizar(Evento.RES_REGISTER_OK, _client);

			} else {
				gui.actualizar(Evento.RES_REGISTER_KO, null);
			}
			break;
		}

		case LOGOUT: {
			_client.handleEvent(evento, datos);

			gui.actualizar(Evento.RES_LOGOUT_OK, datos);

			break;
		}
		case LIST_FILES: {
			Object res = _client.handleEvent(evento, datos);

			if (res != null)
				gui.actualizar(Evento.RES_LIST_OK, res);
			else
				gui.actualizar(Evento.RES_LIST_KO, null);
			break;
		}
		case UPLOAD_FILES: {
			Object res = _client.handleEvent(evento, datos);

			if (res != null) {

				gui.actualizar(Evento.RES_UPLOAD_OK, res);

			} else {
				gui.actualizar(Evento.RES_UPLOAD_KO, null);
			}
			break;
		}
		case DOWNLOAD_FILES: {

			boolean res = (boolean) _client.handleEvent(evento, datos);

			gui.actualizar(res ? Evento.RES_DOWNLOAD_OK : Evento.RES_DOWNLOAD_KO, null);

			break;
		}

		case LIST_USERS: {
			Object res = _client.handleEvent(evento, datos);

			if (res != null)
				gui.actualizar(Evento.RES_LIST_USERS_OK, res);
			else
				gui.actualizar(Evento.RES_LIST_USERS_KO, null);
			break;
		}

		default:
			break;

		}
	}

	public static Controller init(Client client) {
		if (instance == null) {
			instance = new Controller(client);
		}

		return instance;
	}

	public static Controller getInstancia() {
		return instance;
	}

}