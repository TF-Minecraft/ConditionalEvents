package net.tfminecraft.conditionalevents;

import net.tfminecraft.conditionalevents.character.CharacterTracker;
import net.tfminecraft.conditionalevents.character.LegacyRecipient;
import net.tfminecraft.conditionalevents.model.CEEvent;
import net.tfminecraft.conditionalevents.api.ConditionalEventsAPI;
import net.tfminecraft.conditionalevents.api.ExpansionCE;
import net.tfminecraft.conditionalevents.configs.ConfigsManager;
import net.tfminecraft.conditionalevents.libs.armorequipevent.ArmorListener;
import net.tfminecraft.conditionalevents.libs.itemselectevent.ItemSelectListener;
import net.tfminecraft.conditionalevents.libs.itemselectevent.ItemSelectListenerNew;
import net.tfminecraft.conditionalevents.libs.offhandevent.OffHandListener;
import net.tfminecraft.conditionalevents.libs.repairevent.RepairListener;
import net.tfminecraft.conditionalevents.listeners.*;
import net.tfminecraft.conditionalevents.listeners.dependencies.CitizensListener;
import net.tfminecraft.conditionalevents.listeners.dependencies.WGRegionEventsListener;
import net.tfminecraft.conditionalevents.managers.*;
import net.tfminecraft.conditionalevents.managers.commandregister.CommandRegisterManager;
import net.tfminecraft.conditionalevents.model.EventType;
import net.tfminecraft.conditionalevents.model.internal.ConditionEvent;
import net.tfminecraft.conditionalevents.tasks.PlayerDataSaveTask;
import net.tfminecraft.conditionalevents.utils.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.ArrayList;

public class ConditionalEvents extends JavaPlugin {
  
	PluginDescriptionFile pdfFile = getDescription();
	public String version = pdfFile.getVersion();
	public static ServerVersion serverVersion;
	public static String prefix;

	private EventsManager eventsManager;
	private DependencyManager dependencyManager;
	private PlayerManager playerManager;
	private CharacterTracker characterTracker;
	private ConfigsManager configsManager;
	private DebugManager debugManager;
	private BungeeMessagingManager bungeeMessagingManager;
	private MessagesManager messagesManager;
	private VerifyManager verifyManager;
	private CommandRegisterManager commandRegisterManager;
	private SavedItemsManager savedItemsManager;
	private APIManager apiManager;
	private InterruptEventManager interruptEventManager;

	private PlayerDataSaveTask playerDataSaveTask;

	
	public void onEnable(){
		setVersion();
		setPrefix();

		this.eventsManager = new EventsManager(this);
		this.dependencyManager = new DependencyManager(this);
		this.bungeeMessagingManager = new BungeeMessagingManager(this);
		this.debugManager = new DebugManager(this);
		this.playerManager =  new PlayerManager(this);
		this.savedItemsManager = new SavedItemsManager(this);
		this.apiManager = new APIManager(this);
		this.interruptEventManager = new InterruptEventManager(this);
		this.configsManager = new ConfigsManager(this);
		this.configsManager.configure();
		this.characterTracker = new CharacterTracker(playerManager, name -> {
			CEEvent event = eventsManager.getEvent(name);
			return event != null && event.usesCharacterData();
		}, new LegacyRecipient(getDataFolder().getParentFile().toPath()), new OneTimeSharingManager(this), getLogger());

		registerEvents();
		registerCommands();

		this.verifyManager = new VerifyManager(this);
		this.verifyManager.verifyEvents();

		this.commandRegisterManager = new CommandRegisterManager(this);
		commandRegisterManager.registerCommands();



		characterTracker.start();
		reloadPlayerDataSaveTask();

		ConditionalEventsAPI api = new ConditionalEventsAPI(this);
		if(getServer().getPluginManager().getPlugin("PlaceholderAPI") != null){
			new ExpansionCE(this).register();
		}

		Bukkit.getConsoleSender().sendMessage(MessagesManager.getLegacyColoredMessage(prefix+" &eHas been enabled! &fVersion: "+version));

		new ConditionEvent(this, null, null, EventType.SERVER_START, null)
				.checkEvent();
	}
	  
	public void onDisable(){
		new ConditionEvent(this, null, null, EventType.SERVER_STOP, null)
				.checkEvent();
		this.configsManager.getPlayerConfigsManager().saveConfigs();
		Bukkit.getConsoleSender().sendMessage(MessagesManager.getLegacyColoredMessage(prefix+" &eHas been disabled! &fVersion: "+version));
	}

