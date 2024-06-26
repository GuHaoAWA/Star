package com.guhao.star.mixins;

import com.guhao.star.api.StarAPI;
import com.guhao.star.regirster.Effect;
import com.guhao.star.regirster.Sounds;
import com.guhao.star.units.Array;
import com.nameless.indestructible.world.capability.AdvancedCustomHumanoidMobPatch;
import com.nameless.toybox.common.attribute.ai.ToyBoxAttributes;
import com.nameless.toybox.config.CommonConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.client.animation.ClientAnimator;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.gameasset.EpicFightSounds;
import yesman.epicfight.particle.EpicFightParticles;
import yesman.epicfight.particle.HitParticleType;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillDataManager;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.skill.guard.ParryingSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.EntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.entity.eventlistener.HurtEvent;

import java.util.Arrays;

@Mixin(value = ParryingSkill.class, remap = false,priority = 99999)
public class ActiveGuardMixin extends GuardSkill {
    public ActiveGuardMixin(Builder builder) {
        super(builder);
    }
    private HurtEvent.Pre event;
    @Unique
    Array star_new$array = new Array();
    @Unique
    StaticAnimation[] star_new$GUARD = star_new$array.getGuard();
    @Unique
    StaticAnimation[] star_new$PARRY = star_new$array.getParry();

    @Mutable
    @Final
    @Shadow
    private static final SkillDataManager.SkillDataKey<Integer> LAST_ACTIVE;

    static {
        LAST_ACTIVE = SkillDataManager.SkillDataKey.createDataKey(SkillDataManager.ValueType.INTEGER);
    }

    @Unique
    public void star_new$Sta(DamageSource entity) {
        StarAPI starAPI = new StarAPI();
        if (entity.getDirectEntity() instanceof LivingEntity entity1) {
            LazyOptional<EntityPatch> optional = entity1.getCapability(EpicFightCapabilities.CAPABILITY_ENTITY);
            optional.ifPresent(patch -> {
                if (patch instanceof AdvancedCustomHumanoidMobPatch<?> Patch) {
                    Patch.setStamina(Patch.getStamina() - starAPI.getStamina(entity1));
                }
            });
        }
    }

