package org.iota.qupla.statement;

import java.util.ArrayList;

import org.iota.qupla.context.CodeContext;
import org.iota.qupla.expression.AffectExpr;
import org.iota.qupla.expression.AssignExpr;
import org.iota.qupla.expression.CondExpr;
import org.iota.qupla.expression.JoinExpr;
import org.iota.qupla.expression.NameExpr;
import org.iota.qupla.expression.StateExpr;
import org.iota.qupla.expression.base.BaseExpr;
import org.iota.qupla.expression.constant.ConstTypeName;
import org.iota.qupla.helper.TritVector;
import org.iota.qupla.parser.Token;
import org.iota.qupla.parser.Tokenizer;

public class FuncStmt extends BaseExpr
{
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
  public int useIndex;

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
    callNr = 0;

    // if this function has an associated use statement
    // then we should replace placeholders with actual types
    currentUse = use;
    currentUseIndex = useIndex;

    if (use != null)
    {
      // find the template types we created during signature analysis
      for (final BaseExpr type : use.template.types)
      {
        String typeName = use.template.name;
        final ArrayList<BaseExpr> typeArgs = use.typeInstantiations.get(currentUseIndex);
        for (final BaseExpr typeArg : typeArgs)
        {
          typeName += SEPARATOR + typeArg.name;
        }

        typeName += SEPARATOR + type.name;
        type.typeInfo = null;
        for (final TypeStmt moduleType : use.module.types)
        {
          if (moduleType.name.equals(typeName))
          {
            type.typeInfo = moduleType;
            break;
          }
        }

        if (type.typeInfo == null)
        {
          error("WTF? failed finding type: " + typeName + " in " + use.module);
        }
      }
    }

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

    scope.clear();

    currentUse = null;
    currentUseIndex = 0;
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
  public BaseExpr append()
  {
    appendSignature();

    append(" {").newline().indent();

    for (final BaseExpr envExpr : envExprs)
    {
      append(envExpr).newline();
    }

    for (final BaseExpr stateExpr : stateExprs)
    {
      append(stateExpr).newline();
    }

    for (final BaseExpr assignExpr : assignExprs)
    {
      append(assignExpr).newline();
    }

    append("return ").append(returnExpr).newline();

    return undent().append("}");
  }

  public void appendSignature()
  {
    append("func ").append(returnType).append(" ").append(name.split("_")[0]);
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
    for (final BaseExpr param : params)
    {
      append(first ? "(" : ", ").append(param);
      first = false;
    }

    append(")");
  }

  @Override
  public BaseExpr clone()
  {
    return new FuncStmt(this);
  }

  public void copyFrom(final FuncStmt copy)
  {
    // replace placeholder with template function instantiated by UseStmt
    // constructor FuncStmt(final FuncStmt copy) cannot be used here, as that would create a new object
    module = copy.module;
    name = copy.name;
    origin = copy.origin;
    size = copy.size;
    stackIndex = copy.stackIndex;
    typeInfo = copy.typeInfo;
    anyNull = copy.anyNull;
    use = copy.use;
    useIndex = copy.useIndex;
    returnType = clone(copy.returnType);
    nullReturn = copy.nullReturn;
    cloneArray(funcTypes, copy.funcTypes);
    cloneArray(params, copy.params);
    cloneArray(envExprs, copy.envExprs);
    cloneArray(stateExprs, copy.stateExprs);
    cloneArray(assignExprs, copy.assignExprs);
    returnExpr = clone(copy.returnExpr);
  }

  @Override
  public void eval(final CodeContext context)
  {
    for (final BaseExpr stateExpr : stateExprs)
    {
      stateExpr.eval(context);
    }

    for (final BaseExpr assignExpr : assignExprs)
    {
      assignExpr.eval(context);
    }

    returnExpr.eval(context);
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
    appendSignature();
  }
}
