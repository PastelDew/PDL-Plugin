package pdlab.minecraft.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.yaml.snakeyaml.Yaml;

import pdlab.minecraft.core.PDCore;
import pdlab.minecraft.debug.Assert;
import pdlab.minecraft.services.ServiceManager.ServiceStatus;

public class ServiceInfo {
	private PDCore core = null;
	private File fJar = null;
	
	private ServiceBase service = null;
	private String label = null;
	private String name = null;
	private String version = null;
	private String main = null;
	private String description = null;
	private Map<String, String> dependencies = null;
	
	private ServiceInfo() {}
	
	@SuppressWarnings("unchecked")
	public static ServiceInfo newInstance(PDCore core, String pathToJar, boolean initService) throws Exception {
		ServiceInfo info = null;
		
		File fJar = new File(pathToJar);
		Assert.condition(fJar.exists(), "That service does not exist!");
		
		JarFile jarFile = new JarFile(pathToJar);
		try {
			for(Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements();) {
				JarEntry entry = entries.nextElement();
				String entryName = entry.getName();
				if(entryName.equals("service.yml")) {
					Yaml yaml = new Yaml();
					InputStream stream = jarFile.getInputStream(entry);
					Map<String, Object> serviceData = (Map<String, Object>) yaml.load(stream);
					stream.close();
					
					String serviceLabel = (String) serviceData.get("label");
					Assert.condition(serviceLabel != null && serviceLabel.length() > 0, "Service label is wrong! [" + serviceLabel + "]");
					
					String serviceName = (String) serviceData.get("name");
					String jarName = new File(jarFile.getName()).getName().split("\\.")[0];
					Assert.condition(
							serviceName != null
							&& serviceName.length() > 0
							&& (serviceName).equals(jarName),
							"Service name is wrong! ['" + serviceName + "' != '" + jarName + "']");
					
					String serviceVersion = (String) serviceData.get("version");
					Assert.condition(serviceVersion != null && serviceVersion.length() > 0, "Service version is wrong! [" + serviceVersion + "]");
					
					String mainClass = (String) serviceData.get("main");
					Assert.condition(mainClass != null && mainClass.length() > 0, "Class path is wrong! [" + mainClass + "]");
					
					String requiredCoreVersion = (String) serviceData.get("requiredCoreVersion");
					Assert.condition(requiredCoreVersion != null && requiredCoreVersion.length() > 0, "Required core version is wrong! [" + requiredCoreVersion + "]");
					
					String[] versionRange = requiredCoreVersion.split("~");
					if(versionRange.length == 2) {
						Assert.condition(
								core.validateVersion(
										versionRange[0],
										"<=")
								&& core.validateVersion(
										versionRange[1],
										">"),
								"The core has to be updated! (Required version: " + requiredCoreVersion + ", Now: " + core.getVersion() + ")"
								);
					} else if(versionRange.length == 1) {
						Assert.condition(
								core.validateVersion(
										versionRange[0],
										"=="),
								"The version is mismatched! (Required version: " + requiredCoreVersion + ", Now: " + core.getVersion() + ")"
								);
					} else Assert.condition(false, "The version is not correct! [" + serviceVersion + "]");
						
					Map<String, String> dependencies = (Map<String, String>) serviceData.get("dependencies");
					if(dependencies == null) dependencies = new HashMap<>();
					
					info = new ServiceInfo();
					info.fJar = fJar;
					info.core = core;
					info.label = serviceLabel;
					info.name = serviceName;
					info.version = serviceVersion;
					info.main = mainClass;
					info.description = (String) serviceData.get("description");
					info.dependencies = dependencies;
					if(initService) info.createInstance();
					break;
				}
			}
		} finally {
			jarFile.close();
		}
		
		return info;
	}
	
	public ServiceBase createInstance() throws Exception {
		ServiceManager manager = core.getServiceManager();
		HashMap<String, ServiceStatus> statuses = manager.getServiceStatuses();
		for(String serviceName : dependencies.keySet()) {
			Assert.condition(statuses.containsKey(serviceName), "Dependency crashed! The server has no '" + serviceName + "' service.");
			if(statuses.get(serviceName) == ServiceStatus.NotAttached)
				manager.attach(serviceName);
			if(statuses.get(serviceName) != ServiceStatus.Activated)
				manager.activate(serviceName);
		}
		
		byte[] buf = new byte[(int)fJar.length()];
		
		FileInputStream in = null;
		try {
			in = new FileInputStream(fJar);
			in.read(buf);
		} finally {
			if(in != null) in.close();
		}
		
		File tmpFile = File.createTempFile(this.fJar.getName(), ".jar");
		FileOutputStream out = null;
		try{
			out = new FileOutputStream(tmpFile);
			out.write(buf);
		} finally {
			if(out != null) out.close();
		}
		tmpFile.deleteOnExit();
		
		URL jarUrl = new URL("jar", "", -1, tmpFile.toURI().toString() + "!/");
		URLConnection conn = jarUrl.openConnection();
		conn.setUseCaches(true);
		
		ClassLoader loader = URLClassLoader.newInstance(
				new URL[] { jarUrl },
				getClass().getClassLoader()
				);
		Class<?> clazz = Class.forName(this.main, true, loader);
		Class<? extends ServiceBase> serviceClass = clazz.asSubclass(ServiceBase.class);
		Constructor<? extends ServiceBase> ctor = serviceClass.getConstructor();
		
		ServiceBase service = ctor.newInstance();
		service.onCreate(this, core);
		this.service = service;
		
		((JarURLConnection) conn).getJarFile().close();
		return service;
	}
	
	public void dispose() {
		this.fJar = null;
		this.core = null;
		this.name = null;
		this.version = null;
		this.main = null;
		this.description = null;
		this.dependencies.clear();
		this.service = null;
	}
	
	public String getLabel() { return label; }
	public String getName() { return name; }
	public String getVersion() { return version; }
	public String getMain() { return main; }
	public String getDescription() { return description; }
	public String getJarName() { return new File(fJar.getName()).getName(); }
	public File getJar() { return fJar; }
	public ServiceBase getService() { return service; }
}
