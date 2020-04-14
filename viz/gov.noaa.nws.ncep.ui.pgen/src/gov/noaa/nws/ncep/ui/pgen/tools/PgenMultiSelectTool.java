/*
 * gov.noaa.nws.ncep.ui.pgen.tools.PgenMultiSelectTool
 * 
 * 22 August 2009
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.ui.pgen.tools;

import gov.noaa.nws.ncep.ui.pgen.PgenConstant;
import gov.noaa.nws.ncep.ui.pgen.PgenSession;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.AttrDlgFactory;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.FrontAttrDlg;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.SymbolAttrDlg;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.WatchBoxAttrDlg;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.vaadialog.VolcanoVaaAttrDlg;
import gov.noaa.nws.ncep.ui.pgen.contours.Contours;
import gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableElement;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableElementFactory;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableType;
import gov.noaa.nws.ncep.ui.pgen.elements.Line;
import gov.noaa.nws.ncep.ui.pgen.filter.AcceptFilter;
import gov.noaa.nws.ncep.ui.pgen.rsc.PgenResource;

import java.awt.Color;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.ui.PlatformUI;
import org.opengis.coverage.grid.GridEnvelope;

import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.PixelExtent;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.rsc.IInputHandler;
import org.locationtech.jts.geom.Coordinate;


/**
 * Implements PGEN palette MultiSelect functions.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 08/09        #149        B. Yin      Initial Creation.
 * 04/10        #165        G. Zhang    add support for VAA.
 * 10/10        #289       Archana    Added logic to handle the delete key
 * 07/12        #610        B. Yin      Make the multi-select work for GFA.
 * 12/12        #908        B. Yin      Do not change to selecting mode.
 * 04/13        #874        B. Yin      Handle elements in contours.
 * 12/13        #1065       J. Wu       use LineAttrDlg for kink lines.
 * 09/15        R8535       J. Lopez    Made multi-select work outside of the map area
 * 06/07/2016   R17380      B. Yin      Use Ctrl key to draw polygon.
 * 06/16/2016   R17380      B. Yin      Set multi-selecting flag for attr dialog.
 * 12/20/2019   71072       smanoj      Modifications to handle multi-select functionality.
 * 02/05/2020   74136       smanoj      Set AttrDialog with attributes of the
 *                                      nearest component when multi-select.
 * 02/12/2020   74776       smanoj      ResetAllElements in the display after delete.
 * 
 * </pre>
 * 
 * @author B. Yin
 */
public class PgenMultiSelectTool extends AbstractPgenDrawingTool {

    @Override
    /**
     * Get the MultiSelect mouse handler.
     */
    public IInputHandler getMouseHandler() {
        this.mouseHandler = new PgenMultiSelectHandler();
        return this.mouseHandler;
    }

    /**
     * Implements input handler for mouse events.
     * 
     * @author bingfan
     * 
     */
    public class PgenMultiSelectHandler extends InputHandlerDefaultImpl {

        // x of the first mouse click
        private int theFirstMouseX;

        // y of the first mouse click
        private int theFirstMouseY;

        // flag to indicate if Pgen category has been selected
        private boolean noCat;

        // flag to indicate if an area is selecting
        private boolean selectRect;

        // Current Pgen Category
        private String pgenCat;

        private String pgenObj;

        private List<Coordinate> polyPoints;

        private DrawableElementFactory def;

        /*
         * Ctrl key flag
         */
        private boolean ctrlKeyDown = false;

        private PgenMultiSelectHandler() {
            super();
            def = new DrawableElementFactory();

        }

        /*
         * (non-Javadoc)
         * 
         * @see com.raytheon.viz.ui.input.IInputHandler#handleMouseDown(int,
         * int, int)
         */
        @Override
        public boolean handleMouseDown(int anX, int aY, int button) {
            if (!isResourceEditable() || shiftDown) {
                return false;
            }

            theFirstMouseX = anX;
            theFirstMouseY = aY;

            if (button == 1) {

                pgenCat = PgenSession.getInstance().getPgenPalette()
                        .getCurrentCategory();
                pgenObj = PgenSession.getInstance().getPgenPalette()
                        .getCurrentObject();

                // if no pgen category, pop up a warning box.
                if (pgenCat == null) {

                    MessageDialog infoDlg = new MessageDialog(PlatformUI
                            .getWorkbench().getActiveWorkbenchWindow()
                            .getShell(), "Information", null,
                            "Please select a Pgen Class from the Palette.",
                            MessageDialog.INFORMATION, new String[] { "OK" }, 0);

                    infoDlg.open();
                    noCat = true;

                } else {
                    noCat = false;
                }

                return true;

            } else if (button == 3) {

                Coordinate loc = mapEditor.translateClick(anX, aY);
                if (loc == null)
                    return false;
                AbstractDrawableComponent adc = null;
                adc = drawingLayer.getNearestComponent(loc,
                        new AcceptFilter(), true);
                if (adc != null) {
                    pgenType = adc.getPgenType();
                    pgenCat  = adc.getPgenCategory();
                }

                return true;

            } else {

                return false;

            }

        }

