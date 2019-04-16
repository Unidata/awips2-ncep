/*
 * gov.noaa.nws.ncep.ui.pgen.tools.PgenVolcanoCreateTool
 *
 * October 2010
 *
 * This code has been developed by the SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.viz.tools.frame;

import java.util.TreeSet;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.IDescriptor.FramesInfo;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.viz.ui.editor.AbstractEditor;
import com.raytheon.viz.ui.tools.AbstractTool;

import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;
import gov.noaa.nws.ncep.viz.ui.display.NcEditorUtil;

/**
 * The class for skipping bad frames action
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date           Ticket#     Engineer     Description
 * ------------   ----------  -----------  --------------------------
 * 10/10          #309        G. Zhang     Initial Creation.
 * 02/11/13       #972        G. Hull      AbstractEditor instead of NCMapEditor
 * 11/08/16       5976        bsteffen     Update deprecated method calls.
 * Feb 13, 2019   7577        tgurney      Prevent tagging a bad frame if no
 *                                         data resource is loaded
 *
 * </pre>
 *
 * @author G. Zhang
 */
public class BadFrameAction extends AbstractTool {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(BadFrameAction.class);

    private static final int NOT_VALID_INDEX = -1;

    @Override
    public Object execute(ExecutionEvent arg0) throws ExecutionException {

        super.execute(arg0);

        AbstractEditor editor = NcDisplayMngr.getActiveNatlCntrsEditor();
        if (editor == null) {
            return null;
        }

        if (editor.getActiveDisplayPane() == null) {
            return null;
        }

        IRenderableDisplay display = editor.getActiveDisplayPane()
                .getRenderableDisplay();
        if (display == null) {
            return null;
        }

        IDescriptor descriptor = display.getDescriptor();
        if (descriptor == null) {
            return null;
        }

        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getShell();
        ResourceList rscList = descriptor.getResourceList();
        if ((rscList == null) || (rscList.size() == 0)) {
            return null;
        }
        FramesInfo fi = descriptor.getFramesInfo();

        boolean hasData = rscList.stream()
                .anyMatch(rscPair -> (rscPair != null && rscPair
                        .getResourceData() instanceof AbstractNatlCntrsRequestableResourceData));

        if (!hasData || fi.getFrameCount() == 0) {
            MessageDialog.openError(shell, "Warning",
                    "There is no frame to be tagged as a bad frame!");
            return null;

        }

        if (isLastFrame(fi)) {
            MessageDialog.openError(shell, "Warning",
                    "The last frame cannot be tagged as a bad frame!");
            return null;
        }

        boolean ok = MessageDialog.openConfirm(shell, "Bad Frame Confirmation",
                "Do you really want to tag this as a bad frame?");

        // if user choose OK, we proceed to skip the frame
        if (ok) {
            try {

                int index = fi.getFrameIndex();
                fi.getFrameTimes()[index].setVisible(false);

                int nextIndex = getNextIndex(descriptor, index);

                fi = new FramesInfo(fi.getFrameTimes(), nextIndex);
            } catch (Exception e) {
                statusHandler.info("The editor or frame is invalid!", e);
                return null;
            }

            editor.refresh();
            NcEditorUtil.refreshGUIElements(editor);
        }

        return null;
    }

    /*
     * get the next visible frame index
     */
    private int getNextIndex(IDescriptor idtor, int skipIdx) {
        DataTime[] dt = idtor.getFramesInfo().getFrameTimes();
        TreeSet<Integer> set = new TreeSet<>();
        for (int i = 0; i < dt.length; i++) {
            if (dt[i].isVisible()) {
                set.add(i);
                if (i > skipIdx) {
                    return i;
                }
            }
        }
        return set.isEmpty() ? NOT_VALID_INDEX : set.first();
    }

    /*
     * check if this is the last frame
     */
    private boolean isLastFrame(FramesInfo info) {
        if (info.getFrameCount() == 1) {
            return true;
        }

        int counter = 0;
        for (DataTime dt : info.getFrameTimes()) {
            if (dt.isVisible()) {
                counter++;
                if (counter > 1) {
                    return false;
                }
            }
        }
        // if counter is 0 or 1
        return true;
    }

}
