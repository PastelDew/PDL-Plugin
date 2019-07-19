package pdlab.minecraft.services;

import java.util.ArrayList;
import java.util.List;

import pdlab.minecraft.core.PDCore;
import pdlab.minecraft.listeners.IServiceListener;

public abstract class ServiceBase implements IServiceListener{
	private PDCore mCore;
	private ServiceManager mServiceManager;
	private ServiceInfo mInfo;
	private ArrayList<IServiceListener> mServiceListener;
	
	private long beginTime;
	private long endTime;
	
	public ServiceBase() {
		mServiceListener = new ArrayList<>();
		beginTime = 0;
		endTime = -1;
	}
	
	public void onCreate(ServiceInfo info, PDCore core) {
		mInfo = info;
		mCore = core;
		mServiceManager = core.getServiceManager();
	}
	
	public void addServiceListener(IServiceListener listener) {
		if(mServiceListener.contains(listener)) return;
		mServiceListener.add(listener);
	}
	
	public void removeServiceListener(IServiceListener listener) {
		mServiceListener.remove(listener);
	}
	
	protected PDCore getCore() { return mCore; }
	protected ServiceManager getServiceManager() { return mServiceManager; }
	
	public ServiceInfo getServiceInfo() { return mInfo; }
	public abstract List<String> getCommandHelp();
	
	public void update() {
		if(endTime == -1) endTime = System.nanoTime();
		beginTime = System.nanoTime();
		float deltaTime = (beginTime - endTime) / (float) 1000000;
		this.onUpdate(deltaTime);
		endTime = System.nanoTime();
	}
	
	protected void onUpdate(float deltaTime) {/*Empty Body*/}
	
	@Override
	public void onUpdatingNotified(ServiceBase service) {
		if(service.equals(this)) {
			for(IServiceListener listener : mServiceListener)
				listener.onUpdatingNotified(this);
		}
	}

	@Override
	public void onAttached(ServiceBase service) {
		if(service.equals(this)) {
			for(IServiceListener listener : mServiceListener)
				listener.onAttached(this);
		}
	}

	@Override
	public void onDetached(ServiceBase service) {
		if(service.equals(this)) {
			for(IServiceListener listener : mServiceListener)
				listener.onDetached(this);
		}
	}

	@Override
	public boolean onActivating(ServiceBase service) {
		if(service.equals(this)) {
			for(IServiceListener listener : mServiceListener)
				if(!listener.onActivating(this)) return false;
		}
		return true;
	}

	@Override
	public void onActivated(ServiceBase service) {
		if(service.equals(this)) {
			for(IServiceListener listener : mServiceListener)
				listener.onActivated(this);
		}
	}

	@Override
	public boolean onDeactivating(ServiceBase service) {
		if(service.equals(this)) {
			for(IServiceListener listener : mServiceListener) {
				if(!listener.onDeactivating(this)) return false;
			}
		}
		return true;
	}

	@Override
	public void onDeactivated(ServiceBase service) {
		if(service.equals(this)) {
			for(IServiceListener listener : mServiceListener)
				listener.onDeactivated(this);
		}
	}
}
