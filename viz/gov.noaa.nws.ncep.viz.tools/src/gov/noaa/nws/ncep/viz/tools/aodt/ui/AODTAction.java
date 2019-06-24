package gov.noaa.nws.ncep.viz.tools.aodt.ui;

import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.rsc.IInputHandler;
import com.raytheon.viz.ui.input.InputAdapter;
import com.raytheon.viz.ui.perspectives.AbstractVizPerspectiveManager;
import com.raytheon.viz.ui.perspectives.VizPerspectiveListener;
import org.locationtech.jts.geom.Coordinate;

import gov.noaa.nws.ncep.viz.gempak.nativelib.LibraryLoader;
import gov.noaa.nws.ncep.viz.rsc.satellite.rsc.NcSatelliteResource;
import gov.noaa.nws.ncep.viz.tools.aodt.AODTProcesser;
import gov.noaa.nws.ncep.viz.ui.display.AbstractNcModalTool;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;

/**
 * Cloud Height Dialog
 *
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 09/30/09                 M. Li       Created
 * 10/05/09      169        Greg Hull   integrate with NCMapEditor,
 *                                      AbstractNCModalMapTool and InputHandlerDefaultImpl
 * 02/11/13      972        G. Hull     AbstractEditor instead of NCMapEditor
 * 11/07/18      #7552      dgilling    Allow tool to work with arbitrary
 *                                      NcSatResources.
 * Feb 4, 2018   7570       tgurney     Add close callback to the dialog to
 *                                      disable the tool when dialog is closed
 *
 * </pre>
 *
 */
public class AODTAction extends AbstractNcModalTool {

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(getClass());

    protected IInputHandler mouseHndlr = null;

    protected static AODTDialog aodtDlg = null;

    private AODTProcesser aodtProcessor = null;

    private NcSatelliteResource satResource = null;

    @Override
    protected void activateTool() {
        LibraryLoader.load("aodtv64");

        mapEditor = NcDisplayMngr.getActiveNatlCntrsEditor();

        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getShell();

        synchronized (AODTAction.class) {
            if (aodtDlg == null) {

                String aodtVersion = null;
                try {
                    aodtVersion = event.getCommand().getName();
                } catch (NotDefinedException e) {
                    statusHandler.warn(e.getLocalizedMessage(), e);
                    return;
                }

                aodtDlg = new AODTDialog(shell, aodtVersion);
                aodtDlg.addCloseCallback((v) -> {
                    deactivate();
                });
                satResource = aodtDlg.getSatResource();
            }
        }

        aodtProcessor = new AODTProcesser(aodtDlg);

        if (!aodtDlg.isOpen()) {

            if (mouseHndlr == null) {
                mouseHndlr = new MouseHandler();
            }
            mapEditor.registerMouseHandler(this.mouseHndlr);

            if (satResource != null) {
                aodtDlg.open();
            } else {
                issueAlert();
            }

            synchronized (AODTAction.class) {
                aodtDlg = null;
            }

            // deactivateTool();
            AbstractVizPerspectiveManager mgr = VizPerspectiveListener
                    .getCurrentPerspectiveManager();
            if (mgr != null) {
                mgr.getToolManager().deselectModalTool(this);
            }
        }

        aodtProcessor = null;

        return;
    }

    private void issueAlert() {

        String msg = "Unable to invoke AODT tool.\nPlease load an IR Satellite image!";
        MessageDialog messageDlg = new MessageDialog(
                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                "Warning", null, msg, MessageDialog.WARNING,
                new String[] { "OK" }, 0);
        messageDlg.open();
        deactivate();
    }

    @Override
    public void deactivateTool() {
        if (mapEditor != null && mouseHndlr != null) {
            mapEditor.unregisterMouseHandler(mouseHndlr);
        }
        if (aodtProcessor != null) {
            aodtProcessor.close();
        }

        synchronized (AODTAction.class) {
            if (aodtDlg != null) {
                aodtDlg.close();
                aodtDlg = null;
            }
        }

    }

    public class MouseHandler extends InputAdapter {

        private boolean preempt = false;

        @Override
        public boolean handleMouseDown(int x, int y, int button) {

            preempt = false;

            if (button == 1) {
                Coordinate ll = mapEditor.translateClick(x, y);
                if (ll == null || satResource == null) {
                    return false;
                }

                Double value = satResource.getSatIRTemperature(ll);
                if (value != null && !value.isNaN()) {
                    aodtProcessor.processAODT(ll);
                    preempt = false;
                }

            }

            return preempt;
        }

        @Override
        public boolean handleMouseDownMove(int aX, int aY, int button) {
            return preempt;
        }

    }
}