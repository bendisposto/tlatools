package org.lamport.tla.toolbox.tool.tlc.ui.modelexplorer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.lamport.tla.toolbox.spec.Spec;
import org.lamport.tla.toolbox.tool.ToolboxHandle;
import org.lamport.tla.toolbox.tool.tlc.handlers.CloneModelHandlerDelegate;
import org.lamport.tla.toolbox.tool.tlc.model.TLCSpec;
import org.lamport.tla.toolbox.tool.tlc.ui.TLCUIActivator;
import org.lamport.tla.toolbox.util.UIHelper;

/**
 * Contributes a list of models for cloning.
 * 
 * @author Daniel Ricketts
 *
 */
public class CloneModelContributionItem extends CompoundContributionItem
{
    /**
     * Contrary to CloneModelHandlerDelegate.COMMAND_ID, no enabledWhen expression plugin.xml
     */
    public static final String COMMAND_ID_ALWAYS_ENABLED = CloneModelHandlerDelegate.COMMAND_ID + ".always.enabled";

    private ImageDescriptor modelIcon = TLCUIActivator.getImageDescriptor("icons/full/choice_sc_obj.gif");

    protected IContributionItem[] getContributionItems()
    {
        final Vector<CommandContributionItem> modelContributions = new Vector<CommandContributionItem>();

        Spec currentSpec = ToolboxHandle.getCurrentSpec();
        if (currentSpec == null) {
        	return new IContributionItem[0];
        }
		IProject specProject = currentSpec.getProject();

        try
        {
            // refresh local to pick up any models that have been added
            // to the file system but not recognized by the toolbox's resource
            // framework.
			// TODO decouple from UI thread or question why it has to be done
			// here. Meaning, why doesn't the resource fw handle this case
			// already?
            specProject.refreshLocal(IResource.DEPTH_ONE, new NullProgressMonitor());
        } catch (CoreException e)
        {
        	e.printStackTrace();
        }

		// First, search for all models for the given spec.
		final Set<String> modelNames = currentSpec.getAdapter(TLCSpec.class).getModels().keySet();
		for (String modelName : modelNames) {
            // Next, set the command and the parameters for the command
            // that will be called when the user selects this item.
            Map<String, String> parameters = new HashMap<String, String>();

            // fill the model name for the handler
            parameters.put(CloneModelHandlerDelegate.PARAM_MODEL_NAME, modelName);

            // create the contribution item
            CommandContributionItemParameter param = new CommandContributionItemParameter(UIHelper
                    .getActiveWindow(), "toolbox.command.model.clone." + modelName,
                    COMMAND_ID_ALWAYS_ENABLED, parameters, modelIcon, null, null, modelName, null,
                    "Clones " + modelName, CommandContributionItem.STYLE_PUSH, null, true);

            // add contribution item to the list
            modelContributions.add(new CommandContributionItem(param));
        }

        return (IContributionItem[]) modelContributions.toArray(new IContributionItem[modelContributions.size()]);
    }
}
