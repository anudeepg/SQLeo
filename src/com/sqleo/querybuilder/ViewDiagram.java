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

package com.sqleo.querybuilder;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.dnd.DropTarget;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.DefaultDesktopManager;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;


import com.sqleo.common.gui.BorderLayoutPanel;
import com.sqleo.common.util.I18n;
import com.sqleo.querybuilder.dnd.EntityDropTargetListener;
import com.sqleo.querybuilder.syntax.DerivedTable;
import com.sqleo.querybuilder.syntax.QueryTokens;
import com.sun.image.codec.jpeg.ImageFormatException;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

public class ViewDiagram extends BorderLayoutPanel
{
	static Color BGCOLOR_DEFAULT	= Color.white;
	static Color BGCOLOR_JOINED		= new Color(225,235,224);
	static Color BGCOLOR_START_JOIN = new Color(255,230,230);
	
	private static int FRAME_OFFSET = 50;
	
	private Point nextGoodPoint = new Point(10,10);
	private Point maxCorner = new Point(0,0); 
	
	private DiagramRelation highlight;
	private DiagramRelation temporany;
	
	private InternalDiagramManager manager;
	
	private JScrollPane scroll; 
	private JDesktopPane desktop;
	private QueryBuilder builder;
        
    private JPopupMenu jPopupMenuDiagram = null;
	
	ViewDiagram(QueryBuilder builder)
	{
		this.setBuilder(builder);
		
		desktop = new JDesktopPane();
		desktop.setDesktopManager(manager = new InternalDiagramManager());
		desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
        desktop.setDropTarget(new DropTarget(this, new EntityDropTargetListener(this)) );
		
		scroll = new JScrollPane(desktop);
		scroll.getVerticalScrollBar().setUnitIncrement(25);
		setComponentCenter(scroll);
                
        jPopupMenuDiagram = new JPopupMenu();
        jPopupMenuDiagram.add(builder.getActionMap().get(QueryActions.ENTITIES_ARRANGE));
		jPopupMenuDiagram.add(builder.getActionMap().get(QueryActions.ENTITIES_PACK));
		jPopupMenuDiagram.add(builder.getActionMap().get(QueryActions.ENTITIES_REMOVE));
		jPopupMenuDiagram.addSeparator();
		jPopupMenuDiagram.add(builder.getActionMap().get(QueryActions.DIAGRAM_SAVE_AS_IMAGE));
		jPopupMenuDiagram.addSeparator();
		jPopupMenuDiagram.add(builder.getActionMap().get(QueryActions.COPY_SYNTAX));
	
        desktop.addMouseListener(new MouseAdapter()
        {
            public void mouseReleased(MouseEvent e)
            {
                if(SwingUtilities.isRightMouseButton(e))
                {
                    jPopupMenuDiagram.show((Component) e.getSource(), e.getX(), e.getY() );
                }
            }
        });
	}
	
	public Component add(Component c)
	{
		if(c instanceof DiagramAbstractEntity)
		{
			((DiagramAbstractEntity)c).doFlush();
			return desktop.add(c);
		}
		else if(c instanceof DiagramRelation)
		{
			desktop.add(((DiagramRelation)c).anchor);
			return desktop.add(c);
		}
		
		return super.add(c);
	}

	public void remove(Component c)
	{
		if(c instanceof DiagramAbstractEntity)
		{
			DiagramAbstractEntity entity = (DiagramAbstractEntity)c;
			desktop.remove(c);
			
			if(temporany!=null && entity == temporany.primaryEntity)
				temporany = null;
		}
		else if(c instanceof DiagramRelation)
		{
			desktop.remove(((DiagramRelation)c).anchor);
			desktop.remove(c);
		}
		else
			super.remove(c);
	}
	
	void addEntity(DiagramAbstractEntity item)
	{
		if(item.getWidth() + nextGoodPoint.x > this.getBounds().getWidth())
			nextGoodPoint.setLocation(10, maxCorner.y + FRAME_OFFSET);

		addEntity(item,nextGoodPoint);
	}
	
	void addEntity(DiagramAbstractEntity item, Point location)
	{
		item.setLocation(location);
		item.setVisible(true);

		manager.openFrame(item);
	}
	
