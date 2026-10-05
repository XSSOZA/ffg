package com.arsenal;

public record WeaponDef(String id, WType type, Element element, Tier tier, String skillName) {
    public float damage() {
        return type.baseDamage * tier.damageMult;
    }
    public int cooldownTicks() {
        return tier.cooldown;
    }
}
