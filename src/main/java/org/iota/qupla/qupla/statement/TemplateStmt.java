package org.iota.qupla.qupla.statement;

import java.util.ArrayList;

import org.iota.qupla.qupla.expression.NameExpr;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;

public class TemplateStmt extends BaseExpr
{
  public boolean analyzed;
  public ArrayList<BaseExpr> funcs = new ArrayList<>();
  public final ArrayList<BaseExpr> params = new ArrayList<>();
  public final ArrayList<BaseExpr> types = new ArrayList<>();

  public TemplateStmt(final TemplateStmt copy)
  {
    super(copy);

    params.addAll(copy.params);
    cloneArray(types, copy.types);
    cloneArray(funcs, copy.funcs);
    this.analyzed = copy.analyzed;
  }

  public TemplateStmt(final Tokenizer tokenizer)
  {
    super(tokenizer);

    tokenizer.nextToken();

    final Token templateName = expect(tokenizer, Token.TOK_NAME, "template name");
    name = templateName.text;
    module.checkDuplicateName(module.templates, this);

    expect(tokenizer, Token.TOK_TEMPL_OPEN, "<");

    params.add(new NameExpr(tokenizer, "placeholder type name"));
    while (tokenizer.tokenId() == Token.TOK_COMMA)
    {
      tokenizer.nextToken();

      params.add(new NameExpr(tokenizer, "placeholder type name"));
    }

    expect(tokenizer, Token.TOK_TEMPL_CLOSE, "',' or '>'");

    expect(tokenizer, Token.TOK_GROUP_OPEN, "'{'");

    while (tokenizer.tokenId() == Token.TOK_TYPE)
    {
      types.add(new TypeStmt(tokenizer));
    }

    while (tokenizer.tokenId() == Token.TOK_FUNC)
    {
      funcs.add(new FuncStmt(tokenizer));
    }

    expect(tokenizer, Token.TOK_GROUP_CLOSE, "'}'");
  }

  @Override
  public void analyze()
  {
    // just to avoid findEntity calling analyze again
    size = params.size();
    analyzed = true;
  }

  @Override
  public BaseExpr clone()
  {
    return new TemplateStmt(this);
  }

  @Override
  public void toStringify()
  {
    printer.evalTemplateSignature(this);

    if (funcs.size() == 1)
    {
      FuncStmt func = (FuncStmt) funcs.get(0);
      printer.evalFuncBodySignature(func);
      return;
    }

    final boolean first = true;
    for (final BaseExpr func : funcs)
    {
      printer.append(first ? "{ " : ", ").append(func.name);
    }

    printer.append(" }");
  }
}
