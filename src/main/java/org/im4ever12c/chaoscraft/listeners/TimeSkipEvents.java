package org.im4ever12c.chaoscraft.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.TimeSkipEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class TimeSkipEvents implements Listener {

    private final Random random = new Random();

    @EventHandler
    public void onTimeSkip(TimeSkipEvent event) {
        // Only proceed if the night is skipped by sleeping
        if (event.getSkipReason() != TimeSkipEvent.SkipReason.NIGHT_SKIP) {
            return;
        }

        // Pick exactly ONE random modifier (based on weight/rarity)
        TimeSkipModifier chosen = getRandomModifier();
        if (chosen == null) {
            return; // No modifier selected
        }

        chosen.apply(random);
    }

    /**
     * Picks exactly one modifier from TimeSkipModifier enum
     * based on each modifier's rarity weight.
     */
    private TimeSkipModifier getRandomModifier() {
        double totalWeight = 0.0;
        for (TimeSkipModifier mod : TimeSkipModifier.values()) {
            totalWeight += mod.getRarity();
        }

        double roll = random.nextDouble() * totalWeight;
        for (TimeSkipModifier mod : TimeSkipModifier.values()) {
            if (roll < mod.getRarity()) {
                return mod;
            }
            roll -= mod.getRarity();
        }
        return null; // fallback
    }

    /**
     * Enum describing possible goofy modifiers.
     * Each has an "apply(Random)" method and a "rarity" (chance).
     */
    private enum TimeSkipModifier {
        /**
         * Teleport all loaded entities in the server to a random player's location.
         */
        TELEPORT_ALL_ENTITIES(0.10) {
            @Override
            public void apply(Random random) {
                List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
                if (players.isEmpty()) return;

                // 1) Pick a random player
                Player chosenOne = players.get(random.nextInt(players.size()));

                // 2) Teleport every entity in that player's world
                World world = chosenOne.getWorld();
                Location loc = chosenOne.getLocation();

                for (Entity entity : world.getEntities()) {
                    entity.teleport(loc);
                }

                Bukkit.broadcastMessage("All entities have been summoned to " + chosenOne.getName() + "'s location!");
            }
        },

        /**
         * Apply a random potion effect to every online player.
         */
        RANDOM_POTION_EFFECT(0.15) {
            @Override
            public void apply(Random random) {
                List<PotionEffectType> possibleEffects = Arrays.asList(
                        PotionEffectType.LEVITATION,
                        PotionEffectType.JUMP_BOOST,
                        PotionEffectType.INVISIBILITY,
                        PotionEffectType.SPEED,
                        PotionEffectType.BLINDNESS,
                        PotionEffectType.SLOW_FALLING
                );
                PotionEffectType chosenEffect = possibleEffects.get(random.nextInt(possibleEffects.size()));

                // 10 to 40 seconds, amplifier 0..1
                int duration = 20 * (10 + random.nextInt(31));
                int amplifier = random.nextInt(2);

                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.addPotionEffect(new PotionEffect(chosenEffect, duration, amplifier, false, true));
                }
                Bukkit.broadcastMessage("Everyone has been affected by " + chosenEffect + "!");
            }
        },

        /**
         * Teleport EVERY player to a single random location in the world.
         * (Contrast with the next modifier that picks individual random spots.)
         */
        TELEPORT_ALL_PLAYERS_SINGLE_SPOT(0.08) {
            @Override
            public void apply(Random random) {
                List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
                if (players.isEmpty()) return;

                Player reference = players.get(0);
                World world = reference.getWorld();

                // Generate random X, Z in +/- 5000 range
                double x = (random.nextDouble() * 10000) - 5000;
                double z = (random.nextDouble() * 10000) - 5000;
                double y = world.getHighestBlockYAt((int) x, (int) z) + 1;

                Location loc = new Location(world, x, y, z);

                for (Player player : players) {
                    player.teleport(loc);
                }
                Bukkit.broadcastMessage("All players were brought to the same random location!");
            }
        },

        /**
         * Teleport each player to a different random location in their current world.
         */
        TELEPORT_EACH_PLAYER_RANDOMLY(0.07) {
            @Override
            public void apply(Random random) {
                List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
                if (players.isEmpty()) return;

                for (Player p : players) {
                    World w = p.getWorld();

                    double x = (random.nextDouble() * 10000) - 5000;
                    double z = (random.nextDouble() * 10000) - 5000;
                    double y = w.getHighestBlockYAt((int) x, (int) z) + 1;

                    Location newLocation = new Location(w, x, y, z);
                    p.teleport(newLocation);
                    p.sendMessage("You have been teleported to a random location: "
                            + newLocation.getBlockX() + ", " + newLocation.getBlockY() + ", " + newLocation.getBlockZ());
                }
                Bukkit.broadcastMessage("Everyone was scattered to random locations!");
            }
        },

        /**
         * Spawn random animals or mobs around *multiple random players*.
         * Could be just 1, or up to 3 players.
         */
        SPAWN_RANDOM_MOBS(0.10) {
            @Override
            public void apply(Random random) {
                List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
                if (onlinePlayers.isEmpty()) return;

                // Decide how many players to affect: 1..3
                int affectedPlayersCount = 1 + random.nextInt(Math.min(3, onlinePlayers.size()));

                // Shuffle the list to pick random players easily
                Collections.shuffle(onlinePlayers, random);

                List<Player> chosenOnes = onlinePlayers.subList(0, affectedPlayersCount);

                // Mobs we might spawn
                List<org.bukkit.entity.EntityType> mobChoices = Arrays.asList(
                        org.bukkit.entity.EntityType.CHICKEN,
                        org.bukkit.entity.EntityType.COW,
                        org.bukkit.entity.EntityType.PIG,
                        org.bukkit.entity.EntityType.CREEPER
                );

                for (Player p : chosenOnes) {
                    World w = p.getWorld();
                    Location center = p.getLocation();

                    // Spawn 3-8 random mobs around each chosen player
                    int amount = 3 + random.nextInt(6);
                    for (int i = 0; i < amount; i++) {
                        org.bukkit.entity.EntityType type = mobChoices.get(random.nextInt(mobChoices.size()));
                        Location spawnLoc = center.clone().add(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
                        spawnLoc.setY(w.getHighestBlockYAt(spawnLoc) + 1.0); // place on top of ground

                        w.spawnEntity(spawnLoc, type);
                    }
                    p.sendMessage("You have been visited by some creatures!");
                }
                Bukkit.broadcastMessage("Random mobs have spawned around " + affectedPlayersCount + " unlucky players...");
            }
        },

        /**
         * Same as original "RANDOMIZE_SPAWN_BLOCKS", just left for completeness.
         */
        RANDOMIZE_SPAWN_BLOCKS(0.05) {
            @Override
            public void apply(Random random) {
                World overworld = Bukkit.getWorlds().get(0); // Usually the main world
                Location spawn = overworld.getSpawnLocation();

                int range = 8;
                List<Material> sillyMats = Arrays.asList(
                        Material.GLASS,
                        Material.SLIME_BLOCK,
                        Material.DIAMOND_BLOCK,
                        Material.MELON,
                        Material.TNT,
                        Material.HONEY_BLOCK
                );

                int baseY = overworld.getHighestBlockYAt(spawn) - 1;
                for (int x = -range; x <= range; x++) {
                    for (int z = -range; z <= range; z++) {
                        Location loc = spawn.clone().add(x, 0, z);
                        loc.setY(baseY);
                        Material randomMat = sillyMats.get(random.nextInt(sillyMats.size()));
                        overworld.getBlockAt(loc).setType(randomMat);
                    }
                }
                Bukkit.broadcastMessage("The spawn area has been mysteriously changed!");
            }
        },

        /**
         * A silly example of launching every player upward with a big velocity.
         * Make sure to handle fall damage if you want them to survive...
         */
        LAUNCH_ALL_PLAYERS(0.05) {
            @Override
            public void apply(Random random) {
                List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
                if (players.isEmpty()) return;

                // Launch factor from 1.0..3.0
                double launchFactor = 1.0 + random.nextDouble() * 2.0;

                for (Player p : players) {
                    Vector launchVel = new Vector(0, launchFactor, 0);
                    p.setVelocity(launchVel);
                    p.sendMessage("Wheee! You have been launched into the air!");
                }

                Bukkit.broadcastMessage("All players have been forcibly launched upwards!");
            }
        };

        // Shared random
        private final double rarity;

        TimeSkipModifier(double rarity) {
            this.rarity = rarity;
        }

        public double getRarity() {
            return rarity;
        }

        /**
         * Called when this modifier is chosen.
         */
        public abstract void apply(Random random);
    }
}