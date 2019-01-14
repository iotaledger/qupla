package org.iota.qupla.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

public class Verilog
{
  public final ArrayList<Integer> addedFuncs = new ArrayList<>();
  public final HashSet<Integer> mergeFuncs = new HashSet<>();
  public final String prefix = "merge__";

  public BaseContext appendVector(final BaseContext context, final String trits)
  {
    final int size = trits.length() * 2;
    context.append(size + "'b");
    for (int i = 0; i < trits.length(); i++)
    {
      switch (trits.charAt(i))
      {
      case '0':
        context.append("01");
        break;
      case '1':
        context.append("10");
        break;
      case '-':
        context.append("11");
        break;
      case '@':
        context.append("00");
        break;
      }
    }

    return context;
  }

  private void generateMergeFunc(final BaseContext context, final int[] sizes)
  {
    int size = 0;
    for (int i = 0; i < sizes.length; i++)
    {
      size += sizes[i];
    }

    mergeFuncs.remove(size);
    addedFuncs.add(size);

    final String funcName = prefix + size;
    context.newline().append("function " + size(size) + " ").append(funcName).append("(").newline().indent();
    context.append("  input " + size(size) + " input1").newline();
    context.append(", input " + size(size) + " input2").newline();
    context.append(");").newline();

    for (int i = 0; i < sizes.length; i++)
    {
      context.append("reg " + size(sizes[i]) + " p" + i + ";").newline();
    }

    context.append("begin").newline().indent();

    int offset = size * 2 - 1;
    for (int i = 0; i < sizes.length; i++)
    {
      final int length = sizes[i] * 2;
      final int from = offset;
      final int to = from - length + 1;
      context.append("p" + i + " = " + prefix + sizes[i]);
      context.append("(input1[" + from + ":" + to + "], input2[" + from + ":" + to + "]);");
      context.newline();
      offset -= length;
    }

    context.append(funcName).append(" = { ");
    boolean first = true;
    for (int i = 0; i < sizes.length; i++)
    {
      context.append(first ? "" : ", ").append("p" + i);
      first = false;
    }

    context.append(" };").newline().undent();
    context.append("end").newline().undent();
    context.append("endfunction").newline();
  }

  public void generateMergeFuncs(final BaseContext context)
  {
    // we have the lut func for size 1
    generateMergeLut(context);
    mergeFuncs.remove(1);
    addedFuncs.add(1);

    int largest = 1;
    for (final Integer value : mergeFuncs)
    {
      if (largest < value)
      {
        largest = value;
      }
    }

    // add all needed powers of 3
    for (int power = 3; power <= largest; power *= 3)
    {
      int size = power / 3;
      generateMergeFunc(context, new int[] {
          size,
          size,
          size
      });
    }

    final int sizes[] = new int[100];

    // go through the remaining ones in sorted order
    final ArrayList<Integer> remaining = new ArrayList<>(mergeFuncs);
    Collections.sort(remaining);
    for (final Integer size : remaining)
    {
      Collections.sort(addedFuncs);

      // compose the next from from existing ones by gathering
      // as many existing ones needed to get to the requires size
      int totalSizes = 0;
      int remain = size;
      while (remain > 0)
      {
        for (int i = addedFuncs.size() - 1; i > 0; i--)
        {
          int next = addedFuncs.get(i);
          if (next <= remain)
          {
            sizes[totalSizes++] = next;
            remain -= next;
            break;
          }
        }
      }

      generateMergeFunc(context, Arrays.copyOf(sizes, totalSizes));
    }
  }

  private void generateMergeLut(final BaseContext context)
  {
    context.newline().append("reg [0:0] x;").newline();
    context.newline().append("function [1:0] merge__1(").newline().indent();
    context.append("  input [1:0] input1").newline();
    context.append(", input [1:0] input2").newline();
    context.append(");").newline();
    context.append("begin").newline().indent();
    context.append("case ({input1, input2})").newline();
    context.append("4'b0000: merge__1 = 2'b00;").newline();
    context.append("4'b0001: merge__1 = 2'b01;").newline();
    context.append("4'b0010: merge__1 = 2'b10;").newline();
    context.append("4'b0011: merge__1 = 2'b11;").newline();
    context.append("4'b0100: merge__1 = 2'b01;").newline();
    context.append("4'b1000: merge__1 = 2'b10;").newline();
    context.append("4'b1100: merge__1 = 2'b11;").newline();
    context.append("4'b0101: merge__1 = 2'b01;").newline();
    context.append("4'b1010: merge__1 = 2'b10;").newline();
    context.append("4'b1111: merge__1 = 2'b11;").newline();
    context.append("default: begin").newline();
    context.append("           merge__1 = 2'b00;").newline();
    context.append("           x = 1;").newline();
    context.append("         end").newline();
    context.append("endcase").newline().undent();
    context.append("end").newline().undent();
    context.append("endfunction").newline();
  }

  public String size(final int trits)
  {
    return "[" + (trits * 2 - 1) + ":0]";
  }
}
