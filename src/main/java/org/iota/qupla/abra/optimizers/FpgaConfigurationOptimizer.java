package org.iota.qupla.abra.optimizers;

import java.util.ArrayList;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockLut;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteMerge;
import org.iota.qupla.abra.block.site.AbraSiteParam;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.optimizers.base.BaseOptimizer;
import org.iota.qupla.exception.CodeException;

// first slice all input vectors into trit-sized slices
// replace all site inputs with those trits
// replace all call knots with the contents of the branch they call
// replace their inputs directly with the actual input parameters
// replace all site inputs with the branch output trits
//TODO how to deal with latches?

public class FpgaConfigurationOptimizer extends BaseOptimizer
{
  private final ArrayList<AbraBaseSite> inputs = new ArrayList<>();
  private final ArrayList<AbraBaseSite> outputs = new ArrayList<>();
  private final ArrayList<ArrayList<AbraBaseSite>> siteTrits = new ArrayList<>();
  private final ArrayList<AbraBaseSite> sites = new ArrayList<>();

  public FpgaConfigurationOptimizer(final AbraModule module, final AbraBlockBranch branch)
  {
    super(module, branch);
  }

  private void addInputs(final AbraSiteMerge from, final AbraSiteMerge to)
  {
    for (final AbraBaseSite input : from.inputs)
    {
      final ArrayList<AbraBaseSite> newInputs = siteTrits.get(input.index);
      to.inputs.addAll(newInputs);
      for (final AbraBaseSite newInput : newInputs)
      {
        newInput.references++;
      }
    }
  }

  private void instantiateSites(final ArrayList<AbraBaseSite> sites, final AbraBlockBranch call, final ArrayList<AbraBaseSite> params)
  {
    for (final AbraBaseSite callSite : sites)
    {
      final AbraSiteMerge site = (AbraSiteMerge) callSite;
      for (int i = 0; i < site.inputs.size(); i++)
      {
        final AbraBaseSite in = site.inputs.get(i);
        if (in.index < call.inputs.size())
        {
          final AbraBaseSite param = params.get(in.index);
          in.references--;
          site.inputs.set(i, param);
          param.references++;
        }
      }
    }
  }

  private void processInput()
  {
    final ArrayList<AbraBaseSite> trits = new ArrayList<>(branch.inputs.size());
    final AbraSiteParam input = (AbraSiteParam) branch.inputs.get(index);
    for (int i = 0; i < input.size; i++)
    {
      final AbraSiteParam param = new AbraSiteParam();
      param.index = sites.size();
      param.offset = input.offset + i;
      param.size = 1;
      if (input.name != null)
      {
        param.name = input.name + (input.size != 1 ? "_" + i : "");
      }

      if (input.varName != null)
      {
        param.varName = input.varName + (input.size != 1 ? "_" + i : "");
      }

      inputs.add(param);
      trits.add(param);
    }

    siteTrits.add(trits);
  }

  @Override
  protected void processKnot(final AbraSiteKnot knot)
  {
    if (knot.block instanceof AbraBlockLut)
    {
      sites.add(processKnotLut(knot));
      return;
    }

    if (!(knot.block instanceof AbraBlockBranch))
    {
      throw new CodeException("Unknown block");
    }

    processKnotBranch(knot);
  }

  private void processKnotBranch(final AbraSiteKnot knot)
  {
    final ArrayList<AbraBaseSite> params = new ArrayList<>();
    for (final AbraBaseSite input : knot.inputs)
    {
      final ArrayList<AbraBaseSite> newInputs = siteTrits.get(input.index);
      params.addAll(newInputs);
    }

    final AbraBlockBranch block = (AbraBlockBranch) knot.block;
    if (!block.analyzed)
    {
      new FpgaConfigurationOptimizer(module, block).run();
    }

    final AbraBlockBranch call = block.clone();

    instantiateSites(call.sites, call, params);
    sites.addAll(call.sites);

    instantiateSites(call.outputs, call, params);

    final ArrayList<AbraBaseSite> trits = new ArrayList<>();
    for (final AbraBaseSite output : call.outputs)
    {
      if (output instanceof AbraSiteMerge)
      {
        final AbraSiteMerge merge = (AbraSiteMerge) output;
        if (merge.inputs.size() == 1)
        {
          final AbraBaseSite input = merge.inputs.get(0);
          input.references--;
          trits.add(input);
          continue;
        }
      }

      throw new CodeException("Invalid output");
    }

    siteTrits.add(trits);
  }

  private AbraSiteKnot processKnotLut(final AbraSiteKnot knot)
  {
    final AbraSiteKnot lut = new AbraSiteKnot();
    lut.size = 1;
    lut.block = knot.block;
    addInputs(knot, lut);

    final ArrayList<AbraBaseSite> trits = new ArrayList<>();
    trits.add(lut);
    siteTrits.add(trits);
    return lut;
  }

  @Override
  protected void processMerge(final AbraSiteMerge merge)
  {
    if (merge.size != 1)
    {
      throw new CodeException("Unexpected merge size");
    }

    final AbraSiteMerge mrg = new AbraSiteMerge();
    mrg.size = 1;
    addInputs(merge, mrg);
    sites.add(mrg);

    final ArrayList<AbraBaseSite> trits = new ArrayList<>();
    trits.add(mrg);
    siteTrits.add(trits);
  }

  private void processOutput()
  {
    final AbraBaseSite site = branch.outputs.get(index);
    if (site.getClass() == AbraSiteMerge.class)
    {
      processOutputMerge((AbraSiteMerge) site);
      return;
    }

    throw new CodeException("Unexpected output site");
  }

  private void processOutputMerge(final AbraSiteMerge merge)
  {
    if (merge.inputs.size() != 1)
    {
      throw new CodeException("Unexpected merge");
    }

    final ArrayList<AbraBaseSite> trits = siteTrits.get(merge.inputs.get(0).index);
    for (final AbraBaseSite trit : trits)
    {
      final AbraSiteMerge out = new AbraSiteMerge();
      out.size = 1;
      out.inputs.add(trit);
      trit.references++;
      outputs.add(out);
    }
  }

  private void processSite()
  {
    final AbraBaseSite site = branch.sites.get(index);
    if (site.getClass() == AbraSiteMerge.class)
    {
      processMerge((AbraSiteMerge) site);
      return;
    }

    if (site.getClass() == AbraSiteKnot.class)
    {
      processKnot((AbraSiteKnot) site);
      return;
    }

    throw new CodeException("Unexpected site");
  }

  @Override
  public void run()
  {
    branch.analyzed = true;

    for (index = 0; index < branch.inputs.size(); index++)
    {
      processInput();
    }

    for (index = 0; index < branch.sites.size(); index++)
    {
      processSite();
    }

    for (index = 0; index < branch.outputs.size(); index++)
    {
      processOutput();
    }

    branch.inputs.clear();
    branch.inputs.addAll(inputs);
    branch.sites.clear();
    branch.sites.addAll(sites);
    branch.outputs.clear();
    branch.outputs.addAll(outputs);
    branch.numberSites();
  }
}
