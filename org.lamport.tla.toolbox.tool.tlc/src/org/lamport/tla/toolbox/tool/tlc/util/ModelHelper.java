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
 *   Simon Zambrovski - initial API and implementation
 ******************************************************************************/
package org.lamport.tla.toolbox.tool.tlc.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.part.FileEditorInput;
import org.lamport.tla.toolbox.spec.Spec;
import org.lamport.tla.toolbox.tool.ToolboxHandle;
import org.lamport.tla.toolbox.tool.tlc.TLCActivator;
import org.lamport.tla.toolbox.tool.tlc.launch.IModelConfigurationConstants;
import org.lamport.tla.toolbox.tool.tlc.launch.IModelConfigurationDefaults;
import org.lamport.tla.toolbox.tool.tlc.launch.TLCModelLaunchDelegate;
import org.lamport.tla.toolbox.tool.tlc.model.Assignment;
import org.lamport.tla.toolbox.tool.tlc.model.Formula;
import org.lamport.tla.toolbox.tool.tlc.traceexplorer.SimpleTLCState;
import org.lamport.tla.toolbox.util.AdapterFactory;
import org.lamport.tla.toolbox.util.ResourceHelper;
import org.lamport.tla.toolbox.util.UIHelper;

import tla2sany.modanalyzer.SpecObj;
import tla2sany.semantic.ModuleNode;
import tla2sany.semantic.OpDeclNode;
import tla2sany.semantic.OpDefNode;
import tla2sany.semantic.SymbolNode;
import tla2sany.st.Location;
import tlc2.output.MP;

/**
 * Provides utility methods for model manipulation
 */
public class ModelHelper implements IModelConfigurationConstants, IModelConfigurationDefaults
{

    private static final String SPEC_MODEL_DELIM = "___";

	/**
     * Empty location
     */
    public static final int[] EMPTY_LOCATION = new int[] { 0, 0, 0, 0 };

    /**
     * Marker indicating an error in the model
     */
    public static final String TLC_MODEL_ERROR_MARKER = "org.lamport.tla.toolbox.tlc.modelErrorMarker";
    /**
     * Marker indicating the TLC Errors
     */
    public static final String TLC_MODEL_ERROR_MARKER_TLC = "org.lamport.tla.toolbox.tlc.modelErrorTLC";
    /**
     * Marker indicating the SANY Errors
     */
    public static final String TLC_MODEL_ERROR_MARKER_SANY = "org.lamport.tla.toolbox.tlc.modelErrorSANY";

    public static final String TLC_MODEL_ERROR_MARKER_ATTRIBUTE_NAME = "attributeName";
    public static final String TLC_MODEL_ERROR_MARKER_ATTRIBUTE_IDX = "attributeIndex";

	/**
	 * The zero-based id of the BasicFormPage to show the error on.
	 */
	public static final String TLC_MODEL_ERROR_MARKER_ATTRIBUTE_PAGE = "basicFormPageId";

    /**
     * marker on .launch file with boolean attribute modelIsRunning 
     */
    public static final String TLC_MODEL_IN_USE_MARKER = "org.lamport.tla.toolbox.tlc.modelMarker";
    /**
     * marker on .launch file, binary semantics
     */
    public static final String TLC_CRASHED_MARKER = "org.lamport.tla.toolbox.tlc.crashedModelMarker";
    /**
     * model is being run
     */
    private static final String MODEL_IS_RUNNING = "modelIsRunning";
    /**
     * model is locked by a user lock
     */
    private static final String MODEL_IS_LOCKED = "modelIsLocked";
    /**
     * marker on .launch file, with boolean attribute isOriginalTraceShown
     */
    public static final String TRACE_EXPLORER_MARKER = "org.lamport.tla.toolbox.tlc.traceExplorerMarker";
    /**
     * boolean attribute indicating if the original trace of a model checking
     * run should be shown in the error view for that model
     */
    public static final String IS_ORIGINAL_TRACE_SHOWN = "isOriginalTraceShown";
    /**
     * Delimiter used to serialize lists  
     */
    private static final String LIST_DELIMITER = ";";
    private static final String CR = "\n";
    /**
     * Delimiter used to serialize parameter-value pair  
     */
    private static final String PARAM_DELIMITER = ":";
    private static final String SPACE = " ";

    public static final String MC_MODEL_NAME = "MC";
    public static final String FILE_TLA = MC_MODEL_NAME + ".tla";
    public static final String FILE_CFG = MC_MODEL_NAME + ".cfg";
    public static final String FILE_OUT = MC_MODEL_NAME + ".out";

    // trace explorer file names
    public static final String TE_MODEL_NAME = "TE";
    public static final String TE_FILE_TLA = TE_MODEL_NAME + ".tla";
    public static final String TE_FILE_CFG = TE_MODEL_NAME + ".cfg";
    public static final String TE_FILE_OUT = TE_MODEL_NAME + ".out";
    // the file to which TLC's output is written so
    // that the trace explorer can retrieve the trace when it is run
    public static final String TE_TRACE_SOURCE = "MC_TE.out";

    private static final String CHECKPOINT_STATES = MC_MODEL_NAME + ".st.chkpt";
    private static final String CHECKPOINT_QUEUE = "queue.chkpt";
    private static final String CHECKPOINT_VARS = "vars.chkpt";

    /**
     * Constructs the model called Foo___Model_1 from the SpecName Foo
     * if Foo___Model_1 already exists, delivers Foo___Model_2, and so on...
     * 
     * This method tests the existence of the launch configuration AND of the file
     * 
     * @param specProject
     * @return
     */
    public static String constructModelName(IProject specProject)
    {

        return doConstructModelName(specProject, "Model_1");
    }

    /**
     * Implementation of the {@link ModelHelper#constructModelName(IProject, String)}
     * @param specProject
     * @param proposition
     * @return
     */
    public static String doConstructModelName(IProject specProject, String proposition)
    {

        ILaunchConfiguration existingModel = getModelByName(specProject, proposition);
        if (existingModel != null || specProject.getFile(proposition + ".tla").exists())
        {
            String oldNumber = proposition.substring(proposition.lastIndexOf("_") + 1);
            int number = Integer.parseInt(oldNumber) + 1;
            proposition = proposition.substring(0, proposition.lastIndexOf("_") + 1);
            return doConstructModelName(specProject, proposition + number);
        }

        return proposition;
    }
    public static String getModelName(ILaunchConfiguration config) {
    	return getModelName(config.getFile());
    }

    /**
     * Transforms a model name to the name visible to the user 
     * @param modelFile
     * @return
     */
    public static String getModelName(IFile modelFile)
    {
        String name = modelFile.getLocation().removeFileExtension().lastSegment();
        int i = name.indexOf(modelFile.getProject().getName() + SPEC_MODEL_DELIM);
        if (i != -1)
        {
            name = name.substring(i + (modelFile.getProject().getName() + SPEC_MODEL_DELIM).length());
        }
        return name;
    }

    /**
     * Convenience method retrieving the model for the project of the current specification
     * @param modelName name of the model
     * @return launch configuration or null, if not found
     */
    public static ILaunchConfiguration getModelByName(String modelName)
    {
    	Assert.isNotNull(modelName);
        final Spec currentSpec = ToolboxHandle.getCurrentSpec();
        if (currentSpec != null) {
        	return getModelByName(currentSpec.getProject(), modelName);
        } else {
        	return null;
        }
    }

    /**
     * Retrieves the model name by name
     * @param specProject
     * @param modelName
     * @return ILaunchConfiguration representing a model or null
     */
    public static ILaunchConfiguration getModelByName(IProject specProject, String modelName)
    {
        // a model name can be "spec__modelname" or just "modelname"
        if (modelName.indexOf(specProject.getName() + SPEC_MODEL_DELIM) != 0)
        {
            modelName = specProject.getName() + SPEC_MODEL_DELIM + modelName;
        }

        if (modelName.endsWith(".launch"))
        {
            modelName = modelName.substring(0, modelName.length() - ".launch".length());
        }

        try
        {
        	ILaunchConfiguration[] launchConfigurations = getAllLaunchConfigurations();
            for (int i = 0; i < launchConfigurations.length; i++)
            {

                if (launchConfigurations[i].getName().equals(modelName))
                {
                    return launchConfigurations[i];
                }
            }

        } catch (CoreException e)
        {
            TLCActivator.logError("Error finding the model name", e);
        }

        return null;
    }
    
    /**
     * @return All models associated with the given spec
     * @throws CoreException 
     */
    public static List<ILaunchConfiguration> getModelsBySpec(final Spec aSpec) throws CoreException {
    	final List<ILaunchConfiguration> res = new ArrayList<ILaunchConfiguration>();
    	
    	final ILaunchConfiguration[] launchConfigurations = getAllLaunchConfigurations();
		for (int i = 0; i < launchConfigurations.length; i++) {
			final ILaunchConfiguration iLaunchConfiguration = launchConfigurations[i];
			if (getSpecPrefix(iLaunchConfiguration).equals(aSpec.getName())) {
				res.add(iLaunchConfiguration);
			}
		}
    	
    	return res;
    }
    
