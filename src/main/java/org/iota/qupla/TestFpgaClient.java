package org.iota.qupla;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.iota.qupla.abra.FpgaClient;
import org.iota.qupla.helper.TritVector;

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
      byte[] input = Files.readAllBytes(Paths.get("add_27.qbc"));
      byte[] output = client.process('c', input);
      if (output == null)
      {
        System.out.println("Config failed");
        return;
      }

      final String tritString = "111000000000000000000000000" + "1-1000000000000000000000000";
      final TritVector value = new TritVector(tritString);

      input = value.trits();
      for (int i = 0; i < input.length; i++)
      {
        input[i] = TritVector.tritToBits(input[i]);
      }

      long start = System.currentTimeMillis();
      for (int i = 0; i < 1000; i++)
      {
        output = client.process('d', input);
      }
      long end = System.currentTimeMillis();
      Qupla.log("" + (end - start));

      if (output == null)
      {
        System.out.println("Exec failed");
        return;
      }

      for (int i = 0; i < output.length; i++)
      {
        output[i] = TritVector.bitsToTrit(output[i]);
      }

      System.out.println("Result: " + new TritVector(output));
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
