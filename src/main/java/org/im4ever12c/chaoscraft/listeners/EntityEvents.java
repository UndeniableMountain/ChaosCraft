package org.im4ever12c.chaoscraft.listeners;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.im4ever12c.chaoscraft.ChaosCraft;

import java.util.Random;

public class EntityEvents implements Listener {

    private final Random random = new Random();

    /**
     * On creature spawn, assign metadata based on weighted probabilities.
     */
    @EventHandler
    public void onCreatureSpawn(EntitySpawnEvent event) {
        // Only affect LivingEntities.
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity entity = (LivingEntity) event.getEntity();
        JavaPlugin plugin = ChaosCraft.getPlugin(ChaosCraft.class);

        // bombOnDamage (20% chance)
        if (random.nextDouble() < 0.20) {
            entity.setMetadata("bombOnDamage", new FixedMetadataValue(plugin, true));
        }
        // extraLootMultiplier (25% chance, value 1 to 10)
        if (random.nextDouble() < 0.25) {
            double multiplier = 1 + random.nextInt(10);
            entity.setMetadata("extraLootMultiplier", new FixedMetadataValue(plugin, multiplier));
        }
        // extraSpawnOnDeath (10% chance, value 1 to 25)
        if (random.nextDouble() < 0.10) {
            int extraCount = 1 + random.nextInt(25);
            entity.setMetadata("extraSpawnOnDeath", new FixedMetadataValue(plugin, extraCount));
        }
        // fireOnDamage (15% chance)
        if (random.nextDouble() < 0.15) {
            entity.setMetadata("fireOnDamage", new FixedMetadataValue(plugin, true));
        }
        // freezeOnDamage (10% chance)
        if (random.nextDouble() < 0.10) {
            entity.setMetadata("freezeOnDamage", new FixedMetadataValue(plugin, true));
        }
        // cloneOnDamage (10% chance)
        if (random.nextDouble() < 0.10) {
            entity.setMetadata("cloneOnDamage", new FixedMetadataValue(plugin, true));
        }
        // speedBoostOnDamage (10% chance)
        if (random.nextDouble() < 0.10) {
            entity.setMetadata("speedBoostOnDamage", new FixedMetadataValue(plugin, true));
        }
        // explodeOnDeathDelayed (5% chance)
        if (random.nextDouble() < 0.05) {
            entity.setMetadata("explodeOnDeathDelayed", new FixedMetadataValue(plugin, true));
        }
        // lightningOnDeath (5% chance)
        if (random.nextDouble() < 0.05) {
            entity.setMetadata("lightningOnDeath", new FixedMetadataValue(plugin, true));
        }
        // randomPotionOnDeath (5% chance)
        if (random.nextDouble() < 0.05) {
            entity.setMetadata("randomPotionOnDeath", new FixedMetadataValue(plugin, true));
        }
    }

    /**
     * When an entity is damaged, check for metadata keys and apply effects.
     */
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity entity = (LivingEntity) event.getEntity();
        Location loc = entity.getLocation();
        JavaPlugin plugin = ChaosCraft.getPlugin(ChaosCraft.class);

        // bombOnDamage: Turn the entity into a timed bomb.
        if (entity.hasMetadata("bombOnDamage")) {
            entity.removeMetadata("bombOnDamage", plugin);
            event.setCancelled(true);
            if (loc.getWorld() == null) {
                return;
            }
            final ArmorStand timerStand = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
            timerStand.setGravity(false);
            timerStand.setVisible(false);
            timerStand.setCustomNameVisible(true);
            timerStand.setCustomName("5");
            entity.setInvulnerable(true);
            new BukkitRunnable() {
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
            }.runTaskTimer(plugin, 0L, 20L);
        }

        // fireOnDamage: Set the entity on fire for 5 seconds.
        if (entity.hasMetadata("fireOnDamage")) {
            entity.removeMetadata("fireOnDamage", plugin);
            entity.setFireTicks(100);
        }

