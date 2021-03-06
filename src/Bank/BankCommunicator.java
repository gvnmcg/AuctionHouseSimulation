package Bank;

import BankProxy.BankInfo;
import BankProxy.BankRequest;
import Network.NetworkDevice;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Dedicated thread for a socket connection
 */
public class BankCommunicator implements Runnable {

    private Socket s;
    private Bank bank;
    private ObjectInputStream is;
    private ObjectOutputStream os;

    /**
     * Thread for communication with a single socket
     *
     * @param s Socket for communication
     * @param bank AuctionHouse reference
     */
    BankCommunicator(Socket s, Bank bank) {

        // Create sockets that are passed
        this.s = s;
        this.bank = bank;

        // Create new ObjectDataStreams
        try {
            is = new ObjectInputStream(s.getInputStream());
            os = new ObjectOutputStream(s.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Start new thread for processing messages
        new Thread(this).start();
    }

    /**
     * Runs thread and processes messages
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        System.out.println("Starting thread for " + s.getInetAddress() +
                " on thread: " + Thread.currentThread().getName());

        // Check for connection and aliveness
        while(s.isConnected() && bank.isAlive()) {
            try {
                // Process BankRequest from input stream
                BankRequest br = (BankRequest)is.readObject();

                // Throw error if br is not processable
                if (br == null) throw new ClassNotFoundException();

                // Process messages
                processMessage(br);
            } catch (IOException | ClassNotFoundException e) {
                try {
                    // Attempt to close the socket
                    System.out.println("Closing " + s);
                    s.close();
                } catch (IOException e1) {

                    e1.printStackTrace();
                    return;
                }
                break;
            }
        }

        System.out.println("Connection broke for " + s.getInetAddress());
    }

    /**
     * Process the AuctionRequest and the respond appropriately
     *
     * @param br AuctionRequest to handle
     */
    private void processMessage(BankRequest br) {
        System.out.println("THE TYPE OF REQUEST IS: " + br.getType());

        // Create a response for br
        BankRequest response = new BankRequest(br.getType(), br.getPacketID());

        // Check type and map appropriate actions
        switch (br.getType()) {
            case NEWACCOUNT: // Create a new account
                int accountID = bank.addAccount();

                response.setID(accountID);

                System.out.println("\tNew Account Created: " + accountID);
                break;
            case GETBALANCE: // Get the balance of the given account
                double balance = bank.getBalance(br.getID());
                response.setAmount(balance);

                System.out.println("\tBalance for Account#: " + br.getID() +
                        " is $" + balance);
                break;
            case GETTOTALBALANCE:
                double total = bank.getTotalBalance(br.getID());
                response.setAmount(total);

                System.out.println("\tTotal balance for Account#: " +
                        br.getID() + " is $" + total);
                break;
            case ADD: // AddFunds to a given Account
                double bal1 = bank.getBalance(br.getID());
                response.setStatus(bank.addFunds(br.getID(), br.getAmount()));
                double bal2 = bank.getBalance(br.getID());

                System.out.println("\tAdded funds to Account#: " + br.getID() +
                        " | Amount Added: $" + br.getAmount() + " | Old Bala" +
                        "nce: $" + bal1 + " | New Balance: $" + bal2);
                break;
            case REMOVE: // RemoveFunds from given account
                double bal3 = bank.getBalance(br.getID());
                response.setStatus(bank.removeFunds(br.getID(),br.getAmount()));
                double bal4 = bank.getBalance(br.getID());

                System.out.println("\tAdded funds to Account#: " + br.getID() +
                        " | Amount Added: $" + br.getAmount() + " | Old Bala" +
                        "nce: $" + bal3 + " | New Balance: $" + bal4);
                break;
            case LOCK: // Lock funds of the given account
                int lockNumber = bank.lockFunds(br.getID(), br.getAmount());
                response.setLockNumber(lockNumber);

                System.out.println("\tLocked funds to Account#: " + br.getID() +
                        " | Amount locked: $" + br.getAmount() + " | Lock Num" +
                        "ber: $" + lockNumber);
                break;
            case UNLOCK: // Unlock funds of the given account
                response.setStatus(bank.unlockFunds(br.getID(),
                        br.getLockNumber()));

                System.out.println("\tUnlocked funds for Account#: " +
                        br.getID());
                break;
            case TRANSFER: // Transfer funds from account1 to account2
                response.setStatus(bank.transferFunds(br.getID(), br.getToID(),
                        br.getAmount()));

                System.out.println("\tTransferred $" + br.getAmount() + " fro" +
                        "m Account#: " + br.getID() + " to Account#: " +
                        br.getToID());
                break;
            case TRANSFERFROMLOCK: // Transfer funds based on a lock
                response.setStatus(bank.transferFunds(br.getID(), br.getToID(),
                        br.getLockNumber()));

                System.out.println("\tTransferred $" + br.getLockNumber() +
                        " from Account#: " + br.getID() + " to Account#: " +
                        br.getToID());
                break;
            case OPENAUCTION:
                response.setStatus(true);
                bank.openServer(br.getNetworkDevice());

                bank.notifyAuction(br.getNetworkDevice());

                System.out.println("\tNew Server on " + br.getNetworkDevice());
                break;
            case CLOSEAUCTION:
                response.setStatus(true);
                bank.closeServer(br.getNetworkDevice());

                System.out.println("\tStopped distributing the server of " +
                        br.getNetworkDevice());
                break;
            case GETAUCTIONS:
                LinkedBlockingQueue<NetworkDevice> auctions = bank.getServers();

                System.out.print("\tSending the following servers: \n\t");
                for (NetworkDevice nd : auctions) {
                    System.out.print(nd + " ");
                }
                System.out.println();

                response.setNetworkDevices(auctions);
                break;
        }

        try {
            // Write a response back
            os.writeObject(response);
            System.out.println("\tSent Message!\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * notifies of a new auction
     *
     * @param networkDevice network device of new auction
     */
    public void notifyNewAuction(NetworkDevice networkDevice) {
        BankRequest ar = new BankRequest(BankInfo.OPENAUCTION);
        ar.setAck(false);
        ar.addNetworkDevices(networkDevice);

        try {
            os.writeObject(ar);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
