/*
 * gov.noaa.nws.ncep.ui.pgen.tools.PgenVectorDrawingTool
 *
 * May 7th, 2009
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.ui.pgen.tools;

import java.util.ArrayList;

import com.raytheon.uf.viz.core.rsc.IInputHandler;

import gov.noaa.nws.ncep.ui.pgen.PgenUtil;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.AttrSettings;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.VectorAttrDlg;
import gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableElementFactory;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableType;
import gov.noaa.nws.ncep.ui.pgen.elements.Vector;

import org.locationtech.jts.geom.Coordinate;
//import gov.noaa.nws.ncep.ui.display.InputHandlerDefaultImpl;

/**
 * Implements a modal map tool for PGEN vector drawing.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * 05/09         111      J. Wu     Initial creation
 * Mar 03, 2016  R13557   J. Beck   Set keyboard focus to wind speed text field
 *                                  for Wind Barb and Wind Arrow
 * Jul 26, 2019  66393    mapeters  Handle {@link AttrSettings#getSettings}
 *                                  change
 *
 * </pre>
 *
 * @author J. Wu
 */

public class PgenVectorDrawingTool extends AbstractPgenDrawingTool {

    public PgenVectorDrawingTool() {
        super();
    }

    @Override
    protected void activateTool() {

        super.activateTool();

        AbstractDrawableComponent attr = AttrSettings.getInstance()
                .getSettings(pgenType);
        if (attr == null) {
            ((VectorAttrDlg) attrDlg).adjustAttrForDlg(pgenType);
        }

        // If the attrDlg has an enabled spdTxt field, set focus on it initially
        if (isSpeedTextFocusNeeded((VectorAttrDlg) attrDlg)) {
            ((VectorAttrDlg) attrDlg).setSpeedTextFocus(attrDlg);
        }
    }

    @Override
    public IInputHandler getMouseHandler() {

        if (this.mouseHandler == null) {
            this.mouseHandler = new PgenVectorDrawingHandler();
        }

        return this.mouseHandler;
    }

    /**
     * Determines if we want keyboard focus maintained on the speed text field
     * for a given VectorAttrDlg.
     *
     * Our solution is that if the actual speedText Text field is enabled, then
     * we want to maintain focus on it.
     *
     * This may seem unnecessary but this extra, yet simple level of abstraction
     * provides a place for other/future criteria and business logic.
     *
     * @param dialog
     *            the dialog that contains the speedText field
     *
     * @return true if this dialog has the speedText attribute enabled, false
     *         otherwise
     *
     * @author J.Beck
     *
     */
    public boolean isSpeedTextFocusNeeded(VectorAttrDlg dialog) {

        if (dialog != null) {
            return dialog.isSpeedTextEnabled();
        }

        return false;
    }

    /**
     * Implements input handler for mouse events.
     *
     * @author bingfan
     *
     */

    public class PgenVectorDrawingHandler extends InputHandlerDefaultImpl {

        /**
         * Left mouse button initializes drawing, and sets an attribute like
         * direction, depending on drawing tool.
         */
        private int LEFT_MOUSE_BUTTON_CLICK = 1;

        /**
         * Left mouse click, followed by right mouse click, finalizes editing.
         */
        private int RIGHT_MOUSE_BUTTON_CLICK = 3;

        /**
         * Points of the new element.
         */
        private ArrayList<Coordinate> points = new ArrayList<>();

        /**
         * An instance of DrawableElementFactory, which is used to create new
         * elements.
         */
        private DrawableElementFactory def = new DrawableElementFactory();

        /**
         * Current element.
         */
        private AbstractDrawableComponent elem = null;

        @Override
        public boolean handleMouseDown(int anX, int aY, int button) {
            if (!isResourceEditable()) {
                return false;
            }

            // Check if mouse is in geographic extent
            Coordinate loc = mapEditor.translateClick(anX, aY);

            if (loc == null || shiftDown) {
                return false;
            }

            // If this dialog has an enabled speedText field, keep focus on it.
            if (isSpeedTextFocusNeeded((VectorAttrDlg) attrDlg)) {
                ((VectorAttrDlg) attrDlg).setSpeedTextFocus(attrDlg);
            }

            if (button == (LEFT_MOUSE_BUTTON_CLICK)) {

                if (points.size() == 0) {
                    points.add(0, loc);

                    elem = def.create(DrawableType.VECTOR, attrDlg,
                            pgenCategory, pgenType, points.get(0),
                            drawingLayer.getActiveLayer());
                } else {

                    drawingLayer.removeElement(elem);

                    elem = def.create(DrawableType.VECTOR, attrDlg,
                            pgenCategory, pgenType, points.get(0),
                            drawingLayer.getActiveLayer());

                    double dir = ((Vector) elem).vectorDirection(points.get(0),
                            loc);

                    ((Vector) elem).setDirection(dir);
                    ((VectorAttrDlg) attrDlg)
                            .setDirection(((Vector) elem).getDirection());

                }

                // add the product to the PGEN resource and repaint.
                if (elem != null) {
                    drawingLayer.addElement(elem);
                    mapEditor.refresh();
                    AttrSettings.getInstance().setSettings(elem);
                }

                return true;

            } else if (button == RIGHT_MOUSE_BUTTON_CLICK) {

                return true;

            } else {

                return false;

            }
        }

        @Override
        public boolean handleMouseMove(int x, int y) {
            if (!isResourceEditable()) {
                return false;
            }

            // Check if mouse is in geographic extent
            Coordinate loc = mapEditor.translateClick(x, y);
            if (loc == null) {
                return false;
            }

            // If this dialog has an enabled speedText field, keep focus on it.
            if (isSpeedTextFocusNeeded((VectorAttrDlg) attrDlg)) {
                ((VectorAttrDlg) attrDlg).setSpeedTextFocus(attrDlg);
            }

            if (attrDlg != null && points.size() != 0) {

                AbstractDrawableComponent ghost = null;

                ghost = def.create(DrawableType.VECTOR, attrDlg, pgenCategory,
                        pgenType, points.get(0), drawingLayer.getActiveLayer());

                double dir = ((Vector) ghost).vectorDirection(points.get(0),
                        loc);

                ((Vector) ghost).setDirection(dir);
                ((VectorAttrDlg) attrDlg).setDirection(dir);

                drawingLayer.setGhostLine(ghost);
                mapEditor.refresh();
            }

            return false;
        }

        @Override
        public boolean handleMouseUp(int x, int y, int button) {
            if (!drawingLayer.isEditable() || shiftDown) {
                return false;
            }

            // If the dialog has an enabled spdTxt field, keep focus on it.
            if (isSpeedTextFocusNeeded((VectorAttrDlg) attrDlg)) {
                ((VectorAttrDlg) attrDlg).setSpeedTextFocus(attrDlg);
            }

            if (button == RIGHT_MOUSE_BUTTON_CLICK) {
                drawingLayer.removeGhostLine();
                mapEditor.refresh();

                if (points.size() > 0) {

                    points.clear();

                } else {

                    attrDlg.close();

                    PgenUtil.setSelectingMode();
                }
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean handleMouseDownMove(int x, int y, int mouseButton) {

            if (!isResourceEditable() || shiftDown) {
                return false;
            } else {
                return true;
            }
        }

    }

}
