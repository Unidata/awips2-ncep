/*
 * gov.noaa.nws.ncep.ui.pgen.rsc.PgenTextDrawingTool
 * 
 * 22 May 2009
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.ui.pgen.tools;

import java.awt.geom.Line2D;

import com.raytheon.uf.viz.core.rsc.IInputHandler;
import com.raytheon.viz.ui.editor.AbstractEditor;
import org.locationtech.jts.geom.Coordinate;

import gov.noaa.nws.ncep.ui.pgen.PgenConstant;
import gov.noaa.nws.ncep.ui.pgen.PgenUtil;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.AttrSettings;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.TextAttrDlg;
import gov.noaa.nws.ncep.ui.pgen.display.IAttribute;
import gov.noaa.nws.ncep.ui.pgen.display.IText;
import gov.noaa.nws.ncep.ui.pgen.display.IText.DisplayType;
import gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent;
import gov.noaa.nws.ncep.ui.pgen.elements.ComboSymbol;
import gov.noaa.nws.ncep.ui.pgen.elements.DECollection;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableElement;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableElementFactory;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableType;
import gov.noaa.nws.ncep.ui.pgen.elements.Line;
import gov.noaa.nws.ncep.ui.pgen.elements.Outlook;
import gov.noaa.nws.ncep.ui.pgen.elements.Symbol;

/**
 * Implements a modal map tool for PGEN text drawing.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 05/09            79      B. Yin      Moved from PgenSinglePointDrawingTool
 * 06/09            116     B. Yin      Handle the labeled symbol case
 * 07/10            ?       B. Yin      Added '[' or ']' for front labels 
 * 02/11            ?       B. Yin      Fixed Outlook type problem.
 * 04/11            ?       B. Yin      Re-factor IAttribute
 * 08/12         #802       Q. Zhou     Fixed Front text of 2 lines. Modified handleMouseMove.
 * 12/12         #591       J. Wu       TTR343 - added default label value for some fronts.
 * 10/13         TTR768     J. Wu       Set default attributes for outlook labels (Text).
 * 12/15         R12989     P. Moyer    Prior text attribute tracking via pgenTypeLabels HashMap
 * 05/16/2016    R18388     J. Wu       Move some constants to PgenConstant.
 * May 16, 2016 5640        bsteffen    Access triggering component using PgenUtil.
 * Feb 14, 2020  74902      smanoj      Remove leading empty lines from Text Attributes.
 * Feb 26, 2020  75024      smanoj      Fix to have correct default Text Attributes for 
 *                                      tropical TROF front label.
 * </pre>
 * 
 * @author B. Yin
 */
public class PgenTextDrawingTool extends AbstractPgenDrawingTool {

    private boolean addLabelToSymbol;

    private AbstractDrawableComponent prevElem;

    private boolean usePrevColor;

    public PgenTextDrawingTool() {

        super();

    }

