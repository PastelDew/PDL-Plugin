package pdlab.minecraft.services;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import pdlab.minecraft.core.PDCore;
import pdlab.minecraft.core.PDIO;
import pdlab.minecraft.debug.PDLException;

public class ServiceManager {
	public static enum ServiceStatus { NotAttached, Attached, Activated, Unknown };
	public static String SERVICE_DIRECTORY_NAME = "services";
	
	private static ServiceManager mSvcMgr = null;
	private PDCore mCore = null;
	private PDIO mIO = null;
	
	private HashMap<String, ServiceInfo> mServices;
	private ArrayList<ServiceInfo> mActivatedServices;
	private Thread mUpdateThread;
	private boolean isUpdating = false;
	
	private ServiceManager(PDCore core) {
		mCore = core;
		mIO = mCore.getIO();
		mServices = new HashMap<>();
		mActivatedServices = new ArrayList<>();
		
	}
	
	public static ServiceManager initialize(PDCore core) {
		if(mSvcMgr == null) {
			mSvcMgr = new ServiceManager(core);
			mSvcMgr.mUpdateThread = new Thread(new Runnable() {
				@Override
				public void run() {
					while(mSvcMgr.isUpdating) {
						try {
							for(ServiceInfo info : mSvcMgr.mActivatedServices)
								info.getService().update();
							Thread.sleep(0);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			});
		}
		return mSvcMgr;
	}
	
	public static ServiceManager getInstance() throws PDLException {
		if(mSvcMgr == null)
			throw new PDLException(PDLException.ErrorInfo.CORE_NOT_INITIALIZED);
		return mSvcMgr;
	}
	
	public static ServiceManager getInstance(PDCore core) {
		if(mSvcMgr == null) initialize(core);
		return mSvcMgr;
	}
	
	public void startUpdating() {
		this.isUpdating = true;
		mUpdateThread.start();
	}
	
	public void stopUpdating() throws InterruptedException {
		this.stopUpdating(-1);
	}
	
	public void stopUpdating(long joinTime) throws InterruptedException {
		this.isUpdating = false;
		if(joinTime < 0) mUpdateThread.join();
		else mUpdateThread.join(joinTime);
	}
	
	public void attach(String serviceName) throws Exception {
		String servicePath = mIO.getSubDirectory(ServiceManager.SERVICE_DIRECTORY_NAME).getPath()
				+ "\\" + serviceName;
		if(!servicePath.endsWith(".jar"))
			servicePath += ".jar";
		
		ServiceInfo serviceInfo = ServiceInfo.newInstance(mCore, servicePath, true);
		this.attach(serviceInfo);
	}
	
	public void attach(ServiceInfo serviceInfo) throws PDLException {
		if(mServices.containsKey(serviceInfo.getName())) {
			mIO.warn("Service attach failed: The service '" + serviceInfo.getName() + "' is already exists!");
			return;
		}
		mServices.put(serviceInfo.getName(), serviceInfo);
		serviceInfo.getService().onAttached(serviceInfo.getService());
	}
	
	public boolean activate(String serviceName) {
		try{
			ServiceInfo serviceInfo = getServiceInfo(serviceName);
			assert serviceInfo != null : "Service activate failed: The service '" + serviceName + "' was not found.";
			return activate(serviceInfo);
		}catch(Exception err) {
			mIO.error("Error: " + err.getMessage());
			return false;
		}
	}
	
	private boolean activate(ServiceInfo serviceInfo) {
		String serviceName = serviceInfo.getName();
		ServiceBase service = serviceInfo.getService();
		if(isActivated(serviceInfo))
			mIO.warn("Service activate failed: The service '" + serviceName + "' was already activated.");
		else if(service.onActivating(service)) {
			mActivatedServices.add(serviceInfo);
			service.onActivated(service);
			mIO.info("The service '" + serviceName + "' was activated.");
			return true;
		}
		else mIO.warn("Service activate failed: The service '" + serviceName + "' cannot be activated.");
		
		return false;
	}
	
	public void activateAll() {
		ArrayList<ServiceInfo> unactivatedServices = new ArrayList<>(mServices.size());
		for(ServiceInfo serviceInfo : mServices.values()) {
			if(!mActivatedServices.contains(serviceInfo))
				unactivatedServices.add(serviceInfo);
		}
		for(ServiceInfo serviceInfo : unactivatedServices)
			activate(serviceInfo);
	}
	
	public void detach(String serviceName) {
		try {
			ServiceInfo info = getServiceInfo(serviceName);
			detach(info);
		}catch(Exception err) {
			mIO.error("Error: " + err.getMessage());
		}
	}
	
	public void detach(ServiceInfo serviceInfo) {
		assert serviceInfo.getService() != null : "Service detach failed: The service was given is null.";
		
		String serviceName = serviceInfo.getName();
		assert isAttached(serviceInfo) : "Service detach failed: The service '" + serviceName + "' was not found.";
		assert !isActivated(serviceInfo) || deactivate(serviceName) : "Service detach failed: The service '" + serviceName + "' is still running.";
		mServices.remove(serviceName);
		serviceInfo.getService().onDetached(serviceInfo.getService());
		serviceInfo.dispose();
	}
	
	public void detachAll() {
		ServiceInfo services[] = new ServiceInfo[mServices.values().size()];
		mServices.values().toArray(services);
		for(int i = 0; i < services.length; i++)
			detach(services[i]);
	}
	
	public void deactivateAll() {
		@SuppressWarnings("unchecked")
		ArrayList<ServiceInfo> services = (ArrayList<ServiceInfo>) mActivatedServices.clone();
		for(ServiceInfo info : services) {
			deactivate(info.getName());
		}
	}
	
	public boolean deactivate(String serviceName) {
		if(isActivated(serviceName)) {
			try {
				ServiceInfo info = this.getServiceInfo(serviceName);
				ServiceBase service = info.getService();
				if(!service.onDeactivating(service)) {
					mIO.warn("Service deactivate failed: The service '" + serviceName + "' cannot be deactivated.");
					return false;
				}
				mIO.info("Service deactivate: The service '" + serviceName + "' was deactivated.");
				mActivatedServices.remove(info);
				service.onDeactivated(service);
				return true;
			}catch(Exception err) {
				mIO.error("Error: " + err.getMessage());
				return false;
			}
		} else {
			mIO.warn("Service deactivate failed: The service '" + serviceName + "' is not activated.");
			return false;
		}
	}
	
	public ServiceStatus getServiceStatus(ServiceInfo service) throws Exception{
		return getServiceStatuses().get(service.getName());
	}
	
	public HashMap<String, ServiceStatus> getServiceStatuses() throws Exception{
		HashMap<String, ServiceStatus> map = new HashMap<>();
		File dir = mIO.getSubDirectory(ServiceManager.SERVICE_DIRECTORY_NAME);
		for(String fileName : dir.list()) {
			if(!fileName.endsWith(".jar")) continue;
			
			ServiceInfo info = ServiceInfo.newInstance(mCore, dir.getPath() + "\\" + fileName, false);
			String serviceName = info.getName();
			
			ServiceStatus status = ServiceStatus.NotAttached;
			if(this.isActivated(serviceName))
				status = ServiceStatus.Activated;
			else if(this.isAttached(serviceName))
				status = ServiceStatus.Attached;
			else
				status = ServiceStatus.NotAttached;
			
			map.put(info.getName(), status);
		}
		
		for(String serviceName : mServices.keySet()) {
			if(!map.containsKey(serviceName)) {
				if(this.isActivated(serviceName))
					map.put(serviceName, ServiceStatus.Activated);
				else
					map.put(serviceName, ServiceStatus.Attached);
			}
		}
		
		return map;
	}
	
	public ServiceInfo getServiceInfo(String serviceName) throws Exception {
		ServiceInfo result = null;
		for(ServiceInfo s : mServices.values()) {
			if(s.getName().equals(serviceName)) {
				result = s;
				break;
			}
		}
		
		File dir = mIO.getSubDirectory(ServiceManager.SERVICE_DIRECTORY_NAME);
		for(String fileName : dir.list()) {
			if(!fileName.endsWith(".jar")) continue;
			
			ServiceInfo info = ServiceInfo.newInstance(mCore, dir.getPath() + "\\" + fileName, false);
			if(info.getName().equals(serviceName) && !this.isAttached(serviceName)) {
				result = info;
				break;
			}
		}
		
		return result;
	}
	
	public List<ServiceInfo> getServiceList() throws Exception {
		File dir = mIO.getSubDirectory(ServiceManager.SERVICE_DIRECTORY_NAME);
		List<ServiceInfo> infoList = new ArrayList<>();
		for(String fileName : dir.list()) {
			if(!fileName.endsWith(".jar")) continue;
			
			ServiceInfo info = ServiceInfo.newInstance(mCore, dir.getPath() + "\\" + fileName, false);
			if(this.isAttached(info.getName())) {
				info = this.getServiceInfo(info.getName());
			}
			infoList.add(info);
		}
		return infoList;
	}
	
	public Collection<ServiceInfo> getAttachedServices() {
		return mServices.values();
	}
	
	public Collection<ServiceInfo> getActivatedServices(){
		return Collections.unmodifiableList(mActivatedServices);
	}
	
	public boolean isAttached(String serviceName) {
		return mServices.containsKey(serviceName);
	}
	
	public boolean isAttached(ServiceInfo serviceInfo) {
		return mServices.containsValue(serviceInfo);
	}
	
	public boolean isActivated(String serviceName) {
		return mActivatedServices.contains(mServices.get(serviceName));
	}
	
	public boolean isActivated(ServiceInfo serviceInfo) {
		return mActivatedServices.contains(serviceInfo);
	}
	
	public void notifyUpdating() {
		for(ServiceInfo info : mServices.values()) {
			ServiceBase service = info.getService();
			if(service == null)
				continue;
			service.onUpdatingNotified(service);
		}
	}
	
	
}
