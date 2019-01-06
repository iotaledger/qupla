package org.iota.qupla.qupla.context;

import java.util.ArrayList;

import org.iota.qupla.qupla.context.base.QuplaBaseContext;
import org.iota.qupla.qupla.expression.AffectExpr;
import org.iota.qupla.qupla.expression.AssignExpr;
import org.iota.qupla.qupla.expression.ConcatExpr;
import org.iota.qupla.qupla.expression.CondExpr;
import org.iota.qupla.qupla.expression.FieldExpr;
import org.iota.qupla.qupla.expression.FuncExpr;
import org.iota.qupla.qupla.expression.IntegerExpr;
import org.iota.qupla.qupla.expression.JoinExpr;
import org.iota.qupla.qupla.expression.LutExpr;
import org.iota.qupla.qupla.expression.MergeExpr;
import org.iota.qupla.qupla.expression.NameExpr;
import org.iota.qupla.qupla.expression.SliceExpr;
import org.iota.qupla.qupla.expression.StateExpr;
import org.iota.qupla.qupla.expression.SubExpr;
import org.iota.qupla.qupla.expression.TypeExpr;
import org.iota.qupla.qupla.expression.base.BaseBinaryExpr;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.expression.base.BaseSubExpr;
import org.iota.qupla.qupla.expression.constant.ConstFactor;
import org.iota.qupla.qupla.expression.constant.ConstNumber;
import org.iota.qupla.qupla.expression.constant.ConstSubExpr;
import org.iota.qupla.qupla.expression.constant.ConstTypeName;
import org.iota.qupla.qupla.parser.Module;
import org.iota.qupla.qupla.statement.ExecStmt;
import org.iota.qupla.qupla.statement.FuncStmt;
import org.iota.qupla.qupla.statement.ImportStmt;
import org.iota.qupla.qupla.statement.LutStmt;
import org.iota.qupla.qupla.statement.TemplateStmt;
import org.iota.qupla.qupla.statement.TypeStmt;
import org.iota.qupla.qupla.statement.UseStmt;
import org.iota.qupla.qupla.statement.helper.LutEntry;
import org.iota.qupla.qupla.statement.helper.TritStructDef;
import org.iota.qupla.qupla.statement.helper.TritVectorDef;

public class QuplaPrintContext extends QuplaBaseContext
{
  @Override
  public void eval(final Module module)
  {
    fileOpen("Qupla.txt");

    for (final ImportStmt imp : module.imports)
    {
      append("import ").append(imp.name).newline();
    }

    for (final TypeStmt type : module.types)
    {
      evalTypeDefinition(type);
    }

    super.eval(module);

    for (final TemplateStmt template : module.templates)
    {
      evalTemplateDefinition(template);
      newline();
    }

    for (final UseStmt use : module.uses)
    {
      evalUseDefinition(use);
      newline();
    }

    for (final ExecStmt exec : module.execs)
    {
      evalExec(exec);
      newline();
    }

    fileClose();
  }

  public void evalAffect(final AffectExpr affect)
  {
    append("affect ").append(affect.name);
    if (affect.delay != null)
    {
      append(" delay ").append(affect.delay.name);
    }
  }

  @Override
  public void evalAssign(final AssignExpr assign)
  {
    append(assign.name).append(" = ");
    assign.expr.eval(this);
  }

