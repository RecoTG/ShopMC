package com.yourorg.servershop.logging;

public interface LogStorage {
    void append(Transaction tx) throws Exception;
    java.util.List<Transaction> last(int limit) throws Exception;
    java.util.List<Transaction> lastOf(String player, int limit) throws Exception;
    void flush() throws Exception;
    void close() throws Exception;
}