    private static ILaunchConfiguration[] getAllLaunchConfigurations() throws CoreException {
		final ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		final ILaunchConfigurationType launchConfigurationType = launchManager
				.getLaunchConfigurationType(TLCModelLaunchDelegate.LAUNCH_CONFIGURATION_TYPE);

		return launchManager.getLaunchConfigurations(launchConfigurationType);
    }

    /**
     * Convenience method
     * @param modelFile file containing the model
     * @return ILaunchconfiguration
     */
    public static ILaunchConfiguration getModelByFile(IFile modelFile)
    {
        ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        return launchManager.getLaunchConfiguration(modelFile);
    }
    
    
    /**
     * Rename all models of the given spec to be aligned with the spec name
     * @param aSpec
     * @param aNewSpecName 
     */
    public static void realignModelNames(final Spec aSpec, final String aNewSpecName) {
    	try {
    		final List<ILaunchConfiguration> models = ModelHelper.getModelsBySpec(aSpec);
    		for (ILaunchConfiguration model : models) {
 				renameModel(model, aNewSpecName, getModelSuffix(model));
			}
    	} catch(CoreException e) {
            TLCActivator.logError("Error realigning models.", e);
    	}
    }

	/**
	 * Renames the given model to the new model name passed
	 * @param model
	 * @param specPrefix
	 * @param newModelSuffix
	 */
	public static void renameModel(final ILaunchConfiguration model, final String specPrefix, final String newModelSuffix) {
		try {
			// create the model with the new name
			final ILaunchConfigurationWorkingCopy copy = model.copy(specPrefix + SPEC_MODEL_DELIM + newModelSuffix);
			copy.setAttribute(MODEL_NAME, newModelSuffix);
			copy.doSave();

			// delete the old model
			model.delete();
		} catch (CoreException e) {
            TLCActivator.logError("Error renaming model.", e);
		}
	}
	
	public static String getSpecPrefix(final ILaunchConfiguration aModel) {
        final String oldModelName = aModel.getName(); // old full qualified name
        int indexOf = oldModelName.indexOf(SPEC_MODEL_DELIM); // position model delimiter
    	return oldModelName.substring(0, indexOf);
	}
	
	public static String getModelSuffix(final ILaunchConfiguration aModel) {
        final String oldModelName = aModel.getName(); // old full qualified name
        int indexOf = oldModelName.indexOf(SPEC_MODEL_DELIM); // position model delimiter
    	return oldModelName.substring(indexOf + SPEC_MODEL_DELIM.length());
	}

    /**
     * Saves the config working copy
     * @param config
     */
    public static void doSaveConfigurationCopy(ILaunchConfigurationWorkingCopy config)
    {
        try
        {
            config.doSave();
        } catch (CoreException e)
        {
            TLCActivator.logError("Error saving the model", e);
        }
    }

    /**
     * Creates a serial version of the assignment list, to be stored in the {@link ILaunchConfiguration}
     * 
     * Any assignment consist of a label, parameter list and the right side
     * These parts are serialized as a list with delimiter {@link ModelHelper#LIST_DELIMETER}
     * The parameter list is stored as a list with delimiter {@link ModelHelper#PARAM_DELIMITER}
     * 
     * If the assignment is using a model value {@link Assignment#isModelValue()} == <code>true, the right
     * side is set to the label resulting in (foo = foo) 
     * 
     *   
     */
    public static List<String> serializeAssignmentList(List<Assignment> assignments)
    {
        Iterator<Assignment> iter = assignments.iterator();
        Vector<String> result = new Vector<String>(assignments.size());

        StringBuffer buffer;
        while (iter.hasNext())
        {
            Assignment assign = (Assignment) iter.next();

            buffer = new StringBuffer();

            // label
            buffer.append(assign.getLabel()).append(LIST_DELIMITER);

            // parameters if any
            for (int j = 0; j < assign.getParams().length; j++)
            {
                String param = assign.getParams()[j];
                if (param != null)
                {
                    buffer.append(param);
                }
                buffer.append(PARAM_DELIMITER);
            }
            buffer.append(LIST_DELIMITER);

            // right side
            // encode the model value usage (if model value is set, the assignment right side is equals to the label)

            if (assign.getRight() != null)
            {
                buffer.append(assign.getRight());
            }

            // isModelValue
            buffer.append(LIST_DELIMITER).append((assign.isModelValue() ? "1" : "0"));

            // is symmetrical
            buffer.append(LIST_DELIMITER).append((assign.isSymmetricalSet() ? "1" : "0"));

            result.add(buffer.toString());
        }
        return result;
    }

    /**
     * De-serialize assignment list. 
     * @see ModelHelper#serializeAssignmentList(List)
     */
    public static List<Assignment> deserializeAssignmentList(final List<String> serializedList) {
    	return deserializeAssignmentList(serializedList, false);
    }
    
    /**
     * De-serialize assignment list. 
     * @param serializedList
     * @param stripSymmetry Strips any symmetry definitions from assignments iff true
     * @return The list of all {@link Assignment}
     * @see ModelHelper#serializeAssignmentList(List)
     */
    public static List<Assignment> deserializeAssignmentList(final List<String> serializedList, final boolean stripSymmetry)
    {
        Vector<Assignment> result = new Vector<Assignment>(serializedList.size());
        Iterator<String> iter = serializedList.iterator();
        String[] fields = new String[] { null, "", "", "0", "0" };
        while (iter.hasNext())
        {
            String[] serFields = (iter.next()).split(LIST_DELIMITER);
            System.arraycopy(serFields, 0, fields, 0, serFields.length);

            String[] params;
            if ("".equals(fields[1]))
            {
                params = new String[0];
            } else
            {
                params = fields[1].split(PARAM_DELIMITER);
            }
            // assignment with label as right side are treated as model values
            Assignment assign = new Assignment(fields[0], params, fields[2]);

            // is Model Value
            if (fields.length > 3 && fields[3].equals("1"))
            {
                assign.setModelValue(true);

                // is symmetrical
                if (!stripSymmetry && fields.length > 4 && fields[4].equals("1"))
                {
                    assign.setSymmetric(true);
                }
            }

            result.add(assign);
        }
        return result;
    }

    /**
     * De-serialize formula list, to a list of formulas, that are selected (have a leading "1")
     * 
     * The first character of the formula is used to determine if the formula is enabled in the model 
     * editor or not. This allows the user to persist formulas, which are not used in the current model 
     */
    public static List<Formula> deserializeFormulaList(List<String> serializedList)
    {
        Vector<Formula> result = new Vector<Formula>(serializedList.size());
        Iterator<String> serializedIterator = serializedList.iterator();
        while (serializedIterator.hasNext())
        {
            String entry = serializedIterator.next();
            Formula formula = new Formula(entry.substring(1));
            if ("1".equals(entry.substring(0, 1)))
            {
                result.add(formula);
            }
        }
        return result;
    }

    /**
     * Extract the constants from module node
     * @param moduleNode
     * @return a list of assignments
     */
    public static List<Assignment> createConstantsList(ModuleNode moduleNode)
    {
        OpDeclNode[] constantDecls = moduleNode.getConstantDecls();
        Vector<Assignment> constants = new Vector<Assignment>(constantDecls.length);
        for (int i = 0; i < constantDecls.length; i++)
        {
            String[] params = new String[constantDecls[i].getNumberOfArgs()];
            // pre-fill the empty array
            Arrays.fill(params, EMPTY_STRING);
            Assignment assign = new Assignment(constantDecls[i].getName().toString(), params, EMPTY_STRING);
            constants.add(assign);
        }
        return constants;
    }

