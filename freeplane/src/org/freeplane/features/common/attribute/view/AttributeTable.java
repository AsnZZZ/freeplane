/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Dimitry Polivaev
 *
 *  This file author is Dimitry Polivaev
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.features.common.attribute.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.freeplane.core.model.NodeModel;
import org.freeplane.features.common.attribute.AttributeRegistry;
import org.freeplane.features.common.attribute.AttributeTableLayoutModel;
import org.freeplane.features.common.attribute.ColumnWidthChangeEvent;
import org.freeplane.features.common.attribute.IAttributeTableModel;
import org.freeplane.features.common.attribute.IColumnWidthChangeListener;
import org.freeplane.view.swing.map.MapView;
import org.freeplane.view.swing.map.NodeView;

/**
 * @author Dimitry Polivaev
 */
public class AttributeTable extends JTable implements IColumnWidthChangeListener {
	static private class HeaderMouseListener extends MouseAdapter {
		@Override
		public void mouseReleased(final MouseEvent e) {
			final JTableHeader header = (JTableHeader) e.getSource();
			final AttributeTable table = (AttributeTable) header.getTable();
			final float zoom = table.attributeView.getMapView().getZoom();
			final Dimension preferredScrollableViewportSize = table
			    .getPreferredScrollableViewportSize();
			final JViewport port = (JViewport) table.getParent();
			final Dimension extentSize = port.getExtentSize();
			if (preferredScrollableViewportSize.width != extentSize.width) {
				final IAttributeTableModel model = (IAttributeTableModel) table.getModel();
				for (int col = 0; col < table.getColumnCount(); col++) {
					final int modelColumnWidth = model.getColumnWidth(col);
					final int currentColumnWidth = (int) (table.getColumnModel().getColumn(col)
					    .getWidth() / zoom);
					if (modelColumnWidth != currentColumnWidth) {
						model.setColumnWidth(col, currentColumnWidth);
					}
				}
			}
		}
	}

	static private class MyFocusListener implements FocusListener {
		private AttributeTable focusedTable;

