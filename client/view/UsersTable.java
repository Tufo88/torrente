package view;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import utils.Pair;

public class UsersTable extends AbstractTableModel {

	private List<Pair<String, Boolean>> users;
	private Pair<String, Boolean> user;

	private String[] columnNames = { "User Id", "Logged Status" };

	public UsersTable(List<Pair<String, Boolean>> users) {
		this.users = users;
	}

	public UsersTable(Pair<String, Boolean> user) {
		this.user = user;
		this.users = null;
	}

	@Override
	public int getRowCount() {
		if (users != null)
			return users.size();

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
		if (users != null) {
			user = users.get(rowIndex);
		}

		switch (columnIndex) {
		case 0: {
			return user.key();
		}
		case 1: {
			String text = "Online ✓";
			if (!user.value())
				text = "Offline X";
			return text;
		}
		}
		return null;
	}

}