/**
 * This file is part of Logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with Logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + Haute École Spécialisée Bernoise
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 *   + REDS Institute - HEIG-VD, Yverdon-les-Bains, Switzerland
 *     http://reds.heig-vd.ch
 * This version of the project is currently maintained by:
 *   + Kevin Walsh (kwalsh@holycross.edu, http://mathcs.holycross.edu/~kwalsh)
 */

package com.cburch.logisim.gui.log;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.EventObject;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DropMode;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;

import com.cburch.logisim.circuit.RadixOption;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.util.Icons;

public class SelectionList extends JTable {

  private class SelectionListModel
    extends AbstractTableModel implements Model.Listener {

    @Override
    public void modeChanged(Model.Event event) { }
    @Override
    public void signalsExtended(Model.Event event) { }
    @Override
    public void signalsReset(Model.Event event) { }
    @Override
    public void filePropertyChanged(Model.Event event) { }
    @Override
    public void historyLimitChanged(Model.Event event) { }

    @Override
    public void selectionChanged(Model.Event event) {
      fireTableDataChanged();
    }

    @Override
    public int getColumnCount() { return 1; };
    @Override
    public String getColumnName(int column) { return ""; }
    @Override
    public Class<?> getColumnClass(int columnIndex) { return SignalInfo.class; }

    @Override
    public int getRowCount() { return logModel == null ? 0 : logModel.getSignalCount(); }
    @Override
    public Object getValueAt(int row, int col) { return logModel.getItem(row); }
    @Override
    public void setValueAt(Object o, int row, int column) { /* nothing to do */ }

    @Override
    public boolean isCellEditable(int row, int column) { return true; }

  }

  private class SignalInfoRenderer extends DefaultTableCellRenderer {
    @Override
    public java.awt.Component getTableCellRendererComponent(JTable table,
        Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      java.awt.Component ret = super.getTableCellRendererComponent(table,
          value, isSelected, hasFocus, row, column);
      if (ret instanceof JLabel && value instanceof SignalInfo) {
        JLabel label = (JLabel) ret;
        SignalInfo item = (SignalInfo)value;
        label.setIcon(item.icon);
        label.setText(item.toString() + " [" + item.getRadix().toDisplayString() + "]");
      }
      return ret;
    }
  }

  class SignalInfoEditor extends AbstractCellEditor implements TableCellEditor {
    JPanel panel = new JPanel();
    JLabel label = new JLabel();
    JButton button = new JButton(Icons.getIcon("dropdown.png"));
    JPopupMenu popup = new JPopupMenu("Options");
    SignalInfo item;
    SignalInfo.List items;
    HashMap<RadixOption, JRadioButtonMenuItem> radixMenuItems = new HashMap<>();

    public SignalInfoEditor() {
      panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

      label.setFont(label.getFont().deriveFont(Font.PLAIN));
      button.setFont(button.getFont().deriveFont(9.0f));

      ButtonGroup g = new ButtonGroup();
      for (RadixOption r : RadixOption.OPTIONS) {
        JRadioButtonMenuItem m = new JRadioButtonMenuItem(r.toDisplayString()); 
        radixMenuItems.put(r, m);
        popup.add(m);
        g.add(m);
        m.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {              
            for (SignalInfo s : items)
              logModel.setRadix(s, r);
            if (item != null)
              label.setText(item.toString() + " [" + item.getRadix().toDisplayString() + "]");
            SelectionList.this.repaint();
          }
        });
      }

