package org.iota.qupla.qupla.context;

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
import org.iota.qupla.qupla.expression.base.BaseSubExpr;
import org.iota.qupla.qupla.parser.QuplaModule;
import org.iota.qupla.qupla.statement.FuncStmt;
import org.iota.qupla.qupla.statement.LutStmt;
import org.iota.qupla.qupla.statement.TypeStmt;

public class QuplaTreeViewerContext extends QuplaBaseContext
{
  @Override
  protected void appendify(final String text)
  {
    super.appendify(text.length() > 150 ? text.substring(0, 150) + " ..." : text);
  }

  @Override
  public void eval(final QuplaModule module)
  {
    fileOpen("QuplaTree.txt");

    for (final TypeStmt type : module.types)
    {
      evalTypeDefinition(type);
    }


    super.eval(module);
    fileClose();
  }

  @Override
  public void evalAssign(final AssignExpr assign)
  {
    append("evalAssign: " + assign).newline();
    indent();
    assign.expr.eval(this);
    undent();
  }

  @Override
  public void evalBaseExpr(final BaseExpr expr)
  {
    append("evalBaseExpr: " + expr).newline();
  }

  @Override
  public void evalConcat(final ConcatExpr concat)
  {
    append("evalConcat: " + concat).newline();
    indent();
    concat.lhs.eval(this);
    concat.rhs.eval(this);
    undent();
  }

  @Override
  public void evalConditional(final CondExpr conditional)
  {
    append("evalConditional: " + conditional).newline();
    indent();
    conditional.condition.eval(this);
    conditional.trueBranch.eval(this);
    if (conditional.falseBranch != null)
    {
      conditional.falseBranch.eval(this);
    }

    undent();
  }

  @Override
  public void evalFuncBody(final FuncStmt func)
  {
    append("\nevalFuncBody: " + func).newline();
    indent();
    for (final BaseExpr stateExpr : func.stateExprs)
    {
      stateExpr.eval(this);
    }

    for (final BaseExpr assignExpr : func.assignExprs)
    {
      assignExpr.eval(this);
    }

    func.returnExpr.eval(this);
    undent();
  }

  @Override
  public void evalFuncCall(final FuncExpr call)
  {
    append("evalFuncCall: " + call).newline();
    indent();
    for (final BaseExpr arg : call.args)
    {
      arg.eval(this);
    }

    undent();
  }

  @Override
  public void evalFuncSignature(final FuncStmt func)
  {
    append("evalFuncSignature: " + func).newline();
  }

  @Override
  public void evalLutDefinition(final LutStmt lut)
  {
    append("evalLutDefinition: " + lut).newline();
  }

  @Override
  public void evalLutLookup(final LutExpr lookup)
  {
    append("evalLutLookup: " + lookup).newline();
    indent();
    for (final BaseExpr arg : lookup.args)
    {
      arg.eval(this);
    }

    undent();
  }

  @Override
  public void evalMerge(final MergeExpr merge)
  {
    append("evalMerge: " + merge).newline();
    indent();
    merge.lhs.eval(this);
    if (merge.rhs != null)
    {
      merge.rhs.eval(this);
    }

    undent();

  }

  @Override
  public void evalSlice(final SliceExpr slice)
  {
    append("evalSlice: " + slice).newline();
  }

  @Override
  public void evalState(final StateExpr state)
  {
    append("evalState: " + state).newline();
  }

  @Override
  public void evalSubExpr(final BaseSubExpr sub)
  {
    append("evalSubExpr: " + sub).newline();
    indent();
    super.evalSubExpr(sub);
    undent();
  }

  @Override
  public void evalType(final TypeExpr type)
  {
    append("evalType: " + type).newline();
  }

  @Override
  public void evalTypeDefinition(final TypeStmt type)
  {
    append("evalTypeDefinition: " + type).newline();
  }

  @Override
  public void evalVector(final VectorExpr vector)
  {
    append("evalVector: " + vector).newline();
  }
}