    /**
     * Checks whether the constant defined by assignment is defined in the module node
     * @param assignment
     * @param node
     * @return
     */
    public static boolean hasConstant(Assignment assignment, ModuleNode moduleNode)
    {
        OpDeclNode[] constantDecls = moduleNode.getConstantDecls();
        for (int i = 0; i < constantDecls.length; i++)
        {
            if (assignment.getLabel().equals(constantDecls[i].getName().toString())
                    && assignment.getParams().length == constantDecls[i].getNumberOfArgs())
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Extract the variables from module node
     * @param moduleNode
     * @return a string representation of the variables
     * 
     * This method is being called with moduleNode = null when the model is
     * saved when the spec is unparsed.  I added a hack to handle that case,
     * but I'm not positive that there are no further problems that this can
     * cause.  LL. 20 Sep 2009
     */
    public static String createVariableList(ModuleNode moduleNode)
    {
        if (moduleNode == null)
        {
            return "";
        }
        StringBuffer buffer = new StringBuffer();
        OpDeclNode[] variableDecls = moduleNode.getVariableDecls();
        for (int i = 0; i < variableDecls.length; i++)
        {
            buffer.append(variableDecls[i].getName().toString());
            if (i != variableDecls.length - 1)
            {
                buffer.append(", ");
            }
        }
        return buffer.toString();
    }

    /*
     * Returns an array of Strings containing the variables declared
     * in the module.  Added 10 Sep 2009 by LL & DR
     */
    public static String[] createVariableArray(ModuleNode moduleNode)
    {
        OpDeclNode[] variableDecls = moduleNode.getVariableDecls();
        String[] returnVal = new String[variableDecls.length];
        for (int i = 0; i < variableDecls.length; i++)
        {
            returnVal[i] = variableDecls[i].getName().toString();
        }
        return returnVal;
    }

    /*
     * Returns true iff the specified module declares
     * at least one variable.  Added 10 Sep 2009 by LL & DR
     */
    public static boolean hasVariables(ModuleNode moduleNode)
    {
        OpDeclNode[] variableDecls = moduleNode.getVariableDecls();
        return variableDecls.length > 0;
    }

    public static SymbolNode getSymbol(String name, ModuleNode moduleNode)
    {
        return moduleNode.getContext().getSymbol(name);
    }

    /**
     * Extract the operator definitions from module node
     * @param moduleNode
     * @return a list of assignments
     */
    public static List<Assignment> createDefinitionList(ModuleNode moduleNode)
    {
        OpDefNode[] operatorDefinitions = moduleNode.getOpDefs();

        Vector<Assignment> operations = new Vector<Assignment>(operatorDefinitions.length);
        for (int i = 0; i < operatorDefinitions.length; i++)
        {
            String[] params = new String[operatorDefinitions[i].getNumberOfArgs()];
            // pre-fill the empty array
            Arrays.fill(params, "");
            Assignment assign = new Assignment(operatorDefinitions[i].getName().toString(), params, "");
            operations.add(assign);
        }
        return operations;
    }

    /**
     * This method eventually changes the constants list. If the signature constants of both
     * lists are equal, the constants list is left untouched. Otherwise, all constants not present
     * in the constantsFromModule are removed, and all missing are added.
     * <br>
     * For constant comparison {@link Assignment#equalSignature(Assignment)} is used
     * 
     * @param constants the list with constants, eventually the subject of change
     * @param constantsFromModule a list of constants from the module (no right side, no params)
     */
    public static List<Assignment> mergeConstantLists(List<Assignment> constants, List<Assignment> constantsFromModule)
    {
        Vector<Assignment> constantsToAdd = new Vector<Assignment>();
        Vector<Assignment> constantsUsed = new Vector<Assignment>();
        Vector<Assignment> constantsToDelete = new Vector<Assignment>();

        // iterate over constants from module
        for (int i = 0; i < constantsFromModule.size(); i++)
        {
            Assignment fromModule = (Assignment) constantsFromModule.get(i);
            // find it in the module list
            boolean found = false;

            for (int j = 0; j < constants.size(); j++)
            {
                Assignment constant = constants.get(j);
                if (fromModule.equalSignature(constant))
                {
                    // store the information that the constant is used
                    constantsUsed.add(constant);
                    found = true;
                    break;
                }
            }
            // constant is in the module but not in the model
            if (!found)
            {
                // save the constant for adding later
                constantsToAdd.add(fromModule);
            }
        }

        // add all
        constantsToDelete.addAll(constants);
        // remove all used
        constantsToDelete.removeAll(constantsUsed);

        // at this point, all used constants are in the constantUsed list
        constants.retainAll(constantsUsed);

        // all constants to add are in constantsTo Add list
        constants.addAll(constantsToAdd);

        return constantsToDelete;
    }

    /**
     * Retrieves the editor with model instance opened, or null, if no editor found
     * @param model
     * @return
     */
    public static IEditorPart getEditorWithModelOpened(ILaunchConfiguration model)
    {
        if (model != null)
        {
            return UIHelper.getActivePage().findEditor(new FileEditorInput(model.getFile()));
        }
        return null;
    }

    /**
     * Retrieves the working directory for the model
     * <br>Note, this is a handle operation only, the resource returned may not exist
     * @param config 
     * @return the Folder.
     */
    public static IFolder getModelTargetDirectory(ILaunchConfiguration config)
    {
        Assert.isNotNull(config);
        Assert.isTrue(config.getFile().exists());
        return (IFolder) config.getFile().getProject().findMember(getModelName(config.getFile()));
    }

    /**
     * Retrieves a file where the log of the TLC run is written. If isTraceExploration is true, this
     * will return the log file for trace exploration. If that flag is false, this will return the log file
     * for normal model checking.
     * 
     * @param config configuration representing the model
     * @param getTraceExplorerOutput flag indicating if the log file for trace exploration is to be returned
     * @return the file handle, or null
     */
    public static IFile getModelOutputLogFile(ILaunchConfiguration config, boolean getTraceExplorerOutput)
    {
        Assert.isNotNull(config);
        IFolder targetFolder = ModelHelper.getModelTargetDirectory(config);
        if (targetFolder != null && targetFolder.exists())
        {
            String fileName = ModelHelper.FILE_OUT;
            if (getTraceExplorerOutput)
            {
                fileName = ModelHelper.TE_FILE_OUT;
            }
            IFile logFile = (IFile) targetFolder.findMember(fileName);
            if (logFile != null && logFile.exists())
            {
                return logFile;
            }
        }

        return null;
    }
    
    public static void createModelOutputLogFile(ILaunchConfiguration config, InputStream is, IProgressMonitor monitor) throws CoreException {
        Assert.isNotNull(config);
        IFolder targetFolder = ModelHelper.getModelTargetDirectory(config);
		// Create targetFolder which might be missing if the model has never
		// been checked but the user wants to load TLC output anyway.
		// This happens with distributed TLC, where the model is executed
		// remotely and the log is send to the user afterwards.
        if (targetFolder == null || !targetFolder.exists()) {
            String modelName = getModelName(config.getFile());
    		targetFolder = config.getFile().getProject().getFolder(modelName);
    		targetFolder.create(true, true, monitor);
        }
        if (targetFolder != null && targetFolder.exists())
        {
			// Always refresh the folder in case it has to override an existing
			// file that is out-of-sync with the Eclipse foundation layer.
        	targetFolder.refreshLocal(IFolder.DEPTH_INFINITE, monitor);
        	
        	// Import MC.out
        	IFile mcOutFile = targetFolder.getFile(ModelHelper.FILE_OUT);
        	if (mcOutFile.exists()) {
        		mcOutFile.delete(true, monitor);
        	}
        	mcOutFile.create(is, true, monitor); // create closes the InputStream is.
        	
        	// Import MC_TE.out by copying the MC.out file to MC_TE.out.
			// The reason why there are two identical files (MC.out and
			// MC_TE.out) has been lost in history.
        	IFile mcTEOutfile = targetFolder.getFile(ModelHelper.TE_TRACE_SOURCE);
        	if (mcTEOutfile.exists()) {
        		mcTEOutfile.delete(true, monitor);
        	}
        	mcOutFile.copy(mcTEOutfile.getFullPath(), true, monitor);
        }
    }

    /**
     * Retrieves the TLA file that is being model checked on the model run
     * @param config configuration representing the model
     * @return a file handle or <code>null</code>
     */
    public static IFile getModelTLAFile(ILaunchConfiguration config)
    {
        Assert.isNotNull(config);
        IFolder targetFolder = ModelHelper.getModelTargetDirectory(config);
        if (targetFolder != null && targetFolder.exists())
        {
            IFile mcFile = (IFile) targetFolder.findMember(ModelHelper.FILE_TLA);
            if (mcFile != null && mcFile.exists())
            {
                return mcFile;
            }
        }
        return null;
    }

    /**
     * Retrives the TLA file used by the trace explorer
     * @param config configuration representing the model
     * @return a file handle or <code>null</code>
     */
    public static IFile getTraceExplorerTLAFile(ILaunchConfiguration config)
    {
        Assert.isNotNull(config);
        IFolder targetFolder = ModelHelper.getModelTargetDirectory(config);
        if (targetFolder != null && targetFolder.exists())
        {
            IFile teFile = (IFile) targetFolder.findMember(ModelHelper.TE_FILE_TLA);
            if (teFile != null && teFile.exists())
            {
                return teFile;
            }
        }
        return null;
    }

    /**
     * Installs a model modification change listener  
     * @param provider provider for the file representing the model
     * @param runnable a runnable to run if the model is changed 
     */
    public static IResourceChangeListener installModelModificationResourceChangeListener(final IFileProvider provider,
            final Runnable runnable)
    {
        // construct the listener
        IResourceChangeListener listener = new IResourceChangeListener() {
            public void resourceChanged(IResourceChangeEvent event)
            {
                // get the marker changes
                IMarkerDelta[] markerChanges = event.findMarkerDeltas(TLC_MODEL_IN_USE_MARKER, false);

                // usually this list has at most one element
                for (int i = 0; i < markerChanges.length; i++)
                {
                    if (provider.getResource(IFileProvider.TYPE_MODEL).equals(markerChanges[i].getResource()))
                    {
                        UIHelper.runUIAsync(runnable);
                    }
                }
            }
        };

        // add to the workspace root
        ResourcesPlugin.getWorkspace().addResourceChangeListener(listener, IResourceChangeEvent.POST_CHANGE);

        // return the listener
        return listener;
    }

    /**
     * Checks whether the model is running or not
     * @param config
     * @return
     * @throws CoreException
     */
    public static boolean isModelRunning(ILaunchConfiguration config) throws CoreException
    {
        // marker
        IFile resource = config.getFile();
        if (resource.exists())
        {
            IMarker marker;
            IMarker[] foundMarkers = resource.findMarkers(TLC_MODEL_IN_USE_MARKER, false, IResource.DEPTH_ZERO);
            if (foundMarkers.length > 0)
            {
                marker = foundMarkers[0];
                // remove trash if any
                for (int i = 1; i < foundMarkers.length; i++)
                {
                    foundMarkers[i].delete();
                }

                return marker.getAttribute(MODEL_IS_RUNNING, false);
            } else
            {
                return false;
            }
        } else
        {
            return false;
        }
        /*
        // persistence property
        String isLocked = config.getFile().getPersistentProperty(new QualifiedName(TLCActivator.PLUGIN_ID, MODEL_IS_RUNNING));
        if (isLocked == null) 
        {
            return false;
        } else {
            return Boolean.getBoolean(isLocked);
        }
        */

        /*
        return config.getAttribute(MODEL_IS_RUNNING, false);
        */
    }

    /**
     * Checks whether the model is locked or not
     * @param config
     * @return
     * @throws CoreException
     */
    public static boolean isModelLocked(ILaunchConfiguration config) throws CoreException
    {
        // marker
        IFile resource = config.getFile();
        if (resource.exists())
        {
            IMarker marker;
            IMarker[] foundMarkers = resource.findMarkers(TLC_MODEL_IN_USE_MARKER, false, IResource.DEPTH_ZERO);
            if (foundMarkers.length > 0)
            {
                marker = foundMarkers[0];
                // remove trash if any
                for (int i = 1; i < foundMarkers.length; i++)
                {
                    foundMarkers[i].delete();
                }

                return marker.getAttribute(MODEL_IS_LOCKED, false);
            } else
            {
                return false;
            }
        } else
        {
            return false;
        }
        /*
        // persistence property
        String isLocked = config.getFile().getPersistentProperty(new QualifiedName(TLCActivator.PLUGIN_ID, MODEL_IS_RUNNING));
        if (isLocked == null) 
        {
            return false;
        } else {
            return Boolean.getBoolean(isLocked);
        }
        */

        /*
        return config.getAttribute(MODEL_IS_RUNNING, false);
        */
    }

    /**
     * Looks up if the model has a stale marker. The stale marker is installed in case,
     * if the model is locked, but no TLC is running on this model.
     * @param config
     * @return
     * @throws CoreException
     */
    public static boolean isModelStale(ILaunchConfiguration config) throws CoreException
    {
        // marker
        IFile resource = config.getFile();
        if (resource.exists())
        {
            IMarker[] foundMarkers = resource.findMarkers(TLC_CRASHED_MARKER, false, IResource.DEPTH_ZERO);
            if (foundMarkers.length > 0)
            {
                return true;
            } else
            {
                return false;
            }
        } else
        {
            return false;
        }
    }

    /**
     * Returns whether the original trace or the trace with trace explorer expressions from the
     * most recent run of the trace explorer for the model should be shown in the TLC error view.
     * 
     * See {@link ModelHelper#setOriginalTraceShown(ILaunchConfiguration, boolean)} for setting this
     * return value.
     * 
     * @param config
     * @return whether the original trace or the trace with trace explorer expressions from the
     * most recent run of the trace explorer for the model should be shown in the TLC error view
     * @throws CoreException 
     */
    public static boolean isOriginalTraceShown(ILaunchConfiguration config) throws CoreException
    {
        // marker
        IFile resource = config.getFile();
        if (resource.exists())
        {
            IMarker marker;
            IMarker[] foundMarkers = resource.findMarkers(TRACE_EXPLORER_MARKER, false, IResource.DEPTH_ZERO);
            if (foundMarkers.length > 0)
            {
                marker = foundMarkers[0];
                // remove trash if any
                for (int i = 1; i < foundMarkers.length; i++)
                {
                    foundMarkers[i].delete();
                }

                return marker.getAttribute(IS_ORIGINAL_TRACE_SHOWN, true);
            } else
            {
                return true;
            }
        } else
        {
            return true;
        }
    }

    /**
     * Tries to recover model after an abnormal TLC termination
     * It deletes all temporary files on disk and restores the state to unlocked.
     * @param config
     */
    public static void recoverModel(ILaunchConfiguration config) throws CoreException
    {
        IFile resource = config.getFile();
        if (resource.exists())
        {
            // remove any crashed markers
            IMarker[] foundMarkers = resource.findMarkers(TLC_CRASHED_MARKER, false, IResource.DEPTH_ZERO);
            if (foundMarkers.length == 0)
            {
                return;
            }

            ModelHelper.cleanUp(config);

            for (int i = 0; i < foundMarkers.length; i++)
            {
                foundMarkers[i].delete();
            }

            foundMarkers = resource.findMarkers(TLC_MODEL_IN_USE_MARKER, false, IResource.DEPTH_ZERO);
            for (int i = 0; i < foundMarkers.length; i++)
            {
                foundMarkers[i].delete();
            }
        }
    }

    /**
     * Cleans up the TLC working directory
     * @param config
     */
    private static void cleanUp(ILaunchConfiguration config) throws CoreException
    {

    }

    /**
     * Signals that the model is staled
     */
    public static void staleModel(ILaunchConfiguration config) throws CoreException
    {
        config.getFile().createMarker(TLC_CRASHED_MARKER);
    }

    /**
     * Signals the start of model execution
     * @param config
     * @param isRunning whether TLC is running on the config or not
     */
    public static void setModelRunning(ILaunchConfiguration config, boolean isRunning) throws CoreException
    {
        IFile resource = config.getFile();
        if (resource.exists())
        {
            IMarker marker;
            IMarker[] foundMarkers = resource.findMarkers(TLC_MODEL_IN_USE_MARKER, false, IResource.DEPTH_ZERO);
            if (foundMarkers.length > 0)
            {
                marker = foundMarkers[0];
                // remove trash if any
                for (int i = 1; i < foundMarkers.length; i++)
                {
                    foundMarkers[i].delete();
                }
            } else
            {
                marker = resource.createMarker(TLC_MODEL_IN_USE_MARKER);
            }

            marker.setAttribute(MODEL_IS_RUNNING, isRunning);
        }
        /*
        // persistence property
        config.getFile().setPersistentProperty(new QualifiedName(TLCActivator.PLUGIN_ID, MODEL_IS_RUNNING), Boolean.toString(true));
         */

        /*
        // file modification 
        ModelHelper.writeAttributeValue(config, IModelConfigurationConstants.MODEL_IS_RUNNING, true);
         */
    }

    /**
     * Signals that the model is locked if isLocked is true, signals that
     * the model is unlocked if isLocked is false
     * @param config
     * @param lock whether the model should be locked or not
     * @throws CoreException
     */
    public static void setModelLocked(ILaunchConfiguration config, boolean lock) throws CoreException
    {
        IFile resource = config.getFile();
        if (resource.exists())
        {
            IMarker marker;
            IMarker[] foundMarkers = resource.findMarkers(TLC_MODEL_IN_USE_MARKER, false, IResource.DEPTH_ZERO);
            if (foundMarkers.length > 0)
            {
                marker = foundMarkers[0];
                // remove trash if any
                for (int i = 1; i < foundMarkers.length; i++)
                {
                    foundMarkers[i].delete();
                }
            } else
            {
                marker = resource.createMarker(TLC_MODEL_IN_USE_MARKER);
            }

            marker.setAttribute(MODEL_IS_LOCKED, lock);
        }
        /*
        // persistence property
        config.getFile().setPersistentProperty(new QualifiedName(TLCActivator.PLUGIN_ID, MODEL_IS_RUNNING), Boolean.toString(true));
         */

        /*
        // file modification 
        ModelHelper.writeAttributeValue(config, IModelConfigurationConstants.MODEL_IS_RUNNING, true);
         */
    }

    /**
     * Sets whether the original trace or the trace with trace explorer expressions
     * should be shown in the TLC error view for the model represented by this
     * configuration.
     * 
     * Code the raises the TLC error view or updates the TLC error view for a model
     * can use {@link ModelHelper#isOriginalTraceShown(ILaunchConfiguration)} to determine
     * if the original trace should be shown for a given model.
     * 
     * @param config
     * @param isOriginalTraceShown true if the original trace should be shown, false if
     * the trace with trace explorer expressions should be shown
     */
    public static void setOriginalTraceShown(ILaunchConfiguration config, boolean isOriginalTraceShown)
            throws CoreException
    {
        IFile resource = config.getFile();
        if (resource.exists())
        {
            IMarker marker;
            IMarker[] foundMarkers = resource.findMarkers(TRACE_EXPLORER_MARKER, false, IResource.DEPTH_ZERO);
            if (foundMarkers.length > 0)
            {
                marker = foundMarkers[0];
                // remove trash if any
                for (int i = 1; i < foundMarkers.length; i++)
                {
                    foundMarkers[i].delete();
                }
            } else
            {
                marker = resource.createMarker(TRACE_EXPLORER_MARKER);
            }

            marker.setAttribute(IS_ORIGINAL_TRACE_SHOWN, isOriginalTraceShown);
        }
    }

    /**
     * Write a boolean value into the launch config and saves it
     * @param config
     * @param attributeName
     * @param value
     */
    public static void writeAttributeValue(ILaunchConfiguration config, String attributeName, boolean value)
            throws CoreException
    {
        ILaunchConfigurationWorkingCopy copy;
        if (config instanceof ILaunchConfigurationWorkingCopy)
        {
            copy = (ILaunchConfigurationWorkingCopy) config;
        } else
        {
            copy = config.getWorkingCopy();
        }

        copy.setAttribute(attributeName, value);
        copy.doSave();
    }

    /**
     * Simple interface for getting a resource 
     */
    public static interface IFileProvider
    {
        public static final int TYPE_MODEL = 1;
        public static final int TYPE_RESULT = 2;

        public IFile getResource(int type);
    }

    /**
     * Remove a model marker of a particular type
     * @param configuration the model to remove markers from
     * @param type the marker type
     */
    public static void removeModelProblemMarkers(ILaunchConfiguration configuration, String type)
    {
        try
        {
            IMarker[] foundMarkers = configuration.getFile().findMarkers(type, true, IResource.DEPTH_ONE);
            for (int i = 0; i < foundMarkers.length; i++)
            {
                foundMarkers[i].delete();
            }
        } catch (CoreException e)
        {
            TLCActivator.logError("Error removing model markers", e);
        }
    }

    /**
     * Delete all model error markers from a resource
     * @param configuration the model to remove markers from
     */
    public static void removeModelProblemMarkers(ILaunchConfiguration configuration)
    {
        removeModelProblemMarkers(configuration, TLC_MODEL_ERROR_MARKER);
    }

    /**
     * Installs a marker on the model
     * @param resource the model file to install markers on
	 * @param properties a map of attribute names to attribute values 
	 *		(key type : <code>String</code> value type : <code>String</code>, 
	 *		<code>Integer</code>, or <code>Boolean</code>) or <code>null</code>
     */
    public static IMarker installModelProblemMarker(IResource resource, Map<String, Object> properties, String markerType)
    {
        Assert.isNotNull(resource);
        Assert.isTrue(resource.exists());

        try
        {
            // create an empty marker
            IMarker marker = resource.createMarker(markerType);
            marker.setAttributes(properties);
            return marker;
        } catch (CoreException e)
        {
            TLCActivator.logError("Error installing a model marker", e);
        }

        return null;
    }

    /**
     * For an given id that is used in the document retrieves the four coordinates of it's first occurrence.
     * @param document
     * @param searchAdapter
     * @param idRegion
     * @return location coordinates in the sense of {@link Location} class (bl, bc, el, ec).
     * @throws CoreException on errors
     */
    public static int[] calculateCoordinates(IDocument document, FindReplaceDocumentAdapter searchAdapter, String id)
            throws CoreException
    {
        try
        {
            IRegion foundId = searchAdapter.find(0, id, true, true, false, false);
            if (foundId == null)
            {
                return EMPTY_LOCATION;
            } else
            {
                // return the coordinates
                return regionToLocation(document, foundId, true);
            }
        } catch (BadLocationException e)
        {
            throw new CoreException(new Status(IStatus.ERROR, TLCActivator.PLUGIN_ID,
                    "Error during detection of the id position in MC.tla.", e));
        }
    }

    /**
     * Converts four-int-location to a region
     * @param document
     * @param location
     * @return
     * @throws BadLocationException 
     * @deprecated use {@link AdapterFactory#locationToRegion(IDocument, Location)} instead
     */
    public static IRegion locationToRegion(IDocument document, Location location) throws BadLocationException
    {
        int offset = document.getLineOffset(location.beginLine() - 1) + location.beginColumn() - 1;
        int length = document.getLineOffset(location.endLine() - 1) + location.endColumn() - offset;
        return new Region(offset, length);
    }

    /**
     * Recalculate region in a document to four-int-coordinates
     * @param document
     * @param region
     * @param singleLine true, if the region covers one line only
     * @return four ints: begin line, begin column, end line, end column
     * @throws BadLocationException
     */
    public static int[] regionToLocation(IDocument document, IRegion region, boolean singleLine)
            throws BadLocationException
    {
        if (!singleLine)
        {
            throw new IllegalArgumentException("Not implemented");
        }

        int[] coordinates = new int[4];
        // location of the id found in the provided document
        int offset = region.getOffset();
        int length = region.getLength();
        // since the id is written as one word, we are in the same line
        coordinates[0] = document.getLineOfOffset(offset) + 1; // begin line
        coordinates[2] = document.getLineOfOffset(offset) + 1; // end line

        // the columns are relative to the offset of the line
        IRegion line = document.getLineInformationOfOffset(offset);
        coordinates[1] = offset - line.getOffset(); // begin column
        coordinates[3] = coordinates[1] + length; // end column

        // return the coordinates
        return coordinates;
    }

    /**
     * Create a map with marker parameters
     * @param config 
     * @param errorMessage
     * @param severityError
     * @return
     */
    public static Hashtable<String, Object> createMarkerDescription(String errorMessage, int severity)
    {
        Hashtable<String, Object> prop = new Hashtable<String, Object>();

        prop.put(IMarker.SEVERITY, new Integer(severity));
        prop.put(IMarker.MESSAGE, errorMessage);
        prop.put(TLC_MODEL_ERROR_MARKER_ATTRIBUTE_NAME, EMPTY_STRING);
        prop.put(TLC_MODEL_ERROR_MARKER_ATTRIBUTE_IDX, new Integer(0));
        prop.put(IMarker.LOCATION, EMPTY_STRING);
        prop.put(IMarker.CHAR_START, new Integer(0));
        prop.put(IMarker.CHAR_END, new Integer(0));
        return prop;
    }

    /**
     * Using the supplied findReplaceAdapter finds the name of the attribute 
     * (saved in the comment, previous to the region in which the error has been detected) 
     * 
     * @param configuration the configuration of the launch
     * @param document the document of the file containing the generated model .tla file
     * @param searchAdapter the search adapter on the document
     * @param message the error message
     * @param severity the error severity
     * @param coordinates coordinates in the document describing the area that is the id
     * 
     * @return a map object containing the information required for the marker installation:
     * <ul>
     *  <li>IMarker.SEVERITY</li>
     *  <li>IMarker.MESSAGE</li>
     *  <li>TLC_MODEL_ERROR_MARKER_ATTRIBUTE_NAME</li>
     *  <li>TLC_MODEL_ERROR_MARKER_ATTRIBUTE_IDX</li>
     *  <li>IMarker.LOCATION</li>
     *  <li>IMarker.CHAR_START</li>
     *  <li>IMarker.CHAR_END</li>
     * </ul> 
     * @throws CoreException if something goes wrong
     */
    public static Hashtable<String, Object> createMarkerDescription(ILaunchConfiguration configuration, IDocument document,
            FindReplaceDocumentAdapter searchAdapter, String message, int severity, int[] coordinates)
            throws CoreException
    {
        String attributeName;
        Region errorRegion = null;
        int attributeIndex = -1;
        try
        {
            // find the line in the document
            IRegion lineRegion = document.getLineInformation(coordinates[0] - 1);
            if (lineRegion != null)
            {
                int errorLineOffset = lineRegion.getOffset();

                // find the previous comment
                IRegion commentRegion = searchAdapter.find(errorLineOffset, ModelWriter.COMMENT, false, false, false,
                        false);

                // find the next separator
                IRegion separatorRegion = searchAdapter.find(errorLineOffset, ModelWriter.SEP, true, false, false,
                        false);
                if (separatorRegion != null && commentRegion != null)
                {
                    // find the first attribute inside of the
                    // comment
                    IRegion attributeRegion = searchAdapter.find(commentRegion.getOffset(), ModelWriter.ATTRIBUTE
                            + "[a-z]*[A-Z]*", true, false, false, true);
                    if (attributeRegion != null)
                    {
                        // get the attribute name without the
                        // attribute marker
                        attributeName = document.get(attributeRegion.getOffset(), attributeRegion.getLength())
                                .substring(ModelWriter.ATTRIBUTE.length());

                        // find the index
                        IRegion indexRegion = searchAdapter.find(attributeRegion.getOffset()
                                + attributeRegion.getLength(), ModelWriter.INDEX + "[0-9]+", true, false, false, true);
                        if (indexRegion != null && indexRegion.getOffset() < separatorRegion.getOffset())
                        {
                            // index value found
                            String indexString = document.get(indexRegion.getOffset(), indexRegion.getLength());
                            if (indexString != null && indexString.length() > 1)
                            {
                                try
                                {
                                    attributeIndex = Integer.parseInt(indexString.substring(1));
                                } catch (NumberFormatException e)
                                {
                                    throw new CoreException(new Status(IStatus.ERROR, TLCActivator.PLUGIN_ID,
                                            "Error during detection of the error position in MC.tla."
                                                    + "Error parsing the attribute index. " + message, e));
                                }
                            }
                        } else
                        {
                            // no index
                        }

                        // the first character of the next line
                        // after the comment

                        IRegion firstBlockLine = document.getLineInformation(document.getLineOfOffset(commentRegion
                                .getOffset()) + 1);
                        int beginBlockOffset = firstBlockLine.getOffset();
                        // get the user input
                        if (attributeName.equals(MODEL_PARAMETER_NEW_DEFINITIONS))
                        {
                            // there is no identifier in this
                            // block
                            // the user input starts directly
                            // from the first character
                        } else
                        {
                            // the id-line representing the
                            // identifier "id_number ==" comes
                            // first
                            // the user input starts only on the
                            // second line
                            // so adding the length of the
                            // id-line
                            beginBlockOffset = beginBlockOffset + firstBlockLine.getLength() + 1;
                        }

                        // calculate the error region

                        // end line coordinate
                        if (coordinates[2] == 0)
                        {
                            // not set
                            // mark one char starting from the
                            // begin column
                            errorRegion = new Region(errorLineOffset + coordinates[1] - beginBlockOffset, 1);
                        } else if (coordinates[2] == coordinates[0])
                        {
                            // equals to the begin line
                            // mark the actual error region
                            int length = coordinates[3] - coordinates[1];

                            errorRegion = new Region(errorLineOffset + coordinates[1] - beginBlockOffset,
                                    (length == 0) ? 1 : length);
                        } else
                        {
                            // the part of the first line from
                            // the begin column to the end
                            int summedLength = lineRegion.getLength() - coordinates[1];

                            // iterate over all full lines
                            for (int l = coordinates[0] + 1; l < coordinates[2]; l++)
                            {
                                IRegion line = document.getLineInformation(l - 1);
                                summedLength = summedLength + line.getLength();
                            }
                            // the part of the last line to the
                            // end column
                            summedLength += coordinates[3];

                            errorRegion = new Region(errorLineOffset + coordinates[1] - beginBlockOffset, summedLength);
                        }

                        // install the marker showing the
                        // information in the corresponding
                        // attribute (and index), at the given place

                    } else
                    {
                        // problem could not detect attribute
                        throw new CoreException(new Status(IStatus.ERROR, TLCActivator.PLUGIN_ID,
                                "Error during detection of the error position in MC.tla."
                                        + "Could not detect the attribute. " + message));
                    }
                } else
                {
                    // problem could not detect block
                    throw new CoreException(new Status(IStatus.ERROR, TLCActivator.PLUGIN_ID,
                            "Error during detection of the error position in MC.tla."
                                    + "Could not detect definition block. " + message));
                }
            } else
            {
                // problem could not detect line
                throw new CoreException(new Status(IStatus.ERROR, TLCActivator.PLUGIN_ID,
                        "Error during detection of the error position in MC.tla."
                                + "Could not data on specified location. " + message));
            }
        } catch (BadLocationException e)
        {
            throw new CoreException(new Status(IStatus.ERROR, TLCActivator.PLUGIN_ID,
                    "Error during detection of the error position in MC.tla." + "Accessing MC.tla file failed. "
                            + message, e));
        }

        // If the message refers to module MC, this should be replaced with
        // the location in the model.
        message = getMessageWithoutMC(message, attributeName, attributeIndex);

        // create the return object
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(IMarker.SEVERITY, new Integer(severity));
        props.put(IMarker.MESSAGE, message);
        props.put(TLC_MODEL_ERROR_MARKER_ATTRIBUTE_NAME, attributeName);
        props.put(TLC_MODEL_ERROR_MARKER_ATTRIBUTE_IDX, new Integer(attributeIndex));
        props.put(IMarker.LOCATION, "");
        props.put(IMarker.CHAR_START, new Integer(errorRegion.getOffset()));
        props.put(IMarker.CHAR_END, new Integer(errorRegion.getOffset() + errorRegion.getLength()));

        return props;
    }

    /**
     * This method takes an error message generated by SANY for the
     * MC.tla file and attempts to remove the mention of the MC file and insert
     * the name of the model attribute and location within that attribute
     * where the parse error occured.
     * 
     * For example, the location in the message returned by SANY that says
     * "at line 25, column 2 in module MC" will be replaced with something
     * like "in Definition Overrides, line 2".
     * 
     * It is definitely possible that the set of expressions searched for by this method
     * is not exhaustive, so some messages will remain the same even if they mention the
     * MC file.
     * 
     * @param message the SANY error message for the MC file
     * @param attributeName the name of the model attribute where the error occured
     * @param attributeIndex the 0-based index of the error within the model attribute.
     * For example, if the second definition override caused an error, the attributeIndex
     * should be 1. It can be -1 if no index is found, in which case no information about
     * the location within the model attribute will be included in the message.
     * @return the message without MC location information and with model location information
     */
    private static String getMessageWithoutMC(String message, String attributeName, int attributeIndex)
    {
        if (message.indexOf("in module MC") != -1 || message.indexOf("of module MC") != -1)
        {
            // first possible expression
            String[] splitMessage = message.split("at line [0-9]{1,}, column [0-9]{1,} in module MC", 2);
            if (splitMessage.length != 2)
            {
                // split around other possibility
                splitMessage = message.split(
                        "line [0-9]{1,}, col [0-9]{1,} to line [0-9]{1,}, col [0-9]{1,} of module MC", 2);
            }
            if (splitMessage.length == 2)
            {
                String toReturn = splitMessage[0] + " in " + getSectionNameFromAttributeName(attributeName);
                if (attributeIndex != -1)
                {
                    // the exact location is known
                    toReturn = toReturn + " at line " + (attributeIndex + 1);
                }
                return toReturn + splitMessage[1];
            } else
            {
                // cannot find expression containing MC module
                // even though it is in the message
                return message;
            }
        } else
        {
            return message;
        }
    }

    /**
     * This gets the title of the section in the model editor
     * corresponding to the attributeName.
     * 
     * @param attributeName
     * @return
     */
    private static String getSectionNameFromAttributeName(String attributeName)
    {
        if (attributeName.equals(MODEL_CORRECTNESS_INVARIANTS))
        {
            return "Invariants";
        } else if (attributeName.equals(MODEL_CORRECTNESS_PROPERTIES))
        {
            return "Properties";
        } else if (attributeName.equals(MODEL_PARAMETER_ACTION_CONSTRAINT))
        {
            return "Action Contraint";
        } else if (attributeName.equals(MODEL_PARAMETER_CONSTRAINT))
        {
            return "Constraint";
        } else if (attributeName.equals(MODEL_PARAMETER_CONSTANTS))
        {
            return "What is the model?";
        } else if (attributeName.equals(MODEL_PARAMETER_DEFINITIONS))
        {
            return "Definition Override";
        } else if (attributeName.equals(MODEL_PARAMETER_NEW_DEFINITIONS))
        {
            return "Additional Definitions";
        } else if (attributeName.equals(MODEL_PARAMETER_MODEL_VALUES))
        {
            return "Model Values";
        } else if (attributeName.equals(MODEL_BEHAVIOR_CLOSED_SPECIFICATION))
        {
            return "Temporal formula";
        } else if (attributeName.equals(MODEL_BEHAVIOR_SEPARATE_SPECIFICATION_INIT))
        {
            return "Init";
        } else if (attributeName.equals(MODEL_BEHAVIOR_SEPARATE_SPECIFICATION_NEXT))
        {
            return "Next";
        } else if (attributeName.equals(MODEL_PARAMETER_VIEW))
        {
            return "View";
        } else if (attributeName.equals(MODEL_EXPRESSION_EVAL))
        {
            return "Expression";
        }
        return attributeName;
    }

    /**
     * Retrieves error markers of the model
     * @param config
     * @return
     * @throws CoreException
     */
    public static IMarker[] getModelProblemMarker(ILaunchConfiguration config) throws CoreException
    {
        IFile resource = config.getFile();
        if (resource.exists())
        {
            IMarker[] foundMarkers = resource.findMarkers(TLC_MODEL_ERROR_MARKER, true, IResource.DEPTH_ZERO);
            return foundMarkers;
        }

        return new IMarker[0];
    }

    /**
     * Checks whether the checkpoint files exist for a given model
     * If doRefresh is set to true, this method will refresh the model directory,
     * and if a checkpoint folder is found, it will refresh the contents of that folder.
     * This means that the eclipse workspace representation of that directory will
     * synch with the file system. This is a long running job, so this method should not
     * be called within the running of another job unless the scheduling rule for
     * refreshing the model directory is included in the scheduling rule of the job which
     * is calling this method. This scheduling rule can be found by calling
     * 
     * Note: Because the Toolbox deletes any existing checkpoint when running TLC,
     * there should be at most one checkpoint.  Therefore, this method should return an array
     * of length 0 or 1.
     * 
     * {@link IResourceRuleFactory#refreshRule(IResource)}
     * @param config
     * @param doRefresh whether the model directory's contents and any checkpoint
     * folders contents should be refreshed
     * @return the array of checkpoint directories, sorted from last to first
     */
    public static IResource[] getCheckpoints(ILaunchConfiguration config, boolean doRefresh) throws CoreException
    {
        // yy-MM-dd-HH-mm-ss
        Pattern pattern = Pattern.compile("[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{2}");

        Vector<IResource> checkpoints = new Vector<IResource>();
        IFolder directory = getModelTargetDirectory(config);

        if (directory != null && directory.exists())
        {
            // refreshing is necessary because TLC creates
            // the checkpoint folders, but they may not have
            // been incorporated into the toolbox workspace
            // yet
            // the depth is one to find any checkpoint folders
            if (doRefresh)
            {
                directory.refreshLocal(IResource.DEPTH_ONE, null);
            }
            IResource[] members = directory.members();
            for (int i = 0; i < members.length; i++)
            {
                if (members[i].getType() == IResource.FOLDER)
                {
                    Matcher matcher = pattern.matcher(members[i].getName());
                    if (matcher.matches())
                    {
                        // if there is a checkpoint folder, it is necessary
                        // to refresh its contents because they may not
                        // be part of the workspace yet
                        if (doRefresh)
                        {
                            members[i].refreshLocal(IResource.DEPTH_ONE, null);
                        }
                        if (((IFolder) members[i]).findMember(CHECKPOINT_QUEUE) != null
                                && ((IFolder) members[i]).findMember(CHECKPOINT_VARS) != null
                                && ((IFolder) members[i]).findMember(CHECKPOINT_STATES) != null)
                        {
                            checkpoints.add(members[i]);
                        }
                    }
                }
            }
        }
        IResource[] result = (IResource[]) checkpoints.toArray(new IResource[checkpoints.size()]);
        // sort the result
        Arrays.sort(result, new Comparator<IResource>() {
            public int compare(IResource arg0, IResource arg1)
            {
                return arg0.getName().compareTo(arg1.getName());
            }
        });

        return result;
    }

    /**
     * Find the IDs in the given text and return the array of 
     * regions pointing to those or an empty array, if no IDs were found.
     * An ID is scheme_timestamp, created by {@link ModelWriter#getValidIdentifier(String)} e.G. next_125195338522638000
     * @param text text containing IDs (error text)
     * @return array of regions or empty array
     */
    public static IRegion[] findIds(String text)
    {
        return ModelWriter.findIds(text);
    }

    /**
     * Finds the locations in the given text and return the array of 
     * regions pointing to those or an empty array, if no location were found.
     * A location is a pointer in the TLA file, e.G. "line 11, col 8 to line 14, col 26 of module Foo"
     * @param text text containing locations (error text)
     * @return array of regions or empty array
     */
    public static IRegion[] findLocations(String text)
    {
        if (text == null || text.length() == 0)
        {
            return new IRegion[0];
        }

        Matcher matcher = Location.LOCATION_MATCHER.matcher(text);
        Vector<IRegion> regions = new Vector<IRegion>();
        while (matcher.find())
        {
            regions.add(new Region(matcher.start(), matcher.end() - matcher.start()));
        }
        // look for this pattern also
        // this pattern appears when there
        // is an error evaluating a nested expression
        matcher = Location.LOCATION_MATCHER4.matcher(text);
        while (matcher.find())
        {
            regions.add(new Region(matcher.start(), matcher.end() - matcher.start()));
        }
        return regions.toArray(new IRegion[regions.size()]);
    }

    /**
     * Returns the OpDefNode with name equal to input string
     * Returns null if there is no such OpDefNode
     * @param name
     * @return
     */
    public static OpDefNode getOpDefNode(String name)
    {
        SpecObj specObj = ToolboxHandle.getCurrentSpec().getValidRootModule();
        /*
         * SpecObj can be null if the spec is unparsed.
         */
        if (specObj != null)
        {
            OpDefNode[] opDefNodes = specObj.getExternalModuleTable().getRootModule().getOpDefs();
            for (int j = 0; j < opDefNodes.length; j++)
            {
                if (opDefNodes[j].getName().toString().equals(name))
                {
                    return opDefNodes[j];
                }
            }
        }
        return null;
    }

    /**
     * Checks, whether a model attribute is a list 
     * @param attributeName name of the attribute, see {@link IModelConfigurationConstants}
     * @return true for invariants, properties, constants and definition overriders
     */
    public static boolean isListAttribute(String attributeName)
    {
        if (attributeName != null
                && (attributeName.equals(IModelConfigurationConstants.MODEL_CORRECTNESS_INVARIANTS)
                        || attributeName.equals(IModelConfigurationConstants.MODEL_CORRECTNESS_PROPERTIES)
                        || attributeName.equals(IModelConfigurationConstants.MODEL_PARAMETER_CONSTANTS) || attributeName
                        .equals(IModelConfigurationConstants.MODEL_PARAMETER_DEFINITIONS)))
        {
            return true;
        }
        return false;
    }

    /**
     * A convenience method for access to the root module node
     * @return a module or null, if spec not parsed
     */
    public static ModuleNode getRootModuleNode()
    {
        SpecObj specObj = ToolboxHandle.getSpecObj();
        if (specObj != null)
        {
            return specObj.getExternalModuleTable().getRootModule();
        }
        return null;
    }

    /**
     * Creates the files if they do not exist, and
     * sets the contents of each file equal to "".
     * 
     * @param files
     * @param monitor
     */
    public static void createOrClearFiles(final IFile[] files, IProgressMonitor monitor)
    {
        ISchedulingRule fileRule = MultiRule.combine(ResourceHelper.getModifyRule(files), ResourceHelper
                .getCreateRule(files));

        // create files
        try
        {
            ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
                public void run(IProgressMonitor monitor) throws CoreException
                {
                    for (int i = 0; i < files.length; i++)
                    {
                        if (files[i].exists())
                        {
                            files[i].setContents(new ByteArrayInputStream("".getBytes()), IResource.DERIVED
                                    | IResource.FORCE, new SubProgressMonitor(monitor, 1));
                        } else
                        {
                            files[i].create(new ByteArrayInputStream("".getBytes()), IResource.DERIVED
                                    | IResource.FORCE, new SubProgressMonitor(monitor, 1));
                        }
                    }
                }

            }, fileRule, IWorkspace.AVOID_UPDATE, new SubProgressMonitor(monitor, 100));
        } catch (CoreException e)
        {
            TLCActivator.logError("Error creating files.", e);
        }

    }
    
