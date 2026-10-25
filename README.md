# Custom Hitsplashes
A RuneLite plugin that replaces hitsplats with animated themes: Spray Paint, Gilded, Bubblegum, Slime, Kitty and Stardust. Classic keeps the game's own look.

## How it works
- The game's hitsplat background sprites are swapped for transparent ones of the same size.
- Each theme is drawn from code, coloured per hit type from the game's own sprites, with RuneLite's RuneScape fonts.
- Other players' hits use a grey version of the theme.

## Known limits
- Turning the plugin on or off, or changing "Skin other players' hits", shows after logging out and back in, because the game caches hitsplat sprites until then.
- Hitsplat types are a copy of the OSRS cache table in `HitsplatDefs`; types Jagex adds later are left vanilla until the table is updated.
- Hits are assumed to stay up 20 client cycles longer than their cache definition says; confirm against the game if timing looks off.
- Hitsplats are anchored at half the actor's logical height, so a few NPCs may sit a little off the game's position.
