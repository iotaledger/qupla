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

    if (assign.stateIndex != 0)
    {
      // move last site to latches
      branch.sites.remove(branch.sites.size() - 1);
      branch.latches.add(lastSite);

      // forward placeholder state site to actual state site
      final AbraSiteState state = (AbraSiteState) stack.get(assign.stateIndex);
      state.latch = lastSite;
    }
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

    final AbraSite condition = lastSite;

    conditional.trueBranch.eval(this);
    final AbraSite trueBranch = lastSite;
    trueBranch.nullifyTrue = condition;

    //    // create a site for nullifyTrue<size>(conditon, trueBranch)
    //    final AbraSiteKnot trueResult = new AbraSiteKnot();
    //    trueResult.size = conditional.size;
    //    trueResult.inputs.add(condition);
    //    trueResult.inputs.add(trueBranch);
    //    trueResult.nullifyTrue(this);
    //    addSite(trueResult);

    // create a site for trueBranch ( | falseBranch)
    final AbraSiteMerge merge = new AbraSiteMerge();
    merge.size = conditional.size;
    merge.inputs.add(trueBranch);

    if (conditional.falseBranch != null)
    {
      conditional.falseBranch.eval(this);
      final AbraSite falseBranch = lastSite;
      falseBranch.nullifyFalse = condition;

      //      // create a site for nullifyFalse<size>(conditon, falseBranch)
      //      final AbraSiteKnot falseResult = new AbraSiteKnot();
      //      falseResult.size = conditional.size;
      //      falseResult.inputs.add(condition);
      //      falseResult.inputs.add(falseBranch);
      //      falseResult.nullifyFalse(this);
      //      addSite(falseResult);

      merge.inputs.add(falseBranch);
    }

    addSite(merge);
  }

  @Override
  public void evalFuncBody(final FuncStmt func)
  {
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
      while (site.inputs.size() < 3)
      {
        site.inputs.add(site.inputs.get(0));
      }

      site.lut(this);
      addSite(site);

      concat.size += site.size;
      concat.inputs.add(site);
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
    final AbraSite varSite = stack.get(slice.stackIndex);

    if (slice.startOffset == null && slice.fields.size() == 0)
    {
      // entire variable, use single-input merge
      final AbraSiteMerge site = new AbraSiteMerge();
      site.from(slice);
      site.inputs.add(varSite);
      addSite(site);
      return;
    }

    // slice of variable, use correct slice function
    final AbraSiteKnot site = new AbraSiteKnot();
    site.from(slice);
    site.inputs.add(varSite);
    site.slice(this, varSite.size, slice.start);
    addSite(site);
  }

  @Override
  public void evalState(final StateExpr state)
  {
    // create placeholder for latch
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
    abra.optimize(this);
    abra.code();
    abra.append(this);

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
