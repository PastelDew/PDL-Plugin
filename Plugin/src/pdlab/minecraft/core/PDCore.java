package pdlab.minecraft.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import pdlab.minecraft.debug.PDLException;
import pdlab.minecraft.listeners.IPluginStateListener;
import pdlab.minecraft.services.ServiceManager;
import pdlab.minecraft.update.UpdateManager;

public class PDCore {
	private static PDCore mCore = null;
	private static PDIO mIO = null;
	private static ServiceManager mServiceManager = null;
	private static UpdateManager mUpdateManager = null;
	private static CommandExecutor mCmdExe = null;
	private JavaPlugin mPlugin;
	private FileConfiguration mConfig;
	
	private ArrayList<IPluginStateListener> mPluginListeners;
	private ArrayList<CommandExecutor> onCommandListeners;
	private ArrayList<TabCompleter> onTabCompleteListeners;
	private PluginDescriptionFile mPDFile;
	private String mVersion = null;
	private int mMajorVersion = 0;
	private int mMinorVersion = 0;
	private int mReleaseVersion = 0;
	
	private PDCore(JavaPlugin plugin) {
		mPlugin = plugin;
		mPlugin.saveDefaultConfig();
		mConfig = mPlugin.getConfig();
		mPDFile = mPlugin.getDescription();
		mVersion = mPDFile.getVersion();
		String[] versions = mVersion.split("\\.");
		mMajorVersion = Integer.parseInt(versions[0]);
		mMinorVersion = Integer.parseInt(versions[1]);
		mReleaseVersion = Integer.parseInt(versions[2]);
		mPluginListeners = new ArrayList<>();
		onCommandListeners = new ArrayList<>();
		onTabCompleteListeners = new ArrayList<>();
	}
	
	public static void initialize(JavaPlugin plugin) throws PDLException {
		if(mCore != null)
			throw new PDLException(PDLException.ErrorInfo.CORE_ALREADY_INITIALIZED);
		mCore = new PDCore(plugin);
		mIO = PDIO.getInstance(mCore);
		mServiceManager = ServiceManager.initialize(mCore);
		mUpdateManager = UpdateManager.getInstance();
		mCmdExe = new CommandExecutor(mCore);
	}
	
	public static PDCore getInstance() throws PDLException {
		if(mCore == null) throw new PDLException(PDLException.ErrorInfo.CORE_NOT_INITIALIZED);
		return mCore;
	}
	
	public JavaPlugin getPlugin() { return mPlugin; }
	public PluginDescriptionFile getPluginDescriptionFile() { return mPDFile; }
	public PDIO getIO() { return mIO; }
	public ServiceManager getServiceManager() { return mServiceManager; }
	public UpdateManager getUpdateManager() { return mUpdateManager; }
	
	public String getVersion() { return mVersion; }
	public int getMajorVersion() { return mMajorVersion; }
	public int getMinorVersion() { return mMinorVersion; }
	public int getReleaseVersion() { return mReleaseVersion; }
	
	public void addPluginStateListener(IPluginStateListener listener) {
		if(listener == null)
			mIO.error(PDLException.ErrorInfo.INVALID_ARGUMENTS + " Listener is null!");
		mPluginListeners.add(listener);
	}
	
	public void addOnCommandListener(CommandExecutor listener) {
		if(listener == null)
			mIO.error(PDLException.ErrorInfo.INVALID_ARGUMENTS + " Listener is null!");
		onCommandListeners.add(listener);
	}
	
	public void addOnTabCompleteListener(TabCompleter listener) {
		if(listener == null)
			mIO.error(PDLException.ErrorInfo.INVALID_ARGUMENTS + " Listener is null!");
		onTabCompleteListeners.add(listener);
	}
	
	public void removePluginStateListener(IPluginStateListener listener) {
		mPluginListeners.remove(listener);
	}
	
	public void removeOnCommandListener(CommandExecutor listener) {
		onCommandListeners.remove(listener);
	}
	
	public void removeOnTabCompleteListener(TabCompleter listener) {
		onTabCompleteListeners.remove(listener);
	}
	
