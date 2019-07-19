package pdlab.minecraft.core;

import java.nio.file.NoSuchFileException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;

import pdlab.minecraft.debug.PDLException;
import pdlab.minecraft.services.ServiceInfo;
import pdlab.minecraft.services.ServiceManager;
import pdlab.minecraft.services.ServiceManager.ServiceStatus;
import pdlab.minecraft.update.UpdateManager;
import pdlab.minecraft.update.UpdateManager.ServiceInstallationStatus;

public class CommandExecutor implements org.bukkit.command.CommandExecutor {
	private PDCore mCore;
	private PDIO mIO;
	private ServiceManager mServiceManager;
	
	public CommandExecutor(PDCore core) {
		mCore = core;
		mIO = mCore.getIO();
		mServiceManager = mCore.getServiceManager();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(mCore.notifyOnCommand(sender, command, label, args))
			return true;
		try {
			if(args.length == 0) {
				List<String> helpList = this.getHelp(1);
				for(String help : helpList)
					sender.sendMessage(help);
				return true;
			}
			
			for(int i = 0; i < args.length - 1; i++)
				args[i] = args[i].toLowerCase();
			
			mIO.info("Commander[" + sender.getName() + "] cmd: " + String.join(" ", args));
			if(!sender.hasPermission("op"))
				throw new PDLException(PDLException.ErrorInfo.NOT_PERMITTED);
			switch(args[0]){
			case "help":
				List<String> helpList = null;
				if(args.length >= 2 && args[1].matches("\\d+")) {
					helpList = this.getHelp(Integer.parseInt(args[1]));
				} else helpList = this.getHelp(1);
				for(String help : helpList)
					sender.sendMessage(help);
				return true;
			case "debug":
				if(args.length < 2 || !(sender instanceof Player))
					return false;
				boolean flag = false;
				if(args[1].equals("on"))
					flag = true;
				mIO.setDebugForUser((Player) sender, flag);
				sender.sendMessage(ChatColor.GREEN + "DEBUG MODE: " + (flag ? "on" : "off"));
				mIO.debug("Debug " + (flag ? "on" : "off") + ": " + sender.getName());
			case "services":
				try {
					HashMap<String, ServiceStatus> map = mServiceManager.getServiceStatuses();
					sender.sendMessage(ChatColor.GOLD + " -- " + ChatColor.YELLOW + "Services status" + ChatColor.GOLD + " -- ");
					for(String serviceName : map.keySet()) {
						String msg = ChatColor.GREEN + serviceName
								+ " " + ChatColor.DARK_GRAY + mServiceManager.getServiceInfo(serviceName).getVersion()
								+ ChatColor.GREEN + ": ";
						switch(map.get(serviceName)) {
						case NotAttached:
							msg += ChatColor.RED + "Not Attached";
							break;
						case Attached:
							msg += ChatColor.YELLOW + "Attached";
							break;
						case Activated:
							msg += ChatColor.GREEN + "Activated";
							break;
						default:
							break;
						}
						sender.sendMessage(msg);
					}
					return true;
				} catch(Exception err) {
					sender.sendMessage(ChatColor.RED + "Error: " + err.getMessage());
					err.printStackTrace();
					return true;
				}
				
			case "service":
				if(args.length <= 2) {
					sender.sendMessage(this.getServiceHelp());
					return true;
				}
				
				this.serviceCommand(sender, args[1], args[2]);
				return true;
				
			case "update":
			{
				if(args.length <= 1) {
					sender.sendMessage(this.getUpdateHelp());
					return true;
				}
				
				UpdateManager manager = mCore.getUpdateManager();
				switch(args[1]) {
				case "help":
					sender.sendMessage(this.getUpdateHelp());
					return true;
					
				case "check":
					manager.coreUpdateCheck();
					manager.updateCheck();
					return true;
					
				case "status":
					HashMap<UpdateManager.ServiceSummary, UpdateManager.ServiceInstallationStatus> services = manager.getAllServices();
					String sendMsg = ChatColor.GOLD + "-------------" + ChatColor.YELLOW + " All Services Status " + ChatColor.GOLD + "-------------\n";
					for(UpdateManager.ServiceSummary summary : services.keySet()) {
						sendMsg += ChatColor.AQUA + summary.getLabel() + ChatColor.DARK_AQUA + "(" + summary.getName() + ")" +
								ChatColor.WHITE +  " - ";
						switch(services.get(summary)) {
						case Installed:
							sendMsg += ChatColor.GREEN + "Installed\n";
							break;
						case NotInstalled:
							sendMsg += ChatColor.DARK_GRAY + "Not Installed\n";
							break;
						case NotSupported:
							sendMsg += ChatColor.YELLOW + "" + ChatColor.ITALIC + ChatColor.STRIKETHROUGH + "Not Supported\n";
							break;
						default:
							break;
						}
					}
					sender.sendMessage(sendMsg);
					return true;
					
				case "service":
					if(args.length != 3) return false;
					String serviceName = args[2];
					ServiceInfo serviceInfo = mServiceManager.getServiceInfo(serviceName);
					JSONObject obj = manager.getUpdateInfo(serviceInfo);
					if(!(boolean)obj.get("updatable")) {
						sender.sendMessage(ChatColor.RED + "This service is already up-to-date.");
						return true;
					}
					
					manager.updateService(serviceInfo, obj.get("updateVersion").toString());
					sender.sendMessage(ChatColor.GREEN + "Update processing finished.");
					return true;
					
				case "services":
					manager.updateCheck(true);
					return true;
					
				default:
					sender.sendMessage(this.getUpdateHelp());
					return true;
				}
			}
			case "install":
			{
				if(args.length < 2) return false;
				switch(args[1]) {
				case "service":
					if(args.length < 4) return false;
					String serviceLabel = args[2];
					String serviceVersion = args[3];
					UpdateManager manager = mCore.getUpdateManager();
					HashMap<UpdateManager.ServiceSummary, UpdateManager.ServiceInstallationStatus> services = manager.getAllServices();
					for(UpdateManager.ServiceSummary summary : services.keySet()) {
						UpdateManager.ServiceInstallationStatus status = services.get(summary);
						if(summary.getLabel().equals(serviceLabel)) {
							if(status == ServiceInstallationStatus.Installed) {
								sender.sendMessage(ChatColor.YELLOW + "Already installed.");
								return true;
							}else if(status == ServiceInstallationStatus.NotSupported) {
								sender.sendMessage(ChatColor.YELLOW + "Not supported service.");
								return true;
							}
							if(manager.installService(serviceLabel, serviceVersion))
								sender.sendMessage(ChatColor.GREEN + "Successfully installed.");
							else
								sender.sendMessage(ChatColor.RED + "Installation was failed.");
							return true;
						}
					}
					sender.sendMessage(ChatColor.YELLOW + "Cannot find that service.");
					return true;
				}
			}
			case "uninstall":
			{
				if(args.length < 2) return false;
				switch(args[1]) {
				case "service":
					if(args.length < 3) return false;
					String serviceLabel = args[2];
					UpdateManager manager = mCore.getUpdateManager();
					HashMap<UpdateManager.ServiceSummary, UpdateManager.ServiceInstallationStatus> services = manager.getAllServices();
					for(UpdateManager.ServiceSummary summary : services.keySet()) {
						UpdateManager.ServiceInstallationStatus status = services.get(summary);
						if(summary.getLabel().equals(serviceLabel)) {
							if(status == ServiceInstallationStatus.Installed || status == ServiceInstallationStatus.NotSupported) {
								ServiceInfo info = mServiceManager.getServiceInfo(summary.getName());
								manager.uninstallService(info);
								sender.sendMessage(ChatColor.GREEN + "Uninstall processing finished.");
								return true;
							} else {
								sender.sendMessage(ChatColor.YELLOW + "That service is not installed.");
								return true;
							}
						}
					}
					sender.sendMessage(ChatColor.YELLOW + "Cannot find that service.");
					return true;
				}
			}
			}
		} catch(Exception | AssertionError e) {
			sender.sendMessage(ChatColor.RED + "Error: " + e.getMessage());
			mIO.error(e.getMessage());
			e.printStackTrace();
			return true;
		}
		
		return false;
	}
	
