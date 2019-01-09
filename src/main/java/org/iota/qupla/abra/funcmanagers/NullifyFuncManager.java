package org.iota.qupla.abra.funcmanagers;

import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteParam;
import org.iota.qupla.abra.funcmanagers.base.BaseFuncManager;
import org.iota.qupla.helper.TritVector;

public class NullifyFuncManager extends BaseFuncManager
{
  public boolean trueFalse;

  public NullifyFuncManager(final boolean trueFalse)
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
    final NullifyFuncManager manager = new NullifyFuncManager(trueFalse);
    manager.instances = instances;
    manager.sorted = sorted;

    final AbraBlockBranch branch = new AbraBlockBranch();
    branch.name = funcName + SEPARATOR + inputSize;
    branch.size = inputSize;

    final AbraSiteParam inputFlag = branch.addInputParam(1);
    final AbraSiteParam inputValue = branch.addInputParam(inputSize);
    int offset = 0;
    for (int i = 0; i < inputSizes.length; i++)
    {
      final AbraSiteKnot slice = new AbraSiteKnot();
      slice.inputs.add(inputValue);
      inputValue.references++;
      slice.block = AbraSiteKnot.slicers.find(context, inputSizes[i], offset);
      slice.size = slice.block.size();
      branch.sites.add(slice);

      final AbraSiteKnot nullify = new AbraSiteKnot();
      nullify.inputs.add(inputFlag);
      inputFlag.references++;
      nullify.inputs.add(slice);
      slice.references++;
      nullify.block = manager.find(context, inputSizes[i]);
      nullify.size = nullify.block.size();
      branch.outputs.add(nullify);

      offset += inputSizes[i];
    }

    branch.specialType = trueFalse ? AbraBaseBlock.TYPE_NULLIFY_TRUE : AbraBaseBlock.TYPE_NULLIFY_FALSE;
    branch.constantValue = new TritVector(size, '@');
    return branch;
  }

  @Override
  protected void generateLut()
  {
    final String trueTrits = "@@-@@0@@1@@-@@0@@1@@-@@0@@1";
    final String falseTrits = "-@@0@@1@@-@@0@@1@@-@@0@@1@@";
    lut = context.abraModule.addLut(funcName + SEPARATOR, trueFalse ? trueTrits : falseTrits);
    lut.specialType = trueFalse ? AbraBaseBlock.TYPE_NULLIFY_TRUE : AbraBaseBlock.TYPE_NULLIFY_FALSE;
    lut.constantValue = new TritVector(1, '@');
  }

  @Override
  protected void generateLutFunc(final int inputSize)
  {
    // generate function that use LUTs
    final AbraBlockBranch branch = new AbraBlockBranch();
    branch.name = funcName + SEPARATOR + inputSize;
    branch.size = inputSize;

    final AbraSiteParam inputFlag = branch.addInputParam(1);
    final AbraSiteParam inputValue = branch.addInputParam(inputSize);
    for (int i = 0; i < inputSize; i++)
    {
      final AbraSiteKnot slice = new AbraSiteKnot();
      slice.inputs.add(inputValue);
      inputValue.references++;
      slice.block = AbraSiteKnot.slicers.find(context, 1, i);
      slice.size = slice.block.size();
      branch.sites.add(slice);

      final AbraSiteKnot knot = new AbraSiteKnot();
      knot.inputs.add(inputFlag);
      inputFlag.references++;
      knot.inputs.add(slice);
      slice.references++;
      knot.inputs.add(slice);
      slice.references++;
      knot.block = lut;
      knot.size = knot.block.size();
      branch.outputs.add(knot);
    }

    branch.specialType = trueFalse ? AbraBaseBlock.TYPE_NULLIFY_TRUE : AbraBaseBlock.TYPE_NULLIFY_FALSE;
    branch.constantValue = new TritVector(size, '@');
    saveBranch(branch);
  }
}
