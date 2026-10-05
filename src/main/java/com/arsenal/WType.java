package com.arsenal;

public enum WType {
    //            baseDamage, attackSpeed
    KATANA(5.0f, 1.8f),
    GREATSWORD(8.0f, 1.0f),
    SCYTHE(7.0f, 1.2f),
    WAND(2.5f, 1.6f),
    BOW(3.0f, 1.4f),
    DAGGER(3.5f, 2.6f);

    public final float baseDamage;
    public final float attackSpeed;

    WType(float baseDamage, float attackSpeed) {
        this.baseDamage = baseDamage;
        this.attackSpeed = attackSpeed;
    }
}