    public static void deleteModels(ILaunchConfiguration[] ilcs, IProgressMonitor monitor) throws CoreException {
    	for (int i = 0; i < ilcs.length; i++) {
    		deleteModel(ilcs[i], monitor);
		}
    }

    /**
     * Deletes the given model plus its model folder
     * @param monitor
     * @param ilc the config file corresponding to the model folder
     * @throws CoreException
     */
	public static void deleteModel(final ILaunchConfiguration ilc,
			IProgressMonitor monitor) throws CoreException {
		
		final IResource[] members;

		// if the model has never been model checked, no model folder will exist
		final IFolder modelFolder = ModelHelper.getModelTargetDirectory(ilc);
		if(modelFolder != null) {
			members = new IResource[2];
			members[0] = modelFolder; // model folder
			members[1] = ilc.getFile(); // modle launch config
		} else {
			members = new IResource[]{ilc.getFile()};
		}
		
		// schedule combined deletion of both the model folder as well as the
		// launch config
		final ISchedulingRule deleteRule = ResourceHelper.getDeleteRule(members);

		ResourcesPlugin.getWorkspace().run(
				new IWorkspaceRunnable() {

					/* (non-Javadoc)
					 * @see org.eclipse.core.resources.IWorkspaceRunnable#run(org.eclipse.core.runtime.IProgressMonitor)
					 */
					public void run(IProgressMonitor subMonitor)
							throws CoreException {
						subMonitor.beginTask("Deleting files", members.length);

						// actually deletes all IResource members
						try {
							for (int i = 0; i < members.length; i++) {
								members[i].delete(IResource.FORCE,
										new SubProgressMonitor(subMonitor, 1));
							}
						} catch (CoreException e) {
							TLCActivator.logError("Error deleting a file "
									+ e.getMessage(), e);
							throw e;
						}
						
						subMonitor.done();
					}
				}, deleteRule, IWorkspace.AVOID_UPDATE,
				new SubProgressMonitor(monitor, members.length));
	}

