package org.iota.qupla.abra.funcs;

import org.iota.qupla.abra.AbraBlockBranch;
import org.iota.qupla.abra.AbraBlockLut;
import org.iota.qupla.abra.AbraSiteKnot;
import org.iota.qupla.abra.AbraSiteParam;

public class ConstZeroManager extends AbraFuncManager
{
  public ConstZeroManager()
  {
    super("constZero");
  }

  @Override
  protected void createBaseInstances()
  {
    createStandardBaseInstances();
  }

  @Override
  protected void createInstance()
  {
    createBestFuncFunc();
  }

  @Override
  protected AbraBlockBranch generateFuncFunc(final int inputSize, final Integer[] inputSizes)
  {
    // generate function that use smaller functions
    final ConstZeroManager manager = new ConstZeroManager();
    manager.instances = instances;
    manager.sorted = sorted;

    final AbraBlockBranch branch = new AbraBlockBranch();
    branch.name = "$" + funcName + "$" + inputSize;
    branch.size = inputSize;

    final AbraSiteParam inputValue = branch.addInputParam(1);
    for (int i = 0; i < inputSizes.length; i++)
    {
      final AbraSiteKnot knot = new AbraSiteKnot();
      knot.size = 1;
      knot.inputs.add(inputValue);
      knot.block = manager.find(context, inputSizes[i]);
      branch.outputs.add(knot);
    }

    return branch;
  }

  @Override
  protected void generateLut()
  {
    lut = new AbraBlockLut();
    lut.name = "$" + funcName + "$";
    lut.tritCode.putTrits("000000000000000000000000000");
    context.abra.luts.add(lut);
  }

  protected void generateLutFunc(final int inputSize)
  {
    // generate function that use LUTs
    final AbraBlockBranch branch = new AbraBlockBranch();
    branch.name = "$" + funcName + "$" + inputSize;
    branch.size = inputSize;

    final AbraSiteParam inputValue = branch.addInputParam(1);
    for (int i = 0; i < inputSize; i++)
    {
      final AbraSiteKnot knot = new AbraSiteKnot();
      knot.size = 1;
      knot.inputs.add(inputValue);
      knot.inputs.add(inputValue);
      knot.inputs.add(inputValue);
      knot.block = lut;
      branch.outputs.add(knot);
    }

    saveBranch(branch);
  }
}
