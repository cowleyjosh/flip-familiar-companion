package com.getradejournal;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Dependency-free deterministic GE event lifecycle tests. */
public final class JournalRegression
{
    private static int assertions = 0;
    private static OfferState o(int slot, int item, long price, int qty, int fill, long gp, String state)
    { return new OfferState(slot, item, price, qty, fill, gp, state); }

    private static void check(boolean result, String message)
    {
        assertions++;
        if (!result) throw new AssertionError(message);
    }

    private static final class Run
    {
        final Path folder;
        final TradeJournal trade;
        Run() throws Exception
        {
            folder = Files.createTempDirectory(
                Path.of(System.getenv("GE_JOURNAL_TEST_ROOT")), "ge-regression-");
            trade = new TradeJournal(new TestJournalStorage(folder), "Account\"A");
        }
        void baseline(OfferState... initial) throws Exception
        {
            OfferState[] offers = new OfferState[8];
            for (int i = 0; i < 8; i++) offers[i] = o(i,0,0,0,0,0,"EMPTY");
            String[] names = new String[8];
            for (OfferState state : initial) { offers[state.slot] = state; names[state.slot] = "Rune \"Test\""; }
            trade.bootstrap(offers,names);
        }
        List<String> lines() throws Exception
        {
            Path p = folder.resolve("events.jsonl");
            return Files.exists(p) ? Files.readAllLines(p, StandardCharsets.UTF_8) : new ArrayList<>();
        }
        long count(String event) throws Exception
        { return lines().stream().filter(s -> s.contains("\"event\":\""+event+"\"")).count(); }
        String current() throws Exception
        { return Files.readString(folder.resolve("current_offers.json")); }
    }

