package org.iota.qupla.qupla.expression.constant;

import org.iota.qupla.helper.TritConverter;
import org.iota.qupla.qupla.context.QuplaEvalContext;
import org.iota.qupla.qupla.expression.FuncExpr;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;

public class ConstFuncExpr extends FuncExpr
{
  private ConstFuncExpr(final ConstFuncExpr copy)
  {
    super(copy);
  }

  public ConstFuncExpr(final Tokenizer tokenizer, final Token identifier)
  {
    super(tokenizer, identifier);
  }

  @Override
  public void analyze()
  {
    super.analyze();

    if (!wasAnalyzed())
    {
      return;
    }

    final QuplaEvalContext context = new QuplaEvalContext();
    eval(context);
    size = TritConverter.toInt(context.value.trits());
    typeInfo = null;
  }

  @Override
  public BaseExpr clone()
  {
    return new ConstFuncExpr(this);
  }
}
