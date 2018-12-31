package org.iota.qupla.abra.funcs;

import org.iota.qupla.abra.AbraBlock;
import org.iota.qupla.abra.AbraBlockBranch;
import org.iota.qupla.abra.AbraSiteKnot;
import org.iota.qupla.abra.AbraSiteParam;
import org.iota.qupla.helper.TritVector;

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
    branch.name = funcName + "_" + inputSize;
    branch.size = inputSize;

    final AbraSiteParam inputFlag = branch.addInputParam(1);
    for (int i = 0; i < inputSizes.length; i++)
    {
      final AbraSiteParam inputValue = branch.addInputParam(inputSizes[i]);
      final AbraSiteKnot knot = new AbraSiteKnot();
      knot.inputs.add(inputFlag);
      knot.inputs.add(inputValue);
      knot.block = manager.find(context, inputSizes[i]);
      knot.size = knot.block.size();
      branch.outputs.add(knot);
    }

    branch.type = trueFalse ? AbraBlock.TYPE_NULLIFY_TRUE : AbraBlock.TYPE_NULLIFY_FALSE;
    branch.constantValue = new TritVector(size, '@');
    return branch;
  }

  @Override
  protected void generateLut()
  {
    final String trueTrits = "@@-@@0@@1@@-@@0@@1@@-@@0@@1";
    final String falseTrits = "-@@0@@1@@-@@0@@1@@-@@0@@1@@";
    lut = context.abra.addLut(funcName + "_", trueFalse ? trueTrits : falseTrits);
    lut.type = trueFalse ? AbraBlock.TYPE_NULLIFY_TRUE : AbraBlock.TYPE_NULLIFY_FALSE;
    lut.constantValue = new TritVector(1, '@');
  }

  @Override
  protected void generateLutFunc(final int inputSize)
  {
    // generate function that use LUTs
    final AbraBlockBranch branch = new AbraBlockBranch();
    branch.name = funcName + "_" + inputSize;
    branch.size = inputSize;

    final AbraSiteParam inputFlag = branch.addInputParam(1);
    for (int i = 0; i < inputSize; i++)
    {
      final AbraSiteParam inputValue = branch.addInputParam(1);
      final AbraSiteKnot knot = new AbraSiteKnot();
      knot.inputs.add(inputFlag);
      knot.inputs.add(inputValue);
      knot.inputs.add(inputValue);
      knot.block = lut;
      knot.size = knot.block.size();
      branch.outputs.add(knot);
    }

    branch.type = trueFalse ? AbraBlock.TYPE_NULLIFY_TRUE : AbraBlock.TYPE_NULLIFY_FALSE;
    branch.constantValue = new TritVector(size, '@');
    saveBranch(branch);
  }
}
