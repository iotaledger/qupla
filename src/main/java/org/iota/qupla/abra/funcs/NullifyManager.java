package org.iota.qupla.abra.funcs;

import org.iota.qupla.abra.AbraBlockBranch;
import org.iota.qupla.abra.AbraSiteKnot;
import org.iota.qupla.abra.AbraSiteParam;

public class NullifyManager extends AbraFuncManager
{
  public boolean trueFalse;

  public NullifyManager(final boolean trueFalse)
  {
    super(trueFalse ? "nullifyTrue" : "nullifyFalse");
    this.trueFalse = trueFalse;
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
    final NullifyManager manager = new NullifyManager(trueFalse);
    manager.instances = instances;
    manager.sorted = sorted;

    final AbraBlockBranch branch = new AbraBlockBranch();
    branch.name = "$" + funcName + "$" + inputSize;
    branch.size = inputSize;

    final AbraSiteParam inputFlag = branch.addInputParam(1);
    for (int i = 0; i < inputSizes.length; i++)
    {
      final AbraSiteParam inputValue = branch.addInputParam(inputSizes[i]);
      final AbraSiteKnot knot = new AbraSiteKnot();
      knot.size = 1;
      knot.inputs.add(inputFlag);
      knot.inputs.add(inputValue);
      knot.block = manager.find(context, inputSizes[i]);
      branch.outputs.add(knot);
    }

    return branch;
  }

  @Override
  protected void generateLut()
  {
    final String trueTrits = "@@-@@0@@1@@-@@0@@1@@-@@0@@1";
    final String falseTrits = "-@@0@@1@@-@@0@@1@@-@@0@@1@@";
    lut = context.abra.addLut("$" + funcName + "$", trueFalse ? trueTrits : falseTrits);
  }

  @Override
  protected void generateLutFunc(final int inputSize)
  {
    // generate function that use LUTs
    final AbraBlockBranch branch = new AbraBlockBranch();
    branch.name = "$" + funcName + "$" + inputSize;
    branch.size = inputSize;

    final AbraSiteParam inputFlag = branch.addInputParam(1);
    for (int i = 0; i < inputSize; i++)
    {
      final AbraSiteParam inputValue = branch.addInputParam(1);
      final AbraSiteKnot knot = new AbraSiteKnot();
      knot.size = 1;
      knot.inputs.add(inputFlag);
      knot.inputs.add(inputValue);
      knot.inputs.add(inputValue);
      knot.block = lut;
      branch.outputs.add(knot);
    }

    saveBranch(branch);
  }
}
