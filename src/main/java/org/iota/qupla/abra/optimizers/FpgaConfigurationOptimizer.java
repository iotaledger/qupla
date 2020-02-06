package org.iota.qupla.abra.optimizers;

import java.util.ArrayList;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockLut;
import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteParam;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.optimizers.base.BaseOptimizer;

// first slice all input vectors into trit-sized slices
// replace all site inputs with those trits
// replace all call knots with the contents of the branch they call
// replace their inputs directly with the actual input parameters
// replace all site inputs with the branch output trits
//TODO how to deal with latches?

public class FpgaConfigurationOptimizer extends BaseOptimizer
{
  private final ArrayList<AbraSiteParam> inputs = new ArrayList<>();
  private final ArrayList<AbraBaseSite> outputs = new ArrayList<>();
  private final ArrayList<ArrayList<AbraBaseSite>> siteTrits = new ArrayList<>();
  private final ArrayList<AbraSiteKnot> sites = new ArrayList<>();

  public FpgaConfigurationOptimizer(final AbraModule module, final AbraBlockBranch branch)
  {
    super(module, branch);
  }

  private void addInputs(final AbraSiteKnot from, final AbraSiteKnot to)
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

  private void processInput(final AbraSiteParam input)
  {
    final ArrayList<AbraBaseSite> trits = new ArrayList<>(branch.inputs.size());
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

      inputs.add(param);
      trits.add(param);
    }

