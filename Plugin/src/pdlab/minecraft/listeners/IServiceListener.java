package pdlab.minecraft.listeners;

import pdlab.minecraft.services.ServiceBase;

public interface IServiceListener {
	abstract void onUpdatingNotified(ServiceBase service);
	abstract void onAttached(ServiceBase service);
	abstract void onDetached(ServiceBase service);
	abstract boolean onActivating(ServiceBase service);
	abstract void onActivated(ServiceBase service);
	abstract boolean onDeactivating(ServiceBase service);
	abstract void onDeactivated(ServiceBase service);
}
