package AuctionHouse;

import AuctionProxy.AuctionProcess;
import AuctionProxy.BidInfo;
import BankProxy.BankProxy;
import Network.NetworkDevice;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import static Agent.AgentApp.auctionPort;
import static Agent.AgentApp.bankPort;

public class AuctionHouse implements AuctionProcess {

    // User can set this in the parameters
    public static long waitTime = 50000;

    private ConcurrentHashMap<Integer, Item> items =
            new ConcurrentHashMap<Integer, Item>();
    private ArrayList<Item> itemsNotUpForAuction = new ArrayList<Item>();
    private LinkedBlockingQueue<ItemInfo> itemInfos =
            new LinkedBlockingQueue<ItemInfo>();
    private LinkedBlockingQueue<Bid> bids = new LinkedBlockingQueue<>();
    private BankProxy bankProxy;
    private int auctionID = 0;
    private static int counter = 0;
    private boolean alive;

    /**
     * Constructor initializing an AuctionHouse
     *
     * @param operatingPort Port of auctionhouse
     * @param bankHostname Bank's hostname
     * @param bankPort Bank's port
     */
    public AuctionHouse(int operatingPort, String bankHostname, int bankPort) {
        alive = true;
        bankProxy = new BankProxy(bankHostname, bankPort, null);

        // Make a bank account for the auction house
        bankProxy.addAccount();

        // Read items this house will sell
        readInItems();

        ServerSocket ss = null;
        try {
            ss = new ServerSocket(operatingPort);
        } catch (IOException e) {
            e.printStackTrace();
        }

        bankProxy.openServer(
                new NetworkDevice("127.0.0.1",operatingPort));

        while (true) {
            try {
                Socket s = ss.accept();
                AuctionCommunicator ac =
                        new AuctionCommunicator(s,this);

            } catch (IOException e) {
                System.out.println("Socket failed.");
            }

        }

    }

    /**
     * Read in the items from test file.
     * File should be placed in 'resources' directory
     * Saves 3 items to list of currently auctioned items
     * and saves the rest to a list to be used later.
     */
    private void readInItems() {
        try {
            BufferedReader br = new BufferedReader(
                    new FileReader("resources/items.txt"));

            String line;
            String[] lineArr;

            int itemNum = 0;

            // Read in the items
            while ((line = br.readLine()) != null){

                lineArr = line.split(" ");

                ItemInfo itemInfo = new ItemInfo(lineArr[0],
                        Integer.parseInt(lineArr[1]), counter++);
                Item item = new Item(bankProxy, this,itemInfo,auctionID);

                // Start the first three item threads
                if(itemNum < 3){
                    itemInfos.add(itemInfo);
                    System.out.println(itemInfo);
                    items.put(item.getItemID(), item);
                    item.startThread();
                    itemNum++;
                }
                else {
                    itemsNotUpForAuction.add(item);
                }

            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Remove an item when done
     *
     * @param itemID Item ID
     */
    public synchronized void removeItem(int itemID) {
        if (items.containsKey(itemID)) {

            itemInfos.remove(items.get(itemID).getItemInfo());
            items.remove(itemID);

            // Add a new item to replace it
            if(!itemsNotUpForAuction.isEmpty()) {

                Item itemUp = itemsNotUpForAuction.remove(0);
                ItemInfo itemInfo = itemUp.getItemInfo();
                itemInfos.add(itemInfo);
                items.put(itemUp.getItemID(), itemUp);
                itemUp.startThread();

            }
            else {
                System.out.println("Auction House says: No more items in the " +
                        "Auction house!");
            }
        }
    }


    /**
     * To place a bid, with the completed bid object
     *
     * @param bid    Bid object that contains elements
     */
    @Override
    public BidInfo bid(Bid bid) {
        // Sanity check
        if (bid == null || !items.containsKey(bid.getItemID())) {
            return BidInfo.REJECTION;
        }

        // Get the item to be bid upon
        Item item = items.get(bid.getItemID());
        BidInfo info = item.setBid(bid);
        addBid(bid);

        return info;
    }

    /**
     * Add the bid
     *
     * @param bid bid to synchronously add
     */
    private void addBid(Bid bid){
        bids.add(bid);
    }

    /**
     * Gets the Item Info from the Item ID.
     *
     * @param itemID Identifier of Item
     * @return ItemInfo
     */
    @Override
    public ItemInfo getItemInfo(int itemID) {
        if (items.get(itemID) != null) {
            return items.get(itemID).getItemInfo();
        }
        else {
            return null;
        }
    }

    /**
     * Gets an ArrayList of all of the Items
     *
     * @return ArrayList of Items
     */
    @Override
    public ArrayList<ItemInfo> getItems() {
        ArrayList<ItemInfo> items = new ArrayList<ItemInfo>();
        for (ItemInfo item : this.itemInfos) {
            items.add((ItemInfo) item.clone());
        }
        return items;


    }

    /**
     * Check to see if the close is allowed
     *
     * @param accountID Account ID to be used for checking
     * @return True if no active bids, false if active bids
     */
    @Override
    public boolean closeRequest(int accountID) {
        for (Item item : items.values()) {
            if (item.contains(accountID)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return state of auction house
     */
    public boolean isAlive() {
        return alive;
    }

    /**
     * Starts a new Auction House
     * @param args Args to take
     */
    public static void main(String[] args) {

        if (args.length == 4) {

            int operatingPort;
            int bankPort;

            try {
                operatingPort = Integer.parseInt(args[0]);
                bankPort = Integer.parseInt(args[2]);
                waitTime = Long.parseLong(args[3]);

            } catch (NumberFormatException e) {
                System.out.println("Input not correct:\n Correct usage: Auct" +
                        "ionHouse <Operating Port> <Bank Host> <Bank Port> <" +
                        "Wait Time>");
                return;
            }

            AuctionHouse ah = new AuctionHouse(operatingPort,args[1],bankPort);

        } else {
            AuctionHouse ah = new AuctionHouse(auctionPort, "localhost",
                    bankPort);
        }

    }
}
