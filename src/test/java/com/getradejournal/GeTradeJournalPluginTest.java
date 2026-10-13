package com.getradejournal;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
public class GeTradeJournalPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(GeTradeJournalPlugin.class);
        RuneLite.main(args);
    }
}
