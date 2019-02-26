package org.iota.qupla.dispatcher.entity;

import java.awt.event.WindowAdapter;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import org.iota.qupla.dispatcher.Dispatcher;
import org.iota.qupla.dispatcher.Entity;
import org.iota.qupla.dispatcher.Environment;
import org.iota.qupla.dispatcher.entity.helper.ViewModel;
import org.iota.qupla.helper.TritVector;

public class ViewEntity extends Entity
{
  public static WindowAdapter windowAdapter;
  public String envName;
  public JFrame frame;
  public ViewModel input;
  public ViewModel model;

  public ViewEntity(final Dispatcher dispatcher, final String envName)
  {
    super(0);

    // this viewer will both join and affect its environment
    // you will see received effects scroll by and data sent
    // from the viewer gets sent immediately to this environment

    this.envName = envName;
    final Environment env = dispatcher.getEnvironment(envName, null);
    join(env);
    affect(env, 0);

    input = new ViewModel(env.typeInfo);
    input.edit = true;
    input.vectors.add(new TritVector(env.typeInfo.size, '0'));

    final JTable inTable = new JTable(input);
    updateWidths(inTable, input);
    inTable.setTableHeader(null);

    model = new ViewModel(env.typeInfo);
    final JTable table = new JTable(model);
    updateWidths(table, model);
    table.setFillsViewportHeight(true);
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

    //TODO add this code to auto-scroll, but do it only when bottom row is visible
    //    table.addComponentListener(new ComponentAdapter() {
    //      public void componentResized(ComponentEvent e) {
    //        table.scrollRectToVisible(table.getCellRect(table.getRowCount()-1, 0, true));
    //      }
    //    });

    final JScrollPane scroller = new JScrollPane(table);

    final JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
    mainPanel.add(inTable);
    mainPanel.add(scroller);

    frame = new JFrame(envName);
    frame.addWindowListener(windowAdapter);
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    frame.add(mainPanel);
    frame.pack();
    frame.setVisible(true);
  }

  public TritVector onEffect(final TritVector effect)
  {
    int size = model.vectors.size();
    model.vectors.add(effect);
    model.fireTableRowsInserted(size, size);

    // return null, no need to propagate, we only log inputValue
    return null;
  }

  public void updateWidths(final JTable table, final ViewModel model)
  {
    for (int i = 0; i < model.columns - 1; i++)
    {
      final TableColumn column = table.getColumnModel().getColumn(i);
      column.setPreferredWidth(80);

      final DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
      renderer.setHorizontalAlignment(JLabel.RIGHT);
      column.setCellRenderer(renderer);
    }

    final TableColumn column = table.getColumnModel().getColumn(model.columns - 1);
    column.setPreferredWidth(800);
  }
}
