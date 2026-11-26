package com.unholymc.item;

import com.unholymc.config.ConfigManager;
import com.unholymc.registry.ModComponents;
import com.unholymc.registry.ModItems;
import com.unholymc.registry.ModSounds;
import com.unholymc.registry.ModTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Unholy Sword (GDD 1.4, DECISIONS #25): 10 damage, +50% against slops and hallucinations, 666 durability,
 * recharged with Nightmare Gems in an anvil (+50 each). At 0 durability it does not break: it becomes
 * "discharged" (4 damage, different texture) until recharged. Held in the main hand it drains sanity and
 * amplifies negative auras (see SwordAura).
 */
public class UnholySwordItem extends Item {
    public UnholySwordItem(Properties properties) {
        super(properties);
    }

    public static ItemAttributeModifiers attributes(float damage, float speed) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, damage - 1f, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, speed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    public static boolean isDischarged(ItemStack stack) {
        return stack.getOrDefault(ModComponents.DISCHARGED, false);
    }

    /** Keeps the discharged flag, attack damage and model in line with the current durability. */
    public static void syncCharge(ItemStack stack, @Nullable Entity holder) {
        boolean empty = stack.getDamageValue() >= stack.getMaxDamage();
        if (empty == isDischarged(stack)) return;
        var cfg = ConfigManager.get().unholySword;
        stack.set(ModComponents.DISCHARGED, empty);
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, attributes(empty ? cfg.dischargedAttackDamage : cfg.attackDamage, cfg.attackSpeed));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(empty), List.of(), List.of()));
        if (holder != null && holder.level() instanceof ServerLevel level) {
            level.playSound(null, holder.getX(), holder.getY(), holder.getZ(),
                    empty ? ModSounds.ITEM_UNHOLY_SWORD_DISCHARGED : ModSounds.ITEM_UNHOLY_SWORD_CHARGE, SoundSource.PLAYERS, 0.8f, 1f);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        syncCharge(stack, owner);
    }

    @Override
    public float getAttackDamageBonus(Entity victim, float damage, DamageSource source) {
        return victim.is(ModTags.UNHOLY_SWORD_BONUS) ? damage * ConfigManager.get().unholySword.slopDamageBonus : 0f;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity victim, LivingEntity attacker) {
        super.postHurtEnemy(stack, victim, attacker);
        if (victim.level() instanceof ServerLevel level) {
            level.playSound(null, victim.getX(), victim.getY() + victim.getBbHeight() / 2, victim.getZ(), ModSounds.ITEM_UNHOLY_SWORD_HIT,
                    SoundSource.PLAYERS, 1f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        }
    }

    public static boolean isHeld(LivingEntity entity) {
        return entity.getMainHandItem().is(ModItems.UNHOLY_SWORD);
    }
}
