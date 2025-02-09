/*
    @MAIN-TITLE: ChaosCraft.java
    @MAIN-DESCRIPTION: This plugin randomly modifies variables that are generally
    at a fixed rate in the vanilla version of Minecraft. With modified fixed variables
    becoming random, this will create a very interesting gameplay play-through.
 */

package org.im4ever12c.chaoscraft;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.im4ever12c.chaoscraft.listeners.*;

public final class ChaosCraft extends JavaPlugin {

    @Override
    public void onEnable() {
        initializeListeners();
    }

    private void initializeListeners() {
        PluginManager manager = getServer().getPluginManager();
        manager.registerEvents(new ExplosionEvents(), this);
        manager.registerEvents(new TimeSkipEvents(), this);
        manager.registerEvents(new BreakBlockEvent(), this);
        manager.registerEvents(new EntitySpawnEvents(), this);
        manager.registerEvents(new EntityEvents(), this);
        manager.registerEvents(new ProjectileFireEvents(this), this);
    }
}