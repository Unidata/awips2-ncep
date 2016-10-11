package gov.noaa.nws.ncep.viz.tools.predefinedArea;

import gov.noaa.nws.ncep.viz.common.area.AreaName;
import gov.noaa.nws.ncep.viz.common.area.AreaName.AreaSource;
import gov.noaa.nws.ncep.viz.common.area.IGridGeometryProvider;
import gov.noaa.nws.ncep.viz.common.area.NcAreaProviderMngr;
import gov.noaa.nws.ncep.viz.common.area.PredefinedArea;
import gov.noaa.nws.ncep.viz.common.area.PredefinedAreaFactory;
import gov.noaa.nws.ncep.viz.common.display.INatlCntrsDescriptor;
import gov.noaa.nws.ncep.viz.common.display.INatlCntrsRenderableDisplay;
import gov.noaa.nws.ncep.viz.common.display.NcDisplayType;
import gov.noaa.nws.ncep.viz.ui.display.AbstractNcPaneManager;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;
import gov.noaa.nws.ncep.viz.ui.display.NcEditorUtil;

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;

import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.globals.VizGlobalsManager;
import com.raytheon.viz.ui.EditorUtil;
import com.raytheon.viz.ui.editor.AbstractEditor;
import com.raytheon.viz.ui.editor.IMultiPaneEditor;

/**
 * Load the predefined area map to the display
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * Jul 09, 2009             gilbert      Started with RTS class com.raytheon.uf.viz.d2d.ui.map.actions.ScaleHandler 
 *                                       and added our modifications from TO10
 *                                       version of PredefinedAreaAction
 * Sep 25, 2009             B. Hebbard   Zap the isMapLayer() exemption for copying resources in setScale(),
 *                                       as temporary(?) workaround to allow things to work with our new
 *                                       area bundles which have map overlays removed.  Still need to verify
 *                                       permanent solution which correctly preserves all display resources.
 * Oct 09, 2009             B. Hebbard   Switch design to that proposed by Greg:  No longer copy resources
 *                                       to new map descriptor; rather, just get the new parameters from the
 *                                       bundle, and apply to the existing descriptor and display.  (Position
 *                                       error resolved via recenter() and changing order of method calls.)
 * Oct 10, 2009             G. Hull      Multi-Pane
 * Oct 14, 2009             B. Hebbard   Added proper zooming to the newly selected area
 * Oct 27, 2009             G. Hull      Moved out of perspectives project
 * Feb 26. 2010             G. Hull      retrieve PredefinedArea instead of a Bundle file
 * Sep 10. 2012             B. Yin       Remove the call to setRenderableDisplay which creates 
 *                                       a new GLTarget
 * Nov 18, 2012             G. Hull      add areaType parameter and code to get the area based on other types (ie RESOURCES and DISPLAYS)
 * Dec 12  2012    #630     G. Hull      replace ZoomUtil.allowZoom with refreshGUIelements
 * Feb 12  2012    #972     G. Hull      change to INatlCntrsRenderableDisplay
 * May 15  2013    #862     G. Hull      replace code to get areas from displays or resources with call to 
 *                                       new NcAreaProviderMngr.createGeomProvider() with areaSource & name
 * 07/15/2015      R8899    J. Lopez     Disables the zoom if it is specified in the XML file
 * 09/23/2015      R9397    N. Jensen    Added updateElement(UIElement, Map)
 * 02/08/2015      R15438   J. Huber     Use original value of mapcenter instead of Raytheon calculated center to
 *                                       define where map is centered.
 * 04/11/2016      R15714   bkowal       Only attempt to center the display if a center has been defined
 *                                       for the pre-defined area.
 * </pre>
 * 
 */
