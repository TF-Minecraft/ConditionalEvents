package net.tfminecraft.conditionalevents.utils;

import net.tfminecraft.conditionalevents.model.EventType;
import net.tfminecraft.conditionalevents.model.StoredVariable;
import net.tfminecraft.conditionalevents.model.internal.PostEventVariableResult;
import net.tfminecraft.conditionalevents.model.internal.VariablesProperties;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VariablesUtilsExperimental {

    private static final Pattern subVariablesPattern = Pattern.compile("\\{([^{}]+)}");


    public static String replaceAllVariablesInLine(String textLine,VariablesProperties variablesProperties){
        StringBuilder newText = new StringBuilder();

        int pos = 0;
        while(pos < textLine.length()){
            char posChar = textLine.charAt(pos);
            if(posChar == '%'){
                int indexLast = textLine.indexOf('%',pos+1);
                if(indexLast != -1){
                    String variable = textLine.substring(pos,indexLast+1);
                    String replacedSubVariables = replaceSubVariables(variable,variablesProperties);
                    String finalReplaced = VariablesUtils.replaceVariable(replacedSubVariables.substring(1,replacedSubVariables.length()-1),variablesProperties,false,true);
                    pos = indexLast;
                    if(variable.equals(finalReplaced)){
                        pos--;
                        finalReplaced = finalReplaced.substring(0,finalReplaced.length()-1);
                    }

                    pos++;
                    newText.append(finalReplaced);
                    continue;
                }
            }
            newText.append(posChar);
            pos++;
        }

        return newText.toString();
    }

    private static String replaceSubVariables(String input,VariablesProperties variablesProperties) {
        boolean parseOther = input.contains("parseother_");
        Matcher matcher = subVariablesPattern.matcher(input);

        StringBuffer buffer = new StringBuffer();
        int finds = 0;
        while (matcher.find()) {
            if(parseOther && finds >= 1){
                break;
            }
            String variable = matcher.group(1);
            String replacement = VariablesUtils.replaceVariable(variable,variablesProperties,true,true);
            if(parseOther && finds == 0){
                replacement = "{"+replacement+"}";
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));

            finds++;
        }
        matcher.appendTail(buffer);

        String replaced = buffer.toString();
        if (!parseOther && !replaced.equals(input)) {
            return replaceSubVariables(replaced,variablesProperties);
        }
        return replaced;
    }

}
