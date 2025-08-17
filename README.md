# ShopMC
MC Shop
## Logging
This plugin records shop transactions either to a local YAML file or to a
MySQL database. Logging and lookups run asynchronously to avoid blocking the
server thread. When using MySQL, the table is created with indexes on the
player and timestamp columns to keep queries fast.
## Commands
- `/shop [buy|price|search|admin]` – Buy from the server shop or open GUI
- `/sell <material> <qty>` – Sell to the server shop
- `/sellall` – Sell all sellable items in your inventory
- `/shoplog [player] [limit]` – View transaction log
- `/weeklyshop` – Show weekly shop items

