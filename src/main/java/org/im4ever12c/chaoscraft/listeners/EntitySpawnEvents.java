package org.im4ever12c.chaoscraft.listeners;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.im4ever12c.chaoscraft.ChaosCraft;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class EntitySpawnEvents implements Listener {

    private final Random random = new Random();

    @EventHandler
    public void onCreatureSpawn(EntitySpawnEvent event) {
        // Only affect living entities.
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity entity = (LivingEntity) event.getEntity();
        // If it's a passive animal, use the Animal modifier pool.
        if (entity instanceof Animals) {
            AnimalSpawnModifier modifier = getRandomAnimalModifier();
            if (modifier != null) {
                modifier.apply(entity, random);
            }
        } else {
            // Otherwise, use the Creature modifier pool.
            CreatureSpawnModifier modifier = getRandomCreatureModifier();
            if (modifier != null) {
                modifier.apply(entity, random);
            }
        }
    }

    // --- Animal Modifiers ---

    private AnimalSpawnModifier getRandomAnimalModifier() {
        double total = 0.0;
        for (AnimalSpawnModifier mod : AnimalSpawnModifier.values()) {
            total += mod.getRarity();
        }
        double roll = random.nextDouble() * total;
        for (AnimalSpawnModifier mod : AnimalSpawnModifier.values()) {
            if (roll < mod.getRarity()) {
                return mod;
            }
            roll -= mod.getRarity();
        }
        return null;
    }

    private enum AnimalSpawnModifier {
        NONE(0.40) {
            @Override
            public void apply(LivingEntity entity, Random random) {
                // Do nothing.
            }
        },
        ATTRIBUTE_BOOST(0.15) {
            @Override
            public void apply(LivingEntity entity, Random random) {
                if (entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
                    double factor = 1.5 + random.nextDouble() * 1.5; // 1.5x to 3x
                    entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)
                            .setBaseValue(entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue() * factor);
                }
                if (entity.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                    double factor = 1.5 + random.nextDouble() * 1.5;
                    double newMax = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue() * factor;
                    entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(newMax);
                    entity.setHealth(newMax);
                }
            }
        },
        NAME_TAG_CHANGE(0.10) {
            @Override
            public void apply(LivingEntity entity, Random random) {
                List<String> names = Arrays.asList("Fluffy", "Moo Moo", "Baa Baa", "Clucky", "Wiggly");
                String chosen = names.get(random.nextInt(names.size()));
                entity.setCustomName(chosen);
                entity.setCustomNameVisible(true);
            }
        },
        POTION_EFFECT(0.10) {
            @Override
            public void apply(LivingEntity entity, Random random) {
                List<PotionEffectType> effects = Arrays.asList(
                        PotionEffectType.SPEED,
                        PotionEffectType.REGENERATION,
                        PotionEffectType.INVISIBILITY,
                        PotionEffectType.JUMP_BOOST,
                        PotionEffectType.RESISTANCE
                );
                PotionEffectType chosen = effects.get(random.nextInt(effects.size()));
                int duration = 20 * (10 + random.nextInt(21)); // 10-30 seconds
                int amplifier = random.nextInt(2); // 0 or 1
                entity.addPotionEffect(new PotionEffect(chosen, duration, amplifier, false, true));
            }
        },
        ANIMAL_CLONE(0.10) {
            @Override
            public void apply(LivingEntity entity, Random random) {
                // Spawn a duplicate of the animal at the same location.
                Location loc = entity.getLocation();
                entity.getWorld().spawnEntity(loc, entity.getType());
            }
        },
        ENTITY_TYPE_CHANGE(0.10) {
            @Override
            public void apply(LivingEntity entity, Random random) {
                // Change animal type based on a simple mapping.
                EntityType current = entity.getType();
                EntityType newType = null;
                switch (current) {
                    case COW: newType = EntityType.SHEEP; break;
                    case SHEEP: newType = EntityType.COW; break;
                    case PIG: newType = EntityType.CHICKEN; break;
                    case CHICKEN: newType = EntityType.PIG; break;
                    default:
                        List<EntityType> allowed = Arrays.asList(
                                EntityType.COW,
                                EntityType.SHEEP,
                                EntityType.PIG,
                                EntityType.CHICKEN
                        );
                        newType = allowed.get(random.nextInt(allowed.size()));
                        break;
                }
                Location loc = entity.getLocation();
                entity.remove();
                if (loc.getWorld() == null) {
                    return;
                }
                loc.getWorld().spawnEntity(loc, newType);
            }
        },
        LAUNCH_ANIMAL(0.05) {
            @Override
            public void apply(LivingEntity entity, Random random) {
                // Give the animal a small upward velocity.
                entity.setVelocity(entity.getVelocity().setY(0.5 + random.nextDouble() * 0.5));
            }
        };

        private final double rarity;
        AnimalSpawnModifier(double rarity) { this.rarity = rarity; }
        public double getRarity() { return rarity; }
        public abstract void apply(LivingEntity entity, Random random);
    }

    // --- Creature (Hostile) Modifiers ---

    private CreatureSpawnModifier getRandomCreatureModifier() {
        double total = 0.0;
        for (CreatureSpawnModifier mod : CreatureSpawnModifier.values()) {
            total += mod.getRarity();
        }
        double roll = random.nextDouble() * total;
        for (CreatureSpawnModifier mod : CreatureSpawnModifier.values()) {
            if (roll < mod.getRarity()) {
                return mod;
            }
            roll -= mod.getRarity();
        }
        return null;
    }

    private enum CreatureSpawnModifier {
        NONE(0.40) {
            @Override
            public void apply(LivingEntity entity, Random random) {
                // Do nothing.
            }
        },
        TIMER_EXPLOSION(0.10) {
            @Override
            public void apply(LivingEntity entity, Random random) {
                Location loc = entity.getLocation();
                if (loc.getWorld() == null) {
                    return;
                }
                // Spawn an invisible ArmorStand to display a countdown.
                ArmorStand timerStand = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
                timerStand.setGravity(false);
                timerStand.setVisible(false);
                timerStand.setCustomNameVisible(true);
                timerStand.setCustomName("5");
                entity.setInvulnerable(true);
                new org.bukkit.scheduler.BukkitRunnable() {
                    int count = 5;
                    @Override
                    public void run() {
                        if (count > 0) {
                            timerStand.setCustomName(String.valueOf(count));
                            count--;
                        } else {
                            timerStand.remove();
                            float explosionPower = 3.0F * (1 + random.nextInt(10));
                            loc.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), explosionPower, false, true);
                            entity.remove();
                            cancel();
                        }
                    }
                }.runTaskTimer(ChaosCraft.getPlugin(ChaosCraft.class), 0L, 20L);
            }
        },
        ATTRIBUTE_BOOST(0.15) {
            @Override
            public void apply(LivingEntity entity, Random random) {
                if (entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
                    double factor = 1.5 + random.nextDouble() * 1.5;
                    entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)
                            .setBaseValue(entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue() * factor);
                }
                if (entity.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                    double factor = 1.5 + random.nextDouble() * 1.5;
                    double newMax = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue() * factor;
                    entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(newMax);
                    entity.setHealth(newMax);
                }
                if (entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                    double factor = 1.5 + random.nextDouble() * 1.5;
                    entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)
                            .setBaseValue(entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getBaseValue() * factor);
                }
                if (entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE) != null) {
                    double factor = 1.0 + random.nextDouble();
                    entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)
                            .setBaseValue(entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).getBaseValue() * factor);
                }
            }
        },
        NAME_TAG_CHANGE(0.10) {
            @Override
            public void apply(LivingEntity entity, Random random) {
                List<String> names = Arrays.asList(
                        "Silly Billy",
                        "Party Animal",
                        "Epic Spawner",
                        "Mad Scientist",
                        "The Unstoppable"
                );
                String chosen = names.get(random.nextInt(names.size()));
                entity.setCustomName(chosen);
                entity.setCustomNameVisible(true);
            }
        },
        POTION_EFFECT(0.10) {
            @Override
            public void apply(LivingEntity entity, Random random) {
                List<PotionEffectType> effects = Arrays.asList(
                        PotionEffectType.SPEED,
                        PotionEffectType.REGENERATION,
                        PotionEffectType.INVISIBILITY,
                        PotionEffectType.JUMP_BOOST,
                        PotionEffectType.RESISTANCE
                );
                PotionEffectType chosen = effects.get(random.nextInt(effects.size()));
                int duration = 20 * (10 + random.nextInt(21));
                int amplifier = random.nextInt(2);
                entity.addPotionEffect(new PotionEffect(chosen, duration, amplifier, false, true));
            }
        },
        ENTITY_TYPE_CHANGE(0.10) {
            @Override
            public void apply(LivingEntity entity, Random random) {
                EntityType current = entity.getType();
                EntityType newType = null;
                switch (current) {
                    case ZOMBIE: newType = EntityType.SKELETON; break;
                    case SKELETON: newType = EntityType.ZOMBIE; break;
                    default:
                        List<EntityType> allowed = Arrays.asList(
                                EntityType.ZOMBIE,
                                EntityType.SKELETON
                        );
                        newType = allowed.get(random.nextInt(allowed.size()));
                        break;
                }
                Location loc = entity.getLocation();
                entity.remove();
                if (loc.getWorld() == null) {
                    return;
                }
                loc.getWorld().spawnEntity(loc, newType);
            }
        };

        private final double rarity;
        CreatureSpawnModifier(double rarity) { this.rarity = rarity; }
        public double getRarity() { return rarity; }
        public abstract void apply(LivingEntity entity, Random random);
    }
}