package net.tfminecraft.conditionalevents.configs;

import net.tfminecraft.conditionalevents.ConditionalEvents;
import net.tfminecraft.conditionalevents.configs.model.CommonConfig;
import net.tfminecraft.conditionalevents.model.player.EventData;
import net.tfminecraft.conditionalevents.model.player.GenericCallback;
import net.tfminecraft.conditionalevents.model.player.PlayerData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayersConfigsManager extends DataFolderConfigManager{
	
	public PlayersConfigsManager(ConditionalEvents plugin, String folderName) {
		super(plugin, folderName);
	}

	@Override
	public void createFiles() {

	}

	@Override
	public void loadConfigs() {
		// No use for player config
	}

	public PlayerData loadConfig(UUID uuid){
		PlayerData playerData = null;
		CommonConfig playerConfig = getConfigFile(uuid+".yml",false);
		if(playerConfig != null){
			playerData = read(uuid,playerConfig.getConfig());
		}

		return playerData;
	}

	public void loadConfigAsync(UUID uuid, GenericCallback<PlayerData> callback){
		new BukkitRunnable(){
			@Override
			public void run() {
				PlayerData playerData = null;
				CommonConfig playerConfig = getConfigFile(uuid+".yml",false);
				if(playerConfig != null){
					playerData = read(uuid,playerConfig.getConfig());
				}

				PlayerData finalPlayer = playerData;

				new BukkitRunnable(){
					@Override
					public void run() {
						callback.onDone(finalPlayer);
					}
				}.runTask(plugin);
			}
		}.runTaskAsynchronously(plugin);
	}

	/** Account events under {@code events}, per-character events under {@code characters.<character>.events}. */
	static PlayerData read(UUID uuid, ConfigurationSection config){
		PlayerData playerData = new PlayerData(uuid,config.getString("name"));
		playerData.setEventData(readEvents(config.getConfigurationSection("events")));
		ConfigurationSection characters = config.getConfigurationSection("characters");
		if(characters != null){
			for(String key : characters.getKeys(false)){
				try{
					playerData.setCharacterEventData(UUID.fromString(key),readEvents(characters.getConfigurationSection(key+".events")));
				}catch(IllegalArgumentException notCharacter){
					// Not a character ID.
				}
			}
		}
		return playerData;
	}

	private static ArrayList<EventData> readEvents(ConfigurationSection events){
		ArrayList<EventData> eventData = new ArrayList<>();
		if(events != null){
			for(String key : events.getKeys(false)){
				eventData.add(new EventData(key,events.getLong(key+".cooldown"),events.getBoolean(key+".one_time")));
			}
		}
		return eventData;
	}

	static void write(PlayerData.Snapshot player, ConfigurationSection players){
		players.set("name", player.name());
		players.set("events", null);
		players.set("characters", null);
		writeEvents(players, "events", player.account());
		for(Map.Entry<UUID, List<EventData>> character : player.characters().entrySet()){
			writeEvents(players, "characters."+character.getKey()+".events", character.getValue());
		}
	}

	private static void writeEvents(ConfigurationSection players, String path, List<EventData> events){
		for(EventData event : events){
			players.set(path+"."+event.getName()+".one_time", event.isOneTime());
			players.set(path+"."+event.getName()+".cooldown", event.getCooldown());
		}
	}

	/** Writes a snapshot; changes made meanwhile keep the data marked modified for the next save. */
	public void saveConfig(PlayerData player){
		synchronized(player.saveLock()){
			PlayerData.Snapshot snapshot = player.snapshot();
			CommonConfig playerConfig = getConfigFile(player.getUuid().toString()+".yml",true);
			write(snapshot, playerConfig.getConfig());
			playerConfig.saveConfig();
			player.markSaved(snapshot.revision());
		}
	}

	@Override
	public void saveConfigs() {
		Map<UUID, PlayerData> players = plugin.getPlayerManager().getPlayers();
		for(Map.Entry<UUID, PlayerData> entry : players.entrySet()) {
			PlayerData playerData = entry.getValue();
			if(playerData.isModified()){
				saveConfig(playerData);
			}
		}
	}

	public void resetDataForAllPlayers(String eventName){
		ArrayList<CommonConfig> configs = getConfigs();
		for(CommonConfig commonConfig : configs) {
			if(reset(commonConfig.getConfig(), eventName)){
				commonConfig.saveConfig();
			}
		}
	}

	/** Clears the event, or all events, for the account and its characters. Returns whether anything changed. */
	static boolean reset(ConfigurationSection config, String eventName){
		List<String> paths = new ArrayList<>();
		paths.add("events");
		ConfigurationSection characters = config.getConfigurationSection("characters");
		if(characters != null){
			for(String key : characters.getKeys(false)){
				paths.add("characters."+key+".events");
			}
		}
		boolean changed = false;
		for(String path : paths){
			String target = eventName.equals("all") ? path : path+"."+eventName;
			if(config.contains(target)){
				config.set(target,null);
				changed = true;
			}
		}
		return changed;
	}
}
