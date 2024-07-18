/*******************************************************************************
 * Copyright (c) 2015 Microsoft Research. All rights reserved. 
 *
 * The MIT License (MIT)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software. 
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Contributors:
 *   Markus Alexander Kuppe - initial API and implementation
 ******************************************************************************/

package org.lamport.tla.toolbox.jcloud;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jclouds.ContextBuilder;

public class AzureCloudTLCInstanceParameters extends CloudTLCInstanceParameters {

	public AzureCloudTLCInstanceParameters(final String tlcParams, int numberOfWorkers) {
        super(tlcParams.trim(), numberOfWorkers);
	}
	
	// vm args

	/* (non-Javadoc)
	 * @see org.lamport.tla.toolbox.jcloud.CloudTLCInstanceParameters#getJavaVMArgs()
	 */
	@Override
	public String getJavaVMArgs() {
		if (numberOfWorkers == 1) {
			return getJavaWorkerVMArgs();
		}
		// See org.lamport.tla.toolbox.tool.tlc.job.TLCProcessJob.getAdditionalVMArgs()
		return "--add-modules=java.activation -XX:+IgnoreUnrecognizedVMOptions -Xmx96G -Xms96G";
	}
	
	/* (non-Javadoc)
	 * @see org.lamport.tla.toolbox.jcloud.CloudTLCInstanceParameters#getJavaWorkerVMArgs()
	 */
	@Override
	public String getJavaWorkerVMArgs() {
		// See org.lamport.tla.toolbox.tool.tlc.job.TLCProcessJob.getAdditionalVMArgs()
		return "--add-modules=java.activation -XX:+IgnoreUnrecognizedVMOptions -Xmx32G -Xms32G -XX:MaxDirectMemorySize=64g";
	}
	
	/* (non-Javadoc)
	 * @see org.lamport.tla.toolbox.jcloud.CloudTLCInstanceParameters#getTLCParameters()
	 */
	@Override
	public String getTLCParameters() {
		if (numberOfWorkers == 1) {
			if (tlcParams.length() > 0) {
				return "-workers 16 " + tlcParams;
			}
			return "-workers 16";
		} else {
			return "-coverage 0 -checkpoint 0";
		}
	}

	/* (non-Javadoc)
	 * @see org.lamport.tla.toolbox.jcloud.CloudTLCInstanceParameters#getCloudProvier()
	 */
	@Override
	public String getCloudProvider() {
		return "azurecompute";
	}

	/* (non-Javadoc)
	 * @see org.lamport.tla.toolbox.jcloud.CloudTLCInstanceParameters#getRegion()
	 */
	@Override
	public String getRegion() {
		return "us-east";
	}

	/* (non-Javadoc)
	 * @see org.lamport.tla.toolbox.jcloud.CloudTLCInstanceParameters#getImageId()
	 */
	@Override
	public String getImageId() {
		// 'azure vm image list eastus canonical' (manually lookup image release date from output)
		// With azure-cli v2 (based on Python) extract date from 'az vm image list --all --publisher Canonical'.
		return "b39f27a8b8c64d52b05eac6a62ebad85__Ubuntu-16_04-LTS-amd64-server-20170919-en-us-30GB";
	}

	/* (non-Javadoc)
	 * @see org.lamport.tla.toolbox.jcloud.CloudTLCInstanceParameters#getHardwareId()
	 */
	@Override
	public String getHardwareId() {
		return "STANDARD_D14";
		// 16 cores
		// 112GB
	}

	/* (non-Javadoc)
	 * @see org.lamport.tla.toolbox.jcloud.CloudTLCInstanceParameters#getIdentity()
	 */
	@Override
	public String getIdentity() {
		final String identity = System.getenv("AZURE_COMPUTE_IDENTITY");
		Assert.isNotNull(identity);
		return identity;
	}

	/* (non-Javadoc)
	 * @see org.lamport.tla.toolbox.jcloud.CloudTLCInstanceParameters#getCredentials()
	 */
	@Override
	public String getCredentials() {
		final String credential = System.getenv("AZURE_COMPUTE_CREDENTIALS");
		Assert.isNotNull(credential);
		return credential;
	}

	private String getSubscriptionId() {
		final String subscription = System.getenv("AZURE_COMPUTE_SUBSCRIPTION");
		Assert.isNotNull(subscription);
		return subscription;
	}

	/* (non-Javadoc)
	 * @see org.lamport.tla.toolbox.jcloud.CloudTLCInstanceParameters#validateCredentials()
	 */
	@Override
	public IStatus validateCredentials() {
		final String credential = System.getenv("AZURE_COMPUTE_CREDENTIALS");
		final String identity = System.getenv("AZURE_COMPUTE_IDENTITY");
		final String subscription = System.getenv("AZURE_COMPUTE_SUBSCRIPTION");
		if (credential == null || identity == null || subscription == null) {
			return new Status(Status.ERROR, "org.lamport.tla.toolbox.jcloud",
					"Invalid credentials, please check the environment variables "
							+ "(AZURE_COMPUTE_CREDENTIALS & AZURE_COMPUTE_IDENTITY "
							+ "and AZURE_COMPUTE_SUBSCRIPTION) are correctly "
							+ "set up and picked up by the Toolbox.");
		}
		return Status.OK_STATUS;
	}

	/* (non-Javadoc)
	 * @see org.lamport.tla.toolbox.jcloud.CloudTLCInstanceParameters#mungeBuilder(org.jclouds.ContextBuilder)
	 */
	@Override
	public void mungeBuilder(ContextBuilder builder) {
		builder.endpoint("https://management.core.windows.net/" + getSubscriptionId());
	}

