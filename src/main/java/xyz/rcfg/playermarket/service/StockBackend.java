package xyz.rcfg.playermarket.service;
public interface StockBackend { long getStock(String key); void setStock(String key, long value); void addStock(String key, long delta); }