    @Override
    protected void activateTool() {

        super.activateTool();

        if (attrDlg != null && !isDelObj()) {

            String param = event.getParameter(PgenConstant.EVENT_LABEL);
            if (param != null) {

                AbstractDrawableComponent triggerComponent = PgenUtil
                        .getTriggerComponent(event);

                if (Boolean.parseBoolean(param)) {
                    addLabelToSymbol = true;
                }
                if (triggerComponent != null) {
                    prevElem = triggerComponent;
                    if (prevElem.getParent() instanceof Outlook
                            && ((Outlook) prevElem.getParent()).getOutlookType()
                                    .equalsIgnoreCase("MESO_DSC")) {
                        ((TextAttrDlg) attrDlg).setBoxText(true,
                                DisplayType.BOX);
                    } else if (prevElem.getName()
                            .equalsIgnoreCase(Outlook.OUTLOOK_LABELED_LINE)
                            || prevElem.getPgenCategory().equalsIgnoreCase(
                                    PgenConstant.CATEGORY_FRONT)) {
                        if (!(PgenConstant.TYPE_TROPICAL_TROF
                                .equalsIgnoreCase(prevElem.getPgenType()))) {
                            ((TextAttrDlg) attrDlg).setBoxText(false,
                                    DisplayType.NORMAL);
                            ((TextAttrDlg) attrDlg).setFontSize(18);
                        }
                    }

                    /*
                     * Set default text attributes for outlook labels -
                     * depending on the outlook type!
                     */
                    AbstractDrawableComponent dfltText = null;
                    if (prevElem.getParent() instanceof Outlook) {
                        String outlookType = ((Outlook) (prevElem.getParent()))
                                .getOutlookType();
                        String key = new String(outlookType);
                        if ((param = event.getParameter(
                                PgenConstant.EVENT_DEFAULT_TEXT)) != null
                                && !param.equalsIgnoreCase(
                                        PgenConstant.EVENT_OTHER)) {
                            key = key + param;
                        }

                        dfltText = AttrSettings.getInstance()
                                .getOutlookLabelSettings().get(key);
                        if (dfltText == null) {
                            for (String skey : AttrSettings.getInstance()
                                    .getOutlookLabelSettings().keySet()) {
                                dfltText = AttrSettings.getInstance()
                                        .getOutlookLabelSettings().get(skey);
                                if (dfltText != null)
                                    break;
                            }
                        }

                        if (dfltText != null) {
                            ((TextAttrDlg) attrDlg).setAttr(dfltText);
                        }
                    }
                }

                if ((param = event
                        .getParameter(PgenConstant.EVENT_PREV_COLOR)) != null) {

                    if (param.equalsIgnoreCase(PgenConstant.TRUE))
                        usePrevColor = true;

                    if (usePrevColor) {
                        ((TextAttrDlg) attrDlg).setColor(
                                prevElem.getPrimaryDE().getColors()[0]);
                    }
                }

                if (prevElem.getName()
                        .equalsIgnoreCase(PgenConstant.TYPE_VOLCANO)) {
                    ((TextAttrDlg) attrDlg).setFontSize(18);
                    ((TextAttrDlg) attrDlg).setBoxText(true, DisplayType.BOX);
                }

                // if current item pgenType exists in hashMap, retrieve the
                // text, otherwise use existing code.
                // this is the case for symbols with labels (not text by itself)
                // This also skips Outlook text labels, which are handled by the
                // "defaultTxt" section below.
                String[] textLabel;
                if ((!(prevElem.getParent() instanceof Outlook))
                        && ((textLabel = AttrSettings.getInstance()
                                .getPgenTypeLabel(
                                        prevElem.getPgenType())) != null)) {
                    ((TextAttrDlg) attrDlg).setText(textLabel);
                    return;
                    // interrupts the other attempts below.
                }

                // handles items that initially have defaultTxt parameters in
                // the event call, such as Outlook
                if ((param = event
                        .getParameter(PgenConstant.EVENT_DEFAULT_TEXT)) != null
                        && !param.equalsIgnoreCase(PgenConstant.EVENT_OTHER)) {
                    String txt = param;
                    if (!txt.isEmpty()) {
                        String[] txtArray = { "", "" };
                        if (txt.contains("\n")) {
                            txtArray[0] = txt.substring(0, txt.indexOf('\n'));
                            txtArray[1] = txt.substring(txt.indexOf('\n') + 1,
                                    txt.length());
                        } else {
                            txtArray[0] = txt;
                        }
                        ((TextAttrDlg) attrDlg).setText(txtArray);
                        return;
                    }
                }

                /*
                 * Jun (12/18/2012, TTR 343/TTN 591) - for Squall Line,Tropical
                 * Wave, Outflow boundary, Dry Line, and Shear Line, need to use
                 * the default label so the client won't need to type it -
                 */
                if (prevElem.getName()
                        .equalsIgnoreCase(PgenConstant.LABELED_FRONT)) {
                    String flbl = getDefaultFrontLabel(prevElem);
                    ((TextAttrDlg) attrDlg).setText(new String[] { flbl });
                    return;
                }

                /*
                 * final check - if defaults above did not trigger, and the
                 * prevElem's pgenType did not exist in the hash map, fill the
                 * text box with blank string array.
                 */
                if (AttrSettings.getInstance()
                        .getPgenTypeLabel(prevElem.getPgenType()) == null) {
                    ((TextAttrDlg) attrDlg).setText(new String[] { "" });
                }
            } else { // need to check that this is a General Text object.
                // if this is a straight text object, retrieve the text if it
                // exists in the HashMap, otherwise default to blank.
                String[] textLabel;
                if ((textLabel = AttrSettings.getInstance().getPgenTypeLabel(
                        PgenConstant.CATEGORY_TEXT)) != null) {
                    ((TextAttrDlg) attrDlg).setText(textLabel);
                    return; // for initial testing of concept
                } else {
                    ((TextAttrDlg) attrDlg).setText(new String[] { "" });
                }
                return;
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

            this.mouseHandler = new PgenTextDrawingHandler();

        }

        return this.mouseHandler;
    }

    /**
     * Implements input handler for mouse events.
     * 
     * @author bingfan
     * 
     */

    public class PgenTextDrawingHandler extends InputHandlerDefaultImpl {

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
            if (!isResourceEditable())
                return false;

            // Check if mouse is in geographic extent
            Coordinate loc = mapEditor.translateClick(anX, aY);
            if (loc == null || shiftDown)
                return false;
            if (button == 1) {

                // variables for current type tracking for label array
                // defaults to "Text" type if not a child label.
                String pgenTypeStr = PgenConstant.CATEGORY_TEXT;
                String[] pgenLabelArray = null;

                // create an element.
                if (((IText) attrDlg).getString().length > 0) {

                    pgenLabelArray = ((IText) attrDlg).getString();

                    elem = def.create(DrawableType.TEXT, (IAttribute) attrDlg,
                            pgenCategory, pgenType, loc,
                            drawingLayer.getActiveLayer());
                }

                // add the product to the PGEN resource and repaint.
                if (elem != null) {

                    if (addLabelToSymbol && prevElem != null
                            && (prevElem.getName().equalsIgnoreCase(
                                    PgenConstant.LABELED_SYMBOL)
                            || prevElem.getName().equalsIgnoreCase(
                                    PgenConstant.TYPE_VOLCANO))) {
                        ((DECollection) prevElem).add(elem);
                    } else if (prevElem != null && prevElem.getName()
                            .equalsIgnoreCase(Outlook.OUTLOOK_LABELED_LINE)) {
                        ((DECollection) prevElem).add(elem);
                    } else if (prevElem instanceof DECollection
                            && prevElem.getPgenCategory().equalsIgnoreCase(
                                    PgenConstant.CATEGORY_FRONT)) {
                        ((DECollection) prevElem).add(elem);
                    } else {
                        drawingLayer.addElement(elem);
                    }
                    AttrSettings.getInstance()
                            .setSettings((DrawableElement) elem);
                    mapEditor.refresh();

                    attrDlg.getShell().setActive();
                }

                if (addLabelToSymbol) {

                    pgenTypeStr = prevElem.getPgenType();

                    addLabelToSymbol = false;
                    usePrevColor = false;
                    if (prevElem.getName()
                            .equalsIgnoreCase(PgenConstant.LABELED_SYMBOL)
                            || prevElem.getName().equalsIgnoreCase(
                                    PgenConstant.TYPE_VOLCANO)) {
                        if (prevElem.getPrimaryDE() instanceof Symbol) {
                            PgenUtil.setDrawingSymbolMode(
                                    prevElem.getPrimaryDE().getPgenCategory(),
                                    prevElem.getPgenType(), false, null);
                        } else if (prevElem
                                .getPrimaryDE() instanceof ComboSymbol) {
                            PgenUtil.setDrawingSymbolMode(
                                    PgenConstant.CATEGORY_COMBO,
                                    prevElem.getPgenType(), false, null);
                        }
                    } else if (prevElem instanceof DECollection
                            && prevElem.getPgenCategory().equalsIgnoreCase(
                                    PgenConstant.CATEGORY_FRONT)) {
                        PgenUtil.setDrawingFrontMode(
                                (Line) prevElem.getPrimaryDE());
                    } else if (prevElem.getName()
                            .equalsIgnoreCase(Outlook.OUTLOOK_LABELED_LINE)) {
                        PgenUtil.loadOutlookDrawingTool();
                    }

                    prevElem = null;
                }

                // Add the new text string to the shared pgenTypeLabels hashMap
                // unless it is an Outlook item which handles itself differently
                AttrSettings.getInstance().setPgenTypeLabel(pgenTypeStr,
                        pgenLabelArray);

                return true;

            } else if (button == 3) {

                return true;

            } else {

                return false;

            }

        }

       
        @Override
        public boolean handleMouseMove(int x, int y) {
            if (!isResourceEditable())
                return false;

            // Check if mouse is in geographic extent
            Coordinate loc = mapEditor.translateClick(x, y);
            if (loc == null)
                return false;

            if (attrDlg != null) {

                AbstractDrawableComponent ghost = null;

                if (((IText) attrDlg).getString().length > 0) {
                    // add "[" or "]" to front labels. The rule: If the label is
                    // only number and in one line, will be surrounded by [,].
                    if (addLabelToSymbol && prevElem.getPgenCategory() != null
                            && prevElem.getPgenCategory().equalsIgnoreCase(
                                    PgenConstant.CATEGORY_FRONT)) {

                        String[] text = ((IText) attrDlg).getString();
                        if (text.length == 1) {
                            StringBuffer lbl = new StringBuffer(
                                    ((TextAttrDlg) attrDlg).getString()[0]);

                            if (lbl.length() > 0) {
                                if (lbl.charAt(0) == '[')
                                    lbl.deleteCharAt(0);
                                if (lbl.charAt(lbl.length() - 1) == ']')
                                    lbl.deleteCharAt(lbl.length() - 1);

                                try {
                                    Integer.parseInt(lbl.toString());
                                    // check if the text is right or left of the
                                    // front
                                    if (rightOfLine(mapEditor, loc,
                                            (Line) prevElem
                                                    .getPrimaryDE()) >= 0) {

                                        ((TextAttrDlg) attrDlg).setText(
                                                new String[] { lbl + "]" });
                                    } else {
                                        ((TextAttrDlg) attrDlg).setText(
                                                new String[] { "[" + lbl });
                                    }
                                } catch (NumberFormatException e) {
                                    /* do nothing */}
                            }
                        }

                        ghost = def.create(DrawableType.TEXT,
                                (IAttribute) attrDlg, pgenCategory, pgenType,
                                loc, drawingLayer.getActiveLayer());

                    } else {

                        // Remove the leading empty lines from text.
                        String[] textArray =((IText) attrDlg).getString();
                        String textStr = String.join("\n",textArray);
                        textStr = textStr.trim();
                        textArray= textStr.split("\n");
                        ((TextAttrDlg) attrDlg).setText(textArray);

                        ghost = def.create(DrawableType.TEXT, (IText) attrDlg,
                                pgenCategory, pgenType, loc,
                                drawingLayer.getActiveLayer());
                    }
                }

                drawingLayer.setGhostLine(ghost);
                mapEditor.refresh();

            }

            return false;

        }

        @Override
        public boolean handleMouseDownMove(int x, int y, int mouseButton) {
            if (!isResourceEditable() || shiftDown)
                return false;
            else
                return true;
        }

       
        @Override
        public boolean handleMouseUp(int x, int y, int mouseButton) {
            if (!isResourceEditable() || shiftDown)
                return false;

            // prevent the click going through to other handlers
            // in case adding labels to symbols or fronts.
            if (mouseButton == 3) {

                drawingLayer.removeGhostLine();
                mapEditor.refresh();

                if (addLabelToSymbol) {
                    addLabelToSymbol = false;
                    usePrevColor = false;
                    if (prevElem.getName()
                            .equalsIgnoreCase(PgenConstant.LABELED_SYMBOL)) {
                        if (prevElem.getPrimaryDE() instanceof Symbol) {
                            PgenUtil.setDrawingSymbolMode(
                                    prevElem.getPrimaryDE().getPgenCategory(),
                                    prevElem.getPgenType(), false, null);
                        } else if (prevElem
                                .getPrimaryDE() instanceof ComboSymbol) {
                            PgenUtil.setDrawingSymbolMode(
                                    PgenConstant.CATEGORY_COMBO,
                                    prevElem.getPgenType(), false, null);
                        }
                    } else if (prevElem instanceof DECollection
                            && prevElem.getPgenCategory().equalsIgnoreCase(
                                    PgenConstant.CATEGORY_FRONT)) {
                        PgenUtil.setDrawingFrontMode(
                                (Line) prevElem.getPrimaryDE());
                    } else if (prevElem.getName()
                            .equalsIgnoreCase(Outlook.OUTLOOK_LABELED_LINE)) {
                        PgenUtil.loadOutlookDrawingTool();
                    }
                    prevElem = null;
                } else {
                    PgenUtil.setSelectingMode();
                }

                return true;

            } else {

                return false;

            }
        }

    }

