package org.iota.qupla.expression;

import java.util.ArrayList;

import org.iota.qupla.context.CodeContext;
import org.iota.qupla.expression.base.BaseExpr;
import org.iota.qupla.expression.constant.ConstTypeName;
import org.iota.qupla.parser.Token;
import org.iota.qupla.parser.Tokenizer;
import org.iota.qupla.statement.FuncStmt;

public class FuncExpr extends BaseExpr
{
  public final ArrayList<BaseExpr> args = new ArrayList<>();
  public int callIndex;
  public FuncStmt func;
  public final ArrayList<BaseExpr> funcTypes = new ArrayList<>();

  public FuncExpr(final FuncExpr copy)
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

    name = identifier.text;

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
  public BaseExpr append()
  {
    append(name.split("_")[0]);

    if (funcTypes.size() != 0)
    {
      boolean first = true;
      for (final BaseExpr funcType : funcTypes)
      {
        append(first ? "<" : ", ").append(funcType);
        first = false;
      }

      append(">");
    }

    boolean first = true;
    for (final BaseExpr arg : args)
    {
      append(first ? "(" : ", ").append(arg);
      first = false;
    }

    return append(")");
  }

  @Override
  public BaseExpr clone()
  {
    return new FuncExpr(this);
  }

  @Override
  public void eval(final CodeContext context)
  {
    context.evalFuncCall(this);
  }
}
