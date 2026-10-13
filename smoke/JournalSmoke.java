package com.getradejournal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

public class JournalSmoke
{
    private static OfferState offer(int slot, int item, int price, int qty, int filled, long gp, String state)
    { return new OfferState(slot,item,price,qty,filled,gp,state); }
    private static void check(boolean ok,String msg) { if(!ok) throw new AssertionError(msg); }
    public static void main(String[] args) throws Exception
    {
        Path dir=Files.createTempDirectory("ge-journal-");
        LocalJournal disk=new TestJournalStorage(dir);
        TradeJournal journal=new TradeJournal(disk,"Test account");
        OfferState[] baseline=new OfferState[8];
        for(int i=0;i<8;i++) baseline[i]=offer(i,0,0,0,0,0,"EMPTY");
        baseline[1]=offer(1,100,50,10,4,200,"BUYING");
        String[] names=new String[8];names[1]="Test item";
        journal.bootstrap(baseline,names);
        journal.observe(offer(1,100,50,10,6,300,"BUYING"),"Test item");
        journal.observe(offer(1,100,50,10,10,500,"BOUGHT"),"Test item");
        journal.observe(offer(1,100,50,10,10,500,"BOUGHT"),"Test item");
        journal.observe(offer(1,0,0,0,0,0,"EMPTY"),"");
        journal.observe(offer(1,100,50,10,0,0,"BUYING"),"Test item");
        journal.observe(offer(1,100,50,10,5,250,"BUYING"),"Test item");
        String s=Files.readString(dir.resolve("events.jsonl"),StandardCharsets.UTF_8);
        String current=Files.readString(dir.resolve("current_offers.json"),StandardCharsets.UTF_8);
        check(s.contains("OFFER_OBSERVED"),"initial baseline missing");
        check(s.contains("\"delta_quantity\":2"),"partial fill delta wrong");
        check(s.contains("\"delta_quantity\":4"),"completion fill delta wrong");
        check(s.contains("OFFER_COMPLETED"),"completion event missing");
        check(s.contains("OFFER_CLEARED"),"clear event missing");
        check(s.split("OFFER_COMPLETED",-1).length==2,"repeated terminal event created duplicate order");
        check(s.contains("\"delta_quantity\":5"),"new order fill missing");
        check(current.contains("\"filled_quantity\":5"),"snapshot wrong");
        check(!s.contains("flip-familiar") && !s.contains("http"),"bot-specific data");
        System.out.println("GE Trade Journal local-only smoke PASSED");
    }
}
