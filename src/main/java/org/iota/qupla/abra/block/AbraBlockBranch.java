package org.iota.qupla.abra.block;

import java.util.ArrayList;

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
import org.iota.qupla.qupla.context.QuplaToAbraContext;

public class AbraBlockBranch extends AbraBaseBlock
{
  public ArrayList<AbraBaseSite> inputs = new ArrayList<>();
  public ArrayList<AbraBaseSite> latches = new ArrayList<>();
  public int offset;
  public ArrayList<AbraBaseSite> outputs = new ArrayList<>();
  public ArrayList<AbraBaseSite> sites = new ArrayList<>();
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
    inputSite.name = "P" + inputs.size();
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
  public void optimize(final QuplaToAbraContext context)
  {
    // first move the nullifies up the chain as far as possible
    new NullifyOptimizer(context, this).run();

    // then insert actual nullify operations and rewire accordingly
    new NullifyInserter(context, this).run();

    // remove some obvious nonsense before doing more complex analysis
    optimizeCleanup(context);

    // run the set of actual optimizations
    optimizePass(context);

    // and finally one last cleanup
    optimizeCleanup(context);
  }

  private void optimizeCleanup(final QuplaToAbraContext context)
  {
    // bypass superfluous single-input merges
    new SingleInputMergeOptimizer(context, this).run();

    // and remove all unreferenced sites
    new UnreferencedSiteRemover(context, this).run();
  }

  private void optimizePass(final QuplaToAbraContext context)
  {
    // bypass all function calls that do nothing
    new EmptyFunctionOptimizer(context, this).run();

    // replace lut wrapper function with direct lut operations
    new LutFunctionWrapperOptimizer(context, this).run();

    // pre-slice inputs that will be sliced later on
    new SlicedInputOptimizer(context, this).run();

    // replace concatenation knot that is passed as input to a knot
    new ConcatenationOptimizer(context, this).run();

    // if possible, replace lut calling lut with a single lut that does it all
    new MultiLutOptimizer(context, this).run();

    // move concatenated sites from body to outputs
    new ConcatenatedOutputOptimizer(context, this).run();
  }

  @Override
  public int size()
  {
    return size;
  }

  @Override
  public String toString()
  {
    return super.toString() + "()";
  }

  public int totalSites()
  {
    return inputs.size() + sites.size() + outputs.size() + latches.size();
  }
}
