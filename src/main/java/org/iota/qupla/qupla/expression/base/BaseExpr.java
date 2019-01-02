package org.iota.qupla.qupla.expression.base;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

import org.iota.qupla.exception.CodeException;
import org.iota.qupla.qupla.context.base.QuplaBaseContext;
import org.iota.qupla.qupla.parser.Module;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;
import org.iota.qupla.qupla.statement.TypeStmt;
import org.iota.qupla.qupla.statement.UseStmt;

public abstract class BaseExpr
{
  public static final String SEPARATOR = "_";
  public static int callNr;
  public static TypeStmt constTypeInfo;
  public static Module currentModule;
  public static UseStmt currentUse;
  public static int currentUseIndex;
  private static boolean mustIndent;
  private static int newlines;
  public static final ArrayList<BaseExpr> scope = new ArrayList<>();
  private static int spaces;
  private static String string;
  public static Writer writer;

  public Module module;
  public String name;
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

  protected BaseExpr(final Tokenizer tokenizer, final Token origin)
  {
    module = tokenizer.module;
    this.origin = origin != null ? origin : tokenizer.currentToken();
  }

  public static void logLine(final String text)
  {
    System.out.println(text);
  }

  public abstract void analyze();

  protected TypeStmt analyzeType()
  {
    if (currentUse != null)
    {
      for (final BaseExpr type : currentUse.template.types)
      {
        if (type.name.equals(name))
        {
          if (type.typeInfo.size == 0)
          {
            error("Did not analyze: " + name);
          }

          size = type.typeInfo.size;
          return type.typeInfo;
        }
      }
    }

    final TypeStmt type = (TypeStmt) findEntity(TypeStmt.class, "type");
    size = type.size;
    return type;
  }

  public abstract BaseExpr append();

  public BaseExpr append(final BaseExpr item)
  {
    return item == null ? this : item.append();
  }

  public BaseExpr append(final String text)
  {
    if (text.length() == 0)
    {
      return this;
    }

    if ("\n".equals(text))
    {
      // count consecutive newlines
      newlines++;
      if (newlines == 1)
      {
        // always emit first newline since it ends previous line
        appendify("\n");
      }

      return this;
    }

    // never have empty lines before a closing brace
    if (!"}".equals(text))
    {
      // emit accumulated newlines, without indenting
      for (int i = 1; i < newlines; i++)
      {
        appendify("\n");
      }
    }
    newlines = 0;

    if (mustIndent)
    {
      mustIndent = false;
      for (int i = 0; i < spaces; i++)
      {
        appendify(" ");
      }
    }

    appendify(text);
    return this;
  }

  private void appendify(final String text)
  {
    if (string != null)
    {
      string += text;
      return;
    }

    if (writer != null)
    {
      try
      {
        writer.append(text);
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
    }
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
    error("Cannot call eval: " + toString());
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
        if (entity.size == 0)
        {
          entity.analyze();
        }

        return entity;
      }
    }

    BaseExpr externEntity = null;
    for (final Module extern : module.modules)
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

    if (externEntity == null)
    {
      error("Undefined " + what + " name: " + name);
    }

    return externEntity;
  }

  public BaseExpr indent()
  {
    spaces += 2;
    return this;
  }

  public void log(final String text)
  {
    final String name = getClass().getName();
    logLine(name.substring(name.lastIndexOf(".") + 1) + ": " + text);
  }

  public BaseExpr newline()
  {
    append("\n");
    mustIndent = true;
    return this;
  }

  public BaseExpr optimize()
  {
    return this;
  }

  public String source()
  {
    string = "";
    append();
    final String ret = string;
    string = null;
    return ret;
  }

  @Override
  public String toString()
  {
    final String oldString = string;
    string = new String(new char[0]);
    toStringify();
    final String ret = string;
    string = oldString;
    return ret;
  }

  public void toStringify()
  {
    append();
  }

  public BaseExpr undent()
  {
    spaces -= 2;
    return this;
  }

  public void warning(final String message)
  {
    log("WARNING: " + message);
  }
}
