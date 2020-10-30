/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package gov.noaa.nws.ncep.ui.pgen.tools;

import java.util.Iterator;
import java.util.List;
import java.awt.Color;

import com.raytheon.viz.ui.editor.AbstractEditor;
import com.vividsolutions.jts.geom.Coordinate;

import gov.noaa.nws.ncep.ui.pgen.PgenConstant;
import gov.noaa.nws.ncep.ui.pgen.PgenSession;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.AttrDlg;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.ContoursAttrDlg;
import gov.noaa.nws.ncep.ui.pgen.contours.ContourLine;
import gov.noaa.nws.ncep.ui.pgen.contours.Contours;

import gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableElement;
import gov.noaa.nws.ncep.ui.pgen.elements.Text;
import gov.noaa.nws.ncep.ui.pgen.rsc.PgenResource;

import gov.noaa.nws.ncep.ui.pgen.display.IText.DisplayType;
import gov.noaa.nws.ncep.ui.pgen.display.IText.FontStyle;
import gov.noaa.nws.ncep.ui.pgen.display.IText.TextJustification;
import gov.noaa.nws.ncep.ui.pgen.display.IText.TextRotation;

/**
*
* Mouse handler to add a new label to ContourLine.
*
* <pre>
*
* SOFTWARE HISTORY
*
* Date          Ticket#  Engineer  Description
* ------------- -------- --------- -----------------
* Sep 01, 2020   81798    smanoj    Initial creation
* Sep 17, 2020   81798    smanoj    Fixed label count issue when drawing
*                                   more than one contour
* Oct 30, 2020   84101    smanoj    Create a default contour label for the
*                                   case of empty labels in the contour line.
* </pre>
*
* @author smanoj
*/
public class PgenAddNewLabelHandler extends InputHandlerDefaultImpl {

    protected AbstractEditor mapEditor;

    protected PgenResource drawingLayer;

    protected AbstractPgenTool tool;

    public PgenAddNewLabelHandler(AbstractPgenTool tool) {
        this.mapEditor = tool.mapEditor;
        this.drawingLayer = tool.getDrawingLayer();
        this.tool = tool;
    }

    private void addNewLabel(Coordinate loc, Text lbl, ContourLine cline) {
        int nlabels = cline.getNumOfLabels() + 1;
        lbl.setText(lbl.getText());
        lbl.setLocation(loc);
        lbl.setAuto(false);
        cline.setNumOfLabels(nlabels);
        cline.add(lbl);

        PgenSession.getInstance().getPgenResource().issueRefresh();
        PgenSession.getInstance().getPgenResource().resetAllElements();
        AbstractPgenTool thisTool = PgenSession.getInstance().getPgenTool();
        if (thisTool instanceof AbstractPgenDrawingTool) {
            AttrDlg thisDlg = ((AbstractPgenDrawingTool) thisTool).getAttrDlg();
            if (thisDlg instanceof ContoursAttrDlg
                    && thisTool instanceof PgenContoursTool) {
                ContoursAttrDlg pgenDlg = (ContoursAttrDlg) thisDlg;
                pgenDlg.close();
                thisDlg.close();
            }
        }

    }

    @Override
    public boolean handleMouseDown(int anX, int aY, int button) {

        if (!drawingLayer.isEditable())
            return false;

        // Check if mouse is in geographic extent
        Coordinate loc = mapEditor.translateClick(anX, aY);
        if (loc == null || shiftDown)
            return false;

        if (button == 1) {

            // Find selected element
            DrawableElement de = drawingLayer.getSelectedDE();
            if (de != null) {

                // if selected element is ContourLine add new label
                if (de.getParent() instanceof ContourLine) {
                    ContourLine cline = (ContourLine) de.getParent();

                    // Create a default contour label
                    Text lbl = new Text(null, "Courier", 14.0f,
                            TextJustification.CENTER, cline.getPoints().get(0),
                            0.0, TextRotation.SCREEN_RELATIVE,
                            new String[] { "0" }, FontStyle.REGULAR, Color.RED,
                            0, 0, true, DisplayType.NORMAL,
                            PgenConstant.CATEGORY_TEXT,
                            PgenConstant.TYPE_GENERAL_TEXT);

                    List<Text> texts = cline.getLabels();
                    if (!texts.isEmpty()) {
                        lbl = (Text) (texts.get(0).copy());
                    }

                    addNewLabel(loc, lbl, cline);
                }

            }
            return true;

        } else if (button == 3) {
            drawingLayer.removeSelected();
            return true;

        } else {
            return false;
        }
    }

    @Override
    public boolean handleMouseUp(int x, int y, int button) {
        tool.resetMouseHandler();
        return true;
    }

    @Override
    public boolean handleMouseMove(int anX, int aY) {
        return true;
    }

    @Override
    public boolean handleMouseDownMove(int x, int y, int mouseButton) {
        return true;
    }

    public AbstractEditor getMapEditor() {
        return mapEditor;
    }

    public PgenResource getPgenrsc() {
        return drawingLayer;
    }
}