	/* (non-Javadoc)
	 * @see org.lamport.tla.toolbox.jcloud.CloudTLCInstanceParameters#getExtraRepositories()
	 */
	@Override
	public String getExtraRepositories() {
		return "echo \"deb [arch=amd64] https://packages.microsoft.com/repos/azure-cli/ wheezy main\" | sudo tee /etc/apt/sources.list.d/azure-cli.list && apt-key adv --keyserver packages.microsoft.com --recv-keys 417A0893";
	}

	/* (non-Javadoc)
	 * @see org.lamport.tla.toolbox.jcloud.CloudTLCInstanceParameters#getExtraPackages()
	 */
	@Override
	public String getExtraPackages() {
		// https://docs.microsoft.com/en-us/cli/azure/install-azure-cli?view=azure-cli-latest#install-on-debianubuntu-with-apt-get
		// see getExtraRepositories too.
		return "azure-cli";
	}
	
	/* (non-Javadoc)
	 * @see org.lamport.tla.toolbox.jcloud.CloudTLCInstanceParameters#getHostnameSetup()
	 */
	@Override
	public String getHostnameSetup() {
		// Append ".cloudapp.net" to automatically set hostname and add a mapping from
		// public ip (obtained via third party service ifconfig.co) to hostname in
	    // /etc/hosts. Results in FQDN being used my MailSender and thus less likely
		// to be classified or rejected as spam.
		// The suffix ".cloudapp.net" is something that might change on the Azure end in
		// the future. It will then break this statement (suffix can be found in portal).
		// It would also be nice for Azure to offer a public API to query the hostname
		// (similar to EC2CloudTLCInstanceParameters#getHostnameSetup.
		return "hostname \"$(hostname).cloudapp.net\" && echo \"$(curl -s ifconfig.co) $(hostname)\" >> /etc/hosts";
	}

	/* (non-Javadoc)
	 * @see org.lamport.tla.toolbox.jcloud.CloudTLCInstanceParameters#getCleanup()
	 */
	@Override
	public String getCloudAPIShutdown() {
		final String servicePrincipal = System.getenv("AZURE_COMPUTE_SERVICE_PRINCIPAL");
		final String password = System.getenv("AZURE_COMPUTE_SERVICE_PRINCIPAL_PASSWORD");
		final String tenant = System.getenv("AZURE_COMPUTE_SERVICE_PRINCIPAL_TENANT");
		if (servicePrincipal == null || password == null || tenant == null) {
			// Missing credentials.
			return super.getCloudAPIShutdown();
		}
		// What we try to accomplish is to purge the complete Azure Resource Group (a RG
		// combines all Azure resources associated with the VM (storage, networking,
		// ips, ...).
		// The way we do it, is to use the azure CLI to deploy the template in
		// /tmp/rg.json created by the printf statement under the same resource group
		// identify we wish to purge. The trick is, that the template defines no
		// resources whatsoever. This effectively purges the old resources in the
		// resource group. Idea taken from
		// http://www.codeisahighway.com/effective-ways-to-delete-resources-in-a-resource-group-on-azure/
		// bash/screen/ssh apparently fiddle with quotes and which is why the json is base64 encoded 
		// and decoded on the instance:
		// { "$schema": "https://schema.management.azure.com/schemas/2015-01-01/deploymentTemplate.json#", "contentVersion": "1.0.0.0", "parameters": {}, "variables": {}, "resources": [], "outputs": {} } | base64
		//
		// Unfortunately, the azure CLI needs credentials to talk to the Azure API. For
		// that, one manually creates a service principal once as described in
		// https://docs.microsoft.com/en-us/cli/azure/create-an-azure-service-principal-azure-cli?view=azure-cli-latest
		// and uses it to log into Azure as described in
		// https://docs.microsoft.com/en-us/cli/azure/authenticate-azure-cli?view=azure-cli-latest.
		// Using AZURE_COMPUTE_CREDENTIALS and AZURE_COMPUTE_IDENTITY to login with azure CLI
		// would trigger a two-factor auth for Microsoft FTEs. Not something we want.
		//
		// An alternative might be to use an auth.properties file, but this doesn't seem
		// supported by azure CLI yet. Read "File based authentication" at
		// https://docs.microsoft.com/en-us/java/azure/java-sdk-azure-authenticate#mgmt-file
		return "echo eyAiJHNjaGVtYSI6ICJodHRwczovL3NjaGVtYS5tYW5hZ2VtZW50LmF6dXJlLmNvbS9zY2hlbWFzLzIwMTUtMDEtMDEvZGVwbG95bWVudFRlbXBsYXRlLmpzb24jIiwgImNvbnRlbnRWZXJzaW9uIjogIjEuMC4wLjAiLCAicGFyYW1ldGVycyI6IHt9LCAidmFyaWFibGVzIjoge30sICJyZXNvdXJjZXMiOiBbXSwgIm91dHB1dHMiOiB7fSB9Cg== | base64 -d > /tmp/rg.json"
				+ " && " + "az login --service-principal -u \"" + servicePrincipal + "\" -p " + password + " --tenant "
				// $(hostname -s) only works iff hostname is correctly setup with getHostnameSetup() above.
				+ tenant + " && " + "az group deployment create --resource-group $(hostname -s)"
				+ " --template-file /tmp/rg.json --mode Complete";
	}
}
