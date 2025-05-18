package view;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import utils.tData;

public class FileTable extends AbstractTableModel {

	private List<tData> files;
	private tData file;

	private String[] columnNames = { "File Name", "File Size (KB)", "MD5 Checksum" };

	public FileTable(List<tData> files) {
		this.files = files;
	}

	public FileTable(tData file) {
		this.file = file;
		this.files = null;
	}

	@Override
	public int getRowCount() {
		if (files != null)
			return files.size();

		return 0;
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public String getColumnName(int column) {
		return columnNames[column];
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (files != null) {
			file = files.get(rowIndex);
		}

		switch (columnIndex) {
		case 0: {
			return file.getFileName();
		}
		case 1: {
			return String.format("%.2f", file.getFileSize() / (double) 1024);
		}
		case 2: {
			return file.getFileHash();
		}
		}
		return null;
	}

}