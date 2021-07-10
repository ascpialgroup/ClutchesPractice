package io.github.ascpialgroup.clp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import io.github.ascpialgroup.clp.configuration.LoadedConfiguration;
import io.github.ascpialgroup.clp.configuration.Translations;
import io.github.ascpialgroup.clp.listeners.CommandsHandler;
import io.github.ascpialgroup.clp.listeners.EventsHandlerClp;

public class ClutchesPracticePlugin extends JavaPlugin {
	private FileConfiguration config;
	private YamlConfiguration armorStandsConf;
	private LoadedConfiguration lConfig;
	private Translations lTrans;
	public ArrayList<BukkitRunnable> delayedTasks;

	private void loadArmorStandsConf() {
		getLogger().log(Level.INFO, "Loading armor stands...");
		armorStandsConf = new YamlConfiguration();

		File armorStandsFile = new File(getDataFolder(), "armorstands.yml");
		armorStandsFile.getParentFile().mkdirs();
		if (armorStandsFile.exists()) {
			try {
				armorStandsConf.load(armorStandsFile);
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "Failed to load armor stands...", e);
				return;
			}
		} else {
			getLogger().log(Level.INFO, "No need to load armor stands.");
		}
		getLogger().log(Level.INFO, "Armor stands loaded.");

	}

	private void loadTranslations() {
		getLogger().log(Level.INFO, "Parsing translations...");
		lTrans = new Translations();
		lTrans.prefix = config.getString("prefix").replace('&', '�');
		lTrans.noPermissions = lTrans.prefix + config.getString("no-permission");
		lTrans.consoleSummoning = config.getString("console-summoning");
		lTrans.punchMe = config.getString("punch-me");
		lTrans.helperAdded = lTrans.prefix + config.getString("helper-added");
		lTrans.helperRemoved = lTrans.prefix + config.getString("helper-removed");
		lTrans.nonActiveWorld = lTrans.prefix + config.getString("non-active-world");
		lTrans.nearSpawnPlacing = lTrans.prefix + config.getString("near-spawn-placing");
		lTrans.tryAgain = lTrans.prefix + config.getString("try-again");
		getLogger().log(Level.INFO, "Translations parsed.");
	}

	private void parseConfiguration() {
		getLogger().log(Level.INFO, "Parsing configuration...");
		lConfig = new LoadedConfiguration();
		lConfig.activeWorlds = config.getStringList("active-worlds");
		lConfig.preventPlayerDamages = config.getBoolean("prevent-player-damages");
		lConfig.failedHeight = config.getDouble("height-when-failed");
		lConfig.deletionDelayTicks = config.getInt("time-before-block-deletes");
		lConfig.firstKnockbackDelayTicks = config.getInt("time-before-first-knockback");
		lConfig.cancelWeatherChanges = config.getBoolean("cancel-weather-changes");
		lConfig.knockbackForce = config.getDouble("knockback-force");
		lConfig.jumpForce = config.getDouble("jump-force");
		lConfig.clutchBlockType = Material.valueOf(config.getString("clutch-block"));
		lConfig.clutchBlocksAmount = config.getInt("clutch-blocks-amount");
		lConfig.blocksAllowed = config.getInt("blocks-allowed");
		lConfig.commandsToExecJoin = config.getStringList("command-to-exec-joining");
		lConfig.commandsToExecLeave = config.getStringList("command-to-exec-leaving");
		getLogger().log(Level.INFO, "Configuration parsed.");

	}

	private void saveArmorStandsConf() {
		getLogger().log(Level.INFO, "Saving armor stands...");
		File armorStandsFile = new File(getDataFolder(), "armorstands.yml");
		armorStandsFile.getParentFile().mkdirs();
		try {
			armorStandsConf.save(armorStandsFile);
		} catch (IOException e) {
			getLogger().log(Level.WARNING, "Failed to save armor stands...", e);
			return;
		}
		getLogger().log(Level.INFO, "Armor stands saved.");
	}

	@Override
	public void onEnable() {
		delayedTasks = new ArrayList<>();
		saveDefaultConfig();
		config = getConfig();
		parseConfiguration();
		loadTranslations();
		loadArmorStandsConf();
		cleanupUnusedRefs();
		Bukkit.getPluginManager().registerEvents(new EventsHandlerClp(this, lConfig, lTrans), this);
		getCommand("summonhelper").setExecutor(new CommandsHandler(this, lConfig, lTrans));
	}

	public void registerNewArmorStandIfNotExisting(UUID uuid) {
		List<String> armorStands = armorStandsConf.getStringList("armorStands");
		if (!isArmorStandExisting(uuid)) {
			armorStands.add(uuid.toString());
		}
		armorStandsConf.set("armorStands", armorStands);
	}

	public void cleanupUnusedRefs() {
		getLogger().log(Level.INFO, "Cleaning up non existing armor stands...");
		List<String> armorStands = armorStandsConf.getStringList("armorStands");
		List<String> armorStandsFinal = armorStandsConf.getStringList("armorStands");
		armorStands.forEach(armorStandId -> {
			boolean hasMatched[] = new boolean[1];
			getServer().getWorlds().forEach(world -> {
				world.getEntities().forEach(entity -> {
					if (entity.getUniqueId().toString().equals(armorStandId)) {
						hasMatched[0] = true;
					}
				});
			});
			if (!hasMatched[0]) {
				getLogger().log(Level.INFO,
						"The armor stand " + armorStandId + " hasn't been removed properly, cleaning it up.");
				armorStandsFinal.remove(armorStandId);
			}
		});
		armorStandsConf.set("armorStands", armorStandsFinal);
		getLogger().log(Level.INFO, "Non existing armor stands cleaned up !");

	}

	public boolean isArmorStandExisting(UUID uuid) {
		List<String> armorStands = armorStandsConf.getStringList("armorStands");
		if (armorStands.contains(uuid.toString())) {
			return true;
		}
		return false;
	}

	public void removeArmorStandIfExisting(UUID uuid) {
		List<String> armorStands = armorStandsConf.getStringList("armorStands");
		if (isArmorStandExisting(uuid)) {
			armorStands.remove(uuid.toString());
		}
		armorStandsConf.set("armorStands", armorStands);
	}

	@Override
	public void onDisable() {
		getLogger().log(Level.INFO, "Executing delayed tasks...");
		delayedTasks.forEach(task -> {
			task.cancel();
			task.run();
		});
		getLogger().log(Level.INFO, "Delayed tasks executed.");
		HandlerList.unregisterAll(this);
		saveArmorStandsConf();
	}

}
