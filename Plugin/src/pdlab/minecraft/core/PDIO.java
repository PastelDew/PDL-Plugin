package pdlab.minecraft.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;
//import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.Yaml;

import net.md_5.bungee.api.chat.TextComponent;

public class PDIO {
	// Color Code begin
	// https://en.wikipedia.org/wiki/ANSI_escape_code
	
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";
	
	public static final String ANSI_BLACK_BACKGROUND = "\u001B[40m";
	public static final String ANSI_RED_BACKGROUND = "\u001B[41m";
	public static final String ANSI_GREEN_BACKGROUND = "\u001B[42m";
	public static final String ANSI_YELLOW_BACKGROUND = "\u001B[43m";
	public static final String ANSI_BLUE_BACKGROUND = "\u001B[44m";
	public static final String ANSI_PURPLE_BACKGROUND = "\u001B[45m";
	public static final String ANSI_CYAN_BACKGROUND = "\u001B[46m";
	public static final String ANSI_WHITE_BACKGROUND = "\u001B[47m";
	
	private static final HashMap<String, String> ANSI_CHAT_COLOR_MAP = new HashMap<>();
	
	// Color Code end
	
	private static PDCore mCore;
	private static PDIO mIO;
	private Server server;
	private Logger mLogger;
	private File mDataFolder;
	private HashMap<UUID, Boolean> debugModePerSender;
	
	private PDIO(PDCore core) {
		mCore = core;
		server = mCore.getPlugin().getServer();
		mLogger = mCore.getPlugin().getLogger();
		mDataFolder = mCore.getPlugin().getDataFolder();
		debugModePerSender = new HashMap<>();
		if(!mDataFolder.exists())
			mDataFolder.mkdirs();
		
		ANSI_CHAT_COLOR_MAP.put(ANSI_RESET, ChatColor.RESET.toString());
		ANSI_CHAT_COLOR_MAP.put(ANSI_BLACK, ChatColor.BLACK.toString());
		ANSI_CHAT_COLOR_MAP.put(ANSI_RED, ChatColor.RED.toString());
		ANSI_CHAT_COLOR_MAP.put(ANSI_GREEN, ChatColor.GREEN.toString());
		ANSI_CHAT_COLOR_MAP.put(ANSI_YELLOW, ChatColor.YELLOW.toString());
		ANSI_CHAT_COLOR_MAP.put(ANSI_BLUE, ChatColor.BLUE.toString());
		ANSI_CHAT_COLOR_MAP.put(ANSI_PURPLE, ChatColor.DARK_PURPLE.toString());
		ANSI_CHAT_COLOR_MAP.put(ANSI_CYAN, ChatColor.AQUA.toString());
		ANSI_CHAT_COLOR_MAP.put(ANSI_WHITE, ChatColor.WHITE.toString());
		
		ANSI_CHAT_COLOR_MAP.put(ANSI_BLACK_BACKGROUND, "");
		ANSI_CHAT_COLOR_MAP.put(ANSI_RED_BACKGROUND, "");
		ANSI_CHAT_COLOR_MAP.put(ANSI_GREEN_BACKGROUND, "");
		ANSI_CHAT_COLOR_MAP.put(ANSI_YELLOW_BACKGROUND, "");
		ANSI_CHAT_COLOR_MAP.put(ANSI_BLUE_BACKGROUND, "");
		ANSI_CHAT_COLOR_MAP.put(ANSI_PURPLE_BACKGROUND, "");
		ANSI_CHAT_COLOR_MAP.put(ANSI_CYAN_BACKGROUND, "");
		ANSI_CHAT_COLOR_MAP.put(ANSI_WHITE_BACKGROUND, "");
	}
	
	public static PDIO getInstance() {
		return mIO;
	}
	
	public static PDIO getInstance(PDCore core) {
		if(mIO == null) mIO = new PDIO(core);
		return mIO;
	}
	
	public File getSubDirectory(String name) {
		File subDir = new File(mDataFolder.getPath() + "/" + name);
		if(!subDir.exists())
			subDir.mkdirs();
		return subDir;
	}
	
	public void print(String str) {
		String msg = str + ANSI_RESET;
		this.sendMessageToUsers(msg);
		mLogger.fine(msg);
	}
	
	public void warn(String str) {
		String msg = ANSI_YELLOW + str + ANSI_RESET;
		this.sendMessageToUsers(msg);
		mLogger.warning(msg);
	}
	
	public void error(String str) {
		String msg = ANSI_RED + str + ANSI_RESET;
		this.sendMessageToUsers(msg);
		mLogger.warning(msg);
	}
	
	public void info(String str) {
		String msg = ANSI_GREEN + str + ANSI_RESET;
		this.sendMessageToUsers(msg);
		mLogger.info(msg);
	}
	
	public void debug(String str) {
		String msg = ANSI_CYAN + str + ANSI_RESET;
		this.sendMessageToUsers(msg);
		mLogger.info(msg);
	}
	
	public void setDebugForUser(Player user, boolean flag) {
		debugModePerSender.put(user.getUniqueId(), flag);
	}
	
	private void sendMessageToUsers(String msg) {
		msg = convertANSIToChatColor(msg);
		for(UUID uuid : debugModePerSender.keySet()) {
			if(!debugModePerSender.get(uuid)) continue;
			Player player = server.getPlayer(uuid);
			if(player.isOnline()) {
				player.spigot().sendMessage(new TextComponent(msg));
			}
		}
	}
	
	private String convertANSIToChatColor(String str) {
		for(String ansi : ANSI_CHAT_COLOR_MAP.keySet()) {
			str = str.replace(ansi, ANSI_CHAT_COLOR_MAP.get(ansi));
		}
		return str;
	}
	
	@SuppressWarnings("unchecked")
	public static JSONObject readYMLwithJSON(String path, String ymlName) throws IOException {
		String servicePath = "jar:file:/" + path.replace("\\", "/") + "!/" + ymlName;
		URL inputURL = new URL(servicePath);
		JarURLConnection conn = (JarURLConnection)inputURL.openConnection();
		if(conn.getContentLength() < 0) return null;
		
		InputStream is = conn.getInputStream();
		Yaml yaml = new Yaml();
		Map<String, Object> map = (Map<String, Object>) yaml.load(is);
		is.close();
		return new JSONObject(map);
	}
	
	public static File getPluginJar() {
		return new File(PDIO.class.getProtectionDomain()
				  .getCodeSource()
				  .getLocation()
				  .getPath());
	}
}
