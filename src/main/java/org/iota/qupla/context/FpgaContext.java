package org.iota.qupla.context;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import org.iota.qupla.expression.AssignExpr;
import org.iota.qupla.expression.CondExpr;
import org.iota.qupla.expression.FuncExpr;
import org.iota.qupla.expression.IntegerExpr;
import org.iota.qupla.expression.LutExpr;
import org.iota.qupla.expression.MergeExpr;
import org.iota.qupla.expression.SliceExpr;
import org.iota.qupla.expression.StateExpr;
import org.iota.qupla.expression.base.BaseExpr;
import org.iota.qupla.helper.TritVector;
import org.iota.qupla.statement.FuncStmt;
import org.iota.qupla.statement.LutStmt;
import org.iota.qupla.statement.helper.LutEntry;

public class FpgaContext extends CodeContext
{
  private static final boolean allowLog = true;

  public File file;
  public HashSet<Integer> mergefuncs = new HashSet<>();
  public BufferedWriter out;
  public FileWriter writer;

  public FpgaContext()
  {
    try
    {
      file = new File("Verilog.txt");
      writer = new FileWriter(file);
      out = new BufferedWriter(writer);
    }
    catch (final IOException e)
    {
      e.printStackTrace();
    }
  }

  private void addMergeFuncs()
  {
    newline().append("x reg [0:0];").newline();
    newline().append("function [1:0] Qupla_merge(").newline().indent();
    append("  input [1:0] input1").newline();
    append(", input [1:0] input2").newline();
    append(");").newline();
    append("begin").newline().indent();
    append("case ({input1, input2})").newline();
    append("4'b0000: Qupla_merge = 2'b00;").newline();
    append("4'b0001: Qupla_merge = 2'b01;").newline();
    append("4'b0010: Qupla_merge = 2'b10;").newline();
    append("4'b0011: Qupla_merge = 2'b11;").newline();
    append("4'b0100: Qupla_merge = 2'b01;").newline();
    append("4'b1000: Qupla_merge = 2'b10;").newline();
    append("4'b1100: Qupla_merge = 2'b11;").newline();
    append("4'b0101: Qupla_merge = 2'b01;").newline();
    append("4'b1010: Qupla_merge = 2'b10;").newline();
    append("4'b1111: Qupla_merge = 2'b11;").newline();
    append("default: Qupla_merge = 2'b00;").newline();
    append("         x <= 1;").newline();
    append("endcase").newline().undent();
    append("end").newline().undent();
    append("endfunction").newline();

    for (final Integer size : mergefuncs)
    {
      final String funcName = "Qupla_merge_" + size;
      newline().append("function [" + (size * 2 - 1) + ":0] ").append(funcName).append("(").newline().indent();
      append("  input [" + (size * 2 - 1) + ":0] input1").newline();
      append(", input [" + (size * 2 - 1) + ":0] input2").newline();
      append(");").newline();
      append("begin").newline().indent();
      append(funcName).append(" = {").newline().indent();
      boolean first = true;
      for (int i = 0; i < size; i++)
      {
        final int from = i * 2 + 1;
        final int to = i * 2;
        append(first ? "" : ": ").append("Qupla_merge(input1[" + from + ":" + to + "], input2[" + from + ":" + to + "])").newline();
        first = false;
      }
      undent();
      append("};").newline().undent();
      append("end").newline().undent();
      append("endfunction").newline();
    }
  }

  @Override
  protected void appendify(final String text)
  {
    try
    {
      out.write(text);
    }
    catch (final IOException e)
    {
      e.printStackTrace();
    }
  }

  public void cleanup()
  {
    // clean up after evaluation and compilation
    // file.delete();
  }

  @Override
  public void evalAssign(final AssignExpr assign)
  {
    append(assign.name).append(" = ");
    assign.expr.eval(this);
  }

  @Override
  public void evalConcat(final ArrayList<BaseExpr> exprs)
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
    tritVector("1").append(" ? ");
    conditional.trueBranch.eval(this);
    append(" : ");
    if (conditional.falseBranch == null)
    {
      tritVector(new TritVector(conditional.size).trits);
      return;
    }

    conditional.falseBranch.eval(this);
  }

  @Override
  public void evalFuncBody(final FuncStmt func)
  {
    newline();

    final String funcName = func.name.replace('$', '_');
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
    append(call.name.replace('$', '_'));

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
    // generate Verilog forward declarations for functions
  }

  @Override
  public void evalLutDefinition(final LutStmt lut)
  {
    final String lutName = lut.name.replace('$', '_') + "_lut";
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
      tritVector(entry.inputs).append(": ").append(lutName).append(" = ");
      tritVector(entry.outputs).append(";").newline();
    }

    append("default: ").append(lutName).append(" = ");
    tritVector(lut.undefined.trits).append(";").newline();
    append("endcase").newline().undent();

    append("end").newline().undent();
    append("endfunction").newline().newline();
  }

  @Override
  public void evalLutLookup(final LutExpr lookup)
  {
    append(lookup.name.replace('$', '_')).append("_lut(");
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

    mergefuncs.add(merge.lhs.size);
    append(("Qupla_merge_" + merge.lhs.size) + "(");
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
  public void evalVector(final IntegerExpr integer)
  {
    tritVector(integer.vector.trits);
  }

  @Override
  public void finished()
  {
    addMergeFuncs();

    try
    {
      if (out != null)
      {
        out.close();
      }

      if (writer != null)
      {
        writer.close();
      }
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }

  public void log(final String text, final TritVector vector, final BaseExpr expr)
  {
    if (!allowLog)
    {
      // avoid converting vector to string, which is slow
      return;
    }

    log(text + vector + " : " + expr);
  }

  public void log(final String text)
  {
    if (allowLog)
    {
      BaseExpr.logLine(text);
    }
  }

  private CodeContext tritVector(final String trits)
  {
    final int size = trits.length() * 2;
    append(size + "'b");
    for (int i = 0; i < trits.length(); i++)
    {
      switch (trits.charAt(i))
      {
      case '0':
        append("00");
        break;
      case '1':
        append("01");
        break;
      case '-':
        append("11");
        break;
      case '@':
        append("10");
        break;
      }
    }

    return this;
  }
}

