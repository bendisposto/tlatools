package org.lamport.tla.toolbox.tool.tlc.job;

import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.runtime.IStatus;

public interface ITLCJobStatus extends IStatus {

	URL getURL();

	InputStream getOutput();
	
	void killTLC();

	boolean isReconnect();
}