    public static void main(String[] args) throws Exception
    {
        // Existing offers are baselined and prior fills are never invented.
        Run r = new Run();
        r.baseline(o(0, 23, 100, 40, 10, 1000, "BUYING"));
        check(r.count("OFFER_OBSERVED") == 1, "baseline should be an observed offer");
        check(r.count("BUY_FILL") == 0, "baseline must not invent fills");
        check(r.current().contains("\"filled_quantity\":10"), "baseline snapshot should include prior fills");
        check(r.current().contains("Account\\\"A"), "account name must be JSON escaped");
        r.trade.observe(o(0, 23, 100, 40, 25, 2500, "BUYING"), "Rune");
        r.trade.observe(o(0, 23, 100, 40, 40, 4000, "BOUGHT"), "Rune");
        r.trade.observe(o(0, 23, 100, 40, 40, 4000, "BOUGHT"), "Rune");
        check(r.count("BUY_FILL") == 2, "two cumulative fill changes must produce two deltas");
        check(r.count("OFFER_COMPLETED") == 1, "repeated completed snapshot must be idempotent");
        check(r.lines().stream().filter(s -> s.contains("BUY_FILL") && s.contains("\"delta_quantity\":15")).count()==2,
            "fill deltas should reflect changes, not cumulative totals");
        r.trade.observe(o(0,0,0,0,0,0,"EMPTY"), "");
        r.trade.observe(o(0,23,100,40,0,0,"BUYING"),"Rune");
        r.trade.observe(o(0,23,100,40,10,1000,"BUYING"),"Rune");
        check(r.count("OFFER_CLEARED") == 1, "clearing slot must be observed once");
        check(r.count("OFFER_STARTED") == 1, "same-item/price slot reuse must start a new order");
        check(r.count("BUY_FILL") == 3, "new order must generate independent fills");

        // A partial sell which is cancelled should not be mistaken for completed.
        Run sell = new Run();
        sell.baseline();
        sell.trade.observe(o(7,30,99,100,0,0,"SELLING"),"Test");
        sell.trade.observe(o(7,30,99,100,30,2970,"SELLING"),"Test");
        sell.trade.observe(o(7,30,99,100,30,2970,"CANCELLED_SELL"),"Test");
        sell.trade.observe(o(7,30,99,100,30,2970,"CANCELLED_SELL"),"Test");
        check(sell.count("SELL_FILL") == 1, "cancelled partial sell must have one fill");
        check(sell.count("OFFER_CANCELLED") == 1, "cancel must happen only once");
        check(sell.count("OFFER_COMPLETED") == 0, "cancelled partial sell is not completed");
        check(sell.current().contains("\"slot\":7"), "eighth GE slot must be recorded");

        // Repricing is a new order identity and never retroactively inferred as a fill.
        Run price = new Run();
        price.baseline();
        price.trade.observe(o(1,88,100,20,5,500,"BUYING"),"Item");
        price.trade.observe(o(1,88,101,20,0,0,"BUYING"),"Item");
        check(price.count("ORDER_REPLACED_OR_CHANGED") == 1, "reprice should identify old offer as replaced");
        check(price.count("BUY_FILL") == 0, "unknown fills on first observation cannot be fabricated");

        // A completed order first observed at terminal state is NOT a reliable new fill.
        Run startedDone = new Run();
        startedDone.baseline();
        startedDone.trade.observe(o(3,50,1000,10,10,10000,"BOUGHT"),"Item");
        check(startedDone.count("OFFER_OBSERVED_TERMINAL") == 1, "terminal first observation must disclose uncertainty");
        check(startedDone.count("BUY_FILL") == 0, "terminal first observation cannot infer all fill prices");

        // Impossible cost/quantity increments cannot be used for profit accounting.
        Run inconsistent = new Run();
        inconsistent.baseline();
        inconsistent.trade.observe(o(2,90,100,20,0,0,"BUYING"),"Item");
        inconsistent.trade.observe(o(2,90,100,20,0,200,"BUYING"),"Item");
        check(inconsistent.count("REVIEW_REQUIRED") == 1, "changed GP without changed qty needs review");

        // A new journal object (simulated restart) baselines existing orders, preserving old event log.
        Run restart = new Run();
        restart.baseline();
        restart.trade.observe(o(4,8,500,10,3,1500,"BUYING"),"Item");
        long before=restart.count("BUY_FILL");
        TradeJournal restarted = new TradeJournal(new TestJournalStorage(restart.folder),"Account\"A");
        OfferState[] existing = new OfferState[8];
        existing[4]=o(4,8,500,10,3,1500,"BUYING");
        restarted.bootstrap(existing,new String[8]);
        check(restart.count("BUY_FILL") == before, "restart must not replay old fill as new");
        restarted.observe(o(4,8,500,10,8,4000,"BUYING"),"Item");
        check(restart.count("BUY_FILL") == before+1, "new fills after restart should be recorded");

        // Two live slots must not contaminate each other's identity or fill counts.
        Run multiple = new Run();
        multiple.baseline();
        multiple.trade.observe(o(0,1,100,5,0,0,"BUYING"),"Item A");
        multiple.trade.observe(o(6,2,200,5,0,0,"SELLING"),"Item B");
        multiple.trade.observe(o(0,1,100,5,2,200,"BUYING"),"Item A");
        multiple.trade.observe(o(6,2,200,5,3,600,"SELLING"),"Item B");
        check(multiple.count("BUY_FILL")==1 && multiple.count("SELL_FILL")==1, "slots must be isolated");
        check(multiple.current().contains("\"item_id\":1") && multiple.current().contains("\"item_id\":2"), "multi-slot snapshot");
        check(!multiple.current().contains("http") && !multiple.current().contains("token"),"strictly local-only records");

        // JSON and filesystem snapshots are produced even with no active offers.
        Run empty = new Run();
        empty.baseline();
        check(empty.current().contains("\"empty\":true"), "empty slots must be represented");
        check(empty.count("OFFER_OBSERVED")==0,"no fake history on fresh account");

        // RuneLite's GrandExchangeOffer.getPrice() returns long; do not truncate rare-item prices.
        Run expensive = new Run();
        expensive.baseline();
        long rarePrice = 3_100_000_000L;
        expensive.trade.observe(o(5, 999, rarePrice, 2, 0, 0, "BUYING"), "High-value item");
        expensive.trade.observe(o(5, 999, rarePrice, 2, 1, rarePrice, "BUYING"), "High-value item");
        check(expensive.current().contains("\"offer_price\":" + rarePrice), "GE offer price must preserve 64-bit values");
        check(expensive.lines().stream().anyMatch(line -> line.contains("\"delta_gp\":" + rarePrice)),
            "Fill GP must preserve 64-bit values");

        System.out.println("GE Trade Journal regression PASSED (" + assertions + " assertions)");
    }
}
