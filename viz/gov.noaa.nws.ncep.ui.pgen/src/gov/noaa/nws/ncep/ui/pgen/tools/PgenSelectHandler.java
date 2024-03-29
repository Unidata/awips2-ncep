/*
 * gov.noaa.nws.ncep.ui.pgen.tools.PgenSelectHandler
 *
 * 1 April 2013
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.ui.pgen.tools;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.ui.PlatformUI;
import org.geotools.referencing.GeodeticCalculator;

import com.raytheon.viz.ui.editor.AbstractEditor;

import gov.noaa.nws.ncep.ui.pgen.PgenUtil;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.AttrDlg;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.AttrDlgFactory;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.AttrSettings;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.ContoursAttrDlg;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.GfaAttrDlg;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.JetAttrDlg;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.LabeledSymbolAttrDlg;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.OutlookAttrDlg;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.SymbolAttrDlg;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.TextAttrDlg;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.TrackAttrDlg;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.TrackExtrapPointInfoDlg;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.WatchBoxAttrDlg;
import gov.noaa.nws.ncep.ui.pgen.contours.ContourCircle;
import gov.noaa.nws.ncep.ui.pgen.contours.ContourLine;
import gov.noaa.nws.ncep.ui.pgen.contours.ContourMinmax;
import gov.noaa.nws.ncep.ui.pgen.contours.Contours;
import gov.noaa.nws.ncep.ui.pgen.display.IText;
import gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent;
import gov.noaa.nws.ncep.ui.pgen.elements.Arc;
import gov.noaa.nws.ncep.ui.pgen.elements.ComboSymbol;
import gov.noaa.nws.ncep.ui.pgen.elements.DECollection;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableElement;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableType;
import gov.noaa.nws.ncep.ui.pgen.elements.Jet;
import gov.noaa.nws.ncep.ui.pgen.elements.Line;
import gov.noaa.nws.ncep.ui.pgen.elements.MultiPointElement;
import gov.noaa.nws.ncep.ui.pgen.elements.Outlook;
import gov.noaa.nws.ncep.ui.pgen.elements.SinglePointElement;
import gov.noaa.nws.ncep.ui.pgen.elements.Symbol;
import gov.noaa.nws.ncep.ui.pgen.elements.Text;
import gov.noaa.nws.ncep.ui.pgen.elements.Track;
import gov.noaa.nws.ncep.ui.pgen.elements.WatchBox;
import gov.noaa.nws.ncep.ui.pgen.elements.labeledlines.LabeledLine;
import gov.noaa.nws.ncep.ui.pgen.elements.tcm.Tcm;
import gov.noaa.nws.ncep.ui.pgen.filter.AcceptFilter;
import gov.noaa.nws.ncep.ui.pgen.gfa.Gfa;
import gov.noaa.nws.ncep.ui.pgen.gfa.GfaReducePoint;
import gov.noaa.nws.ncep.ui.pgen.gfa.IGfa;
import gov.noaa.nws.ncep.ui.pgen.rsc.PgenResource;
import gov.noaa.nws.ncep.ui.pgen.tca.TCAElement;
import gov.noaa.nws.ncep.viz.common.SnapUtil;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
/**
 * Implements input handler for mouse events for the selecting action.
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 04/13        #927        B. Yin      Moved from the PgenSelectingTool class
 * 05/13        #994        J. Wu       Removed "DEL" - make it same as "Ctrl+X"
 * 07/13        ?           J. Wu       Set the "otherTextLastUsed for GFA.
 * 09/13        ?           J. Wu       Call buildVortext for GFA when mouse is
 *                                      down since GFA converted from VGF does not
 *                                      have vorText set.
 * 04/14        #1117       J. Wu       Set focus to label/update line type for Contours.
 * 04/2014      TTR867      pswamy      Select-tool-on-center-vertex of circle does not
 *                                      move circle (as in NMAP)
 * 04/21/2014   TTR992      D. Sushon   Contour tool's Label >> Edit option should not close
 *                                      main Contour tool window on Cancel, changing a
 *                                      symbol's label should not change the symbol;
 *                                      Both issues fixed.
 * 05/14        TTR1008     J. Wu       Set "adc" to current contour for PgenContoursTool.
 * 12/14     R5198/TTR1057  J. Wu       Select a label over a line for Contours.
 * 01/15     R5201/TTR1060  J. Wu       Update settings when an element is selected.
 * 05/15     Redmine 7804   S. Russell  Updated handleMouseDownMove()
 * 07/15        R8352       J. Wu       update hide flag for contour symbol.
 * 01/27/2016   R13166      J. Wu       Add symbol only & label only capability.
 * 03/15/2016   R15959      E. Brown    Fixed issue where right click was launching the resource manager
 *                                      while a PGEN attribute dialog was open and/or selecting/editing a
 *                                      PGEN object
 * 06/01/2016   R18387      B. Yin      Open attribute dialog when a sub-object in contour is selected.
 * 06/15/2016   R13559      bkowal      File cleanup. No longer simulate mouse clicks.
 * 06/28/2016   R10233      J. Wu       Pass calling handler to PgenContoursTool.
 * 07/01/2016   R17377      J. Wu       Return control to panning when "SHIFT" is down.
 * 09/23/2019   68970       KSunil      Make sure the symbol text moves with the symbol.
 * 12/19/2019   71072       smanoj      Fixed some run time errors.
 * 02/27/2020   75479       smanoj      Fixed an issue with moving Contour line with multiple labels.
 * 10/30/2020   84101       smanoj      Add "Snap Labels to ContourLine" option on the 
 *                                      Contours Attributes dialog.
 * </pre>
 *
 * @author sgilbert
 */

public class PgenSelectHandler extends InputHandlerDefaultImpl {

    protected AbstractEditor mapEditor;

    protected PgenResource pgenrsc;

    protected AttrDlg attrDlg;

    protected AbstractPgenTool tool;

    private boolean preempt;

    private boolean dontMove = false; // flag to prevent moving when selecting

    /**
     * Attribute dialog for displaying track points info
     */
    TrackExtrapPointInfoDlg trackExtrapPointInfoDlg = null;

    /**
     * Flag if any point of the element is selected.
     */
    protected boolean ptSelected = false;

