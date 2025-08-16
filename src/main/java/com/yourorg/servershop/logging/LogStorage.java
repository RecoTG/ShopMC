package com.yourorg.servershop.logging;

public interface LogStorage {
    void append(Transaction tx) throws Exception;
    java.util.List<Transaction> last(int limit) throws Exception;
    java.util.List<Transaction> lastOf(String uuid, int limit) throws Exception;
    default void purgeOlderThan(long cutoffMs) throws Exception { }
    default void compact() throws Exception { }
    void close() throws Exception;
}
