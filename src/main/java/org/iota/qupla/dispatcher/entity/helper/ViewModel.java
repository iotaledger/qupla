package org.iota.qupla.dispatcher.entity.helper;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;
import org.iota.qupla.helper.TritVector;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.statement.TypeStmt;

public class ViewModel extends AbstractTableModel
{
  public int columns;
  public boolean edit;
  public TypeStmt typeInfo;
  public final ArrayList<TritVector> vectors = new ArrayList<>();

  public ViewModel(final TypeStmt typeInfo)
  {
    this.typeInfo = typeInfo;
    columns = typeInfo.struct != null ? typeInfo.struct.fields.size() + 1 : 2;
  }

  //  @Override
  //  public Class<?> getColumnClass(final int col)
  //  {
  //    return col == columns - 1 ? String.class : Number.class;
  //  }

  @Override
  public int getColumnCount()
  {
    return columns;
  }

  @Override
  public String getColumnName(final int col)
  {
    if (col == columns - 1)
    {
      return "Effect trit vector";
    }

    if (typeInfo.struct == null)
    {
      return "value";
    }

    return typeInfo.struct.fields.get(col).name;
  }

  @Override
  public int getRowCount()
  {
    return vectors.size();
  }

  @Override
  public Object getValueAt(final int row, final int col)
  {
    final TritVector vector = vectors.get(row);

    if (col == columns - 1)
    {
      return vector.trits();
    }

    if (typeInfo.isFloat || typeInfo.struct == null)
    {
      return typeInfo.toString(vector);
    }

    int offset = 0;
    for (int i = 0; i < col; i++)
    {
      offset += typeInfo.struct.fields.get(i).size;
    }

    final BaseExpr field = typeInfo.struct.fields.get(col);
    final TritVector slice = vector.slice(offset, field.size);
    return slice.toDecimal();
  }

  @Override
  public boolean isCellEditable(final int row, final int col)
  {
    // if editable still cannot edit trit vector field
    return edit && col != columns - 1;
  }
}
