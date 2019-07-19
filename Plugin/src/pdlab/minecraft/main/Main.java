package pdlab.minecraft.main;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import pdlab.minecraft.core.PDCore;

public class Main extends JavaPlugin {
	private PDCore mCore;
	
	public Main() {
		try {
			PDCore.initialize(this);
			mCore = PDCore.getInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		boolean result = super.onCommand(sender, command, label, args);
		return result ? true : mCore.notifyOnCommand(sender, command, label, args);
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		List<String> results = super.onTabComplete(sender, command, alias, args);
		results.addAll(mCore.notifyOnTabComplete(sender, command, alias, args));
		return results;
	}

	@Override public void onLoad() { mCore.notifyOnLoad(); }
	public void onEnable() { mCore.notifyOnEnable(); }
	public void onDisable() { mCore.notifyOnDisable(); }
}