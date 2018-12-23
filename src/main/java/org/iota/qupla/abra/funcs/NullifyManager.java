package org.iota.qupla.abra.funcs;

import java.util.ArrayList;

import org.iota.qupla.abra.AbraBlockBranch;
import org.iota.qupla.abra.AbraBlockLut;
import org.iota.qupla.abra.AbraSiteKnot;
import org.iota.qupla.abra.AbraSiteParam;

public class NullifyManager extends AbraFuncManager
{
  public AbraBlockLut lutNullify;
  public boolean trueFalse;

  public NullifyManager(final boolean trueFalse)
  {
    super(trueFalse ? "nullifyTrue" : "nullifyFalse");
    this.trueFalse = trueFalse;
  }

  @Override
  protected void createBaseInstances()
  {
    generateLuts();

    // for 1, 2, and 3 trit sizes functions directly use LUT
    for (int i = 1; i <= 3; i++)
    {
      generateLutFunc(i);
    }

    // for higher powers of 3 and their double sizes
    // functions are composed of smaller functions
    for (int i = 3; i <= 2187; i *= 3)
    {
      saveBranch(generateFuncFunc(i * 2, new Integer[] {
          i,
          i
      }));
      saveBranch(generateFuncFunc(i * 3, new Integer[] {
          i,
          i,
          i
      }));
    }
  }

  @Override
  protected void createInstance()
  {
    // determine factors that make up this one as integer array
    int exist = size / 2;
    if (exist * 2 == size && sorted.contains(exist))
    {
      branch = generateFuncFunc(size, new Integer[] {
          exist,
          exist
      });
      return;
    }

    exist = size / 3;
    if (exist * 3 == size && sorted.contains(exist))
    {
      branch = generateFuncFunc(size, new Integer[] {
          exist,
          exist,
          exist
      });
      return;
    }

    int highest = sorted.size() - 1;
    int remain = size;
    final ArrayList<Integer> factors = new ArrayList<>();
    while (remain > 0)
    {
      int value = sorted.get(highest);
      while (value > remain)
      {
        highest--;
        value = sorted.get(highest);
      }
      remain -= value;
      factors.add(value);
    }

    branch = generateFuncFunc(size, factors.toArray(new Integer[0]));
  }

  private AbraBlockBranch generateFuncFunc(final int inputSize, final Integer[] inputSizes)
  {
    // nullify functions that use smaller functions
    final NullifyManager nullifyManager = new NullifyManager(trueFalse);
    nullifyManager.instances = instances;
    nullifyManager.sorted = sorted;

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
      knot.block = nullifyManager.find(context, inputSizes[i]);
      branch.outputs.add(knot);
    }

    return branch;
  }

  private void generateLutFunc(final int inputSize)
  {
    // nullify functions that use LUT
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
      knot.block = lutNullify;
      branch.outputs.add(knot);
    }

    saveBranch(branch);
  }

  private void generateLuts()
  {
    final String trueTrits = "@@-@@0@@1@@-@@0@@1@@-@@0@@1";
    final String falseTrits = "-@@0@@1@@-@@0@@1@@-@@0@@1@@";
    lutNullify = new AbraBlockLut();
    lutNullify.name = "$" + funcName + "$";
    lutNullify.tritCode.putTrits(trueFalse ? trueTrits : falseTrits);
    context.abra.luts.add(lutNullify);
  }
}