        // freezeOnDamage: Cancel damage and freeze the entity.
        if (entity.hasMetadata("freezeOnDamage")) {
            entity.removeMetadata("freezeOnDamage", plugin);
            event.setCancelled(true);
            entity.setVelocity(new Vector(0, 0, 0));
            // (Optional: apply a slowness potion effect here.)
        }

        // cloneOnDamage: Spawn one duplicate of the entity.
        if (entity.hasMetadata("cloneOnDamage")) {
            entity.removeMetadata("cloneOnDamage", plugin);
            if (loc.getWorld() == null) {
                return;
            }
            loc.getWorld().spawnEntity(loc, entity.getType());
        }

        // speedBoostOnDamage: Apply a temporary speed boost for 5 seconds.
        if (entity.hasMetadata("speedBoostOnDamage")) {
            entity.removeMetadata("speedBoostOnDamage", plugin);
            entity.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SPEED, 20 * 5, 1, false, true));
        }
    }

    /**
     * When an entity dies, check for metadata keys to modify drops and spawn extra entities.
     */
    @EventHandler
    public void onEntityDeath(org.bukkit.event.entity.EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Location loc = entity.getLocation();
        org.bukkit.World world = loc.getWorld();
        JavaPlugin plugin = ChaosCraft.getPlugin(ChaosCraft.class);

        // extraLootMultiplier: Multiply the drops.
        if (entity.hasMetadata("extraLootMultiplier")) {
            double multiplier = entity.getMetadata("extraLootMultiplier").get(0).asDouble();
            event.getDrops().forEach(drop -> {
                int extraCopies = (int) multiplier - 1;
                for (int i = 0; i < extraCopies; i++) {
                    if (world == null) {
                        return;
                    }
                    world.dropItemNaturally(loc, drop.clone());
                }
            });
        }

        // extraSpawnOnDeath: Spawn additional copies of the same entity.
        if (entity.hasMetadata("extraSpawnOnDeath")) {
            int extraCount = entity.getMetadata("extraSpawnOnDeath").get(0).asInt();
            for (int i = 0; i < extraCount; i++) {
                if (world == null) {
                    return;
                }
                world.spawnEntity(loc, entity.getType());
            }
        }

        // explodeOnDeathDelayed: Schedule a delayed explosion (3 seconds later).
        if (entity.hasMetadata("explodeOnDeathDelayed")) {
            entity.removeMetadata("explodeOnDeathDelayed", plugin);
            new BukkitRunnable() {
                @Override
                public void run() {
                    float explosionPower = 3.0F * (1 + random.nextInt(10));
                    if (world == null) {
                        return;
                    }
                    world.createExplosion(loc.getX(), loc.getY(), loc.getZ(), explosionPower, false, true);
                }
            }.runTaskLater(plugin, 60L);
        }

        // lightningOnDeath: Strike lightning at the entity's death location.
        if (entity.hasMetadata("lightningOnDeath")) {
            entity.removeMetadata("lightningOnDeath", plugin);
            if (world == null) {
                return;
            }
            world.strikeLightning(loc);
        }

        // randomPotionOnDeath: Apply a random potion effect to all nearby living entities for 10 seconds.
        if (entity.hasMetadata("randomPotionOnDeath")) {
            entity.removeMetadata("randomPotionOnDeath", plugin);
            if (world == null) {
                return;
            }
            for (Entity e : world.getNearbyEntities(loc, 10, 10, 10)) {
                if (e instanceof LivingEntity) {
                    LivingEntity le = (LivingEntity) e;
                    org.bukkit.potion.PotionEffectType[] effects = {
                            org.bukkit.potion.PotionEffectType.SPEED,
                            PotionEffectType.SLOWNESS,
                            PotionEffectType.JUMP_BOOST,
                            org.bukkit.potion.PotionEffectType.INVISIBILITY,
                            org.bukkit.potion.PotionEffectType.REGENERATION
                    };
                    org.bukkit.potion.PotionEffectType effect = effects[random.nextInt(effects.length)];
                    le.addPotionEffect(new org.bukkit.potion.PotionEffect(effect, 20 * 10, 1, false, true));
                }
            }
        }
    }
}