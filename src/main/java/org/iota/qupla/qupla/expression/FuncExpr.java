package org.iota.qupla.qupla.expression;

import java.util.ArrayList;

import org.iota.qupla.qupla.context.base.QuplaBaseContext;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.expression.constant.ConstTypeName;
import org.iota.qupla.qupla.parser.QuplaModule;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;
import org.iota.qupla.qupla.statement.FuncStmt;
import org.iota.qupla.qupla.statement.TemplateStmt;
import org.iota.qupla.qupla.statement.UseStmt;

public class FuncExpr extends BaseExpr
{
  public final ArrayList<BaseExpr> args = new ArrayList<>();
  public int callIndex;
  public FuncStmt func;
  public final ArrayList<BaseExpr> funcTypes = new ArrayList<>();

  protected FuncExpr(final FuncExpr copy)
  {
    super(copy);

    callIndex = copy.callIndex;
    func = copy.func;
    cloneArray(funcTypes, copy.funcTypes);
    cloneArray(args, copy.args);
  }

  public FuncExpr(final Tokenizer tokenizer, final Token identifier)
  {
    super(tokenizer, identifier);

    if (tokenizer.tokenId() == Token.TOK_TEMPL_OPEN)
    {
      tokenizer.nextToken();

      funcTypes.add(new ConstTypeName(tokenizer));
      while (tokenizer.tokenId() == Token.TOK_COMMA)
      {
        tokenizer.nextToken();

        funcTypes.add(new ConstTypeName(tokenizer));
      }

      expect(tokenizer, Token.TOK_TEMPL_CLOSE, "'>'");
    }

    expect(tokenizer, Token.TOK_FUNC_OPEN, "'('");

    args.add(new CondExpr(tokenizer).optimize());

    while (tokenizer.tokenId() == Token.TOK_COMMA)
    {
      tokenizer.nextToken();

      args.add(new CondExpr(tokenizer).optimize());
    }

    expect(tokenizer, Token.TOK_FUNC_CLOSE, "')'");
  }

  @Override
  public void analyze()
  {
    if (wasAnalyzed())
    {
      return;
    }

    callIndex = callNr++;

    for (final BaseExpr funcType : funcTypes)
    {
      funcType.analyze();
      name += SEPARATOR + funcType.size;
    }

    func = (FuncStmt) findEntity(FuncStmt.class, "func");
    size = func.size;
    typeInfo = func.typeInfo;

    final ArrayList<BaseExpr> params = func.params;
    if (params.size() > args.size())
    {
      final BaseExpr param = params.get(args.size());
      error("Missing argument: " + param.name);
    }

    if (params.size() < args.size())
    {
      final BaseExpr arg = args.get(params.size());
      arg.error("Extra argument to function: " + func.name);
    }

    for (int i = 0; i < params.size(); i++)
    {
      final BaseExpr param = params.get(i);
      final BaseExpr arg = args.get(i);
      constTypeInfo = param.typeInfo;
      arg.analyze();
      constTypeInfo = null;
      if (param.size != arg.size)
      {
        arg.error("Invalid argument size " + param.size + " != " + arg.size);
      }
    }
  }

  @Override
  public BaseExpr clone()
  {
    return new FuncExpr(this);
  }

  @Override
  protected BaseExpr entityNotFound(final String what)
  {
    if (funcTypes.size() != 1)
    {
      return multiParameterInstantiation(what);
    }

    int triplet = funcTypes.get(0).size;
    while (triplet / 3 * 3 == triplet)
    {
      triplet /= 3;
    }

    final String funcName = origin.text;

    // find a template that can instantiate this function
    for (final TemplateStmt template : module.templates)
    {
      for (final BaseExpr func : template.funcs)
      {
        if (((FuncStmt) func).funcTypes.size() != funcTypes.size())
        {
          continue;
        }

        if (!func.name.equals(funcName))
        {
          continue;
        }

        if (template.relations.size() == 3 && triplet != 1)
        {
          // skip special case template which is only for for powers of 3
          continue;
        }

        final UseStmt autoUse = new UseStmt(template, this);
        autoUse.analyze();
        if (autoUse.wasAnalyzed())
        {
          // this template was acceptable
          return findEntity(FuncStmt.class, what);
        }
      }
    }

    for (final QuplaModule extern : module.modules)
    {
      for (final TemplateStmt template : extern.templates)
      {
        for (final BaseExpr func : template.funcs)
        {
          if (((FuncStmt) func).funcTypes.size() != funcTypes.size())
          {
            continue;
          }

          if (!func.name.equals(funcName))
          {
            continue;
          }

          if (template.relations.size() == 3 && triplet != 1)
          {
            // skip special case template which is only for for powers of 3
            continue;
          }

          final UseStmt autoUse = new UseStmt(template, this);
          autoUse.analyze();
          if (autoUse.wasAnalyzed())
          {
            // this template was acceptable
            return findEntity(FuncStmt.class, what);
          }
        }
      }
    }

    return super.entityNotFound(what);
  }

  @Override
  public void eval(final QuplaBaseContext context)
  {
    context.evalFuncCall(this);
  }

  private BaseExpr multiParameterInstantiation(final String what)
  {
    final String funcName = origin.text;

    // find a template that can instantiate this function
    for (final TemplateStmt template : module.templates)
    {
      for (final BaseExpr func : template.funcs)
      {
        if (((FuncStmt) func).funcTypes.size() != funcTypes.size())
        {
          continue;
        }

        if (!func.name.equals(funcName))
        {
          continue;
        }

        final UseStmt autoUse = new UseStmt(template, this);
        autoUse.analyze();
        if (autoUse.wasAnalyzed())
        {
          // this template was acceptable
          return findEntity(FuncStmt.class, what);
        }
      }
    }

    for (final QuplaModule extern : module.modules)
    {
      for (final TemplateStmt template : extern.templates)
      {
        for (final BaseExpr func : template.funcs)
        {
          if (((FuncStmt) func).funcTypes.size() != funcTypes.size())
          {
            continue;
          }

          if (!func.name.equals(funcName))
          {
            continue;
          }

          final UseStmt autoUse = new UseStmt(template, this);
          autoUse.analyze();
          if (autoUse.wasAnalyzed())
          {
            // this template was acceptable
            return findEntity(FuncStmt.class, what);
          }
        }
      }
    }

    return super.entityNotFound(what);
  }
}
