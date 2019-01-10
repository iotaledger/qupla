package org.iota.qupla.abra.funcmanagers.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockLut;
import org.iota.qupla.qupla.context.QuplaToAbraContext;

public class BaseFuncManager
{
  protected static final String SEPARATOR = "_";
  public AbraBlockBranch branch;
  public QuplaToAbraContext context;
  public String funcName;
  public HashMap<String, AbraBlockBranch> instances = new HashMap<>();
  public AbraBlockLut lut;
  public String name;
  private int reuse;
  public int size;
  public ArrayList<Integer> sorted = new ArrayList<>();

  public BaseFuncManager(final String funcName)
  {
    this.funcName = funcName;
  }

  protected void createBaseInstances()
  {
  }

  protected void createBestFuncFunc()
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

  protected void createInstance()
  {
    branch = new AbraBlockBranch();
    branch.name = name;
    branch.size = size;
  }

  protected void createStandardBaseInstances()
  {
    generateLut();

    if (lut == null)
    {
      // no lut generated? then don't bother
      return;
    }

    // for 1, 2, and 3 trit sizes functions directly use LUT
    for (int i = 1; i <= 3; i++)
    {
      generateLutFunc(i);
    }

    // for higher powers of 3 and their double sizes
    // functions are composed of smaller functions
    for (int power = 3; power < 81; power *= 3)
    {
      saveBranch(generateFuncFunc(power * 2, new Integer[] {
          power,
          power
      }));
      saveBranch(generateFuncFunc(power * 3, new Integer[] {
          power,
          power,
          power
      }));
    }
  }

  public AbraBlockBranch find(final QuplaToAbraContext context, final int size)
  {
    this.context = context;
    this.size = size;
    name = funcName + SEPARATOR + size;
    return findInstance();
  }

  protected AbraBlockBranch findInstance()
  {
    final AbraBlockBranch instance = instances.get(name);
    if (instance != null)
    {
      reuse++;
      return instance;
    }

    if (instances.size() == 0)
    {
      createBaseInstances();
      if (instances.size() != 0)
      {
        // retry find now that we're initialized
        return findInstance();
      }
    }

    createInstance();
    return saveBranch(branch);
  }

  protected AbraBlockBranch generateFuncFunc(final int inputSize, final Integer[] inputSizes)
  {
    // generate function that use smaller functions
    return null;
  }

  protected void generateLut()
  {
  }

  protected void generateLutFunc(final int inputSize)
  {
    // generate function that use LUTs
  }

  protected AbraBlockBranch saveBranch(final AbraBlockBranch branch)
  {
    instances.put(branch.name, branch);
    sorted.add(branch.size);
    Collections.sort(sorted);
    context.abraModule.addBranch(branch);
    return branch;
  }
}
