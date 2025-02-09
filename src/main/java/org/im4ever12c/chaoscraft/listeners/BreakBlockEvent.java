package org.im4ever12c.chaoscraft.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class BreakBlockEvent implements Listener {

    private final Random random = new Random();

    @EventHandler
    public void onBlockBreak(BlockBreakEvent breakEvent) {
        // This code picks exactly ONE random modifier based on weighting,
        // which can also result in NO_EVENT if chosen.

        BlockBreakModifier chosen = getRandomModifier();
        if (chosen != null) {
            chosen.apply(breakEvent, random);
        }
    }

    /**
     * Picks exactly one modifier from BlockBreakModifier based on each modifier's rarity weight.
     * If NO_EVENT is chosen, nothing will happen.
     */
    private BlockBreakModifier getRandomModifier() {
        // Sum of all rarities
        double totalWeight = 0.0;
        for (BlockBreakModifier mod : BlockBreakModifier.values()) {
            totalWeight += mod.getRarity();
        }

        // Get a random number between [0, totalWeight)
        double roll = random.nextDouble() * totalWeight;

        // Go through each modifier in turn, subtracting its rarity.
        for (BlockBreakModifier mod : BlockBreakModifier.values()) {
            if (roll < mod.getRarity()) {
                return mod;
            }
            roll -= mod.getRarity();
        }

        return null; // Just in case, though it should never happen with the above logic.
    }

    /**
     * Enum describing possible modifiers. Each has:
     *   - A "rarity" (chance weight)
     *   - An "apply()" method that is called if chosen
     * <p>
     * We include NO_EVENT with its own rarity, so sometimes nothing happens.
     */
    private enum BlockBreakModifier {
        /**
         * Sometimes no effect at all!
         */
        NO_EVENT(0.20) {
            @Override
            public void apply(BlockBreakEvent event, Random random) {
                // Do nothing
            }
        },

        /**
         * Spawns exactly one random mob (animal, monster, or boss).
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
         * Create an explosion at the block location with a random size
         * from creeper-size (3F) up to ~10x creeper (30F).
         */
        EXPLODING_BLOCK(0.10) {
            @Override
            public void apply(BlockBreakEvent event, Random random) {
                World world = event.getBlock().getWorld();
                Location loc = event.getBlock().getLocation().add(0.5, 0.5, 0.5);

                float explosionSize = 3F + random.nextFloat() * 27F;

                // If you don’t want the block to drop items, you can call:
                // event.setDropItems(false);

                world.createExplosion(loc.getX(), loc.getY(), loc.getZ(), explosionSize, false, true);
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
                int dropCount = 1 + random.nextInt(3); // 1..3 items

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
                int xOffset = random.nextInt(3) - 1; // -1..1
                int zOffset = random.nextInt(3) - 1;

                Location strikeLoc = loc.add(xOffset, 0, zOffset);
                if (strikeLoc.getWorld() == null) {
                    return;
                }
                strikeLoc.getWorld().strikeLightning(strikeLoc);
            }
        },

        /**
         * Launch the player who broke the block upward.
         */
        LAUNCH_BREAKER(0.05) {
            @Override
            public void apply(BlockBreakEvent event, Random random) {
                Player player = event.getPlayer();

                double launchFactor = 1.0 + random.nextDouble() * 2.0; // 1..3
                player.setVelocity(player.getVelocity().add(player.getLocation().getDirection().setY(launchFactor)));
                player.sendMessage("You have been launched by the block you broke!");
            }
        };

        private final double rarity;

        BlockBreakModifier(double rarity) {
            this.rarity = rarity;
        }

        public double getRarity() {
            return rarity;
        }

        public abstract void apply(BlockBreakEvent event, Random random);
    }
}