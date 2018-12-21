package org.iota.qupla.context;

import java.util.ArrayList;

import org.iota.qupla.expression.AssignExpr;
import org.iota.qupla.expression.CondExpr;
import org.iota.qupla.expression.FuncExpr;
import org.iota.qupla.expression.IntegerExpr;
import org.iota.qupla.expression.LutExpr;
import org.iota.qupla.expression.MergeExpr;
import org.iota.qupla.expression.SliceExpr;
import org.iota.qupla.expression.StateExpr;
import org.iota.qupla.expression.base.BaseExpr;
import org.iota.qupla.statement.FuncStmt;
import org.iota.qupla.statement.LutStmt;

public abstract class CodeContext
{
  private boolean mustIndent = false;
  private int spaces = 0;

  public CodeContext append(final String text)
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

  public abstract void evalAssign(AssignExpr assign);

  public abstract void evalConcat(ArrayList<BaseExpr> exprs);

  public abstract void evalConditional(CondExpr conditional);

  public abstract void evalFuncBody(FuncStmt func);

  public abstract void evalFuncCall(FuncExpr call);

  public abstract void evalFuncSignature(FuncStmt func);

  public abstract void evalLutDefinition(LutStmt lut);

  public abstract void evalLutLookup(LutExpr lookup);

  public abstract void evalMerge(MergeExpr merge);

  public abstract void evalSlice(SliceExpr slice);

  public abstract void evalState(StateExpr state);

  public abstract void evalVector(IntegerExpr integer);

  public void finished()
  {
  }

  public CodeContext indent()
  {
    spaces += 2;
    return this;
  }

  public CodeContext newline()
  {
    append("\n");
    mustIndent = true;
    return this;
  }

  public CodeContext undent()
  {
    spaces -= 2;
    return this;
  }
}
