/*
 * gov.noaa.nws.ncep.ui.pgen.tools.PgenDeleteElementHandler
 *
 * 1 April 2013
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.ui.pgen.tools;

import com.raytheon.viz.ui.editor.AbstractEditor;
import com.vividsolutions.jts.geom.Coordinate;

import gov.noaa.nws.ncep.ui.pgen.PgenSession;
import gov.noaa.nws.ncep.ui.pgen.PgenUtil;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.AttrDlg;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.ContoursAttrDlg;
import gov.noaa.nws.ncep.ui.pgen.contours.ContourLine;
import gov.noaa.nws.ncep.ui.pgen.contours.Contours;
import gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent;
import gov.noaa.nws.ncep.ui.pgen.elements.DECollection;
import gov.noaa.nws.ncep.ui.pgen.elements.Layer;
import gov.noaa.nws.ncep.ui.pgen.elements.Outlook;
import gov.noaa.nws.ncep.ui.pgen.elements.Text;
import gov.noaa.nws.ncep.ui.pgen.filter.AcceptFilter;
import gov.noaa.nws.ncep.ui.pgen.rsc.PgenResource;

/**
 * Implements input handler for mouse events for the deleting action.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * 04/13         927      B. Yin    Moved from the PgenDeleteElement class
 * 04/14         1117     J. Wu     Added confirmation for deleting contours
 * 07/15         8352     J. Wu     "Delete" contours components, not whole
 *                                  contours.
 * Jan 13, 2020  71072    smanoj    "Delete" for multi-select.
 * Feb 20, 2020  74901    smanoj    Removed confirmation for deleting contours.
 *                                  Undo functionality issues are also fixed.
 * Apr 06, 2020  77420    tjensen   Allow delete of specific contour labels
 *
 * </pre>
 *
 * @author bingfan
 */

public class PgenDeleteElementHandler extends InputHandlerDefaultImpl {

    protected AbstractEditor mapEditor;

    protected PgenResource pgenrsc;

    protected AbstractPgenTool tool;

    protected AttrDlg attrDlg;

    /**
     * Constructor
     *
     * @param tool
     */
    public PgenDeleteElementHandler(AbstractPgenTool tool) {
        this.tool = tool;
        pgenrsc = tool.getDrawingLayer();
        mapEditor = tool.mapEditor;

        if (tool instanceof AbstractPgenDrawingTool) {
            attrDlg = ((AbstractPgenDrawingTool) tool).getAttrDlg();
        }
    }

    @Override
    public boolean handleMouseDown(int anX, int aY, int button) {
        if (!tool.isResourceEditable()) {
            return false;
        }

        boolean preempt = false;

        // Check if mouse is in geographic extent
        Coordinate loc = mapEditor.translateClick(anX, aY);
        if (loc == null) {
            return false;
        }

        if (button == 1) {
            if (pgenrsc.getSelectedComp() != null) {
                doDelete();
            } else {
                // Get the nearest element and set it as the selected element.
                AbstractDrawableComponent elSelected = pgenrsc
                        .getNearestComponent(loc, new AcceptFilter(), true);

                // Delete watch status line
                if (elSelected instanceof DECollection
                        && elSelected.getName().equalsIgnoreCase("Watch")
                        && pgenrsc.getNearestElement(loc).getPgenType()
                                .equalsIgnoreCase("POINTED_ARROW")) {
                    elSelected = pgenrsc.getNearestElement(loc);
                } else if ((elSelected instanceof Outlook
                        && ((Outlook) elSelected).getDEs() > 1)) {
                    AbstractDrawableComponent adc = pgenrsc
                            .getNearestElement(loc);
                    elSelected = adc.getParent();
                } else if (elSelected instanceof Contours) {
                    AbstractDrawableComponent adc = pgenrsc
                            .getNearestElement(loc);
                    if (adc instanceof Text) {
                        elSelected = adc;
                    } else {
                        elSelected = adc.getParent();
                    }
                }

                if (elSelected != null) {
                    pgenrsc.setSelected(elSelected);
                    preempt = true;
                }
                mapEditor.refresh();
            }
        } else if (button == 2) {
            preempt = true;
        } else if (button == 3) {
            if (pgenrsc.getSelectedComp() != null) {
                // de-select element
                pgenrsc.removeSelected();
                mapEditor.refresh();
            } else {
                // set selecting mode
                PgenUtil.setSelectingMode();
            }

            preempt = true;
        } else {
            preempt = true;
        }

        return preempt;

    }

    @Override
    public boolean handleMouseDownMove(int x, int y, int mouseButton) {
        return !(!tool.isResourceEditable() || shiftDown);
    }

    /**
     * Deletes the selected element and reset the handler. For a single element,
     * closes the attributes dialog when the element is deleted.
     */
    @Override
    public void preprocess() {

        if (pgenrsc.getSelectedComp() != null) {

            if (attrDlg != null
                    && (pgenrsc.getSelectedComp().getParent() instanceof Layer
                            || pgenrsc.getSelectedComp().getParent().getName()
                                    .equalsIgnoreCase("labeledSymbol"))) {
                attrDlg.close();
            }

            doDelete();
            tool.resetMouseHandler();
        }
    }

    /**
     * Deletes the selected element or component from the PGEN resource.
     */
    private void doDelete() {
        AbstractDrawableComponent adc = pgenrsc.getSelectedComp();

        if (adc.getParent() instanceof ContourLine && adc instanceof Text) {
            removeContourLineLabel((Text) adc);
        } else {
            pgenrsc.deleteSelectedElements();
        }
        mapEditor.refresh();
    }

    public AbstractEditor getMapEditor() {
        return mapEditor;
    }

    public PgenResource getPgenrsc() {
        return pgenrsc;
    }

    private void removeContourLineLabel(Text label) {

        ContourLine cline = (ContourLine) label.getParent();
        Contours myContour = (Contours) cline.getParent();
        // Make a copy of the ContourLine and update the labels.
        int nlabels = cline.getNumOfLabels() - 1;
        cline.setNumOfLabels(nlabels);

        pgenrsc.removeSelected(label);
        pgenrsc.removeElement(label);

        AbstractPgenTool thisTool = PgenSession.getInstance().getPgenTool();
        if (thisTool instanceof AbstractPgenDrawingTool) {
            AttrDlg thisDlg = ((AbstractPgenDrawingTool) thisTool).getAttrDlg();
            if (thisDlg instanceof ContoursAttrDlg
                    && thisTool instanceof PgenContoursTool) {
                PgenContoursTool pgenTool = (PgenContoursTool) thisTool;
                ContoursAttrDlg pgenDlg = (ContoursAttrDlg) thisDlg;

                pgenTool.setCurrentContour(myContour);
                pgenDlg.setDrawableElement(myContour);
                pgenDlg.setLabel(cline.getLabelString()[0]);
                pgenDlg.setNumOfLabels(cline.getNumOfLabels());
                pgenDlg.closeAttrEditDialogs();
            }
        }
        pgenrsc.resetAllElements();
    }
}
