package com.unholymc.mixin;

import com.unholymc.config.ConfigManager;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** World anomaly (DECISIONS #32): some zombies wear a carved pumpkin. */
@Mixin(Zombie.class)
public abstract class ZombieMixin {
    @Inject(method = "populateDefaultEquipmentSlots", at = @At("TAIL"))
    private void unholymc$pumpkinHead(RandomSource random, DifficultyInstance difficulty, CallbackInfo ci) {
        Zombie self = (Zombie) (Object) this;
        if (self.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && random.nextFloat() < ConfigManager.get().world.zombiePumpkinChance) {
            self.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CARVED_PUMPKIN));
            self.setDropChance(EquipmentSlot.HEAD, 0.05f);
        }
    }
}
