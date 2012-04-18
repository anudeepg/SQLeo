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

package com.sqleo.environment.mdi;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import com.sqleo.common.jdbc.ConnectionAssistant;
import com.sqleo.common.jdbc.ConnectionHandler;
import com.sqleo.common.util.I18n;
import com.sqleo.environment.Application;
import com.sqleo.environment.Preferences;
import com.sqleo.environment._Constants;
import com.sqleo.environment.ctrl.content.AbstractActionContent;
import com.sqleo.environment.ctrl.define.TableMetaData;
import com.sqleo.querybuilder.DiagramLayout;
import com.sqleo.querybuilder.syntax.QueryExpression;
import com.sqleo.querybuilder.syntax.QueryTokens;


public abstract class MDIActions implements _Constants
{
    public static abstract class AbstractBase extends AbstractAction
    {
		public AbstractBase(){super();}
		public AbstractBase(String text){super(text);}
    	
		protected void setAccelerator(KeyStroke stroke)
		{
			putValue(ACCELERATOR_KEY,stroke);
		}

        protected void setIcon(String iconkey)
        {
        	putValue(SMALL_ICON,Application.resources.getIcon(iconkey));
        }
        
        protected void setText(String text)
        {
        	putValue(NAME,text);
        }
        
        protected void setTooltip(String text)
        {
        	putValue(SHORT_DESCRIPTION,text);
        }
    }

	public final static class Dummy extends AbstractAction
	{
		public Dummy(String text){super(text);}
		
		public void actionPerformed(ActionEvent ae)
		{
			Application.alert(Application.PROGRAM,"not implemented!");
		}
	}
    
	public static class NewQuery extends AbstractBase
	{
		public NewQuery(){super(I18n.getString("application.menu.newQuery","new query"));}
		public void actionPerformed(ActionEvent ae)
		{
			if(!ConnectionAssistant.getHandlers().isEmpty())
			{
				Object keycah = null;
				if(ConnectionAssistant.getHandlers().size() > 1)
					keycah = JOptionPane.showInputDialog(Application.window,I18n.getString("application.message.useConnection","use connection:"),Application.PROGRAM,JOptionPane.PLAIN_MESSAGE,null,ConnectionAssistant.getHandlers().toArray(),null);
				else
					keycah = ConnectionAssistant.getHandlers().toArray()[0];
				
				if(keycah != null)
				{
					DiagramLayout dl = new DiagramLayout();
					if(!Preferences.getBoolean("querybuilder.use-schema"))
					{
						ConnectionHandler ch = ConnectionAssistant.getHandler(keycah.toString());
						ArrayList schemas = (ArrayList)ch.getObject("$schema_names");
						if(schemas.size()>0)
						{
							Object schema = JOptionPane.showInputDialog(Application.window,I18n.getString("application.message.schema","schema:"),Application.PROGRAM,JOptionPane.PLAIN_MESSAGE,null,schemas.toArray(),null);
							if(schema == null) return;
							dl.getQueryModel().setSchema(schema.toString());
						}
					}
				
					ClientQueryBuilder cqb = new ClientQueryBuilder(keycah.toString());
					cqb.setDiagramLayout(dl);
					Application.window.add(cqb);
				}
			}
			else
				Application.alert(Application.PROGRAM,I18n.getString("application.message.noConnection","No connection!"));
		}
	}
    
	public static class LoadQuery extends AbstractBase
	{
		public LoadQuery(){super(I18n.getString("application.menu.loadQuery","load query..."));}
		
		private void setSchema(String schema, QueryExpression qe)
		{
			if(qe == null) return;
			
			QueryTokens._Base[] tokens = qe.getQuerySpecification().getSelectList();
			for(int i=0; i<tokens.length; i++)
			{
				if(tokens[i] instanceof QueryTokens.Column)
				{
					((QueryTokens.Column)tokens[i]).getTable().setSchema(schema);
				}
			}
			
			tokens = qe.getQuerySpecification().getFromClause();
			for(int i=0; i<tokens.length; i++)
			{
				if(tokens[i] instanceof QueryTokens.Join)
				{
					((QueryTokens.Join)tokens[i]).getPrimary().getTable().setSchema(schema);
					((QueryTokens.Join)tokens[i]).getForeign().getTable().setSchema(schema);
				}
				else
					((QueryTokens.Table)tokens[i]).setSchema(schema);
			}
			
			setSchema(schema,qe.getUnion());
		}
		
