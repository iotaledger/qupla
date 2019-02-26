package org.iota.qupla.dispatcher.entity.tangle;

public class TangleAddressListener extends ZMQListener
{
  private static final int TIMEOUT_TOLERANCE = 60000;

  public TangleAddressListener(String zmqAddress)
  {
    super(zmqAddress, TIMEOUT_TOLERANCE);
  }

  public static void main(String[] args)
  {
    TangleAddressListener listener = new TangleAddressListener("tcp://nodes.devnet.iota.org:5556");
    listener.subscribe("999999999999999999999999999999999999999999999999999999999999999999999999999999999");
    listener.start();
  }

  private void processTxMessage(String txAddress, String txMessage)
  {
  }

  @Override
  public void processZMQMessage(String zmqMessage)
  {
    System.out.println(zmqMessage);
    String[] messageFragments = zmqMessage.split(" ");
    String address = messageFragments[0];
    String transactionHash = messageFragments[1];
    String txMessage = TangleInterface.getInstance().fetchByHash(transactionHash);
    if (txMessage != null)
    {
      processTxMessage(address, txMessage);
    }
  }
}
