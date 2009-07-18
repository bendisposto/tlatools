package org.lamport.tla.toolbox.tool.tlc.job;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.lamport.tla.toolbox.tool.tlc.launch.IModelConfigurationConstants;
import org.lamport.tla.toolbox.tool.tlc.launch.IModelConfigurationDefaults;
import org.lamport.tla.toolbox.tool.tlc.ui.ConsoleFactory;
import org.lamport.tla.toolbox.util.ResourceHelper;

/**
 * Abstract TLC job
 * @author Simon Zambrovski
 * @version $Id$
 */
public abstract class TLCJob extends AbstractJob implements IModelConfigurationConstants, IModelConfigurationDefaults
{

    protected static final int STEP = 30;
    protected static final long TIMEOUT = 1000 * 1;
    protected IFile rootModule;
    protected IFile cfgFile;
    protected IFile outFile;
    protected ISchedulingRule rule;
    protected IFolder launchDir;
    protected int workers = 1;
    protected IOConsoleOutputStream outputStream = ConsoleFactory.getTLCConsole().newOutputStream();
    protected ILaunch launch;
    protected String modelName;
    
    protected boolean appendConsole = false;

    /**
     * Creates a TLC job for a given spec and model
     * @param rootModule
     * @param cfgFile
     * @param launchDir
     */
    public TLCJob(String specName, String modelName, ILaunch launch)
    {
        super("TLC run for " + modelName);
        this.modelName = modelName;

        IProject project = ResourceHelper.getProject(specName);
        Assert.isNotNull(project, "Error accessing the spec project " + specName);

        this.launchDir = project.getFolder(modelName);
        Assert.isNotNull(this.launchDir, "Error accessing the model folder " + modelName);

        this.launch = launch;
        this.rootModule = this.launchDir.getFile("MC.tla");
        this.cfgFile = this.launchDir.getFile("MC.cfg");
        this.outFile = this.launchDir.getFile("MC.out");
        this.rule = ResourceHelper.getModifyRule(outFile);
    }

    /**
     * Sets the number of workers
     * @param workers number of threads to be run in parallel
     */
    public void setWorkers(int workers)
    {
        this.workers = workers;
    }

    protected Action getJobCompletedAction()
    {
        return new Action("View job results") 
        {
            public void run()
            {
                // TODO
                System.out.println("Results viewed");
            }
        };
    }

    /**
     * The run method
     */
    protected abstract IStatus run(IProgressMonitor monitor);


    /**
     * Reports progress to the console, output file, etc...
     * @param string
     */
    protected void reportProgress(final String message) 
    {
        try
        {
            ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() 
            {
                public void run(IProgressMonitor monitor) throws CoreException
                {
                    // System.out.print(Thread.currentThread().getId() + " : " + message);
                    outFile.appendContents(new ByteArrayInputStream(message.getBytes()), IResource.KEEP_HISTORY | IResource.FORCE, monitor);
                }
            }, rule, IResource.NONE, new NullProgressMonitor());
            
            // if the console output is active, print to it
            if (appendConsole) 
            {
                this.outputStream.write(message.getBytes());
            }
        } catch (CoreException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Checks if TLC is still running
     * @return true, if TLC is still running
     */
    public abstract boolean checkAndSleep();

    /**
     * Initializes the console and shows the view 
     */
    protected void initConsole()
    {
        ConsoleFactory.getTLCConsole().activate();
    }
}