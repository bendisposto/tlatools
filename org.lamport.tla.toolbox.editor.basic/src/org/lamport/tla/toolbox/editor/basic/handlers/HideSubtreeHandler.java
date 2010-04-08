package org.lamport.tla.toolbox.editor.basic.handlers;

import org.eclipse.jface.text.ITextSelection;
import org.lamport.tla.toolbox.editor.basic.TLAEditor;
import org.lamport.tla.toolbox.editor.basic.proof.IProofFoldCommandIds;
import org.lamport.tla.toolbox.editor.basic.util.EditorUtil;

/**
 * Handler for command that hides the proof subtree rooted
 * at the step currently containing the caret. It collapses
 * all folds in the subtree.
 * 
 * @author Daniel Ricketts
 *
 */
public class HideSubtreeHandler extends ProofFoldCommandHandler
{

    /**
     * This method is used to update the enablement state. This has the
     * effect of graying out any menu items for the
     * command if the handler is disabled. Through experimentation, this method seems to be
     * called just before such menu items are rendered in the UI and
     * just before the handler is executed.
     */
    public void setEnabled(Object context)
    {
        TLAEditor editor = EditorUtil.getTLAEditorWithFocus();

        if (editor != null)
        {
            if (editor.getProofStructureProvider() != null)
            {
                setBaseEnabled(editor.getProofStructureProvider().canRunFoldOperation(
                        IProofFoldCommandIds.COLLAPSE_SUBTREE,
                        (ITextSelection) editor.getSelectionProvider().getSelection()));
            }
        }
    }

}
