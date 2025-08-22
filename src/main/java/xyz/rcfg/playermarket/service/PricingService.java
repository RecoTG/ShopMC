package xyz.rcfg.playermarket.service;

public class PricingService {
    private final int anchor; private final double alpha; private final double sellSpread;
    public record Prices(double buyUnit, double sellUnit){}
    public PricingService(int anchor, double alpha, double sellSpread){ this.anchor=anchor; this.alpha=alpha; this.sellSpread=sellSpread; }
    public Prices computePrices(ShopService.ItemInfo info, long stock, boolean weekly){
        if (info == null) return new Prices(0.0, 0.0);
        double base = info.basePrice();
        double factor = 1.0 + alpha * (1.0 - Math.min(1.5, Math.max(0.0, (double)stock / Math.max(1, info.anchor()))));
        if (weekly) factor *= 0.95;
        double buy = Math.max(0.01, base * factor);
        double sell = Math.max(0.01, buy * sellSpread);
        return new Prices(round2(buy), round2(sell));
    }
    private double round2(double d){ return Math.round(d*100.0)/100.0; }
}