  @Override
  public void evalBaseExpr(final BaseExpr expr)
  {
    if (expr instanceof ConstTypeName)
    {
      append(expr.name);
      return;
    }

    if (expr instanceof ConstNumber)
    {
      append(expr.name);
      return;
    }

    if (expr instanceof TritVectorDef)
    {
      if (expr.name != null)
      {
        append(expr.name).append(" ");
      }

      evalTritVector((TritVectorDef) expr);
      return;
    }

    if (expr instanceof TritStructDef)
    {
      append(expr.name).append(" ");
      evalTritStruct((TritStructDef) expr);
      return;
    }

    if (expr instanceof LutEntry)
    {
      evalLutEntry((LutEntry) expr);
      return;
    }

    if (expr instanceof NameExpr)
    {
      final NameExpr nameExpr = (NameExpr) expr;
      if (nameExpr.type != null)
      {
        append(nameExpr.type.name).append(" ");
      }

      append(nameExpr.name);
      return;
    }

    if (expr instanceof BaseBinaryExpr)
    {
      final BaseBinaryExpr binaryExpr = (BaseBinaryExpr) expr;
      binaryExpr.lhs.eval(this);
      if (binaryExpr.rhs != null)
      {
        append(" " + binaryExpr.operator.text + " ");
        binaryExpr.rhs.eval(this);
      }
      return;
    }

    if (expr instanceof ExecStmt)
    {
      evalExec((ExecStmt) expr);
      return;
    }

    if (expr instanceof UseStmt)
    {
      evalUseDefinition((UseStmt) expr);
      return;
    }

    super.evalBaseExpr(expr);
  }

  @Override
  public void evalConcat(final ConcatExpr concat)
  {
    concat.lhs.eval(this);
    if (concat.rhs == null)
    {
      return;
    }

    append(" & ");
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

    append(" ? ");
    conditional.trueBranch.eval(this);
    append(" : ");
    if (conditional.falseBranch == null)
    {
      append("null");
      return;
    }

    conditional.falseBranch.eval(this);
  }

  private void evalExec(final ExecStmt exec)
  {
    if (exec.expected == null)
    {
      append("eval ");
      exec.expr.eval(this);
      return;
    }

    append("test ");
    exec.expected.eval(this);
    append(" = ");
    exec.expr.eval(this);
  }

  @Override
  public void evalFuncBody(final FuncStmt func)
  {
    newline();

    evalFuncBodySignature(func);

    append(" {").newline().indent();

    for (final BaseExpr envExpr : func.envExprs)
    {
      if (envExpr instanceof AffectExpr)
      {
        evalAffect((AffectExpr) envExpr);
        newline();
        continue;
      }

      evalJoin((JoinExpr) envExpr);
      newline();
    }

    for (final BaseExpr stateExpr : func.stateExprs)
    {
      stateExpr.eval(this);
      newline();
    }

    for (final BaseExpr assignExpr : func.assignExprs)
    {
      assignExpr.eval(this);
      newline();
    }

    append("return ");
    func.returnExpr.eval(this);
    newline();

    undent().append("}").newline();
  }

  public void evalFuncBodySignature(final FuncStmt func)
  {
    append("func ").append(func.returnType.name).append(" ").append(func.name.split("_")[0]);
    if (func.funcTypes.size() != 0)
    {
      boolean first = true;
      for (final BaseExpr funcType : func.funcTypes)
      {
        append(first ? "<" : ", ").append(funcType.name);
        first = false;
      }

      append(">");
    }

    boolean first = true;
    for (final BaseExpr param : func.params)
    {
      final NameExpr var = (NameExpr) param;
      append(first ? "(" : ", ").append(var.type.name).append(" ").append(var.name);
      first = false;
    }

    append(")");
  }

  @Override
  public void evalFuncCall(final FuncExpr call)
  {
    append(call.name.split("_")[0]);

    if (call.funcTypes.size() != 0)
    {
      boolean first = true;
      for (final BaseExpr funcType : call.funcTypes)
      {
        append(first ? "<" : ", ").append(funcType.name);
        first = false;
      }

      append(">");
    }

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

  }

  public void evalJoin(final JoinExpr join)
  {
    append("join ").append(join.name);
    if (join.limit != null)
    {
      append(" limit ").append(join.limit.name);
    }
  }

  @Override
  public void evalLutDefinition(final LutStmt lut)
  {
    newline();
    append("lut ").append(lut.name).append(" {").newline().indent();

    for (final LutEntry entry : lut.entries)
    {
      evalLutEntry(entry);
      newline();
    }

    undent().append("}").newline();
  }

  private void evalLutEntry(final LutEntry entry)
  {
    boolean first = true;
    for (int i = 0; i < entry.inputs.length(); i++)
    {
      append(first ? "" : ",").append(entry.inputs.substring(i, i + 1));
      first = false;
    }

    append(" = ");

    first = true;
    for (int i = 0; i < entry.outputs.length(); i++)
    {
      append(first ? "" : ",").append(entry.outputs.substring(i, i + 1));
      first = false;
    }
  }

