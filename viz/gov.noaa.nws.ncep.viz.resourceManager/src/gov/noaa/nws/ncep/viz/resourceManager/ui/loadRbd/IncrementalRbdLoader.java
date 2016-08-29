/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package gov.noaa.nws.ncep.viz.resourceManager.ui.loadRbd;

import gov.noaa.nws.ncep.viz.resources.manager.AbstractRBD;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceBndlLoader;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;
import gov.noaa.nws.ncep.viz.ui.display.NcEditorUtil;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.drawables.AbstractRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.PaintStatus;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource.ResourceStatus;
import com.raytheon.viz.ui.editor.AbstractEditor;

/**
 * 
 * Provides the ability to load multiple {@link AbstractRBD}s incrementally.
 * Each RBD is loaded and given time to populate the display before the next RBD
 * is started. This must be run on the UI thread but it will continue to allow
 * SWT events to process. A modal progress dialog is displayed during loading to
 * prevent the user from causing any conflicting actions while it is loading.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date        Ticket#  Engineer  Description
 * ----------- -------- --------- -----------------
 * 08/26/2016  R21170   bsteffen  Initial creation
 * 
 * </pre>
 * 
 * @author bsteffen
 */
public class IncrementalRbdLoader implements Runnable {

    private static final int LOAD_TIMEOUT_MS = Integer.getInteger(
            "rbd.load.timeout.seconds", 3) * 1000;

    private final ResourceBndlLoader rbdLoader;

    private final List<AbstractRBD<?>> rbdsToLoad;

    private final boolean duplicatesRBDsAllowed;

    public IncrementalRbdLoader(ResourceBndlLoader rbdLoader,
            List<AbstractRBD<?>> rbdsToLoad, boolean duplicatesRBDsAllowed) {
        this.rbdLoader = rbdLoader;
        this.rbdsToLoad = rbdsToLoad;
        this.duplicatesRBDsAllowed = duplicatesRBDsAllowed;
    }

    protected void runWithProgress(Shell shell, IProgressMonitor monitor)
            throws InterruptedException {
        /*
         * Assign 2 units of work for each rbd, one for the actual rbd load and
         * one while it waits for paint.
         */
        monitor.beginTask("Loading RBDs", rbdsToLoad.size() * 2);

        /*
         * Since the rbdLoader is not a UI thread it can not create an editor so
         * we need to create the editors here and pass them to the rbdLoader
         */
        rbdLoader.removeAllSeldRBDs();

        Iterator<AbstractRBD<?>> iterator = rbdsToLoad.iterator();
        while (iterator.hasNext()) {
            AbstractRBD<?> rbdBndl = iterator.next();
            String rbdName = rbdBndl.getRbdName();
            monitor.subTask("Loading " + rbdName);
            /*
             * Since rbdLoader uses the same resources in the AbstractRBD<?> to
             * load in the editor, we will need to make a copy here so that
             * future edits are not immediately reflected in the loaded display.
             * The easiest way to do this is to marshal and then unmarshal the
             * rbd.
             */
            try {
                rbdBndl = AbstractRBD.clone(rbdBndl);
                /* timeMatcher currently isn't marshalling completely. */

                AbstractRenderableDisplay panes[] = rbdBndl.getDisplays();

                if (panes == null || panes.length == 0) {
                    throw new VizException(
                            "No Panes are defined for this RBD???.");
                }

                int paneCount = panes.length;

                AbstractEditor newEditor = NcDisplayMngr
                        .findDisplayByNameAndType(rbdBndl.getDisplayType(),
                                rbdName);

                /*
                 * if there is already a display with this RBD name then check
                 * whether duplicates have been allowed
                 */
                boolean duplicateRBD = false;
                if (newEditor != null
                        && NcEditorUtil.getPaneLayout(newEditor)
                                .getNumberOfPanes() == paneCount) {
                    duplicateRBD = true;
                    if (duplicatesRBDsAllowed) {
                        newEditor = null;
                    }
                }

                /*
                 * TODO : punt on putting multi-pane RBDs into empty displays
                 * not sure if this is hard or easy but don't have time to test
                 * it.
                 */
                if (newEditor == null && paneCount == 1) {
                    newEditor = NcDisplayMngr
                            .findUnmodifiedDisplayToLoad(rbdBndl
                                    .getDisplayType());
                }

                if (newEditor == null) {
                    newEditor = NcDisplayMngr.createNatlCntrsEditor(
                            rbdBndl.getDisplayType(), rbdName,
                            rbdBndl.getPaneLayout());
                }

                if (newEditor == null) {
                    throw new VizException(
                            "Unable to find or create an Editor for RBD "
                                    + rbdName);
                }

                if (panes.length > 1) {
                    NcEditorUtil.setGeoSyncPanesEnabled(newEditor,
                            rbdBndl.isGeoSyncedPanes());
                }

                if (!duplicateRBD || (duplicateRBD && duplicatesRBDsAllowed)) {
                    rbdLoader.addRBD(rbdBndl, newEditor);
                    rbdLoader.run();
                    monitor.worked(1);

                    if (iterator.hasNext()) {
                        waitForEditor(shell.getDisplay(), monitor, newEditor);
                    }
                    monitor.worked(1);
                } else {
                    monitor.worked(2);
                }
            } catch (VizException e) {
                MessageBox mb = new MessageBox(shell, SWT.OK);
                mb.setText("Error Loading RBD " + rbdName);
                mb.setMessage("Error Loading RBD " + rbdName + ".\n\n"
                        + e.getMessage());
                mb.open();
                monitor.worked(2);
            }
        }
    }