	DiagramQuery getEntity(DerivedTable subquery)
	{
		DiagramAbstractEntity[] entities = this.getEntities();
		for(int i=0; i<entities.length; i++)
		{
			if(!(entities[i] instanceof DiagramQuery)) continue;
			DiagramQuery entity = (DiagramQuery)entities[i];
			
			if(subquery.getAlias().equalsIgnoreCase(entity.getQueryToken().getReference())) return entity;
		}
		return null;
	}
	
    /**
     * Returns a DiagramEntity for the given name or null is no matching entity is found...
     */
	public DiagramEntity getEntity(QueryTokens.Table table)
	{
		return getEntity(table,true);
	}
	
    /**
     * Returns a DiagramEntity for the given name or null if no matching entity is found...
     */
	DiagramEntity getEntity(String schema, String table)
	{
		QueryTokens.Table token = new QueryTokens.Table(schema,table);
		return getEntity(token,false);
	}
	
	private DiagramEntity getEntity(QueryTokens.Table token, boolean ref)
	{
		DiagramAbstractEntity[] entities = this.getEntities();
		for(int i=0; i<entities.length; i++)
		{
			if(!(entities[i] instanceof DiagramEntity)) continue;
			DiagramEntity entity = (DiagramEntity)entities[i];
			
			if(ref)
			{
				if(token.getReference().equalsIgnoreCase(entity.getQueryToken().getReference())) return entity;
			}
			else
			{
				if(token.getIdentifier().equalsIgnoreCase(entity.getQueryToken().getIdentifier())) return entity;
			}
		}	
		return null;
	}
	
	DiagramAbstractEntity[] getEntities()
	{
		JInternalFrame[] internalframes = desktop.getAllFramesInLayer(JDesktopPane.PALETTE_LAYER.intValue());
		DiagramAbstractEntity[] entities = new DiagramAbstractEntity[internalframes.length];
		
		System.arraycopy(internalframes,0,entities,0,internalframes.length);
		
		return entities;
	}
	
	int getEntityCount()
	{
		return desktop.getAllFramesInLayer(JDesktopPane.PALETTE_LAYER.intValue()).length;
	}

	void addRelation(DiagramRelation relation,QueryTokens.Join join)
	{
		relation.onCreate(getBuilder(),join);
		relation.doResize();
		
		int idx = getRelationCount();
		desktop.add(relation.anchor,0);
		desktop.add(relation,(idx*2)+1);
	}
	
	DiagramRelation getRelation(QueryTokens.Join token)
	{
		DiagramRelation[] relations = getRelations();
		
		for(int i=0; i<relations.length; i++)
		{
			if(token.getPrimary().getIdentifier().equalsIgnoreCase(relations[i].querytoken.getPrimary().getIdentifier())
			&& token.getForeign().getIdentifier().equalsIgnoreCase(relations[i].querytoken.getForeign().getIdentifier()))
				return relations[i];
		}
		
		return null;
	}
	
	DiagramRelation[] getRelations()
	{
		DiagramRelation[] relations = new DiagramRelation[getRelationCount()];
		
		if(relations.length > 0)
		{
			for(int i=0,j=0,count=desktop.getComponentCount(); i<count; i++)
			{
				Component next = desktop.getComponent(i);
				
				if(next instanceof DiagramRelation)
					relations[j++] = (DiagramRelation)next;
			}
		}
		
		return relations;
	}
	
	int getRelationCount()
	{
		return (desktop.getComponentCount() - this.getEntityCount())/2;
	}
	
	void removeRelation(DiagramRelation relation)
	{
		desktop.remove(relation.anchor);
		desktop.remove(relation);
		desktop.repaint();
		
		relation.onDestroy(getBuilder());
	}
	
	void removeAllRelation(DiagramAbstractEntity table)
	{
		DiagramRelation[] relations = getRelations();
		for(int i=0; i<relations.length; i++)
		{
			if(relations[i].primaryEntity == table || relations[i].foreignEntity == table)
				removeRelation(relations[i]);
		}
	}
	
	void setHighlight(DiagramRelation relation)
	{
		if(highlight!=null)
		{
			if(relation.equals(highlight))
			{
				//highlight = null;
				return;
			}
            else
            {
                highlight.setHighlight(false);
            }
		}
		
		highlight = relation;
		highlight.setHighlight(true);
		
		desktop.setPosition(relation.anchor,0);
		desktop.setPosition(relation,getRelationCount());
	}
	