    /**
     * Check if a point is at right or left of the line. return: 1 - right side
     * of the line -1 - left side of the line 0 - on the line
     */
    public static int rightOfLine(AbstractEditor mEditor, Coordinate pt,
            Line ln) {

        double screenPt[] = mEditor.translateInverseClick(pt);

        Coordinate lnPts[] = ln.getLinePoints();

        double minDist = 0;
        double startPt[] = new double[2];
        double endPt[] = new double[2];

        for (int ii = 0; ii < lnPts.length - 1; ii++) {
            double pt0[] = mEditor.translateInverseClick(lnPts[ii]);
            double pt1[] = mEditor.translateInverseClick(lnPts[ii + 1]);

            double min = Line2D.ptSegDist(pt0[0], pt0[1], pt1[0], pt1[1],
                    screenPt[0], screenPt[1]);

            if (ii == 0 || min < minDist) {
                minDist = min;
                startPt[0] = pt0[0];
                startPt[1] = pt0[1];
                endPt[0] = pt1[0];
                endPt[1] = pt1[1];
            }
        }

        if (minDist == 0)
            return 0;

        else
            return Line2D.relativeCCW(screenPt[0], screenPt[1], startPt[0],
                    startPt[1], endPt[0], endPt[1]);

    }

    /*
     * Set default label for a few specific fronts.
     */
    private String getDefaultFrontLabel(AbstractDrawableComponent elem) {

        // Use default label for specific fronts.
        String frontLabel = "";
        String ptype = elem.getPgenType();
        if (PgenConstant.TYPE_TROF.equalsIgnoreCase(ptype)) {
            frontLabel = new String(PgenConstant.LABEL_TROF);
        } else if (PgenConstant.TYPE_TROPICAL_TROF.equalsIgnoreCase(ptype)) {
            frontLabel = new String(PgenConstant.LABEL_TRPCL_WAVE);
        } else if (PgenConstant.TYPE_DRY_LINE.equalsIgnoreCase(ptype)) {
            frontLabel = new String(PgenConstant.LABEL_DRYLINE);
        } else if (PgenConstant.TYPE_INSTABILITY.equalsIgnoreCase(ptype)) {
            frontLabel = new String(PgenConstant.LABEL_SQUALL_LINE);
        } else if (PgenConstant.TYPE_SHEAR_LINE.equalsIgnoreCase(ptype)) {
            frontLabel = new String(PgenConstant.LABEL_SHEARLINE);
        }

        return frontLabel;

    }

}
