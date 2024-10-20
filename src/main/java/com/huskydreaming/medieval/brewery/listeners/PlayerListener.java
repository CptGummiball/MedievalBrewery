package com.huskydreaming.medieval.brewery.listeners;

import com.huskydreaming.medieval.brewery.MedievalBreweryPlugin;
import com.huskydreaming.medieval.brewery.data.Brewery;
import com.huskydreaming.medieval.brewery.data.Effect;
import com.huskydreaming.medieval.brewery.data.Hologram;
import com.huskydreaming.medieval.brewery.data.Recipe;
import com.huskydreaming.medieval.brewery.enumerations.BreweryStatus;
import com.huskydreaming.medieval.brewery.handlers.interfaces.ConfigHandler;
import com.huskydreaming.medieval.brewery.repositories.interfaces.BreweryRepository;
import com.huskydreaming.medieval.brewery.repositories.interfaces.RecipeRepository;
import com.huskydreaming.medieval.brewery.storage.Message;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.TripwireHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;

public class PlayerListener implements Listener {

    private final BreweryRepository breweryRepository;
    private final RecipeRepository recipeRepository;

    private final ConfigHandler configHandler;

    public PlayerListener(MedievalBreweryPlugin plugin) {
        this.breweryRepository = plugin.getBreweryRepository();
        this.recipeRepository = plugin.getRecipeRepository();

        this.configHandler = plugin.getConfigHandler();
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack itemStack = event.getItem();

        ItemMeta itemMeta = itemStack.getItemMeta();
        if(itemMeta == null) return;

        NamespacedKey namespacedKey = MedievalBreweryPlugin.getNamespacedKey();
        PersistentDataContainer dataContainer = itemMeta.getPersistentDataContainer();
        if(!dataContainer.has(namespacedKey, PersistentDataType.STRING)) return;

        String data = dataContainer.get(namespacedKey, PersistentDataType.STRING);
        if(data == null) return;

        String recipeName = data;
        int multiplier = 1;

        if(configHandler.hasQualities()) {
            String[] splitData = data.split(":");
            recipeName = splitData[0];
            multiplier = Integer.parseInt(splitData[1]);
        }

        Recipe recipe = recipeRepository.getRecipe(recipeName);
        if(recipe == null) return;

        Player player = event.getPlayer();
        for(Effect effect : recipe.getEffects()) {
            PotionEffect potionEffect = effect.toPotionEffect(multiplier);
            if(potionEffect == null) continue;
            player.addPotionEffect(potionEffect);
        }
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        if (block.getType() == Material.BARREL) {
            if (!breweryRepository.isBrewery(block)) return;

            Player player = event.getPlayer();
            Brewery brewery = breweryRepository.getBrewery(block);

            if (brewery.getStatus() == BreweryStatus.BREWING) {
                player.sendMessage(Message.GENERAL_IN_PROGRESS.prefix());
                event.setCancelled(true);
                return;
            } else if (brewery.getStatus() == BreweryStatus.READY) {
                event.setCancelled(true);
            }
        }

        if (block.getType() == Material.TRIPWIRE_HOOK) {

            ItemStack itemStack = event.getItem();
            if (itemStack == null || itemStack.getType() != Material.GLASS_BOTTLE) return;

            TripwireHook tripwireHook = (TripwireHook) block.getState().getBlockData();
            BlockFace blockFace = tripwireHook.getFacing().getOppositeFace();
            Block relativeBlock = block.getRelative(blockFace);

            if (!breweryRepository.isBrewery(relativeBlock)) return;

            event.setCancelled(true);

            Brewery brewery = breweryRepository.getBrewery(relativeBlock);
            if (brewery.getStatus() == BreweryStatus.READY) {
                Hologram hologram = brewery.getHologram();
                String recipeName = brewery.getRecipeName();
                int remaining = brewery.getRemaining();

                if (remaining <= 1) {
                    hologram.update(Message.TITLE_IDLE_HEADER.parse(), Message.TITLE_IDLE_FOOTER.parse());
                    brewery.setStatus(BreweryStatus.IDLE);
                } else {
                    Recipe recipe = recipeRepository.getRecipe(recipeName);
                    int uses = recipe.getUses();
                    brewery.setRemaining(remaining -= 1);
                    String header = Message.TITLE_READY_HEADER.parameterize(recipe.getChatColor(), recipeName);
                    String footer = Message.TITLE_READY_FOOTER.parameterize(remaining, uses);
                    hologram.update(header, footer);
                }

                itemStack.setAmount(itemStack.getAmount() - 1);
                ItemStack recipeItem = recipeRepository.getRecipeItem(brewery);
                Player player = event.getPlayer();
                player.getInventory().addItem(recipeItem);

                Location location = block.getLocation();
                World world = location.getWorld();
                if (world != null) {
                    world.playSound(block.getLocation(), Sound.ENTITY_WANDERING_TRADER_DRINK_MILK, 1, 1);
                }
            }
        }
    }
}