		/*
		 * (non-Javadoc)
		 * @see
		 * java.awt.event.FocusListener#focusGained(java.awt.event.FocusEvent)
		 */
		public void focusGained(final FocusEvent event) {
			final Component source = (Component) event.getSource();
			event.getOppositeComponent();
			if (source instanceof AttributeTable) {
				focusedTable = (AttributeTable) source;
			}
			else {
				focusedTable = (AttributeTable) SwingUtilities.getAncestorOfClass(
				    AttributeTable.class, source);
			}
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					if (focusedTable != null) {
						final Component newNodeViewInFocus = SwingUtilities.getAncestorOfClass(
						    NodeView.class, focusedTable);
						if (newNodeViewInFocus != null) {
							final NodeView viewer = (NodeView) newNodeViewInFocus;
							if (viewer != viewer.getMap().getSelected()) {
								viewer.getMap().selectAsTheOnlyOneSelected(viewer, false);
							}
						}
					}
				}
			});
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * java.awt.event.FocusListener#focusLost(java.awt.event.FocusEvent)
		 */
		public void focusLost(final FocusEvent event) {
			final Component oppositeComponent = event.getOppositeComponent();
			final Component newTable = SwingUtilities.getAncestorOfClass(AttributeTable.class,
			    oppositeComponent);
			if (focusedTable != null && focusedTable != newTable) {
				if (focusedTable.isEditing()) {
					focusedTable.getCellEditor().stopCellEditing();
				}
				if (!focusedTable.attributeView.isPopupShown()) {
					final AttributeView attributeView = focusedTable.getAttributeView();
					final String currentAttributeViewType = AttributeRegistry.getRegistry(
					    attributeView.getNode().getMap()).getAttributeViewType();
					if (attributeView.getViewType() != currentAttributeViewType) {
						attributeView.stateChanged(null);
					}
				}
				focusedTable = null;
			}
		}
	}

	static private MouseListener componentListener = new HeaderMouseListener();
	static private ComboBoxModel defaultComboBoxModel = null;
	static private AttributeTableCellRenderer dtcr = new AttributeTableCellRenderer();
	private static final int EXTRA_HEIGHT = 4;
	static private MyFocusListener focusListener = new MyFocusListener();
	private static final int MAX_HEIGTH = 300;
	private static final int MAX_WIDTH = 600;
	private static final Dimension prefHeaderSize = new Dimension(1, 8);
	private static final float TABLE_ROW_HEIGHT = 4;

	static ComboBoxModel getDefaultComboBoxModel() {
		if (AttributeTable.defaultComboBoxModel == null) {
			AttributeTable.defaultComboBoxModel = new DefaultComboBoxModel();
		}
		return AttributeTable.defaultComboBoxModel;
	}

	final private AttributeView attributeView;
	private int highRowIndex = 0;

	AttributeTable(final AttributeView attributeView) {
		super();
		this.attributeView = attributeView;
		addFocusListener(AttributeTable.focusListener);
		final NodeModel model = attributeView.getNodeView().getModel();
		if (!model.getMap().isReadOnly()) {
			getTableHeader().addMouseListener(AttributeTable.componentListener);
		}
		else {
			getTableHeader().setResizingAllowed(false);
		}
		setModel(attributeView.getCurrentAttributeTableModel());
		updateFontSize(this, 1F);
		updateColumnWidths();
		setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		getTableHeader().setReorderingAllowed(false);
		getTableHeader().setPreferredSize(AttributeTable.prefHeaderSize);
		getRowHeight();
		updateRowHeights();
		setRowSelectionAllowed(false);
		putClientProperty("JTable.autoStartsEdit", Boolean.FALSE);
	}

	private void changeSelectedRowHeight(final int rowIndex) {
		if (highRowIndex != rowIndex) {
			if (highRowIndex < getRowCount()) {
				final int h = getRowHeight(highRowIndex);
				setRowHeight(highRowIndex, h - AttributeTable.EXTRA_HEIGHT);
			}
			final int h = getRowHeight(rowIndex);
			setRowHeight(rowIndex, h + AttributeTable.EXTRA_HEIGHT);
			highRowIndex = rowIndex;
		}
	}

	@Override
	public void changeSelection(int rowIndex, int columnIndex, final boolean toggle,
	                            final boolean extend) {
		final int rowCount = getRowCount();
		if (rowCount == 0) {
			return;
		}
		if (rowIndex >= rowCount) {
			rowIndex = 0;
			columnIndex = 0;
		}
		changeSelectedRowHeight(rowIndex);
		super.changeSelection(rowIndex, columnIndex, toggle, extend);
	}

	public void columnWidthChanged(final ColumnWidthChangeEvent event) {
		final float zoom = getZoom();
		final int col = event.getColumnNumber();
		final AttributeTableLayoutModel layoutModel = (AttributeTableLayoutModel) event.getSource();
		final int width = layoutModel.getColumnWidth(col);
		getColumnModel().getColumn(col).setPreferredWidth((int) (width * zoom));
		getAttributeView().getNode().getMap().nodeChanged(getAttributeView().getNode());
	}

	/**
	 * @return Returns the currentModel.
	 */
	public AttributeTableModelDecoratorAdapter getAttributeTableModel() {
		return (AttributeTableModelDecoratorAdapter) getModel();
	}

	public AttributeView getAttributeView() {
		return attributeView;
	}

	@Override
	public TableCellEditor getCellEditor(final int row, final int column) {
		final JComboBox comboBox = new JComboBox();
		final DefaultCellEditor dce = new DefaultCellEditor(comboBox);
		return dce;
	}

	@Override
	public TableCellRenderer getCellRenderer(final int row, final int column) {
		final String text = getValueAt(row, column).toString();
		AttributeTable.dtcr.setText(text);
		final int prefWidth = AttributeTable.dtcr.getPreferredSize().width;
		final int width = getColumnModel().getColumn(column).getWidth();
		if (prefWidth > width) {
			AttributeTable.dtcr.setToolTipText(text);
		}
		else {
			AttributeTable.dtcr.setToolTipText(null);
		}
		return AttributeTable.dtcr;
	}

	private float getFontSize() {
		return AttributeRegistry.getRegistry(attributeView.getNode().getMap()).getFontSize();
	}

	@Override
	public Dimension getPreferredScrollableViewportSize() {
		if (!isValid()) {
			validate();
		}
		final float zoom = getZoom();
		final Dimension dimension = super.getPreferredSize();
		dimension.width = Math.min((int) (AttributeTable.MAX_WIDTH * zoom), dimension.width);
		dimension.height = Math.min((int) (AttributeTable.MAX_HEIGTH * zoom)
		        - getTableHeaderHeight(), dimension.height);
		return dimension;
	}

	int getTableHeaderHeight() {
		final JTableHeader tableHeader = getTableHeader();
		return tableHeader != null ? tableHeader.getPreferredSize().height : 0;
	}

	float getZoom() {
		return attributeView.getMapView().getZoom();
	}

	/**
	 */
	public void insertRow(final int row) {
		if (getModel() instanceof ExtendedAttributeTableModelDecorator) {
			final ExtendedAttributeTableModelDecorator model = (ExtendedAttributeTableModelDecorator) getModel();
			if (isEditing() && getCellEditor() != null && !getCellEditor().stopCellEditing()) {
				return;
			}
			model.insertRow(row);
			changeSelection(row, 0, false, false);
			if (editCellAt(row, 0)) {
				getEditorComponent().requestFocus();
			}
		}
	}

	@Override
	public boolean isVisible() {
		return attributeView.areAttributesVisible();
	}

	/**
	 */
	public void moveRowDown(final int row) {
		if (getModel() instanceof ExtendedAttributeTableModelDecorator) {
			final ExtendedAttributeTableModelDecorator model = (ExtendedAttributeTableModelDecorator) getModel();
			model.moveRowDown(row);
		}
	}

	/**
	 */
	public void moveRowUp(final int row) {
		if (getModel() instanceof ExtendedAttributeTableModelDecorator) {
			final ExtendedAttributeTableModelDecorator model = (ExtendedAttributeTableModelDecorator) getModel();
			model.moveRowUp(row);
		}
	}

	/**
	 *
	 */
	@Override
	public Component prepareEditor(final TableCellEditor tce, final int row, final int col) {
		ComboBoxModel model;
		final JComboBox comboBox = (JComboBox) ((DefaultCellEditor) tce).getComponent();
		final NodeModel node = getAttributeTableModel().getNode();
		final AttributeRegistry attributes = AttributeRegistry.getRegistry(node.getMap());
		switch (col) {
			case 0:
				model = attributes.getComboBoxModel();
				comboBox.setEditable(!attributes.isRestricted());
				break;
			case 1:
				final String attrName = getAttributeTableModel().getValueAt(row, 0).toString();
				model = attributes.getDefaultComboBoxModel(attrName);
				comboBox.setEditable(!attributes.isRestricted(attrName));
				break;
			default:
				model = AttributeTable.getDefaultComboBoxModel();
		}
		comboBox.setModel(model);
		model.setSelectedItem(getValueAt(row, col));
		comboBox.addFocusListener(AttributeTable.focusListener);
		comboBox.getEditor().getEditorComponent().addFocusListener(AttributeTable.focusListener);
		final Component editor = super.prepareEditor(tce, row, col);
		updateFontSize(editor, getZoom());
		return editor;
	}

	@Override
	protected boolean processKeyBinding(final KeyStroke ks, final KeyEvent e, final int condition,
	                                    final boolean pressed) {
		if (ks.getKeyCode() == KeyEvent.VK_TAB && e.getModifiers() == 0 && pressed
		        && getSelectedColumn() == 1 && getSelectedRow() == getRowCount() - 1
		        && getModel() instanceof ExtendedAttributeTableModelDecorator) {
			insertRow(getRowCount());
			return true;
		}
		if (ks.getKeyCode() == KeyEvent.VK_ESCAPE && e.getModifiers() == 0 && pressed) {
			attributeView.getNodeView().requestFocus();
			return true;
		}
		boolean retValue = super.processKeyBinding(ks, e, condition, pressed);
		if (!retValue && condition == JComponent.WHEN_FOCUSED && isFocusOwner()
		        && ks.getKeyCode() != KeyEvent.VK_TAB && e != null
		        && e.getID() == KeyEvent.KEY_PRESSED && !e.isActionKey()
		        && e.getKeyChar() != KeyEvent.CHAR_UNDEFINED
		        && 0 == (e.getModifiers() & (InputEvent.CTRL_MASK | InputEvent.ALT_MASK))) {
			final int leadRow = getSelectionModel().getLeadSelectionIndex();
			final int leadColumn = getColumnModel().getSelectionModel().getLeadSelectionIndex();
			if (leadRow != -1 && leadColumn != -1 && !isEditing()) {
				if (!editCellAt(leadRow, leadColumn)) {
					return false;
				}
			}
			final Component editorComponent = getEditorComponent();
			if (editorComponent instanceof JComboBox) {
				final JComboBox comboBox = (JComboBox) editorComponent;
				if (comboBox.isEditable()) {
					final ComboBoxEditor editor = comboBox.getEditor();
					editor.selectAll();
					KeyEvent keyEv;
					keyEv = new KeyEvent(editor.getEditorComponent(), KeyEvent.KEY_TYPED, e
					    .getWhen(), e.getModifiers(), KeyEvent.VK_UNDEFINED, e.getKeyChar(),
					    KeyEvent.KEY_LOCATION_UNKNOWN);
					retValue = SwingUtilities.processKeyBindings(keyEv);
				}
				else {
					editorComponent.requestFocus();
					retValue = true;
				}
			}
		}
		if (ks.getKeyCode() == KeyEvent.VK_SPACE) {
			return true;
		}
		return retValue;
	}

	@Override
	public void removeEditor() {
		removeListenerFromEditor();
		getAttributeTableModel().editingCanceled();
		super.removeEditor();
	}

	private void removeListenerFromEditor() {
		final JComboBox comboBox = (JComboBox) getEditorComponent();
		comboBox.removeFocusListener(AttributeTable.focusListener);
		comboBox.getEditor().getEditorComponent().removeFocusListener(AttributeTable.focusListener);
		comboBox.setModel(new DefaultComboBoxModel());
	}

	/**
	 */
	public void removeRow(final int row) {
		if (getModel() instanceof ExtendedAttributeTableModelDecorator) {
			final ExtendedAttributeTableModelDecorator model = (ExtendedAttributeTableModelDecorator) getModel();
			model.removeRow(row);
		}
	}

	@Override
	public void setModel(final TableModel dataModel) {
		super.setModel(dataModel);
	}

	/**
	 *
	 */
	public void setOptimalColumnWidths() {
		Component comp = null;
		int cellWidth = 0;
		int maxCellWidth = 2 * (int) (Math.ceil(getFontSize() + AttributeTable.TABLE_ROW_HEIGHT));
		for (int col = 0; col < 2; col++) {
			for (int row = 0; row < getRowCount(); row++) {
				comp = AttributeTable.dtcr.getTableCellRendererComponent(this,
				    getValueAt(row, col), false, false, row, col);
				cellWidth = comp.getPreferredSize().width;
				maxCellWidth = Math.max(cellWidth, maxCellWidth);
			}
			getAttributeTableModel().setColumnWidth(col, maxCellWidth + 1);
		}
	}

	@Override
	public void tableChanged(final TableModelEvent e) {
		super.tableChanged(e);
		if (getParent() == null) {
			return;
		}
		if (e.getType() == TableModelEvent.DELETE && e.getFirstRow() == highRowIndex
		        && e.getFirstRow() == getRowCount() && e.getFirstRow() != 0) {
			changeSelection(e.getFirstRow() - 1, 0, false, false);
		}
		else {
			updateRowHeights();
		}
		final MapView map = getAttributeView().getNodeView().getMap();
		getParent().getParent().invalidate();
		map.getModel().nodeChanged(getAttributeView().getNode());
	}

	/**
	 *
	 */
	void updateAttributeTable() {
		updateFontSize(this, 1F);
		updateRowHeights();
		updateColumnWidths();
	}

	private void updateColumnWidths() {
		final float zoom = getZoom();
		for (int i = 0; i < 2; i++) {
			final int width = (int) (getAttributeTableModel().getColumnWidth(i) * zoom);
			getColumnModel().getColumn(i).setPreferredWidth(width);
		}
	}

	private void updateFontSize(final Component c, final float zoom) {
		Font font = c.getFont();
		if (font != null) {
			final float oldFontSize = font.getSize2D();
			final float newFontSize = getFontSize() * zoom;
			if (oldFontSize != newFontSize) {
				font = font.deriveFont(newFontSize);
				c.setFont(font);
			}
		}
	}

	private void updateRowHeights() {
		final int rowCount = getRowCount();
		if (rowCount == 0) {
			return;
		}
		final int constHeight = getTableHeaderHeight() + AttributeTable.EXTRA_HEIGHT;
		final float zoom = getZoom();
		final float fontSize = getFontSize();
		final float tableRowHeight = fontSize + zoom * AttributeTable.TABLE_ROW_HEIGHT;
		int newHeight = (int) ((tableRowHeight * rowCount + (zoom - 1) * constHeight) / rowCount);
		if (newHeight < 1) {
			newHeight = 1;
		}
		final int highRowsNumber = (int) ((tableRowHeight - newHeight) * rowCount);
		for (int i = 0; i < highRowsNumber; i++) {
			setRowHeight(i, 1 + newHeight + (i == highRowIndex ? AttributeTable.EXTRA_HEIGHT : 0));
		}
		for (int i = highRowsNumber; i < rowCount; i++) {
			setRowHeight(i, newHeight + (i == highRowIndex ? AttributeTable.EXTRA_HEIGHT : 0));
		}
	}

	public void viewRemoved() {
		getModel().removeTableModelListener(this);
	}
}
