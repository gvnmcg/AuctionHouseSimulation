package Bank;

import BankProxy.BankProcess;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class Bank implements BankProcess {

    //UniqueID Counter
    public static int counter = 0;
    private HashMap<Integer, Account> accounts = new HashMap<Integer, Account>();
    private HashMap<Integer, Double> lockedMoney = new HashMap<Integer, Double>();


    public Bank(int port) {

        ServerSocket ss = null;
        try {
            ss = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true) {
            try {
                Socket s = ss.accept();
                BankCommunicator ac = new BankCommunicator(s,this);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public boolean addAccount(int AccountID) {

        if (accounts.containsKey(AccountID)) return false;
        counter++;
        Account newAccount = new Account();
        accounts.put(AccountID, newAccount);
        return true;
    }


    // TODO implement
    public boolean isAlive() {
        return true;
    }

    /**
     * Get the balance of the Account number
     *
     * @param AccountID Unique Identifier of Account
     * @return Amount of money
     */
    @Override
    public double getBalance(int AccountID) {
        System.out.println("Getting the balance for Account#: " + AccountID);
        Account account = accounts.get(AccountID);
        return account.getBalance();
    }

    /**
     * Add the funds to the specified account number
     *
     * @param AccountID Unique Identifier of Account
     * @param amount    Amount of Money
     */
    @Override
    public boolean addFunds(int AccountID, double amount) {
        System.out.println("Added $" + amount + " to the Account #: " + AccountID);
        Account account = accounts.get(AccountID);
        account.addFunds(amount);
        return true;
    }

    /**
     * Removes the funds from the given account
     *
     * @param AccountID Unique Identifier of Account
     * @param amount    Amount of Money
     */
    @Override
    public boolean removeFunds(int AccountID, double amount) {
        System.out.println("Removed $" + amount + " to the Account #: " + AccountID);
        Account account = accounts.get(AccountID);
        account.removeFunds(amount);
        return true;
    }

    /**
     * Locks a certain amount of money away for potential use, returns an
     * integer value that can be used for later use.
     *
     *
     * @param AccountID Unique Identifier of Account
     * @param amount    Amount of Money
     * @return Random/Unique Integer of lock, for later retrieval
     */
    @Override
    public int lockFunds(int AccountID, double amount) {
        System.out.println("Locked $" + amount + " from the Account #: " + AccountID);
        Account account = accounts.get(AccountID);
        int lockIDReturned = account.lockFunds(amount);
        lockedMoney.put(lockIDReturned, amount);
        return lockIDReturned;
    }

    /**
     * Unlocks the lock given by the identifier!
     *  @param AccountID Unique Identifier of Account
     * @param lockID    Identifier of the lock
     */
    @Override
    public boolean unlockFunds(int AccountID, int lockID) {
        System.out.println("Unlocked LockID# " + lockID + " to the Account #: " + AccountID);
        Account account = accounts.get(AccountID);
        return account.unlockFunds(lockID);
    }

    /**
     * Transfer funds of amount specified from ID1 to ID2
     *  @param fromID Unique Identifier of Account1
     * @param toID   Unique Identifier of Account2
     * @param amount Amount of Money
     */
    @Override
    public synchronized boolean transferFunds(int fromID, int toID, double amount) {
        System.out.println("Transfered $" + amount + " from Account#: " + fromID + " to Account#: " + toID);
        Account account1 = accounts.get(fromID);
        Account account2 = accounts.get(toID);
        account1.removeFunds(amount);
        account2.addFunds(amount);
        return true;
    }

    /**
     * Transfer funds based on the lock within the account tied to the fromID
     *  @param fromID Unique Identifier of Account1
     * @param toID   Unique Identifier of Account2
     * @param lockID Lock identifier
     */
    @Override
    public boolean transferFunds(int fromID, int toID, int lockID) {

        return false;
    }
}

