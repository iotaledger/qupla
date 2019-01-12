package org.iota.qupla.helper;

import org.iota.qupla.qupla.expression.base.BaseExpr;

public class DummyStmt extends BaseExpr
{
  public DummyStmt(final String text)
  {
    name = text;
  }

  @Override
  public void analyze()
  {
  }

  @Override
  public BaseExpr clone()
  {
    return null;
  }

  @Override
  public String toString()
  {
    return name;
  }
}
