# Anime Arsenal (Fabric - Minecraft 26.3)

57 weapons: 6 types (katana, greatsword, scythe, wand, bow, dagger) x 9 elements + 3 Souls-like specials.
5 tiers (Uncommon > Rare > Epic > Legendary > Mythic). Right-click = active skill with cooldown.
Left-click hit = 35% chance of elemental effect. Each weapon has a crafting recipe and a creative tab "Anime Arsenal".

## Build
Requires Java 25 and internet (first build downloads Minecraft + Fabric).
    gradle wrapper --gradle-version 9.5.1     # once, if you have no wrapper
    ./gradlew build
Jar: build/libs/anime-arsenal-0.1.0.jar  -> put in .minecraft/mods with Fabric Loader 0.19.5 + Fabric API 0.160.7+26.3

## Customise
- Edit lists at top of generate_assets.py then run: python3 generate_assets.py
- Replace any PNG in src/main/resources/assets/arsenal/textures/item/ with your own higher-res art (64x64 or 128x128 works).
- Numbers (damage/cooldown/durability): Tier.java, WType.java. Skills: Skills.java.

NOTE: written without being able to compile against 26.3. If Gradle reports a renamed method, fix that line.