		public void actionPerformed(ActionEvent ae)
		{
			Object[] ret = DialogQuery.showLoad();
			if(ret[0]!=null && ret[1]!=null && ret[2]!=null)
			{
				ClientQueryBuilder cqb = new ClientQueryBuilder(ret[2].toString());
				String fileName = ret[0].toString();
				cqb.setFileName(fileName);
				
				DiagramLayout dl = (DiagramLayout)ret[1];
				
				/* gestire schema */
				if(Preferences.getBoolean("querybuilder.use-schema"))
				{
					if(dl.getQueryModel().getSchema()==null)
					{
						if(ret[3]!=null)
						{
							int option = JOptionPane.showConfirmDialog(Application.window,"do you want to apply '" + ret[3] + "' schema on all elements?",Application.PROGRAM,JOptionPane.YES_NO_CANCEL_OPTION);
						
							if(option == JOptionPane.YES_OPTION)
								setSchema(ret[3].toString(),dl.getQueryModel().getQueryExpression());
							else if(option == JOptionPane.CANCEL_OPTION)
								return;
						}
					}
				}
				else
				{
					if(dl.getQueryModel().getSchema()==null)
					{
						if(ret[3]!=null)
						{
							dl.getQueryModel().setSchema(ret[3].toString());
							setSchema(null,dl.getQueryModel().getQueryExpression());
						}
					}
					else if(ret[3]!=null)
						dl.getQueryModel().setSchema(ret[3].toString());
				}

				Application.window.add(cqb);
				cqb.setDiagramLayout(dl);
				if(fileName.toLowerCase().endsWith(".sql")){
					cqb.getBuilder().setSelectedIndex(1);
				}
			}
		}
	}

	public static class LoadGivenQuery extends AbstractBase
	{
		public LoadGivenQuery(){super(I18n.getString("application.menu.loadQuery","load query..."));}

		public void actionPerformed(ActionEvent ae)
		{
			String fileName = ae.getActionCommand();
			if(!new File(fileName).exists()){
				Application.alert(Application.getVersion2(),"File not found : "+fileName);
				return;
			}
			if(!ConnectionAssistant.getHandlers().isEmpty())
			{
				Object keycah = null;
				if(ConnectionAssistant.getHandlers().size() > 1)
					keycah = JOptionPane.showInputDialog(Application.window,I18n.getString("application.message.useConnection","use connection:"),Application.PROGRAM,JOptionPane.PLAIN_MESSAGE,null,ConnectionAssistant.getHandlers().toArray(),null);
				else
					keycah = ConnectionAssistant.getHandlers().toArray()[0];

				if(keycah != null)
				{

					ClientQueryBuilder cqb = new ClientQueryBuilder(keycah.toString());
					cqb.setFileName(fileName);

					DiagramLayout dl = DialogQuery.getDiagramLayoutForFile(fileName);
					if(!Preferences.getBoolean("querybuilder.use-schema"))
					{
						ConnectionHandler ch = ConnectionAssistant.getHandler(keycah.toString());
						ArrayList schemas = (ArrayList)ch.getObject("$schema_names");
						if(schemas.size()>0)
						{
							Object schema = JOptionPane.showInputDialog(Application.window,I18n.getString("application.message.schema","schema:"),Application.PROGRAM,JOptionPane.PLAIN_MESSAGE,null,schemas.toArray(),null);
							if(schema == null) return;
							dl.getQueryModel().setSchema(schema.toString());
						}
					}
					Application.window.menubar.addMenuItemAtFirst(fileName);
					Application.window.add(cqb);
					cqb.setDiagramLayout(dl);
					if(fileName.toLowerCase().endsWith(".sql")){
						cqb.getBuilder().setSelectedIndex(1);
					}
				}
			}else{
				Application.alert(Application.window.getTitle(),"No connections exists!");
			}
		}
	}

	public static class Exit extends AbstractBase
	{
		public Exit(){super(I18n.getString("application.menu.exit","exit"));}
        
		public void actionPerformed(ActionEvent ae)
		{
			Application.shutdown();
		}
	}

	public static class GoBack extends AbstractBase
	{
		public GoBack()
		{
			super("go back");
			setIcon(ICON_BACK);
			setTooltip("<empty>");
			setEnabled(false);
		}
        
		public void actionPerformed(ActionEvent ae)
		{
			Application.window.menubar.history.previous();
		}
	}

	public static class GoForward extends AbstractBase
	{
		public GoForward()
		{
			super("go forward");
			setIcon(ICON_FWD);
			setTooltip("<empty>");
			setEnabled(false);
		}
        
		public void actionPerformed(ActionEvent ae)
		{
			Application.window.menubar.history.next();
		}
	}

	public static class ShowContent extends AbstractActionContent
	{
		private TableMetaData tmd = null;
		
		public ShowContent(){this.putValue(NAME,I18n.getString("application.tool.content","show content..."));}
        
		public void actionPerformed(ActionEvent e)
		{
			tmd = null;
			super.actionPerformed(e);
		}
        
		protected void onActionPerformed(int records, int option)
		{
			if(option == JOptionPane.CANCEL_OPTION || (records == 0 && option == JOptionPane.NO_OPTION)) return;
			boolean retrieve = records > 0 && option == JOptionPane.YES_OPTION;
			
			ClientContent client = new ClientContent(this.getTableMetaData(),retrieve);
			client.setTitle(ClientContent.DEFAULT_TITLE+" : "+ this.getTableMetaData() + " : " + this.getTableMetaData().getHandlerKey());

			Application.window.add(client);
		}

