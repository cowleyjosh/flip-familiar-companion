package com.getradejournal;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("ge-trade-journal")
public interface GeTradeJournalConfig extends Config
{
    @ConfigItem(keyName = "enabled", name = "Record GE trades", description = "Write your Grand Exchange offers and fills to local files only.", position = 0)
    default boolean enabled() { return true; }
}
