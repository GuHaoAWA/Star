package com.guhao.star.efmex;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.MainFrameAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.client.model.ClientModels;
import yesman.epicfight.api.forgeevent.AnimationRegistryEvent;
import yesman.epicfight.api.model.Model;
import yesman.epicfight.api.utils.EpicFightDamageSource;
import yesman.epicfight.gameasset.Models;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.EntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import static com.guhao.star.Star.MODID;

public class StarAnimations {
    public static StaticAnimation EAT_LEFT;
    public static StaticAnimation EAT_RIGHT;
    public static StaticAnimation DRINK_LEFT;
    public static StaticAnimation DRINK_RIGHT;

    public static void init() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(StarAnimations::registerAnimations);
        MinecraftForge.EVENT_BUS.register(new StarAnimations());
    }
    public static void registerAnimations(AnimationRegistryEvent event) {
        event.getRegistryMap().put(MODID, StarAnimations::build);
    }

    private static void build() {
        Models<?> models = FMLEnvironment.dist == Dist.CLIENT ? ClientModels.LOGICAL_CLIENT : Models.LOGICAL_SERVER;
        Model biped = ((Models)models).biped;
        EAT_LEFT = new MainFrameAnimation(0.15F, "biped/living/eat_left", biped);
        EAT_RIGHT = new MainFrameAnimation(0.15F, "biped/living/eat_right", biped);
        DRINK_LEFT = new MainFrameAnimation(0.15F, "biped/living/drink_left", biped);
        DRINK_RIGHT = new MainFrameAnimation(0.15F, "biped/living/drink_right", biped);
    }
    @SubscribeEvent
    @SuppressWarnings("rawtypes")
    public void itemTick(LivingEntityUseItemEvent.Tick event) {
        if (event.getEntityLiving() instanceof Player player) {
            ItemStack stack = event.getItem();
            if (stack.getUseAnimation() == UseAnim.EAT) {
                LazyOptional<EntityPatch> optional = player.getCapability(EpicFightCapabilities.CAPABILITY_ENTITY);
                if (optional.isPresent()) {
                    ((LivingEntityPatch<?>) optional.resolve().get()).playAnimationSynchronized(
                            (player.getMainArm() == HumanoidArm.LEFT && player.getUsedItemHand() == InteractionHand.MAIN_HAND)
                                    || player.getUsedItemHand() == InteractionHand.OFF_HAND ? EAT_LEFT : EAT_RIGHT, 0.0F);
                }
            } else if (stack.getUseAnimation() == UseAnim.DRINK) {
                LazyOptional<EntityPatch> optional = player.getCapability(EpicFightCapabilities.CAPABILITY_ENTITY);
                if (optional.isPresent()) {
                    ((LivingEntityPatch<?>) optional.resolve().get()).playAnimationSynchronized(
                            (player.getMainArm() == HumanoidArm.LEFT && player.getUsedItemHand() == InteractionHand.MAIN_HAND)
                                    || player.getUsedItemHand() == InteractionHand.OFF_HAND ? DRINK_LEFT : DRINK_RIGHT, 0.0F);
                }
            }
        }
    }

    @SubscribeEvent
    public void livingHurtEvent(LivingDamageEvent event) {
        if (event.getEntity() != null && !event.getEntity().level.isClientSide) {
            DamageSource source = event.getSource();
            if (source != null && source.getMsgId().equals("player") && !(source instanceof EpicFightDamageSource)) {
                event.setAmount(event.getAmount() / 2f);
            }
        }
    }
}
