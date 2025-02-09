package org.im4ever12c.chaoscraft.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Sound;
import org.im4ever12c.chaoscraft.ChaosCraft;

import java.util.Random;

public class ProjectileHitEvents implements Listener {

    private final Random random = new Random();

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        // Pick exactly one random modifier based on weighted rarity.
        ProjectileHitModifier modifier = getRandomModifier();
        if (modifier != null) {
            modifier.apply(event, random);
        }
    }

    /**
     * Weighted random selection of a projectile hit modifier.
     */
    private ProjectileHitModifier getRandomModifier() {
        double totalWeight = 0.0;
        for (ProjectileHitModifier mod : ProjectileHitModifier.values()) {
            totalWeight += mod.getRarity();
        }
        double roll = random.nextDouble() * totalWeight;
        for (ProjectileHitModifier mod : ProjectileHitModifier.values()) {
            if (roll < mod.getRarity()) {
                return mod;
            }
            roll -= mod.getRarity();
        }
        return null; // Fallback (should not happen)
    }

    /**
     * Helper method to determine the impact location.
     * If a block was hit, returns the center of that block.
     * Else if an entity was hit, returns the entity's location.
     * Otherwise returns the projectile's location.
     */
    private static Location getImpactLocation(ProjectileHitEvent event) {
        if (event.getHitBlock() != null) {
            Location loc = event.getHitBlock().getLocation();
            return loc.add(0.5, 0.5, 0.5);
        } else if (event.getHitEntity() != null) {
            return event.getHitEntity().getLocation();
        } else {
            return event.getEntity().getLocation();
        }
    }

    /**
     * Enum of 15 funny projectile hit modifiers.
     * Each modifier uses its weighted rarity to determine its chance of occurring.
     * Effects affect blocks, players, and other living entities.
     */
    private enum ProjectileHitModifier {
        // 1. Explosive Impact: Create an explosion at the impact location.
        EXPLOSIVE_IMPACT(0.10) {
            @Override
            public void apply(ProjectileHitEvent event, Random random) {
                Location loc = getImpactLocation(event);
                World world = loc.getWorld();
                if (world == null) return;
                float power = 3.0F + random.nextFloat() * 7.0F; // Explosion power between 3 and 10
                world.createExplosion(loc.getX(), loc.getY(), loc.getZ(), power, false, true);
            }
        },

        // 2. Teleport Nearby: Teleport all nearby living entities (players, mobs, animals) to a random nearby location.
        TELEPORT_NEARBY(0.07) {
            @Override
            public void apply(ProjectileHitEvent event, Random random) {
                Location loc = getImpactLocation(event);
                World world = loc.getWorld();
                if (world == null) return;
                for (Entity e : world.getNearbyEntities(loc, 5, 5, 5)) {
                    // Exclude projectiles
                    if (e instanceof LivingEntity && !(e instanceof Projectile)) {
                        double newX = loc.getX() + (random.nextDouble() * 100 - 50);
                        double newZ = loc.getZ() + (random.nextDouble() * 100 - 50);
                        double newY = world.getHighestBlockYAt((int) newX, (int) newZ) + 1;
                        e.teleport(new Location(world, newX, newY, newZ));
                    }
                }
            }
        },

        // 3. Summon Cows: Spawn 3–5 cows at the impact location.
        SUMMON_COWS(0.08) {
            @Override
            public void apply(ProjectileHitEvent event, Random random) {
                Location loc = getImpactLocation(event);
                World world = loc.getWorld();
                if (world == null) return;
                int count = 3 + random.nextInt(3); // 3 to 5 cows
                for (int i = 0; i < count; i++) {
                    world.spawnEntity(loc, EntityType.COW);
                }
            }
        },

        // 4. Summon Chickens: Spawn 5–10 chickens at the impact location.
        SUMMON_CHICKENS(0.08) {
            @Override
            public void apply(ProjectileHitEvent event, Random random) {
                Location loc = getImpactLocation(event);
                World world = loc.getWorld();
                if (world == null) return;
                int count = 5 + random.nextInt(6); // 5 to 10 chickens
                for (int i = 0; i < count; i++) {
                    world.spawnEntity(loc, EntityType.CHICKEN);
                }
            }
        },

        // 5. Drop Item Rain: Drop several random valuable items from above at the impact location.
        DROP_ITEM_RAIN(0.12) {
            @Override
            public void apply(ProjectileHitEvent event, Random random) {
                Location loc = getImpactLocation(event);
                World world = loc.getWorld();
                if (world == null) return;
                Material[] items = { Material.DIAMOND, Material.GOLD_INGOT, Material.IRON_INGOT, Material.EMERALD, Material.APPLE };
                int count = 3 + random.nextInt(4); // 3 to 6 items
                for (int i = 0; i < count; i++) {
                    Material item = items[random.nextInt(items.length)];
                    Location dropLoc = loc.clone().add(0, 10, 0);
                    world.dropItemNaturally(dropLoc, new ItemStack(item));
                }
            }
        },

        // 6. Play Funny Sound: Play a random humorous sound at the impact location.
        PLAY_FUNNY_SOUND(0.10) {
            @Override
            public void apply(ProjectileHitEvent event, Random random) {
                Location loc = getImpactLocation(event);
                World world = loc.getWorld();
                if (world == null) return;
                Sound[] sounds = {
                        Sound.ENTITY_CAT_AMBIENT,
                        Sound.ENTITY_COW_AMBIENT,
                        Sound.ENTITY_CHICKEN_AMBIENT,
                        Sound.ENTITY_PIG_AMBIENT,
                        Sound.ENTITY_PARROT_AMBIENT
                };
                Sound sound = sounds[random.nextInt(sounds.length)];
                world.playSound(loc, sound, 1.0F, 1.0F);
            }
        },

        // 7. Launch Firework: Spawn a firework that explodes shortly after at the impact location.
        LAUNCH_FIREWORK(0.07) {
            @Override
            public void apply(ProjectileHitEvent event, Random random) {
                Location loc = getImpactLocation(event);
                World world = loc.getWorld();
                if (world == null) return;
                Firework fw = (Firework) world.spawnEntity(loc, EntityType.FIREWORK_ROCKET);
                FireworkMeta meta = fw.getFireworkMeta();
                meta.setPower(1 + random.nextInt(3)); // Power between 1 and 3
                FireworkEffect effect = FireworkEffect.builder()
                        .withColor(Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256)))
                        .withFade(Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256)))
                        .with(Type.values()[random.nextInt(Type.values().length)])
                        .flicker(random.nextBoolean())
                        .trail(random.nextBoolean())
                        .build();
                meta.addEffect(effect);
                fw.setFireworkMeta(meta);
                Bukkit.getScheduler().runTaskLater(
                        ChaosCraft.getPlugin(ChaosCraft.class),
                        fw::detonate,
                        5L
                );
            }
        },

        // 8. Create Fire: Set a 3x3 area at the impact location on fire.
        CREATE_FIRE(0.06) {
            @Override
            public void apply(ProjectileHitEvent event, Random random) {
                Location loc = getImpactLocation(event);
                World world = loc.getWorld();
                if (world == null) return;
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        Location fireLoc = loc.clone().add(x, 0, z);
                        if (fireLoc.getBlock().getType() == Material.AIR) {
                            fireLoc.getBlock().setType(Material.FIRE);
                        }
                    }
                }
            }
        },

        // 9. Summon Lightning: Strike lightning at the impact location.
        SUMMON_LIGHTNING(0.05) {
            @Override
            public void apply(ProjectileHitEvent event, Random random) {
                Location loc = getImpactLocation(event);
                World world = loc.getWorld();
                if (world == null) return;
                world.strikeLightning(loc);
            }
        },

        // 10. Reverse Gravity: Give all nearby living entities (players, mobs, animals) a burst of upward velocity.
        REVERSE_GRAVITY(0.04) {
            @Override
            public void apply(ProjectileHitEvent event, Random random) {
                Location loc = getImpactLocation(event);
                World world = loc.getWorld();
                if (world == null) return;
                for (Entity e : world.getNearbyEntities(loc, 7, 7, 7)) {
                    if (e instanceof LivingEntity && !(e instanceof Projectile)) {
                        e.setVelocity(e.getVelocity().setY(2.0));
                    }
                }
            }
        },

        // 11. Spawn Slime: Spawn 2–5 slimes at the impact location.
        SPAWN_SLIME(0.06) {
            @Override
            public void apply(ProjectileHitEvent event, Random random) {
                Location loc = getImpactLocation(event);
                World world = loc.getWorld();
                if (world == null) return;
                int count = 2 + random.nextInt(4);
                for (int i = 0; i < count; i++) {
                    world.spawnEntity(loc, EntityType.SLIME);
                }
            }
        },

        // 12. Spawn Villager Shout: Spawn a villager with a custom name at the impact location.
        SPAWN_VILLAGER_SHOUT(0.05) {
            @Override
            public void apply(ProjectileHitEvent event, Random random) {
                Location loc = getImpactLocation(event);
                World world = loc.getWorld();
                if (world == null) return;
                Villager villager = (Villager) world.spawnEntity(loc, EntityType.VILLAGER);
                villager.setCustomName("No, sir!");
                villager.setCustomNameVisible(true);
            }
        },

        // 13. Grow Tall: If a block was hit and it is dirt or grass, replace it with grass and tall grass.
        GROW_TALL(0.08) {
            @Override
            public void apply(ProjectileHitEvent event, Random random) {
                if (event.getHitBlock() != null) {
                    Location loc = event.getHitBlock().getLocation();
                    Material type = loc.getBlock().getType();
                    if (type == Material.DIRT || type == Material.GRASS_BLOCK) {
                        loc.getBlock().setType(Material.GRASS_BLOCK);
                        loc.clone().add(0, 1, 0).getBlock().setType(Material.TALL_GRASS);
                    }
                }
            }
        },

        // 14. Advance Time: Advance the world's time by a random amount.
        ADVANCE_TIME(0.03) {
            @Override
            public void apply(ProjectileHitEvent event, Random random) {
                World world = getImpactLocation(event).getWorld();
                if (world == null) return;
                long currentTime = world.getTime();
                long add = 6000 + random.nextInt(6000); // add between 6000 and 12000 ticks
                world.setTime(currentTime + add);
            }
        },

        // 15. Confuse Players: Apply a confusion effect to all nearby living entities (players, mobs, etc.).
        CONFUSE_PLAYERS(0.07) {
            @Override
            public void apply(ProjectileHitEvent event, Random random) {
                Location loc = getImpactLocation(event);
                World world = loc.getWorld();
                if (world == null) return;
                for (Entity e : world.getNearbyEntities(loc, 10, 10, 10)) {
                    if (e instanceof LivingEntity && !(e instanceof Projectile)) {
                        ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 200, 5));
                    }
                }
            }
        };

        private final double rarity;

        ProjectileHitModifier(double rarity) {
            this.rarity = rarity;
        }

        public double getRarity() {
            return rarity;
        }

        public abstract void apply(ProjectileHitEvent event, Random random);

        // Helper method to determine the impact location.
        private static Location getImpactLocation(ProjectileHitEvent event) {
            if (event.getHitBlock() != null) {
                Location loc = event.getHitBlock().getLocation();
                return loc.add(0.5, 0.5, 0.5);
            } else if (event.getHitEntity() != null) {
                return event.getHitEntity().getLocation();
            } else {
                return event.getEntity().getLocation();
            }
        }
    }
}