package org.iota.qupla;

import java.util.HashSet;

public class Helix
{
  private static final String helix[] = {
      "---  - - -",
      "--0  0 - -",
      "--1  - 1 -",
      "-0-  - - 0",
      "-00  0 0 -",
      "-01  - 0 1",
      "-1-  1 - -",
      "-10  - 1 0",
      "-11  1 - 1",
      "0--  - 0 -",
      "0-0  - 0 0",
      "0-1  0 - 1",
      "00-  0 - 0",
      "000  0 0 0",
      "001  1 0 0",
      "01-  0 1 -",
      "010  0 0 1",
      "011  1 1 0",
      "1--  - - 1",
      "1-0  1 - 0",
      "1-1  1 1 -",
      "10-  1 0 -",
      "100  0 1 0",
      "101  0 1 1",
      "11-  - 1 1",
      "110  1 0 1",
      "111  1 1 1"
  };
  private static HashSet<String> outputs = new HashSet<>();
  private static final String trits = "-01";

  private static void addOuts(final String outs)
  {
    for (int offset = 1; offset < 6; offset += 2)
    {
      String out = "";
      for (int i = 0; i < 18; i += 6)
      {
        out += outs.substring(offset + i, offset + i + 1);
      }

      outputs.add(out);
    }
  }

  private static void helix1()
  {
    log("One-trit lookups");

    for (int b = 0; b < 3; b++)
    {
      char B = trits.charAt(b);
      for (int c = 0; c < 3; c++)
      {
        char C = trits.charAt(c);
        log("\nB=" + B + ", C=" + C);
        log("A  X Y Z");
        String outs = "";
        for (int a = 0; a < 3; a++)
        {
          char A = trits.charAt(a);
          final String in = "" + A + B + C;
          for (int i = 0; i < 27; i++)
          {
            if (helix[i].startsWith(in))
            {
              log("" + A + helix[i].substring(3));
              outs += helix[i].substring(4);
              break;
            }
          }
        }

        addOuts(outs);
      }
    }

    for (int a = 0; a < 3; a++)
    {
      char A = trits.charAt(a);
      for (int c = 0; c < 3; c++)
      {
        char C = trits.charAt(c);
        log("\nA=" + A + ", C=" + C);
        log("B  X Y Z");
        String outs = "";
        for (int b = 0; b < 3; b++)
        {
          char B = trits.charAt(b);
          final String in = "" + A + B + C;
          for (int i = 0; i < 27; i++)
          {
            if (helix[i].startsWith(in))
            {
              log("" + B + helix[i].substring(3));
              outs += helix[i].substring(4);
              break;
            }
          }
        }

        addOuts(outs);
      }
    }

    for (int a = 0; a < 3; a++)
    {
      char A = trits.charAt(a);
      for (int b = 0; b < 3; b++)
      {
        char B = trits.charAt(b);
        log("\nA=" + A + ", B=" + B);
        log("C  X Y Z");
        String outs = "";
        for (int c = 0; c < 3; c++)
        {
          char C = trits.charAt(c);
          final String in = "" + A + B + C;
          for (int i = 0; i < 27; i++)
          {
            if (helix[i].startsWith(in))
            {
              log("" + C + helix[i].substring(3));
              outs += helix[i].substring(4);
              break;
            }
          }
        }

        addOuts(outs);
      }
    }

    log("\nOutput combinations: " + outputs.size());

    for (int i = 0; i < 27; i++)
    {
      final String output = helix[i].substring(0, 3);
      if (outputs.contains(output))
      {
        log(output);
      }
    }
  }

  private static void helix2()
  {
    log("\n\nTwo-trit lookups");

    for (int a = 0; a < 3; a++)
    {
      char A = trits.charAt(a);
      log("\nA=" + A);
      log("B C  X Y Z");
      for (int b = 0; b < 3; b++)
      {
        char B = trits.charAt(b);
        for (int c = 0; c < 3; c++)
        {
          char C = trits.charAt(c);
          final String in = "" + A + B + C;
          for (int i = 0; i < 27; i++)
          {
            if (helix[i].startsWith(in))
            {
              log("" + B + " " + C + helix[i].substring(3));
              break;
            }
          }
        }
      }
    }

    for (int a = 0; a < 3; a++)
    {
      char A = trits.charAt(a);
      log("\nA=" + A);
      log("C B  X Y Z");
      for (int c = 0; c < 3; c++)
      {
        char C = trits.charAt(c);
        for (int b = 0; b < 3; b++)
        {
          char B = trits.charAt(b);
          final String in = "" + A + B + C;
          for (int i = 0; i < 27; i++)
          {
            if (helix[i].startsWith(in))
            {
              log("" + C + " " + B + helix[i].substring(3));
              break;
            }
          }
        }
      }
    }

    for (int b = 0; b < 3; b++)
    {
      char B = trits.charAt(b);
      log("\nB=" + B);
      log("A C  X Y Z");
      for (int a = 0; a < 3; a++)
      {
        char A = trits.charAt(a);
        for (int c = 0; c < 3; c++)
        {
          char C = trits.charAt(c);
          final String in = "" + A + B + C;
          for (int i = 0; i < 27; i++)
          {
            if (helix[i].startsWith(in))
            {
              log("" + A + " " + C + helix[i].substring(3));
              break;
            }
          }
        }
      }
    }

    for (int b = 0; b < 3; b++)
    {
      char B = trits.charAt(b);
      log("\nB=" + B);
      log("C A  X Y Z");
      for (int c = 0; c < 3; c++)
      {
        char C = trits.charAt(c);
        for (int a = 0; a < 3; a++)
        {
          char A = trits.charAt(a);
          final String in = "" + A + B + C;
          for (int i = 0; i < 27; i++)
          {
            if (helix[i].startsWith(in))
            {
              log("" + C + " " + A + helix[i].substring(3));
              break;
            }
          }
        }
      }
    }

    for (int c = 0; c < 3; c++)
    {
      char C = trits.charAt(c);
      log("\nC=" + C);
      log("A B  X Y Z");
      for (int a = 0; a < 3; a++)
      {
        char A = trits.charAt(a);
        for (int b = 0; b < 3; b++)
        {
          char B = trits.charAt(b);
          final String in = "" + A + B + C;
          for (int i = 0; i < 27; i++)
          {
            if (helix[i].startsWith(in))
            {
              log("" + A + " " + B + helix[i].substring(3));
              break;
            }
          }
        }
      }
    }

    for (int c = 0; c < 3; c++)
    {
      char C = trits.charAt(c);
      log("\nC=" + C);
      log("B A  X Y Z");
      for (int b = 0; b < 3; b++)
      {
        char B = trits.charAt(b);
        for (int a = 0; a < 3; a++)
        {
          char A = trits.charAt(a);
          final String in = "" + A + B + C;
          for (int i = 0; i < 27; i++)
          {
            if (helix[i].startsWith(in))
            {
              log("" + B + " " + A + helix[i].substring(3));
              break;
            }
          }
        }
      }
    }
  }

  private static void helixExplorer()
  {
    helix1();
    helix2();
  }

  public static void log(final String text)
  {
    System.out.println(text);
  }

  public static void main(final String[] args)
  {
    helixExplorer();
  }
}
