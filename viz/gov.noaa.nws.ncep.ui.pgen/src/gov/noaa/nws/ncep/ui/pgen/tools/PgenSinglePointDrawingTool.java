/*
 * gov.noaa.nws.ncep.ui.pgen.rsc.PgenSinglePointDrawingTool
 * 
 * 2 February 2009
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.ui.pgen.tools;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;

import com.raytheon.uf.viz.core.rsc.IInputHandler;
import com.raytheon.viz.ui.input.InputAdapter;
import com.vividsolutions.jts.geom.Coordinate;

import gov.noaa.nws.ncep.ui.pgen.PgenUtil;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.AttrSettings;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.LabeledSymbolAttrDlg;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.SymbolAttrDlg;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.VolcanoAttrDlg;
import gov.noaa.nws.ncep.ui.pgen.display.IAttribute;
import gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent;
import gov.noaa.nws.ncep.ui.pgen.elements.DECollection;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableElement;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableElementFactory;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableType;
import gov.noaa.nws.ncep.ui.pgen.elements.Outlook;

/**
 * Implements a modal map tool for PGEN single point drawing.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 02/09                    B. Yin      Initial Creation.
 * 04/09            72      S. Gilbert  Modified to use PgenSession and PgenCommands
 * 04/09            88      J. Wu       Added Text drawing
 * 04/24            99      G. Hull     Use NmapUiUtils
 * 04/09           103      B. Yin      Extends from AbstractPgenTool
 * 05/09           #42      S. Gilbert  Added pgenType and pgenCategory
 * 05/09            79      B. Yin      Extends from AbstractPgenDrawingTool
 * 06/09           116      B. Yin      Use AbstractDrawableComponent
 * May 16, 2016   5640      bsteffen    Access triggering component using PgenUtil.
 * 06/15/2016       R13559  bkowal      File cleanup. No longer simulate mouse clicks.
 * Jan 30, 2019   7553      bhurley     Fixed panning issue when PGEN symbols/markers are selected
 * 
 * </pre>
 * 
 * @author B. Yin
 */

public class PgenSinglePointDrawingTool extends AbstractPgenDrawingTool {

    private static DECollection dec;

    private AbstractDrawableComponent prevElem;

    private boolean usePrevColor;

    public PgenSinglePointDrawingTool() {

        super();
        dec = null;

    }

    @Override
    protected void activateTool() {
        super.activateTool();

        if (attrDlg != null && !isDelObj()) {

            prevElem = PgenUtil.getTriggerComponent(event);

            String param = event.getParameter("usePrevColor");
            if (param != null) {

                if (Boolean.parseBoolean(param)) {
                    usePrevColor = true;
                }

                if (usePrevColor) {
                    ((SymbolAttrDlg) attrDlg)
                            .setColor(prevElem.getPrimaryDE().getColors()[0]);
                }
            }

        }

    }

    /**
     * Returns the current mouse handler.
     * 
     * @return
     */
    public IInputHandler getMouseHandler() {

        if (this.mouseHandler == null) {

            this.mouseHandler = new PgenSinglePointDrawingHandler();

        }

        return this.mouseHandler;
    }

    /**
     * Implements input handler for mouse events.
     * 
     * @author bingfan
     * 
     */

    public class PgenSinglePointDrawingHandler extends InputAdapter {

        /**
         * An instance of DrawableElementFactory, which is used to create new
         * elements.
         */
        private DrawableElementFactory def = new DrawableElementFactory();

        /**
         * Current element.
         */
        private DrawableElement elem = null;

