package org.iota.qupla.qupla.context;

import org.iota.qupla.Qupla;
import org.iota.qupla.qupla.context.base.QuplaBaseContext;
import org.iota.qupla.qupla.expression.AssignExpr;
import org.iota.qupla.qupla.expression.ConcatExpr;
import org.iota.qupla.qupla.expression.CondExpr;
import org.iota.qupla.qupla.expression.FuncExpr;
import org.iota.qupla.qupla.expression.LutExpr;
import org.iota.qupla.qupla.expression.MergeExpr;
import org.iota.qupla.qupla.expression.SliceExpr;
import org.iota.qupla.qupla.expression.StateExpr;
import org.iota.qupla.qupla.expression.TypeExpr;
import org.iota.qupla.qupla.expression.VectorExpr;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.statement.FuncStmt;
import org.iota.qupla.qupla.statement.LutStmt;

public class QuplaRecursionContext extends QuplaBaseContext
{
  private static final boolean allowLog = true;

  public QuplaRecursionContext()
  {
  }

  @Override
  public void evalAssign(final AssignExpr assign)
  {
    assign.expr.eval(this);
  }

  @Override
  public void evalConcat(final ConcatExpr concat)
  {
    concat.lhs.eval(this);
    if (concat.rhs == null)
    {
      return;
    }

    concat.rhs.eval(this);
  }

  @Override
  public void evalConditional(final CondExpr conditional)
  {
    conditional.condition.eval(this);
    if (conditional.trueBranch == null)
    {
      return;
    }

    // if either expression is non-null the result is non-null
    conditional.trueBranch.eval(this);
    if (conditional.falseBranch == null)
    {
      return;
    }

    conditional.falseBranch.eval(this);
  }

  @Override
  public void evalFuncBody(final FuncStmt func)
  {
    if (func.recursion != 0)
    {
      if (func.recursion == 1)
      {
        log("RECURSION: " + func.name);
      }

      func.recursion = 3;
      return;
    }

    func.recursion++;
    for (final BaseExpr assignExpr : func.assignExprs)
    {
      assignExpr.eval(this);
    }

    func.returnExpr.eval(this);
    func.recursion--;
  }

  @Override
  public void evalFuncCall(final FuncExpr call)
  {
    for (final BaseExpr arg : call.args)
    {
      arg.eval(this);
    }

    call.func.eval(this);
  }

  @Override
  public void evalFuncSignature(final FuncStmt func)
  {
  }

  @Override
  public void evalLutDefinition(final LutStmt lut)
  {
  }

  @Override
  public void evalLutLookup(final LutExpr lookup)
  {
    // if any arg is null the result is null
    for (final BaseExpr arg : lookup.args)
    {
      arg.eval(this);
    }
  }

  @Override
  public void evalMerge(final MergeExpr merge)
  {
    // if either expression is non-null the result is non-null
    merge.lhs.eval(this);
    if (merge.rhs == null)
    {
      return;
    }

    merge.rhs.eval(this);
  }

  @Override
  public void evalSlice(final SliceExpr slice)
  {
  }

  @Override
  public void evalState(final StateExpr state)
  {
  }

  @Override
  public void evalType(final TypeExpr type)
  {
  }

  @Override
  public void evalVector(final VectorExpr vector)
  {
  }

  private void log(final String text)
  {
    if (allowLog)
    {
      Qupla.log(text);
    }
  }
}
