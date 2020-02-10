package org.iota.qupla.qupla.context;

import java.util.ArrayList;
import java.util.Stack;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockLut;
import org.iota.qupla.abra.block.AbraBlockSpecial;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteLatch;
import org.iota.qupla.abra.block.site.AbraSiteParam;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.context.AbraAnalyzeContext;
import org.iota.qupla.abra.context.AbraOrderBlockContext;
import org.iota.qupla.abra.context.AbraPrintContext;
import org.iota.qupla.abra.context.AbraReadDebugInfoContext;
import org.iota.qupla.abra.context.AbraReadTritCodeContext;
import org.iota.qupla.abra.context.AbraToVerilogContext;
import org.iota.qupla.abra.context.AbraViewTreeContext;
import org.iota.qupla.abra.context.AbraWriteDebugInfoContext;
import org.iota.qupla.abra.context.AbraWriteTritCodeContext;
import org.iota.qupla.exception.CodeException;
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
import org.iota.qupla.qupla.parser.QuplaModule;
import org.iota.qupla.qupla.statement.FuncStmt;
import org.iota.qupla.qupla.statement.LutStmt;
import org.iota.qupla.qupla.statement.UseStmt;
import org.iota.qupla.qupla.statement.helper.LutEntry;

//TODO fix double generated fullAdd_27

public class QuplaToAbraContext extends QuplaBaseContext
{
  public AbraModule abraModule = new AbraModule();
  private int bodies;
  public AbraBlockBranch branch;
  private AbraBaseSite lastSite;
  private AbraBaseSite nullifySite;
  private final Stack<AbraBaseSite> stack = new Stack<>();
  private BaseExpr stmt;

  public QuplaToAbraContext()
  {
  }

  private void addSite(final AbraSiteKnot site)
  {
    if (stmt != null)
    {
      site.stmt = stmt;
      stmt = null;
    }

    site.index = branch.totalSites();
    branch.sites.add(site);
    lastSite = site;
    nullifySite = site;
  }

  @Override
  public void eval(final QuplaModule module)
  {
    // generate dumb Abra code into abraModule
    super.eval(module);

    abraModule.optimize();

    new AbraOrderBlockContext().eval(abraModule);
    new AbraPrintContext().eval(abraModule);
    new AbraToVerilogContext().eval(abraModule);
    new AbraViewTreeContext().eval(abraModule);

    final AbraWriteTritCodeContext codeWriter = new AbraWriteTritCodeContext();
    codeWriter.eval(abraModule);

    final AbraWriteDebugInfoContext debugWriter = new AbraWriteDebugInfoContext();
    debugWriter.eval(abraModule);

    // we start a new AbraModule from the generated tritcode
    abraModule = new AbraModule();
    final AbraReadTritCodeContext codeReader = new AbraReadTritCodeContext();
    codeReader.buffer = new String(codeWriter.buffer, 0, codeWriter.bufferOffset).toCharArray();
    codeReader.eval(abraModule);

    final AbraReadDebugInfoContext debugReader = new AbraReadDebugInfoContext();
    debugReader.buffer = new String(debugWriter.buffer, 0, debugWriter.bufferOffset).toCharArray();
    debugReader.eval(abraModule);

    new AbraAnalyzeContext().eval(abraModule);

    final AbraPrintContext printer = new AbraPrintContext();
    printer.fileName = "NewAbra.txt";
    printer.eval(abraModule);
  }

  @Override
  public void evalAssign(final AssignExpr assign)
  {
    assign.expr.eval(this);
    lastSite.name = assign.name;
    stack.push(lastSite);

    if (assign.stateIndex != 0)
    {
      // point corresponding state site to site that needs to be latched
      final AbraSiteLatch state = (AbraSiteLatch) stack.get(assign.stateIndex);
      state.latchSite = lastSite;
    }
  }

  @Override
  public void evalConcat(final ConcatExpr concat)
  {
    final ArrayList<BaseExpr> exprs = new ArrayList<>();
    exprs.add(concat.lhs);
    if (concat.rhs != null)
    {
      exprs.add(concat.rhs);
    }

    evalConcatExprs(exprs);
  }

  private void evalConcatExprs(final ArrayList<BaseExpr> exprs)
  {
    final AbraSiteKnot site = new AbraSiteKnot();
    for (final BaseExpr expr : exprs)
    {
      expr.eval(this);
      site.size += expr.size;
      if (lastSite instanceof AbraSiteKnot)
      {
        final AbraSiteKnot knot = (AbraSiteKnot) lastSite;
        if (knot.block.index == AbraBlockSpecial.TYPE_CONCAT)
        {
          site.inputs.addAll(knot.inputs);
          continue;
        }
      }

      site.inputs.add(lastSite);
    }

    site.block = new AbraBlockSpecial(AbraBlockSpecial.TYPE_CONCAT, site.size);
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

    final AbraBaseSite condition = lastSite;

    conditional.trueBranch.eval(this);
    final AbraBaseSite trueBranch = lastSite;

    // note that actual insertion of nullifyTrue(condition, ...)
    // is done after nullify position has been optimized
    nullifySite.nullifyTrue = condition;

    if (conditional.falseBranch == null)
    {
      final AbraSiteKnot merge = new AbraSiteKnot();
      merge.size = conditional.size;
      merge.block = new AbraBlockSpecial(AbraBlockSpecial.TYPE_MERGE, merge.size);
      merge.inputs.add(trueBranch);
      addSite(merge);
      nullifySite = condition;
      return;
    }

    conditional.falseBranch.eval(this);
    final AbraBaseSite falseBranch = lastSite;

    // note that actual insertion of nullifyFalse(condition, ...)
    // is done after nullify position has been optimized
    nullifySite.nullifyFalse = condition;

    // create a site for trueBranch ( | falseBranch)
    final AbraSiteKnot merge = new AbraSiteKnot();
    merge.size = conditional.size;
    merge.block = new AbraBlockSpecial(AbraBlockSpecial.TYPE_MERGE, merge.size);
    merge.inputs.add(trueBranch);
    merge.inputs.add(falseBranch);
    addSite(merge);

    nullifySite = condition;
  }