public class PredefinedAreaAction extends AbstractHandler implements
        IElementUpdater {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        AreaName areaName = null;

        try {
            try {
                areaName = new AreaName(AreaSource.getAreaSource(event
                        .getParameter("areaSource")),
                        event.getParameter("areaName"));
            } catch (Exception excep) {
                throw new VizException(
                        "areaName or areaSource parameter not recognized");
            }

            AbstractEditor editor = NcDisplayMngr.getActiveNatlCntrsEditor();
            NcDisplayType displayType = NcEditorUtil.getNcDisplayType(editor);
            if (displayType != NcDisplayType.NMAP_DISPLAY) {
                return null;
            }

            IGridGeometryProvider geomProv = NcAreaProviderMngr
                    .createGeomProvider(areaName);
            if (geomProv == null) {
                throw new VizException("Unable to create area for "
                        + areaName.toString());
            }

            PredefinedArea pArea = PredefinedAreaFactory
                    .createPredefinedArea(geomProv);

            // get the panes to set the area in.
            IDisplayPane[] displayPanes = NcEditorUtil
                    .arePanesGeoSynced(editor) ? editor.getDisplayPanes()
                    : NcEditorUtil.getSelectedPanes(editor);

            for (IDisplayPane pane : displayPanes) {
                setPredefinedArea(pane, pArea);
            }

            NcEditorUtil.refreshGUIElements(editor);
            VizGlobalsManager.getCurrentInstance().updateUI(editor);
            editor.refresh();
        } catch (VizException vizEx) {
            MessageDialog errDlg = new MessageDialog(
                    NcDisplayMngr.getCaveShell(), "Error", null,
                    "Error Changing Area:\n\n" + vizEx.getMessage(),
                    MessageDialog.ERROR, new String[] { "OK" }, 0);
            errDlg.open();
        }

        return null;
    }

    /**
     * Sets the predefined area on the display pane, replacing the previous
     * PredefinedArea.
     * 
     * @param pane
     * @param pArea
     * @throws VizException
     */
    public static void setPredefinedArea(IDisplayPane pane, PredefinedArea pArea)
            throws VizException {
        INatlCntrsRenderableDisplay existingDisplay = (INatlCntrsRenderableDisplay) pane
                .getRenderableDisplay();
        boolean zoomDisable = pArea.getZoomDisable();

        /*
         * Note: setGridGeometry does an implicit reproject of all resources on
         * the descriptor, so don't need to do this explicitly
         */
        existingDisplay.setInitialArea(pArea);
        /*
         * Use the existing map center if the predefined area does not have a
         * defined center. Extracted to maintain the integrity of the
         * pre-defined area.
         */
        final double[] mapCenter = (pArea.getMapCenter() == null) ? existingDisplay
                .getMapCenter() : pArea.getMapCenter();
        pane.setZoomLevel(existingDisplay.getZoomLevel());
        pane.scaleToClientArea();
        existingDisplay.recenter(mapCenter);
        existingDisplay.getView().zoom(existingDisplay.getZoomLevel());
        ((INatlCntrsDescriptor) existingDisplay.getDescriptor())
                .setSuspendZoom(zoomDisable);
    }

    /**
     * This code was derived/inspired by the code in the class LoadMap in plugin
     * com.raytheon.uf.viz.core.maps. It handles the UI of marking which radio
     * is selected.
     */
    @Override
    @SuppressWarnings("rawtypes")
    public void updateElement(UIElement element, Map parameters) {
        IDisplayPaneContainer container = EditorUtil.getActiveVizContainer();
        boolean set = false;

        /*
         * The null checks below are safety checks. It could be possible to get
         * a null container or pane if there were no tabs or no tabs with focus.
         * In these cases the menu item will set checked to false.
         */
        if (container != null) {
            IDisplayPane pane = null;
            if (container instanceof IMultiPaneEditor) {
                pane = ((IMultiPaneEditor) container)
                        .getSelectedPane(AbstractNcPaneManager.NC_PANE_SELECT_ACTION);
            } else {
                pane = container.getActiveDisplayPane();
            }

            /*
             * The following null handling is just a safety check.
             */
            if (pane != null) {
                IRenderableDisplay display = pane.getDescriptor()
                        .getRenderableDisplay();
                if (display instanceof INatlCntrsRenderableDisplay) {
                    PredefinedArea area = ((INatlCntrsRenderableDisplay) display)
                            .getInitialArea();
                    String uiName = (String) parameters.get("areaName");
                    if (uiName != null && area != null
                            && uiName.equals(area.getAreaName())) {
                        element.setChecked(true);
                        set = true;
                    }
                }
            }
        }

        if (!set) {
            element.setChecked(false);
        }

    }

}