package gov.noaa.nws.ncep.viz.ui.seek;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PlatformUI;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.datum.DefaultEllipsoid;

import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.maps.display.VizMapEditor;
import com.raytheon.uf.viz.core.rsc.IInputHandler;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.viz.ui.EditorUtil;
import com.raytheon.viz.ui.editor.AbstractEditor;
import com.raytheon.viz.ui.editor.EditorInput;
import com.raytheon.viz.ui.input.InputAdapter;
import com.vividsolutions.jts.geom.Coordinate;

import gov.noaa.nws.ncep.viz.common.display.NcDisplayType;
import gov.noaa.nws.ncep.viz.ui.display.AbstractNcModalTool;
import gov.noaa.nws.ncep.viz.ui.display.AbstractNcPaneManager;
import gov.noaa.nws.ncep.viz.ui.display.NcEditorUtil;

/**
 * Popup SEEK results dialog in National Centers perspective.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#     Engineer  Description
 * ------------- ----------- --------- -----------------------------------------
 * March 2009    86          M. Li     Initial creation.
 * Sept  2009    169         G. Hull   AbstractNCModalMapTool
 * Dec   2010    351      A  chana     Removed getSeekLayer() Added logic to
 *                                     initializeTheSeekLayer() such that the
 *                                     seekDrawingLayer is created afresh for
 *                                     each descriptor Moved the data associated
 *                                     with the seek resource (seekDrawingLayer)
 *                                     to the seekResourceData object Updated
 *                                     the execute() method to toggle the
 *                                     display of the seek layer.
 * Jan   2012    TTR 326     J. Zeng   handled NUllPointerException
 * May   2012    747         B. Yin    Made the pan tool work when the shift key
 *                                     is held down.
 * Feb   2012    972         G. Hull   don't implement for NTRANS displays
 * Apr 12, 2019  7803        K. Sunil  changes to make SeekTool work on D2D
 *                                     perspective
 * May 08, 2019  63530       tjensen   Added takeControl()
 * Sep 30, 2019  69187       ksunil    removed dependence on the active display.
 *                                     Make SeekTool work for Multi-Panel
 *
 * </pre>
 *
 * @author mli
 *
 */

public class SeekResultsAction extends AbstractNcModalTool {

    protected IInputHandler mouseHandler;

    protected SeekResourceData seekResourceData;

    protected SeekDrawingLayer seekDrawingLayer;

    protected SeekResultsDialog seekRsltsDlg;

    @Override
    protected void activateTool() {

        /*
         * Register mouse handler.
         */
        mapEditor = (AbstractEditor) EditorUtil.getActiveEditor();

        if (!isSeekToolAllowed(mapEditor)) {
            // NOTE : Disable for NTRANS and SWPC
            deactivateTool();
            return;
        }

        if (mouseHandler == null) {
            mouseHandler = createSeekMouseHandler();
        }
        if (mapEditor != null) {
            mapEditor.registerMouseHandler(this.mouseHandler);
        }

        initializeTheSeekLayer();

        /*
         * Pop up Seek result window
         */
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getShell();
        if (seekRsltsDlg == null) {
            seekRsltsDlg = SeekResultsDialog.getInstance(shell, this);
        }
        if (!seekRsltsDlg.isDlgOpen()) {
            seekRsltsDlg.open();
        }
    }

    @Override
    public void deactivateTool() {

        if (mapEditor != null && mouseHandler != null) {
            mapEditor.unregisterMouseHandler(this.mouseHandler);
            mouseHandler = null;
        }
        removeSeekLayer();
    }

    public class SeekMouseHandler extends InputAdapter {

        private Coordinate firstMouseClick;

        private boolean shiftDown;

        private boolean simulate;

        @Override
        public boolean handleMouseDown(int x, int y, int button) {
            Coordinate[] endpts = new Coordinate[2];
            if (button != 1 || simulate) {
                return false;
            }
            if (mapEditor != null) {
                Coordinate ll = mapEditor.translateClick(x, y);
                firstMouseClick = ll;
                if (ll == null) {
                    return false;
                }

                if (seekRsltsDlg != null && seekRsltsDlg.isDlgOpen()) {
                    seekRsltsDlg.setPosition(ll);
                    endpts = seekRsltsDlg.getEndPoints();
                    if (endpts[0] != null || endpts[1] != null) {
                        seekDrawingLayer.getResourceData()
                                .setFirstPt(endpts[0]);
                        seekDrawingLayer.getResourceData().setLastPt(endpts[1]);
                    }
                }
                mapEditor.refresh();
            }
            return false;
        }

