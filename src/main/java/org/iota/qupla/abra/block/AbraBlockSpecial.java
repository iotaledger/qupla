package org.iota.qupla.abra.block;


import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.context.base.AbraBaseContext;
import org.iota.qupla.helper.TritVector;

public class AbraBlockSpecial extends AbraBaseBlock
{
  public static final int TYPE_CONCAT = 2;
  public static final int TYPE_CONST = 3;
  public static final int TYPE_MERGE = 0;
  public static final int TYPE_NULLIFY_FALSE = 5;
  public static final int TYPE_NULLIFY_TRUE = 4;
  public static final int TYPE_SLICE = 1;
  public static final String[] names = {
      "merge",
      "slice",
      "concat",
      "const",
      "nullifyTrue",
      "nullifyFalse"
  };

  public TritVector constantValue;
  public int offset;
  public int size;

  public AbraBlockSpecial(final int type)
  {
    index = type;
    name = names[type];
  }

  public AbraBlockSpecial(final int type, final int size)
  {
    this(type);

    this.size = size;
    name += "_" + size;
  }

  public AbraBlockSpecial(final int type, final int size, final int offset)
  {
    this(type, size);

    this.offset = offset;
    name += "_" + offset;
  }

  public AbraBlockSpecial(final int type, final int size, final TritVector vector)
  {
    this(type, size);

    if (vector.isZero())
    {
      name = "constZero_" + size;
    }
    else
    {
      final String trits = vector.trits();
      int len = trits.length();
      while (trits.charAt(len - 1) == '0')
      {
        len--;
      }

      name += "_" + trits.substring(0, len).replace('-', 'T');
    }

    constantValue = vector;
  }

  @Override
  public void eval(final AbraBaseContext context)
  {
    context.evalSpecial(this);
  }

  @Override
  public String toString()
  {
    return name + "()";
  }
}
