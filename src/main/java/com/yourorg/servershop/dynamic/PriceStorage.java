package com.yourorg.servershop.dynamic;

import org.bukkit.Material;

public interface PriceStorage {
    java.util.Map<Material, PriceState> loadAll() throws Exception;
    void save(Material material, PriceState state) throws Exception;
    void saveAll(java.util.Map<Material, PriceState> map) throws Exception;
    default void flush() throws Exception {}
    void close() throws Exception;
}
