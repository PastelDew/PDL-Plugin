package pdlab.minecraft.core;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public abstract class CommandWrapper extends Command{

	public CommandWrapper(String name) { super(name); }

	@Override public abstract boolean execute(CommandSender sender, String label, String[] args);

}
