package org.iota.qupla.qupla.expression;

import java.util.ArrayList;

import org.iota.qupla.qupla.context.base.QuplaBaseContext;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.expression.constant.ConstExpr;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;

public class SliceExpr extends BaseExpr
{
  public int fieldSize;
  public int fieldStart;
  public final ArrayList<BaseExpr> fields = new ArrayList<>();
  public BaseExpr sliceSize;
  public BaseExpr sliceStart;
  public int start;
  public int varSize;

  private SliceExpr(final SliceExpr copy)
  {
    super(copy);

    sliceSize = clone(copy.sliceSize);
    fields.addAll(copy.fields);
    fieldStart = copy.fieldStart;
    fieldSize = copy.fieldSize;
    start = copy.start;
    sliceStart = clone(copy.sliceStart);
    varSize = copy.varSize;
  }

  public SliceExpr(final Tokenizer tokenizer, final Token identifier)
  {
    super(tokenizer, identifier);

    while (tokenizer.tokenId() == Token.TOK_DOT)
    {
      tokenizer.nextToken();

      fields.add(new NameExpr(tokenizer, "field name"));
    }

    if (tokenizer.tokenId() == Token.TOK_ARRAY_OPEN)
    {
      tokenizer.nextToken();

      sliceStart = new ConstExpr(tokenizer).optimize();

      if (tokenizer.tokenId() == Token.TOK_COLON)
      {
        tokenizer.nextToken();
        sliceSize = new ConstExpr(tokenizer).optimize();
      }

      expect(tokenizer, Token.TOK_ARRAY_CLOSE, "']'");
    }
  }

  @Override
  public void analyze()
  {
    analyzeVar();

    reanalyze();
  }

  private void analyzeVar()
  {
    for (int i = scope.size() - 1; i >= 0; i--)
    {
      BaseExpr var = scope.get(i);
      if (var.name.equals(name))
      {
        if (var instanceof AssignExpr)
        {
          // does this assignment assign to a state variable?
          final int stateIndex = ((AssignExpr) var).stateIndex;
          if (stateIndex != 0)
          {
            // reference the actual state variable instead
            var = scope.get(stateIndex);
          }
        }

        stackIndex = var.stackIndex;
        typeInfo = var.typeInfo;
        size = var.size;
        varSize = size;
        fieldStart = 0;
        fieldSize = size;

        analyzeVarFields(var);
        return;
      }
    }

    error("Cannot find variable: " + name);
  }

  //TODO var is never used????
  private void analyzeVarFields(final BaseExpr var)
  {
    // start with entire vector
    start = 0;

    String fieldPath = name;
    for (final BaseExpr field : fields)
    {
      if (typeInfo == null || typeInfo.struct == null)
      {
        error("Expected structured trit vector: " + fieldPath);
      }

      boolean found = false;
      for (final BaseExpr structField : typeInfo.struct.fields)
      {
        if (structField.name.equals(field.name))
        {
          found = true;
          typeInfo = structField.typeInfo;
          size = structField.size;
          break;
        }

        start += structField.size;
      }

      if (!found)
      {
        error("Invalid structured trit vector field name: " + field);
      }

      fieldPath += "." + field;
    }

    fieldStart = start;
    fieldSize = size;
  }

  @Override
  public BaseExpr clone()
  {
    return new SliceExpr(this);
  }

  @Override
  public void eval(final QuplaBaseContext context)
  {
    context.evalSlice(this);
  }

  public void reanalyze()
  {
    if (sliceStart == null)
    {
      return;
    }

    start = fieldStart;
    size = fieldSize;

    sliceStart.analyze();
    if (sliceStart.size < 0 || sliceStart.size >= size)
    {
      sliceStart.error("Invalid slice start: " + sliceStart.size);
    }

    if (sliceSize != null)
    {
      sliceSize.analyze();
      if (sliceStart.size + sliceSize.size > size)
      {
        sliceSize.error("Invalid slice size (" + sliceStart.size + "+" + sliceSize.size + ">" + size + ")");
      }
    }

    start += sliceStart.size;
    size = sliceSize == null ? 1 : sliceSize.size;
  }
}
