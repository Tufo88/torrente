package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;

import utils.tData;

public class GUIDownload extends JPanel implements IGUI {

	private JPanel result;
	private JButton ListarFiles;
	private JPanel main;
	private JTable tabla;

	private ArrayList<tData> data;

	public GUIDownload() {
		initGUI();
	}

	private void initGUI() {
		this.setLayout(new BorderLayout());
		main = new JPanel(new BorderLayout());

		TitledBorder titulo = new TitledBorder("Torrente Download");
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

		ListarFiles = new JButton("List Files");

		ListarFiles.addActionListener(e -> {
			result.removeAll();
			try {
				Controller.getInstancia().accion(Evento.LIST_FILES, null);
			} catch (Exception e1) {

			}
			result.revalidate();
		});
		options.add(ListarFiles);
		main.add(division);
		this.add(main, BorderLayout.CENTER);
	}

	@Override
	public void actualizar(Evento evento, Object datos) {
		switch (evento) {
		case RES_LIST_OK: {
			if (datos == null)
				JOptionPane.showMessageDialog(this, "No files");
			else {
				data = (ArrayList<tData>) datos;
				tabla = new JTable(new FileTable((data)));
				tabla.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
				JScrollPane panel = new JScrollPane(tabla);

				panel.setAutoscrolls(false);
				result.add(panel, BorderLayout.CENTER);
				JButton downloadButton = new JButton("Download");
				downloadButton.addActionListener(e -> {
					int[] rows = tabla.getSelectedRows();

					if (rows.length == 0) {
						ViewUtils.showErrorMsg("No files have been selected");
						return;
					}

					ArrayList<tData> toSend = new ArrayList<>();
					for (int row : rows) {
						toSend.add(data.get(row));
					}

					try {

						// descargamos en otro thread
						new Thread(() -> {
							try {
								Controller.getInstancia().accion(Evento.DOWNLOAD_FILES, toSend);
							} catch (Exception e1) {
								e1.printStackTrace();
							}
						}).start();
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					tabla.clearSelection();
				});
				result.add(downloadButton, BorderLayout.SOUTH);
			}

			break;
		}
		case RES_LIST_KO: {
			JOptionPane.showMessageDialog(this, "Couldn't retrieve files", "ERROR", JOptionPane.ERROR_MESSAGE);
			break;
		}
		case RES_DOWNLOAD_OK: {
			JOptionPane.showMessageDialog(this, "Files downloaded properly");
			break;

		}
		case RES_DOWNLOAD_KO: {
			JOptionPane.showMessageDialog(this, "Couldn't download files", "ERROR", JOptionPane.ERROR_MESSAGE);
			break;
		}
		default:
			break;
		}

	}
}