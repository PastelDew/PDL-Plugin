package pdlab.minecraft.core;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Iterator;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import pdlab.minecraft.debug.Assert;
import pdlab.minecraft.debug.PDLException;

public abstract class PDConfig {
	private String rootDirectory = null;
	private String serviceName = null;
	private JSONObject config = null;
	
	public PDConfig() {
		rootDirectory = getRootDirectory();
		serviceName = getConfigName();
		Assert.False(rootDirectory.isEmpty() || serviceName.isEmpty());
	}
	
	@SuppressWarnings("unchecked")
	public void loadConfig() throws PDLException {
		File configFile = this.getConfigFile();
		try {
			JSONParser parser = new JSONParser();
			JSONObject readConfig = (JSONObject) parser.parse(new FileReader(configFile));
			JSONObject defaultConfig = this.getDefaultConfig();
			for(Iterator<?> it = defaultConfig.keySet().iterator(); it.hasNext();) {
				Object key = it.next();
				if(readConfig.containsKey(key))
					continue;
				readConfig.put(key, defaultConfig.get(key));
			}
			this.config = readConfig;
		} catch (Exception e) {
			throw new PDLException(e.getMessage());
		}
	}
	
	public void saveConfig() throws PDLException {
		File configFile = this.getConfigFile();
		try {
			String strJson = this.config.toJSONString();
			PrintWriter writer = new PrintWriter(configFile);
			writer.write(strJson);
			writer.close();
		} catch (Exception e) {
			throw new PDLException(e.getMessage());
		}
	}
	
	public File getConfigFile() throws PDLException {
		File configFile = new File(rootDirectory + "/" + serviceName + ".json");
		if(!configFile.exists()) {
			this.config = this.getDefaultConfig();
			String strJson = this.config.toJSONString();
			
			try {
				configFile.createNewFile();
				PrintWriter writer = new PrintWriter(configFile);
				writer.write(strJson);
				writer.close();
			} catch (Exception e) {
				throw new PDLException(e.getMessage());
			}
		}
		return configFile;
	}
	
	public JSONObject getConfig() { return config; }
	public String getRootDirectory() { return "."; }
	
	public void setConfig(JSONObject config) { this.config = config; } 
	
	public abstract String getConfigName();
	public abstract JSONObject getDefaultConfig();
	
}