    /**
     * Copies the module files that are extended by specRootFile into the
     * folder given by targetFolderPath.
     * @param specRootFile the file corresponding to the root module
     * @param targetFolderPath the path of the folder to which the extended modules are to be copied
     * @param monitor - the progress monitor
     * @param STEP the unit of work this corresponds to in the progress monitor 
     * @param project the project that contains the specRootFile
     * @throws CoreException
     */
    public static void copyExtendedModuleFiles(IFile specRootFile, IPath targetFolderPath, IProgressMonitor monitor,
            int STEP, IProject project) throws CoreException
    {
        // get the list of dependent modules
        List<String> extendedModules = ToolboxHandle.getExtendedModules(specRootFile.getName());

        // iterate and copy modules that are needed for the spec
        IFile moduleFile = null;
        for (int i = 0; i < extendedModules.size(); i++)
        {
            String module = extendedModules.get(i);
            // only take care of user modules
            if (ToolboxHandle.isUserModule(module))
            {
                moduleFile = ResourceHelper.getLinkedFile(project, module, false);
                if (moduleFile != null)
                {
                    moduleFile.copy(targetFolderPath.append(moduleFile.getProjectRelativePath()), IResource.DERIVED
                            | IResource.FORCE, new SubProgressMonitor(monitor, STEP / extendedModules.size()));
                }

                // TODO check the existence of copied files
            }
        }
    }