		protected TableMetaData getTableMetaData()
		{
			if(tmd == null)
			{
				Object[] ret = DialogQuickObject.show("show content");
				if(ret != null)
					tmd = new TableMetaData(ret[0].toString(), ret[1] == null ? null : ret[1].toString(), ret[2].toString());
			}
						
			return tmd;
		}
	}
	
	public static class ShowDefinition extends AbstractBase
	{
		public ShowDefinition(){super(I18n.getString("application.tool.definition","show definition..."));}
        
		public void actionPerformed(ActionEvent ae)
		{
			Object[] ret = DialogQuickObject.show("show definition");
			if(ret == null) return;
			
			String schema = ret[1] == null ? null : ret[1].toString();
			Application.window.add(new ClientDefinition(ret[0].toString(), new QueryTokens.Table(schema,ret[2].toString()), "TABLE"));
		}
	}	
		
	public static class ShowPreferences extends AbstractBase
	{
		public ShowPreferences()
		{
			super("preferences...");
			setIcon(ICON_PREFERENCES);
			setTooltip("edit preferences");
		}
        
		public void actionPerformed(ActionEvent ae)
		{
			new DialogPreferences().setVisible(true);
		}
	}
    
    public static abstract class AbstractShow extends AbstractBase
    {
		public abstract String getMDIClientName();
        
        public void actionPerformed(ActionEvent ae)
        {
        	if(ae!=null) Application.window.menubar.history.enableSequence();
            Application.window.showClient(this.getMDIClientName());
        }
    }
    
    public static abstract class AbstractShowTool extends AbstractShow
    {
		public AbstractShowTool(KeyStroke ks, String iconKey)
		{
			setAccelerator(ks);
			setIcon(iconKey);
			setTooltip(this.getMDIClientName());
			setText("show " + this.getMDIClientName());
		}
		
		public void actionPerformed(ActionEvent ae)
		{
			if(!Application.window.showClient(this.getMDIClientName()))
				Application.window.add(create());
		}
		
		protected abstract MDIClient create();
    }
    
    public static class ShowMetadataExplorer extends AbstractShowTool
    {
        public ShowMetadataExplorer()
        {
			super(KeyStroke.getKeyStroke(KeyEvent.VK_1,InputEvent.CTRL_MASK),ICON_EXPLORER);
        }
        
        public String getMDIClientName()
        {
			return ClientMetadataExplorer.DEFAULT_TITLE;
        }
        
		protected MDIClient create()
		{
			return new ClientMetadataExplorer();
		}        
    }
    
	public static class ShowCommandEditor extends AbstractShowTool
	{
		public ShowCommandEditor()
		{
			super(KeyStroke.getKeyStroke(KeyEvent.VK_2,InputEvent.CTRL_MASK),ICON_EDITOR);
		}
        
		public String getMDIClientName()
		{
			return ClientCommandEditor.DEFAULT_TITLE;
		}
		
		protected MDIClient create()
		{
			return new ClientCommandEditor();
		}        
	}

	public static class ShowSchemaComparer extends AbstractShowTool
	{
		public ShowSchemaComparer()
		{
			super(KeyStroke.getKeyStroke(KeyEvent.VK_3,InputEvent.CTRL_MASK),ICON_COMPARER);
		}
        
		public String getMDIClientName()
		{
			return ClientSchemaComparer.DEFAULT_TITLE;
		}
		
		protected MDIClient create()
		{
			return new ClientSchemaComparer();
		}        
	}

	public static class CascadeClients extends AbstractBase
	{
		public CascadeClients()
		{
			super(I18n.getString("application.menu.cascade","cascade"));
		}
        
		public void actionPerformed(ActionEvent ae)
		{
			Application.window.cascadeClients();  
		}
	}
    
	public static class TileClients extends AbstractBase
	{
		public TileClients()
		{
			super(I18n.getString("application.menu.tileHorizontal","tile horizontal"));
		}
        
		public void actionPerformed(ActionEvent ae)
		{
			Application.window.tileClients();
		}
	}
    
	public static class CloseAllClients extends AbstractBase
	{
		public CloseAllClients()
		{
			super(I18n.getString("application.menu.closeAll","close all"));
		}
        
		public void actionPerformed(ActionEvent ae)
		{
			Application.window.closeAllClients();
			Application.window.menubar.history.enableActions();
		}
	}
	
	public static class About extends AbstractBase
	{
		public About(){super(I18n.getFormattedString("application.menu.about","about {0}...", new Object[]{""+Application.PROGRAM}));}
        
		public void actionPerformed(ActionEvent ae)
		{
			  new DialogAbout().setVisible(true);
		}
	}
}