package org.iota.qupla.dispatcher.entity.tangle;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import jota.IotaAPI;
import jota.dto.response.SendTransferResponse;
import jota.error.ArgumentException;
import jota.model.Transaction;
import jota.model.Transfer;
import jota.pow.pearldiver.PearlDiverLocalPoW;
import jota.utils.Converter;
import jota.utils.TrytesConverter;

public class TangleInterface
{
  private static final int SIGNATURE_FRAGMENTS_LENGTH = 2187, MESSAGE_PACKET_HEADER_LENGTH = 6;
  private static TangleInterface instance = new TangleInterface();
  private final int depth = 4;
  private IotaAPI iotaAPI;
  private final int minWeightMagnitude = 9;

  private TangleInterface()
  {
    iotaAPI = new IotaAPI.Builder().protocol("https").host("nodes.devnet.thetangle.org").port("443").localPoW(new PearlDiverLocalPoW()).build();
  }

  private static String accumulateTransactionMessages(List<Transaction> transactions)
  {
    StringBuilder sb = new StringBuilder();
    for (Transaction transaction : transactions)
    {
      sb.append(transaction.getSignatureFragments().substring(0, SIGNATURE_FRAGMENTS_LENGTH));
    }
    return sb.toString();
  }

  private static String createMessagePacketHeader(int contentLength)
  {
    int[] headerTrits = Converter.fromValue(contentLength);
    int[] paddedHeaderTrits = new int[MESSAGE_PACKET_HEADER_LENGTH * 3];
    for (int i = 0; i < headerTrits.length; i++)
    {
      paddedHeaderTrits[i] = headerTrits[i];
    }
    return Converter.trytes(paddedHeaderTrits);
  }

  /**
   * @param trytes Trytes to trim.
   * @return Trytes after trimming of all '9' trytes at end. Returned string will never end with a '9'.
   */
  private static String cutOffAllNinesAtEnd(String trytes)
  {
    char[] txMsgChars = trytes.toCharArray();
    if (trytes.isEmpty())
    {
      return "";
    }
    int trimPos;
    for (trimPos = trytes.length() - 1; trimPos >= 0 && txMsgChars[trimPos] == '9'; trimPos--)
    {
    }
    return trytes.substring(0, trimPos + 1);
  }

  public static TangleInterface getInstance()
  {
    return instance;
  }

  private static List<Transaction> orderTransactionsByIndex(List<Transaction> unordered)
  {
    Transaction[] ordered = new Transaction[unordered.size()];
    for (Transaction transaction : unordered)
    {
      ordered[(int) transaction.getCurrentIndex()] = transaction;
    }
    return Arrays.asList(ordered);
  }

  private static List<Transfer> partitionMessagePacket(String address, String tag, String messageTrytes)
  {
    List<Transfer> transfers = new LinkedList<>();
    for (int i = 0; i < messageTrytes.length(); i += SIGNATURE_FRAGMENTS_LENGTH)
    {
      transfers.add(new Transfer(address, 0, messageTrytes.substring(i, Math.min(i + SIGNATURE_FRAGMENTS_LENGTH, messageTrytes.length())), tag));
    }
    return transfers;
  }

  private static String randomAddress()
  {
    String trytes = "ABCDEFGHIJKLMNOPQRSTUVWXYZ9";
    char[] address = new char[81];
    for (int i = 0; i < 81; i++)
    {
      address[i] = trytes.charAt((int) (27 * Math.random()));
    }
    return new String(address);
  }

  public static String tritsToTrytes(String tritString)
  {
    int[] trits = new int[tritString.length() + (3 - tritString.length() % 3) % 3];
    for (int i = 0; i < tritString.length(); i++)
    {
      char c = tritString.charAt(i);
      trits[i] = c == '-' ? -1 : c == '1' ? 1 : 0;
    }
    return Converter.trytes(trits);
  }

  public static String trytesToTrits(String trytes)
  {
    int[] trits = Converter.trits(trytes);
    char[] tritChars = new char[trits.length];
    for (int i = 0; i < trits.length; i++)
    {
      tritChars[i] = trits[i] == 1 ? '1' : trits[i] == -1 ? '-' : '0';
    }
    return new String(tritChars);
  }

