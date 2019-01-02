package org.iota.qupla.qupla.context.base;

import java.util.ArrayList;

import org.iota.qupla.helper.BaseContext;
import org.iota.qupla.qupla.expression.AssignExpr;
import org.iota.qupla.qupla.expression.CondExpr;
import org.iota.qupla.qupla.expression.FuncExpr;
import org.iota.qupla.qupla.expression.IntegerExpr;
import org.iota.qupla.qupla.expression.LutExpr;
import org.iota.qupla.qupla.expression.MergeExpr;
import org.iota.qupla.qupla.expression.SliceExpr;
import org.iota.qupla.qupla.expression.StateExpr;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.parser.Module;
import org.iota.qupla.qupla.statement.FuncStmt;
import org.iota.qupla.qupla.statement.LutStmt;

public abstract class QuplaBaseContext extends BaseContext
{
  public QuplaBaseContext append(final String text)
  {
    return (QuplaBaseContext) super.append(text);
  }

  public void eval(final Module module)
  {
    for (final LutStmt lut : module.luts)
    {
      evalLutDefinition(lut);
    }

    for (final FuncStmt func : module.funcs)
    {
      evalFuncSignature(func);
    }

    for (final FuncStmt func : module.funcs)
    {
      evalFuncBody(func);
    }
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
}
