package com.arsenal;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Rarity;

public enum Tier {
    UNCOMMON("Uncommon", ChatFormatting.GREEN, Rarity.UNCOMMON, 1.0f, 130, 600),
    RARE("Rare", ChatFormatting.AQUA, Rarity.RARE, 1.25f, 110, 900),
    EPIC("Epic", ChatFormatting.LIGHT_PURPLE, Rarity.EPIC, 1.55f, 95, 1400),
    LEGENDARY("Legendary", ChatFormatting.GOLD, Rarity.EPIC, 1.9f, 80, 2000),
    MYTHIC("Mythic", ChatFormatting.RED, Rarity.EPIC, 2.4f, 65, 3000);

    public final String label;
    public final ChatFormatting color;
    public final Rarity rarity;
    public final float damageMult;
    public final int cooldown;
    public final int durability;

    Tier(String label, ChatFormatting color, Rarity rarity, float damageMult, int cooldown, int durability) {
        this.label = label;
        this.color = color;
        this.rarity = rarity;
        this.damageMult = damageMult;
        this.cooldown = cooldown;
        this.durability = durability;
    }
}