	private void serviceCommand(CommandSender sender, String cmd, String serviceName) {
		try {
			String msg = ChatColor.DARK_GREEN + "The " + ChatColor.GREEN + serviceName + ChatColor.DARK_GREEN + " service ";
			switch(cmd) {
			case "attach":
				mServiceManager.attach(serviceName);
				sender.sendMessage(msg + "was attached.");
				break;
			case "activate":
				mServiceManager.activate(serviceName);
				sender.sendMessage(msg + "was activated.");
				break;
			case "deactivate":
				if(mServiceManager.deactivate(serviceName))
					sender.sendMessage(msg + "was deactivated.");
				else
					sender.sendMessage(msg + "was " + ChatColor.RED + "not deactivated.");
				break;
			case "detach":
				mServiceManager.detach(serviceName);
				sender.sendMessage(msg + "was detached.");
				break;
			case "reload":
				ServiceManager.ServiceStatus status = mServiceManager.getServiceStatus(mServiceManager.getServiceInfo(serviceName));
				mServiceManager.detach(serviceName);
				mServiceManager.attach(serviceName);
				if(status == ServiceStatus.Activated)
					mServiceManager.activate(serviceName);
				sender.sendMessage(msg + "was reloaded.");
				break;
			case "help":
			default:
				sender.sendMessage(getServiceHelp());
				break;
			}
			
		} catch(NoSuchFileException | NullPointerException e) {
			sender.sendMessage(ChatColor.RED + "Error: Not found the service '"+ serviceName +"'");
		} catch(Exception err) {
			sender.sendMessage(ChatColor.RED + "Error: " + err.getMessage());
			err.printStackTrace();
		}
	}
	
