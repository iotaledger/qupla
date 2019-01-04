package org.iota.qupla.helper;

import java.util.HashSet;

public class Verilog
{
  public HashSet<Integer> mergefuncs = new HashSet<>();
  public final String prefix = "merge__";

  public void addMergeFuncs(final BaseContext context)
  {
    for (final Integer size : mergefuncs)
    {
      final String funcName = prefix + size;
      context.newline().append("function [" + (size * 2 - 1) + ":0] ").append(funcName).append("(").newline().indent();
      context.append("  input [" + (size * 2 - 1) + ":0] input1").newline();
      context.append(", input [" + (size * 2 - 1) + ":0] input2").newline();
      context.append(");").newline();
      context.append("begin").newline().indent();
      context.append(funcName).append(" = {").newline().indent();
      boolean first = true;
      for (int i = 0; i < size; i++)
      {
        final int from = i * 2 + 1;
        final int to = i * 2;
        context.append(first ? "" : ": ").append("merge_lut_(input1[" + from + ":" + to + "], input2[" + from + ":" + to + "])").newline();
        first = false;
      }
      context.undent();
      context.append("};").newline().undent();
      context.append("end").newline().undent();
      context.append("endfunction").newline();
    }
  }

  public void addMergeLut(final BaseContext context)
  {
    context.newline().append("x reg [0:0];").newline();
    context.newline().append("function [1:0] merge_lut_(").newline().indent();
    context.append("  input [1:0] input1").newline();
    context.append(", input [1:0] input2").newline();
    context.append(");").newline();
    context.append("begin").newline().indent();
    context.append("case ({input1, input2})").newline();
    context.append("4'b0000: merge_lut_ = 2'b00;").newline();
    context.append("4'b0001: merge_lut_ = 2'b01;").newline();
    context.append("4'b0010: merge_lut_ = 2'b10;").newline();
    context.append("4'b0011: merge_lut_ = 2'b11;").newline();
    context.append("4'b0100: merge_lut_ = 2'b01;").newline();
    context.append("4'b1000: merge_lut_ = 2'b10;").newline();
    context.append("4'b1100: merge_lut_ = 2'b11;").newline();
    context.append("4'b0101: merge_lut_ = 2'b01;").newline();
    context.append("4'b1010: merge_lut_ = 2'b10;").newline();
    context.append("4'b1111: merge_lut_ = 2'b11;").newline();
    context.append("default: merge_lut_ = 2'b00;").newline();
    context.append("         x <= 1;").newline();
    context.append("endcase").newline().undent();
    context.append("end").newline().undent();
    context.append("endfunction").newline();
  }

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
}
