package gov.noaa.nws.ncep.viz.cloudHeight.ui;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.IInputHandler;
import com.raytheon.uf.viz.core.rsc.IInputHandler.InputPriority;
import com.raytheon.viz.ui.editor.ISelectedPanesChangedListener;
import com.raytheon.viz.ui.input.InputAdapter;
import com.raytheon.viz.ui.panes.VizDisplayPane;
import com.raytheon.viz.ui.perspectives.AbstractVizPerspectiveManager;
import com.raytheon.viz.ui.perspectives.VizPerspectiveListener;
import org.locationtech.jts.geom.Coordinate;

import gov.noaa.nws.ncep.viz.cloudHeight.CloudHeightProcesser;
import gov.noaa.nws.ncep.viz.rsc.satellite.rsc.NcSatelliteResource;
import gov.noaa.nws.ncep.viz.ui.display.AbstractNcModalTool;
import gov.noaa.nws.ncep.viz.ui.display.NCPaneManager;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;
import gov.noaa.nws.ncep.viz.ui.display.NcEditorUtil;

/**
 * Cloud Height Dialog
 *
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------    -----------    --------------------------
 * 04/30/09                 Greg Hull   Created
 * 09/27/09      #169       Greg Hull   AbstractNcModalTool
 * 03/07/11     migration   Greg Hull   use Raytheons ISelectedPanesChangedListener
 * 03/01/12     524/TTR11   B. Hebbard  Various changes to allow mutual
 *                                      operation of 'Take Control' button with
 *                                      other Modal Tools
 * 06/01/12     747         B. Yin      Made the pan tool work when the shift is
 *                                      held down.
 * 06/21/12     826         Archana     Updated the activateTool() method to
 *                                      remove the cloudheight tool from the
 *                                      tool manager when there is no IR image
 *                                      loaded. Instead, the default Pan tool is
 *                                      loaded.
 * 02/12/13     972         G. Hull     AbstractEditor, IDisplayPane
 * 11/07/2018   7552        dgilling    Allow tool to work with arbitrary
 *                                      NcSatResources.
 * Feb 1, 2019  7570        tgurney     Add close callback to the dialog to
 *                                      disable the tool when dialog is closed
 * </pre>
 *
 */
public class CloudHeightAction extends AbstractNcModalTool {

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(getClass());

    private IInputHandler mouseHndlr = null;

    private static CloudHeightDialog cldHghtDlg = null;

    private CloudHeightProcesser cldHghtProcessor = null;

    private NcSatelliteResource satResource = null;

    private ISelectedPanesChangedListener paneChangeListener = new ISelectedPanesChangedListener() {

        @Override
        public void selectedPanesChanged(String id, IDisplayPane[] seldPanes) {
            if (!id.equals(NCPaneManager.NC_PANE_SELECT_ACTION)) {
                return;
            }

            // NOTE: can only use cloud height on one pane at a time.
            if (cldHghtProcessor != null && seldPanes != null
                    && seldPanes.length > 0) {
                if (seldPanes[0] instanceof VizDisplayPane) {
                    cldHghtProcessor.setPane(seldPanes[0]);
                }
            }
        }
    };

    @Override
    protected void activateTool() {

        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getShell();
        synchronized (CloudHeightAction.class) {
            if (cldHghtDlg == null) {
                cldHghtDlg = CloudHeightDialog.getInstance(shell, this);
                cldHghtDlg.addCloseCallback((v) -> {
                    deactivate();
                });
            }
        }

        mapEditor = NcDisplayMngr.getActiveNatlCntrsEditor();

        NcEditorUtil.addSelectedPaneChangedListener(mapEditor,
                paneChangeListener);

        // Register mouse handler.
        if (mouseHndlr == null) {
            mouseHndlr = new MouseHandler();
        }
        if (mapEditor != null) {
            mapEditor.registerMouseHandler(this.mouseHndlr,
                    InputPriority.LOWEST);
        }

        IDisplayPane[] seldPanes = NcEditorUtil.getSelectedPanes(mapEditor);

        if (seldPanes.length != 1) {
            statusHandler
                    .error("Cloud Height will only work on one selected pane.");
            return;
        }

        try {
            cldHghtProcessor = new CloudHeightProcesser(seldPanes[0],
                    cldHghtDlg);
        } catch (VizException e) {
            statusHandler.error("Error Starting Cloud Height (Processor)", e);
            return;
        }

        satResource = cldHghtProcessor.getSatResource();
        if (satResource == null) {
            issueAlert();
            AbstractVizPerspectiveManager mgr = VizPerspectiveListener
                    .getCurrentPerspectiveManager();
            if (mgr != null) {
                mgr.getToolManager().deselectModalTool(this);
                NcDisplayMngr.setPanningMode();

            }
            return;
        }

        // Pop up Cloud Height result window (if not already open)
        try {
            if (!cldHghtDlg.isOpen()) {
                cldHghtDlg.open();
            }
        } catch (Exception ex) {
            statusHandler.error("Error Starting Cloud Height Dialog", ex);

        }

        /*
         * Note that CloudHeightDialog.open() will, after the dialog is closed,
         * call ModalToolManager.deselectModalTool(...) to shut things down,
         * which in turn eventually calls back to deactivateTool() here.
         */
    }

    private void issueAlert() {

        String msg = "Please load a supported Satellite data type\n to invoke the Cloud Height tool.";
        MessageDialog messageDlg = new MessageDialog(
                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                "Warning", null, msg, MessageDialog.WARNING,
                new String[] { "OK" }, 0);
        messageDlg.open();
        deactivate();
    }

    // TODO : make cloud height work as a modal tool
    @Override
    public void setEnabled(boolean enable) {
        super.setEnabled(enable);
    }

    @Override
    public void deactivateTool() {
        if (mapEditor != null) {

            NcEditorUtil.removeSelectedPaneChangedListener(mapEditor,
                    paneChangeListener);

            if (mouseHndlr != null) {
                mapEditor.unregisterMouseHandler(mouseHndlr);
            }
        }

        if (cldHghtProcessor != null) {
            cldHghtProcessor.close();
        }
    }

    public class MouseHandler extends InputAdapter {

        private boolean preempt = false;

        private boolean shiftDown;

        @Override
        public boolean handleMouseDown(int x, int y, int button) {

            preempt = false;

            if (shiftDown) {
                return false;
            }

            if (button == 1) {
                Coordinate ll = mapEditor.translateClick(x, y);
                if (ll == null || satResource == null) {
                    return false;
                }

                Double value = satResource.getSatIRTemperature(ll);
                if (value != null && !value.isNaN()) {
                    cldHghtProcessor.processCloudHeight(ll, true);
                    preempt = false;
                }

            }

            return preempt;
        }

        @Override
        public boolean handleMouseDownMove(int x, int y, int button) {
            return preempt;
        }

        @Override
        public boolean handleKeyDown(int keyCode) {
            if (keyCode == SWT.SHIFT) {
                shiftDown = true;
            }

            return true;
        }

        @Override
        public boolean handleKeyUp(int keyCode) {
            if (keyCode == SWT.SHIFT) {
                shiftDown = false;
            }

            return true;
        }

    }

    public String getCommandId() {
        return commandId;
    }
}