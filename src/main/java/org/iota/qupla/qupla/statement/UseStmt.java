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
  public boolean automatic;
  public String funcName;
  public UseStmt nextUse;
  public TemplateStmt template;
  public final ArrayList<BaseExpr> typeArgs = new ArrayList<>();
  public final ArrayList<BaseExpr> types = new ArrayList<>();

  private UseStmt(final UseStmt copy)
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

    parseTypeArgs(tokenizer, templateName);
  }

  private UseStmt(final Tokenizer tokenizer, final Token templateName)
  {
    super(tokenizer, templateName);

    parseTypeArgs(tokenizer, templateName);
  }

  public UseStmt(final TemplateStmt template, final FuncExpr func)
  {
    automatic = true;
    module = func.module;
    origin = func.origin;
    name = template.name;

    funcName = func.name;

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
      if (!typeArg.wasAnalyzed())
      {
        typeArg.analyze();
      }
    }

    // set up use-specific types
    for (final BaseExpr type : template.types)
    {
      final BaseExpr useType = type.clone();
      useType.analyze();
      types.add(useType);
    }

    if (typeArgs.size() == 1)
    {
      // check relationship expression
      if (template.relations.size() != 0)
      {
        int totalSize = 0;
        for (final BaseExpr relation : template.relations)
        {
          boolean found = false;
          for (final BaseExpr useType : types)
          {
            if (useType.name.equals(relation.name))
            {
              found = true;
              totalSize += useType.size;
              break;
            }
          }

          if (!found)
          {
            relation.error("Relation type name not found: " + relation.name);
          }
        }

        if (totalSize != typeArgs.get(0).size)
        {
          if (automatic)
          {
            // wrong template, do not instantiate any functions
            currentModule = oldModule;
            currentUse = oldUse;
            return;
          }

          error("Relation sum does not match");
        }
      }
    }


    boolean found = false;
    final ArrayList<FuncStmt> funcs = new ArrayList<>();
    for (final BaseExpr func : template.funcs)
    {
      final FuncStmt useFunc = new FuncStmt((FuncStmt) func);
      useFunc.origin = origin;
      useFunc.module = module;
      useFunc.use = this;
      useFunc.analyzeSignature();
      funcs.add(useFunc);
      if (automatic && useFunc.name.equals(funcName))
      {
        found = true;
      }
    }

    if (automatic && !found)
    {
      // wrong template, do not instantiate any functions
      currentModule = oldModule;
      currentUse = oldUse;
      return;
    }

    module.funcs.addAll(funcs);

    size = template.funcs.size();

    currentModule = oldModule;
    currentUse = oldUse;

    if (nextUse != null)
    {
      nextUse.analyze();
    }
  }

  @Override
  public BaseExpr clone()
  {
    return new UseStmt(this);
  }

  private void parseTypeArgs(final Tokenizer tokenizer, final Token templateName)
  {
    expect(tokenizer, Token.TOK_TEMPL_OPEN, "'<'");

    typeArgs.add(new ConstTypeName(tokenizer));

    while (tokenizer.tokenId() == Token.TOK_COMMA)
    {
      tokenizer.nextToken();

      typeArgs.add(new ConstTypeName(tokenizer));
    }

    expect(tokenizer, Token.TOK_TEMPL_CLOSE, "',' or '>'");

    if (tokenizer.tokenId() == Token.TOK_COMMA)
    {
      tokenizer.nextToken();

      nextUse = new UseStmt(tokenizer, templateName);
    }
  }
}
