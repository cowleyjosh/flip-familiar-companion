package com.getradejournal;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import net.runelite.client.util.Filepath;

/** Local-only, append-only history and crash-safe replacement of current snapshots. */
final class LocalJournal implements JournalStorage
{
    private final Filepath dir;

    LocalJournal(Filepath dir) throws IOException
    {
        this.dir = dir;
        dir.createDirectories();
    }

    @Override public void append(String jsonLine) throws IOException
    {
        dir.joinSegment("events.jsonl").write(jsonLine + "\n",
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    @Override public void current(String json) throws IOException
    {
        Filepath target = dir.joinSegment("current_offers.json");
        Filepath temp = dir.createTempFile("current_offers", ".tmp");
        try
        {
            temp.write(json);
            try
            {
                temp.moveTo(target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (AtomicMoveNotSupportedException ex)
            {
                temp.moveTo(target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        finally
        {
            temp.deleteIfExists();
        }
    }
}