        /*
         * (non-Javadoc)
         * 
         * @see com.raytheon.viz.ui.input.IInputHandler#handleMouseDownMove(int,
         * int, int)
         */

        public boolean handleMouseDownMove(int anX, int aY, int button) {
            if (!isResourceEditable() || button != 1 || noCat || shiftDown) {
                return false;
            }

            selectRect = true;

            IDisplayPane activePane = mapEditor.getActiveDisplayPane();
            IExtent extent = activePane.getRenderableDisplay().getExtent();
            org.eclipse.swt.graphics.Rectangle bounds = activePane.getBounds();
            IDescriptor mapDesc = activePane.getDescriptor();
            GridEnvelope ge = mapDesc.getGridGeometry().getGridRange();

            // Convert screen coordinates to canvas coordinates
            int canvasPointX1 = (int) ((theFirstMouseX
                    * (extent.getMaxX() - extent.getMinX()) / bounds.width) + extent
                    .getMinX());
            int canvasPointX2 = (int) ((anX
                    * (extent.getMaxX() - extent.getMinX()) / bounds.width) + extent
                    .getMinX());
            int canvasPointY1 = (int) ((theFirstMouseY
                    * (extent.getMaxY() - extent.getMinY()) / bounds.height) + extent
                    .getMinY());
            int canvasPointY2 = (int) ((aY
                    * (extent.getMaxY() - extent.getMinY()) / bounds.height) + extent
                    .getMinY());

            IExtent worldExtent = new PixelExtent(ge);
            IExtent box = new PixelExtent(canvasPointX1, canvasPointX2,
                    canvasPointY1, canvasPointY2);
            IExtent drawingBox = worldExtent.intersection(box);

            // Convert canvas coordinates to Lat/Lon coordinates
            double[] worldPoint1 = mapDesc.pixelToWorld(new double[] {
                    drawingBox.getMinX(), drawingBox.getMinY() });
            double[] worldPoint2 = mapDesc.pixelToWorld(new double[] {
                    drawingBox.getMinX(), drawingBox.getMaxY() });
            double[] worldPoint3 = mapDesc.pixelToWorld(new double[] {
                    drawingBox.getMaxX(), drawingBox.getMaxY() });
            double[] worldPoint4 = mapDesc.pixelToWorld(new double[] {
                    drawingBox.getMaxX(), drawingBox.getMinY() });
            double[] worldPoint5 = mapDesc.pixelToWorld(new double[] {
                    ((drawingBox.getMinX() + drawingBox.getMaxX()) / 2),
                    drawingBox.getMinY() });
            double[] worldPoint6 = mapDesc.pixelToWorld(new double[] {
                    ((drawingBox.getMinX() + drawingBox.getMaxX()) / 2),
                    drawingBox.getMaxY() });

            // Set the points in the ghost box
            ArrayList<Coordinate> ghostPoints = new ArrayList<Coordinate>();
            ghostPoints.add(new Coordinate(worldPoint1[0], worldPoint1[1]));
            ghostPoints.add(new Coordinate(worldPoint5[0], worldPoint5[1]));
            ghostPoints.add(new Coordinate(worldPoint4[0], worldPoint4[1]));

            ghostPoints.add(new Coordinate(worldPoint3[0], worldPoint3[1]));
            ghostPoints.add(new Coordinate(worldPoint6[0], worldPoint6[1]));
            ghostPoints.add(new Coordinate(worldPoint2[0], worldPoint2[1]));

            // draw the selected area
            Line ghost = (Line) def.create(DrawableType.LINE, null, "Lines",
                    "LINE_SOLID", ghostPoints, drawingLayer.getActiveLayer());
            ghost.setLineWidth(1.0f);
            ghost.setColors(new Color[] { new java.awt.Color(255, 255, 255),
                    new java.awt.Color(255, 255, 255) });
            ghost.setClosed(true);
            ghost.setSmoothFactor(0);

            drawingLayer.setGhostLine(ghost);
            mapEditor.refresh();
            return true;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.raytheon.viz.ui.input.IInputHandler#handleMouseUp(int, int,
         * int)
         */
        @Override
        public boolean handleMouseUp(int anX, int aY, int button) {

            if (!isResourceEditable() || noCat || shiftDown) {
                return false;
            }

            if (button == 1) {

                if (ctrlKeyDown && !selectRect) {
                    if (polyPoints == null) {
                        polyPoints = new ArrayList<Coordinate>();
                    }
                    polyPoints.add(new Coordinate(anX, aY));

                } else if (selectRect) {

                    // select elements in the rectangle
                    int[] xpoints = { theFirstMouseX, theFirstMouseX, anX, anX };
                    int[] ypoints = { theFirstMouseY, aY, aY, theFirstMouseY };

                    Polygon rectangle = new Polygon(xpoints, ypoints, 4);

                    drawingLayer.addSelected(inPoly(rectangle));

                    drawingLayer.removeGhostLine();

                    selectRect = false;
                }

                else {

                    // Check if mouse is in geographic extent
                    Coordinate loc = mapEditor.translateClick(anX, aY);
                    if (loc == null)
                        return false;
                    if (pgenCat.equalsIgnoreCase(PgenConstant.CATEGORY_MET)) {
                        if (pgenObj != null
                                && pgenObj.equalsIgnoreCase("GFA")) {
                            // Get the nearest element and set it as the selected
                            // element.
                            AbstractDrawableComponent adc = drawingLayer
                                    .getNearestComponent(loc, new AcceptFilter(),
                                            true);

                            if (adc != null
                                    && adc.getPgenType().equalsIgnoreCase("GFA")) {

                                if (pgenType == null
                                        || pgenType.equalsIgnoreCase(PgenConstant.ACTION_MULTISELECT)) {
                                    pgenType = adc.getPgenType();
                                }

                                if (!drawingLayer.getAllSelected().contains(adc)) {
                                    drawingLayer.addSelected(adc);
                                } else {
                                    drawingLayer.removeSelected(adc);
                                }

                            }
                        } else {
                            noCat = false;

                            AbstractDrawableComponent adc = null;
                            AbstractDrawableComponent contour = drawingLayer
                                    .getNearestComponent(loc, new AcceptFilter(),
                                            false);

                            if (contour instanceof Contours) {
                                adc = drawingLayer.getNearestElement(loc,
                                        (Contours) contour);

                            } else {
                                adc = drawingLayer.getNearestComponent(loc,
                                        new AcceptFilter(), true);
                            }

                            if (adc != null){
                                pgenType = adc.getPgenType();
                                pgenCat = adc.getPgenCategory();
                            }

                            if (!drawingLayer.getAllSelected()
                                    .contains(adc)) {
                                drawingLayer.addSelected(adc);
                            }

                        }
                    } else {

                        noCat = false;

                        // Get the nearest element and set it as the selected
                        // element.
                        // For contours, get the nearest DE inside of it.
                        AbstractDrawableComponent adc = null;
                        AbstractDrawableComponent contour = drawingLayer
                                .getNearestComponent(loc, new AcceptFilter(),
                                        false);

                        if (contour instanceof Contours) {
                            adc = drawingLayer.getNearestElement(loc,
                                    (Contours) contour);

                        } else {
                            adc = drawingLayer.getNearestComponent(loc,
                                    new AcceptFilter(), true);
                        }

                        if (adc != null){
                            pgenType = adc.getPgenType();
                            pgenCat = adc.getPgenCategory();
                        }
                        if (!drawingLayer.getAllSelected()
                                .contains(adc)) {
                            drawingLayer.addSelected(adc);
                        }

                    }
                }
            }

            else if (button == 3) {

                if (polyPoints != null) {
                    if (polyPoints.size() > 2) {
                        int[] xpoints = new int[polyPoints.size()];
                        int[] ypoints = new int[polyPoints.size()];
                        for (int ii = 0; ii < polyPoints.size(); ii++) {
                            xpoints[ii] = (int) polyPoints.get(ii).x;
                            ypoints[ii] = (int) polyPoints.get(ii).y;
                        }

                        Polygon poly = new Polygon(xpoints, ypoints,
                                polyPoints.size());
                        drawingLayer.addSelected(inPoly(poly));

                        drawingLayer.removeGhostLine();

                    }

                    polyPoints.clear();
                    ctrlKeyDown = false;
                }
            }

            // pop up attribute window
            if (attrDlg != null && attrDlg.getShell() == null) {
                attrDlg = null;
            }

            if (attrDlg == null && drawingLayer.getAllSelected() != null
                    && !drawingLayer.getAllSelected().isEmpty()) {
                if (pgenCat.equalsIgnoreCase(PgenConstant.CATEGORY_MET)) {
                    pgenType = pgenObj;
                }

                // Use line attribute dialog for kink lines.
                if (pgenType != null
                        && (pgenType.equals("KINK_LINE_1") || pgenType
                                .equals("KINK_LINE_2"))) {
                    attrDlg = AttrDlgFactory.createAttrDlg(pgenCat, null,
                            PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                                 .getShell());
                }

                if (!(PgenConstant.ACTION_MULTISELECT.equalsIgnoreCase(pgenType))) {
                    attrDlg = AttrDlgFactory.createAttrDlg(pgenCat, pgenType,
                           PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                                .getShell());
                }

                // NO Volcano attributes editing from multiple selecting
                if (attrDlg instanceof VolcanoVaaAttrDlg)
                    return false;

                if (attrDlg != null) {
                    attrDlg.setMultiSelectMode(true);
                    attrDlg.setBlockOnOpen(false);
                    if (attrDlg.open() != Window.OK) {
                        return false;
                    }
                    attrDlg.enableButtons();
                    attrDlg.setPgenCategory(pgenCat);
                    attrDlg.setPgenType(null);
                    attrDlg.setDrawingLayer(drawingLayer);
                    attrDlg.setMapEditor(mapEditor);

                    if (attrDlg instanceof SymbolAttrDlg) {
                        ((SymbolAttrDlg) attrDlg).enableLatLon(false);
                    } else if (attrDlg instanceof WatchBoxAttrDlg) {
                        ((WatchBoxAttrDlg) attrDlg).enableShapeBtn(false);
                        ((WatchBoxAttrDlg) attrDlg).enableDspBtn(false);
                    } else if (attrDlg instanceof FrontAttrDlg) {
                        // for fronts with two color buttons
                        for (AbstractDrawableComponent adc : drawingLayer
                                .getAllSelected()) {
                            if (adc instanceof DrawableElement) {
                                if (((DrawableElement) adc).getColors().length > 1) {
                                    ((FrontAttrDlg) attrDlg)
                                            .setColor(new Color[] {
                                                    Color.green, Color.green });
                                }
                            }
                        }
                    }

                    // Set AttrDlg dialog with the attributes of
                    // the nearest component.
                    Coordinate loc = mapEditor.translateClick(anX, aY);
                    if (loc != null) {
                        AbstractDrawableComponent component = drawingLayer
                            .getNearestComponent(loc, new AcceptFilter(),
                                    false);
                        if (component instanceof Contours) {
                            AbstractDrawableComponent adc = drawingLayer
                                    .getNearestElement(loc,
                                            (Contours) component);
                            attrDlg.setAttrForDlg(adc.getPrimaryDE());
                        } else {
                            if (component != null) {
                                attrDlg.setAttrForDlg(component.getPrimaryDE());
                            }
                        }
                    }
                }
            }
            mapEditor.setFocus();
            mapEditor.refresh();

            return true;
        }