        @Override
        public boolean handleMouseDownMove(int x, int y, int button) {
            if (button != 1 || shiftDown) {
                return false;
            }
            if (mapEditor != null) {
                Coordinate c1 = firstMouseClick;
                Coordinate c2 = mapEditor.translateClick(x, y);
                if (seekRsltsDlg != null && seekRsltsDlg.isDlgOpen()
                        && c1 != null && c2 != null) {
                    seekDrawingLayer.getResourceData().setPoint1(c1);
                    seekDrawingLayer.getResourceData().setPoint2(c2);
                    // Calculate distance and direction
                    GeodeticCalculator gc = new GeodeticCalculator(
                            DefaultEllipsoid.WGS84);
                    gc.setStartingGeographicPoint(c2.x, c2.y);
                    gc.setDestinationGeographicPoint(c1.x, c1.y);

                    double azimuth = gc.getAzimuth();
                    if (azimuth < 0) {
                        azimuth += 360.0;
                    }
                    double distanceInMeter = gc.getOrthodromicDistance();

                    double firstScnPt[] = mapEditor
                            .translateInverseClick(firstMouseClick);

                    Coordinate c = mapEditor.translateClick(firstScnPt[0] - 15,
                            firstScnPt[1] - 15);
                    String str = seekRsltsDlg.getFormatDistance(distanceInMeter,
                            azimuth);

                    seekDrawingLayer.getResourceData().clearStrings();
                    if (str != null) {
                        seekDrawingLayer.getResourceData().drawString(c, str);
                    }
                    mapEditor.refresh();

                    // simulate a mouse down event so that the pan tool gets the
                    // location of the last click.
                    Event me = new Event();
                    me.display = mapEditor.getActiveDisplayPane().getDisplay();
                    me.button = 1;
                    me.type = SWT.MouseDown;
                    me.x = x;
                    me.y = y;

                    simulate = true;
                    mapEditor.getMouseManager().handleEvent(me);
                    simulate = false;

                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean handleMouseUp(int x, int y, int button) {
            if (button != 1) {
                return false;
            }
            if (mapEditor != null) {
                Coordinate ll = mapEditor.translateClick(x, y);
                if (ll == null) {
                    return false;
                }

                seekDrawingLayer.getResourceData().clearStrings();
                seekDrawingLayer.getResourceData().clearLine();
                mapEditor.refresh();
                return true;
            }
            return false;

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

    protected void initializeTheSeekLayer() {
        if (mapEditor != null) {
            try {
                if (seekResourceData == null) {
                    seekResourceData = new SeekResourceData();
                }

                if (seekDrawingLayer == null) {
                    seekDrawingLayer = seekResourceData
                            .construct(new LoadProperties());
                    seekDrawingLayer
                            .init(editor.getActiveDisplayPane().getTarget());

                    for (IDisplayPane pane : mapEditor.getDisplayPanes()) {
                        IRenderableDisplay display = pane
                                .getRenderableDisplay();
                        if (display != null) {
                            ResourceList rscs = display.getDescriptor()
                                    .getResourceList();
                            rscs.add(seekDrawingLayer);
                        }
                    }

                }
            } catch (VizException | NullPointerException e) {
                e.printStackTrace();
            }
            mapEditor.refresh();
        }
    }

    protected void removeSeekLayer() {

        if (seekDrawingLayer != null) {
            /*
             * save off the resource data for the next time the handler is
             * activated
             */
            if (mapEditor != null) {
                NcEditorUtil.getDescriptor(mapEditor).getResourceList()
                        .removeRsc(seekDrawingLayer);
                seekDrawingLayer = null;
                mapEditor.refresh();
            }
        }
    }

    protected SeekMouseHandler createSeekMouseHandler() {
        return (new SeekMouseHandler());
    }

    public String getCommandId() {
        return commandId;
    }

    /*
     * Now the SeekTool can be used from the D2D perspective also. The old logic
     * hard coded and checked for items like the instance and display type. It
     * is still maintained. But also allows simple D2D pane managers.
     */
    public static boolean isSeekToolAllowed(AbstractEditor theEditor) {

        if (theEditor == null) {
            return false;
        }
        IEditorInput edin = theEditor.getEditorInput();
        if (edin instanceof EditorInput && ((EditorInput) edin)
                .getPaneManager() instanceof AbstractNcPaneManager) {
            AbstractNcPaneManager pm = (AbstractNcPaneManager) ((EditorInput) edin)
                    .getPaneManager();
            if (pm.getDisplayType().equals(NcDisplayType.NMAP_DISPLAY)) {
                return true;
            }
        } else if (theEditor instanceof VizMapEditor) {
            return true;
        }
        return false;
    }

    protected void takeControl() {
        seekDrawingLayer.setEditable(true);
    }
}
