package com.getradejournal;

import com.google.inject.Provides;
import net.runelite.client.util.Filepath;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(name="GE Trade Journal", internalName="ge-trade-journal", description="Save Grand Exchange offers and fills as local JSON files", tags={"grand exchange","ge","history","trading"})
public class GeTradeJournalPlugin extends Plugin
{
    private static final Logger log=LoggerFactory.getLogger(GeTradeJournalPlugin.class);
    @Inject private Client client;
    @Inject private ItemManager items;
    @Inject private GeTradeJournalConfig config;
    private String character;
    private TradeJournal journal;
    private int ticksSinceLogin;
    private int ticksSinceSnapshot;
    private boolean trackingWasEnabled;

    @Provides GeTradeJournalConfig provideConfig(ConfigManager manager)
    { return manager.getConfig(GeTradeJournalConfig.class); }

    @Override protected void startUp()
    { character=null; journal=null; ticksSinceLogin=0; ticksSinceSnapshot=0; trackingWasEnabled=false; }

    @Override protected void shutDown()
    { character=null; journal=null; trackingWasEnabled=false; }

    @Subscribe public void onGameStateChanged(GameStateChanged e)
    {
        if (e.getGameState()==GameState.LOGGED_IN) { ticksSinceLogin=0; ticksSinceSnapshot=0; }
        else if (e.getGameState()!=GameState.LOGGED_IN) { journal=null; character=null; ticksSinceLogin=0; ticksSinceSnapshot=0; }
    }

    @Subscribe public void onGameTick(GameTick e)
    {
        if (!config.enabled())
        {
            // Resume with a new baseline instead of claiming fills that happened while disabled.
            trackingWasEnabled=false;
            journal=null;
            character=null;
            return;
        }
        if (client.getGameState()!=GameState.LOGGED_IN) return;
        if (!trackingWasEnabled)
        {
            journal=null;
            character=null;
            ticksSinceLogin=0;
            ticksSinceSnapshot=0;
            trackingWasEnabled=true;
        }
        if (!ensureCharacter()) return;
        ticksSinceLogin++;
        if (!journal.isBootstrapped() && ticksSinceLogin>=4)
        {
            GrandExchangeOffer[] current=client.getGrandExchangeOffers();
            if (current==null || current.length<8) return;
            OfferState[] state=new OfferState[8]; String[] names=new String[8];
            for(int i=0;i<8;i++)
            {
                state[i]=offer(i,current[i]);
                names[i]=itemName(state[i].itemId);
            }
            try { journal.bootstrap(state,names); }
            catch (Exception ex) { log.warn("GE Trade Journal baseline write failed",ex); }
        }
        if (journal.isBootstrapped() && ++ticksSinceSnapshot>=5)
        {
            ticksSinceSnapshot=0;
            // Client may update the offer state without firing an event in some transitions.
            GrandExchangeOffer[] current=client.getGrandExchangeOffers();
            if (current != null && current.length>=8)
            {
                for(int i=0;i<8;i++) report(i,current[i]);
            }
        }
    }

    @Subscribe public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged e)
    {
        if (!config.enabled() || !trackingWasEnabled || client.getGameState()!=GameState.LOGGED_IN) return;
        if (!ensureCharacter() || journal==null || !journal.isBootstrapped()) return;
        report(e.getSlot(),e.getOffer());
    }

    private boolean ensureCharacter()
    {
        if (client.getLocalPlayer()==null || client.getLocalPlayer().getName()==null) return false;
        String name=client.getLocalPlayer().getName().trim();
        if (name.isEmpty()) return false;
        if (name.equals(character) && journal!=null) return true;
        String safe=name.replaceAll("[\\\\/:*?\"<>|]","_").replaceAll("[. ]+$","");
        if (safe.isEmpty()) return false;
        try
        {
            Filepath dir=getPluginDirectory().joinSegment(safe);
            dir.createDirectories();
            journal=new TradeJournal(new LocalJournal(dir),name);
            character=name;
            ticksSinceLogin=0;
            ticksSinceSnapshot=0;
            log.info("GE Trade Journal recording local files for {}",name);
            return true;
        }
        catch (Exception ex) { log.warn("GE Trade Journal cannot create local character folder",ex); return false; }
    }

    private OfferState offer(int slot,GrandExchangeOffer offer)
    {
        if(offer==null) return new OfferState(slot,0,0,0,0,0,"EMPTY");
        return new OfferState(slot,offer.getItemId(),offer.getPrice(),offer.getTotalQuantity(),
            offer.getQuantitySold(),offer.getSpent(),String.valueOf(offer.getState()));
    }

    private String itemName(int id)
    {
        if (id<=0) return "";
        try { return items.getItemComposition(id).getName(); }
        catch (RuntimeException ex) { return "Item "+id; }
    }

    private void report(int slot,GrandExchangeOffer ge)
    {
        if (journal==null || slot<0 || slot>7) return;
        OfferState state=offer(slot,ge);
        try { journal.observe(state,itemName(state.itemId)); }
        catch (Exception ex) { log.warn("GE Trade Journal event persistence failed (slot {})",slot,ex); }
    }
}
