package org.iota.qupla.abra.context;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockImport;
import org.iota.qupla.abra.block.AbraBlockLut;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteLatch;
import org.iota.qupla.abra.block.site.AbraSiteMerge;
import org.iota.qupla.abra.block.site.AbraSiteParam;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.context.base.AbraTritCodeBaseContext;

public class AbraDebugTritCodeContext extends AbraTritCodeBaseContext
{
  @Override
  public void eval(final AbraModule module)
  {
    module.numberBlocks();

    //TODO add all types to context and write them out
    //     (vector AND struct) name/size/isFloat?
    putInt(0); // version
    putInt(module.luts.size());
    evalBlocks(module.luts);
    putInt(module.branches.size());
    evalBlocks(module.branches);
    putInt(module.imports.size());
    evalBlocks(module.imports);

    //TODO generate tritcode hash so that we can create a function
    //     in the normal tritcode that returns that hash as a constant
  }

  @Override
  public void evalBranch(final AbraBlockBranch branch)
  {
    //TODO origin?
    putString(branch.name);
    evalBranchSites(branch);
  }

  @Override
  public void evalImport(final AbraBlockImport imp)
  {
    //TODO origin?
    putString(imp.name);
  }

  @Override
  public void evalKnot(final AbraSiteKnot knot)
  {
    evalSite(knot);
  }

  @Override
  public void evalLatch(final AbraSiteLatch latch)
  {
    evalSite(latch);
  }

  @Override
  public void evalLut(final AbraBlockLut lut)
  {
    //TODO origin?
    final boolean isUnnamed = lut.name.equals(AbraBlockLut.unnamed(lut.lookup));
    putString(isUnnamed ? null : lut.name);
  }

  @Override
  public void evalMerge(final AbraSiteMerge merge)
  {
    evalSite(merge);
  }

  @Override
  public void evalParam(final AbraSiteParam param)
  {
    evalSite(param);
  }

  private void evalSite(final AbraBaseSite site)
  {
    //TODO origin?
    //TODO stmt?
    //TODO putInt(typeId) (index of origin.typeInfo)
    putString(site.name);
  }
}
