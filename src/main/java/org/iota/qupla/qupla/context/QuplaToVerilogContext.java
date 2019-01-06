package org.iota.qupla.qupla.context;

import java.util.ArrayList;

import org.iota.qupla.helper.BaseContext;
import org.iota.qupla.helper.TritVector;
import org.iota.qupla.helper.Verilog;
import org.iota.qupla.qupla.context.base.QuplaBaseContext;
import org.iota.qupla.qupla.expression.AssignExpr;
import org.iota.qupla.qupla.expression.ConcatExpr;
import org.iota.qupla.qupla.expression.CondExpr;
import org.iota.qupla.qupla.expression.FuncExpr;
import org.iota.qupla.qupla.expression.IntegerExpr;
import org.iota.qupla.qupla.expression.LutExpr;
import org.iota.qupla.qupla.expression.MergeExpr;
import org.iota.qupla.qupla.expression.SliceExpr;
import org.iota.qupla.qupla.expression.StateExpr;
import org.iota.qupla.qupla.expression.TypeExpr;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.parser.Module;
import org.iota.qupla.qupla.statement.FuncStmt;
import org.iota.qupla.qupla.statement.LutStmt;
import org.iota.qupla.qupla.statement.helper.LutEntry;

public class QuplaToVerilogContext extends QuplaBaseContext
{
  private final Verilog verilog = new Verilog();

  private BaseContext appendVector(final String trits)
  {
    return verilog.appendVector(this, trits);
  }

  @Override
  public void eval(final Module module)
  {
    fileOpen("QuplaVerilog.txt");

    super.eval(module);

    verilog.addMergeLut(this);
    verilog.addMergeFuncs(this);

    fileClose();
  }

  @Override
  public void evalAssign(final AssignExpr assign)
  {
    append(assign.name).append(" = ");
    assign.expr.eval(this);
  }

  @Override
  public void evalConcat(final ConcatExpr concat)
  {
    if (concat.rhs == null)
    {
      concat.lhs.eval(this);
      return;
    }

    append("{ ");
    concat.lhs.eval(this);
    append(" : ");
    concat.rhs.eval(this);
    append(" }");
  }

  public void evalConcatExprs(final ArrayList<BaseExpr> exprs)
  {
    if (exprs.size() == 1)
    {
      final BaseExpr expr = exprs.get(0);
      expr.eval(this);
      return;
    }

    boolean first = true;
    for (final BaseExpr expr : exprs)
    {
      append(first ? "{ " : " : ");
      first = false;
      expr.eval(this);
    }

    append(" }");
  }

  @Override
  public void evalConditional(final CondExpr conditional)
  {
    //TODO proper handling of nullify when condition not in [1, -]
    conditional.condition.eval(this);
    append(" == ");
    appendVector("1").append(" ? ");
    conditional.trueBranch.eval(this);
    append(" : ");
    if (conditional.falseBranch == null)
    {
      appendVector(new TritVector(conditional.size, '@').trits());
      return;
    }

    conditional.falseBranch.eval(this);
  }

  @Override
  public void evalFuncBody(final FuncStmt func)
  {
    newline();

    final String funcName = func.name;
    append("function [" + (func.size * 2 - 1) + ":0] ").append(funcName).append("(").newline().indent();

    boolean first = true;
    for (final BaseExpr param : func.params)
    {
      append(first ? "  " : ", ");
      first = false;
      append("input [" + (param.size * 2 - 1) + ":0] ").append(param.name).newline();
    }

    append(");").newline();

    for (final BaseExpr assignExpr : func.assignExprs)
    {
      append("reg [" + (assignExpr.size * 2 - 1) + ":0] ").append(assignExpr.name).append(";").newline();
    }

    if (func.assignExprs.size() != 0)
    {
      newline();
    }

    //TODO state vars

    append("begin").newline().indent();

    for (final BaseExpr assignExpr : func.assignExprs)
    {
      assignExpr.eval(this);
      append(";").newline();
    }

    append(funcName).append(" = ");
    func.returnExpr.eval(this);
    append(";").newline().undent();

    append("end").newline().undent();
    append("endfunction").newline();
  }

  @Override
  public void evalFuncCall(final FuncExpr call)
  {
    append(call.name);

    boolean first = true;
    for (final BaseExpr arg : call.args)
    {
      append(first ? "(" : ", ");
      first = false;
      arg.eval(this);
    }

    append(")");
  }

  @Override
  public void evalFuncSignature(final FuncStmt func)
  {
    // generate Verilog forward declarations for functions?
  }

  @Override
  public void evalLutDefinition(final LutStmt lut)
  {
    final String lutName = lut.name + "_lut";
    append("function [" + (lut.size * 2 - 1) + ":0] ").append(lutName).append("(").newline().indent();

    boolean first = true;
    for (int i = 0; i < lut.inputSize; i++)
    {
      append(first ? "  " : ", ");
      first = false;
      append("input [1:0] ").append("p" + i).newline();
    }
    append(");").newline();

    append("begin").newline().indent();

    append("case ({");
    first = true;
    for (int i = 0; i < lut.inputSize; i++)
    {
      append(first ? "" : ", ").append("p" + i);
      first = false;
    }

    append("})").newline();

    for (final LutEntry entry : lut.entries)
    {
      appendVector(entry.inputs).append(": ").append(lutName).append(" = ");
      appendVector(entry.outputs).append(";").newline();
    }

    append("default: ").append(lutName).append(" = ");
    appendVector(lut.undefined.trits()).append(";").newline();
    append("endcase").newline().undent();

    append("end").newline().undent();
    append("endfunction").newline().newline();
  }

  @Override
  public void evalLutLookup(final LutExpr lookup)
  {
    append(lookup.name).append("_lut(");
    boolean first = true;
    for (final BaseExpr arg : lookup.args)
    {
      append(first ? "" : ", ");
      first = false;
      arg.eval(this);
    }

    append(")");
  }

  @Override
  public void evalMerge(final MergeExpr merge)
  {
    // if there is no rhs we return lhs
    if (merge.rhs == null)
    {
      merge.lhs.eval(this);
      return;
    }

    verilog.mergefuncs.add(merge.lhs.size);
    append(verilog.prefix + merge.lhs.size + "(");
    merge.lhs.eval(this);
    append(", ");
    merge.rhs.eval(this);
    append(")");
  }

  @Override
  public void evalSlice(final SliceExpr slice)
  {
    append(slice.name);
    if (slice.startOffset == null && slice.fields.size() == 0)
    {
      return;
    }

    final int start = slice.start * 2;
    final int end = start + slice.size * 2 - 1;
    append("[" + end + ":" + start + "]");
  }

  @Override
  public void evalState(final StateExpr state)
  {
    //    // save index of state variable to be able to distinguish
    //    // between multiple state vars in the same function
    //    callTrail[callNr] = (byte) state.stackIndex;
    //
    //    final StateValue call = new StateValue();
    //    call.path = callTrail;
    //    call.pathLength = callNr + 1;
    //
    //    // if state was saved before set to that value otherwise set to zero
    //    final StateValue stateValue = stateValues.get(call);
    //    value = stateValue != null ? stateValue.value : state.zero;
    //    stack.push(value);
  }

  @Override
  public void evalType(final TypeExpr type)
  {
    // type expression is a concatenation, but in declared field order
    // analyze will have sorted the fields in order already
    evalConcatExprs(type.fields);
  }

  @Override
  public void evalVector(final IntegerExpr integer)
  {
    appendVector(integer.vector.trits());
  }
}

