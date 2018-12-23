package org.iota.qupla.abra.funcs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.iota.qupla.abra.AbraBlockBranch;
import org.iota.qupla.context.AbraContext;

public class AbraFuncManager
{
  public AbraBlockBranch branch;
  public AbraContext context;
  public String funcName;
  public HashMap<String, AbraBlockBranch> instances = new HashMap<>();
  public String name;
  private int reuse;
  public int size;
  public ArrayList<Integer> sorted = new ArrayList<>();

  public AbraFuncManager(final String funcName)
  {
    this.funcName = funcName;
  }

  protected void createBaseInstances()
  {
  }

  protected void createInstance()
  {
    branch = new AbraBlockBranch();
    branch.name = name;
    branch.size = size;
  }

  public AbraBlockBranch find(final AbraContext context, final int size)
  {
    this.context = context;
    this.size = size;
    name = "$" + funcName + "$" + size;
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
        return findInstance();
      }
    }

    createInstance();
    return saveBranch(branch);
  }

  protected AbraBlockBranch saveBranch(final AbraBlockBranch branch)
  {
    instances.put(branch.name, branch);
    sorted.add(branch.size);
    Collections.sort(sorted);
    context.abra.branches.add(branch);
    return branch;
  }
}
