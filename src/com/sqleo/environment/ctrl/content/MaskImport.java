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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import com.sqleo.common.gui.BorderLayoutPanel;
import com.sqleo.environment.Application;


public class MaskImport extends AbstractMaskPerform
{
	private AbstractChoice iChoice;
	
	public void setEnabled(boolean b)
	{
		super.setEnabled(b);
		for(int i=0; i<iChoice.getComponentCount();i++)
			iChoice.getComponent(i).setEnabled(b);
	}
	
	void setType(short type, String tname, String fname)
	{
		if(iChoice!=null) remove(iChoice);
		
		progress.setValue(0);
		progress.setMaximum(0);
		
		if(type == TXT)
		{
			if(!fname.endsWith(".txt")) fname = fname + ".txt"; 
			setComponentCenter(iChoice = new TxtChoice());
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
		progress.setMaximum(iChoice.open());
	}
	
	void next()
	{
		String line = iChoice.nextln();
		
		Object[] rowdata = new Object[view.getColumnCount()];
		iChoice.flush(line,rowdata);
		
		view.addRow(rowdata,true);
		progress.setValue(progress.getValue() + line.length() + 1);
	}
	
	boolean finished()
	{
		if(progress.getValue() == progress.getMaximum())
		{
			iChoice.close();
			
			view.onTableChanged(true);
			view.getControl().doRefreshStatus();

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
		private FileInputStream stream;
		
		JRadioButton rbAll;
		JRadioButton rbUser;
		
		JTextField txtInterval;

		AbstractChoice()
		{
			setBorder(new TitledBorder("options"));
		}
/*		
		void initComponents()
		{
			JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT));
			setComponentSouth(pnl);

			pnl.add(new JLabel("records:"));
			pnl.add(rbAll	= new JRadioButton("all",true));
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
				}
			};
			
			bg.add(rbAll);			
			bg.add(rbUser);
								
			rbAll.addItemListener(il);
			rbUser.addItemListener(il);
		}
*/		
		int open()
		{
			try
			{
				stream = new FileInputStream(MaskImport.this.lblFile.getText().substring(6));
				return stream.available();
			}
			catch (FileNotFoundException e)
			{
				Application.println(e,true);
			}
			catch (IOException e)
			{
				Application.println(e,true);
			}
			
			return 0;
		}
		
		void close()
		{
			try
			{
				stream.close();
			}
			catch (IOException e)
			{
				Application.println(e,true);
			}
		}
		
		abstract void flush(String line, Object[] rowdata);
		
		String nextln()
		{
			try
			{
				String line = new String("");
				
				int nChar;
				while((nChar=stream.read())!=-1 && (char)nChar!='\n')
					line = line + (char)nChar;
				
				return line;
			}
			catch (IOException e)
			{
				Application.println(e,true);
			}
			
			return null;
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
		
		TxtChoice()
		{
			super();
			
			JPanel pnl1 = new JPanel(new GridLayout(3,1));
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
		}
		
		private String getDelimiter()
		{
			if(rbTab.isSelected()) return "\t";
			return txtDelimiter.getText();
		}
		
		int open()
		{
			int bytes = super.open();
			
			if(cbxHeader.isSelected())
			{
				String line = nextln();
				bytes = bytes - (line.length() + 1);				
			}
			
			return bytes;
		}
		
		void flush(String line, Object[] rowdata)
		{
			Vector vRow = new Vector();
			
			int bix = 0;
			int eix = 0;
			
			while((eix = line.indexOf(getDelimiter(),bix))!=-1)
			{
				String value = line.substring(bix,eix);
				bix = eix + getDelimiter().length();
				
				if(cbxNull.isSelected() && value.trim().length() == 0) value = null;
				if(cbxTrim.isSelected() && value!=null) value = value.trim();
				
				vRow.addElement(value);
			}
			
			if(bix < line.length()-1)
			{
				String value = line.substring(bix);
				
				if(cbxNull.isSelected() && value.trim().length() == 0) value = null;
				if(cbxTrim.isSelected() && value!=null) value = value.trim();
				
				vRow.addElement(value);
			}

			vRow.toArray(rowdata);
		}
	}
}