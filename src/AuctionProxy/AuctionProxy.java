package AuctionProxy;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import Agent.AgentApp;
import AuctionHouse.Bid;
import AuctionHouse.ItemInfo;

/**
 * Auction Proxy used for communication
 * in the network by the Auction house
 */
public class AuctionProxy implements AuctionProcess, Runnable {

    private ConcurrentHashMap<Integer, AuctionRequest> messages =
            new ConcurrentHashMap<Integer, AuctionRequest>();
    private ObjectInputStream is = null;
    private ObjectOutputStream os = null;
    private Socket s;
    private boolean open;
    private String hostname;
    private int port;
    private ArrayList<Bid> bids;
    private AgentApp agentApp;

    /**
     * Proxy design for the Auction House. Creates a socket from the
     * passed parameters
     *
     * @param hostname host name
     * @param port port number
     */
    public AuctionProxy(String hostname, int port, AgentApp agentApp) {
        this.agentApp = agentApp;
        open = true;

        this.hostname = hostname;
        this.port = port;

        connectToServer(hostname, port);

        new Thread(this).start();

        bids = new ArrayList<>();
    }

    /**
     * Used for connecting to a server
     *
     * @param hostname host name
     * @param port (int) port number
     */
    private void connectToServer(String hostname, int port) {
        try {
            s = new Socket(hostname, port);

            os = new ObjectOutputStream(s.getOutputStream());

            is = new ObjectInputStream(s.getInputStream());

        } catch (IOException e) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            connectToServer(hostname, port);
            e.printStackTrace();
        }
    }

    /**
     * To place a bid
     *
     * @param bid Bid object that contains elements
     */
    @Override
    public BidInfo bid(Bid bid) {

        // Auction request
        AuctionRequest ar = new AuctionRequest(AuctionInfo.BID);
        ar.setBid(bid);
        bids.add(bid);

        try {
            // Write the object and wait upon the return
            os.writeObject(ar);
            waitOn(ar.getPacketID());

            AuctionRequest response = messages.get(ar.getPacketID());
            messages.remove(ar.getPacketID());
            return response.getBidStatus();
        } catch (IOException e) {
            e.printStackTrace();
        }
        bids.remove(bid);
        return BidInfo.REJECTION;
    }

    /**
     * Gets the Item Info from the Item ID.
     *
     * @param itemID Identifier of Item
     * @return ItemInfo
     */
    @Override
    public ItemInfo getItemInfo(int itemID) {
        AuctionRequest ar = new AuctionRequest(AuctionInfo.GET);
        ar.setItemID(itemID);

        try {
            // Write the object and wait upon the return
            os.writeObject(ar);
            waitOn(ar.getPacketID());

            AuctionRequest response = messages.get(ar.getPacketID());
            messages.remove(ar.getPacketID());

            return response.getItem();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Gets an ArrayList of all of the Items
     *
     * @return ArrayList of Items
     */
    @Override
    public ArrayList<ItemInfo> getItems() {
        AuctionRequest ar = new AuctionRequest(AuctionInfo.GETALL);
        ar.setItems(null);

        try {
            // Write the object and wait upon the return
            os.writeObject(ar);
            waitOn(ar.getPacketID());

            AuctionRequest response = messages.get(ar.getPacketID());
            messages.remove(ar.getPacketID());
            for (ItemInfo items : response.getItems()) {
                items.setProxy(this);
            }
            return response.getItems();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


    /**
     * Check to see if the close is allowed
     *
     * @param accountID Account ID to be used for checking
     * @return True if no active bids, false if active bids
     */
    @Override
    public boolean closeRequest(int accountID) {
        AuctionRequest ar = new AuctionRequest(AuctionInfo.CLOSEREQUEST);
        ar.setItemID(accountID);

        try {
            // Write the object and wait upon the return
            os.writeObject(ar);
            waitOn(ar.getPacketID());

            AuctionRequest response = messages.get(ar.getPacketID());
            messages.remove(response.getPacketID());
            return response.isContains();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Run that reads in objects and then either returns the response or
     * processes the notification
     */
    @Override
    public void run() {
        while (isOpen()) {
            // Attempt to read in an AR from the input stream
            AuctionRequest newAr;
            try {
                newAr = (AuctionRequest) is.readObject();

                // Either notify or process immediately
                if (newAr.getAck()) {
                    messages.put(newAr.getPacketID(), newAr);
                    synchronized (this) { notify(); }
                } else {
                    processMessage(newAr);
                }


            } catch (IOException | ClassNotFoundException e) {
                System.out.println("An agent has left the auction house");
                open = false;
                //e.printStackTrace();
                return;
            }


        }
    }

    /**
     * Processes messages received about actions
     * Auction should take. This takes care
     * of notifying whether an Agent has been
     * outbid on an item or won it.
     *
     * @param newAr an AuctionRequest
     */
    private void processMessage(AuctionRequest newAr) {
        switch(newAr.getType()) {
            case BID:
                switch (newAr.getBidStatus()) {
                    case OUTBID:
                        ItemInfo item = newAr.getItem();
                        double amount = newAr.getNewAmount();

                        agentApp.newPopUp("You were outbid on " +
                                item.getName()+ ". The new bid is " + amount);
                        break;
                    case WINNER:
                        ItemInfo itemWon = newAr.getItem();
                        double amountPaid = newAr.getNewAmount();

                        agentApp.newPopUp("You won " + itemWon.getName() +
                                ". There was $" + amountPaid + " transferred " +
                                "from your bank account. Please allow 6-8 wee" +
                                "ks in delivery for your item to arrive");

                        break;
                }
                break;
        }
    }

    /**
     * Gets the connection address of the socket
     *
     * @return Address of socket
     */
    public InetAddress getConnectedAddress() {
        return s.getInetAddress();
    }

    /**
     * Returns the port of the socket
     *
     * @return Port of the socket
     */
    public int getPort() {
        return port;
    }

    /**
     * @return Whether the proxy is open
     */
    private boolean isOpen() {
        return open;
    }

    /**
     * Wait on a given packetID
     *
     * @param packetID Wait until messages contains key
     */
    private void waitOn(int packetID) {
        synchronized (this) {
            while (!messages.containsKey(packetID)) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


}
