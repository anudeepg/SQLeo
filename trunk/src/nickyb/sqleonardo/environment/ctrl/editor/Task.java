/*
 *
 * Modified by SQLeo Visual Query Builder :: java database frontend with join definitions
 * Copyright (C) 2012 anudeepgade@users.sourceforge.net
 * 
 * SQLeonardo :: java database frontend
 * Copyright (C) 2004 nickyb@users.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package nickyb.sqleonardo.environment.ctrl.editor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.NumberFormat;
import java.util.Arrays;

import nickyb.sqleonardo.common.jdbc.ConnectionAssistant;
import nickyb.sqleonardo.common.jdbc.ConnectionHandler;
import nickyb.sqleonardo.environment.Preferences;

public class Task implements Runnable {
	private static final int MAX_DISPLAY_SIZE = 50;

	private _TaskSource source;
	private _TaskTarget target;

	private int limit;

	private Statement stmt = null;
	private ResultSet rs = null;

	public Task(_TaskSource source, _TaskTarget target, int limit) {
		this.source = source;
		this.target = target;
		this.limit = limit;
	}

	@Override
	public void run() {
		String message = null;

		try {
			String syntax = source.getSyntax().trim();
			if (ConnectionAssistant.hasHandler(source.getHandlerKey())) {
				if (syntax.length() > 6) {
					ConnectionHandler ch = ConnectionAssistant
							.getHandler(source.getHandlerKey());
					stmt = ch.get().createStatement();
					stmt.setFetchSize(limit);
					stmt.setMaxRows(limit);

					String sqlcmd = syntax.toUpperCase().substring(0, 6);
					if (sqlcmd.equals("SELECT")) {
						rs = stmt.executeQuery(syntax);
						printSelect();
						rs.close();
					} else {
						rs = null;
						int rows = stmt.executeUpdate(syntax);

						if (sqlcmd.startsWith("DELETE")
								|| sqlcmd.startsWith("INSERT")
								|| sqlcmd.startsWith("UPDATE")) {
							target.write(rows + " row(s) affected");
							ch.setUncommittedTransactionExists(true && !ConnectionAssistant.getAutoCommitPrefered());
						} else {
							target.write("command has been executed successfully");
						}
					}
					stmt.close();

					if (!target.continueRun()) {
						message = "execution has been stopped";
					}
				}
			} else {
				message = "no connection!";
			}

			target.onTaskFinished(message, false);
		} catch (SQLException sqle) {
			target.onTaskFinished(sqle.toString(), true);
		}
	}

	private void printSelect() throws SQLException {
		if (rs == null) {
			return;
		}

		int maxSize = Preferences.getInteger("editor.maxcolsize",
				MAX_DISPLAY_SIZE);
		long started = System.currentTimeMillis();

		// System.out.println("reading...");

		StringBuffer header = new StringBuffer("| ");
		StringBuffer divider = new StringBuffer("+-");

		int columnDisplaySize[] = new int[getColumnCount()];
		for (int i = 1; i <= getColumnCount(); i++) {
			header.append(getColumnLabel(i));

			char[] filler = new char[getColumnLabel(i).length()];
			Arrays.fill(filler, '-');
			divider.append(filler);

			columnDisplaySize[i - 1] = getColumnDisplaySize(i);
			int diff = columnDisplaySize[i - 1] - getColumnLabel(i).length();

			if (diff > 0) {
				if (diff > maxSize) {
					diff = maxSize - getColumnLabel(i).length();
					columnDisplaySize[i - 1] = maxSize;
				}

				filler = new char[diff];
				Arrays.fill(filler, ' ');
				header.append(filler);

				Arrays.fill(filler, '-');
				divider.append(filler);
			} else {
				columnDisplaySize[i - 1] = getColumnLabel(i).length();
			}

			header.append(" | ");
			divider.append("-+-");
		}

		divider.deleteCharAt(divider.length() - 1);
		header.deleteCharAt(header.length() - 1);

		target.write(divider.toString() + "\n");
		target.write(header.toString() + "\n");
		target.write(divider.toString() + "\n");

		int bytes = 0;
		int rowcount = 0;
		while (rs.next() && target.continueRun()) {
			rowcount++;
			// System.out.println("reading record " + rowcount + " , bytes " +
			// bytes);

			StringBuffer row = new StringBuffer("| ");

			for (int i = 1; i <= getColumnCount(); i++) {
				String value = rs.getString(i);
				if (value == null) {
					value = new String();
				}

				bytes += value.length();

				int diff = columnDisplaySize[i - 1] - value.length();
				if (diff > 0) {
					char[] filler = new char[diff];
					Arrays.fill(filler, ' ');
					row.append(value + new String(filler));
				} else if (diff < 0) {
					value = value.substring(0, columnDisplaySize[i - 1] - 3);
					row.append(value + "...");
				} else {
					row.append(value);
				}

				row.append(" | ");
			}
			row.deleteCharAt(row.length() - 1);
			target.write(row.toString() + "\n");
		}

		long ended = System.currentTimeMillis();
		long millis = ended - started;
		double seconds = (double) millis / (double) 1000;

		if (rowcount > 0) {
			target.write(divider.toString() + "\n");
		}
		target.write("record(s): "
				+ NumberFormat.getInstance().format(rowcount) + " [ seconds: "
				+ NumberFormat.getInstance().format(seconds) + " bytes: "
				+ NumberFormat.getInstance().format(bytes) + " ]");
	}

	public int getColumnCount() throws SQLException {
		return rs.getMetaData().getColumnCount();
	}

	public int getColumnDisplaySize(int index) throws SQLException {
		int type = rs.getMetaData().getColumnType(index);
		if (type == Types.TIMESTAMP) {
			return 21;
		}

		return rs.getMetaData().getColumnDisplaySize(index);
	}

	public String getColumnLabel(int index) throws SQLException {
		return rs.getMetaData().getColumnLabel(index);
	}
}
