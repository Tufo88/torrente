package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.TitledBorder;

import utils.tData;

public class GUIUpload extends JPanel implements IGUI {

	private JFileChooser _fc;

	private JPanel result;
	private JButton SelectFiles;
	private JPanel main;

	public GUIUpload() {
		initGUI();
	}

	private void initGUI() {
		this.setLayout(new BorderLayout());
		main = new JPanel(new BorderLayout());

		TitledBorder titulo = new TitledBorder("Torrente Upload");
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

		_fc = new JFileChooser();
		_fc.setMultiSelectionEnabled(true);

		SelectFiles = new JButton("Select Files");
		SelectFiles.addActionListener(e -> {
			result.removeAll();
			try {
				int result = _fc.showOpenDialog(ViewUtils.getWindow(this));
				if (result == JFileChooser.APPROVE_OPTION) {
					File[] files = _fc.getSelectedFiles();

					try {
						Controller.getInstancia().accion(Evento.UPLOAD_FILES, files);
					} catch (Exception ex) {
						ViewUtils.showErrorMsg(ex.getLocalizedMessage());
					}

				}
			} catch (Exception e1) {
			}
			result.revalidate();
		});
		options.add(SelectFiles);
		main.add(division);
		this.add(main, BorderLayout.CENTER);
	}

	@Override
	public void actualizar(Evento evento, Object datos) {
		switch (evento) {

		case RES_UPLOAD_OK:
			if (datos == null)
				JOptionPane.showMessageDialog(this, "No files");
			else {
				JScrollPane panel = new JScrollPane(new JTable(new FileTable((List<tData>) datos)));
				panel.setAutoscrolls(false);
				result.add(panel, BorderLayout.CENTER);
				JOptionPane.showMessageDialog(this, "Files uploaded properly");
			}

			break;

		case GUI_UPLOAD:
			if (datos == null)
				JOptionPane.showMessageDialog(this, "No files");
			else {
				JScrollPane panel = new JScrollPane(new JTable(new FileTable((List<tData>) datos)));
				panel.setAutoscrolls(false);
				result.add(panel, BorderLayout.CENTER);
			}

			break;
		case RES_UPLOAD_KO: {
			JOptionPane.showMessageDialog(this, "Couldn't upload files", "ERROR", JOptionPane.ERROR_MESSAGE);
			break;
		}

		default:
			break;
		}

	}
}