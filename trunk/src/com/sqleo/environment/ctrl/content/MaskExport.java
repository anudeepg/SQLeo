/*
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

package com.sqleo.environment.ctrl.content;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.Types;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import com.sqleo.common.gui.BorderLayoutPanel;
import com.sqleo.common.util.Text;
import com.sqleo.environment.Application;


public class MaskExport extends AbstractMaskPerform
{
	private AbstractChoice eChoice;

	public void setEnabled(boolean b)
	{
		super.setEnabled(b);
		for(int i=0; i<eChoice.getComponentCount();i++)
			eChoice.getComponent(i).setEnabled(b);
	}
	
	void setType(short type, String tname, String fname)
	{
		if(eChoice!=null) remove(eChoice);
		
		progress.setValue(0);
		progress.setMaximum(0);
		
		if(type == WEB)
		{
			if(!fname.endsWith(".htm") && !fname.endsWith(".html")) fname = fname + ".html"; 
			setComponentCenter(eChoice = new WebChoice());
		}
		else if(type == SQL)
		{
			if(!fname.endsWith(".sql")) fname = fname + ".sql"; 
			setComponentCenter(eChoice = new SqlChoice(tname));
		}
		else if(type == TXT)
		{
			if(!fname.endsWith(".txt")) fname = fname + ".txt"; 
			setComponentCenter(eChoice = new TxtChoice());
		}
			
		lblFile.setText("file: " + fname);
	}	
//	-----------------------------------------------------------------------------------------
//	?????????????????????????????????????????????????????????????????????????????????????????
//	-----------------------------------------------------------------------------------------
	void init()
	{
		super.init();
		
		progress.setValue(0);
		progress.setMaximum(eChoice.getLastRow() - eChoice.getFirstRow() + 1);
		
		eChoice.open();
	}
	
	void next()
	{
		eChoice.handle(view.getValues(progress.getValue() + eChoice.getFirstRow() - 1));
		progress.setValue(progress.getValue()+1);
	}
	
	boolean finished()
	{
		if(progress.getValue() == progress.getMaximum())
		{
			eChoice.close();
			
			btnStop.setEnabled(false);
			lblMsg.setText("ready!");
			
			return true;
		}
		
		return false;
	}
//	-----------------------------------------------------------------------------------------
//	-----------------------------------------------------------------------------------------
	private abstract class AbstractChoice extends BorderLayoutPanel
	{
		private PrintStream stream;
		
		JRadioButton rbAll;
		JRadioButton rbBlock;
		JRadioButton rbUser;
		
		JTextField txtInterval;

		AbstractChoice()
		{
			setBorder(new TitledBorder("options"));
			initComponents();
		}
		
		void initComponents()
		{
			JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT));
			setComponentSouth(pnl);

			pnl.add(new JLabel("records:"));
			pnl.add(rbAll	= new JRadioButton("all",true));
			pnl.add(rbBlock	= new JRadioButton("current block"));
			pnl.add(rbUser	= new JRadioButton("define:"));
			pnl.add(txtInterval = new JTextField("1..",8));
			txtInterval.setEditable(false);
			txtInterval.setEnabled(false);
			
			ButtonGroup bg = new ButtonGroup();
			ItemListener il = new ItemListener()
			{
				public void itemStateChanged(ItemEvent e)
				{
					txtInterval.setEditable(rbUser.isSelected());
					txtInterval.setEnabled(rbUser.isSelected());
					
					if(rbAll.isSelected())
					{
						txtInterval.setText("1..");
					}
					else if(rbBlock.isSelected())
					{
						int last = view.getBlock() * ContentModel.MAX_BLOCK_RECORDS;
						int first = last - ContentModel.MAX_BLOCK_RECORDS;
						if(first == 0) first = 1;
						if(last > view.getFlatRowCount()) last = view.getFlatRowCount() -1;
						
						txtInterval.setText(first + ".." + last);
					}
				}
			};
			
			bg.add(rbAll);			
			bg.add(rbBlock);
			bg.add(rbUser);
								
			rbAll.addItemListener(il);
			rbBlock.addItemListener(il);
			rbUser.addItemListener(il);
		}

		int getFirstRow()
		{
			String interval = eChoice.txtInterval.getText();
			int pos = interval.indexOf("..");
						
			return Integer.valueOf(interval.substring(0,pos)).intValue();
		}
		
		int getLastRow()
		{
			String interval = eChoice.txtInterval.getText();
			int pos = interval.indexOf("..") + 2;
			
			return pos < interval.length() ? Integer.valueOf(interval.substring(pos)).intValue() : view.getFlatRowCount();
		}
		
		void open()
		{
			try
			{
				stream = new PrintStream(new FileOutputStream(MaskExport.this.lblFile.getText().substring(6)));
			}
			catch (FileNotFoundException e)
			{
				Application.println(e,true);
			}
		}
		
		abstract void handle(Object[] vals);
		
		void close()
		{
			stream.close();
		}
		
		void print(String s)
		{
			stream.print(s);
		}
		
		void println(String s)
		{
			stream.println(s);
		}
	}

	private class WebChoice extends AbstractChoice
	{
		JCheckBox cbxHeader;
		
		void initComponents()
		{
			JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT));
			pnl.add(cbxHeader = new JCheckBox("with header"));
			setComponentCenter(pnl);
			
			super.initComponents();
		}

		void open()
		{
			super.open();
			println("<html><body><table border=1>");
			
			if(cbxHeader.isSelected())
			{
				print("<tr>");
				for(int col=0; col<view.getColumnCount(); col++)
				{
					print("<th>" + view.getColumnName(col) + "</th>");
				}		
				println("</tr>");
			}
		}

		void handle(Object[] vals)
		{
			print("<tr>");
			for(int i=0; i<vals.length; i++)
			{
				String val = vals[i] == null ? "null" : vals[i].toString();
				print("<td>" + val + "</td>");
			}		
			println("</tr>");
		}

		void close()
		{
			println("</table></body></html>");
			super.close();
		}
	}

	private class SqlChoice extends AbstractChoice
	{
		JCheckBox cbxDelete;
		JTextField txtTable;
		
		String insert = null;
		
		SqlChoice(String tname)
		{
			super();
			txtTable.setText(tname);
		}
		
		void initComponents()
		{
			JPanel pnl1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
			pnl1.add(new JLabel("table name:"));
			pnl1.add(txtTable = new JTextField(10));			
			
			cbxDelete = new JCheckBox("with delete statement");
			
			JPanel pnl2 = new JPanel(new GridLayout(2,1));
			pnl2.add(pnl1);
			pnl2.add(cbxDelete);

			JPanel pnl3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
			setComponentCenter(pnl3);
			pnl3.add(pnl2);

			super.initComponents();
		}

		void open()
		{
			super.open();
			
			if(cbxDelete.isSelected())
			{
				println("DELETE FROM " + txtTable.getText() + ";");
			}
			
			StringBuffer buffer = new StringBuffer("INSERT INTO " + txtTable.getText() + " (");
			for(int col=0; col<view.getColumnCount(); col++)
			{
				buffer.append(view.getColumnName(col) + ",");
			}
			buffer.deleteCharAt(buffer.length()-1);
			insert = buffer.toString() + ")";
		}

		void handle(Object[] vals)
		{
			StringBuffer buffer = new StringBuffer();
			for(int i=0; i<vals.length; i++)
			{
//				String val = vals[i] == null ? "null" : vals[i].toString();
				buffer.append(toSQLValue(vals[i],i) + ",");
			}
			buffer.deleteCharAt(buffer.length()-1);		
			println(insert + " VALUES (" + buffer.toString() + ");");
		}
		
		private String toSQLValue(Object value,int col)
		{
			if(value==null) return "null";
		
			switch(MaskExport.this.view.getColumnType(col))
			{
				case Types.CHAR:
				case Types.VARCHAR:
					value = Text.replaceText(value.toString(),"\'","\\\'");
					return "'" + value.toString() + "'";
				case Types.DATE:
					return "{d '" + value.toString() + "'}";
				case Types.TIME:
					return "{t '" + value.toString() + "'}";
				case Types.TIMESTAMP:
					return "{ts '" + value.toString() + "'}";
				default:
					return value.toString();
			}
		}
	}

	private class TxtChoice extends AbstractChoice
	{
		JCheckBox cbxHeader;
		JCheckBox cbxNull;
		JCheckBox cbxTrim;
		
		JRadioButton rbTab;
		JRadioButton rbOther;
		
		JTextField txtDelimiter;
		
		void initComponents()
		{
			JPanel pnl1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
			pnl1.add(cbxHeader = new JCheckBox("with header"));
			pnl1.add(cbxNull = new JCheckBox("null if blanks"));
			pnl1.add(cbxTrim = new JCheckBox("trim value"));
			
			JPanel pnl2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
			pnl2.add(new JLabel("delimiter:"));
			pnl2.add(rbTab = new JRadioButton("tab",true));
			pnl2.add(rbOther = new JRadioButton("other"));
			pnl2.add(txtDelimiter = new JTextField(";",5));
			txtDelimiter.setEditable(false);
			txtDelimiter.setEnabled(false);

			JPanel pnl3 = new JPanel(new GridLayout(2,1));
			pnl3.add(pnl1);
			pnl3.add(pnl2);

			JPanel pnl4 = new JPanel(new FlowLayout(FlowLayout.LEFT));
			setComponentCenter(pnl4);
			pnl4.add(pnl3);
			
			ButtonGroup bg = new ButtonGroup();
			bg.add(rbTab);
			bg.add(rbOther);

			rbTab.addItemListener(new ItemListener()
			{
				public void itemStateChanged(ItemEvent e)
				{
					txtDelimiter.setEditable(!rbTab.isSelected());
					txtDelimiter.setEnabled(!rbTab.isSelected());
				}
			});
			
			super.initComponents();			
		}
		
		private String getDelimiter()
		{
			if(rbTab.isSelected()) return "\t";
			return txtDelimiter.getText();
		}

		void open()
		{
			super.open();
			
			if(cbxHeader.isSelected())
			{
				StringBuffer buffer = new StringBuffer();
				for(int col=0; col<view.getColumnCount(); col++)
				{
					buffer.append(view.getColumnName(col) + getDelimiter());
				}
				if(buffer.length() > 0) buffer.deleteCharAt(buffer.length()-1);
				println(buffer.toString());
			}
		}

		void handle(Object[] vals)
		{
			StringBuffer buffer = new StringBuffer();
			for(int i=0; i<vals.length; i++)
			{
				String val = vals[i] == null ? "null" : vals[i].toString();
				buffer.append(val + getDelimiter());
			}
			if(buffer.length() > 0) buffer.deleteCharAt(buffer.length()-1);
			println(buffer.toString());
		}
	}
}