    /**
     * Returns a possibly empty List of {@link SimpleTLCState} that represents
     * the error trace produced by the most recent run of TLC on config, if an error
     * trace was produced.
     * 
     * @param config
     * @return
     */
    public static List<SimpleTLCState> getErrorTrace(ILaunchConfiguration config)
    {
        // try
        // {
        // File logFile = getModelOutputLogFile(config, false).getFullPath().toFile();
        // if (logFile.exists())
        // {
        // FileInputStream fis = new FileInputStream(logFile);
        // } else
        // {
        // TLCActivator.getDefault().logDebug("Could not locate log file for model " + config.getName() + ".");
        // }
        // } catch (FileNotFoundException e)
        // {
        // e.printStackTrace();
        // }

        /*
         * Use a file editor input and file document provider to gain access to the
         * document representation of the file containing the trace.
         */
        FileEditorInput logFileEditorInput = new FileEditorInput(getTraceSourceFile(config));
        FileDocumentProvider logFileDocumentProvider = new FileDocumentProvider();
        try
        {
            logFileDocumentProvider.connect(logFileEditorInput);
            IDocument logFileDocument = logFileDocumentProvider.getDocument(logFileEditorInput);

            FindReplaceDocumentAdapter logFileSearcher = new FindReplaceDocumentAdapter(logFileDocument);

            // the regular expression for searching for the start tag for state print outs
            String regExStartTag = MP.DELIM + MP.STARTMSG + "[0-9]{4}" + MP.COLON + MP.STATE + SPACE + MP.DELIM + CR;
            // the regular expression for searching for the end tag for state print outs
            String regExEndTag = MP.DELIM + MP.ENDMSG + "[0-9]{4}" + SPACE + MP.DELIM;

            IRegion startTagRegion = logFileSearcher.find(0, regExStartTag, true, true, false, true);

            // vector of SimpleTLCStates
            Vector<SimpleTLCState> trace = new Vector<SimpleTLCState>();

            while (startTagRegion != null)
            {
                IRegion endTagRegion = logFileSearcher.find(startTagRegion.getOffset() + startTagRegion.getLength(),
                        regExEndTag, true, true, false, true);

                if (endTagRegion != null)
                {
                    int stateInputStart = startTagRegion.getOffset() + startTagRegion.getLength();
                    int stateInputLength = endTagRegion.getOffset() - stateInputStart;
                    // string from which the state can be parsed
                    String stateInputString = logFileDocument.get(stateInputStart, stateInputLength);

                    trace.add(SimpleTLCState.parseSimpleTLCState(stateInputString));

                } else
                {
                    TLCActivator.logDebug("Found start tag region in model log file without end tag for model "
                            + config.getName() + ".");
                }
                // TLCActivator.getDefault().logDebug(logFileDocument.get(startTagRegion.getOffset() + startTagRegion.getLength(),
                // endTagRegion.getOffset() - startTagRegion.getLength() - startTagRegion.getOffset()));

                startTagRegion = logFileSearcher.find(startTagRegion.getOffset() + startTagRegion.getLength(),
                        regExStartTag, true, true, false, true);
            }

            return trace;
        } catch (CoreException e)
        {
            TLCActivator.logError("Error connecting to model log file for model " + config.getName() + ".", e);
        } catch (BadLocationException e)
        {
            TLCActivator.logError("Error searching model log file for " + config.getName() + ".", e);
        } finally
        {
            /*
             * The document provider is not needed. Always disconnect it to avoid a memory leak.
             * 
             * Keeping it connected only seems to provide synchronization of
             * the document with file changes. That is not necessary in this context.
             */
            logFileDocumentProvider.disconnect(logFileEditorInput);
        }

        return new Vector<SimpleTLCState>();
    }