	public void registerEvents() {
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(new PlayerEventsListener(this), this);
		pm.registerEvents(characterTracker, this);
		pm.registerEvents(new ItemEventsListener(this), this);
		pm.registerEvents(new ArmorListener(new ArrayList<>()), this);
		pm.registerEvents(new ItemSelectListener(this), this);
		pm.registerEvents(new OtherEventsListener(this), this);
		pm.registerEvents(new CustomEventListener(this), this);

		if(serverVersion.serverVersionGreaterEqualThan(serverVersion,ServerVersion.v1_9_R1)){
			pm.registerEvents(new ItemSelectListenerNew(), this);
			pm.registerEvents(new PlayerEventsListenerNew1_9(this), this);
			pm.registerEvents(new OffHandListener(this), this);
		}
		if(serverVersion.serverVersionGreaterEqualThan(serverVersion,ServerVersion.v1_16_R3)){
			pm.registerEvents(new RepairListener(this), this);
		}
		if(serverVersion.serverVersionGreaterEqualThan(serverVersion,ServerVersion.v1_16_R1)){
			pm.registerEvents(new PlayerEventsListenerNew1_16(this), this);
		}

		if(dependencyManager.isCitizens()){
			pm.registerEvents(new CitizensListener(this), this);
		}
		if(dependencyManager.isWorldGuardEvents()){
			pm.registerEvents(new WGRegionEventsListener(this), this);
		}
		if(dependencyManager.isPaper()){
			if(serverVersion.serverVersionGreaterEqualThan(serverVersion,ServerVersion.v1_12_R1)){
				pm.registerEvents(new PaperEventsListener(this), this);
			}
		}
	}

	public void setPrefix(){
		prefix = MessagesManager.getLegacyColoredMessage("&4[&bConditionalEvents&4]");
	}

	public void setVersion(){
		String packageName = Bukkit.getServer().getClass().getPackage().getName();
		String minecraftVersion = ServerVersion.getMinecraftVersion();
		switch(minecraftVersion){
			case "1.20.5":
			case "1.20.6":
				serverVersion = ServerVersion.v1_20_R4;
				break;
			case "1.21":
			case "1.21.1":
				serverVersion = ServerVersion.v1_21_R1;
				break;
			case "1.21.2":
			case "1.21.3":
				serverVersion = ServerVersion.v1_21_R2;
				break;
			case "1.21.4":
				serverVersion = ServerVersion.v1_21_R3;
				break;
			case "1.21.5":
				serverVersion = ServerVersion.v1_21_R4;
				break;
			case "1.21.6":
			case "1.21.7":
			case "1.21.8":
				serverVersion = ServerVersion.v1_21_R5;
				break;
			case "1.21.9":
			case "1.21.10":
				serverVersion = ServerVersion.v1_21_R6;
				break;
			case "1.21.11":
				serverVersion = ServerVersion.v1_21_R7;
				break;
			case "26.1":
			case "26.1.1":
			case "26.1.2":
				serverVersion = ServerVersion.v26_1;
				break;
			case "26.2":
				serverVersion = ServerVersion.v26_2;
				break;
			case "26.3":
				serverVersion = ServerVersion.v26_3;
				break;
			default:
				try{
					serverVersion = ServerVersion.valueOf(packageName.replace("org.bukkit.craftbukkit.", ""));
				}catch(Exception e){
					serverVersion = ServerVersion.v26_3;
				}
		}
	}

	public void reloadEvents(){
		HandlerList.unregisterAll(this);
		registerEvents();
	}

	public void reloadPlayerDataSaveTask() {
		if(playerDataSaveTask != null) {
			playerDataSaveTask.end();
		}
		playerDataSaveTask = new PlayerDataSaveTask(this);
		playerDataSaveTask.start(configsManager.getMainConfigManager().getConfig().getInt("Config.data_save_time"));
	}

	public void registerCommands(){
		this.getCommand("conditionalevents").setExecutor(new MainCommand(this));
	}

	public EventsManager getEventsManager() {
		return eventsManager;
	}

	public DependencyManager getDependencyManager() {
		return dependencyManager;
	}

	public ConfigsManager getConfigsManager() {
		return configsManager;
	}

	public DebugManager getDebugManager() {
		return debugManager;
	}

	public BungeeMessagingManager getBungeeMessagingManager() {
		return bungeeMessagingManager;
	}

	public CharacterTracker getCharacterTracker() {
		return characterTracker;
	}

	public PlayerManager getPlayerManager() {
		return playerManager;
	}

	public MessagesManager getMessagesManager() {
		return messagesManager;
	}

	public void setMessagesManager(MessagesManager messagesManager) {
		this.messagesManager = messagesManager;
	}

	public VerifyManager getVerifyManager() {
		return verifyManager;
	}

	public CommandRegisterManager getCommandRegisterManager() {
		return commandRegisterManager;
	}

	public APIManager getApiManager() {
		return apiManager;
	}

	public SavedItemsManager getSavedItemsManager() {
		return savedItemsManager;
	}

	public InterruptEventManager getInterruptEventManager() {
		return interruptEventManager;
	}


}
