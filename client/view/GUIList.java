package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Comparator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.TitledBorder;

import utils.Pair;

public class GUIList extends JPanel implements IGUI {

	private JPanel result;
	private JButton ListarUsers;
	private JPanel main;
	private JTable tabla;

	private ArrayList<Pair<String, Boolean>> data;

	public GUIList() {
		initGUI();
	}

	private void initGUI() {
		this.setLayout(new BorderLayout());
		main = new JPanel(new BorderLayout());

		TitledBorder titulo = new TitledBorder("Torrente List");
		titulo.setBorder(BorderFactory.createLineBorder(Color.white, 4));
		main.setBorder(titulo);

		JPanel division = new JPanel(new GridLayout(2, 1, 10, 10));
		JPanel alloptions = new JPanel();

		JPanel options = new JPanel(new GridLayout(5, 2));
		options.setPreferredSize(new Dimension(500, 200));

		result = new JPanel(new BorderLayout());

		alloptions.add(options);
		division.add(alloptions);
		division.add(result);

		ListarUsers = new JButton("List users");

		ListarUsers.addActionListener(e -> {
			result.removeAll();
			try {
				Controller.getInstancia().accion(Evento.LIST_USERS, null);
			} catch (Exception e1) {
			}
			result.revalidate();
		});
		options.add(ListarUsers);
		main.add(division);
		this.add(main, BorderLayout.CENTER);
	}

	@Override
	public void actualizar(Evento evento, Object datos) {
		switch (evento) {
		case RES_LIST_USERS_OK: {
			if (datos == null)
				JOptionPane.showMessageDialog(this, "No users");
			else {
				data = (ArrayList<Pair<String, Boolean>>) datos;
				data.sort(Comparator.comparing((Pair<String, Boolean> p) -> p.value(), Comparator.reverseOrder())
						.thenComparing(p -> p.key()));
				tabla = new JTable(new UsersTable((data)));
				JScrollPane panel = new JScrollPane(tabla);

				panel.setAutoscrolls(false);
				result.add(panel, BorderLayout.CENTER);
			}

			break;
		}
		case RES_LIST_USERS_KO: {
			ViewUtils.showErrorMsg("Couldn't retrieve users");
			break;
		}
		default:
			break;
		}
	}
}