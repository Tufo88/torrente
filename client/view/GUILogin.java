package view;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import utils.Pair;

public class GUILogin extends JPanel implements IGUI {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private JPanel mainPanel;

	private JLabel name;
	private JTextField tname;
	private JLabel pass;
	private JPasswordField tpass;
	private JButton login;
	private JButton register;
	private JLabel pass2;
	private JPasswordField tpass2;

	public GUILogin() {
		initGUI();
	}

	private void initGUI() {
		mainPanel = new JPanel(new GridLayout(3, 2));
		this.setBorder(BorderFactory.createTitledBorder("LOGIN"));
		this.add(mainPanel, BorderLayout.CENTER);

		name = new JLabel("User");
		tname = new JTextField(10);
		pass = new JLabel("Password");
		tpass = new JPasswordField(10);
		tpass.addActionListener(e -> loginSeq());

		login = new JButton("Confirm Login");
		register = new JButton("Register");
		mainPanel.add(name);
		mainPanel.add(tname);
		mainPanel.add(pass);
		mainPanel.add(tpass);
		mainPanel.add(register);
		mainPanel.add(login);

		login.addActionListener(e -> {
			loginSeq();
		});

		register.addActionListener(e -> {
			this.removeAll();
			initGUIRegister();
			this.revalidate();
		});
	}

	private void loginSeq() {
		String name = tname.getText();

		try {
			Controller.getInstancia().accion(Evento.LOGIN,
					new Pair<String, String>(name, new String(tpass.getPassword())));
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	private void initGUIRegister() {
		mainPanel = new JPanel(new GridLayout(4, 2));
		this.setBorder(BorderFactory.createTitledBorder("REGISTER"));

		this.add(mainPanel, BorderLayout.CENTER);

		name = new JLabel("User");
		tname = new JTextField(10);
		pass = new JLabel("Password");
		tpass = new JPasswordField(10);
		pass2 = new JLabel("Confirm Password");
		tpass2 = new JPasswordField(10);

		tpass.addActionListener(e -> registerSeq());
		tpass2.addActionListener(e -> registerSeq());

		login = new JButton("Login");
		register = new JButton("Confirm Register");
		mainPanel.add(name);
		mainPanel.add(tname);
		mainPanel.add(pass);
		mainPanel.add(tpass);
		mainPanel.add(pass2);
		mainPanel.add(tpass2);
		mainPanel.add(login);
		mainPanel.add(register);

		login.addActionListener(e -> {
			this.removeAll();
			initGUI();
			this.revalidate();
		});

		register.addActionListener(e -> {
			registerSeq();
		});

	}

	private void registerSeq() {
		String name = tname.getText();
		char[] p = tpass.getPassword();
		char[] p2 = tpass2.getPassword();
		try {
			if (this.same(p, p2) == 0)
				Controller.getInstancia().accion(Evento.REGISTER, new Pair<>(name, new String(p)));
			else
				JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden");
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	private int same(char[] a, char[] b) {
		int same = 0;
		if (a.length != b.length)
			return -1;
		for (int i = 0; i < a.length; i++) {
			if (a[i] != b[i]) {
				same = -1;
				break;
			}
		}
		return same;
	}

	@Override
	public void actualizar(Evento evento, Object datos) {
		switch (evento) {
		case RES_LOGIN_KO: {
			JOptionPane.showMessageDialog(this, "El usuario o la contraseña son invalidos o ya está loggeado");
			break;
		}
		case RES_LOGIN_OK: {
			JOptionPane.showMessageDialog(this, "Logueado con exito");
			break;
		}

		case RES_REGISTER_OK: {
			JOptionPane.showMessageDialog(this, "Usuario registrado con exito");
			break;
		}
		case RES_REGISTER_KO: {
			JOptionPane.showMessageDialog(this, "El nombre de usuario ya esta en uso");
			break;
		}
		default:
			break;
		}
	}
}