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

package com.sqleo.querybuilder;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import com.sqleo.common.util.I18n;
import com.sqleo.environment.Application;
import com.sqleo.environment.ctrl.content.AbstractActionContent;
import com.sqleo.environment.ctrl.define.TableMetaData;
import com.sqleo.environment.ctrl.explorer.AbstractViewObjects;
import com.sqleo.environment.mdi.ClientContent;
import com.sqleo.querybuilder.syntax.QueryTokens;


public class DiagramEntity extends DiagramAbstractEntity
{
	private QueryTokens.Table querytoken;
	private static ImageIcon icon;

	DiagramEntity(QueryBuilder builder,QueryTokens.Table qtoken)
	{
		super(builder);
		
		if (icon == null) icon = new javax.swing.ImageIcon(getClass().getResource("/images/database_table.png"));
		getHeaderMenu().setIcon(icon);
		
		getHeaderMenu().addSeparator();
		getHeaderMenu().add(new ActionOpenAllForeignTables());
		getHeaderMenu().add(new ActionOpenAllPrimaryTables());
		getHeaderMenu().addSeparator();
		getHeaderMenu().add(new ActionReferences());
		getHeaderMenu().add(new ActionShowContent(builder.getConnectionHandlerKey()));
		
		setQueryToken(qtoken);
	}
	protected class ActionShowContent extends AbstractActionContent
	{
		private String keych;
		ActionShowContent(String keych){this.putValue(NAME,"show content");this.keych = keych;}
		
		protected TableMetaData getTableMetaData()
		{
			return new TableMetaData(keych, querytoken.getSchema(), querytoken.getName(), null);
		}
		
		protected void onActionPerformed(int records, int option)
		{
			if((option != JOptionPane.YES_OPTION && option != JOptionPane.NO_OPTION)
			|| (records == 0 && option == JOptionPane.NO_OPTION)) return;
			
			boolean retrieve = records > 0 && option == JOptionPane.YES_OPTION;
			
			ClientContent client = new ClientContent(this.getTableMetaData(),retrieve);
			client.setTitle(ClientContent.DEFAULT_TITLE+" : " + this.getTableMetaData() + " : " + this.getTableMetaData().getHandlerKey());
			
			Application.window.add(client);
		}
		
		protected int showConfirmDialog(int records)
		{
			if(records == 0)
			{
				String message = this.getDefaultMessage(records) + "\ndo you want continue?";
				return JOptionPane.showConfirmDialog(Application.window,message,"show content",JOptionPane.YES_NO_OPTION);
			}
			else
			{
				String message = this.getDefaultMessage(records) + "\ndo you want retrieve?";
				return JOptionPane.showConfirmDialog(Application.window,message,"show content",JOptionPane.YES_NO_CANCEL_OPTION);
			}
		}
	}

	
	void onCreate()
	{
		builder.browser.addFromClause(querytoken);
		doFlush();
	}
	
	void onDestroy()
	{
		fireDeselectAll();
		builder.diagram.removeAllRelation(this);
		builder.browser.removeFromClause(querytoken);
	}
	
	DiagramField addField(int ordinalPosition, String label, Object key)
	{
		DiagramField df = new DiagramField(this,label,key!=null);
		df.position = ordinalPosition;
		
		QueryTokens.Column ctoken = new QueryTokens.Column(querytoken,label);
		df.setQueryToken(ctoken);
		
		addField(df);
		return df;
	}

	public QueryTokens.Table getQueryToken()
	{
		return querytoken;
	}

	public void setQueryToken(QueryTokens.Table querytoken)
	{
		this.querytoken = querytoken;
		
		getHeaderMenu().setText(querytoken.getReference());
		getHeaderMenu().setToolTipText(querytoken.isAliasSet() ? querytoken.toString() : null);
		
		pack();
	}	
	
//	/////////////////////////////////////////////////////////////////////////////
//	Menu Actions
//	/////////////////////////////////////////////////////////////////////////////
	private class ActionOpenAllForeignTables extends AbstractAction
	{
		private ActionOpenAllForeignTables()
		{
			super(I18n.getString("querybuilder.menu.openAllForeignTables","open all foreign tables"));
		}
		
		public void actionPerformed(ActionEvent e)
		{
			DiagramLoader.run(DiagramLoader.ALL_FOREIGN_TABLES, DiagramEntity.this.builder, DiagramEntity.this.getQueryToken(), true);
		}
	}
	
	private class ActionOpenAllPrimaryTables extends AbstractAction
	{
		private ActionOpenAllPrimaryTables()
		{
			super(I18n.getString("querybuilder.menu.openAllPrimaryTables","open all primary tables"));
		}
		
		public void actionPerformed(ActionEvent e)
		{
			DiagramLoader.run(DiagramLoader.ALL_PRIMARY_TABLES, DiagramEntity.this.builder, DiagramEntity.this.getQueryToken(), true);
		}
	}
	
	private class ActionReferences extends AbstractAction
	{
		private ActionReferences()
		{
			super(I18n.getString("querybuilder.menu.references","references..."));
		}
		
		public void actionPerformed(ActionEvent e)
		{
			new MaskReferences(DiagramEntity.this,DiagramEntity.this.builder).showDialog();
		}
	}
}