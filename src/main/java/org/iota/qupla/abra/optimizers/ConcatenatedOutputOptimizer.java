package org.iota.qupla.abra.optimizers;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteMerge;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.optimizers.base.BaseOptimizer;
import org.iota.qupla.qupla.expression.base.BaseExpr;

public class ConcatenatedOutputOptimizer extends BaseOptimizer
{
  public ConcatenatedOutputOptimizer(final AbraModule module, final AbraBlockBranch branch)
  {
    super(module, branch);
  }

  @Override
  protected void processKnot(final AbraSiteKnot knot)
  {
    // replace concatenation function with multiple outputs
    if (knot.block.specialType != AbraBaseBlock.TYPE_SLICE)
    {
      // concatenation is done by calling slice function
      return;
    }

    if (((AbraBlockBranch) knot.block).offset != 0)
    {
      // slicing should start at offset zero
      return;
    }

    int inputSize = 0;
    for (final AbraBaseSite input : knot.inputs)
    {
      inputSize += input.size;
    }

    if (inputSize != knot.size)
    {
      // this is an actual slice operation
      return;
    }

    // found an actual concatenation, replace with a series of merge outputs
    branch.outputs.clear();

    for (final AbraBaseSite input : knot.inputs)
    {
      if (input.references == 1 && branch.sites.contains(input))
      {
        branch.sites.remove(input);
        branch.outputs.add(input);
        input.references--;
        continue;
      }

      final AbraSiteMerge merge = new AbraSiteMerge();
      merge.size = input.size;
      merge.inputs.add(input);
      branch.outputs.add(merge);
    }

    if (knot.stmt != null)
    {
      final AbraBaseSite output = branch.outputs.get(0);
      if (output.stmt == null)
      {
        output.stmt = knot.stmt;
        return;
      }

      BaseExpr stmt = output.stmt;
      while (stmt.next != null)
      {
        stmt = stmt.next;
      }

      stmt.next = knot.stmt;
    }
  }

  @Override
  protected void processMerge(final AbraSiteMerge merge)
  {
    // replace single-input merge with single input
    if (merge.inputs.size() != 1)
    {
      // not a single-input merge
      return;
    }

    final AbraBaseSite input = merge.inputs.get(0);
    if (input.references != 1 || !branch.sites.contains(input))
    {
      // something else is referencing this as well
      // or we cannot move it anyway
      return;
    }

    // found a single-input merge, replace with its single input
    branch.outputs.clear();

    branch.sites.remove(input);
    branch.outputs.add(input);
    input.references--;

    if (merge.stmt != null)
    {
      final AbraBaseSite output = branch.outputs.get(0);
      if (output.stmt == null)
      {
        output.stmt = merge.stmt;
        return;
      }

      BaseExpr stmt = output.stmt;
      while (stmt.next != null)
      {
        stmt = stmt.next;
      }

      stmt.next = merge.stmt;
    }
  }

  @Override
  public void run()
  {
    if (branch.outputs.size() == 1)
    {
      final AbraBaseSite site = branch.outputs.get(0);
      if (site.getClass() == AbraSiteKnot.class)
      {
        processKnot((AbraSiteKnot) site);
      }

      if (site.getClass() == AbraSiteMerge.class)
      {
        processMerge((AbraSiteMerge) site);
      }
    }
  }
}
