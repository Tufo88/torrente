package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import model.Client;

public class Torrente extends JFrame implements IGUI {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private GUILogin panelLogin;

	private JButton Upload;
	private GUIUpload panelUpload;

	private JButton Download;
	private GUIDownload panelDownload;

	private JButton List;
	private GUIList panelList;

	private JPanel main;

	private JPanel logStatus;

	private final Client client;

	public Torrente(Client cl) {
		super("Torrente");
		this.client = cl;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				initGUI();
			}
		});

	}

	public void initGUI() {
		this.setSize(700, 700);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		panelUpload = new GUIUpload();
		panelDownload = new GUIDownload();
		panelLogin = new GUILogin();
		panelList = new GUIList();

		main = new JPanel(new BorderLayout());

		main.add(panelLogin, BorderLayout.CENTER);
		this.setLocation(60, 60);
		this.setContentPane(main);
		this.setVisible(true);

	}

	public void initAfterLogin(Client cliente) {

		// añadir paneles para GUICliente SwingUtilities.invokeLater(() -> {}
		main = new JPanel(new BorderLayout());
		this.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {

				// para que no se bloquee si se ha caido el server
				new Thread(() -> {
					try {
						Controller.getInstancia().accion(Evento.LOGOUT, null);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}).start();

			}
		});

		JPanel southPanel = new JPanel(new BorderLayout());
		southPanel.setPreferredSize(new Dimension(700, 550));

		JPanel opciones = new JPanel(new GridLayout(6, 1));
		opciones.setPreferredSize(new Dimension(120, 550));
		opciones.setBackground(Color.white);

		JPanel rightSouthPanel = new JPanel(new BorderLayout());
		rightSouthPanel.setPreferredSize(new Dimension(550, 550));

		Upload = new JButton("Upload File");
		Upload.addActionListener((a) -> {
			panelUpload = new GUIUpload();
			rightSouthPanel.removeAll();
			rightSouthPanel.add(panelUpload, BorderLayout.CENTER);
			actualizar(Evento.GUI_UPLOAD, client.getData());
		});

		Download = new JButton("Download File");
		Download.addActionListener((e) -> {
			panelDownload = new GUIDownload();
			rightSouthPanel.removeAll();
			rightSouthPanel.add(panelDownload, BorderLayout.CENTER);
			actualizar(Evento.GUI_DOWNLOAD, null);
		});

		List = new JButton("List Users");
		List.addActionListener((e) -> {
			panelList = new GUIList();
			rightSouthPanel.removeAll();
			rightSouthPanel.add(panelList, BorderLayout.CENTER);
			actualizar(Evento.GUI_LIST, null);
		});
		opciones.add(Upload);
		opciones.add(Download);
		opciones.add(List);
		southPanel.add(opciones, BorderLayout.WEST);
		southPanel.add(rightSouthPanel, BorderLayout.EAST);
		main.add(southPanel, BorderLayout.CENTER);

		logStatus = new LoggedStatus(cliente);

		main.add(logStatus, BorderLayout.PAGE_START);
		this.setContentPane(main);
		this.setVisible(true);

	}

	@Override
	public void actualizar(Evento evento, Object datos) {
		switch (evento) {
		case RES_LOGIN_OK: {
			visibleOff();
			initAfterLogin((Client) datos);
			panelLogin.actualizar(evento, datos);
			break;
		}
		case RES_LOGIN_KO: {
			visibleOff();
			panelLogin.setVisible(true);
			panelLogin.actualizar(evento, datos);
			break;
		}
		case RES_REGISTER_OK: {
			visibleOff();
			initAfterLogin((Client) datos);
			panelLogin.actualizar(evento, datos);
			break;
		}
		case RES_REGISTER_KO: {
			visibleOff();
			panelLogin.setVisible(true);
			panelLogin.actualizar(evento, datos);
			break;
		}
		case RES_LOGOUT_OK: {
			this.dispose();
			break;
		}
		case GUI_DOWNLOAD: {
			visibleOff();
			panelDownload.setVisible(true);
			// panelDownload.actualizar(evento, datos);
			break;
		}
		case GUI_UPLOAD: {
			visibleOff();
			panelUpload.setVisible(true);
			panelUpload.actualizar(evento, datos);
			break;
		}
		case GUI_LIST: {
			visibleOff();
			panelList.setVisible(true);
			break;
		}
		case RES_LIST_OK: {
			visibleOff();
			this.panelDownload.setVisible(true);
			panelDownload.actualizar(evento, datos);
			break;
		}
		case RES_UPLOAD_OK: {
			panelUpload.actualizar(evento, datos);
			break;
		}
		case RES_UPLOAD_KO: {
			panelUpload.actualizar(evento, datos);
			break;
		}
		case RES_DOWNLOAD_OK: {
			this.panelDownload.actualizar(evento, datos);
			break;
		}
		case RES_DOWNLOAD_KO: {
			this.panelDownload.actualizar(evento, datos);
			break;
		}
		case RES_LIST_KO: {
			visibleOff();
			this.panelDownload.setVisible(true);
			panelDownload.actualizar(evento, datos);
			break;
		}
		case RES_LIST_USERS_OK: {
			visibleOff();
			this.panelList.setVisible(true);
			panelList.actualizar(evento, datos);
			break;
		}
		case RES_LIST_USERS_KO: {
			visibleOff();
			this.panelList.setVisible(true);
			panelList.actualizar(evento, datos);
			break;
		}
		default:
			break;

		}
	}

	private void visibleOff() {

		panelLogin.setVisible(false);
		panelDownload.setVisible(false);
		panelUpload.setVisible(false);
		panelList.setVisible(false);
		// resto de paneles a .setVisible(false);
	}
}