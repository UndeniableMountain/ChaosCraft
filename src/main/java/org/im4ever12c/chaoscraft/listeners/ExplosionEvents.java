/*
    @CLASS-TITLE: ExplosionEvents.java
    @CLASS-DESCRIPTION: This class modifies explosion sizes by applying a random modifier.
    Explosion sources can be either blocks or entities. Explosion size refers to the
    distance/blocks affected by the explosion source.
 */

package org.im4ever12c.chaoscraft.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Random;

public class ExplosionEvents implements Listener {
    Random random = new Random();

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent explodeEvent) {
        ExplosionModifier modifier = getRandomModifier();
        if (modifier == null) {
            return;
        }
        if (modifier == ExplosionModifier.REPLACE_BLOCKS) {
            applyReplacementModifier(explodeEvent);
            explodeEvent.setYield(0);
        } else {
            explodeEvent.setYield(modifier.modifySize(explodeEvent.getYield(), random));
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent explodeEvent) {
        ExplosionModifier modifier = getRandomModifier();
        if (modifier == null) {
            return;
        }
        if (modifier == ExplosionModifier.REPLACE_BLOCKS) {
            applyReplacementModifier(explodeEvent);
            explodeEvent.setYield(0);
        } else {
            explodeEvent.setYield(modifier.modifySize(explodeEvent.getYield(), random));
        }
    }

    /**
     * Chooses a modifier at random.
     *  - 50% chance: no modifier (returns null).
     *  - 30% chance: one of the common yield modifiers (INCREASE, DECREASE, or RANDOM).
     *  - 20% chance: the REPLACE_BLOCKS modifier.
     */
    private ExplosionModifier getRandomModifier() {
        int roll = random.nextInt(100);
        if (roll < 50) {
            return null;
        } else if (roll < 90) {
            ExplosionModifier[] commonModifiers = {
                    ExplosionModifier.INCREASE,
                    ExplosionModifier.DECREASE,
                    ExplosionModifier.RANDOM
            };
            return commonModifiers[random.nextInt(commonModifiers.length)];
        } else {
            return ExplosionModifier.REPLACE_BLOCKS;
        }
    }

    /**
     * Replace every block in the explosion's block list with one material.
     */
    private void applyReplacementModifier(EntityExplodeEvent event) {
        Material replacement = getReplacementMaterial();
        for (Block block : event.blockList()) {
            block.setType(replacement);
        }
    }

    /**
     * Replace every block in the explosion's block list with one material.
     */
    private void applyReplacementModifier(BlockExplodeEvent event) {
        Material replacement = getReplacementMaterial();
        for (Block block : event.blockList()) {
            block.setType(replacement);
        }
    }

    /**
     * Randomly selects one of the replacement materials.
     */
    private Material getReplacementMaterial() {
        Material[] materials = {
                Material.DIAMOND_ORE,
                Material.OBSIDIAN,
                Material.BEDROCK
        };
        return materials[random.nextInt(materials.length)];
    }

    /**
     * Each modifier either changes the explosion yield (which affects the radius/force)
     * or performs an entirely different effect (like replacing blocks).
     */
    private enum ExplosionModifier {
        INCREASE {
            @Override
            float modifySize(float currentSize, Random random) {
                // Increase explosion yield by a factor between 2x and 7x.
                return currentSize * (2 + random.nextInt(6)); // random integer 2-7 inclusive
            }
        },
        DECREASE {
            @Override
            float modifySize(float currentSize, Random random) {
                // Reduce explosion yield by multiplying by a factor between 0.5 and 1.0.
                return currentSize * (0.5f + (random.nextFloat() * 0.5f));
            }
        },
        RANDOM {
            @Override
            float modifySize(float currentSize, Random random) {
                // Multiply explosion yield by a random float between 0 and 5.
                return currentSize * (random.nextFloat() * 5f);
            }
        },
        REPLACE_BLOCKS {
            @Override
            float modifySize(float currentSize, Random random) {
                // This modifier handles block replacement separately.
                // Yield is left unchanged (or set to 0 in our event handler).
                return currentSize;
            }
        };
        abstract float modifySize(float currentSize, Random random);
    }
}