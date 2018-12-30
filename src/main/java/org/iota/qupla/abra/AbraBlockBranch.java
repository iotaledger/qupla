package org.iota.qupla.abra;

import java.util.ArrayList;

import org.iota.qupla.abra.context.AbraCodeContext;
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
import org.iota.qupla.context.AbraContext;
import org.iota.qupla.context.CodeContext;

public class AbraBlockBranch extends AbraBlock
{
  public boolean anyNull;
  public ArrayList<AbraSite> inputs = new ArrayList<>();
  public ArrayList<AbraSite> latches = new ArrayList<>();
  public int offset;
  public int oldSize;
  public ArrayList<AbraSite> outputs = new ArrayList<>();
  public int siteNr;
  public ArrayList<AbraSite> sites = new ArrayList<>();
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

  @Override
  public boolean anyNull()
  {
    return anyNull;
  }

  @Override
  public CodeContext append(final CodeContext context)
  {
    super.append(context).newline().indent();

    numberSites();

    appendSites(context, inputs, "input");
    appendSites(context, sites, "body");
    appendSites(context, outputs, "output");
    appendSites(context, latches, "latch");

    return context.undent();
  }

  private void appendSites(final CodeContext context, final ArrayList<? extends AbraSite> sites, final String type)
  {
    for (final AbraSite site : sites)
    {
      site.type = type;
      site.append(context).newline();
    }
  }

  @Override
  public void code()
  {
    numberSites();

    putSites(inputs);
    putSites(sites);
    putSites(outputs);
    putSites(latches);
  }

  @Override
  public void eval(final AbraCodeContext context)
  {
    context.evalBranch(this);
  }

  @Override
  public void markReferences()
  {
    markReferences(inputs);
    markReferences(sites);
    markReferences(outputs);
    markReferences(latches);
  }

  private void markReferences(final ArrayList<? extends AbraSite> sites)
  {
    for (final AbraSite site : sites)
    {
      site.markReferences();
    }
  }

  private void numberSites()
  {
    siteNr = 0;
    numberSites(inputs);
    numberSites(sites);
    numberSites(outputs);
    numberSites(latches);
  }

  private void numberSites(final ArrayList<? extends AbraSite> sites)
  {
    for (final AbraSite site : sites)
    {
      site.index = siteNr++;
    }
  }

  @Override
  public int offset()
  {
    return offset;
  }

  @Override
  public void optimize(final AbraContext context)
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

  private void optimizeCleanup(final AbraContext context)
  {
    // bypass superfluous single-input merges
    new SingleInputMergeOptimizer(context, this).run();

    // and remove all unreferenced sites
    new UnreferencedSiteRemover(context, this).run();
  }

  private void optimizePass(final AbraContext context)
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

  private void putSites(final ArrayList<? extends AbraSite> sites)
  {
    for (final AbraSite site : sites)
    {
      site.code(tritCode);
    }
  }

  @Override
  public int size()
  {
    return size;
  }

  @Override
  public String type()
  {
    return "()";
  }
}
