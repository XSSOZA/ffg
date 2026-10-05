package com.arsenal;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

public class WeaponItem extends Item {
    private final WeaponDef def;

    public WeaponItem(Properties props, WeaponDef def) {
        super(props);
        this.def = def;
    }

    public WeaponDef def() {
        return def;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel sl) {
            Skills.cast(sl, player, def);
            stack.hurtAndBreak(2, player, hand);
        }
        player.getCooldowns().addCooldown(stack, def.cooldownTicks());
        return InteractionResult.SUCCESS;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        Skills.onHit(target, attacker, def);
        super.hurtEnemy(stack, target, attacker);
    }
}