        @Override
        public boolean handleMouseMove(int anX, int aY) {

            if (!isResourceEditable() || noCat || shiftDown) {
                return false;
            }

            // draw a ghost polygon
            if (polyPoints != null && !polyPoints.isEmpty()) {

                polyPoints.add(new Coordinate(anX, aY));

                if (polyPoints.size() > 1) {

                    ArrayList<Coordinate> points = new ArrayList<Coordinate>();

                    for (Coordinate loc : polyPoints) {
                        points.add(mapEditor.translateClick(loc.x, loc.y));
                    }

                    DrawableElementFactory def = new DrawableElementFactory();
                    Line ghost = (Line) def.create(DrawableType.LINE, null,
                            "Lines", "LINE_SOLID", points,
                            drawingLayer.getActiveLayer());

                    ghost.setLineWidth(1.0f);
                    ghost.setColors(new Color[] {
                            new java.awt.Color(255, 255, 255),
                            new java.awt.Color(255, 255, 255) });
                    ghost.setClosed(true);
                    ghost.setSmoothFactor(0);

                    drawingLayer.setGhostLine(ghost);
                }

                mapEditor.refresh();

                polyPoints.remove(polyPoints.size() - 1);
            }
            return true;
        }

        @Override
        public boolean handleKeyDown(int keyCode) {
            if (!isResourceEditable())
                return false;

            if (keyCode == SWT.SHIFT) {
                shiftDown = true;
            } else if (keyCode == SWT.DEL) {
                PgenResource pResource = PgenSession.getInstance()
                        .getPgenResource();
                pResource.deleteSelectedElements();
                pResource.resetAllElements();
            } else if (keyCode == SWT.CONTROL) {
                ctrlKeyDown = true;
            }
            return true;
        }