  /**
   * @param trytes Trytes to unpad.
   * @return Trims of '9' padding at the end of a tryte string. Might leave a single '9' tryte to keep the length even,
   * so that the returned String can be decoded to ASCII.
   */
  static String unpadTrytes(String trytes)
  {
    // hard trim: all 9's at end trimmed off, result will never end with a '9' but might have an odd length
    String hardTrim = cutOffAllNinesAtEnd(trytes);
    // soft trim: ensures length is always even by adding a single '9' tryte if length is odd
    String softTrim = hardTrim.length() % 2 == 1 ? hardTrim + '9' : hardTrim;
    return softTrim;
  }

  /**
   * @param address Address from which transactions shall be fetched.
   * @return Message of fetched transactions interpreted as ASCII.
   * @throws RuntimeException if argument is invalid.
   **/
  public String[] fetchByAddress(String address)
  {
    List<Transaction> transactions;
    try
    {
      transactions = iotaAPI.findTransactionObjectsByAddresses(new String[] { address });
    }
    catch (ArgumentException e)
    {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    return readAsciiTxMsg(transactions);
  }

  /**
   * @param hash Hash of transaction to fetch.
   * @return Message of fetched transaction interpreted as ASCII.
   * @throws RuntimeException if argument is invalid.
   **/
  public String fetchByHash(String hash)
  {
    Transaction transaction;
    try
    {
      transaction = iotaAPI.findTransactionsObjectsByHashes(new String[] { hash }).get(0);
    }
    catch (ArgumentException e)
    {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    return readAsciiTxMsg(transaction);
  }

  public String fetchMessageFromBundle(String bundleHash)
  {
    List<Transaction> orderedBundleTxs = fetchOrderedBundleTransactions(bundleHash);
    String messagePacket = accumulateTransactionMessages(orderedBundleTxs);
    String header = messagePacket.substring(0, MESSAGE_PACKET_HEADER_LENGTH);
    int contentLength = Converter.value(Converter.trits(header));
    return messagePacket.substring(MESSAGE_PACKET_HEADER_LENGTH, MESSAGE_PACKET_HEADER_LENGTH + contentLength);
  }

  private List<Transaction> fetchOrderedBundleTransactions(String bundleHash)
  {
    List<Transaction> orderedBundleTxs;
    try
    {
      String[] txHashes = iotaAPI.findTransactionsByBundles(bundleHash).getHashes();
      List<Transaction> unorderedBundleTxs = iotaAPI.findTransactionsObjectsByHashes(txHashes);
      orderedBundleTxs = orderTransactionsByIndex(unorderedBundleTxs);
    }
    catch (ArgumentException e)
    {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    return orderedBundleTxs;
  }

  public String publish(String message)
  {
    // use random address, otherwise bundles might be published as reattaches  by jota library for whatever reason
    return publish(randomAddress(), "999999999999999999999999999", message);
  }

  /**
   * Publishes a transaction bundle to the Tangle.
   *
   * @param address Address to which the transactions are sent.
   * @param tag     Tag used for the transactions.
   * @param content Tryte message to publish.
   * @return Hahs of the published bundle.
   * @throws RuntimeException if any argument is invalid.
   */
  public String publish(String address, String tag, String content)
  {

    String header = createMessagePacketHeader(content.length());
    String packet = header + content;
    try
    {
      List<Transfer> transfers = partitionMessagePacket(address, tag, packet);
      SendTransferResponse response = iotaAPI.sendTransfer("", 1, depth, minWeightMagnitude, transfers, new LinkedList<>(), "", false, false, null);
      return response.getTransactions().get(0).getBundle();
    }
    catch (ArgumentException e)
    {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  /**
   * Same as {@link #readAsciiTxMsg(Transaction)} but for multiple transactions.
   */
  private String[] readAsciiTxMsg(List<Transaction> transactions)
  {
    String[] extracted = new String[transactions.size()];
    for (int i = 0; i < extracted.length; i++)
    {
      extracted[i] = readAsciiTxMsg(transactions.get(i));
    }
    return extracted;
  }

  /**
   * @param transaction Transaction whose message shall be converted to ASCII.
   * @return Transaction message decoded to ASCII.
   */
  private String readAsciiTxMsg(Transaction transaction)
  {
    String trytes = transaction.getSignatureFragments();
    String trimmedTrytes = unpadTrytes(trytes);
    return TrytesConverter.trytesToAscii(trimmedTrytes);
  }
}
