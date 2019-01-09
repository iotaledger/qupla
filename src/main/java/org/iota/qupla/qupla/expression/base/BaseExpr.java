package org.iota.qupla.qupla.expression.base;

import java.util.ArrayList;

import org.iota.qupla.exception.CodeException;
import org.iota.qupla.qupla.context.QuplaPrintContext;
import org.iota.qupla.qupla.context.base.QuplaBaseContext;
import org.iota.qupla.qupla.parser.QuplaModule;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;
import org.iota.qupla.qupla.statement.TypeStmt;
import org.iota.qupla.qupla.statement.UseStmt;

public abstract class BaseExpr
{
  protected static final String SEPARATOR = "_";
  public static int callNr;
  public static TypeStmt constTypeInfo;
  public static QuplaModule currentModule;
  public static UseStmt currentUse;
  protected static QuplaPrintContext printer = new QuplaPrintContext();
  public static ArrayList<BaseExpr> scope = new ArrayList<>();

  public QuplaModule module;
  public String name;
  public BaseExpr next;
  public Token origin;
  public int size;
  public int stackIndex;
  public TypeStmt typeInfo;

  protected BaseExpr()
  {
  }

  protected BaseExpr(final BaseExpr copy)
  {
    module = currentModule;
    name = copy.name;
    origin = copy.origin;
    size = copy.size;
    stackIndex = copy.stackIndex;
    typeInfo = copy.typeInfo;
  }

  protected BaseExpr(final Tokenizer tokenizer)
  {
    module = tokenizer.module;
    origin = tokenizer.currentToken();
  }

  protected BaseExpr(final Tokenizer tokenizer, final Token identifier)
  {
    module = tokenizer.module;
    origin = identifier;
    name = identifier.text;
  }

  public static void logLine(final String text)
  {
    System.out.println(text);
  }

  public abstract void analyze();

  public boolean analyzed()
  {
    return size != 0;
  }

  public BaseExpr clone(final BaseExpr expr)
  {
    return expr == null ? null : expr.clone();
  }

  public abstract BaseExpr clone();

  public void cloneArray(final ArrayList<BaseExpr> lhs, final ArrayList<BaseExpr> rhs)
  {
    for (final BaseExpr expr : rhs)
    {
      lhs.add(expr.clone());
    }
  }

  protected BaseExpr entityNotFound(final String what)
  {
    error("Undefined " + what + " name: " + name);
    return null;
  }

  public CodeException error(final Token token, final String message)
  {
    throw new CodeException(token, message);
  }

  public void error(final String message)
  {
    error(origin, message);
  }

  public void eval(final QuplaBaseContext context)
  {
    context.evalBaseExpr(this);
  }

  public Token expect(final Tokenizer tokenizer, final int type, final String what)
  {
    final Token token = tokenizer.currentToken();
    if (token.id != type)
    {
      error(token, "Expected " + what);
    }

    tokenizer.nextToken();
    return token;
  }

  public BaseExpr findEntity(final Class classId, final String what)
  {
    for (final BaseExpr entity : module.entities(classId))
    {
      if (entity.name.equals(name))
      {
        if (!entity.analyzed())
        {
          entity.analyze();
        }

        return entity;
      }
    }

    BaseExpr externEntity = null;
    for (final QuplaModule extern : module.modules)
    {
      for (final BaseExpr entity : extern.entities(classId))
      {
        if (entity.name.equals(name))
        {
          if (externEntity == null || externEntity == entity)
          {
            externEntity = entity;
            break;
          }

          error("Ambiguous " + what + " name: " + name + " in " + externEntity.module + " and " + entity.module);
        }
      }
    }

    return externEntity != null ? externEntity : entityNotFound(what);
  }

  public void log(final String text)
  {
    final String name = getClass().getName();
    logLine(name.substring(name.lastIndexOf(".") + 1) + ": " + text);
  }

  public BaseExpr optimize()
  {
    return this;
  }

  @Override
  public String toString()
  {
    final String oldString = printer.string;
    printer.string = new String(new char[0]);
    toStringify();
    final String ret = printer.string;
    printer.string = oldString;
    return ret;
  }

  public void toStringify()
  {
    eval(printer);
  }

  public void warning(final String message)
  {
    log("WARNING: " + message);
  }
}
