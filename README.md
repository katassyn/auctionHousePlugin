# DSOAuctionHouse

DSOAuctionHouse is a comprehensive auction house plugin for Minecraft servers running version 1.20. It allows players to list items for sale, browse and purchase items from other players, and manage their auctions through an intuitive GUI interface.

## Features

- **User-friendly GUI**: Browse, sell, and buy items through an intuitive interface
- **Item Selling**: Easily sell items directly from your inventory
- **Auction Management**: View and manage your active auctions
- **Mailbox System**: Receive items and money from completed transactions
- **Search Functionality**: Find specific items or browse auctions by player
- **Admin Controls**: Administrative commands for managing the auction house
- **Customizable Messages**: Fully customizable messages and GUI elements
- **Permission-based Access**: Control access to features with detailed permissions
- **Rank-based Limits**: Set different auction limits based on player ranks

## Dependencies

- **Vault**: Required for economy integration
- **MySQL Database**: Required for storing auction data

## Installation

1. Download the plugin JAR file
2. Place the JAR file in your server's `plugins` folder
3. Restart your server to generate the configuration file
4. Configure the `config.yml` file with your database credentials and other settings
5. Restart your server again to apply the changes

## Configuration

The plugin's configuration file (`config.yml`) allows you to customize various aspects:

### Database Settings
```yaml
database:
  host: localhost
  port: 3306
  name: dsoauctionhouse
  user: root
  password: password
  useSSL: false
  autoReconnect: true
```

### GUI Customization
```yaml
gui:
  main_title: "&1&lAuction House"
  player_items_title_template: "&1&l{player_name}'s Auctions"
  mailbox_title: "&1&lYour Mailbox"
  filler_item: BLACK_STAINED_GLASS_PANE
  buttons:
    previous_page: "&cPrevious Page"
    next_page: "&aNext Page"
    back: "&eBack"
    confirm_purchase_lmb: "&aBuy All (LMB)"
    confirm_purchase_rmb: "&aBuy One (RMB)"
    confirm_purchase_mmb: "&aBuy Amount (MMB)"
```

### Auction Limits
```yaml
limits:
  default: 20
  premium: 40
  deluxe: 60
```

### Messages
The plugin includes many customizable messages. See the `config.yml` file for all available message options.

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/ah` or `/auctionhouse` | Open the main auction house GUI | `ah.use` |
| `/ah find <query>` | Search for items in the auction house | `ah.find` |
| `/ah mailbox` | Access your mailbox | `ah.use` |
| `/sell <price>` | Sell the item in your hand | `ah.sell` |
| `/checkah <player>` | View a player's active auctions | `ah.use` |
| `/ahadmin` | Admin commands for the auction house | `ah.admin` |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `ah.use` | Allows using the Auction House main command and buying | `true` |
| `ah.sell` | Allows selling items on the Auction House | `true` |
| `ah.find` | Allows using the /ah find command | `false` |
| `ah.admin` | Allows using admin commands for the Auction House | `op` |

## Usage

### Selling Items
1. Hold the item you want to sell in your main hand
2. Type `/sell <price>` (e.g., `/sell 1000` or `/sell 1k`)
3. The item will be listed in the auction house

### Buying Items
1. Type `/ah` to open the auction house
2. Browse through the available items
3. Click on an item to purchase it:
   - Left-click to buy all
   - Right-click to buy one
   - Middle-click to specify an amount

### Managing Your Auctions
1. Type `/ah` and navigate to your active auctions
2. You can cancel auctions that haven't been sold yet

### Accessing Your Mailbox
1. Type `/ah mailbox` to access your mailbox
2. Claim items and money from completed transactions

## Support

If you encounter any issues or have questions about the plugin, please contact the plugin developer.

## License

This project is licensed under [Your License Here] - see the LICENSE file for details.