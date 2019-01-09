package org.iota.qupla.qupla.statement;

import java.util.ArrayList;

import org.iota.qupla.qupla.expression.FuncExpr;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.expression.constant.ConstTypeName;
import org.iota.qupla.qupla.parser.QuplaModule;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;

public class UseStmt extends BaseExpr
{
  public TemplateStmt template;
  public final ArrayList<BaseExpr> typeArgs = new ArrayList<>();
  public final ArrayList<BaseExpr> types = new ArrayList<>();

  public UseStmt(final UseStmt copy)
  {
    super(copy);

    error("Cannot clone UseStmt");
  }

  public UseStmt(final Tokenizer tokenizer)
  {
    super(tokenizer);

    expect(tokenizer, Token.TOK_USE, "use");

    final Token templateName = expect(tokenizer, Token.TOK_NAME, "template name");
    name = templateName.text;

    expect(tokenizer, Token.TOK_TEMPL_OPEN, "'<'");

    typeArgs.add(new ConstTypeName(tokenizer));

    while (tokenizer.tokenId() == Token.TOK_COMMA)
    {
      tokenizer.nextToken();

      typeArgs.add(new ConstTypeName(tokenizer));
    }

    expect(tokenizer, Token.TOK_TEMPL_CLOSE, "',' or '>'");
  }

  public UseStmt(final TemplateStmt template, final FuncExpr func)
  {
    module = func.module;
    origin = func.origin;
    name = template.name;

    for (final BaseExpr funcType : func.funcTypes)
    {
      final BaseExpr constType = funcType.clone();
      typeArgs.add(constType);
    }
  }

  @Override
  public void analyze()
  {
    template = (TemplateStmt) findEntity(TemplateStmt.class, "template");

    final QuplaModule oldModule = currentModule;
    currentModule = module;

    final UseStmt oldUse = currentUse;
    currentUse = this;

    if (template.params.size() > typeArgs.size())
    {
      final BaseExpr param = template.params.get(typeArgs.size());
      error("Missing type argument: " + param.name);
    }

    if (template.params.size() < typeArgs.size())
    {
      final BaseExpr typeArg = typeArgs.get(template.params.size());
      typeArg.error("Extra type argument: " + typeArg.name);
    }

    for (final BaseExpr typeArg : typeArgs)
    {
      if (!typeArg.analyzed())
      {
        typeArg.analyze();
      }
    }

    generateTypes();

    for (final BaseExpr func : template.funcs)
    {
      final FuncStmt useFunc = new FuncStmt((FuncStmt) func);
      useFunc.origin = origin;
      useFunc.module = module;
      useFunc.use = this;
      useFunc.analyzeSignature();
      module.funcs.add(useFunc);
    }

    currentModule = oldModule;
    currentUse = oldUse;
  }

  @Override
  public BaseExpr clone()
  {
    return new UseStmt(this);
  }

  public void generateTypes()
  {
    // set up template types
    for (final BaseExpr type : template.types)
    {
      final TypeStmt useType = (TypeStmt) type.clone();
      useType.analyze();
      types.add(useType);
    }

    //TODO check relationship expression

  }
}
