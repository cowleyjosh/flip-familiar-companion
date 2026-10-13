package com.getradejournal;

import java.io.IOException;

/** Storage boundary for append-only GE history and current snapshots. */
interface JournalStorage
{
    void append(String jsonLine) throws IOException;
    void current(String json) throws IOException;
}