	private boolean draggable = true;
	boolean isDragAndDropEnabled()
	{
		return draggable;
	}
	
	void setDragAndDropEnabled(boolean b)
	{
		draggable = b;
		
		DiagramAbstractEntity[] entities = this.getEntities();
		for(int i=0; i<entities.length; i++)
			entities[i].setDragAndDropEnabled(b);
		
		if(temporany!=null)
			temporany.primaryField.setBackground(temporany.primaryField.isJoined() ? BGCOLOR_JOINED : BGCOLOR_DEFAULT);
		temporany = null;
	}

    /* BAD? */
	public void join(DiagramAbstractEntity eP, DiagramField fP, DiagramAbstractEntity eF, DiagramField fF)
	{
		joinInternal(eP, fP, eF, fF, "INNER");
	}
	public void join(DiagramAbstractEntity eP, DiagramField fP, DiagramAbstractEntity eF, DiagramField fF,String joinType)
	{	
		joinInternal(eP, fP, eF, fF, joinType);
	}
	private void joinInternal(DiagramAbstractEntity eP, DiagramField fP, DiagramAbstractEntity eF, DiagramField fF,String joinType)
	{
		temporany = null;
		join(eP,fP,joinType);
		join(eF,fF,joinType);
	}
	/* BAD? */
	void join(DiagramAbstractEntity entity, DiagramField field)
	{
		joinInternal(entity, field, "INNER");
	}
	void join(DiagramAbstractEntity entity, DiagramField field,String joinType)
	{
		joinInternal(entity, field, joinType);
	}
	private void joinInternal(DiagramAbstractEntity entity, DiagramField field,String joinType)
	{
		if(temporany==null)
		{
			temporany = new DiagramRelation(this);
			temporany.primaryEntity = entity;
			temporany.primaryField = field;
			
            // Join mode colors are deprecated!
            // 15/02/2007 - nickyb
            // switch between drag&drop and click&click join mode (riabilitato con colorazione solo se C&C)
			if(!getBuilder().isDragAndDropEnabled())
				temporany.primaryField.setBackground(BGCOLOR_START_JOIN);
		}
		else if(entity==temporany.primaryEntity)
		{
			temporany.primaryField.setBackground(temporany.primaryField.isJoined() ? BGCOLOR_JOINED : BGCOLOR_DEFAULT);
			
			if(field!=temporany.primaryField)
			{
				temporany.primaryField = field;
				
                // Join mode colors are deprecated!
				// 15/02/2007 - nickyb
				// switch between drag&drop and click&click join mode (riabilitato con colorazione solo se C&C)
				if(!getBuilder().isDragAndDropEnabled())
					temporany.primaryField.setBackground(BGCOLOR_START_JOIN);
			}
			else
			{
				temporany = null;
			}
		}
		else
		{
			temporany.foreignEntity = entity;
			temporany.foreignField = field;
			
			QueryTokens.Join join = new QueryTokens.Join(QueryTokens.Join.getTypeInt(joinType),temporany.primaryField.querytoken,"=",temporany.foreignField.querytoken);
			if(getRelation(join) == null)
			{
				temporany.primaryField.joined();
				temporany.primaryField.setBackground(BGCOLOR_JOINED);
				
				temporany.foreignField.joined();
				temporany.foreignField.setBackground(BGCOLOR_JOINED);
				
				this.addRelation(temporany,join);
				temporany = null;
			}
			else
			{
				JOptionPane.showMessageDialog(this,I18n.getFormattedString("querybuilder.message.joinAlreadyExists","Join already exists: {0}", new Object[]{""+ join} ),
                                        I18n.getString("querybuilder.message.addJoin","add join"),
                                        JOptionPane.WARNING_MESSAGE);
				
				temporany.foreignEntity = null;
				temporany.foreignField = null;
			}
		}
	}

