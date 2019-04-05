package org.iota.qupla.abra.block;

import java.util.ArrayList;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.block.site.AbraSiteParam;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.context.base.AbraBaseContext;
import org.iota.qupla.abra.optimizers.ConcatenatedOutputOptimizer;
import org.iota.qupla.abra.optimizers.ConcatenationOptimizer;
import org.iota.qupla.abra.optimizers.EmptyFunctionOptimizer;
import org.iota.qupla.abra.optimizers.LutFunctionWrapperOptimizer;
import org.iota.qupla.abra.optimizers.MultiLutOptimizer;
import org.iota.qupla.abra.optimizers.NullifyInserter;
import org.iota.qupla.abra.optimizers.NullifyOptimizer;
import org.iota.qupla.abra.optimizers.SingleInputMergeOptimizer;
import org.iota.qupla.abra.optimizers.SlicedInputOptimizer;
import org.iota.qupla.abra.optimizers.UnreferencedSiteRemover;

public class AbraBlockBranch extends AbraBaseBlock
{
  public final ArrayList<AbraBaseSite> inputs = new ArrayList<>();
  public final ArrayList<AbraBaseSite> latches = new ArrayList<>();
  public int offset;
  public final ArrayList<AbraBaseSite> outputs = new ArrayList<>();
  public final ArrayList<AbraBaseSite> sites = new ArrayList<>();
  public int size;

  public void addInput(final AbraSiteParam inputSite)
  {
    if (inputs.size() != 0)
    {
      final AbraSiteParam lastInput = (AbraSiteParam) inputs.get(inputs.size() - 1);
      inputSite.offset = lastInput.offset + lastInput.size;
    }

    inputs.add(inputSite);
  }

  public AbraSiteParam addInputParam(final int inputSize)
  {
    final AbraSiteParam inputSite = new AbraSiteParam();
    inputSite.size = inputSize;
    inputSite.name = "p" + inputs.size();
    addInput(inputSite);
    return inputSite;
  }

  private void clearReferences(final ArrayList<? extends AbraBaseSite> sites)
  {
    for (final AbraBaseSite site : sites)
    {
      site.references = 0;
    }
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
    clearReferences(sites);
    clearReferences(outputs);
    clearReferences(latches);

    markReferences(inputs);
    markReferences(sites);
    markReferences(outputs);
    markReferences(latches);
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
    siteNr = numberSites(siteNr, sites);
    siteNr = numberSites(siteNr, outputs);
    siteNr = numberSites(siteNr, latches);
  }

  private int numberSites(int siteNr, final ArrayList<? extends AbraBaseSite> sites)
  {
    for (final AbraBaseSite site : sites)
    {
      site.index = siteNr++;
    }

    return siteNr;
  }

  @Override
  public void optimize(final AbraModule module)
  {
    // first move the nullifies up the chain as far as possible
    new NullifyOptimizer(module, this).run();

    // then insert actual nullify operations and rewire accordingly
    new NullifyInserter(module, this).run();

    // remove some obvious nonsense before doing more complex analysis
    optimizeCleanup(module);

    // run the set of actual optimizations
    optimizePass(module);

    // and finally one last cleanup
    optimizeCleanup(module);
  }

  private void optimizeCleanup(final AbraModule module)
  {
    // bypass superfluous single-input merges
    new SingleInputMergeOptimizer(module, this).run();

    // and remove all unreferenced sites
    new UnreferencedSiteRemover(module, this).run();
  }

  private void optimizePass(final AbraModule module)
  {
    // bypass all function calls that do nothing
    new EmptyFunctionOptimizer(module, this).run();

    // replace lut wrapper function with direct lut operations
    new LutFunctionWrapperOptimizer(module, this).run();

    // pre-slice inputs that will be sliced later on
    new SlicedInputOptimizer(module, this).run();

    // replace concatenation knot that is passed as input to a knot
    new ConcatenationOptimizer(module, this).run();

    // if possible, replace lut calling lut with a single lut that does it all
    new MultiLutOptimizer(module, this).run();

    // move concatenated sites from body to outputs
    new ConcatenatedOutputOptimizer(module, this).run();
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
    return inputs.size() + sites.size() + outputs.size() + latches.size();
  }
}