      popup.addSeparator();
      JMenuItem m = new JMenuItem("Delete");
      popup.add(m);
      m.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {              
          cancelCellEditing();
          removeSelected();
        }
      });

      button.setMargin(new Insets(0, 0, 0, 0));
      button.setHorizontalTextPosition(SwingConstants.LEFT);
      button.setText("Options");
      button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          popup.show(panel, button.getX(), button.getY() + button.getHeight());
        }
      });
      button.setMinimumSize(button.getPreferredSize());

      label.setHorizontalAlignment(SwingConstants.LEFT);
      label.setAlignmentX(0.0f);
      label.setAlignmentY(0.5f);
      button.setAlignmentX(1.0f);
      button.setAlignmentY(0.5f);
      panel.add(label);
      panel.add(button);
    }

    @Override
    public Object getCellEditorValue() {
      return item;
    }

    @Override
    public java.awt.Component getTableCellEditorComponent(JTable table,
        Object value, boolean isSelected, int row, int column) {
      int margin = getColumnModel().getColumnMargin();
      label.setBorder(BorderFactory.createEmptyBorder(0, margin, 0, margin));

      Dimension d = new Dimension(getColumnModel().getTotalColumnWidth(), getRowHeight());
      label.setMinimumSize(new Dimension(10, d.height));
      label.setPreferredSize(new Dimension(d.width - button.getWidth(), d.height));
      label.setMaximumSize(new Dimension(d.width - button.getWidth(), d.height));

      panel.setBackground(isSelected ? getSelectionBackground() : getBackground());
      item = (SignalInfo)value;
      items = getSelectedValuesList();
      if (!items.contains(item)) {
        items.clear();
        items.add(item);
      }
      radixMenuItems.get(item.getRadix()).setSelected(true);
      label.setIcon(item.icon);
      label.setText(item.toString() + " [" + item.getRadix().toDisplayString() + "]");
      //width.setSelectedItem(item.getRadix());
      return panel;
    }

     @Override
     public boolean stopCellEditing() {
       super.stopCellEditing();
       return true;
     }
     @Override
     public boolean isCellEditable(EventObject e) {
       return true;
     }
  }

  private static final long serialVersionUID = 1L;

  private Model logModel;

  @SuppressWarnings("unchecked")
  public SelectionList() {
    setModel(new SelectionListModel());
    setDefaultRenderer(SignalInfo.class, new SignalInfoRenderer());
    setDefaultEditor(SignalInfo.class, new SignalInfoEditor());
    setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    getTableHeader().setUI(null);
    setRowHeight(24);
    // setAutoResizeMode(AUTO_RESIZE_OFF);
    setShowGrid(false);
    setFillsViewportHeight(true);
    setDragEnabled(true);
    setDropMode(DropMode.INSERT_ROWS);
    putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    setTransferHandler(new SelectionTransferHandler());

    InputMap inputMap = getInputMap();
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "Delete");
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "Delete");
    ActionMap actionMap = getActionMap();
    actionMap.put("Delete", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        removeSelected();
      }
    });
  }

  void removeSelected() {
    int idx = 0;
    SignalInfo.List items = getSelectedValuesList();
    for (SignalInfo item : items) {
      idx = Math.max(idx, logModel.indexOf(item));
    }
    int count = logModel.remove(items);
    if (count > 0 && logModel.getSignalCount() > 0) {
      idx = Math.min(idx+1-count, logModel.getSignalCount()-1);
      setRowSelectionInterval(idx, idx);
    }
    repaint();
  }

  public void localeChanged() {
    repaint();
  }

  public void setLogModel(Model m) {
    if (logModel != m) {
      SelectionListModel listModel = (SelectionListModel) getModel();
      if (logModel != null)
        logModel.removeModelWeakListener(this, listModel);
      logModel = m;
      if (logModel != null)
        logModel.addModelWeakListener(this, listModel);
      listModel.selectionChanged(null);
    }
  }

  SignalInfo.List getSelectedValuesList() {
    SignalInfo.List items = new SignalInfo.List();
    int[] sel = getSelectedRows();
    for (int i : sel)
      items.add(logModel.getItem(i));
    return items;
  }

  private class SelectionTransferHandler extends TransferHandler {
    boolean removing;

    @Override
    public int getSourceActions(JComponent comp) {
      return MOVE;
    }

    @Override
    public Transferable createTransferable(JComponent comp) {
      removing = true;
      SignalInfo.List items = new SignalInfo.List();
      items.addAll(getSelectedValuesList());
      return items.isEmpty() ? null : items;
    }

    @Override
    public void exportDone(JComponent comp, Transferable trans, int action) {
      if (removing)
        logModel.remove(getSelectedValuesList());
      removing = false;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
      return support.isDataFlavorSupported(SignalInfo.List.dataFlavor);
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport support) {
      removing = false;
      try {
        SignalInfo.List items =
            (SignalInfo.List)support.getTransferable().getTransferData(
                SignalInfo.List.dataFlavor);
        int newIdx = logModel.getSignalCount();
        if (support.isDrop()) {
          try {
            JTable.DropLocation dl = (JTable.DropLocation)support.getDropLocation();
            newIdx = Math.min(dl.getRow(), logModel.getSignalCount());
          } catch (ClassCastException e) {
          }
        }
        addOrMove(items, newIdx);
        return true;
      } catch (UnsupportedFlavorException | IOException e) {
        e.printStackTrace();
        return false;
      }
    }
  }

  private void addOrMove(SignalInfo.List items, int idx) {
    if (items == null || items.size() == 0)
      return;
    logModel.addOrMove(items, idx);
    clearSelection();
    for (SignalInfo item : items) {
      int i = logModel.indexOf(item);
      addRowSelectionInterval(i, i);
    }
  }

  public void add(SignalInfo.List items) {
    addOrMove(items, logModel.getSignalCount());
  }

  private static final Font MSG_FONT = new Font("Sans Serif", Font.ITALIC, 12);

  @Override
  public void paintComponent(Graphics g) {
    super.paintComponent(g);

    /* Anti-aliasing changes from https://github.com/hausen/logisim-evolution */
    Graphics2D g2 = (Graphics2D)g;
    g2.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING,
        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);

    Font f = g.getFont();
    Color c = g.getColor();
    g.setColor(Color.GRAY);
    g.setFont(MSG_FONT);
    g.drawString("drag here to add", 10, getRowHeight() * getRowCount() + 20);
    g.setFont(f);
    g.setColor(c);
  }
}
