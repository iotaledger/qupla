package org.iota.qupla.qupla.statement;

import java.util.ArrayList;

import org.iota.qupla.helper.TritVector;
import org.iota.qupla.qupla.context.base.QuplaBaseContext;
import org.iota.qupla.qupla.expression.AffectExpr;
import org.iota.qupla.qupla.expression.AssignExpr;
import org.iota.qupla.qupla.expression.CondExpr;
import org.iota.qupla.qupla.expression.JoinExpr;
import org.iota.qupla.qupla.expression.NameExpr;
import org.iota.qupla.qupla.expression.StateExpr;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.expression.constant.ConstTypeName;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;

public class FuncStmt extends BaseExpr
{
  public boolean analyzed;
  public boolean anyNull;
  public final ArrayList<BaseExpr> assignExprs = new ArrayList<>();
  public final ArrayList<BaseExpr> envExprs = new ArrayList<>();
  public final ArrayList<BaseExpr> funcTypes = new ArrayList<>();
  public TritVector nullReturn;
  public final ArrayList<BaseExpr> params = new ArrayList<>();
  public BaseExpr returnExpr;
  public BaseExpr returnType;
  public final ArrayList<BaseExpr> stateExprs = new ArrayList<>();
  public UseStmt use;

  public FuncStmt(UseStmt use)
  {
    // this FuncStmt is a placeholder for template function (required for make references before UseStmt is analyzed)
    this.use = use;
    this.module = use.module;
    name = "";
  }

  public FuncStmt(final FuncStmt copy)
  {
    super(copy);

    analyzed = copy.analyzed;
    anyNull = copy.anyNull;
    returnType = clone(copy.returnType);
    cloneArray(funcTypes, copy.funcTypes);
    cloneArray(params, copy.params);
    cloneArray(envExprs, copy.envExprs);
    cloneArray(stateExprs, copy.stateExprs);
    cloneArray(assignExprs, copy.assignExprs);
    returnExpr = clone(copy.returnExpr);
  }

  public FuncStmt(final Tokenizer tokenizer)
  {
    super(tokenizer);

    expect(tokenizer, Token.TOK_FUNC, "func");

    returnType = new ConstTypeName(tokenizer);

    final Token funcName = expect(tokenizer, Token.TOK_NAME, "function name");
    name = funcName.text;

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

    parseParam(tokenizer);

    while (tokenizer.tokenId() == Token.TOK_COMMA)
    {
      tokenizer.nextToken();

      parseParam(tokenizer);
    }

    expect(tokenizer, Token.TOK_FUNC_CLOSE, "',' or ')'");

    expect(tokenizer, Token.TOK_GROUP_OPEN, "'{'");

    parseBody(tokenizer);
    stackIndex = scope.size();
    scope.clear();
  }

  @Override
  public void analyze()
  {
    if (wasAnalyzed())
    {
      return;
    }

    analyzed = true;

    callNr = 0;

    // if this function has an associated use statement
    // then we should replace placeholders with actual types
    final UseStmt oldUse = currentUse;
    currentUse = use;

    final ArrayList<BaseExpr> oldScope = scope;
    scope = new ArrayList<>();

    scope.addAll(params);

    for (final BaseExpr envExpr : envExprs)
    {
      envExpr.analyze();
    }

    for (final BaseExpr stateExpr : stateExprs)
    {
      stateExpr.analyze();
    }

    for (final BaseExpr assignExpr : assignExprs)
    {
      assignExpr.analyze();
    }

    constTypeInfo = typeInfo;
    returnExpr.analyze();
    constTypeInfo = null;

    if (returnExpr.size != returnType.size)
    {
      returnExpr.error("Return type size mismatch");
    }

    scope = oldScope;

    currentUse = oldUse;
  }

  public void analyzeSignature()
  {
    for (final BaseExpr funcType : funcTypes)
    {
      funcType.analyze();
      name += SEPARATOR + funcType.size;
    }

    for (final BaseExpr param : params)
    {
      param.analyze();
    }

    returnType.analyze();
    size = returnType.size;
    typeInfo = returnType.typeInfo;

    nullReturn = new TritVector(size, '@');

    for (final FuncStmt func : module.funcs)
    {
      if (name.equals(func.name) && func != this)
      {
        error("Duplicate function name: " + name);
      }
    }
  }

  @Override
  public BaseExpr clone()
  {
    return new FuncStmt(this);
  }

  @Override
  public void eval(final QuplaBaseContext context)
  {
    context.evalFuncBody(this);
  }

  private void parseBody(final Tokenizer tokenizer)
  {
    while (tokenizer.tokenId() == Token.TOK_JOIN)
    {
      envExprs.add(new JoinExpr(tokenizer));
    }

    while (tokenizer.tokenId() == Token.TOK_AFFECT)
    {
      envExprs.add(new AffectExpr(tokenizer));
    }

    while (tokenizer.tokenId() == Token.TOK_STATE)
    {
      stateExprs.add(new StateExpr(tokenizer));
    }

    while (tokenizer.tokenId() != Token.TOK_RETURN)
    {
      assignExprs.add(new AssignExpr(tokenizer));
    }

    tokenizer.nextToken();

    returnExpr = new CondExpr(tokenizer).optimize();

    expect(tokenizer, Token.TOK_GROUP_CLOSE, "'}'");
  }

  private void parseParam(final Tokenizer tokenizer)
  {
    final ConstTypeName paramType = new ConstTypeName(tokenizer);

    final NameExpr param = new NameExpr(tokenizer, "param name");
    param.type = paramType;

    param.stackIndex = scope.size();
    params.add(param);
    scope.add(param);
  }

  @Override
  public void toStringify()
  {
    printer.evalFuncBodySignature(this);
  }

  @Override
  public boolean wasAnalyzed()
  {
    return analyzed;
  }
}
