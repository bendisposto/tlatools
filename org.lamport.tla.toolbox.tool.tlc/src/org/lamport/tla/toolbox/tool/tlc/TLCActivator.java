package org.lamport.tla.toolbox.tool.tlc;

import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class TLCActivator extends AbstractUIPlugin {
	
	/**
	 * Take a model snapshot when model checking finishes to create a history of model runs. 
	 */
	public static final String I_TLC_SNAPSHOT_PREFERENCE = "takeModelSnapshot";
	
	// The plug-in ID
	public static final String PLUGIN_ID = "org.lamport.tla.toolbox.tool.tlc";

	// The shared instance
	private static TLCActivator plugin;
	
	/**
	 * The constructor
	 */
	public TLCActivator() 
	{
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		plugin.getPreferenceStore().setDefault(I_TLC_SNAPSHOT_PREFERENCE, true);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static TLCActivator getDefault() {
		return plugin;
	}

    /**
     * Logs an error
     * @param message
     * @param e 
     */
    public static void logError(String message, Throwable e)
    {
        getDefault().getLog().log(new Status(Status.ERROR, TLCActivator.PLUGIN_ID, message, e));
    }

    /**
     * Prints a debug message
     * @param message to print
     */
    public static void logInfo(String message)
    {
        getDefault().getLog().log(new Status(Status.INFO, TLCActivator.PLUGIN_ID, message));
    }
    
    /**
     * Prints a debug message
     * @param message to print
     */
    public static void logDebug(String message)
    {
        System.out.println(message);
    }

}