  @Override
  public void evalLutLookup(final LutExpr lookup)
  {
    append(lookup.name);

    boolean first = true;
    for (final BaseExpr arg : lookup.args)
    {
      append(first ? "[" : ", ");
      first = false;
      arg.eval(this);
    }

    append("]");
  }

  @Override
  public void evalMerge(final MergeExpr merge)
  {
    merge.lhs.eval(this);
    if (merge.rhs == null)
    {
      return;
    }

    append(" | ");
    merge.rhs.eval(this);
  }

  @Override
  public void evalSlice(final SliceExpr slice)
  {
    append(slice.name);

    for (final BaseExpr field : slice.fields)
    {
      append(".").append(field.name);
    }

    if (slice.startOffset != null)
    {
      append("[");
      slice.startOffset.eval(this);

      if (slice.endOffset != null)
      {
        append(" : ");
        slice.endOffset.eval(this);
      }

      append("]");
    }
  }

  @Override
  public void evalState(final StateExpr state)
  {
    append("state ").append(state.stateType.name).append(" ").append(state.name);
  }

  @Override
  public void evalSubExpr(final BaseSubExpr sub)
  {
    if (sub instanceof ConstSubExpr || sub instanceof SubExpr)
    {
      append("(");
      super.evalSubExpr(sub);
      append(")");
      return;
    }

    if (sub instanceof ConstFactor)
    {
      final ConstFactor constFactor = (ConstFactor) sub;
      append(constFactor.negative ? "-" : "");
      super.evalSubExpr(constFactor);

      for (final BaseExpr field : constFactor.fields)
      {
        append(".").append(field.name);
      }

      return;
    }

    if (sub instanceof FieldExpr)
    {
      append(sub.name).append(" = ");
      super.evalSubExpr(sub);
      return;
    }

    super.evalSubExpr(sub);
  }

  private void evalTemplateDefinition(final TemplateStmt template)
  {
    newline();

    evalTemplateSignature(template);

    append("{").newline().indent();

    for (final BaseExpr type : template.types)
    {
      type.eval(this);
      newline();
    }

    for (final BaseExpr func : template.funcs)
    {
      func.eval(this);
      newline();
    }

    newline().undent().append("}").newline();
  }

  public void evalTemplateSignature(final TemplateStmt template)
  {
    append("template ").append(template.name);

    boolean first = true;
    for (final BaseExpr param : template.params)
    {
      append(first ? "<" : ", ").append(param.name);
      first = false;
    }

    append("> ");
  }

  private void evalTritStruct(final TritStructDef struct)
  {
    append("{").newline().indent();

    for (final BaseExpr field : struct.fields)
    {
      field.eval(this);
      newline();
    }

    undent().append("}");
  }

  private void evalTritVector(final TritVectorDef vector)
  {
    append("[");
    vector.typeExpr.eval(this);
    append("]");
  }

  @Override
  public void evalType(final TypeExpr type)
  {
    append(type.name).append("{").newline().indent();

    for (final BaseExpr field : type.fields)
    {
      field.eval(this);
      newline();
    }

    undent().append("}");
  }

  @Override
  public void evalTypeDefinition(final TypeStmt type)
  {
    append("type ").append(type.name).append(" ");
    if (type.struct != null)
    {
      evalTritStruct(type.struct);
      newline();
      return;
    }

    evalTritVector(type.vector);
    newline();
  }

  private void evalUseDefinition(final UseStmt use)
  {
    append("use ").append(use.name);

    boolean next = false;
    for (final ArrayList<BaseExpr> typeArgs : use.typeInstantiations)
    {
      append(next ? ", " : "");
      next = true;

      boolean first = true;
      for (final BaseExpr typeArg : typeArgs)
      {
        append(first ? "<" : ", ").append(typeArg.name);
        first = false;
      }

      append(">");
    }
  }

  @Override
  public void evalVector(final IntegerExpr integer)
  {
    append(integer.name);
  }
}
