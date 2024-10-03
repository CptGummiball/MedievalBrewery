package com.huskydreaming.medieval.brewery;

import com.huskydreaming.medieval.brewery.handlers.implementations.BreweryHandlerImpl;
import com.huskydreaming.medieval.brewery.handlers.implementations.DependencyHandlerImpl;
import com.huskydreaming.medieval.brewery.handlers.interfaces.BreweryHandler;
import com.huskydreaming.medieval.brewery.handlers.interfaces.DependencyHandler;
import com.huskydreaming.medieval.brewery.listeners.BlockListener;
import com.huskydreaming.medieval.brewery.listeners.EntityListener;
import com.huskydreaming.medieval.brewery.listeners.InventoryListener;
import com.huskydreaming.medieval.brewery.listeners.PlayerListener;
import com.huskydreaming.medieval.brewery.repositories.implementations.BreweryRepositoryImpl;
import com.huskydreaming.medieval.brewery.repositories.implementations.RecipeRepositoryImpl;
import com.huskydreaming.medieval.brewery.repositories.interfaces.BreweryRepository;
import com.huskydreaming.medieval.brewery.repositories.interfaces.RecipeRepository;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MedievalBreweryPlugin extends JavaPlugin {

    private static NamespacedKey namespacedKey;

    private BreweryRepository breweryRepository;
    private RecipeRepository recipeRepository;

    private BreweryHandler breweryHandler;
    private DependencyHandler dependencyHandler;

    @Override
    public void onEnable() {
        namespacedKey = new NamespacedKey(this, "MedievalBrewery");

        recipeRepository = new RecipeRepositoryImpl();
        recipeRepository.deserialize(this);

        breweryRepository = new BreweryRepositoryImpl();
        breweryRepository.deserialize(this);

        breweryHandler = new BreweryHandlerImpl(this);
        breweryHandler.initialize(this);
        breweryHandler.run(this);

        dependencyHandler = new DependencyHandlerImpl();
        dependencyHandler.initialize(this);

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new BlockListener(this), this);
        pluginManager.registerEvents(new EntityListener(), this);
        pluginManager.registerEvents(new InventoryListener(this), this);
        pluginManager.registerEvents(new PlayerListener(this), this);
    }

    @Override
    public void onDisable() {
        breweryHandler.finalize(this);
        breweryRepository.serialize(this);
    }

    public static NamespacedKey getNamespacedKey() {
        return namespacedKey;
    }

    public BreweryRepository getBreweryRepository() {
        return breweryRepository;
    }

    public RecipeRepository getRecipeRepository() {
        return recipeRepository;
    }

    public DependencyHandler getDependencyHandler() {
        return dependencyHandler;
    }
}