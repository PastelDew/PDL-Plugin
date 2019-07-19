package pdlab.minecraft.userservice.main;

import java.util.List;

import org.bukkit.Server;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import pdlab.minecraft.core.*;
import pdlab.minecraft.services.*;

public class UserService extends ServiceBase implements Listener{
	private Server mServer = null;
	private JavaPlugin mPlugin = null;
	private PluginManager mPluginManager = null;
	
	@Override
	public void onCreate(ServiceInfo info, PDCore core) {
		// TODO Auto-generated method stub
		super.onCreate(info, core);
		mPlugin = core.getPlugin();
		mServer = mPlugin.getServer();
		mPluginManager = mServer.getPluginManager();
	}

	@Override
	public void onActivated(ServiceBase service) {
		mPluginManager.registerEvents(this, mPlugin);
	}
	
	@EventHandler
	public void PlayerJoin(PlayerJoinEvent event) {
		event.getPlayer().sendMessage("UserService : Hello!!");
	}

	@Override
	public List<String> getCommandHelp() {
		// TODO Auto-generated method stub
		return null;
	}
	
}