package com.getradejournal;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Pure-Java order tracking. Never posts data to a network endpoint. */
final class TradeJournal
{
    private final LocalJournal journal;
    private final String account;
    private final String sessionId=UUID.randomUUID().toString();
    private final Map<Integer, OfferState> offers=new HashMap<>();
    private final Map<Integer, String> orderIds=new HashMap<>();
    private final Map<Integer, String> names=new HashMap<>();
    private boolean bootstrapped=false;

    TradeJournal(LocalJournal journal, String account) { this.journal=journal; this.account=account; }
    boolean isBootstrapped() { return bootstrapped; }

    // Initial login is a baseline, not historical fill events.
    void bootstrap(OfferState[] initial, String[] itemNames) throws IOException
    {
        if (bootstrapped) return;
        for (int i=0; i<8; i++)
        {
            OfferState cur=initial != null && i < initial.length ? initial[i] : null;
            if (cur==null || cur.isEmpty()) continue;
            offers.put(i,cur);
            orderIds.put(i,UUID.randomUUID().toString());
            names.put(i,itemNames != null && i < itemNames.length ? itemNames[i] : "");
            append("OFFER_OBSERVED",cur,orderIds.get(i),0,0,"Existing offer at recording start; prior fills are not new",names.get(i));
        }
        bootstrapped=true;
        snapshot();
    }

    void observe(OfferState next,String itemName) throws IOException
    {
        if (!bootstrapped || next==null || next.slot<0 || next.slot>7) return;
        int slot=next.slot;
        OfferState prev=offers.get(slot);
        String orderId=orderIds.get(slot);
        if (next.isEmpty())
        {
            if (prev != null && !prev.isEmpty())
            {
                append("OFFER_CLEARED",prev,orderId,0,0,"Slot cleared; do not infer a cancellation or sale",names.get(slot));
            }
            offers.remove(slot); orderIds.remove(slot); names.remove(slot);
            snapshot(); return;
        }
        if (prev==null || prev.isEmpty() || !prev.sameOrder(next))
        {
            if (prev!=null && !prev.isEmpty() && !prev.isTerminal())
            {
                append("ORDER_REPLACED_OR_CHANGED",prev,orderId,0,0,
                    "Order identity changed; prior offer outcome may require review",names.get(slot));
            }
            orderId=UUID.randomUUID().toString();
            orderIds.put(slot,orderId);
            names.put(slot,itemName);
            offers.put(slot,next);
            // The first observation can already include partial fills; their exact delta is unknown.
            append(next.isTerminal() ? "OFFER_OBSERVED_TERMINAL" : "OFFER_STARTED",next,orderId,0,0,
                next.filled>0 ? "Already filled when first observed; historical delta unknown" : "",itemName);
        }
        else
        {
            int deltaQty=next.filled-prev.filled;
            long deltaGp=next.cumulativeGp-prev.cumulativeGp;
            if (deltaQty>0 && deltaGp>=0)
            {
                append(next.side.equals("BUY") ? "BUY_FILL" : next.side.equals("SELL") ? "SELL_FILL" : "UNKNOWN_FILL",
                    next,orderId,deltaQty,deltaGp,"",itemName);
            }
            else if (deltaQty>0 || deltaGp != 0)
            {
                append("REVIEW_REQUIRED",next,orderId,0,0,
                    "Inconsistent filled quantity or cumulative GP; never inferred a fill",itemName);
            }
            if (!prev.isComplete() && next.isComplete()) append("OFFER_COMPLETED",next,orderId,0,0,"",itemName);
            if (!prev.isCancelled() && next.isCancelled()) append("OFFER_CANCELLED",next,orderId,0,0,"",itemName);
            offers.put(slot,next); names.put(slot,itemName);
        }
        snapshot();
    }

    void snapshot() throws IOException
    {
        StringBuilder json=new StringBuilder("{\"schema_version\":1,\"observed_at\":")
            .append(JsonText.q(Instant.now().toString())).append(",\"account\":")
            .append(JsonText.q(account)).append(",\"session_id\":")
            .append(JsonText.q(sessionId)).append(",\"offers\":[");
        for(int slot=0;slot<8;slot++)
        {
            if(slot>0) json.append(',');
            OfferState offer=offers.get(slot);
            if(offer==null) json.append("{\"slot\":").append(slot).append(",\"empty\":true}");
            else json.append(payload(offer,orderIds.get(slot),names.get(slot))).append(",\"empty\":false}");
        }
        json.append("]}");
        journal.current(json.toString());
    }

    private String payload(OfferState offer,String orderId,String itemName)
    {
        return "{\"slot\":"+offer.slot+
            ",\"order_id\":"+JsonText.q(orderId)+
            ",\"item_id\":"+offer.itemId+
            ",\"item_name\":"+JsonText.q(itemName)+
            ",\"side\":"+JsonText.q(offer.side)+
            ",\"state\":"+JsonText.q(offer.status)+
            ",\"offer_price\":"+offer.price+
            ",\"total_quantity\":"+offer.quantity+
            ",\"filled_quantity\":"+offer.filled+
            ",\"cumulative_gp\":"+offer.cumulativeGp;
    }

    private void append(String type,OfferState offer,String orderId,int qty,long gp,String note,String itemName) throws IOException
    {
        String line="{\"schema_version\":1,\"event_id\":"+JsonText.q(UUID.randomUUID().toString())+
            ",\"time\":"+JsonText.q(Instant.now().toString())+
            ",\"event\":"+JsonText.q(type)+
            ",\"account\":"+JsonText.q(account)+
            ",\"session_id\":"+JsonText.q(sessionId)+
            ",\"delta_quantity\":"+qty+
            ",\"delta_gp\":"+gp+
            ",\"note\":"+JsonText.q(note)+
            ",\"offer\":"+payload(offer,orderId,itemName)+"}}";
        journal.append(line);
    }
}
