# PlayerMarket (Paper 1.21.6)

PlayerMarket is a **player-driven shop** plugin for Paper servers. Prices are **dynamic** based on stock:
- Low stock → price rises
- High stock → price falls
- Sell price is a configurable percentage of buy price

It ships with a clean GUI (UltimateShop-style), Sell-All menu, weekly items category, and either **flatfile** or **MySQL** storage for stock counts.

## Features
- 📦 Player buys/sells feed a shared, server-wide stock pool
- 📈 Dynamic pricing (`alpha`, `anchor`, and `sell_spread`)
- 🧭 Paginated, centered category GUIs with inert prev/next when out of range
- 🧾 Sell-All GUI with summary and total payout
- 💰 Vault economy integration
- 🗂️ Categories from YAML: `src/main/resources/cats/*.yml`
- 🧱 Filters exclude non-survival-only or disallowed items by default

## Commands
- `/shop` — open the root GUI
- `/sellall` — open the Sell-All GUI
- `/pm rebuildcats` — regenerate categories from YAML
- `/pm reloadcats` — reload categories
- `/pm exportprices` — write current base prices to `plugins/PlayerMarket/data/exported-prices.yml`
- `/pm loadandfill` — reload prices and rebuild categories

## Configuration
Key settings in `src/main/resources/config.yml`:
```yml
pricing:
  default_anchor: 256   # stock level where price ~= base
  alpha: 0.35           # sensitivity to stock levels
  sell_spread: 0.80     # sell price as fraction of buy

weekly:
  items: []             # optional, or use cats/weekly.yml

storage:
  mode: FLATFILE        # FLATFILE or MYSQL
  mysql:
    host: localhost
    port: 3306
    database: playermarket
    username: root
    password: ""
    useSsl: false
    tablePrefix: pm_

importer:
  enabled: true
  mode: FILE
  file: data/base-prices.yml
```

### Prices
Base prices are loaded from `src/main/resources/data/base-prices.yml`.  
Export your current map with:
```
/pm exportprices
```

## Build
Requires **Java 21** and **Maven 3.9+**.
```bash
mvn -U -B clean package
```
The plugin jar will be at `target/player-market-0.7.5.jar`.

## Run (local dev)
1. Copy the jar into your Paper server’s `plugins/` folder.
2. Start the server (Vault is recommended for economy).
3. Use `/shop` or `/sellall`.

## GitHub Actions
This repo includes:
- `.github/workflows/build.yml` — CI build on push/PR to `main`
- `.github/workflows/release.yml` — build and publish artifacts when you create a tag like `v0.7.6`

Update the badges and links below to your repository path.

## Badges
[![Build](https://github.com/your-org/your-repo/actions/workflows/build.yml/badge.svg)](https://github.com/your-org/your-repo/actions/workflows/build.yml)

## Folder Structure
```
src/
  main/
    java/xyz/rcfg/playermarket/...      # plugin sources
    resources/
      plugin.yml
      config.yml
      data/base-prices.yml              # base prices
      cats/*.yml                        # category definitions
```

## Notes
- Buying is **blocked** if stock ≤ 0.
- Next/Previous buttons are disabled (and do nothing) at bounds.
- Items excluded via rules won’t appear even if present in a category file.
- MySQL mode creates/uses a simple `pm_stock` table (configurable prefix).

---

Copyright © 2025 — You may adopt a license of your choice.
