package com.yourorg.servershop.logging;

public interface LogStorage {
    void append(Transaction tx) throws Exception;
    java.util.List<Transaction> last(int limit) throws Exception;
    java.util.List<Transaction> lastOf(String player, int limit) throws Exception;
    /**
     * Query the log with optional filters.
     * @param player player name or null
     * @param material material filter or null
     * @param category category filter or null
     * @param offset starting index (0-based)
     * @param limit max number of rows to return
     */
    java.util.List<Transaction> query(String player, org.bukkit.Material material, String category, int offset, int limit) throws Exception;
    /**
     * Get all transactions since the given instant.
     */
    java.util.List<Transaction> since(java.time.Instant from) throws Exception;
    void close() throws Exception;
}