        @Override
        public boolean handleMouseDown(Event event) {
            if (!isResourceEditable()) {
                return false;
            }

            // Check if mouse is in geographic extent
            Coordinate loc = mapEditor.translateClick(event.x, event.y);
            if (loc == null || isShiftKeyPressed(event)) {
                return false;
            }

            if (event.button == 1) {
                // create an element.
                elem = (DrawableElement) def.create(getDrawableType(),
                        (IAttribute) attrDlg, pgenCategory, pgenType, loc,
                        drawingLayer.getActiveLayer());

                ((SymbolAttrDlg) attrDlg).setLatitude(loc.y);
                ((SymbolAttrDlg) attrDlg).setLongitude(loc.x);
                ((SymbolAttrDlg) attrDlg).enableUndoBtn(true);

                // add the product to the PGEN resource and repaint.
                if (elem != null) {
                    if (prevElem != null && prevElem.getName()
                            .equalsIgnoreCase(Outlook.OUTLOOK_LABELED_LINE)) {
                        ((DECollection) prevElem).add(elem);
                    } else if (((SymbolAttrDlg) attrDlg).labelEnabled()) {
                        dec = new DECollection("labeledSymbol");
                        dec.setPgenCategory(pgenCategory);
                        dec.setPgenType(pgenType);
                        dec.addElement(elem);
                        drawingLayer.addElement(dec);
                    } else {
                        drawingLayer.addElement(elem);
                    }

                    attrDlg.setDrawableElement(elem);
                    AttrSettings.getInstance().setSettings(elem);
                    mapEditor.refresh();
                }

                if (prevElem != null) {
                    usePrevColor = false;
                    if (prevElem.getName()
                            .equalsIgnoreCase(Outlook.OUTLOOK_LABELED_LINE)) {
                        PgenUtil.loadOutlookDrawingTool();
                    }
                    prevElem = null;
                }

                return true;

            } else if (event.button == 3) {

                return true;

            } else {

                return false;

            }

        }

        @Override
        public boolean handleMouseMove(Event event) {
            if (!isResourceEditable()) {
                return false;
            }

            // Check if mouse is in geographic extent
            Coordinate loc = mapEditor.translateClick(event.x, event.y);
            if (loc == null) {
                return false;
            }

            if (attrDlg != null) {

                AbstractDrawableComponent ghost = null;

                ghost = def.create(getDrawableType(), (IAttribute) attrDlg,
                        pgenCategory, pgenType, loc,
                        drawingLayer.getActiveLayer());
                drawingLayer.setGhostLine(ghost);
                mapEditor.refresh();
                mapEditor.setFocus();

            }

            return false;

        }

        @Override
        public boolean handleMouseDownMove(Event event) {

            if (!isResourceEditable() || isShiftKeyPressed(event)) {
                return false;
            } else {
                return true;
            }

        }

        @Override
        public boolean handleMouseUp(Event event) {
            if (!isResourceEditable() || isShiftKeyPressed(event)) {
                return false;
            }

            if (event.button == 3) {
                // prevent the click going through to other handlers
                // in case adding labels to symbols or fronts.
                if (elem != null && ((SymbolAttrDlg) attrDlg).labelEnabled()) {
                    drawingLayer.removeGhostLine();
                    mapEditor.refresh();

                    String defaultTxt = "";
                    if (attrDlg instanceof VolcanoAttrDlg) {
                        defaultTxt = ((VolcanoAttrDlg) attrDlg).getVolText();
                        dec.setCollectionName("Volcano");
                    }

                    // in case the label is enabled after symbol is placed.
                    if (dec == null
                            && ((SymbolAttrDlg) attrDlg).labelEnabled()) {
                        dec = new DECollection("labeledSymbol");
                        dec.setPgenCategory(pgenCategory);
                        dec.setPgenType(pgenType);
                        dec.addElement(elem);
                        drawingLayer.replaceElement(elem, dec);
                    }

                    PgenUtil.setDrawingTextMode(true,
                            ((LabeledSymbolAttrDlg) attrDlg).useSymbolColor(),
                            defaultTxt, dec);
                    dec = null;
                    elem = null;
                } else {
                    if (prevElem != null) {
                        usePrevColor = false;
                        if ("OUTLOOK".equalsIgnoreCase(
                                prevElem.getParent().getPgenCategory())) {
                            PgenUtil.loadOutlookDrawingTool();
                        }
                        prevElem = null;
                    } else {
                        elem = null;
                        PgenUtil.setSelectingMode();
                    }
                }
                return true;
            } else {
                return false;
            }
        }

        /**
         * Tests if the shift key was pressed when the event was generated.
         * 
         * @param event
         *            the generated event
         * @return true if the shift key was pressed when the event was
         *         generated; false if otherwise
         */
        private boolean isShiftKeyPressed(Event event) {
            return (event.stateMask & SWT.SHIFT) != 0;
        }
    }

    /**
     * Determine the proper DrawableType from the value of the pgenCategory
     * attribute.
     * 
     * @return
     */
    private DrawableType getDrawableType() {
        DrawableType which = DrawableType.SYMBOL;
        if ("Combo".equals(pgenCategory)) {
            which = DrawableType.COMBO_SYMBOL;
        }
        return which;
    }

    /**
     * Get the collection that contains the symbol. When add the label, add it
     * to the same collection
     * 
     * @return
     */
    public static DECollection getCollection() {
        return dec;
    }

}