    /**
     * instance variable to store the pgenType of the selected drawableElement
     */
    String pgenType;

    /**
     * Index of the selected point.
     */
    protected int ptIndex = 0;

    /**
     * ghost element that shows the modified element.
     */
    protected MultiPointElement ghostEl = null;

    /**
     * Color of the ghost element.
     */
    protected Color ghostColor = new java.awt.Color(255, 255, 255);

    /**
     * For single point element, the original location is needed for undo.
     */
    protected Coordinate oldLoc = null;

    protected Coordinate firstDown = null;

    /**
     * the mouse downMove position. Used for crossing the screen border, for
     * single point element.
     */
    protected Coordinate tempLoc = null;

    /**
     * Flag for point moving cross the screen. inOut=0: outside the map bound,
     * inOut=1: inside the map bound..
     */
    protected int inOut = 1;

    public PgenSelectHandler(AbstractPgenTool tool, AbstractEditor mapEditor,
            PgenResource resource, AttrDlg attrDlg) {
        this.mapEditor = mapEditor;
        this.pgenrsc = resource;
        this.attrDlg = attrDlg;
        this.tool = tool;
    }

    @Override
    public boolean handleMouseDown(int anX, int aY, int button) {

        if (!tool.isResourceEditable() || !tool.isResourceVisible()) {
            return false;
        }
        // Check if mouse is in geographic extent
        Coordinate loc = mapEditor.translateClick(anX, aY);
        if (loc == null || shiftDown) {
            return false;
        }
        preempt = false;

        if (attrDlg != null && attrDlg instanceof ContoursAttrDlg) {
            ((ContoursAttrDlg) attrDlg).setLabelFocus();
            if (((ContoursAttrDlg) attrDlg).isShiftDownInContourDialog()) {
                return false;
            }
        }

        if (button == 1) {

            // reset ptSelected flag in case the dialog is closed without
            // right-mouse click.
            if (pgenrsc.getSelectedDE() == null) {
                ptSelected = false;
            }

            // Return if an element or a point has been selected
            if (ptSelected || pgenrsc.getSelectedDE() != null) {
                dontMove = false;
                preempt = true;

                if (pgenrsc.getSelectedDE() instanceof SinglePointElement
                        && pgenrsc.getDistance(pgenrsc.getSelectedDE(),
                                loc) > pgenrsc.getMaxDistToSelect()) {
                    ptSelected = false; // prevent SPE from moving when
                                        // selecting it and then click far away
                                        // and hold to move.
                }

                if (!(pgenrsc.getSelectedDE() instanceof SinglePointElement)
                        && !ptSelected) {
                    firstDown = loc;
                }

                return false;
            }

            /*
             * Get the nearest element and set it as the selected element. For
             * contour lines, if a line and a label are both "close" to the
             * click point (the difference is within MaxDistToSelect/5), the
             * label will be selected first.
             */
            DrawableElement elSelected = pgenrsc.getNearestElement(loc);

            if (elSelected instanceof SinglePointElement) {
                ptSelected = true; // prevent map from moving when holding and
                                   // dragging too fast.
            }

            AbstractDrawableComponent adc = null;
            if (elSelected != null && elSelected.getParent() != null
                    && !elSelected.getParent().getName().equals("Default")) {

                adc = pgenrsc.getNearestComponent(loc, new AcceptFilter(),
                        true);
            }

            if (elSelected == null) {
                return false;
            }
            preempt = true;

            /*
             * Get the PGEN type category and bring up the attribute dialog
             */
            String pgCategory = null;

            if (elSelected instanceof TCAElement) {
                PgenUtil.loadTCATool(elSelected);
            } else if (elSelected instanceof WatchBox) {
                WatchBoxAttrDlg
                        .getInstance(PlatformUI.getWorkbench()
                                .getActiveWorkbenchWindow().getShell())
                        .setWatchBox((WatchBox) elSelected);
                PgenUtil.loadWatchBoxModifyTool(elSelected);
            } else if (elSelected instanceof Tcm) {
                PgenUtil.loadTcmTool(elSelected);
            }
            /*
             * Select from within a given Contours or within the PgenResource
             */
            if (tool instanceof PgenContoursTool) {

                DECollection dec = ((PgenContoursTool) tool)
                        .getCurrentContour();
                if (dec != null) {
                    elSelected = pgenrsc.getNearestElement(loc, (Contours) dec);

                    /*
                     * If a contour line is selected, check if a label is also
                     * close to the click point.
                     */
                    if (elSelected instanceof Line
                            && elSelected.getParent() != null
                            && elSelected.getParent() instanceof ContourLine) {
                        elSelected = pgenrsc.getNearestElement(loc,
                                (ContourLine) elSelected.getParent(),
                                elSelected);
                    }

                    if (elSelected instanceof MultiPointElement) {
                        // ptSelected could be set to true if there is a
                        // SiglePointElement near the click, which is not part
                        // of the contour.
                        ptSelected = false;
                    }

                    adc = dec;
                }

                pgCategory = "MET";
                pgenType = "Contours";

            } else {

                if (adc != null && adc instanceof Jet
                        && tool instanceof PgenSelectingTool) {

                    if (adc.getPrimaryDE() == elSelected) {

                        pgCategory = adc.getPgenCategory();
                        pgenType = adc.getPgenType();
                        ((PgenSelectingTool) tool).setJet((Jet) adc);
                    } else {
                        pgCategory = elSelected.getPgenCategory();
                        pgenType = elSelected.getPgenType();
                        if (((Jet) adc).getSnapTool() == null) {
                            ((Jet) adc).setSnapTool(new PgenSnapJet(
                                    pgenrsc.getDescriptor(), mapEditor, null));
                        }
                    }
                } else if (adc != null && adc instanceof LabeledLine
                        && (elSelected.getParent() == adc
                                || elSelected.getParent().getParent() == adc)) {
                    PgenUtil.loadLabeledLineModifyTool((LabeledLine) adc);
                    pgenrsc.removeSelected();

                    Iterator<DrawableElement> it = adc.createDEIterator();
                    while (it.hasNext()) {
                        pgenrsc.addSelected(it.next());
                    }

                    elSelected = null;

                } else if (adc != null
                        && adc.getName().equalsIgnoreCase("Contours")) {
                    pgCategory = adc.getPgenCategory();
                    pgenType = adc.getPgenType();

                    /*
                     * set "dontMove" flag to be retrieved in contour tool.
                     */
                    if (elSelected != null) {
                        dontMove = true;
                    }

                    PgenUtil.loadContoursTool((Contours) adc, this);

                } else if (adc != null && elSelected instanceof Line
                        && adc instanceof Outlook) {
                    pgenType = "Outlook";
                    pgCategory = adc.getPgenCategory();
                } else {
                    if (elSelected != null) {
                        pgCategory = elSelected.getPgenCategory();
                        pgenType = elSelected.getPgenType();
                    }
                }
            }

            if (elSelected != null) {
                pgenrsc.setSelected(elSelected);
                dontMove = true;
            }

            if (pgCategory != null) {

                if (attrDlg != null && !(attrDlg instanceof ContoursAttrDlg
                        && tool instanceof PgenContoursTool)) {
                    closeAttrDlg(attrDlg, elSelected.getPgenType());
                    attrDlg = null;
                }

                if (attrDlg == null) {
                    attrDlg = AttrDlgFactory.createAttrDlg(pgCategory, pgenType,
                            PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                                    .getShell());
                }

                if (attrDlg == null) {
                    mapEditor.refresh();
                    return false;
                }

                attrDlg.setBlockOnOpen(false);

                attrDlg.setMouseHandlerName("Pgen Select");
                attrDlg.setDrawableElement(elSelected);

                if (attrDlg != null && attrDlg.getShell() == null) {
                    attrDlg.open();
                    if (tool instanceof AbstractPgenDrawingTool) {
                        ((AbstractPgenDrawingTool) tool).setAttrDlg(attrDlg);
                    }
                }
                mapEditor.setFocus(); // archana

                if (adc != null && adc.getName().equalsIgnoreCase("Contours")) {

                    ((ContoursAttrDlg) attrDlg).setAttrForDlg((Contours) adc);

                    if (elSelected != null) {
                        updateContoursAttrDlg(elSelected);
                        ((ContoursAttrDlg) attrDlg).setSelectMode();
                        // Update the settings.
                        ((ContoursAttrDlg) attrDlg)
                                .setSettings(elSelected.copy());

                        if (elSelected instanceof Arc) {
                            ((ContoursAttrDlg) attrDlg).openCircleAttrDlg();
                        } else if (elSelected instanceof Line) {
                            ((ContoursAttrDlg) attrDlg).openLineAttrDlg();
                        } else if (elSelected instanceof Symbol) {
                            ((ContoursAttrDlg) attrDlg).openSymbolAttrDlg();
                        } else if (elSelected instanceof Text) {
                            ((ContoursAttrDlg) attrDlg).openLabelAttrDlg();
                        }

                    }

                } else {
                    attrDlg.setAttrForDlg(elSelected);
                    // Update the settings.
                    AttrSettings.getInstance().setSettings(elSelected);
                }

                if (elSelected instanceof SinglePointElement
                        && !(elSelected.getParent() instanceof ContourMinmax)) {

                    if ((elSelected instanceof Symbol)
                            || (elSelected instanceof ComboSymbol)) {

                        SymbolAttrDlg sDlg = (SymbolAttrDlg) attrDlg;

                        sDlg.setLongitude(((SinglePointElement) elSelected)
                                .getLocation().x);
                        sDlg.setLatitude(((SinglePointElement) elSelected)
                                .getLocation().y);

                        if (attrDlg instanceof LabeledSymbolAttrDlg) {
                            ((LabeledSymbolAttrDlg) attrDlg)
                                    .setLabelChkBox(false);
                        }

                    }
                } else if (elSelected instanceof Track) {
                    TrackAttrDlg trackAttrDlg = (TrackAttrDlg) attrDlg;
                    trackAttrDlg.isNewTrack = false;
                    trackAttrDlg.initializeTrackAttrDlg((Track) elSelected);
                    displayTrackExtrapPointInfoDlg(trackAttrDlg,
                            (Track) elSelected);
                }

                attrDlg.enableButtons();
                attrDlg.setPgenCategory(pgCategory);

                if (adc != null && adc instanceof Contours) {
                    attrDlg.setPgenType(pgenType);
                } else {
                    attrDlg.setPgenType(elSelected.getPgenType());
                }

                attrDlg.setDrawingLayer(pgenrsc);
                attrDlg.setMapEditor(mapEditor);
                if (attrDlg instanceof JetAttrDlg
                        && tool instanceof PgenSelectingTool) {
                    ((JetAttrDlg) attrDlg)
                            .setJetDrawingTool((PgenSelectingTool) tool);
                    ((JetAttrDlg) attrDlg).updateSegmentPane();
                    if (((PgenSelectingTool) tool).getJet()
                            .getSnapTool() == null) {
                        ((PgenSelectingTool) tool).getJet()
                                .setSnapTool(new PgenSnapJet(
                                        pgenrsc.getDescriptor(), mapEditor,
                                        (JetAttrDlg) attrDlg));
                    }
                } else if (adc != null && attrDlg instanceof OutlookAttrDlg) {
                    ((OutlookAttrDlg) attrDlg)
                            .setOtlkType(((Outlook) adc).getOutlookType());
                    String lbl = null;
                    Iterator<DrawableElement> it = elSelected.getParent()
                            .createDEIterator();
                    while (it.hasNext()) {
                        DrawableElement de = it.next();
                        if (de instanceof Text) {
                            lbl = ((Text) de).getText()[0];
                            break;
                        }
                    }
                    if (lbl != null) {
                        ((OutlookAttrDlg) attrDlg).setLabel(lbl);

                    }
                    attrDlg.setAttrForDlg(elSelected);
                    ((OutlookAttrDlg) attrDlg).setAttrForDlg(((Outlook) adc));

                } else if (attrDlg instanceof GfaAttrDlg) {
                    ((GfaAttrDlg) attrDlg).setOtherTextLastUsed(
                            elSelected.getForecastHours());
                    ((GfaAttrDlg) attrDlg).redrawHazardSpecificPanel();
                    if (((Gfa) elSelected).getGfaVorText() == null
                            || ((Gfa) elSelected).getGfaVorText()
                                    .length() < 1) {
                        ((Gfa) elSelected).setGfaVorText(
                                Gfa.buildVorText((Gfa) elSelected));
                    }
                    attrDlg.setAttrForDlg(elSelected);
                    ((GfaAttrDlg) attrDlg).enableMoveTextBtn(true);
                }
            }

            mapEditor.setFocus();
            mapEditor.refresh();

            return preempt;

        } else {
            // Mousedown that's not button 1

            return false;

        }

    }

