package gov.noaa.nws.ncep.viz.ui.display;

import gov.noaa.nws.ncep.viz.common.ISaveableResourceData;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.PartInitException;

import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.viz.ui.editor.EditorInput;

/**
 * 
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 02/09/13      #972      Greg Hull   Created to derive from AbstractNcEditor and
 *                                     replace NCMapEditor
 * 07/28/2016    R17954    B. Yin      Pop up PGEN SAVE dialog if editor contains PGEN
 * 
 * </pre>
 * 
 * @author ghull
 * @version 1.0
 */
public class NatlCntrsEditor extends AbstractNcEditor {

    @Override
    protected void validateEditorInput(EditorInput input)
            throws PartInitException {
        super.validateEditorInput(input);

        // TODO : implement this to validate the
        if (input.getPaneManager() != null
                && input.getPaneManager() instanceof NCPaneManager == false) {

            throw new PartInitException("Expected pane manager of type: "
                    + NCPaneManager.class);
        }

        // Renderable displays are validated in the base class
    }

    @Override
    public boolean isDirty() {
        for (IDisplayPane pane : getDisplayPanes()) {
            for (ResourcePair resourcePair : pane.getDescriptor()
                    .getResourceList()) {
                if (resourcePair.getResourceData() instanceof ISaveableResourceData) {
                    return ((ISaveableResourceData) resourcePair
                            .getResourceData()).isResourceDataDirty();
                }
            }
        }
        return super.isDirty();
    }

    @Override
    public int promptToSaveOnClose() {
        for (IDisplayPane pane : getDisplayPanes()) {
            for (ResourcePair resourcePair : pane.getDescriptor()
                    .getResourceList()) {
                if (resourcePair.getResourceData() instanceof ISaveableResourceData) {
                    return ((ISaveableResourceData) resourcePair
                            .getResourceData())
                            .promptToSaveOnCloseResourceData();
                }
            }
        }
        return super.promptToSaveOnClose();
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        for (IDisplayPane pane : getDisplayPanes()) {
            for (ResourcePair resourcePair : pane.getDescriptor()
                    .getResourceList()) {
                if (resourcePair.getResourceData() instanceof ISaveableResourceData) {
                    ((ISaveableResourceData) resourcePair.getResourceData())
                            .doSaveResourceData(monitor);
                }
            }
        }
    }

}
