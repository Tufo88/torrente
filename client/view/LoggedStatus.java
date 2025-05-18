package view;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import model.Client;

public class LoggedStatus extends JPanel {

	private Client _cliente;

	private JPanel main;

	public LoggedStatus(Client cliente) {
		_cliente = cliente;
		initGUI();
	}

	public void initGUI() {
		main = new JPanel(new BorderLayout());

		JLabel label = new JLabel("Usuario: " + _cliente.getId() + "  ");

		main.add(label, BorderLayout.WEST);

		JButton logout = new JButton("Logout");

		logout.addActionListener(e -> {
			try {
				Controller.getInstancia().accion(Evento.LOGOUT, null);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});
		main.add(logout, BorderLayout.EAST);
		this.add(main);
	}
}