  @Override
  public void evalFuncBody(final FuncStmt func)
  {
    final UseStmt oldUse = BaseExpr.currentUse;
    BaseExpr.currentUse = func.use;

    stack.clear();

    branch = abraModule.branches.get(bodies++);

    for (final BaseExpr param : func.params)
    {
      final AbraSiteParam site = new AbraSiteParam();
      site.from(param);
      site.name = param.name;
      stack.push(site);
      branch.addInput(site);
    }

    for (final BaseExpr stateExpr : func.stateExprs)
    {
      stateExpr.eval(this);
      branch.latches.add((AbraSiteLatch) lastSite);
    }

    for (final BaseExpr assignExpr : func.assignExprs)
    {
      stmt = assignExpr;
      assignExpr.eval(this);
    }

    stmt = func.returnExpr;
    func.returnExpr.eval(this);

    // last site will be output
    branch.outputs.add(lastSite);

    branch = null;
    BaseExpr.currentUse = oldUse;
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

    site.block = abraModule.branch(call.name);
    if (site.block == null)
    {
      throw new CodeException("Cannot find block: " + call.name);
    }

    addSite(site);
  }

  @Override
  public void evalFuncSignature(final FuncStmt func)
  {
    branch = new AbraBlockBranch();
    branch.origin = func;
    branch.name = func.name;
    branch.size = func.size;
    abraModule.addBranch(branch);
  }

  @Override
  public void evalLutDefinition(final LutStmt lut)
  {
    // note: lut output size can be >1, so we need a lut per output trit
    for (int tritNr = 0; tritNr < lut.size; tritNr++)
    {
      final char[] lookup = AbraBlockLut.LUT_NULL.toCharArray();

      for (final LutEntry entry : lut.entries)
      {
        // build index for this entry in lookup table
        int index = 0;
        int power = 1;
        for (int i = 0; i < lut.inputSize; i++)
        {
          final char trit = entry.inputs.charAt(i);
          if (trit != '-')
          {
            index += trit == '1' ? power * 2 : power;
          }

          power *= 3;
        }

        // set corresponding character
        lookup[index] = entry.outputs.charAt(tritNr);
      }

      // powers of 3:              1     3     9    27
      final int lookupSize = "\u0001\u0003\u0009\u001b".charAt(lut.inputSize);

      // repeat the entries across the entire table if necessary
      for (int offset = lookupSize; offset < 27; offset += lookupSize)
      {
        System.arraycopy(lookup, 0, lookup, offset, lookupSize);
      }

      final AbraBlockLut block = abraModule.addLut(lut.name + "_" + tritNr, new String(lookup));
      block.origin = lut;
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
      site.size = 1;
      site.inputs.addAll(args.inputs);
      if (AbraModule.lutAlways3)
      {
        while (site.inputs.size() < 3)
        {
          site.inputs.add(site.inputs.get(0));
        }
      }

      site.block = abraModule.findLut(lookup.name + "_" + i);
      if (site.block == null)
      {
        throw new CodeException("Cannot find lut: " + lookup.name + "_" + i);
      }

      addSite(site);

      concat.inputs.add(site);
    }

    if (concat.inputs.size() > 1)
    {
      concat.size = concat.inputs.size();
      concat.block = new AbraBlockSpecial(AbraBlockSpecial.TYPE_CONCAT, concat.size);
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

    final AbraSiteKnot site = new AbraSiteKnot();
    site.from(merge);
    site.block = new AbraBlockSpecial(AbraBlockSpecial.TYPE_MERGE, site.size);

    merge.lhs.eval(this);
    site.inputs.add(lastSite);

    merge.rhs.eval(this);
    site.inputs.add(lastSite);

    addSite(site);
  }

  @Override
  public void evalSlice(final SliceExpr slice)
  {
    final AbraBaseSite varSite = stack.get(slice.stackIndex);

    if (slice.sliceStart == null && slice.fields.size() == 0)
    {
      // entire variable, use single-input merge
      final AbraSiteKnot site = new AbraSiteKnot();
      site.from(slice);
      site.block = new AbraBlockSpecial(AbraBlockSpecial.TYPE_MERGE, site.size);
      site.inputs.add(varSite);
      addSite(site);
      return;
    }

    // slice of variable, use correct slice function
    final AbraSiteKnot site = new AbraSiteKnot();
    site.from(slice);
    site.block = new AbraBlockSpecial(AbraBlockSpecial.TYPE_SLICE, site.size, slice.start);
    site.inputs.add(varSite);
    addSite(site);
  }

  @Override
  public void evalState(final StateExpr state)
  {
    // create placeholder for latch
    final AbraSiteLatch site = new AbraSiteLatch();
    site.from(state);
    site.name = state.name;

    lastSite = site;
    stack.push(site);
  }

  @Override
  public void evalType(final TypeExpr type)
  {
    // type expression is a concatenation, but in declared field order
    // analyze will have sorted the fields in order already
    evalConcatExprs(type.fields);
  }

  @Override
  public void evalVector(final VectorExpr vector)
  {
    final AbraSiteKnot site = new AbraSiteKnot();
    site.from(vector);
    site.block = new AbraBlockSpecial(AbraBlockSpecial.TYPE_CONST, site.size, vector.vector);
    addSite(site);
  }
}
