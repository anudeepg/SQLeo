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

import javax.swing.JCheckBox;

import com.sqleo.common.gui.AbstractDialogWizard;
import com.sqleo.common.gui.AbstractMaskChooser;
import com.sqleo.environment.Application;
import com.sqleo.environment.Preferences;
import com.sqleo.environment.ctrl.ContentPane;
import com.sqleo.environment.mdi.DefaultMaskChooser;


public class DialogStream extends AbstractDialogWizard
{
	private String tname = null;
	private ContentView view;
	
	private JCheckBox cbxClose;
	
	private AbstractMaskPerform mkp;
	private AbstractMaskChooser mkc;
	
	private TaskOwner task;
	
	private DialogStream(String title, ContentView view, String tname)
	{
		super(Application.window,title);
		
		this.tname = tname;
		this.view = view;
		
		cbxClose = new JCheckBox("close dialog when finished",Preferences.getBoolean("content.close-dialog",false));
		cbxClose.setVisible(false);		
		bar.add(cbxClose,0);
	}

	public static void showExport(ContentPane content)
	{
		String tname = null;
		new DialogStream("export", content.getView(), tname).setVisible(true);
	}
	
	public static void showImport(ContentPane content)
	{
		String tname = null;
		new DialogStream("import", content.getView(), tname).setVisible(true);
	}
	
	public void dispose()
	{
		Preferences.set("content.close-dialog",new Boolean(cbxClose.isSelected()));
		super.dispose();		
	}

	protected boolean onBack()
	{
		cbxClose.setVisible(false);
		return super.onBack();
	}

	protected boolean onNext()
	{
		cbxClose.setVisible(true);
		
		if(getStep()==0)
		{
			if(mkc.getSelectedFile()!=null)
			{
				mkp.setType(mkc.getPerformType(),tname,mkc.getSelectedFile().toString());
				mkp.setContent(view);
				
				return true;
			}
		}
		else
		{
			new Thread(task = new TaskOwner()).start();
		}
		
		return false;
	}
	
	protected void onOpen()
	{
		if(this.getTitle().equals("export"))
		{
			mkp = new MaskExport();
			mkc = new DefaultMaskChooser(AbstractMaskChooser.SAVE_DIALOG,AbstractMaskChooser.FILES_ONLY,false);
		
			mkc.addChoosableFileFilter(new SQLFilter());
			mkc.addChoosableFileFilter(new WebFilter());
			
		}
		else if(this.getTitle().equals("import"))
		{
			mkp = new MaskImport();
			mkc = new DefaultMaskChooser(AbstractMaskChooser.OPEN_DIALOG,AbstractMaskChooser.FILES_ONLY,false);
		}
		
		mkc.addChoosableFileFilter(new TXTFilter());
		
		addStep(mkc);
		addStep(mkp);
		
		getContentPane().validate();		
	}
	
	private class TaskOwner implements Runnable
	{
		public void run()
		{
			DialogStream.this.setBarEnabled(false);
			DialogStream.this.mkp.setEnabled(false);
			DialogStream.this.mkp.init();
			
			if(DialogStream.this.mkp instanceof MaskExport){
				MaskExport exporter = (MaskExport)DialogStream.this.mkp;
				if(!exporter.isExportFromGrid()){
					exporter.export();
				}else{
					while(DialogStream.this.task != null && !DialogStream.this.mkp.aborted() && !DialogStream.this.mkp.finished())
					{
						DialogStream.this.mkp.next();
					}
				}
				
			}else{
				while(DialogStream.this.task != null && !DialogStream.this.mkp.aborted() && !DialogStream.this.mkp.finished())
				{
					DialogStream.this.mkp.next();
				}
			}
			
			DialogStream.this.mkp.setEnabled(true);
			DialogStream.this.setBarEnabled(true);
			
			if(DialogStream.this.mkp.finished() && cbxClose.isSelected()) DialogStream.this.dispose();
		}
	}
	
	private static class SQLFilter extends AbstractMaskChooser.AbstractFileFilter
	{
		SQLFilter(){super("insert statements",new String[]{".sql"});}
		public short getPerformType(){return MaskExport.SQL;}
	}
	 
	private static class TXTFilter extends AbstractMaskChooser.AbstractFileFilter
	{
		TXTFilter(){super("text files",new String[]{".txt"});}
		public short getPerformType(){return MaskExport.TXT;}
	}
	
	private static class WebFilter extends AbstractMaskChooser.AbstractFileFilter
	{
		WebFilter(){super("web pages",new String[]{".htm",".html"});}
		public short getPerformType(){return MaskExport.WEB;}
	}
}