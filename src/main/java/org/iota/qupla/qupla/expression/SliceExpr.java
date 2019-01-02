package org.iota.qupla.qupla.expression;

import java.util.ArrayList;

import org.iota.qupla.qupla.context.base.QuplaBaseContext;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.expression.constant.ConstExpr;
import org.iota.qupla.qupla.parser.Token;
import org.iota.qupla.qupla.parser.Tokenizer;

public class SliceExpr extends BaseExpr
{
  public BaseExpr endOffset;
  public final ArrayList<BaseExpr> fields = new ArrayList<>();
  public int start;
  public BaseExpr startOffset;

  public SliceExpr(final SliceExpr copy)
  {
    super(copy);

    endOffset = clone(copy.endOffset);
    fields.addAll(copy.fields);
    start = copy.start;
    startOffset = clone(copy.startOffset);
  }

  public SliceExpr(final Tokenizer tokenizer, final Token identifier)
  {
    super(tokenizer, identifier);

    name = identifier.text;

    while (tokenizer.tokenId() == Token.TOK_DOT)
    {
      tokenizer.nextToken();

      fields.add(new NameExpr(tokenizer, "field name"));
    }

    if (tokenizer.tokenId() == Token.TOK_ARRAY_OPEN)
    {
      tokenizer.nextToken();

      startOffset = new ConstExpr(tokenizer).optimize();

      switch (tokenizer.tokenId())
      {
      case Token.TOK_COLON:
        tokenizer.nextToken();
        endOffset = new ConstExpr(tokenizer).optimize();
        break;
      }

      expect(tokenizer, Token.TOK_ARRAY_CLOSE, "']'");
    }
  }

  @Override
  public void analyze()
  {
    analyzeVar();

    if (startOffset == null)
    {
      return;
    }

    startOffset.analyze();
    if (startOffset.size < 0 || startOffset.size >= size)
    {
      startOffset.error("Invalid slice start offset");
    }

    // at least a single indexed trit
    int offset = startOffset.size;
    int end = offset;

    if (endOffset != null)
    {
      endOffset.analyze();
      if (offset + endOffset.size > size)
      {
        endOffset.error("Invalid slice size (" + offset + "+" + endOffset.size + ">" + size + ")");
      }

      end = offset + endOffset.size - 1;
    }

    start += offset;
    size = end - offset + 1;
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

        analyzeVarFields(var);
        return;
      }
    }

    error("Cannot find variable: " + name);
  }

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
  }

  @Override
  public BaseExpr append()
  {
    append(name);

    for (final BaseExpr field : fields)
    {
      append(".").append(field);
    }

    if (startOffset != null)
    {
      append("[").append(startOffset);

      if (endOffset != null)
      {
        append(" : ").append(endOffset);
      }

      append("]");
    }

    return this;
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
}
