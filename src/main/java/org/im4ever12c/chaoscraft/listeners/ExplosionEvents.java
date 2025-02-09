/*
    @CLASS-TITLE: ExplosionEvents.java
    @CLASS-DESCRIPTION: This class modifies explosion sizes by applying a random modifier.
    Explosion sources can be either blocks or entities. Explosion size refers to the
    distance/blocks affected by the explosion source.
 */

package org.im4ever12c.chaoscraft.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Random;

public class ExplosionEvents implements Listener {
    Random random = new Random();

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        ExplosionModifier modifier = getRandomModifier();
        if (modifier == null) {
            return;
        }
        if (modifier.isYieldModifier()) {
            float newYield = modifier.modifySize(event.getYield(), random);
            event.setYield(newYield);
        } else {
            modifier.applyEffect(event, random);
            event.setYield(0);
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        ExplosionModifier modifier = getRandomModifier();
        if (modifier == null) {
            return;
        }
        if (modifier.isYieldModifier()) {
            float newYield = modifier.modifySize(event.getYield(), random);
            event.setYield(newYield);
        } else {
            modifier.applyEffect(event, random);
            event.setYield(0);
        }
    }

    /**
     * Weighted random selection of an explosion modifier.
     * (The sum of the rarities is 1.0.)
     */
    private ExplosionModifier getRandomModifier() {
        double total = 0.0;
        for (ExplosionModifier mod : ExplosionModifier.values()) {
            total += mod.getRarity();
        }
        double roll = random.nextDouble() * total;
        for (ExplosionModifier mod : ExplosionModifier.values()) {
            if (roll < mod.getRarity()) {
                return mod;
            }
            roll -= mod.getRarity();
        }
        return null; // Should never happen
    }

    /**
     * Returns a replacement material for block replacement.
     */
    private Material getReplacementMaterial() {
        Material[] materials = { Material.DIAMOND_ORE, Material.OBSIDIAN, Material.BEDROCK };
        return materials[random.nextInt(materials.length)];
    }

    /**
     * Returns a random block material used for randomizing blocks.
     */
    private static Material getRandomBlockMaterial(Random random) {
        Material[] materials = { Material.GLASS, Material.TNT, Material.SLIME_BLOCK, Material.HONEY_BLOCK, Material.DIAMOND_BLOCK, Material.GOLD_BLOCK, Material.EMERALD_BLOCK };
        return materials[random.nextInt(materials.length)];
    }