	public boolean validateVersion(String version, String condition) {
		if(!version.matches("\\d+\\.\\d+\\.\\d+"))
			return false;
		
		String[] versions = version.split("\\.");
		switch(condition) {
		case "<":
			return Integer.parseInt(versions[0]) == this.getMajorVersion()
					&& Integer.parseInt(versions[1]) < this.getMinorVersion()
					&& Integer.parseInt(versions[2]) < this.getReleaseVersion();
			
		case "<=":
			return Integer.parseInt(versions[0]) == this.getMajorVersion()
					&& Integer.parseInt(versions[1]) <= this.getMinorVersion()
					&& Integer.parseInt(versions[2]) <= this.getReleaseVersion();
			
		case "==":
			return Integer.parseInt(versions[0]) == this.getMajorVersion()
					&& Integer.parseInt(versions[1]) == this.getMinorVersion()
					&& Integer.parseInt(versions[2]) == this.getReleaseVersion();
			
		case ">":
			return Integer.parseInt(versions[0]) == this.getMajorVersion()
					&& Integer.parseInt(versions[1]) > this.getMinorVersion()
					&& Integer.parseInt(versions[2]) > this.getReleaseVersion();
					
		case ">=":
			return Integer.parseInt(versions[0]) == this.getMajorVersion()
					&& Integer.parseInt(versions[1]) >= this.getMinorVersion()
					&& Integer.parseInt(versions[2]) >= this.getReleaseVersion();
					
		case "!=":
			return Integer.parseInt(versions[0]) == this.getMajorVersion()
					&& Integer.parseInt(versions[1]) != this.getMinorVersion()
					&& Integer.parseInt(versions[2]) != this.getReleaseVersion();
		default:
			return false;
		}
	}
	
	public void setCommand(String commandLabel, Command cmd) {
		CommandMap cmdMap = mPlugin.getServer().getCommandMap();
		cmdMap.register(commandLabel, cmd);
	}
	
	public void notifyOnLoad() {
		mUpdateManager.coreUpdateCheck();
		mUpdateManager.updateCheck();
		for(IPluginStateListener listener : mPluginListeners)
			listener.onLoad();
	}
	
	public boolean notifyOnCommand(CommandSender sender, Command command, String label, String[] args) {
		for(CommandExecutor listener : onCommandListeners)
			if(listener.onCommand(sender, command, label, args))
				return true;
		return false;
	}

	public List<String> notifyOnTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		List<String> results = new LinkedList<>();
		for(TabCompleter listener : onTabCompleteListeners)
			results.addAll(listener.onTabComplete(sender, command, alias, args));
		return results;
	}

	public void notifyOnEnable() {
		mPlugin.getCommand("pdl").setExecutor(mCmdExe);
		for(IPluginStateListener listener : mPluginListeners)
			listener.onEnable();
		mIO.info(String.format("Plugin Enabled! Name: %s, Version: %s", mPDFile.getName(), mPDFile.getVersion()));
		if(mConfig.isList("attached")) {
			List<String> attached = mConfig.getStringList("attached");
			for(String serviceName : attached) {
				try {
					mServiceManager.attach(serviceName);
					mIO.debug("PluginEnablingMsg: '" + serviceName + "' was attached.");
				} catch (Exception e) {
					e.printStackTrace();
					mIO.error("ErrMsg: " + e.getMessage());
					mIO.error("The service '" + serviceName + "' could not be loaded.");
				}
			}
		}
		if(mConfig.isList("activated")) {
			List<String> attached = mConfig.getStringList("activated");
			for(String serviceName : attached) {
				try {
					mServiceManager.activate(serviceName);
					mIO.debug("PluginEnablingMsg: '" + serviceName + "' was activated.");
				} catch (Exception e) {
					e.printStackTrace();
					mIO.error("ErrMsg: " + e.getMessage());
					mIO.error("The service '" + serviceName + "' could not be loaded.");
				}
			}
		}
	}
	
	public void notifyOnDisable() {
		for(IPluginStateListener listener : mPluginListeners)
			listener.onDisable();
		mIO.info(String.format("Plugin Disabled! Name: %s, Version: %s", mPDFile.getName(), mPDFile.getVersion()));
		
		try {
			HashMap<String, ServiceManager.ServiceStatus> statuses = mServiceManager.getServiceStatuses();
			List<String> attached = new ArrayList<>();
			List<String> activated = new ArrayList<>();
			for(String serviceName : statuses.keySet()) {
				ServiceManager.ServiceStatus status = statuses.get(serviceName);
				switch(status) {
				case Attached:
					attached.add(serviceName);
					break;
				case Activated:
					attached.add(serviceName);
					activated.add(serviceName);
					break;
				default:
					break;
				}
			}
			mConfig.set("attached", attached);
			mConfig.set("activated", activated);
			mPlugin.saveConfig();
		} catch (Exception e) {
			e.printStackTrace();
			mIO.error("ErrMsg: " + e.getMessage());
			mIO.error("Something was in problem while configuring.");
		}
		
		
		mServiceManager.detachAll();
	}
}
