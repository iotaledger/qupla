package org.iota.qupla;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.iota.qupla.abra.FpgaClient;

public class TestFpgaClient
{
  private FpgaClient client = new FpgaClient();

  public static void main(String[] args)
  {
    TestFpgaClient caller = new TestFpgaClient();
    caller.converse();
  }

  public void converse()
  {
    try
    {
      final byte[] data = Files.readAllBytes(Paths.get("add_27.qbc"));
      byte[] result = client.process('c', data);
      if (result == null)
      {
        System.out.println("Config failed");
        return;
      }

      final String trits = "111000000000000000000000000" + "1-1000000000000000000000000";
      final byte[] input = new byte[trits.length()];
      for (int i = 0; i < input.length; i++)
      {
        final char trit = trits.charAt(i);
        input[i] = (byte) "@1-0".indexOf(trit);
      }

      long start = System.currentTimeMillis();
      for (int i = 0; i < 1000; i++)
      {
        result = client.process('d', input);
      }
      long end = System.currentTimeMillis();
      Qupla.log("" + (end - start));

      if (result == null)
      {
        System.out.println("Exec failed");
        return;
      }

      for (int i = 0; i < result.length; i++)
      {
        final byte trit = result[i];
        result[i] = (byte) "@1-0".charAt(trit);
      }

      System.out.println("Result: " + new String(result, 0, result.length));
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
