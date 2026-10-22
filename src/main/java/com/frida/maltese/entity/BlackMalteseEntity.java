package com.frida.maltese.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;

/**
 * La perrita Maltese. Hereda de TameableEntity (la misma clase base del lobo),
 * asi ya sabe: tener dueno, sentarse, seguir al dueno, etc.
 */
public class BlackMalteseEntity extends TameableEntity {

    // Animaciones "especiales" que el servidor activa y el cliente dibuja.
    public static final int ACTION_NONE = 0;
    public static final int ACTION_DANCE = 1;   // baile con el hueso
    public static final int ACTION_LICK = 2;    // lamerse
    public static final int ACTION_STRETCH = 3; // estirarse

    // Datos sincronizados servidor -> cliente (para que el modelo sepa que animacion toca).
    private static final TrackedData<Integer> ACTION =
            DataTracker.registerData(BlackMalteseEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> ACTION_TICKS =
            DataTracker.registerData(BlackMalteseEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public BlackMalteseEntity(EntityType<? extends BlackMalteseEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(ACTION, ACTION_NONE);
        this.dataTracker.startTracking(ACTION_TICKS, 0);
    }

    /** Vida 12 (6 corazones), velocidad 0.3. */
    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 12.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0);
    }

    /** "Cerebro": lista de objetivos por prioridad (numero menor = mas importante). */
    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new SitGoal(this));
        // Seguir al dueno con velocidad 1.3 (corre cuando esta lejos)
        this.goalSelector.add(3, new FollowOwnerGoal(this, 1.3, 8.0F, 2.0F, false));
        // Se acerca a quien sostiene un hueso
        this.goalSelector.add(4, new TemptGoal(this, 1.2, Ingredient.ofItems(Items.BONE), false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.7));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));
    }

    /** Nombre por defecto: Frida (solo si nadie le puso otro nombre). */
    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty,
                                 SpawnReason spawnReason, EntityData entityData,
                                 net.minecraft.nbt.NbtCompound entityNbt) {
        if (!this.hasCustomName()) {
            this.setCustomName(Text.literal("Frida"));
        }
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }

    // ------------------------------------------------------------------
    //  INTERACCION: clic derecho
    // ------------------------------------------------------------------
    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        boolean client = this.getWorld().isClient;

        // --- HUESO: domestica (50% de probabilidad) y provoca el baile ---
        if (stack.isOf(Items.BONE)) {
            if (!client) {
                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                }
                if (!this.isTamed()) {
                    if (this.random.nextInt(2) == 0) {
                        this.setOwner(player);          // domesticada!
                        this.navigation.stop();
                        this.setTarget(null);
                        this.setSitting(false);
                        this.getWorld().sendEntityStatus(this, (byte) 7); // corazones de domesticacion
                    } else {
                        this.getWorld().sendEntityStatus(this, (byte) 6); // humito: fallo
                    }
                } else {
                    this.heal(4.0F); // los huesos la curan
                }
                this.startAction(ACTION_DANCE);
                this.spawnHearts(7);
            }
            return ActionResult.success(client);
        }

        // --- Mano vacia + dueno: sentar / levantar ---
        if (this.isTamed() && this.isOwner(player) && stack.isEmpty() && hand == Hand.MAIN_HAND) {
            if (!client) {
                this.commandSit(!this.isSitting());
            }
            return ActionResult.success(client);
        }
        return super.interactMob(player, hand);
    }

    /** Sienta (true) o levanta (false) a la perrita. */
    public void commandSit(boolean sit) {
        this.setSitting(sit);
        this.navigation.stop();
        this.setTarget(null);
    }

    // ------------------------------------------------------------------
    //  ANIMACIONES ESPECIALES (logica en el servidor)
    // ------------------------------------------------------------------
    public static int getActionDuration(int action) {
        return switch (action) {
            case ACTION_DANCE -> 80;
            case ACTION_LICK -> 40;
            case ACTION_STRETCH -> 50;
            default -> 1;
        };
    }

    public int getAction() { return this.dataTracker.get(ACTION); }
    public int getActionTicks() { return this.dataTracker.get(ACTION_TICKS); }

    private void startAction(int action) {
        this.dataTracker.set(ACTION, action);
        this.dataTracker.set(ACTION_TICKS, getActionDuration(action));
        if (action == ACTION_DANCE) {
            this.playSound(SoundEvents.ENTITY_WOLF_PANT, 0.8F, 1.7F);
        }
    }

    private void spawnHearts(int count) {
        if (this.getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.HEART, this.getX(), this.getY() + 0.7, this.getZ(),
                    count, 0.3, 0.3, 0.3, 0.02);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient) return;

        int action = this.getAction();
        if (action != ACTION_NONE) {
            int left = this.getActionTicks() - 1;
            if (action == ACTION_DANCE && left % 8 == 0) {
                this.spawnHearts(2); // corazones mientras baila
            }
            if (action == ACTION_DANCE && left % 20 == 0) {
                this.playSound(SoundEvents.ENTITY_WOLF_AMBIENT, 0.6F, 1.9F);
            }
            if (left <= 0) {
                this.dataTracker.set(ACTION, ACTION_NONE);
                this.dataTracker.set(ACTION_TICKS, 0);
            } else {
                this.dataTracker.set(ACTION_TICKS, left);
            }
        } else if (this.isOnGround() && this.getNavigation().isIdle() && this.random.nextInt(400) == 0) {
            // De vez en cuando, parada y tranquila: se lame o se estira
            if (this.isInSittingPose() || this.random.nextBoolean()) {
                this.startAction(ACTION_LICK);
            } else {
                this.startAction(ACTION_STRETCH);
            }
        }
    }

    /** Mientras baila no camina (como si estuviera "inmovil"). */
    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || this.getAction() == ACTION_DANCE;
    }

    // ------------------------------------------------------------------
    //  Otros
    // ------------------------------------------------------------------
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null; // sin crianza, para mantener el mod simple
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return false;
    }

    // ------------------------------------------------------------------
    //  SONIDOS: reutilizamos los del lobo, con tono mas agudo y volumen bajo
    // ------------------------------------------------------------------
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ENTITY_WOLF_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ENTITY_WOLF_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_WOLF_DEATH; }
    @Override protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.ENTITY_WOLF_STEP, 0.12F, 1.3F);
    }
    @Override protected float getSoundVolume() { return 0.6F; }
    @Override public float getSoundPitch() { return super.getSoundPitch() + 0.5F; } // voz de perrita pequena
}
