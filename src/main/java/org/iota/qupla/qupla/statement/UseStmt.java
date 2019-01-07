package org.iota.qupla.qupla.statement;

import java.util.ArrayList;

import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.expression.constant.ConstTypeName;
import org.iota.qupla.qupla.parser.QuplaModule;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;

public class UseStmt extends BaseExpr
{
  public ArrayList<FuncStmt> placeHolders = new ArrayList<>();
  public TemplateStmt template;
  public final ArrayList<ArrayList<BaseExpr>> typeInstantiations = new ArrayList<>();

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

    parseTypeInstantiation(tokenizer);

    while (tokenizer.tokenId() == Token.TOK_COMMA)
    {
      tokenizer.nextToken();
      parseTypeInstantiation(tokenizer);
    }
  }

  @Override
  public void analyze()
  {
    for (final ArrayList<BaseExpr> typeArgs : typeInstantiations)
    {
      for (final BaseExpr typeArg : typeArgs)
      {
        typeArg.analyze();
      }
    }

    template = (TemplateStmt) findEntity(TemplateStmt.class, "template");
    for (int i = 0; i < template.funcs.size() * typeInstantiations.size(); i++)
    {
      placeHolders.add(new FuncStmt(this));
    }

    module.funcs.addAll(placeHolders);

    final QuplaModule oldCurrentModule = currentModule;
    currentModule = module;
    currentUse = this;
    currentUseIndex = 0;
    int placeHolderIndex = 0;
    for (final ArrayList<BaseExpr> typeArgs : typeInstantiations)
    {
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

      generateTypes();

      for (final BaseExpr func : template.funcs)
      {
        final FuncStmt useFunc = new FuncStmt((FuncStmt) func);
        useFunc.origin = origin;
        useFunc.module = module;
        useFunc.use = currentUse;
        useFunc.useIndex = currentUseIndex;
        useFunc.analyzeSignature();
        //TODO WTF?
        placeHolders.get(placeHolderIndex).copyFrom(useFunc);
        placeHolderIndex++;
      }

      currentUseIndex++;
    }

    currentModule = oldCurrentModule;
    currentUse = null;
    currentUseIndex = 0;
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
      type.typeInfo = null;
      type.typeInfo = (TypeStmt) type.clone();
      type.typeInfo.analyze();

      type.typeInfo.name = template.name;
      final ArrayList<BaseExpr> typeArgs = typeInstantiations.get(currentUseIndex);
      for (final BaseExpr typeArg : typeArgs)
      {
        type.typeInfo.name += SEPARATOR + typeArg.name;
      }

      type.typeInfo.name += SEPARATOR + type.name;
      module.types.add(type.typeInfo);
    }
  }

  private void parseTypeInstantiation(final Tokenizer tokenizer)
  {
    final ArrayList<BaseExpr> typeArgs = new ArrayList<>();
    expect(tokenizer, Token.TOK_TEMPL_OPEN, "'<'");

    typeArgs.add(new ConstTypeName(tokenizer));

    while (tokenizer.tokenId() == Token.TOK_COMMA)
    {
      tokenizer.nextToken();

      typeArgs.add(new ConstTypeName(tokenizer));
    }

    expect(tokenizer, Token.TOK_TEMPL_CLOSE, "',' or '>'");

    typeInstantiations.add(typeArgs);
  }
}
