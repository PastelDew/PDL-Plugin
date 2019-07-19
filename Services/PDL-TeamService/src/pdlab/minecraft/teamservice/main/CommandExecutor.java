package pdlab.minecraft.teamservice.main;

import java.util.List;

import org.bukkit.command.CommandSender;

import pdlab.minecraft.core.CommandWrapper;

public class CommandExecutor extends CommandWrapper {

	public CommandExecutor(String name) {
		super(name);
	}

	@Override
	public boolean execute(CommandSender sender, String label, String[] args) {
		return false;
	}
	
	public List<String> getCommandHelp(){
		return null;
	}

}
