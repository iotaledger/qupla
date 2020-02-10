package org.iota.qupla.abra.block;

import java.util.ArrayList;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteLatch;
import org.iota.qupla.abra.block.site.AbraSiteParam;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.context.base.AbraBaseContext;
import org.iota.qupla.abra.optimizers.ConcatenationOptimizer;
import org.iota.qupla.abra.optimizers.ConstantTritOptimizer;
import org.iota.qupla.abra.optimizers.DuplicateSiteOptimizer;
import org.iota.qupla.abra.optimizers.EmptyFunctionOptimizer;
import org.iota.qupla.abra.optimizers.LutFunctionWrapperOptimizer;
import org.iota.qupla.abra.optimizers.MultiLutOptimizer;
import org.iota.qupla.abra.optimizers.NullifyInserter;
import org.iota.qupla.abra.optimizers.NullifyOptimizer;
import org.iota.qupla.abra.optimizers.SingleInputMergeOptimizer;
import org.iota.qupla.abra.optimizers.SlicedInputOptimizer;
import org.iota.qupla.abra.optimizers.UnreferencedSiteRemover;
import org.iota.qupla.qupla.expression.base.BaseExpr;

public class AbraBlockBranch extends AbraBaseBlock
{
  public BaseExpr finalStmt;
  public final ArrayList<AbraSiteParam> inputs = new ArrayList<>();
  public final ArrayList<AbraSiteLatch> latches = new ArrayList<>();
  public final ArrayList<AbraBaseSite> outputs = new ArrayList<>();
  public final ArrayList<AbraSiteKnot> sites = new ArrayList<>();
  public int size;

  public void addInput(final AbraSiteParam inputSite)
  {
    if (inputs.size() != 0)
    {
      final AbraSiteParam lastInput = inputs.get(inputs.size() - 1);
      inputSite.offset = lastInput.offset + lastInput.size;
    }

    inputSite.index = inputs.size();
    inputs.add(inputSite);
  }

  public void addInputParam(final int inputSize)
  {
    final AbraSiteParam inputSite = new AbraSiteParam();
    inputSite.size = inputSize;
    inputSite.name = "p" + inputs.size();
    addInput(inputSite);
  }

  private void clearReferences(final ArrayList<? extends AbraBaseSite> sites)
  {
    for (final AbraBaseSite site : sites)
    {
      site.references = 0;
    }
  }

  public AbraBlockBranch clone()
  {
    final AbraBlockBranch branch = new AbraBlockBranch();
    branch.name = name;
    branch.size = size;

    for (final AbraSiteParam input : inputs)
    {
      branch.inputs.add(new AbraSiteParam(input));
    }

    for (final AbraSiteLatch latch : latches)
    {
      branch.latches.add(new AbraSiteLatch(latch));
    }

    for (final AbraSiteKnot knot : sites)
    {
      branch.sites.add(new AbraSiteKnot(knot));
    }

    branch.numberSites();

    final ArrayList<AbraBaseSite> branchSites = new ArrayList<>();
    branchSites.addAll(branch.inputs);
    branchSites.addAll(branch.latches);
    branchSites.addAll(branch.sites);

    for (int i = 0; i < latches.size(); i++)
    {
      final AbraSiteLatch src = latches.get(i);
      final AbraSiteLatch dst = branch.latches.get(i);
      dst.latchSite = src.latchSite == null ? null : branchSites.get(src.latchSite.index);
    }

    for (int i = 0; i < sites.size(); i++)
    {
      final AbraSiteKnot src = sites.get(i);
      final AbraSiteKnot dst = branch.sites.get(i);
      for (final AbraBaseSite input : src.inputs)
      {
        final AbraBaseSite newInput = branchSites.get(input.index);
        dst.inputs.add(newInput);
        newInput.references++;
      }
    }

    for (final AbraBaseSite output : outputs)
    {
      final AbraBaseSite newOutput = branchSites.get(output.index);
      branch.outputs.add(newOutput);
      newOutput.references++;
    }

    return branch;
  }

  @Override
  public boolean couldBeLutWrapper()
  {
    if (size != 1 || inputs.size() > 3)
    {
      return false;
    }

    for (final AbraBaseSite input : inputs)
    {
      if (input.size != 1)
      {
        return false;
      }
    }

    return true;
  }

  @Override
  public void eval(final AbraBaseContext context)
  {
    context.evalBranch(this);
  }

  @Override
  public void markReferences()
  {
    clearReferences(inputs);
    clearReferences(latches);
    clearReferences(sites);

    markReferences(latches);
    markReferences(sites);
    for (final AbraBaseSite output : outputs)
    {
      output.references++;
    }
  }

  private void markReferences(final ArrayList<? extends AbraBaseSite> sites)
  {
    for (final AbraBaseSite site : sites)
    {
      site.markReferences();
    }
  }

  public void numberSites()
  {
    int siteNr = 0;
    siteNr = numberSites(siteNr, inputs);
    siteNr = numberSites(siteNr, latches);
    siteNr = numberSites(siteNr, sites);
  }

  private int numberSites(int siteNr, final ArrayList<? extends AbraBaseSite> sites)
  {
    for (final AbraBaseSite site : sites)
    {
      site.index = siteNr++;
    }

    return siteNr;
  }

  public void optimize(final AbraModule module)
  {
    // first move the nullifies up the chain as far as possible
    new NullifyOptimizer(module, this).run();

    // then insert actual nullify operations and rewire accordingly
    new NullifyInserter(module, this).run();

    // replace all single-input merges with direct references
    new SingleInputMergeOptimizer(module, this).run();

    // run the set of actual optimizations
    optimizePass(module);

    // and finally remove all unreferenced sites
    new UnreferencedSiteRemover(module, this).run();
  }

  private void optimizePass(final AbraModule module)
  {
    // bypass all function calls that do nothing
    new EmptyFunctionOptimizer(module, this).run();

    // pre-slice inputs that will be sliced later on
    new SlicedInputOptimizer(module, this).run();

    // replace lut wrapper function with direct lut operations
    new LutFunctionWrapperOptimizer(module, this).run();

    // replace concatenation knot that is passed as input to a knot
    new ConcatenationOptimizer(module, this).run();

    // remove duplicate sites, only need to calculate once
    new DuplicateSiteOptimizer(module, this).run();

    // replace single-trit nullifies and constants with LUTs
    new ConstantTritOptimizer(module, this).run();

    // if possible, replace lut calling lut with a single lut that does it all
    new MultiLutOptimizer(module, this).run();
  }

  @Override
  public int size()
  {
    return size;
  }

  @Override
  public String toString()
  {
    return name + "()";
  }

  public int totalSites()
  {
    return inputs.size() + latches.size() + sites.size();
  }
}
