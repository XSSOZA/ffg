package com.arsenal;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** All active skills + elemental on-hit effects. Tweak numbers freely. */
public final class Skills {
    private Skills() {}

    // ------------------------------------------------------------ entry points

    public static void cast(ServerLevel level, Player p, WeaponDef d) {
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.8f);
        switch (d.type()) {
            case KATANA, GREATSWORD -> slashWave(level, p, d);
            case SCYTHE -> slam(level, p, d);
            case WAND -> beam(level, p, d, p.getLookAngle(), 18.0, 1.0f);
            case BOW -> volley(level, p, d);
            case DAGGER -> dash(level, p, d);
        }
    }

    /** Passive proc when the weapon hits something normally. */
    public static void onHit(LivingEntity target, LivingEntity attacker, WeaponDef d) {
        if (attacker.level() instanceof ServerLevel level && level.getRandom().nextFloat() < 0.35f) {
            applyElement(level, attacker, target, d.element(), d.damage());
            fx(level, d.element(), target.position().add(0, target.getBbHeight() * 0.5, 0), 12);
        }
    }

    // ----------------------------------------------------------------- skills

    private static void slashWave(ServerLevel level, Player p, WeaponDef d) {
        Vec3 look = p.getLookAngle();
        Vec3 origin = p.getEyePosition();
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        Set<LivingEntity> hit = new HashSet<>();
        for (int i = 1; i <= 8; i++) {
            Vec3 c = origin.add(look.scale(i));
            for (int k = -3; k <= 3; k++) {
                Vec3 pt = c.add(right.scale(k * 0.55)).add(0, -Math.abs(k) * 0.12, 0);
                fx(level, d.element(), pt, 2);
            }
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(1.6), e -> e != p && e.isAlive())) {
                if (hit.add(e)) strike(level, p, e, d, 1.6f);
            }
        }
    }

    private static void slam(ServerLevel level, Player p, WeaponDef d) {
        Vec3 c = p.position();
        for (int i = 0; i < 48; i++) {
            double a = i * Math.PI * 2 / 48;
            for (double r = 1.5; r <= 4.5; r += 1.5) {
                fx(level, d.element(), c.add(Math.cos(a) * r, 0.2, Math.sin(a) * r), 1);
            }
        }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(4.5, 1.5, 4.5), e -> e != p && e.isAlive())) {
            strike(level, p, e, d, 1.4f);
            Vec3 away = e.position().subtract(c).normalize();
            e.push(away.x * 0.9, 0.5, away.z * 0.9);
            e.hurtMarked = true;
        }
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6f, 1.4f);
    }

    private static void beam(ServerLevel level, Player p, WeaponDef d, Vec3 dir, double range, float dmgMult) {
        Vec3 origin = p.getEyePosition().add(0, -0.2, 0);
        for (double t = 1.0; t <= range; t += 0.5) {
            Vec3 pt = origin.add(dir.scale(t));
            fx(level, d.element(), pt, 1);
            var hits = level.getEntitiesOfClass(LivingEntity.class, new AABB(pt, pt).inflate(0.6), e -> e != p && e.isAlive());
            if (!hits.isEmpty()) {
                strike(level, p, hits.get(0), d, dmgMult);
                fx(level, d.element(), pt, 25);
                return;
            }
            if (!level.getBlockState(net.minecraft.core.BlockPos.containing(pt)).isAir()) {
                fx(level, d.element(), pt, 15);
                return;
            }
        }
    }

    private static void volley(ServerLevel level, Player p, WeaponDef d) {
        Vec3 look = p.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        for (int k = -2; k <= 2; k++) {
            beam(level, p, d, look.add(right.scale(k * 0.07)).normalize(), 28.0, 0.6f);
        }
    }

    private static void dash(ServerLevel level, Player p, WeaponDef d) {
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
        p.setDeltaMovement(flat.x * 1.8, 0.15, flat.z * 1.8);
        p.hurtMarked = true;
        AABB box = p.getBoundingBox().expandTowards(flat.scale(5)).inflate(1.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != p && e.isAlive())) {
            strike(level, p, e, d, 1.3f);
        }
        for (int i = 0; i < 12; i++) {
            fx(level, d.element(), p.position().add(flat.scale(i * 0.4)).add(0, 1, 0), 3);
        }
    }

    // ---------------------------------------------------------------- helpers

    private static void strike(ServerLevel level, Player p, LivingEntity target, WeaponDef d, float mult) {
        DamageSource src = level.damageSources().playerAttack(p);
        target.hurtServer(level, src, d.damage() * mult);
        applyElement(level, p, target, d.element(), d.damage());
    }

    private static void applyElement(ServerLevel level, LivingEntity attacker, LivingEntity t, Element el, float dmg) {
        switch (el) {
            case FIRE -> t.igniteForSeconds(6);
            case ICE -> {
                t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 2));
                t.setTicksFrozen(t.getTicksRequiredToFreeze() + 80);
            }
            case BLOOD -> attacker.heal(Math.max(1.0f, dmg * 0.2f));
            case LIGHTNING -> t.hurtServer(level, level.damageSources().magic(), 3.0f);
            case SHADOW -> t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
            case HOLY -> {
                attacker.heal(2.0f);
                t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0));
            }
            case WIND -> {
                t.push(0, 0.9, 0);
                t.hurtMarked = true;
            }
            case POISON -> t.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1));
            case VOID -> {
                t.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0));
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0));
            }
        }
    }

    private static ParticleOptions particle(Element el) {
        return switch (el) {
            case FIRE -> ParticleTypes.FLAME;
            case ICE -> ParticleTypes.SNOWFLAKE;
            case BLOOD -> new DustParticleOptions(0xB0000F, 1.4f);
            case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK;
            case SHADOW -> ParticleTypes.SQUID_INK;
            case HOLY -> ParticleTypes.END_ROD;
            case WIND -> ParticleTypes.CLOUD;
            case POISON -> ParticleTypes.WITCH;
            case VOID -> ParticleTypes.PORTAL;
        };
    }

    private static void fx(ServerLevel level, Element el, Vec3 pos, int count) {
        level.sendParticles(particle(el), pos.x, pos.y, pos.z, count, 0.15, 0.15, 0.15, 0.02);
    }
}
