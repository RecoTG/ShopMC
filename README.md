# ShopMC

Dynamic server shop plugin for Minecraft with GUIs, weekly deals and per-item dynamic pricing.

## Requirements
- Vault-compatible economy plugin

## Permissions

| Permission        | Description                             | Default |
|-------------------|-----------------------------------------|---------|
| `servershop.use`  | Access `/shop`, `/sell`, `/sellall`, and `/weeklyshop` commands. | `true` |
| `servershop.admin`| Access administrative commands like `/shoplog`. | `op` |

## Configuration

Key settings from `config.yml`:

### Weekly Shop
- `weekly.count` – number of rotating items each week.
- `weekly.discount` – multiplier applied to weekly items.
- `weekly.firstDayOfWeek` – day that starts the cycle.

### Price Model
- `priceModel.minFactor` / `priceModel.maxFactor` – bounds for base prices.
- `priceModel.sellStep` – amount price changes per sale.

### Logging & Database
- `logging.storage` – `YAML` or `MYSQL`.
- `logging.maxEntries` – maximum entries kept in YAML logs.
- `mysql.*` – connection settings when using MySQL.

### Dynamic Pricing
- `dynamicPricing.enabled` – toggle dynamic price adjustments.
- `dynamicPricing.storage` – `YAML` or `MYSQL` storage.
- `dynamicPricing.initialMultiplier` / `minMultiplier` / `maxMultiplier` – price multiplier settings.
- `dynamicPricing.decay.enabled` – enable periodic decay towards 1.0.
- `dynamicPricing.decay.perHourTowards1` – rate of decay.
- `dynamicPricing.decay.saveEveryMinutes` – persistence interval.

### GUI
- `gui.titles.*` – titles for category, item, weekly, and sell menus.
- `gui.rows.*` – number of rows for each menu screen.

## Example Flows

### Buying Items
1. Run `/shop` to open the main categories menu.
2. Choose a category, then select an item and confirm the purchase.

### Selling Items
1. Run `/sell <material> <qty>` to sell a specific item.
2. Run `/sellall` to sell all sellable items in your inventory.

### Viewing Logs
1. Administrators run `/shoplog [player] [limit]` to view recent transactions.

### Weekly Deals
1. Run `/weeklyshop` to open the weekly shop menu and purchase discounted items.

## Building from Source

This project uses Maven:

```bash
mvn package
```

The built plugin will be located in the `target` directory.

## License

[MIT](LICENSE)