        /**
         * Sets the ctrl key flag to false when the key is released.
         */
        @Override
        public boolean handleKeyUp(int keyCode) {

            super.handleKeyUp(keyCode);

            if (keyCode == SWT.CONTROL) {
                ctrlKeyDown = false;
            }

            return true;
        }

        /**
         * return all elements of the current Pgen category in the input area.
         * 
         * @param poly
         * @return
         */
        private List<AbstractDrawableComponent> inPoly(Polygon poly) {
            String pgType = null;
            Iterator<AbstractDrawableComponent> it = drawingLayer
                    .getActiveLayer().getComponentIterator();
            List<AbstractDrawableComponent> adcList = new ArrayList<AbstractDrawableComponent>();

            while (it.hasNext()) {
                AbstractDrawableComponent adc = it.next();

                if (adc instanceof Contours) {
                    if ((adc.getPgenCategory().equalsIgnoreCase(pgenCat))
                            || (pgenCat.equalsIgnoreCase(PgenConstant.CATEGORY_ANY))){
                        adcList.addAll(contourChildrenInPoly((Contours) adc, poly));
                        pgType = pgenObj;
                    }
                }
                // if category is "Any"
                else if (pgenCat.equalsIgnoreCase(PgenConstant.CATEGORY_ANY)){
                    pgType = adc.getPgenType();
                    List<Coordinate> pts = adc.getPoints();
                    for (Coordinate pt : pts) {
                        double pix[] = mapEditor.translateInverseClick(pt);
                        if (poly.contains(pix[0], pix[1])) {
                            adcList.add(adc);
                        }
                    }
                }
                // Multiple kinds of category with "Any"
                // if category is text, all elements need to be the same pgen
                // type
                if ((pgType == null && adc.getPgenCategory()
                        .equalsIgnoreCase(pgenCat))
                        || (pgType == null &&  (pgenCat.equalsIgnoreCase(PgenConstant.CATEGORY_ANY)))
                        || (pgType != null && adc.getPgenType()
                                .equalsIgnoreCase(pgType))) {
                    List<Coordinate> pts = adc.getPoints();
                    for (Coordinate pt : pts) {
                        double pix[] = mapEditor.translateInverseClick(pt);
                        if (poly.contains(pix[0], pix[1])) {
                            adcList.add(adc);
                            if (pgenCat.equalsIgnoreCase(PgenConstant.CATEGORY_TEXT)) {
                                pgType = adc.getPgenType();
                                PgenMultiSelectTool.this.pgenType = pgType;
                            }
                            break;
                        }
                    }
                }
            }
            return adcList;
        }

        /**
         * Returns all elements of current Pgen category in a specified contour
         * in the input area.
         * 
         * @param con
         *            - a contour object
         * @param poly
         *            - polygon
         * @return - a list of drawable elements
         */
        private List<AbstractDrawableComponent> contourChildrenInPoly(
                Contours con, Polygon poly) {
            Iterator<DrawableElement> it = con.createDEIterator();
            List<AbstractDrawableComponent> adcList = new ArrayList<AbstractDrawableComponent>();

            while (it.hasNext()) {
                DrawableElement de = it.next();
                    List<Coordinate> pts = de.getPoints();
                    for (Coordinate pt : pts) {
                        double pix[] = mapEditor.translateInverseClick(pt);
                        if (poly.contains(pix[0], pix[1])) {
                            adcList.add(de);
                            pgenObj = de.getName();
                            break;
                        }
                    }
            }
            return adcList;
        }

    }
}