package org.iota.qupla.context;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;

import org.iota.qupla.abra.AbraBlockBranch;
import org.iota.qupla.abra.AbraBlockLut;
import org.iota.qupla.abra.AbraCode;
import org.iota.qupla.abra.AbraSite;
import org.iota.qupla.abra.AbraSiteKnot;
import org.iota.qupla.abra.AbraSiteMerge;
import org.iota.qupla.abra.AbraSiteParam;
import org.iota.qupla.abra.AbraSiteState;
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

public class AbraContext extends CodeContext
{
  public AbraCode abra = new AbraCode();
  public int bodies;
  public AbraBlockBranch branch;
  public File file;
  public AbraSite lastSite;
  public BufferedWriter out;
  public Stack<AbraSite> stack = new Stack<>();
  public BaseExpr stmt;
  public FileWriter writer;

  public AbraContext()
  {
    try
    {
      file = new File("Abra.txt");
      writer = new FileWriter(file);
      out = new BufferedWriter(writer);
    }
    catch (final IOException e)
    {
      e.printStackTrace();
    }
  }

  private void addSite(final AbraSite site)
  {
    if (stmt != null)
    {
      site.stmt = stmt;
      stmt = null;
    }

    branch.sites.add(site);
    lastSite = site;
  }

  @Override
  protected void appendify(final String text)
  {
    try
    {
      out.write(text);
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }

  @Override
  public void evalAssign(final AssignExpr assign)
  {
    assign.expr.eval(this);
    stack.push(lastSite);
  }

  @Override
  public void evalConcat(final ArrayList<BaseExpr> exprs)
  {
    final AbraSiteKnot site = new AbraSiteKnot();
    for (final BaseExpr expr : exprs)
    {
      expr.eval(this);
      site.size += expr.size;
      site.inputs.add(lastSite);
    }

    site.concat(this);

    addSite(site);
  }

  @Override
  public void evalConditional(final CondExpr conditional)
  {
    conditional.condition.eval(this);
    if (conditional.trueBranch == null)
    {
      // not really a conditional operator
      // should have been optimized away
      return;
    }

    final AbraSite trueCondition = lastSite;

    conditional.trueBranch.eval(this);
    final AbraSite trueBranch = lastSite;

    // create a site for nullify<size>(trueConditon, trueBranch)
    final AbraSiteKnot trueResult = new AbraSiteKnot();
    trueResult.size = conditional.size;
    trueResult.inputs.add(trueCondition);
    trueResult.inputs.add(trueBranch);
    trueResult.name = "nullify$" + conditional.size;
    trueResult.branch(this); //TODO if missing add it
    addSite(trueResult);

    conditional.falseBranch.eval(this);
    final AbraSite falseBranch = lastSite;

    // create a site for not[trueConditon]
    final AbraSiteKnot falseCondition = new AbraSiteKnot();
    falseCondition.size = 1;
    falseCondition.inputs.add(trueCondition);
    falseCondition.name = "not$0";
    falseCondition.lut(this); //TODO if missing add it
    addSite(falseCondition);

    // create a site for nullify<size>(falseConditon, falseBranch)
    final AbraSiteKnot falseResult = new AbraSiteKnot();
    falseResult.size = conditional.size;
    falseResult.inputs.add(falseCondition);
    falseResult.inputs.add(falseBranch);
    falseResult.name = "nullify$" + conditional.size;
    falseResult.branch(this); //TODO if missing add it
    addSite(falseResult);

    // create a site for trueResult | falseResult
    final AbraSiteMerge merge = new AbraSiteMerge();
    merge.size = conditional.size;
    merge.inputs.add(trueResult);
    merge.inputs.add(falseResult);
    addSite(merge);
  }

  @Override
  public void evalFuncBody(final FuncStmt func)
  {
    if (func.name.startsWith("as$") || func.name.startsWith("break$") || func.name.startsWith("print$"))
    {
      // no-op: parameter is returned as is
      return;
    }

    stack.clear();

    branch = abra.branches.get(bodies++);

    for (final BaseExpr param : func.params)
    {
      final AbraSiteParam site = new AbraSiteParam();
      site.from(param);
      stack.push(site);
      branch.inputs.add(site);
    }

    for (final BaseExpr stateExpr : func.stateExprs)
    {
      stateExpr.eval(this);
      branch.latches.add(lastSite);
    }

    for (final BaseExpr assignExpr : func.assignExprs)
    {
      stmt = assignExpr;
      assignExpr.eval(this);
    }

    stmt = func.returnExpr;
    func.returnExpr.eval(this);

    // move last site to outputs
    branch.sites.remove(branch.sites.size() - 1);
    branch.outputs.add(lastSite);

    branch = null;
  }

  @Override
  public void evalFuncCall(final FuncExpr call)
  {
    if (call.name.startsWith("as$") || call.name.startsWith("break$") || call.name.startsWith("print$"))
    {
      // no-op: single parameter is returned as is
      for (final BaseExpr arg : call.args)
      {
        arg.eval(this);
      }

      return;
    }

    final AbraSiteKnot site = new AbraSiteKnot();
    site.from(call);

    for (final BaseExpr arg : call.args)
    {
      arg.eval(this);
      site.inputs.add(lastSite);
    }

    site.branch(this);

    addSite(site);
  }

  @Override
  public void evalFuncSignature(final FuncStmt func)
  {
    if (func.name.startsWith("as$") || func.name.startsWith("break$") || func.name.startsWith("print$"))
    {
      // no-op: parameter is returned as is
      return;
    }

    branch = new AbraBlockBranch();
    branch.origin = func;
    branch.name = func.name;
    branch.size = func.size;
    abra.branches.add(branch);
  }

  @Override
  public void evalLutDefinition(final LutStmt lut)
  {
    // note: lut output size can be >1, so we need a lut per output trit
    for (int i = 0; i < lut.size; i++)
    {
      final AbraBlockLut block = new AbraBlockLut();
      block.origin = lut;
      block.name = lut.name + "$" + i;
      block.tritNr = i;
      abra.luts.add(block);
    }
  }

  @Override
  public void evalLutLookup(final LutExpr lookup)
  {
    final AbraSiteKnot args = new AbraSiteKnot();
    for (final BaseExpr arg : lookup.args)
    {
      arg.eval(this);
      args.inputs.add(lastSite);
    }

    final AbraSiteKnot concat = new AbraSiteKnot();

    for (int i = 0; i < lookup.size; i++)
    {
      final AbraSiteKnot site = new AbraSiteKnot();
      site.from(lookup);
      site.name += "$" + i;
      site.inputs.addAll(args.inputs);

      site.lut(this);

      concat.size += site.size;
      concat.inputs.add(site);

      addSite(site);
    }

    if (concat.inputs.size() > 1)
    {
      concat.concat(this);

      addSite(concat);
    }
  }

  @Override
  public void evalMerge(final MergeExpr merge)
  {
    if (merge.rhs == null)
    {
      merge.lhs.eval(this);
      return;
    }

    final AbraSiteMerge site = new AbraSiteMerge();
    site.from(merge);

    merge.lhs.eval(this);
    site.inputs.add(lastSite);

    merge.rhs.eval(this);
    site.inputs.add(lastSite);

    addSite(site);
  }

  @Override
  public void evalSlice(final SliceExpr slice)
  {
    lastSite = stack.get(slice.stackIndex);

    if (slice.startOffset == null && slice.fields.size() == 0)
    {
      if (stmt == slice || (stmt instanceof AssignExpr && ((AssignExpr) stmt).expr == slice))
      {
        final AbraSiteMerge site = new AbraSiteMerge();
        site.from(slice);
        site.inputs.add(lastSite);

        addSite(site);
      }

      return;
    }

    final AbraSiteKnot site = new AbraSiteKnot();
    site.from(slice);
    site.inputs.add(lastSite);

    site.slice(this, slice.start);

    addSite(site);
  }

  @Override
  public void evalState(final StateExpr state)
  {
    final AbraSiteState site = new AbraSiteState();
    site.from(state);

    lastSite = site;
    stack.push(site);
  }

  @Override
  public void evalVector(final IntegerExpr integer)
  {
    final AbraSiteKnot site = new AbraSiteKnot();
    site.from(integer);
    site.inputs.add(branch.inputs.get(0));

    site.vector(this, integer.vector.trits);

    addSite(site);
  }

  @Override
  public void finished()
  {
    abra.append(this);
    abra.code();

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
    catch (final IOException e)
    {
      e.printStackTrace();
    }
  }
}
