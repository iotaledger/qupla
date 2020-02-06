package org.iota.qupla.dispatcher.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import jota.IotaAPI;
import jota.dto.response.SendTransferResponse;
import jota.error.ArgumentException;
import jota.model.Transaction;
import jota.model.Transfer;
import jota.pow.pearldiver.PearlDiverLocalPoW;
import org.iota.qupla.Qupla;
import org.iota.qupla.dispatcher.Dispatcher;
import org.iota.qupla.dispatcher.Entity;
import org.iota.qupla.dispatcher.Environment;
import org.iota.qupla.dispatcher.entity.tangle.ZMQListener;
import org.iota.qupla.helper.TritVector;

public class TangleEntity extends Entity
{
  private static final int ADDRESS_OFFSET = 243 + 81 * 81 + 243;
  private static final int HASH_SIZE = 243;
  private static final int MAP_OFFSET = 243;
  private static final int MAP_SIZE = 81 * 81;
  private static final int MESSAGE_SIZE = 81 * 81 + 243;
  private static final int SIGNATURE_FRAGMENTS_LENGTH = 2187;
  private static final int SIGNATURE_OFFSET = 243 + 81 * 81;
  private static final int depth = 4;
  private static final int minWeightMagnitude = 9;
  public final HashSet<String> addresses = new HashSet<>();
  public Environment golAddress;
  private Entity golAddressEntity;
  public Environment golMessage;
  public Environment golStore;
  public final HashMap<TritVector, TritVector> idMap = new HashMap<>();
  private IotaAPI iotaAPI;
  public ZMQListener listener;

  public TangleEntity()
  {
    super(0);

    iotaAPI = new IotaAPI.Builder().protocol("https").host("nodes.devnet.thetangle.org").port("443").localPoW(new PearlDiverLocalPoW()).build();

    final Dispatcher dispatcher = Dispatcher.getInstance();
    golAddress = dispatcher.getEnvironment("GolAddress", null);
    golMessage = dispatcher.getEnvironment("GolMessage", null);
    golStore = dispatcher.getEnvironment("GolStore", null);
    join(golMessage);

    golAddressEntity = new Entity(0)
    {
      @Override
      public TritVector onEffect(final TritVector effect)
      {
        return onGolAddress(effect);
      }
    };
    golAddressEntity.join(golAddress);

    listener = new ZMQListener("tcp://nodes.devnet.iota.org:5556", 60000)
    {
      @Override
      public void processZMQMessage(final String zmqMessage)
      {
        process(zmqMessage);
      }
    };

    listener.start();
  }

  @Override
  public TritVector onEffect(final TritVector effect)
  {
    return onGolMessage(effect);
  }

  private TritVector onGolAddress(final TritVector effect)
  {
    // cmd/id/address
    final TritVector cmd = effect.slice(0, 1);
    final TritVector id = effect.slice(1, HASH_SIZE);
    final TritVector address = effect.slice(1 + HASH_SIZE, HASH_SIZE);

    switch (cmd.trit(0))
    {
    case '-':
      listener.unsubscribe(address.toTrytes());
      break;

    case '1':
      listener.subscribe(address.toTrytes());
      onSubscribe(address, id);
      break;
    }

    return null;
  }

  private TritVector onGolMessage(final TritVector effect)
  {
    // id/map/signature/address/cmd
    final TritVector id = effect.slice(0, HASH_SIZE);
    if (id.isZero())
    {
      return null;
    }

    //    final TritVector map = effect.slice(MAP_OFFSET, MAP_SIZE);
    //    final TritVector signature = effect.slice(SIGNATURE_OFFSET, HASH_SIZE);
    final TritVector address = effect.slice(ADDRESS_OFFSET, HASH_SIZE);

    // anything beyond address is expected to be the data
    final String tag = "999QUPLA99GAME99OF99LIFE999";
    final String data = effect.slice(MAP_OFFSET, MESSAGE_SIZE).toTrytes();
    final String bundle = publish(address.toTrytes(), tag, data);

    return null;
  }

  private void onSubscribe(final TritVector address, final TritVector id)
  {
    idMap.put(address, id);

    try
    {
      // we just got informed that this address is interesting to us
      final List<Transaction> transactions = iotaAPI.findTransactionObjectsByAddresses(new String[] { address.toTrytes() });
      if (transactions.size() != 0)
      {
        int breakPoint = 0;
      }
    }
    catch (ArgumentException e)
    {
      e.printStackTrace();
    }
  }

  private void process(final String zmqMessage)
  {
    Qupla.log(zmqMessage);

    try
    {
      final String[] messageFragments = zmqMessage.split(" ");
      String addressTrytes = messageFragments[1];
      String txHashTrytes = messageFragments[2];
      final List<Transaction> transactions = iotaAPI.findTransactionsObjectsByHashes(new String[] { txHashTrytes });
      if (transactions.size() < 2)
      {
        return;
      }

      processTransaction(addressTrytes, transactions);
    }
    catch (final ArgumentException e)
    {
      Qupla.log(zmqMessage);
      e.printStackTrace();
    }
  }

  private void processTransaction(final String addressTrytes, final List<Transaction> transactions)
  {
    final Transaction tx0 = transactions.get(0);
    final Transaction tx1 = transactions.get(1);

    final TritVector id = new TritVector(HASH_SIZE, '0');
    final TritVector map = TritVector.fromTrytes(tx0.getSignatureFragments());
    final TritVector signature = TritVector.fromTrytes(tx1.getSignatureFragments().substring(0, 81));
    final TritVector address = TritVector.fromTrytes(addressTrytes);
    final TritVector cmd = new TritVector(1, '0');
    final TritVector message = TritVector.concat(TritVector.concat(TritVector.concat(TritVector.concat(id, map), signature), address), cmd);
    golStore.affect(message, 0);
  }

  public String publish(final String address, final String tag, final String content)
  {
    try
    {
      final ArrayList<Transfer> transfers = new ArrayList<>();
      transfers.add(new Transfer(address, 0, content.substring(0, SIGNATURE_FRAGMENTS_LENGTH), tag));
      transfers.add(new Transfer(address, 0, content.substring(SIGNATURE_FRAGMENTS_LENGTH, SIGNATURE_FRAGMENTS_LENGTH + 81), tag));
      final SendTransferResponse response = iotaAPI.sendTransfer("", 1, depth, minWeightMagnitude, transfers, new LinkedList<>(), "", false, false, null);
      return response.getTransactions().get(0).getBundle();
    }
    catch (final ArgumentException e)
    {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  @Override
  public void stop()
  {
    listener.terminate();
  }
}