    siteTrits.add(trits);
  }

  @Override
  protected void processKnot(final AbraSiteKnot knot)
  {
    switch (knot.block.specialType)
    {
    case AbraBaseBlock.TYPE_MERGE:
      processKnotMerge(knot);
      return;

    case AbraBaseBlock.TYPE_NULLIFY_FALSE:
    case AbraBaseBlock.TYPE_NULLIFY_TRUE:
      processKnotNullify(knot);
      return;

    case AbraBaseBlock.TYPE_CONSTANT:
      processKnotConstant(knot);
      return;

    case AbraBaseBlock.TYPE_SLICE:
      processKnotSlice(knot);
      return;
    }

    if (knot.block instanceof AbraBlockLut)
    {
      processKnotLut(knot);
      return;
    }

    if (knot.block instanceof AbraBlockBranch)
    {
      processKnotBranch(knot);
      return;
    }

    error("Unknown block");
  }

  private void processKnotBranch(final AbraSiteKnot knot)
  {
    final AbraBlockBranch block = (AbraBlockBranch) knot.block;
    if (!block.analyzed)
    {
      new FpgaConfigurationOptimizer(module, block).run();
    }

    final ArrayList<AbraBaseSite> params = new ArrayList<>();
    for (final AbraBaseSite input : knot.inputs)
    {
      final ArrayList<AbraBaseSite> trits = siteTrits.get(input.index);
      params.addAll(trits);
    }

    final AbraBlockBranch call = block.clone();

    for (final AbraBaseSite callSite : call.sites)
    {
      final AbraSiteKnot site = (AbraSiteKnot) callSite;
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

    sites.addAll(call.sites);

    final ArrayList<AbraBaseSite> trits = new ArrayList<>();
    for (final AbraBaseSite output : call.outputs)
    {
      output.references--;
      if (output.index >= call.inputs.size())
      {
        trits.add(output);
        continue;
      }

      final AbraBaseSite param = params.get(output.index);
      trits.add(param);
    }

    siteTrits.add(trits);
  }

  private void processKnotConstant(final AbraSiteKnot knot)
  {
    final AbraBaseSite input = knot.inputs.get(0);
    final ArrayList<AbraBaseSite> inputTrits = siteTrits.get(input.index);
    final AbraBaseSite inputTrit = inputTrits.get(0);
    final ArrayList<AbraBaseSite> trits = new ArrayList<>();
    final String vector = knot.block.constantValue.trits();
    for (int i = 0; i < vector.length(); i++)
    {
      int lutId = AbraBaseBlock.TYPE_CONSTANT + "@01-".indexOf(vector.charAt(i));
      final AbraSiteKnot lut = new AbraSiteKnot();
      lut.size = 1;
      lut.block = module.luts.get(lutId);
      lut.inputs.add(inputTrit);
      inputTrit.references++;
      trits.add(lut);
      sites.add(lut);
    }

    siteTrits.add(trits);
  }

  private void processKnotLut(final AbraSiteKnot knot)
  {
    final AbraSiteKnot lut = new AbraSiteKnot();
    lut.size = 1;
    lut.block = knot.block;
    lut.name = knot.name;
    addInputs(knot, lut);

    final ArrayList<AbraBaseSite> trits = new ArrayList<>();
    trits.add(lut);
    sites.add(lut);
    siteTrits.add(trits);
  }

  private void processKnotMerge(final AbraSiteKnot knot)
  {
    final AbraBaseSite input1 = knot.inputs.get(0);
    final AbraBaseSite input2 = knot.inputs.get(1);
    final ArrayList<AbraBaseSite> trits1 = siteTrits.get(input1.index);
    final ArrayList<AbraBaseSite> trits2 = siteTrits.get(input2.index);
    final ArrayList<AbraBaseSite> trits = new ArrayList<>();
    for (int i = 0; i < trits1.size(); i++)
    {
      final AbraBaseSite trit1 = trits1.get(i);
      final AbraBaseSite trit2 = trits2.get(i);
      final AbraSiteKnot lut = new AbraSiteKnot();
      lut.size = 1;
      lut.block = knot.block;
      lut.inputs.add(trit1);
      trit1.references++;
      lut.inputs.add(trit2);
      trit2.references++;

      trits.add(lut);
      sites.add(lut);
    }

    siteTrits.add(trits);
  }

  private void processKnotNullify(final AbraSiteKnot knot)
  {
    final AbraBaseSite flag = knot.inputs.get(0);
    final AbraBaseSite value = knot.inputs.get(1);
    final ArrayList<AbraBaseSite> flagTrits = siteTrits.get(flag.index);
    final ArrayList<AbraBaseSite> valueTrits = siteTrits.get(value.index);
    final AbraBaseSite flagTrit = flagTrits.get(0);
    final ArrayList<AbraBaseSite> trits = new ArrayList<>();
    for (int i = 0; i < valueTrits.size(); i++)
    {
      final AbraBaseSite valueTrit = valueTrits.get(i);
      final AbraSiteKnot lut = new AbraSiteKnot();
      lut.size = 1;
      lut.block = knot.block;
      lut.inputs.add(flagTrit);
      flagTrit.references++;
      lut.inputs.add(valueTrit);
      valueTrit.references++;

      trits.add(lut);
      sites.add(lut);
    }
    siteTrits.add(trits);
  }

  private void processKnotSlice(final AbraSiteKnot knot)
  {
    final ArrayList<AbraBaseSite> params = new ArrayList<>();
    for (final AbraBaseSite input : knot.inputs)
    {
      final ArrayList<AbraBaseSite> trits = siteTrits.get(input.index);
      params.addAll(trits);
    }

    final ArrayList<AbraBaseSite> trits = new ArrayList<>();
    final AbraBlockBranch block = (AbraBlockBranch) knot.block;
    for (int i = 0; i < block.size; i++)
    {
      final AbraBaseSite param = params.get(block.offset + i);
      trits.add(param);
    }

    siteTrits.add(trits);
  }

  @Override
  public void run()
  {
    branch.analyzed = true;

    branch.numberSites();

    for (index = 0; index < branch.inputs.size(); index++)
    {
      processInput(branch.inputs.get(index));
    }

    for (index = 0; index < branch.sites.size(); index++)
    {
      final AbraSiteKnot knot = branch.sites.get(index);
      processKnot(knot);
      if (knot.stmt != null)
      {
        //TODO this will sometimes overwrite
        siteTrits.get(knot.index).get(0).stmt = knot.stmt;
      }
    }

    for (index = 0; index < branch.outputs.size(); index++)
    {
      final AbraBaseSite output = branch.outputs.get(index);
      final ArrayList<AbraBaseSite> trits = siteTrits.get(output.index);
      for (final AbraBaseSite trit : trits)
      {
        outputs.add(trit);
        trit.references++;
      }
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