    /**
     * Determines if the spec with root module rootModuleName is dependent on a
     * module with the same name as the root module used for model checking.
     * 
     * @param rootModuleName
     * @return
     */
    public static boolean containsModelCheckingModuleConflict(String rootModuleName)
    {
        String rootModuleFileName = rootModuleName;
        if (!rootModuleName.endsWith(ResourceHelper.TLA_EXTENSION))
        {
            rootModuleFileName = ResourceHelper.getModuleFileName(rootModuleName);
        }
        List<String> extendedModuleNames = ToolboxHandle.getExtendedModules(rootModuleFileName);
        Iterator<String> it = extendedModuleNames.iterator();
        while (it.hasNext())
        {
            String moduleName = it.next();
            if (moduleName.equals(FILE_TLA))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Determines if the spec with root module rootModuleName is dependent on a
     * module with the same name as the root module used for trace exploration.
     * 
     * @param rootModuleName
     * @return
     */
    public static boolean containsTraceExplorerModuleConflict(String rootModuleName)
    {
        String rootModuleFileName = rootModuleName;
        if (!rootModuleName.endsWith(ResourceHelper.TLA_EXTENSION))
        {
            rootModuleFileName = ResourceHelper.getModuleFileName(rootModuleName);
        }
        List<String> extendedModuleNames = ToolboxHandle.getExtendedModules(rootModuleFileName);
        Iterator<String> it = extendedModuleNames.iterator();
        while (it.hasNext())
        {
            String moduleName = it.next();
            if (moduleName.equals(TE_FILE_TLA))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns a handle to the output file {@link ModelHelper#TE_TRACE_SOURCE} used by the
     * trace explorer to retrieve the trace from the most recent run of TLC on
     * the config.
     * 
     * Note that this is a handle-only operation. The file need not exist in the
     * underlying file system.
     * 
     * @param config
     * @return
     */
    public static IFile getTraceSourceFile(ILaunchConfiguration config)
    {

        Assert.isNotNull(config);
        IFolder targetFolder = ModelHelper.getModelTargetDirectory(config);
        if (targetFolder != null && targetFolder.exists())
        {
            IFile logFile = targetFolder.getFile(TE_TRACE_SOURCE);
            Assert.isNotNull(logFile);
            return logFile;
        }
        return null;
    }

	public static String prettyPrintConstants(final ILaunchConfiguration config, String delim) throws CoreException {
		return prettyPrintConstants(config, delim, false);
	}
	
	public static String prettyPrintConstants(final ILaunchConfiguration config, String delim, boolean align) throws CoreException {
		final List<Assignment> assignments = deserializeAssignmentList(
				config.getAttribute(IModelConfigurationConstants.MODEL_PARAMETER_CONSTANTS, new ArrayList<String>()));
		
		// Sort the assignments: Basic assignments alphabetically first, Set
		// model values including symmetric ones (alphabetically), Basic model
		// values.
		Collections.sort(assignments, new Comparator<Assignment>() {
			public int compare(Assignment a1, Assignment a2) {
				if (a1.isSimpleModelValue() && a2.isSimpleModelValue()) {
					return a1.getLeft().compareTo(a2.getLeft());
				} else if (a1.isSetOfModelValues() && a2.isSetOfModelValues()) {
					return a1.getLeft().compareTo(a2.getLeft());
				} else if (a1.isSimpleModelValue() && !a2.isModelValue()) {
					return 1;
				} else if (a1.isSimpleModelValue() && a2.isSetOfModelValues()) {
					return 1;
				} else if (a1.isSetOfModelValues() && !a2.isModelValue()) {
					return 1;
				} else if (a1.isSetOfModelValues() && a2.isSimpleModelValue()) {
					return -1;
				} else if (!a1.isModelValue() && a2.isModelValue()) {
					return -1;
				} else {
					// Basic assignments
					return a1.getLeft().compareTo(a2.getLeft());
				}
			}
		});
		
		// Determine the longest label of the assignment's left hand side.
		int longestLeft = 0;
		for (int i = 0; i < assignments.size() && align; i++) {
			final Assignment assignment = assignments.get(i);
			if (!assignment.isSimpleModelValue()) {
				longestLeft = Math.max(longestLeft, assignment.getLeft().length());
			}
		}

		final StringBuffer buf = new StringBuffer();
		for (int i = 0; i < assignments.size(); i++) {
			final Assignment assignment = assignments.get(i);
			if (assignment.isSimpleModelValue()) {
				buf.append("Model values: ");
				for (; i < assignments.size(); i++) {
					buf.append(assignments.get(i).prettyPrint());
					if (i < assignments.size() - 1) {
						buf.append(", ");
					}
				}
			} else if (align) {
				final int length = longestLeft - assignment.getLeft().length();
				final StringBuffer whitespaces = new StringBuffer(length);
				for (int j = 0; j < length; j++) {
					whitespaces.append(" ");
				}
				buf.append(assignment.prettyPrint(whitespaces.toString()));
				if (i < assignments.size() - 1) {
					buf.append(delim);
				}
			} else {
				buf.append(assignment.prettyPrint());
				if (i < assignments.size() - 1) {
					buf.append(delim);
				}
			}
		}
		return buf.toString();
	}
}