    /**
     * Enum of explosion modifiers.
     * Some modifiers are yield modifiers (which change the explosion’s force/radius),
     * and others are effect modifiers (which apply a special effect within the blast radius).
     * <p>
     * Rarity values (the sum of all modifiers’ rarities is 1.0):
     *   NONE: 0.50 – do nothing
     *   INCREASE: 0.06
     *   DECREASE: 0.06
     *   RANDOM: 0.06
     *   REPLACE_BLOCKS: 0.04
     *   SPAWN_RANDOM_MOBS: 0.06
     *   HEAL_ENTITIES: 0.06
     *   LAUNCH_ENTITIES: 0.04
     *   SET_FIRE_IN_RADIUS: 0.04
     *   CHANGE_BLOCKS_TO_RANDOM: 0.08
     */
    private enum ExplosionModifier {
        NONE(0.50) {
            @Override
            public boolean isYieldModifier() { return true; }
            @Override
            public float modifySize(float currentSize, Random random) { return currentSize; }
            @Override
            public void applyEffect(EntityExplodeEvent event, Random random) { }
            @Override
            public void applyEffect(BlockExplodeEvent event, Random random) { }
        },
        INCREASE(0.06) {
            @Override
            public boolean isYieldModifier() { return true; }
            @Override
            public float modifySize(float currentSize, Random random) {
                // Increase yield by a factor between 2x and 7x.
                return currentSize * (2 + random.nextInt(6));
            }
            @Override
            public void applyEffect(EntityExplodeEvent event, Random random) { }
            @Override
            public void applyEffect(BlockExplodeEvent event, Random random) { }
        },
        DECREASE(0.06) {
            @Override
            public boolean isYieldModifier() { return true; }
            @Override
            public float modifySize(float currentSize, Random random) {
                // Decrease yield by a factor between 0.5 and 1.0.
                return currentSize * (0.5f + random.nextFloat() * 0.5f);
            }
            @Override
            public void applyEffect(EntityExplodeEvent event, Random random) { }
            @Override
            public void applyEffect(BlockExplodeEvent event, Random random) { }
        },
        RANDOM(0.06) {
            @Override
            public boolean isYieldModifier() { return true; }
            @Override
            public float modifySize(float currentSize, Random random) {
                // Multiply yield by a random float between 0 and 5.
                return currentSize * (random.nextFloat() * 5f);
            }
            @Override
            public void applyEffect(EntityExplodeEvent event, Random random) { }
            @Override
            public void applyEffect(BlockExplodeEvent event, Random random) { }
        },
        REPLACE_BLOCKS(0.04) {
            @Override
            public boolean isYieldModifier() { return false; }
            @Override
            public float modifySize(float currentSize, Random random) { return currentSize; }
            @Override
            public void applyEffect(EntityExplodeEvent event, Random random) {
                Material replacement = getReplacementMaterialStatic(random);
                float blastRadius = event.getYield();
                Location center = event.getEntity().getLocation();
                for (Block block : event.blockList()) {
                    Location blockCenter = block.getLocation().clone().add(0.5, 0.5, 0.5);
                    if (blockCenter.distance(center) <= blastRadius) {
                        block.setType(replacement);
                    }
                }
            }
            @Override
            public void applyEffect(BlockExplodeEvent event, Random random) {
                Material replacement = getReplacementMaterialStatic(random);
                float blastRadius = event.getYield();
                Location center = event.getBlock().getLocation().clone().add(0.5, 0.5, 0.5);
                for (Block block : event.blockList()) {
                    Location blockCenter = block.getLocation().clone().add(0.5, 0.5, 0.5);
                    if (blockCenter.distance(center) <= blastRadius) {
                        block.setType(replacement);
                    }
                }
            }
        },
        SPAWN_RANDOM_MOBS(0.06) {
            @Override
            public boolean isYieldModifier() { return false; }
            @Override
            public float modifySize(float currentSize, Random random) { return currentSize; }
            @Override
            public void applyEffect(EntityExplodeEvent event, Random random) {
                spawnMobs(event.getEntity().getLocation(), event.getYield(), random, event.getEntity().getWorld());
            }
            @Override
            public void applyEffect(BlockExplodeEvent event, Random random) {
                spawnMobs(event.getBlock().getLocation(), event.getYield(), random, event.getBlock().getWorld());
            }
            private void spawnMobs(Location center, float blastRadius, Random random, org.bukkit.World world) {
                int count = 3 + random.nextInt(5); // spawn 3-7 mobs
                for (int i = 0; i < count; i++) {
                    double offsetX = (random.nextDouble() * 2 - 1) * blastRadius;
                    double offsetY = (random.nextDouble() * 2 - 1) * blastRadius;
                    double offsetZ = (random.nextDouble() * 2 - 1) * blastRadius;
                    Location spawnLoc = center.clone().add(offsetX, offsetY, offsetZ);
                    world.spawnEntity(spawnLoc, org.bukkit.entity.EntityType.ZOMBIE);
                }
            }
        },
        HEAL_ENTITIES(0.06) {
            @Override
            public boolean isYieldModifier() { return false; }
            @Override
            public float modifySize(float currentSize, Random random) { return currentSize; }
            @Override
            public void applyEffect(EntityExplodeEvent event, Random random) {
                healEntities(event.getEntity().getLocation(), event.getYield(), random, event.getEntity().getWorld());
            }
            @Override
            public void applyEffect(BlockExplodeEvent event, Random random) {
                healEntities(event.getBlock().getLocation(), event.getYield(), random, event.getBlock().getWorld());
            }
            private void healEntities(Location center, float blastRadius, Random random, org.bukkit.World world) {
                for (Entity e : world.getNearbyEntities(center, blastRadius, blastRadius, blastRadius)) {
                    if (e instanceof org.bukkit.entity.LivingEntity) {
                        org.bukkit.entity.LivingEntity le = (org.bukkit.entity.LivingEntity) e;
                        if (le.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                            le.setHealth(le.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                        }
                    }
                }
            }
        },
        LAUNCH_ENTITIES(0.04) {
            @Override
            public boolean isYieldModifier() { return false; }
            @Override
            public float modifySize(float currentSize, Random random) { return currentSize; }
            @Override
            public void applyEffect(EntityExplodeEvent event, Random random) {
                launchEntities(event.getEntity().getLocation(), event.getYield(), random, event.getEntity().getWorld());
            }
            @Override
            public void applyEffect(BlockExplodeEvent event, Random random) {
                launchEntities(event.getBlock().getLocation(), event.getYield(), random, event.getBlock().getWorld());
            }
            private void launchEntities(Location center, float blastRadius, Random random, org.bukkit.World world) {
                for (Entity e : world.getNearbyEntities(center, blastRadius, blastRadius, blastRadius)) {
                    if (e instanceof org.bukkit.entity.LivingEntity) {
                        e.setVelocity(e.getVelocity().setY(2.0));
                    }
                }
            }
        },
        SET_FIRE_IN_RADIUS(0.04) {
            @Override
            public boolean isYieldModifier() { return false; }
            @Override
            public float modifySize(float currentSize, Random random) { return currentSize; }
            @Override
            public void applyEffect(EntityExplodeEvent event, Random random) {
                setFire(event.getEntity().getLocation(), event.getYield(), random, event.getEntity().getWorld());
            }
            @Override
            public void applyEffect(BlockExplodeEvent event, Random random) {
                setFire(event.getBlock().getLocation(), event.getYield(), random, event.getBlock().getWorld());
            }
            private void setFire(Location center, float blastRadius, Random random, org.bukkit.World world) {
                int r = (int) Math.ceil(blastRadius);
                for (int x = -r; x <= r; x++) {
                    for (int y = -r; y <= r; y++) {
                        for (int z = -r; z <= r; z++) {
                            Location loc = center.clone().add(x, y, z);
                            if (loc.distance(center) <= blastRadius) {
                                if (loc.getBlock().getType() == Material.AIR) {
                                    loc.getBlock().setType(Material.FIRE);
                                }
                            }
                        }
                    }
                }
            }
        },
        CHANGE_BLOCKS_TO_RANDOM(0.08) {
            @Override
            public boolean isYieldModifier() { return false; }
            @Override
            public float modifySize(float currentSize, Random random) { return currentSize; }
            @Override
            public void applyEffect(EntityExplodeEvent event, Random random) {
                changeBlocks(event.getEntity().getLocation(), event.getYield(), random, event.blockList(), event.getEntity().getWorld());
            }
            @Override
            public void applyEffect(BlockExplodeEvent event, Random random) {
                changeBlocks(event.getBlock().getLocation(), event.getYield(), random, event.blockList(), event.getBlock().getWorld());
            }
            private void changeBlocks(Location center, float blastRadius, Random random, Iterable<Block> blocks, org.bukkit.World world) {
                for (Block block : blocks) {
                    Location blockCenter = block.getLocation().clone().add(0.5, 0.5, 0.5);
                    if (blockCenter.distance(center) <= blastRadius) {
                        block.setType(getRandomBlockMaterial(random));
                    }
                }
            }
        };

        private final double rarity;

        ExplosionModifier(double rarity) {
            this.rarity = rarity;
        }

        public double getRarity() {
            return rarity;
        }

        public abstract boolean isYieldModifier();

        public abstract float modifySize(float currentSize, Random random);

        public abstract void applyEffect(EntityExplodeEvent event, Random random);

        public abstract void applyEffect(BlockExplodeEvent event, Random random);

        // Helper static method accessible by enum constants:
        private static Material getReplacementMaterialStatic(Random random) {
            Material[] materials = { Material.DIAMOND_ORE, Material.OBSIDIAN, Material.BEDROCK };
            return materials[random.nextInt(materials.length)];
        }
    }
}