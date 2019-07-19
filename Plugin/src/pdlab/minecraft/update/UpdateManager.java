package pdlab.minecraft.update;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.ListIterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import pdlab.minecraft.core.PDCore;
import pdlab.minecraft.core.PDIO;
import pdlab.minecraft.debug.PDLException;
import pdlab.minecraft.json.JsonReader;
import pdlab.minecraft.services.ServiceInfo;
import pdlab.minecraft.services.ServiceManager;

public class UpdateManager {
	public static enum ServiceInstallationStatus {Installed, NotInstalled, NotSupported}
	public static class ServiceSummary {
		private String label;
		private String name;
		private String version;
		public ServiceSummary(String label, String name, String version) {
			this.label = label;
			this.name = name;
			this.version = version;
		}
		
		public void setLabel(String label) { this.label = label; }
		public void setName(String name) { this.name = name; }
		public void setVersion(String version) { this.version = version; }
		
		public String getLabel() { return this.label; }
		public String getName() { return this.name; }
		public String getVersion() { return this.version; }
	}
	
	private static final String HOST_NAME = "api.pdlab.kr";
	private static final String UPDATE_PDL_API_URL = "http://" + HOST_NAME + "/MC/";
	private static UpdateManager manager = null;
	private PDCore core = null;
	private PDIO pdio = null;
	private ServiceManager srvMgr = null;
	
	private UpdateManager() throws PDLException {
		core = PDCore.getInstance();
		pdio = core.getIO();
		srvMgr = core.getServiceManager();
	}
	
	public static UpdateManager getInstance() throws PDLException {
		if(UpdateManager.manager == null)
			UpdateManager.manager = new UpdateManager();
		return UpdateManager.manager;
	}
	
	public void coreUpdateCheck() {
		pdio.debug("Checking core version...");
		try {
			if(!this.pingHost(1000)) {
				pdio.error("Cannot reach to the update server.");
				return;
			}
			
			String path = PDIO.getPluginJar().getAbsolutePath();
			JSONObject pluginObj = PDIO.readYMLwithJSON(path, "plugin.yml");
			JSONObject result = (JSONObject)JsonReader.readJsonFromUrl(
					resolveURL("update_check",
							pluginObj.get("label").toString(),
							core.getVersion(),
							core.getVersion(),
							"1"
							)
					).get("response");
			if((boolean)result.get("critical"))
				pdio.error("The core plugin must be updated to " + result.get("updateVersion") + " version!");
			else if((boolean)result.get("updatable"))
				pdio.info("The core plugin can be updated to " + result.get("updateVersion") + " version.");
			else
				pdio.debug("The core plugin is up to date!");
		} catch (Exception e) {
			pdio.error(e.getMessage());
			pdio.error("Could not check the version of the core plugin.");
		}
		pdio.debug("Checking the version of the core plugin was finished.");
	}
	
	public void updateCheck() {
		this.updateCheck(false);
	}
	
	public void updateCheck(boolean doUpdate) {
		pdio.debug("Checking service updates...");
		
		try {
			if(!this.pingHost(1000)) {
				pdio.error("Cannot reach to the update server.");
				return;
			}
			
			HashMap<ServiceInfo, String> updatableServices = new HashMap<>();
			for(ServiceInfo info : core.getServiceManager().getServiceList()) {
				try {
					String path = pdio.getSubDirectory(ServiceManager.SERVICE_DIRECTORY_NAME).getAbsolutePath();
					JSONObject pluginObj = PDIO.readYMLwithJSON(path + "/" + info.getJarName(), "service.yml");
					JSONObject result = getUpdateInfo(pluginObj.get("label").toString(), pluginObj.get("version").toString());
					if(!(boolean)result.get("updatable"))
						continue;
					
					boolean coreUpdatable = (boolean)result.get("coreUpdateRequired");
					if(coreUpdatable) {
						pdio.warn(info.getName() + " - This plugin must be updated after updating the core to " + result.get("coreUpdateVersion") + " version.");
						continue;
					}
					
					if((boolean)result.get("critical")) {
						updatableServices.put(info, result.get("updateVersion").toString());
						continue;
					}
					pdio.info(info.getName() + " - This plugin can be updated to " + result.get("updateVersion") + " version.");
					if(doUpdate)
						updatableServices.put(info, result.get("updateVersion").toString());
				}catch(Exception e) {
					e.printStackTrace();
					pdio.error("ErrMsg: " + e.getMessage());
					pdio.error("The service '" + info.getName() + "' is incorrect service file!");
				}
			}
			
			for(ServiceInfo info : updatableServices.keySet()) {
				pdio.debug("Start updating service '" + info.getName() + "'.");
				try {
					updateService(info, updatableServices.get(info));
				}catch(Exception e) {
					pdio.error(e.getMessage());
					pdio.error("Updating service '" + info.getName() + "' was failed.");
				}
			}
		} catch (Exception e) {
			pdio.error(e.getMessage());
			pdio.error("Could not update services.");
		}
		pdio.debug("Checking service updates was finished.");
	}
	
	public JSONObject getUpdateInfo(String label, String version) throws Exception {
		if(!this.pingHost(1000))
			throw new Exception("Cannot reach to the update server.");
		return (JSONObject)JsonReader.readJsonFromUrl(
				resolveURL(
						"update_check",
						label,
						version,
						core.getVersion()
						)
				).get("response");
	}
	
