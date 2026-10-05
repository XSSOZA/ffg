package com.arsenal;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;

public class ArsenalMod implements ModInitializer {
    public static final String MOD_ID = "arsenal";
    public static final List<WeaponItem> WEAPONS = new ArrayList<>();

    @Override
    public void onInitialize() {
        for (WeaponDef d : WeaponDefs.ALL) {
            Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, d.id());
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);

            ItemAttributeModifiers attrs = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, d.damage(), AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                    new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, d.type().attackSpeed - 4.0, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND)
                .build();

            ItemLore lore = new ItemLore(List.of(
                Component.literal(d.tier().label + " " + d.element().name().charAt(0) + d.element().name().substring(1).toLowerCase())
                    .withStyle(d.tier().color),
                Component.literal("Right-click: " + d.skillName()).withStyle(ChatFormatting.GRAY),
                Component.literal("Cooldown: " + (d.cooldownTicks() / 20) + "s").withStyle(ChatFormatting.DARK_GRAY)
            ));

            Item.Properties props = new Item.Properties()
                .setId(key)
                .stacksTo(1)
                .durability(d.tier().durability)
                .rarity(d.tier().rarity)
                .attributes(attrs)
                .component(DataComponents.LORE, lore);

            WeaponItem item = Registry.register(BuiltInRegistries.ITEM, id, new WeaponItem(props, d));
            WEAPONS.add(item);
        }

        CreativeModeTab tab = FabricItemGroup.builder()
            .title(Component.translatable("itemGroup.arsenal"))
            .icon(() -> new ItemStack(WEAPONS.get(0)))
            .displayItems((ctx, entries) -> WEAPONS.forEach(entries::accept))
            .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(MOD_ID, "main"), tab);
    }
}
