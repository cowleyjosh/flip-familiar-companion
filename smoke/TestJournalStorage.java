package com.getradejournal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/** Filesystem fixture for dependency-free tests; not included in the production plugin. */
final class TestJournalStorage implements JournalStorage
{
    private final Path dir;
    TestJournalStorage(Path dir) throws IOException
    {
        this.dir = dir;
        Files.createDirectories(dir);
    }

    @Override public void append(String jsonLine) throws IOException
    {
        Files.write(dir.resolve("events.jsonl"), (jsonLine + "\n").getBytes(StandardCharsets.UTF_8),
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    @Override public void current(String json) throws IOException
    {
        Path target = dir.resolve("current_offers.json");
        Path temp = Files.createTempFile(dir, "current_offers", ".tmp");
        try
        {
            Files.write(temp, json.getBytes(StandardCharsets.UTF_8));
            try { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING); }
        }
        finally { Files.deleteIfExists(temp); }
    }
}
