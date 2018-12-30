package org.iota.qupla.abra.context;

import java.util.HashMap;

import org.iota.qupla.abra.AbraBlockBranch;
import org.iota.qupla.abra.AbraBlockImport;
import org.iota.qupla.abra.AbraBlockLut;
import org.iota.qupla.abra.AbraCode;
import org.iota.qupla.abra.AbraSiteKnot;
import org.iota.qupla.abra.AbraSiteLatch;
import org.iota.qupla.abra.AbraSiteMerge;
import org.iota.qupla.abra.AbraSiteParam;
import org.iota.qupla.context.AbraContext;
import org.iota.qupla.expression.base.BaseExpr;

public abstract class AbraCodeContext
{
  protected static final HashMap<String, Integer> indexFromTrits = new HashMap<>();
  protected static final String[] lutIndexes = {
      "---",
      "0--",
      "1--",
      "-0-",
      "00-",
      "10-",
      "-1-",
      "01-",
      "11-",
      "--0",
      "0-0",
      "1-0",
      "-00",
      "000",
      "100",
      "-10",
      "010",
      "110",
      "--1",
      "0-1",
      "1-1",
      "-01",
      "001",
      "101",
      "-11",
      "011",
      "111"
  };
  public AbraCode abraCode;
  private boolean mustIndent = false;
  private int spaces = 0;

  public AbraCodeContext append(final String text)
  {
    if (text.length() == 0)
    {
      return this;
    }

    if (mustIndent)
    {
      mustIndent = false;
      for (int i = 0; i < spaces; i++)
      {
        appendify(" ");
      }
    }

    appendify(text);
    return this;
  }

  protected void appendify(final String text)
  {
  }

  public void eval(final AbraContext context, final BaseExpr expr)
  {
  }

  public abstract void evalBranch(final AbraBlockBranch branch);

  public abstract void evalImport(final AbraBlockImport imp);

  public abstract void evalKnot(final AbraSiteKnot knot);

  public abstract void evalLatch(final AbraSiteLatch latch);

  public abstract void evalLut(final AbraBlockLut lut);

  public abstract void evalMerge(final AbraSiteMerge merge);

  public abstract void evalParam(final AbraSiteParam param);

  public void finished()
  {
  }

  public AbraCodeContext indent()
  {
    spaces += 2;
    return this;
  }

  public AbraCodeContext newline()
  {
    append("\n");
    mustIndent = true;
    return this;
  }

  public void started()
  {
  }

  public AbraCodeContext undent()
  {
    spaces -= 2;
    return this;
  }

  static
  {
    for (int i = 0; i < 27; i++)
    {
      indexFromTrits.put(lutIndexes[i], i);
    }
  }
}