	private List<String> getHelp(int page){
		if(page < 1) page = 1;
		List<String> helpList = new LinkedList<>();
		helpList.add(ChatColor.DARK_GREEN + "/pdl help" + ChatColor.WHITE + ": Show Help.");
		helpList.add(ChatColor.DARK_GREEN + "/pdl services" + ChatColor.WHITE + ": Show informations of each services.");
		helpList.add(ChatColor.DARK_GREEN + "/pdl service help" + ChatColor.WHITE + ": Show help of service command.");
		helpList.add(ChatColor.DARK_GREEN + "/pdl update help" + ChatColor.WHITE + ": Show help of service command.");
		helpList.add(ChatColor.DARK_GREEN + "/pdl install service <service_label> <ServiceVersion>" + ChatColor.WHITE + ": Install Service");
		helpList.add(ChatColor.DARK_GREEN + "/pdl uninstall service <service_label>" + ChatColor.WHITE + ": Uninstall Service");
		helpList.add(ChatColor.DARK_GREEN + "/pdl debug <on/off>" + ChatColor.WHITE + ": Switch on/off debug mode.");
		for(ServiceInfo info : mServiceManager.getAttachedServices()) {
			List<String> help = info.getService().getCommandHelp();
			if(help != null) helpList.addAll(help);
		}
		
		int totalPage = (int)Math.ceil(helpList.size() / (float) 8);
		if(page > totalPage) page = totalPage;
		
		List<String> finalHelpList = new LinkedList<>();
		finalHelpList.add(ChatColor.GOLD + "-------------" + ChatColor.YELLOW + " PDL-Plugin Help [" + page + "/" + totalPage + "] " + ChatColor.GOLD + "-------------");
		totalPage = page * 8;
		page = (page - 1) * 8;
		for(int i = page; i < totalPage && i < helpList.size(); i++)
			finalHelpList.add(helpList.get(i));
		helpList.clear();
		return finalHelpList;
	}
	
	private String getServiceHelp() {
		return ChatColor.GOLD + "-------------" + ChatColor.YELLOW + " Service Help " + ChatColor.GOLD + "-------------\n"
				+ ChatColor.DARK_GREEN + "/pdl service attach <ServiceName>" + ChatColor.WHITE + ": Attach the '<ServiceName>' service to the server.\n"
				+ ChatColor.DARK_GREEN + "/pdl service detach <ServiceName>" + ChatColor.WHITE + ": Detach the '<ServiceName>' service to the server.\n"
				+ ChatColor.DARK_GREEN + "/pdl service activate <ServiceName>" + ChatColor.WHITE + ": Activate the '<ServiceName>' service.\n"
				+ ChatColor.DARK_GREEN + "/pdl service deactivate <ServiceName>" + ChatColor.WHITE + ": Deactivate the '<ServiceName>' service.\n"
				+ ChatColor.DARK_GREEN + "/pdl service reload <ServiceName>" + ChatColor.WHITE + ": Re-attach the '<ServiceName>' service after detach.\n";
	}
	
	private String getUpdateHelp() {
		return ChatColor.GOLD + "-------------" + ChatColor.YELLOW + " Update Help " + ChatColor.GOLD + "-------------\n"
				+ ChatColor.DARK_GREEN + "/pdl update status" + ChatColor.WHITE + ": Show all services(+ Service Server).\n"
				+ ChatColor.DARK_GREEN + "/pdl update check" + ChatColor.WHITE + ": Check all update availables.\n"
				+ ChatColor.DARK_GREEN + "/pdl update services" + ChatColor.WHITE + ": Update all services after update check process.\n"
				+ ChatColor.DARK_GREEN + "/pdl update service <ServiceName>" + ChatColor.WHITE + ": Update <ServiceName> service.\n";
	}

}