    @Inject(at = @At("HEAD"), method = "guard", cancellable = true)
    public void guard(SkillContainer container, CapabilityItem itemCapability, HurtEvent.Pre event, float knockback, float impact, boolean advanced, CallbackInfo ci) {
        ci.cancel();
        ///////////////////////////////////////////////////
        EpicFightDamageSource EFDamageSource = star_new$array.getEpicFightDamageSources(event.getDamageSource());
        if ((EFDamageSource != null)) {
            StaticAnimation an = EFDamageSource.getAnimation();
            if (!(Arrays.asList(star_new$GUARD).contains(an))) {
                if (this.isHoldingWeaponAvailable(event.getPlayerPatch(), itemCapability, BlockType.ADVANCED_GUARD)) {
                    DamageSource damageSource = event.getDamageSource();
                    if (this.isBlockableSource(damageSource, true)) {
                        ServerPlayer playerentity = event.getPlayerPatch().getOriginal();
                        boolean successParrying = (playerentity.tickCount - container.getDataManager().getDataValue(LAST_ACTIVE) < 10);
                        float penalty = container.getDataManager().getDataValue(PENALTY);
                        if (successParrying) {
                            event.getPlayerPatch().playSound(Sounds.BIGBONG, -0.06F, 0.12F);
                        } else {
                            event.getPlayerPatch().playSound(Sounds.BONG, -0.06F, 0.12F);
                        }
                        EpicFightParticles.HIT_BLUNT.get().spawnParticleWithArgument((ServerLevel) playerentity.level, HitParticleType.FRONT_OF_EYES, HitParticleType.ZERO, playerentity, damageSource.getDirectEntity());
                        if (successParrying) {
                            event.setParried(true);
                            penalty = 0.1F;
                            knockback *= 0.4F;
                            container.getDataManager().setData(LAST_ACTIVE, 0);
                            star_new$Sta(event.getDamageSource());
                            if (event.getDamageSource().getDirectEntity() instanceof Monster) {
                                Entity L = event.getDamageSource().getDirectEntity();
                                if (event.getPlayerPatch().getOriginal() != null && (L instanceof LivingEntity E) && (E.hasEffect(Effect.DEFENSE.get()))) {
                                    E.removeEffect(Effect.DEFENSE.get());
                                    LazyOptional<EntityPatch> optional = L.getCapability(EpicFightCapabilities.CAPABILITY_ENTITY);
                                    optional.ifPresent(patch -> {
                                        if (patch instanceof LivingEntityPatch<?> livingEntityPatch) {
                                            ClientAnimator Animator = new ClientAnimator(livingEntityPatch);
                                            livingEntityPatch.playAnimationSynchronized(Animations.BIPED_COMMON_NEUTRALIZED, 0.0F);
                                            Animator.getOwner().getImpact(E.getUsedItemHand());
                                            L.playSound(EpicFightSounds.NEUTRALIZE_BOSSES, 3.0F, 1.2F);
                                        }
                                    });
                                }
                            }
                        } else {
                            penalty += this.getPenalizer(itemCapability);
                            container.getDataManager().setDataSync(PENALTY, penalty, playerentity);
                        }

                        Entity var12 = damageSource.getDirectEntity();
                        if (var12 instanceof LivingEntity) {
                            LivingEntity livingentity = (LivingEntity) var12;
                            knockback += (float) EnchantmentHelper.getKnockbackBonus(livingentity) * 0.1F;
                        }

                        ((ServerPlayerPatch) event.getPlayerPatch()).knockBackEntity(damageSource.getDirectEntity().position(), knockback);
                        float consumeAmount = penalty * impact;
                        ((ServerPlayerPatch) event.getPlayerPatch()).consumeStaminaAlways(consumeAmount);
                        GuardSkill.BlockType blockType = successParrying ? BlockType.ADVANCED_GUARD : (((ServerPlayerPatch) event.getPlayerPatch()).hasStamina(0.0F) ? BlockType.GUARD : BlockType.GUARD_BREAK);
                        StaticAnimation animation = this.getGuardMotion(event.getPlayerPatch(), itemCapability, blockType);
                        if (animation != null) {
                            ((ServerPlayerPatch) event.getPlayerPatch()).playAnimationSynchronized(animation, 0.0F);
                        }

                        if (blockType == BlockType.GUARD_BREAK) {
                            ((ServerPlayerPatch) event.getPlayerPatch()).playSound(EpicFightSounds.NEUTRALIZE_MOBS, 3.0F, 0.0F, 0.1F);
                        }

                        this.dealEvent(event.getPlayerPatch(), event, advanced);
                        ;
                    }
                }
            } else {
                ;
            }
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////
        else {
            if (this.isHoldingWeaponAvailable(event.getPlayerPatch(), itemCapability, BlockType.ADVANCED_GUARD)) {
                DamageSource damageSource = event.getDamageSource();
                if (this.isBlockableSource(damageSource, true)) {

                    ServerPlayer playerentity = event.getPlayerPatch().getOriginal();
                    boolean successParrying = playerentity.tickCount - container.getDataManager().getDataValue(LAST_ACTIVE) < 10;
                    float penalty = container.getDataManager().getDataValue(PENALTY);
                    if (successParrying) {
                        event.getPlayerPatch().playSound(Sounds.BIGBONG, -0.06F, 0.12F);
                    } else {
                        event.getPlayerPatch().playSound(Sounds.BONG, -0.06F, 0.12F);
                    }
                    EpicFightParticles.HIT_BLUNT.get().spawnParticleWithArgument((ServerLevel) playerentity.level, HitParticleType.FRONT_OF_EYES, HitParticleType.ZERO, playerentity, damageSource.getDirectEntity());
                    if (successParrying) {
                        event.setParried(true);
                        penalty = 0.1F;
                        knockback *= 0.4F;
                        container.getDataManager().setData(LAST_ACTIVE, 0);
                        star_new$Sta(event.getDamageSource());
                        if (event.getDamageSource().getDirectEntity() instanceof Monster) {
                            Entity L = event.getDamageSource().getDirectEntity();
                            if (event.getPlayerPatch().getOriginal() != null && (L instanceof LivingEntity E) && (E.hasEffect(Effect.DEFENSE.get()))) {
                                E.removeEffect(Effect.DEFENSE.get());
                                LazyOptional<EntityPatch> optional = L.getCapability(EpicFightCapabilities.CAPABILITY_ENTITY);
                                optional.ifPresent(patch -> {
                                    if (patch instanceof LivingEntityPatch<?> livingEntityPatch) {
                                        ClientAnimator Animator = new ClientAnimator(livingEntityPatch);
                                        livingEntityPatch.playAnimationSynchronized(Animations.BIPED_COMMON_NEUTRALIZED, 0.0F);
                                        Animator.getOwner().getImpact(E.getUsedItemHand());
                                        L.playSound(EpicFightSounds.NEUTRALIZE_BOSSES, 3.0F, 1.2F);
                                    }
                                });
                            }
                        }

                    } else {
                        penalty += this.getPenalizer(itemCapability);
                        container.getDataManager().setDataSync(PENALTY, penalty, playerentity);
                    }

                    Entity var12 = damageSource.getDirectEntity();
                    if (var12 instanceof LivingEntity) {
                        LivingEntity livingentity = (LivingEntity) var12;
                        knockback += (float) EnchantmentHelper.getKnockbackBonus(livingentity) * 0.1F;
                    }

                    ((ServerPlayerPatch) event.getPlayerPatch()).knockBackEntity(damageSource.getDirectEntity().position(), knockback);
                    float consumeAmount = penalty * impact;
                    ((ServerPlayerPatch) event.getPlayerPatch()).consumeStaminaAlways(consumeAmount);
                    GuardSkill.BlockType blockType = successParrying ? BlockType.ADVANCED_GUARD : (((ServerPlayerPatch) event.getPlayerPatch()).hasStamina(0.0F) ? BlockType.GUARD : BlockType.GUARD_BREAK);
                    StaticAnimation animation = this.getGuardMotion(event.getPlayerPatch(), itemCapability, blockType);
                    if (animation != null) {
                        ((ServerPlayerPatch) event.getPlayerPatch()).playAnimationSynchronized(animation, 0.0F);
                    }

                    if (blockType == BlockType.GUARD_BREAK) {
                        ((ServerPlayerPatch) event.getPlayerPatch()).playSound(EpicFightSounds.NEUTRALIZE_MOBS, 3.0F, 0.0F, 0.1F);
                    }

                    this.dealEvent(event.getPlayerPatch(), event, advanced);
                    ;
                }
            }
        }
    }
    @Inject(
            method = {"guard(Lyesman/epicfight/skill/SkillContainer;Lyesman/epicfight/world/capabilities/item/CapabilityItem;Lyesman/epicfight/world/entity/eventlistener/HurtEvent$Pre;FFZ)V"},
            at = {@At("HEAD")},
            remap = false
    )
    private void getSuccessParry(SkillContainer container, CapabilityItem itemCapability, HurtEvent.Pre event, float knockback, float impact, boolean advanced, CallbackInfo ci) {
        this.event = event;
    }

    @ModifyVariable(
            method = {"guard(Lyesman/epicfight/skill/SkillContainer;Lyesman/epicfight/world/capabilities/item/CapabilityItem;Lyesman/epicfight/world/entity/eventlistener/HurtEvent$Pre;FFZ)V"},
            at = @At("HEAD"),
            ordinal = 1,
            remap = false
    )
    private float setImpact(float impact) {
        if (ModList.get().isLoaded("epicparagliders") && (Boolean) CommonConfig.EPIC_PARAGLIDER_COMPAT.get()) {
            return impact;
        } else {
            float blockrate = 1.0F - Math.min((float)((ServerPlayer)((ServerPlayerPatch)this.event.getPlayerPatch()).getOriginal()).getAttributeValue((Attribute) ToyBoxAttributes.BLOCK_RATE.get()) / 100.0F, 0.9F);
            Object var4 = this.event.getDamageSource();
            if (var4 instanceof EpicFightDamageSource) {
                EpicFightDamageSource epicdamagesource = (EpicFightDamageSource)var4;
                float k = epicdamagesource.getImpact();
                return this.event.getAmount() / 4.0F * (1.0F + k / 2.0F) * blockrate;
            } else {
                return this.event.getAmount() / 3.0F * blockrate;
            }
        }
    }
}

