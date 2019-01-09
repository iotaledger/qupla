package org.iota.qupla.abra.funcmanagers;

import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockLut;
import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.funcmanagers.base.BaseFuncManager;
import org.iota.qupla.helper.TritVector;
import org.iota.qupla.qupla.context.QuplaToAbraContext;

public class ConstFuncManager extends BaseFuncManager
{
  private static AbraBlockLut constMin;
  private static AbraBlockLut constOne;
  private static AbraBlockLut constZero;
  private static ConstZeroFuncManager zeroManager = new ConstZeroFuncManager();
  public TritVector trits;
  public TritVector value;

  public ConstFuncManager()
  {
    super("const");
  }

  @Override
  protected void createBaseInstances()
  {
    if (constZero != null)
    {
      // already initialized
      return;
    }

    // make sure zeroManager is initialized
    zeroManager.find(context, 1);

    constZero = zeroManager.lut;

    constOne = context.abraModule.addLut("constOne" + SEPARATOR, "111111111111111111111111111");
    constOne.specialType = AbraBaseBlock.TYPE_CONSTANT;
    constOne.constantValue = new TritVector(1, '1');

    constMin = context.abraModule.addLut("constMin" + SEPARATOR, "---------------------------");
    constMin.specialType = AbraBaseBlock.TYPE_CONSTANT;
    constMin.constantValue = new TritVector(1, '-');
  }

  @Override
  protected void createInstance()
  {
    super.createInstance();

    final AbraBaseSite input = branch.addInputParam(1);

    final AbraSiteKnot siteMin = tritConstant(constMin);
    final AbraSiteKnot siteOne = tritConstant(constOne);
    final AbraSiteKnot siteZero = tritConstant(constZero);

    AbraSiteKnot zeroes = null;
    if (trits.size() < size)
    {
      // need to concatenate rest of zeroes
      zeroes = new AbraSiteKnot();
      zeroes.inputs.add(input);
      zeroes.block = zeroManager.find(context, size);
      zeroes.size = zeroes.block.size();
      branch.sites.add(zeroes);
    }

    final AbraSiteKnot constant = new AbraSiteKnot();
    constant.size = size;
    for (int i = 0; i < trits.size(); i++)
    {
      final char c = trits.trit(i);
      constant.inputs.add(c == '0' ? siteZero : c == '1' ? siteOne : siteMin);
    }

    if (zeroes != null)
    {
      constant.inputs.add(zeroes);
    }

    constant.concat(context);

    branch.specialType = AbraBaseBlock.TYPE_CONSTANT;
    branch.constantValue = value;

    branch.outputs.add(constant);
  }

  public AbraBlockBranch find(final QuplaToAbraContext context, final TritVector value)
  {
    this.context = context;
    this.value = value;
    size = value.size();

    // strip off trailing zeroes
    trits = null;
    for (int i = size - 1; i >= 0; i--)
    {
      if (value.trit(i) != '0')
      {
        trits = value.slice(0, i + 1);
        break;
      }
    }

    if (trits == null || trits.size() == 0)
    {
      // all zeroes, pass it on to zero manager
      return zeroManager.find(context, size);
    }

    name = funcName + SEPARATOR + size + SEPARATOR + trits.trits().replace('-', 'T');
    return findInstance();
  }

  private AbraSiteKnot tritConstant(final AbraBlockLut tritLut)
  {
    final AbraBaseSite input = branch.inputs.get(0);

    final AbraSiteKnot site = new AbraSiteKnot();
    site.name = tritLut.name;
    site.inputs.add(input);
    site.inputs.add(input);
    site.inputs.add(input);
    site.block = tritLut;
    site.size = site.block.size();
    branch.sites.add(site);
    return site;
  }
}
