package org.im4ever12c.chaoscraft.listeners;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Random;

public class ProjectileFireEvents implements Listener {

    private final JavaPlugin plugin;
    private final Random random = new Random();

    public ProjectileFireEvents(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onProjectileFire(ProjectileLaunchEvent launchEvent) {
        Projectile projectile = launchEvent.getEntity();
        if (projectile.getShooter() == null) return;

        // Check each modifier. If the random roll is within its chance, apply it.
        for (ProjectileModifier mod : ProjectileModifier.values()) {
            if (random.nextDouble() < mod.getRarity()) {
                mod.applyModifier(projectile, plugin, random);
            }
        }
    }

    /**
     * Handle hits against blocks or ground for some modifiers (e.g., EXPLOSIVE, SHEEP_EXPLOSION, etc.).
     */
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();

        // 1) Explosive check
        if (hasModifier(projectile, ProjectileModifier.EXPLOSIVE)) {
            float explosionSize = getStoredExplosionSize(projectile);
            if (explosionSize <= 0) {
                // If we didn't store a fixed explosion size, randomize from creeper(3F) to 10x creeper(30F)
                explosionSize = 3F + random.nextFloat() * 27F;
            }
            Location loc = projectile.getLocation();
            // Create explosion (fire=false, breakBlocks=true)
            projectile.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), explosionSize, false, true);

