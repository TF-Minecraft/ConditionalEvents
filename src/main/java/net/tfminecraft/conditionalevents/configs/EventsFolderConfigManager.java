package net.tfminecraft.conditionalevents.configs;


import net.tfminecraft.conditionalevents.ConditionalEvents;
import net.tfminecraft.conditionalevents.configs.model.CommonConfig;

public class EventsFolderConfigManager extends DataFolderConfigManager{

    public EventsFolderConfigManager(ConditionalEvents plugin, String folderName) {
        super(plugin, folderName);
    }

    @Override
    public void createFiles() {
        new CommonConfig("more_events.yml",plugin,folderName,false).registerConfig();
    }

    @Override
    public void loadConfigs() {

    }

    @Override
    public void saveConfigs() {

    }


}