	public JSONObject getUpdateInfo(ServiceInfo info) throws Exception {
		if(!this.pingHost(1000))
			throw new Exception("Cannot reach to the update server.");
		return (JSONObject)JsonReader.readJsonFromUrl(
				resolveURL(
						"update_check",
						info.getLabel(),
						info.getVersion(),
						core.getVersion()
						)
				).get("response");
	}
	
	public ServiceManager.ServiceStatus uninstallService(ServiceInfo info) throws Exception {
		pdio.debug("Uninstalling service...");
		ServiceManager.ServiceStatus status = srvMgr.getServiceStatus(info);
		if(srvMgr.isAttached(info))
			srvMgr.detach(info);
		if(!info.getJar().delete())
			pdio.error("Could not uninstall the service '" + info.getName() + "'");
		pdio.debug("Uninstalling done.");
		return status;
	}
	
	public boolean installService(String label, String version) throws Exception {
		return installService(label, version, false);
	}
	
	public boolean installService(String label, String version, boolean pingPass) throws Exception {
		pdio.debug("Installing service...");
		if(!pingPass && !this.pingHost(1000)) {
			pdio.error("Cannot reach to the update server.");
			return false;
		}
		
		JSONObject result = (JSONObject)JsonReader.readJsonFromUrl(resolveURL("plugin_info", label, version)).get("response");
		if(result.isEmpty()) {
			pdio.error("The given service info is incorrect!");
			return false;
		}
		String serviceName = (String)result.get("name");
		
		InputStream is = new URL(resolveURL("download", label, version)).openStream();
		byte[] buffer = new byte[is.available()];
		is.read(buffer);
		
		String path = pdio.getSubDirectory(ServiceManager.SERVICE_DIRECTORY_NAME).getAbsolutePath() + "/" + serviceName + ".jar";
		File newService = new File(path);
		OutputStream os = new FileOutputStream(newService);
		os.write(buffer);
		os.close();
		is.close();
		
		pdio.info("The service '" + serviceName + "' was successfully installed!");
		return true;
	}
	
	public void updateService(ServiceInfo info, String updateVersion) throws Exception {
		srvMgr.notifyUpdating();
		ServiceManager.ServiceStatus status = uninstallService(info);
		if(!installService(info.getLabel(), updateVersion)) {
			pdio.error("Could not update service '" + info.getName() + "'");
			return;
		}
		
		switch(status) {
		case Attached:
			srvMgr.attach(info);
			break;
			
		case Activated:
			srvMgr.attach(info);
			srvMgr.activate(info.getName());
			break;
		default:
			/*Do Nothing*/
			break;
		}
	}
	
	@SuppressWarnings("unchecked")
	public HashMap<ServiceSummary, ServiceInstallationStatus> getAllServices() throws Exception{
		HashMap<ServiceSummary, ServiceInstallationStatus> resultMap = new HashMap<>();
		
		JSONArray result = (JSONArray)JsonReader.readJsonFromUrl(resolveURL("plugins")).get("response");
		ListIterator<JSONObject> it = result.listIterator();
		while(it.hasNext()) {
			JSONObject obj = (JSONObject)it.next();
			String name = ((String)((JSONArray)obj.get("filenames")).get(0)).replace(".jar", "");
			ServiceInfo info = srvMgr.getServiceInfo(name);
			if(info == null) {
				String label = (String)obj.get("label");
				String version = (String)obj.get("latestVersion");
				resultMap.put(new ServiceSummary(label, name, version), ServiceInstallationStatus.NotInstalled);
			} else {
				ServiceSummary summary = new ServiceSummary(info.getLabel(), info.getName(), info.getVersion());
				resultMap.put(summary, ServiceInstallationStatus.Installed);
			}
		}
		
		for(ServiceInfo info : srvMgr.getServiceList()) {
			boolean supported = false;
			for(ServiceSummary summary : resultMap.keySet()) {
				if(info.getLabel().equals(summary.getLabel()) && info.getName().equals(summary.getName())) {
					supported = true;
					break;
				}
			}
			if(!supported) {
				resultMap.put(new ServiceSummary(
						info.getLabel(),
						info.getName(),
						info.getVersion()
						), ServiceInstallationStatus.NotSupported);
			}
		}
		
		return resultMap;
	}
	
	private String resolveURL(String... cmd) {
		return UPDATE_PDL_API_URL + String.join("/", cmd);
	}
	
	private boolean pingHost(int timeout) throws IOException {
		if (InetAddress.getByName(HOST_NAME).isReachable(timeout))
			return true;
		else
			return UpdateManager.hostExists();
	}
	
	private static boolean hostExists() {

	    try {
	        HttpURLConnection.setFollowRedirects(false);
	        HttpURLConnection con = (HttpURLConnection) new URL("http://" + HOST_NAME).openConnection();
	        con.setConnectTimeout(1000);
	        con.setReadTimeout(1000);
	        con.setRequestMethod("HEAD");
	        return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
	    } catch (Exception e) {
	        e.printStackTrace();
	        return false;
	    }
	}
}

