package com.getradejournal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/** Local-only, append-only history and crash-safe replacement of current snapshots. */
final class LocalJournal
{
    private final Path dir;
    LocalJournal(Path dir) throws IOException { this.dir=dir; Files.createDirectories(dir); }

    void append(String jsonLine) throws IOException
    {
        Files.write(dir.resolve("events.jsonl"), (jsonLine+"\n").getBytes(StandardCharsets.UTF_8),
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    void current(String json) throws IOException
    {
        Path target=dir.resolve("current_offers.json");
        Path temp=Files.createTempFile(dir, "current_offers", ".tmp");
        try
        {
            Files.write(temp, json.getBytes(StandardCharsets.UTF_8));
            try { Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING); }
        }
        finally { Files.deleteIfExists(temp); }
    }
}
