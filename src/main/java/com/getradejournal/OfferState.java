package com.getradejournal;

/** An immutable, SDK-independent GE offer snapshot. */
final class OfferState
{
    final int slot;
    final int itemId;
    final int price;
    final int quantity;
    final int filled;
    final long cumulativeGp;
    final String status;
    final String side;

    OfferState(int slot, int itemId, int price, int quantity, int filled, long cumulativeGp, String status)
    {
        this.slot=slot; this.itemId=itemId; this.price=price;
        this.quantity=quantity; this.filled=filled; this.cumulativeGp=cumulativeGp;
        this.status=status == null ? "EMPTY" : status;
        String s=this.status.toUpperCase(java.util.Locale.ROOT);
        this.side = s.contains("BUY") || s.contains("BOUGHT") ? "BUY" :
            s.contains("SELL") || s.contains("SOLD") ? "SELL" : "UNKNOWN";
    }

    boolean isEmpty() { return itemId <= 0 || quantity <= 0 || "EMPTY".equalsIgnoreCase(status); }
    boolean isTerminal()
    {
        String s=status.toUpperCase(java.util.Locale.ROOT);
        return s.contains("CANCEL") || s.contains("ABORT") || s.contains("BOUGHT") || s.contains("SOLD");
    }
    boolean isComplete()
    {
        String s=status.toUpperCase(java.util.Locale.ROOT);
        return quantity > 0 && filled >= quantity && (s.contains("BOUGHT") || s.contains("SOLD"));
    }
    boolean isCancelled()
    {
        String s=status.toUpperCase(java.util.Locale.ROOT);
        return s.contains("CANCEL") || s.contains("ABORT");
    }
    boolean sameOrder(OfferState next)
    {
        return next != null && !isEmpty() && !next.isEmpty() &&
            itemId==next.itemId && side.equals(next.side) &&
            price==next.price && quantity==next.quantity &&
            next.filled >= filled && next.cumulativeGp >= cumulativeGp &&
            !(isTerminal() && !next.isTerminal());
    }
}