    private void waitForEditor(Display display, IProgressMonitor monitor,
            AbstractEditor editor) throws InterruptedException {
        long timeoutTime = System.currentTimeMillis() + LOAD_TIMEOUT_MS;

        while (timeoutTime > System.currentTimeMillis() && !isReady(editor)
                && !monitor.isCanceled()) {
            /*
             * Cannot use display.sleep() because that only wakes up when there
             * are new SWT events and we need to wakeup periodically to check if
             * the editor is ready or if the timeout has expired.
             */
            Thread.sleep(5);
            while (display.readAndDispatch()) {
                /* Nothing to do. */
            }
        }
    }

    /**
     * Open a {@link ProgressMonitorDialog} and load the RBDs supplied to the
     * constructor. Must be called only from the UI Thread.
     */
    @Override
    public void run() {
        final Shell shell = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getShell();
        ProgressMonitorDialog dialog = new ProgressMonitorDialog(shell);
        IRunnableWithProgress internalRunnable = new IRunnableWithProgress() {

            @Override
            public void run(IProgressMonitor monitor)
                    throws InterruptedException {
                runWithProgress(shell, monitor);
            }
        };

        try {
            dialog.run(false, true, internalRunnable);
        } catch (InvocationTargetException | InterruptedException e) {
            MessageBox mb = new MessageBox(shell, SWT.OK);
            mb.setText("Error Loading RBDs");
            mb.setMessage("Error Loading RBD.\n\n" + e.getMessage());
            mb.open();
        }
    }

    /**
     * Determine if all the resources loaded in a container have had time to
     * Initialize and fully paint.
     * 
     * @param container
     *            the container to check.
     * @return true if all resource are ready, false otherwise.
     */
    private static boolean isReady(IDisplayPaneContainer container) {
        for (IDisplayPane pane : container.getDisplayPanes()) {
            if (pane.getTarget().isNeedsRefresh()) {
                return false;
            }
            for (ResourcePair pair : pane.getDescriptor().getResourceList()) {
                AbstractVizResource<?, ?> resource = pair.getResource();
                ResourceStatus status = resource.getStatus();
                PaintStatus paintStatus = resource.getPaintStatus();
                if (status == ResourceStatus.NEW
                        || status == ResourceStatus.LOADING
                        || paintStatus == PaintStatus.INCOMPLETE
                        || paintStatus == PaintStatus.REPAINT) {
                    return false;
                }
            }
        }
        return true;
    }

}
