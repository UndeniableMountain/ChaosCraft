package org.im4ever12c.chaoscraft.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.TimeSkipEvent;
import org.bukkit.inventory.ItemStack;
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

        // Pick exactly ONE random modifier based on weight/rarity.
        TimeSkipModifier chosen = getRandomModifier();
        if (chosen == null) {
            return; // No modifier selected
        }

        chosen.apply(random);
    }

    /**
     * Picks exactly one modifier from the TimeSkipModifier enum
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
     * Effects may be positive or negative and affect players, entities, or the world.
     * No messages are sent to players.
     */
    private enum TimeSkipModifier {
        // 1. Teleport all loaded entities in the server to a random player's location.
        TELEPORT_ALL_ENTITIES(0.10) {
            @Override
            public void apply(Random random) {
                List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
                if (players.isEmpty()) return;
                Player chosenOne = players.get(random.nextInt(players.size()));
                World world = chosenOne.getWorld();
                Location loc = chosenOne.getLocation();
                for (Entity entity : world.getEntities()) {
                    entity.teleport(loc);
                }
            }
        },
        // 2. Apply a random potion effect to every online player.
        RANDOM_POTION_EFFECT(0.15) {
            @Override
            public void apply(Random random) {
                List<PotionEffectType> possibleEffects = Arrays.asList(
                        PotionEffectType.LEVITATION,
                        PotionEffectType.JUMP_BOOST,
                        PotionEffectType.INVISIBILITY,
                        PotionEffectType.SPEED,
                        PotionEffectType.BLINDNESS,
                        PotionEffectType.SLOW_FALLING,
                        PotionEffectType.REGENERATION,
                        PotionEffectType.RESISTANCE
                );
                PotionEffectType chosenEffect = possibleEffects.get(random.nextInt(possibleEffects.size()));
                int duration = 20 * (10 + random.nextInt(31)); // 10 to 40 seconds
                int amplifier = random.nextInt(2);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.addPotionEffect(new PotionEffect(chosenEffect, duration, amplifier, false, true));
                }
            }
        },
        // 3. Teleport every player to a single random location in the world.
        TELEPORT_ALL_PLAYERS_SINGLE_SPOT(0.08) {
            @Override
            public void apply(Random random) {
                List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
                if (players.isEmpty()) return;
                Player reference = players.get(0);
                World world = reference.getWorld();
                double x = (random.nextDouble() * 10000) - 5000;
                double z = (random.nextDouble() * 10000) - 5000;
                double y = world.getHighestBlockYAt((int) x, (int) z) + 1;
                Location loc = new Location(world, x, y, z);
                for (Player player : players) {
                    player.teleport(loc);
                }
            }
        },
        // 4. Teleport each player to a different random location in their current world.
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
                }
            }
        },
        // 5. Spawn random mobs around multiple random players.
        SPAWN_RANDOM_MOBS(0.10) {
            @Override
            public void apply(Random random) {
                List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
                if (onlinePlayers.isEmpty()) return;
                int affectedPlayersCount = 1 + random.nextInt(Math.min(3, onlinePlayers.size()));
                Collections.shuffle(onlinePlayers, random);
                List<Player> chosenOnes = onlinePlayers.subList(0, affectedPlayersCount);
                List<org.bukkit.entity.EntityType> mobChoices = Arrays.asList(
                        org.bukkit.entity.EntityType.CHICKEN,
                        org.bukkit.entity.EntityType.COW,
                        org.bukkit.entity.EntityType.PIG,
                        org.bukkit.entity.EntityType.CREEPER,
                        org.bukkit.entity.EntityType.SKELETON
                );
                for (Player p : chosenOnes) {
                    World w = p.getWorld();
                    Location center = p.getLocation();
                    int amount = 3 + random.nextInt(6);
                    for (int i = 0; i < amount; i++) {
                        org.bukkit.entity.EntityType type = mobChoices.get(random.nextInt(mobChoices.size()));
                        Location spawnLoc = center.clone().add(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
                        spawnLoc.setY(w.getHighestBlockYAt(spawnLoc) + 1.0);
                        w.spawnEntity(spawnLoc, type);
                    }
                }
            }
        },
        // 6. Randomize blocks around the spawn area.
        RANDOMIZE_SPAWN_BLOCKS(0.05) {
            @Override
            public void apply(Random random) {
                World overworld = Bukkit.getWorlds().get(0); // main world
                Location spawn = overworld.getSpawnLocation();
                int range = 8;
                List<Material> sillyMats = Arrays.asList(
                        Material.GLASS,
                        Material.SLIME_BLOCK,
                        Material.DIAMOND_BLOCK,
                        Material.MELON,
                        Material.TNT,
                        Material.HONEY_BLOCK,
                        Material.GOLD_BLOCK
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
            }
        },
        // 7. Launch every player upward.
        LAUNCH_ALL_PLAYERS(0.05) {
            @Override
            public void apply(Random random) {
                List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
                if (players.isEmpty()) return;
                double launchFactor = 1.0 + random.nextDouble() * 2.0;
                for (Player p : players) {
                    p.setVelocity(new Vector(0, launchFactor, 0));
                }
            }
        },
        // 8. Heal all players: Fully restore health and grant regeneration.
        HEAL_ALL_PLAYERS(0.08) {
            @Override
            public void apply(Random random) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.setHealth(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                    p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 1, false, true));
                }
            }
        },
        // 9. Give random items to all players.
        GIVE_RANDOM_ITEMS(0.07) {
            @Override
            public void apply(Random random) {
                Material[] items = {
                        Material.DIAMOND,
                        Material.GOLD_INGOT,
                        Material.IRON_INGOT,
                        Material.EMERALD,
                        Material.APPLE,
                        Material.BREAD,
                        Material.COOKED_BEEF
                };
                for (Player p : Bukkit.getOnlinePlayers()) {
                    int count = 1 + random.nextInt(3);
                    for (int i = 0; i < count; i++) {
                        Material item = items[random.nextInt(items.length)];
                        p.getInventory().addItem(new ItemStack(item));
                    }
                }
            }
        },
        // 10. Set storm: Change weather to stormy in all worlds.
        SET_STORM(0.05) {
            @Override
            public void apply(Random random) {
                for (World world : Bukkit.getWorlds()) {
                    world.setStorm(true);
                    world.setThundering(true);
                }
            }
        },
        // 11. Clear weather: Change weather to clear in all worlds.
        CLEAR_WEATHER(0.05) {
            @Override
            public void apply(Random random) {
                for (World world : Bukkit.getWorlds()) {
                    world.setStorm(false);
                    world.setThundering(false);
                }
            }
        },
        // 12. Double player speed for 30 seconds.
        DOUBLE_PLAYER_SPEED(0.06) {
            @Override
            public void apply(Random random) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 30, 1, false, true));
                }
            }
        },
        // 13. Invert gravity: Simulate inversion by giving a high jump boost for 10 seconds.
        INVERT_GRAVITY(0.04) {
            @Override
            public void apply(Random random) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 20 * 10, 4, false, true));
                }
            }
        },
        // 14. Advance time: Advance each world's time by a random amount.
        ADVANCE_TIME(0.03) {
            @Override
            public void apply(Random random) {
                for (World world : Bukkit.getWorlds()) {
                    long currentTime = world.getTime();
                    long add = 6000 + random.nextInt(6000); // between 6000 and 12000 ticks
                    world.setTime(currentTime + add);
                }
            }
        },
        // 15. Reverse gravity: Give nearby living entities (except players) an upward velocity boost.
        REVERSE_GRAVITY(0.04) {
            @Override
            public void apply(Random random) {
                for (World world : Bukkit.getWorlds()) {
                    for (Entity e : world.getEntities()) {
                        if (e instanceof LivingEntity && !(e instanceof Player) && !(e instanceof Projectile)) {
                            e.setVelocity(e.getVelocity().setY(2.0));
                        }
                    }
                }
            }
        };

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