            // Remove projectile
            projectile.remove();
        }

        // 2) Sheep explosion check
        if (hasModifier(projectile, ProjectileModifier.SHEEP_EXPLOSION)) {
            int numberOfSheep = 3 + random.nextInt(5); // 3..7 sheep
            Location loc = projectile.getLocation();

            for (int i = 0; i < numberOfSheep; i++) {
                if (loc.getWorld() == null) {
                    return;
                }
                Sheep sheep = (Sheep) loc.getWorld().spawnEntity(loc, EntityType.SHEEP);
                sheep.setCustomName("Baah!!");
                sheep.setCustomNameVisible(true);
            }
            projectile.remove();
        }

        // 3) Other silly on-hit effects can go here...
    }

    /**
     * Handle entity damage for knockback & damage boosts.
     */
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (!(damager instanceof Projectile)) return;

        Projectile projectile = (Projectile) damager;

        // Knockback check
        if (hasModifier(projectile, ProjectileModifier.KNOCKBACK)) {
            double kbFactor = getMetadataDouble(projectile, ProjectileModifier.KNOCKBACK.name());
            if (event.getEntity() instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) event.getEntity();
                Vector kbVec = target.getLocation().toVector().subtract(projectile.getLocation().toVector());
                kbVec.normalize().multiply(kbFactor);
                target.setVelocity(kbVec);
            }
        }

        // Damage boost check
        if (hasModifier(projectile, ProjectileModifier.DAMAGE_BOOST)) {
            double dmgFactor = getMetadataDouble(projectile, ProjectileModifier.DAMAGE_BOOST.name());
            event.setDamage(event.getDamage() * dmgFactor);
        }

        // One-Punch check (silly example)
        if (hasModifier(projectile, ProjectileModifier.ONE_PUNCH)) {
            // Instantly kill for demonstration (be careful with balancing!)
            event.setDamage(1000.0);
        }
    }

    /* ------------------------------------------------------------------------
       UTILITIES FOR METADATA
       ------------------------------------------------------------------------ */

    private boolean hasModifier(Projectile projectile, ProjectileModifier modifier) {
        return projectile.hasMetadata(modifier.name());
    }

    private double getMetadataDouble(Projectile projectile, String key) {
        List<MetadataValue> values = projectile.getMetadata(key);
        if (!values.isEmpty()) {
            return values.get(0).asDouble();
        }
        return 0.0;
    }

    private float getStoredExplosionSize(Projectile projectile) {
        List<MetadataValue> values = projectile.getMetadata(ProjectileModifier.EXPLOSIVE.name());
        if (!values.isEmpty()) {
            return (float) values.get(0).asDouble();
        }
        return 0;
    }

    /* ------------------------------------------------------------------------
       ENUM: PROJECTILE MODIFIERS (Multiple can apply!)
       ------------------------------------------------------------------------ */

    private enum ProjectileModifier {
        MULTI_SHOT(0.10),      // 10% chance
        SPEED_BOOST(0.15),     // 15% chance
        TRANSFORM(0.08),       // 8% chance
        EXPLOSIVE(0.05),       // 5% chance
        KNOCKBACK(0.10),       // 10% chance
        DAMAGE_BOOST(0.10),    // 10% chance
        SHEEP_EXPLOSION(0.02), // 2% chance (silly effect)
        ONE_PUNCH(0.01),       // 1% chance (insta-kill, extremely silly!)
        PLAY_SOUND(0.12);      // 12% chance (e.g., random mob sound)

        private final double rarity;

        ProjectileModifier(double rarity) {
            this.rarity = rarity;
        }

        public double getRarity() {
            return rarity;
        }

        /**
         * The main entry point for applying this modifier.
         * Each case calls an appropriate function or sets metadata.
         */
        public void applyModifier(Projectile projectile, JavaPlugin plugin, Random random) {
            switch (this) {
                case MULTI_SHOT:
                    applyMultiShot(projectile, random);
                    break;
                case SPEED_BOOST:
                    applySpeedBoost(projectile, random);
                    break;
                case TRANSFORM:
                    transformProjectile(projectile, random);
                    break;
                case EXPLOSIVE:
                    markExplosive(projectile, plugin);
                    break;
                case KNOCKBACK:
                    markKnockback(projectile, plugin, random);
                    break;
                case DAMAGE_BOOST:
                    markDamageBoost(projectile, plugin, random);
                    break;
                case SHEEP_EXPLOSION:
                    markSheepExplosion(projectile, plugin);
                    break;
                case ONE_PUNCH:
                    markOnePunch(projectile, plugin);
                    break;
                case PLAY_SOUND:
                    playRandomSound(projectile, random);
                    break;
            }
        }

        /* ------------------- Implementation of Each Modifier ------------------- */

        private static void applyMultiShot(Projectile original, Random random) {
            // Spawn additional projectiles (2..20)
            int duplicates = 2 + random.nextInt(19);
            for (int i = 0; i < duplicates; i++) {
                Projectile extra = (Projectile) original.getWorld()
                        .spawnEntity(original.getLocation(), original.getType());
                extra.setVelocity(original.getVelocity());
            }
        }

        private static void applySpeedBoost(Projectile projectile, Random random) {
            double speedMultiplier = 2.0 + (random.nextDouble() * 8.0); // 2..10
            Vector newVelocity = projectile.getVelocity().multiply(speedMultiplier);
            projectile.setVelocity(newVelocity);
        }

        private static void transformProjectile(Projectile original, Random random) {
            EntityType[] possibleTypes = {
                    EntityType.ARROW,
                    EntityType.SPECTRAL_ARROW,
                    EntityType.FIREBALL,
                    EntityType.WITHER_SKULL,
                    EntityType.SNOWBALL,
                    EntityType.EGG,
                    EntityType.SMALL_FIREBALL
            };

            EntityType newType = pickDifferentType(original.getType(), possibleTypes, random);
            Location loc = original.getLocation();
            Vector velocity = original.getVelocity();
            original.remove();

            if (loc.getWorld() == null) {
                return;
            }
            Projectile transformed = (Projectile) loc.getWorld().spawnEntity(loc, newType);
            transformed.setVelocity(velocity);
        }

        private static void markExplosive(Projectile projectile, JavaPlugin plugin) {
            // Optionally store a fixed explosion size in metadata:
            // float fixedSize = 3F + random.nextFloat() * 27F; // 3..30
            // Instead of 0, store that float if you want the same size every time.
            projectile.setMetadata(EXPLOSIVE.name(),
                    new FixedMetadataValue(plugin, 0)); // 0 => pick random on impact
        }

        private static void markKnockback(Projectile projectile, JavaPlugin plugin, Random random) {
            double factor = 2.0 + random.nextDouble() * 38.0; // 2..40
            projectile.setMetadata(KNOCKBACK.name(),
                    new FixedMetadataValue(plugin, factor));
        }

        private static void markDamageBoost(Projectile projectile, JavaPlugin plugin, Random random) {
            double dmgFactor = 1.5 + (random.nextDouble() * 1.5); // 1.5..3.0
            projectile.setMetadata(DAMAGE_BOOST.name(),
                    new FixedMetadataValue(plugin, dmgFactor));
        }

        private static void markSheepExplosion(Projectile projectile, JavaPlugin plugin) {
            projectile.setMetadata(SHEEP_EXPLOSION.name(),
                    new FixedMetadataValue(plugin, true));
        }

        private static void markOnePunch(Projectile projectile, JavaPlugin plugin) {
            projectile.setMetadata(ONE_PUNCH.name(),
                    new FixedMetadataValue(plugin, true));
        }

        private static void playRandomSound(Projectile projectile, Random random) {
            // Just a fun effect: play a random mob sound at the projectile location
            Location loc = projectile.getLocation();
            Sound[] possibleSounds = {
                    Sound.ENTITY_COW_AMBIENT,
                    Sound.ENTITY_CHICKEN_AMBIENT,
                    Sound.ENTITY_ENDERMAN_SCREAM,
                    Sound.ENTITY_GHAST_SCREAM,
                    Sound.ENTITY_CAT_AMBIENT
            };
            Sound sound = possibleSounds[random.nextInt(possibleSounds.length)];
            float volume = 1.0F;
            float pitch = 0.5F + random.nextFloat() * 1.5F; // 0.5..2.0
            projectile.getWorld().playSound(loc, sound, volume, pitch);
        }

        /* ------------------- Shared Helper ------------------- */
        private static EntityType pickDifferentType(EntityType current, EntityType[] pool, Random random) {
            EntityType newType = current;
            while (newType == current) {
                newType = pool[random.nextInt(pool.length)];
            }
            return newType;
        }
    }
}