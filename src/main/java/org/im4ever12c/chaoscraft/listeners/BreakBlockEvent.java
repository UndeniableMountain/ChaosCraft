/*
    @CLASS-TITLE: BreakBlockEvent.java
    @CLASS-DESCRIPTION: This class applies a random modifier to block break events.
    Modifiers include non-deadly effects such as spawning mobs, altering drops/XP, summoning lightning,
    or triggering a timed explosion that displays a countdown.
 */

package org.im4ever12c.chaoscraft.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.im4ever12c.chaoscraft.ChaosCraft;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class BreakBlockEvent implements Listener {

    private final Random random = new Random();

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // Pick exactly ONE random modifier based on weighted rarities.
        BlockBreakModifier chosen = getRandomModifier();
        if (chosen != null) {
            chosen.apply(event, random);
        }
    }

    /**
     * Picks exactly one modifier from BlockBreakModifier based on each modifier's rarity weight.
     * If NO_EVENT is chosen, nothing will happen.
     */
    private BlockBreakModifier getRandomModifier() {
        double totalWeight = 0.0;
        for (BlockBreakModifier mod : BlockBreakModifier.values()) {
            totalWeight += mod.getRarity();
        }
        double roll = random.nextDouble() * totalWeight;
        for (BlockBreakModifier mod : BlockBreakModifier.values()) {
            if (roll < mod.getRarity()) {
                return mod;
            }
            roll -= mod.getRarity();
        }
        return null; // Should not happen
    }

    /**
     * Enum describing possible block break modifiers.
     * Each modifier has:
     *   - A "rarity" (chance weight)
     *   - An "apply()" method that is called if chosen.
     * <p>
     * No modifier here is intended to be deadly to players.
     */
    private enum BlockBreakModifier {
        /**
         * No effect.
         */
        NO_EVENT(0.20) {
            @Override
            public void apply(BlockBreakEvent event, Random random) {
                // Do nothing.
            }
        },
        /**
         * Spawn exactly one random mob (animal, monster, or boss) at the broken block’s location.
         */
        SPAWN_RANDOM_MOB(0.15) {
            @Override
            public void apply(BlockBreakEvent event, Random random) {
                World world = event.getBlock().getWorld();
                Location loc = event.getBlock().getLocation().add(0.5, 0, 0.5);
                List<EntityType> possibleMobs = Arrays.asList(
                        EntityType.COW,
                        EntityType.PIG,
                        EntityType.ZOMBIE,
                        EntityType.CREEPER,
                        EntityType.SKELETON,
                        EntityType.SPIDER,
                        EntityType.WITHER
                );
                EntityType chosenType = possibleMobs.get(random.nextInt(possibleMobs.size()));
                world.spawnEntity(loc, chosenType);
            }
        },
        /**
         * Timed Explosion: Instead of exploding immediately, spawn a temporarily invisible ArmorStand
         * with a visible countdown name tag (from "5" down to "1"). After 5 seconds, remove the ArmorStand
         * and create an explosion at that location. The explosion strength is randomly chosen between
         * 1× and 10× a creeper explosion (base value of 3).
         */
        TIMED_EXPLOSION(0.10) {
            @Override
            public void apply(BlockBreakEvent event, Random random) {
                World world = event.getBlock().getWorld();
                // Use the center of the broken block.
                final Location center = event.getBlock().getLocation().clone().add(0.5, 0.5, 0.5);
                // Spawn an ArmorStand to display the countdown.
                final ArmorStand timerStand = world.spawn(center, ArmorStand.class);
                timerStand.setGravity(false);
                timerStand.setVisible(false);
                timerStand.setCustomNameVisible(true);
                timerStand.setCustomName("5");
                // Schedule a countdown task.
                new BukkitRunnable() {
                    int countdown = 5;
                    @Override
                    public void run() {
                        if (countdown > 0) {
                            timerStand.setCustomName(String.valueOf(countdown));
                            countdown--;
                        } else {
                            timerStand.remove();
                            // Determine explosion power: base creeper explosion is ~3,
                            // multiplied by a random factor between 1 and 10.
                            float explosionPower = 3.0F * (1 + random.nextInt(10));
                            world.createExplosion(center.getX(), center.getY(), center.getZ(), explosionPower, false, true);
                            cancel();
                        }
                    }
                }.runTaskTimer(ChaosCraft.getPlugin(ChaosCraft.class), 0L, 20L);
            }
        },
        /**
         * Cancel normal block drops and replace them with random "precious" loot.
         */
        CHANGE_DROPS(0.20) {
            @Override
            public void apply(BlockBreakEvent event, Random random) {
                event.setDropItems(false);
                List<Material> precious = Arrays.asList(
                        Material.DIAMOND,
                        Material.EMERALD,
                        Material.GOLD_INGOT,
                        Material.IRON_INGOT,
                        Material.APPLE
                );
                Location dropLoc = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
                int dropCount = 1 + random.nextInt(3);
                for (int i = 0; i < dropCount; i++) {
                    Material chosen = precious.get(random.nextInt(precious.size()));
                    event.getBlock().getWorld().dropItemNaturally(dropLoc, new ItemStack(chosen));
                }
            }
        },
        /**
         * Change XP dropped from block break to a random amount (0..30).
         */
        CHANGE_XP(0.20) {
            @Override
            public void apply(BlockBreakEvent event, Random random) {
                event.setExpToDrop(random.nextInt(31));
            }
        },
        /**
         * Strike lightning near the block location.
         */
        SUMMON_LIGHTNING(0.10) {
            @Override
            public void apply(BlockBreakEvent event, Random random) {
                Location loc = event.getBlock().getLocation();
                int xOffset = random.nextInt(3) - 1; // -1, 0, or 1
                int zOffset = random.nextInt(3) - 1;
                Location strikeLoc = loc.add(xOffset, 0, zOffset);
                if (strikeLoc.getWorld() != null) {
                    strikeLoc.getWorld().strikeLightning(strikeLoc);
                }
            }
        };

        private final double rarity;
        BlockBreakModifier(double rarity) { this.rarity = rarity; }
        public double getRarity() { return rarity; }
        public abstract void apply(BlockBreakEvent event, Random random);
    }
}