    @Override
    public boolean handleMouseDownMove(int x, int y, int button) {

        if (!tool.isResourceEditable() || !tool.isResourceVisible()) {
            return false;
        }

        if (shiftDown) {
            return false;
        }
        if (dontMove && pgenrsc.getSelectedDE() != null) {
            return true;
        }

        if (attrDlg != null && attrDlg instanceof ContoursAttrDlg) {
            ((ContoursAttrDlg) attrDlg).setLabelFocus();
            if (((ContoursAttrDlg) attrDlg).isShiftDownInContourDialog()) {
                return false;
            }
        }

        // Check if mouse is in geographic extent
        Coordinate loc = mapEditor.translateClick(x, y);

        DrawableElement tmpEl = pgenrsc.getSelectedDE();
        if (PgenUtil.isUnmovable(tmpEl)) {
            return false;
        }

        if (loc != null) {
            tempLoc = loc;
        }

        // Redmine 7804
        boolean drawElmSelected = false;
        if (firstDown == null) {
            firstDown = loc;
        }

        if (loc != null && inOut == 1) {

            // Redmine 7804 - is user's intent selection or panning?
            // return false to send control to the panning handlers, if user's
            // actions indicate they are panning and not trying to manipulate
            // a selected drawable element
            if (tmpEl instanceof SinglePointElement) {
                if (pgenrsc.getDistance(tmpEl, firstDown) > pgenrsc
                        .getMaxDistToSelect()) {
                    return false;
                } else if (pgenrsc.getDistance(tmpEl, firstDown) < pgenrsc
                        .getMaxDistToSelect()) {
                    firstDown = loc;
                }
            } else { // Multipoint Element
                if (pgenrsc.getDistance(tmpEl, firstDown) < pgenrsc
                        .getMaxDistToSelect()) {
                    drawElmSelected = true;
                }
                if (!drawElmSelected) {
                    return false;
                }
            }

        } else if (loc != null && inOut == 0) {
            inOut = 1;
        } else {
            if (inOut != 0) {
                inOut = 0;
            }

            return (tmpEl != null);
        }

        if (tmpEl != null) {
            if (tmpEl instanceof SinglePointElement) {

                ptSelected = true; // to prevent map shifting when cursor moving
                                   // too fast for single point elements.

                if (oldLoc == null) {
                    oldLoc = new Coordinate(
                            ((SinglePointElement) tmpEl).getLocation());
                }

                // get current layer and search the tmpEl
                if (tmpEl instanceof Jet.JetBarb) {
                    ((Jet.JetBarb) tmpEl).setLocationOnly(loc);
                    Jet.JetText jt = ((Jet.JetBarb) tmpEl).getFlightLvl();
                    if (jt != null) {
                        pgenrsc.resetElement(jt);
                    }
                } else if (tmpEl.getParent() != null
                        && tmpEl.getParent().getParent() != null
                        && tmpEl.getParent().getParent().getName()
                                .equalsIgnoreCase("Contours")) {

                    pgenrsc.resetADC(tmpEl.getParent()); // reset display of the
                                                         // DECollecion.

                    ((SinglePointElement) tmpEl).setLocationOnly(loc);
                    ContoursAttrDlg cdlg = (ContoursAttrDlg) attrDlg;

                    /*
                     * We need the following (While "moving" a Symbol), to get
                     * the label to move along with the Symbol.
                     */

                    if (tmpEl.getParent() instanceof ContourMinmax) {

                        ContourMinmax cntrMinMax = (ContourMinmax) tmpEl
                                .getParent();
                        if (cntrMinMax.getLabel() != null) {
                            Coordinate symbolPos = ((SinglePointElement) tmpEl)
                                    .getLocation();
                            Coordinate labelTextpos = new Coordinate(
                                    symbolPos.x, symbolPos.y
                                            + ContourMinmax.LABEL_TEXT_YOFFSET);
                            cntrMinMax.getLabel().setLocation(labelTextpos);
                        }
                    }

                    if (tmpEl instanceof Text) {
                        ((Text) tmpEl)
                                .setText(new String[] { cdlg.getLabel() });
                        ((Text) tmpEl).setAuto(false);
                    }

                } else if (tmpEl instanceof Text
                        && tmpEl.getParent() instanceof DECollection
                        && tmpEl.getParent().getPgenCategory() != null
                        && tmpEl.getParent().getPgenCategory()
                                .equalsIgnoreCase("Front")) {

                    String[] text = ((IText) attrDlg).getString();

                    // add "[" or "]" to front labels. The rule: If the label is
                    // only number and in one line, will be surrounded by [,].
                    if (text.length == 1) {
                        StringBuffer lbl = new StringBuffer(
                                ((TextAttrDlg) attrDlg).getString()[0]);

                        if (lbl.length() > 0) {
                            if (lbl.charAt(0) == '[') {
                                lbl.deleteCharAt(0);
                            }
                            if (lbl.charAt(lbl.length() - 1) == ']') {
                                lbl.deleteCharAt(lbl.length() - 1);
                            }
                            try {
                                Integer.parseInt(lbl.toString());
                                // check if the text is right or left of the
                                // front
                                if (PgenTextDrawingTool.rightOfLine(mapEditor,
                                        loc, (Line) tmpEl.getParent()
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

                    ((Text) tmpEl).setText(((TextAttrDlg) attrDlg).getString());
                    ((SinglePointElement) tmpEl).setLocationOnly(loc);
                }

                else {
                    ((SinglePointElement) tmpEl).setLocationOnly(loc);
                }

                pgenrsc.resetElement(tmpEl); // reset display of this element

                mapEditor.refresh();

            } else {
                if (ptSelected) {

                    if ((ghostEl instanceof Arc) && (ptIndex == 0)) {

                        double[] centerOldLoc = mapEditor.translateInverseClick(
                                ghostEl.getPoints().get(0));

                        double deltaX = (x - centerOldLoc[0]);
                        double deltaY = (y - centerOldLoc[1]);

                        double[] circferOldLoc = mapEditor
                                .translateInverseClick(
                                        ghostEl.getPoints().get(1));

                        Coordinate newLoc = mapEditor.translateClick(
                                (circferOldLoc[0] + deltaX),
                                (circferOldLoc[1] + deltaY));

                        // Replace the selected point and repaint.
                        ghostEl.getPoints().set(ptIndex, loc);
                        ghostEl.getPoints().set(ptIndex + 1, newLoc);

                    } else {
                        ghostEl.getPoints().set(ptIndex, loc);
                    }

                    if (ghostEl instanceof Gfa
                            && !((Gfa) ghostEl).isSnapshot()) {
                        ((GfaAttrDlg) attrDlg).setEnableStatesButton(true);
                    }
                    pgenrsc.setGhostLine(ghostEl);
                    mapEditor.refresh();

                } else if (tmpEl != null
                        && (tmpEl instanceof MultiPointElement)) {

                    // Select the nearest point and create the ghost element.
                    ghostEl = (MultiPointElement) tmpEl.copy();

                    if (ghostEl != null) {
                        ghostEl.setColors(new Color[] { ghostColor,
                                new java.awt.Color(255, 255, 255) });

                        ArrayList<Coordinate> points = new ArrayList<Coordinate>();
                        points.addAll(tmpEl.getPoints());

                        ghostEl.setPoints(points);

                        ghostEl.setPgenCategory(tmpEl.getPgenCategory());
                        ghostEl.setPgenType(tmpEl.getPgenType());

                        ptIndex = getNearestPtIndex(ghostEl, loc);
                        // mapEditor
                        double[] locScreen = mapEditor
                                .translateInverseClick(loc);
                        double[] pt = mapEditor.translateInverseClick(
                                (ghostEl.getPoints().get(ptIndex)));

                        Point ptScreen = new GeometryFactory()
                                .createPoint(new Coordinate(pt[0], pt[1]));
                        double dist = ptScreen.distance(new GeometryFactory()
                                .createPoint(new Coordinate(locScreen[0],
                                        locScreen[1])));
                        dist = 0;
                        if (dist <= pgenrsc.getMaxDistToSelect()) {
                            ghostEl.setPoints(points);

                            setGhostLineColorForTrack(ghostEl, ptIndex);

                            ptSelected = true;
                        }

                    }
                }

                return true;
            }
        }

        return preempt;

    }

    @Override
    public boolean handleMouseUp(int x, int y, int button) {
        firstDown = null;

        // This variable, returnValue, is used to make sure that the right mouse
        // button event stops in this handler instead of continuing on to
        // NCPerspective's handleMouseUp and opening the resource manager. Near
        // the bottom of the this method, if the attribute dialog is being
        // closed by the right mouse button click returnValue is set to true so
        // that the mouse button click never reaches the NCPer handler
        boolean preempt = false;

        if (!tool.isResourceEditable() || !tool.isResourceVisible()) {
            return false;
        }

        if (attrDlg != null && attrDlg instanceof ContoursAttrDlg) {
            ((ContoursAttrDlg) attrDlg).setLabelFocus();
            if (((ContoursAttrDlg) attrDlg).isShiftDownInContourDialog()) {
                return false;
            }
        }

        // Finish the editing
        if (button == 1 && pgenrsc != null) {

            // Create a copy of the currently selected element
            DrawableElement el = pgenrsc.getSelectedDE();

            if (el != null) {

                DrawableElement newEl = (DrawableElement) el.copy();

                if (el instanceof SinglePointElement) {

                    if (oldLoc != null) {

                        pgenrsc.resetElement(el); // reset display of this
                                                  // element

                        if (el instanceof Jet.JetBarb) {
                            DECollection dec = (DECollection) el.getParent();
                            if (dec != null && dec.getCollectionName()
                                    .equalsIgnoreCase("WindInfo")) {
                                DECollection parent = (DECollection) dec
                                        .getParent();
                                if (parent != null && parent.getCollectionName()
                                        .equalsIgnoreCase("jet")) {
                                    Jet oldJet = (Jet) parent;
                                    Jet newJet = oldJet.copy();

                                    // to make undo work, replace the whole jet
                                    // and replace the barb in the collection
                                    DECollection newWind = dec.copy();
                                    newJet.replace(
                                            newJet.getNearestComponent(
                                                    ((SinglePointElement) el)
                                                            .getLocation()),
                                            newWind);
                                    pgenrsc.replaceElement(oldJet, newJet);

                                    newWind.replace(
                                            newWind.getNearestComponent(
                                                    ((SinglePointElement) el)
                                                            .getLocation()),
                                            newEl);

                                    // set the old windinfo to the original
                                    // location
                                    Iterator<DrawableElement> it = dec
                                            .createDEIterator();
                                    while (it.hasNext()) {
                                        DrawableElement de = it.next();
                                        if (de instanceof SinglePointElement) {
                                            ((SinglePointElement) de)
                                                    .setLocationOnly(oldLoc);
                                        }
                                    }
                                    oldLoc = null;
                                }
                            }
                        } else if (el.getParent() != null
                                && el.getParent()
                                        .getParent() instanceof Contours
                                && !(el.getParent()
                                        .getParent() instanceof Outlook)) {

                            pgenrsc.resetADC(el.getParent());

                            AbstractDrawableComponent oldAdc = el.getParent();
                            Contours oldContours = (Contours) oldAdc
                                    .getParent();

                            if (oldContours != null) {

                                Contours newContours = oldContours.copy();
                                AbstractDrawableComponent newAdc = oldAdc
                                        .copy();

                                newContours.replace(
                                        newContours.getNearestComponent(
                                                ((SinglePointElement) el)
                                                        .getLocation()),
                                        newAdc);
                                ((DECollection) newAdc).replace(
                                        ((DECollection) newAdc)
                                                .getNearestComponent(
                                                        ((SinglePointElement) el)
                                                                .getLocation()),
                                        newEl);

                                if (newEl instanceof Text) {
                                    ((Text) newEl).setAuto(false);
                                }

                                pgenrsc.replaceElement(oldContours,
                                        newContours);

                                if (tool instanceof PgenContoursTool) {
                                    ((PgenContoursTool) tool)
                                            .setCurrentContour(newContours);
                                }

                                Iterator<DrawableElement> it = oldAdc
                                        .createDEIterator();
                                while (it.hasNext()) {
                                    DrawableElement de = it.next();
                                    if (de.equals(el)) {
                                        ((SinglePointElement) de)
                                                .setLocationOnly(oldLoc);
                                    }
                                }

                                oldLoc = null;

                            }

                        } else {

                            pgenrsc.replaceElement(el, newEl);
                            ((SinglePointElement) el).setLocationOnly(oldLoc);

                            oldLoc = null;

                            if (attrDlg instanceof SymbolAttrDlg) {

                                ((SymbolAttrDlg) attrDlg).setLatitude(
                                        ((SinglePointElement) newEl)
                                                .getLocation().y);
                                ((SymbolAttrDlg) attrDlg).setLongitude(
                                        ((SinglePointElement) newEl)
                                                .getLocation().x);

                            }
                        }

                        Coordinate loc = mapEditor.translateClick(x, y);
                        if (loc != null) {
                            ((SinglePointElement) newEl).setLocation(loc);
                        } else {
                            ((SinglePointElement) newEl).setLocation(tempLoc);
                        }
                        pgenrsc.setSelected(newEl);
                    }

                } else if (ptSelected) {

                    ptSelected = false;

                    if (tool instanceof PgenSelectingTool
                            && el instanceof Jet.JetLine
                            && ((PgenSelectingTool) tool).getJet()
                                    .getPrimaryDE() == el) {
                        Jet newJet = ((PgenSelectingTool) tool).getJet().copy();
                        pgenrsc.replaceElement(
                                ((PgenSelectingTool) tool).getJet(), newJet);
                        newJet.getPrimaryDE().setPoints(ghostEl.getPoints());
                        ((PgenSelectingTool) tool).setJet(newJet);
                        pgenrsc.setSelected(newJet.getPrimaryDE());

                    } else if (el.getParent() instanceof ContourLine
                            || el.getParent() instanceof ContourCircle) {
                        if (ghostEl !=null) {
                            editContoursLineNCircle(el, ghostEl.getPoints());
                        }
                    } else {

                        /*
                         * Replace the selected element with this new element
                         * Note: for GFA, check if the new point will make the
                         * GFA polygon invalid - J. Wu.
                         */
                        if (!(newEl instanceof Gfa) || (newEl instanceof Gfa
                                && ((Gfa) ghostEl).isValid())) {

                            pgenrsc.replaceElement(el, newEl);

                            // Update the new Element with the new points
                            if ("GFA".equalsIgnoreCase(newEl.getPgenType())
                                    && ((IGfa) attrDlg).getGfaFcstHr()
                                            .indexOf("-") > -1) {
                                ArrayList<Coordinate> points = ghostEl
                                        .getPoints();
                                int nearest = getNearestPtIndex(ghostEl,
                                        mapEditor.translateClick(x, y));
                                Coordinate toSnap = ghostEl.getPoints()
                                        .get(nearest);
                                List<Coordinate> tempList = new ArrayList<Coordinate>();
                                tempList.add(toSnap);
                                tempList = SnapUtil.getSnapWithStation(tempList,
                                        SnapUtil.VOR_STATION_LIST, 10, 16);
                                Coordinate snapped = tempList.get(0);
                                // update the coordinate
                                points.get(nearest).setCoordinate(snapped);
                                newEl.setPoints(points);
                            } else {
                                newEl.setPoints(ghostEl.getPoints());
                            }

                            // /update Gfa vor text
                            if (newEl instanceof Gfa) {
                                GfaReducePoint
                                        .WarningForOverThreeLines((Gfa) newEl);
                                ((Gfa) newEl).setGfaVorText(
                                        Gfa.buildVorText((Gfa) newEl));
                                if (attrDlg instanceof GfaAttrDlg) {
                                    ((GfaAttrDlg) attrDlg).setVorText(
                                            ((Gfa) newEl).getGfaVorText());
                                }

                            }

                            if (attrDlg != null) {
                                attrDlg.setDrawableElement(newEl);
                            }

                            // Set this new element as the currently selected
                            // element Collections do not need to reset.
                            if (!(pgenrsc
                                    .getSelectedComp() instanceof DECollection)) {
                                pgenrsc.setSelected(newEl);
                            }

                        }
                    }

                    if (newEl instanceof Track) {
                        if (isModifiedPointOneOfTheLastTwoInitPoint(newEl,
                                ptIndex)) {
                            ((Track) newEl).calculateExtrapTrackPoints();
                        }
                        displayTrackExtrapPointInfoDlg((TrackAttrDlg) attrDlg,
                                (Track) newEl);
                    } else if (newEl instanceof Gfa) {
                        attrDlg.setAttrForDlg(newEl);
                    }

                    pgenrsc.removeGhostLine();

                }

                mapEditor.refresh();
                return true;
            }
        } else if (button == 3) {

            // Close the attribute dialog and do the cleanup.
            if (attrDlg != null) {

                if (attrDlg instanceof ContoursAttrDlg) {
                    if (pgenrsc.getSelectedDE() == null) {
                        closeAttrDlg(attrDlg, pgenType);
                        attrDlg = null;
                        PgenUtil.setSelectingMode();
                    } else {
                        ((ContoursAttrDlg) attrDlg).closeAttrEditDialogs();
                    }
                } else {
                    closeAttrDlg(attrDlg, pgenType);
                    if (attrDlg instanceof GfaAttrDlg) {
                        // re-set event trigger
                        PgenUtil.setSelectingMode();
                    }
                    attrDlg = null;
                }

                // Mouseup event on button 3, the attribute dialog just got
                // closed, and the PGEN object was just deselected, so
                // preempt the event so the resource manager doesn't open
                preempt = true;
            }

            if (trackExtrapPointInfoDlg != null) {
                trackExtrapPointInfoDlg.close();
            }
            trackExtrapPointInfoDlg = null;

            pgenrsc.removeGhostLine();
            ptSelected = false;
            pgenrsc.removeSelected();
            mapEditor.refresh();

        }

        // Use returnValue to either preempt the event or not. If this button 3
        // up event just closed the attribute dialog and/or deselected the PGEN
        // object, the returnValue will be true, preempting the event
        return preempt;
    }

    private void setGhostLineColorForTrack(MultiPointElement multiPointElement,
            int nearestPointIndex) {
        /*
         * If multiPointElement is not a type of Track, do nothing
         */
        if (multiPointElement == null
                || !(multiPointElement instanceof Track)) {
            return;
        }

        Track track = (Track) multiPointElement;
        int initialTrackPointSize = 0;
        if (track.getInitialPoints() != null) {
            initialTrackPointSize = track.getInitialPoints().length;
        }
        if (isInitialPointSelected(initialTrackPointSize, nearestPointIndex)) {
            track.setInitialColor(new java.awt.Color(255, 255, 255));
        } else {
            track.setExtrapColor(new java.awt.Color(255, 255, 255));
        }
    }

    private boolean isInitialPointSelected(int initialPointSize,
            int nearestPointIndex) {
        if (nearestPointIndex < initialPointSize) {
            return true;
        }
        return false;
    }

    private boolean isModifiedPointOneOfTheLastTwoInitPoint(
            DrawableElement drawableElement, int nearestPointIndex) {
        boolean isOneOfTheLastTwoInitPoint = false;
        /*
         * If multiPointElement is not a type of Track, return false
         */
        if (drawableElement == null || !(drawableElement instanceof Track)) {
            return isOneOfTheLastTwoInitPoint;
        }

        Track track = (Track) drawableElement;
        int initialTrackPointSize = 0;
        if (track.getInitialPoints() != null) {
            initialTrackPointSize = track.getInitialPoints().length;
        }
        if (nearestPointIndex == (initialTrackPointSize - 1)
                || nearestPointIndex == (initialTrackPointSize - 2)) {
            isOneOfTheLastTwoInitPoint = true;
        }

        return isOneOfTheLastTwoInitPoint;
    }

    /**
     * Gets the nearest point of an selected element to the input point
     * 
     * @param el
     *            element
     * @param pt
     *            input point
     * @return
     */
    protected int getNearestPtIndex(MultiPointElement el, Coordinate pt) {

        int ptId = 0;
        double minDistance = -1;
        GeodeticCalculator gc;
        gc = new GeodeticCalculator(pgenrsc.getCoordinateReferenceSystem());
        gc.setStartingGeographicPoint(pt.x, pt.y);

        int index = 0;
        for (Coordinate elPoint : el.getPoints()) {

            gc.setDestinationGeographicPoint(elPoint.x, elPoint.y);

            double dist = gc.getOrthodromicDistance();

            if (minDistance < 0 || dist < minDistance) {

                minDistance = dist;
                ptId = index;

            }

            index++;

        }

        return ptId;

    }

    public void closeDlg() {
        if (attrDlg != null) {
            attrDlg.close();
        }
    }

    private void displayTrackExtrapPointInfoDlg(TrackAttrDlg attrDlgObject,
            Track trackObject) {

        if (attrDlgObject == null) {
            return;
        }
        TrackExtrapPointInfoDlg extrapPointInfoDlg = attrDlgObject
                .getTrackExtrapPointInfoDlg();
        if (extrapPointInfoDlg != null) {
            extrapPointInfoDlg.close();
        } else {
            extrapPointInfoDlg = (TrackExtrapPointInfoDlg) AttrDlgFactory
                    .createAttrDlg(Track.TRACK_INFO_DLG_CATEGORY_NAME, pgenType,
                            PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                                    .getShell());
            attrDlgObject.setTrackExtrapPointInfoDlg(extrapPointInfoDlg);
            trackExtrapPointInfoDlg = extrapPointInfoDlg;
        }

        /*
         * Open the dialog and set the default attributes. Note: must call
         * "attrDlg.setBlockOnOpen(false)" first.
         */
        extrapPointInfoDlg.setBlockOnOpen(false);
        extrapPointInfoDlg.open();

        extrapPointInfoDlg.setTrack(trackObject,
                attrDlgObject.getUnitComboSelectedIndex(),
                attrDlgObject.getRoundComboSelectedIndex(),
                attrDlgObject.getRoundDirComboSelectedIndex());

        extrapPointInfoDlg.setBlockOnOpen(true);

    }

    private boolean isTrackElement(DrawableType drawableType) {
        if (drawableType == DrawableType.TRACK) {
            return true;
        }
        return false;
    }

    private DrawableType getDrawableType(String pgenTypeString) {
        if (Track.TRACK_PGEN_TYPE.equalsIgnoreCase(pgenTypeString)) {
            return DrawableType.TRACK;
        }
        return DrawableType.LINE;
    }

    private boolean closeAttrDlg(AttrDlg attrDlgObject, String pgenTypeString) {
        if (attrDlgObject == null) {
            return false;
        }
        if (isTrackElement(getDrawableType(pgenTypeString))) {
            TrackAttrDlg tempTrackAttrDlg = (TrackAttrDlg) attrDlgObject;
            TrackExtrapPointInfoDlg tempTrackExtrapPointInfoDlg = tempTrackAttrDlg
                    .getTrackExtrapPointInfoDlg();
            tempTrackAttrDlg.setTrackExtrapPointInfoDlg(null);
            trackExtrapPointInfoDlg = null;
            closeTrackExtrapPointInfoDlg(tempTrackExtrapPointInfoDlg);
        }
        return attrDlgObject.close();
    }

    private void closeTrackExtrapPointInfoDlg(
            TrackExtrapPointInfoDlg dlgObject) {
        if (dlgObject != null) {
            dlgObject.close();
        }
    }

    /**
     * Edit lines/circle in a Contours.
     *
     * @param el
     *            : the selected DE.
     * @param points
     *            : new locations.
     * @return
     */
    private void editContoursLineNCircle(DrawableElement el,
            ArrayList<Coordinate> points) {

        boolean moveContourLbls = false;
        if (el.getParent() instanceof ContourLine) {
            if (attrDlg != null) {
                if (((ContoursAttrDlg) attrDlg).getToggleSnapLblChecked()) {
                    moveContourLbls = true;
                }
            }
        }

        if (el.getParent() instanceof ContourLine
                || el.getParent() instanceof ContourCircle) {

            Contours oldContours = (Contours) el.getParent().getParent();

            DrawableElement selElem;
            if (el.getParent() instanceof ContourLine) {
                selElem = ((ContourLine) el.getParent()).getLine();
            } else {
                selElem = ((ContourCircle) el.getParent()).getCircle();
            }

            if (oldContours != null) {
                Contours newContours = new Contours();

                Iterator<AbstractDrawableComponent> it = oldContours
                        .getComponentIterator();

                while (it.hasNext()) {

                    AbstractDrawableComponent oldAdc = it.next();
                    AbstractDrawableComponent newAdc = oldAdc.copy();

                    if (oldAdc.equals(el.getParent())) {

                        if (el.getParent() instanceof ContourLine) {
                            ((ContourLine) newAdc).getLine().setPoints(points);
                            List<Text> texts = ((ContourLine) newAdc)
                                    .getLabels();
                            int nlabels = texts.size();
                            selElem = ((ContourLine) newAdc).getLine();

                            //Update labels in the contour line.
                            ContourLine cline = (ContourLine) selElem.getParent();
                            if (nlabels > 0) {

                                if (moveContourLbls) {

                                    Text oldLabel = (Text) (texts.get(0)
                                            .copy());

                                    for (Text lbl : texts) {
                                        cline.removeElement(lbl);
                                    }

                                    for (int ii = 0; ii < nlabels; ii++) {

                                        Text lbl = (Text) (oldLabel.copy());

                                        lbl.setAuto(true);
                                        lbl.setParent(cline);
                                        lbl.setHide(false);

                                        cline.add(lbl);
                                    }
                                }
                            }

                        } else {
                            ((ContourCircle) newAdc).getCircle()
                                    .setPoints(points);
                            // For circles, the circumference text point can be
                            // any of the points except 0
                            ((Text) ((ContourCircle) newAdc).getLabel())
                                    .setLocation(points.get(1));
                            selElem = ((ContourCircle) newAdc).getCircle();
                        }
                    }

                    newAdc.setParent(newContours);
                    newContours.add(newAdc);
                }

                newContours.update(oldContours);
                pgenrsc.replaceElement(oldContours, newContours);
                pgenrsc.setSelected(selElem);
                if (attrDlg != null) {
                    ((ContoursAttrDlg) attrDlg).setCurrentContours(newContours);
                }
                if (newContours != null) {
                    if (tool instanceof PgenContoursTool) {
                        ((PgenContoursTool) tool).setCurrentContour(newContours);
                    }
                }
            }
        }

    }

    /**
     * Update the ContoursAttrDlg if the selected DE is within a Contours.
     * 
     * @param DrawableElement
     *            : the selected DE.
     * @return
     */
    private void updateContoursAttrDlg(DrawableElement elSelected) {

        AbstractDrawableComponent pele = elSelected.getParent();

        if (pele != null && pele.getParent() instanceof Contours) {

            ContoursAttrDlg cdlg = (ContoursAttrDlg) attrDlg;

            if (elSelected instanceof Arc) {
                Text lbl = ((ContourCircle) pele).getLabel();
                if (lbl != null) {
                    cdlg.setLabel(lbl.getText()[0]);
                    cdlg.setNumOfLabels(1);
                    cdlg.setHideCircleLabel(lbl.getHide());
                }
            } else if (elSelected instanceof Line) {

                ArrayList<Text> lbls = ((ContourLine) pele).getLabels();
                if (lbls != null && lbls.size() > 0) {
                    cdlg.setLabel(lbls.get(0).getText()[0]);
                }

                cdlg.setNumOfLabels(((ContourLine) pele).getNumOfLabels());
                cdlg.setClosed(((Line) elSelected).isClosedLine());
                cdlg.setActiveLine(elSelected);
            } else if (elSelected instanceof Symbol) {
                Text lbl = ((ContourMinmax) pele).getLabel();
                cdlg.setMinmaxLabelOnly(false);
                if (lbl != null) {
                    cdlg.setLabel(lbl.getText()[0]);
                    cdlg.setNumOfLabels(1);
                    cdlg.setActiveSymbol(elSelected);
                    cdlg.setHideSymbolLabel(lbl.getHide());
                    cdlg.setMinmaxSymbolOnly(false);
                } else {
                    cdlg.setMinmaxSymbolOnly(true);
                }
            } else if (elSelected instanceof Text) {
                cdlg.setLabel(((Text) elSelected).getText()[0]);

                if (pele instanceof ContourLine) {
                    cdlg.setNumOfLabels(((ContourLine) pele).getNumOfLabels());
                    cdlg.setClosed(
                            ((ContourLine) pele).getLine().isClosedLine());
                    cdlg.setActiveLine(((ContourLine) pele).getLine());
                } else if (pele instanceof ContourMinmax) {
                    cdlg.setNumOfLabels(1);
                    cdlg.setHideSymbolLabel(((Text) elSelected).getHide());
                    cdlg.setMinmaxSymbolOnly(false);
                    if (((ContourMinmax) pele).getSymbol() != null) {
                        cdlg.setActiveSymbol(
                                ((ContourMinmax) pele).getSymbol());
                        cdlg.setMinmaxLabelOnly(false);
                    } else {
                        cdlg.setMinmaxLabelOnly(true);
                    }
                } else if (pele instanceof ContourCircle) {
                    cdlg.setNumOfLabels(1);
                    cdlg.setHideCircleLabel(((Text) elSelected).getHide());
                }
            }
        }
    }

    public AbstractEditor getMapEditor() {
        return mapEditor;
    }

    public PgenResource getPgenrsc() {
        return pgenrsc;
    }

    /**
     * @return the preempt
     */
    public boolean isPreempt() {
        return preempt;
    }

    /**
     * @param preempt
     *            the preempt to set
     */
    public void setPreempt(boolean preempt) {
        this.preempt = preempt;
    }

    /**
     * @return the dontMove
     */
    public boolean isDontMove() {
        return dontMove;
    }

    /**
     * @param dontMove
     *            the dontMove to set
     */
    public void setDontMove(boolean dontMove) {
        this.dontMove = dontMove;
    }
}
