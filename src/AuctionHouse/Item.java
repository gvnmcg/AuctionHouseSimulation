package AuctionHouse;

import Bank.Bank;
import BankProxy.BankProxy;
import SourcesToOrganize.Bid;

public class Item implements Runnable {

    private BankProxy bank;
//    private Bank bank;
    private Bid bid;
    private int auctionID;
    private ItemInfo itemInfo;
    private int itemID;

    public Item(BankProxy bank, ItemInfo itemInfo, int auctionID) {
        this.bank = bank;
        this.itemInfo = itemInfo;
        this.itemID = itemInfo.getItemID();
        this.auctionID = auctionID;


    }

    /**
     * Checks and then sets the bid.
     *
     * Synchronized because it can be called twice at the same time!
     * TODO Maybe synchronize on bid?
     *
     * @param bid
     * @return
     */
    public synchronized boolean setBid(Bid bid) {
        if (System.currentTimeMillis() > itemInfo.getTime()) {
            System.out.println("Auction is over!");
            return false;
        }

        if (bid.getAmount() > itemInfo.getPrice()) {
            System.out.println(bank + " " +
                    bid.getAccountNumber());
            int lockID = bank.lockFunds(bid.getAccountNumber(), bid.getAmount());

            if (lockID == -1) {
                System.out.println("Not enough funds");
                return false;
            }

            if (this.bid != null) {
                bank.unlockFunds(this.bid.getAccountNumber(), this.bid.getLockID());
            }

            synchronized (itemInfo) { itemInfo.setPrice(bid.getAmount()); }

            this.bid = bid;

            return true;
        } else {
            System.out.println("Bid is too low");
            return false;
        }
    }


    public int getItemID() {
        return itemID;
    }

    public ItemInfo getItemInfo() {
        return itemInfo;
    }

    @Override
    public String toString() {
        return itemInfo.toString();
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        // Calculate time plus offset due to imprecision of wait
        Long timeLeft = itemInfo.getTime() - System.currentTimeMillis() + 100;

        if (timeLeft <= 0) {
            try {
                wait(timeLeft);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (System.currentTimeMillis() > itemInfo.getTime()) {
            endAuction();
        } else {
            run();
        }
    }

    private void endAuction() {

        bank.transferFunds(bid.getAccountNumber(), auctionID, bid.getLockID());


    }


}