	void saveAsImage(String filename) throws ImageFormatException, IOException
	{
		BufferedImage image = null;
		Image capture = desktop.createImage(desktop.getWidth(),desktop.getHeight());
		FileOutputStream out = new FileOutputStream(filename);

		Graphics captureG = capture.getGraphics();
		desktop.paint(captureG);
		
		image = (BufferedImage)capture;
		if (image != null)
		{
			JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
			JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(image);
			
			//the first parameter here is the "quality v file size" trade-off
			//play with this value (0-1) to determine the best results for you
			
			param.setQuality(1f, true);
			encoder.encode(image, param);
			out.flush();
		}
		out.close();
	}	
	
	void doArrangeEntities()
	{
		Dimension full = new Dimension(10,10);
		Dimension view = scroll.getVisibleRect().getSize();
			
		DiagramAbstractEntity[] entities = this.getEntities();
		for(int i = 0; i < entities.length; i++)
		{
			Point next = new Point();
				
			next.x = full.width;
			next.y = full.height;
				
			if((next.x+entities[i].getWidth()) > view.width)
			{
				next.x = 10;
				next.y = full.height + entities[i].getHeight() + FRAME_OFFSET;
			}
			entities[i].setLocation(next);
				
			full.width = next.x + entities[i].getWidth() + FRAME_OFFSET;
			full.height = next.y;
		}
		doResize();
	}

	void doResize()
	{
		doResizeDesktop();
		doResizeRelations();
	}

	void doResizeDesktop()
	{
		maxCorner.setLocation(0,0);
		nextGoodPoint.setLocation(10,10);

		DiagramAbstractEntity[] entities = this.getEntities();
		for(int i = 0; i < entities.length; i++)
		{
			Point corner = new Point(entities[i].getX()+entities[i].getWidth(),
									 entities[i].getY()+entities[i].getHeight());

			if(corner.x > maxCorner.x) maxCorner.x = corner.x;
			if(corner.y > maxCorner.y) maxCorner.y = corner.y;
				
			if(entities[i].getY() >= nextGoodPoint.y)
			{
				nextGoodPoint.x = corner.x + FRAME_OFFSET;
				nextGoodPoint.y = entities[i].getY();
			}
		}
			
		Dimension dimView = scroll.getVisibleRect().getSize();
		if(scroll.getBorder()!=null)
		{
			Insets scrollInsets = scroll.getBorder().getBorderInsets(scroll);		
			dimView.setSize(dimView.getWidth() - scrollInsets.left - scrollInsets.right,
							dimView.getHeight() - scrollInsets.top - scrollInsets.bottom);
		}
			
		Dimension dimDesk = new Dimension(maxCorner.x, maxCorner.y);

		if(dimDesk.getWidth() <= dimView.getWidth()) dimDesk.width = ((int)dimView.getWidth()) - 20;
		if(dimDesk.getHeight() <= dimView.getHeight()) dimDesk.height = ((int)dimView.getHeight()) - 20;
		
		desktop.setMinimumSize(dimDesk);
		desktop.setMaximumSize(dimDesk);
		desktop.setPreferredSize(dimDesk);
		desktop.revalidate();
		scroll.validate();
	}

	void doResizeRelations()
	{
		DiagramRelation[] relations = ViewDiagram.this.getRelations();
		for(int i=0; i<relations.length; i++)
			relations[i].doResize();
	}
	
	void onModelChanged()
	{
		highlight = null;
		temporany = null;
		
		nextGoodPoint = new Point(10,10);
		maxCorner = new Point(0,0); 

		desktop.removeAll();
	}
	

    public QueryBuilder getBuilder()
    {
        return builder;
    }

    public void setBuilder(QueryBuilder builder)
    {
        this.builder = builder;
    }
    
	private class InternalDiagramManager extends DefaultDesktopManager
	{
		public void closeFrame(JInternalFrame f)
		{
			DiagramAbstractEntity entity = (DiagramAbstractEntity)f;
			entity.onDestroy();
			
			super.closeFrame(f);
			
			if(temporany!=null && entity == temporany.primaryEntity)
				temporany = null;
			
			ViewDiagram.this.doResizeDesktop();
		}

		public void openFrame(JInternalFrame f)
		{
			ViewDiagram.this.desktop.add(f);
			ViewDiagram.this.doResizeDesktop();
			
			DiagramAbstractEntity entity = (DiagramAbstractEntity)f;
			entity.onCreate();
		}
		
		public void endDraggingFrame(JComponent f)
		{
			super.endDraggingFrame(f);
			ViewDiagram.this.doResize();
		}
	}
}