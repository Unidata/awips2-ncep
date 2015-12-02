package gov.noaa.nws.ncep.ui.pgen.tools;

import gov.noaa.nws.ncep.ui.pgen.attrdialog.AttrDlg;
import gov.noaa.nws.ncep.ui.pgen.contours.ContourMinmax;
import gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent;
import gov.noaa.nws.ncep.ui.pgen.elements.DECollection;
import gov.noaa.nws.ncep.ui.pgen.elements.Layer;
import gov.noaa.nws.ncep.ui.pgen.elements.Text;
import gov.noaa.nws.ncep.ui.pgen.rsc.PgenResource;

import java.util.Iterator;

import com.raytheon.viz.ui.editor.AbstractEditor;

/**
 * Implements a modal map tool for PGEN deleting part function for labels of
 * non-met contour symbols only. This is the action hanlder for doing that as
 * registered in the plugin.xml
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 2015 June 09 R 8199      S Russell   Initial Creation.
 * 
 * </pre>
 * 
 * @author Steve Russell
 */

public class PgenDeleteLabelHandler extends InputHandlerDefaultImpl {

    protected AbstractEditor mapEditor;

    protected PgenResource pgenrsc;

    protected AbstractPgenTool tool;

    protected AttrDlg attrDlg;

    /**
     * Constructor
     * 
     * @param AbstractPgenTool
     *            tool
     */
    public PgenDeleteLabelHandler(AbstractPgenTool tool) {
        this.tool = tool;
        pgenrsc = tool.getDrawingLayer();
        mapEditor = tool.mapEditor;

        if (tool instanceof AbstractPgenDrawingTool) {
            attrDlg = ((AbstractPgenDrawingTool) tool).getAttrDlg();
        }
    }

    /**
     * Handle a mouse down event
     * 
     * @param x
     *            the x screen coordinate
     * @param y
     *            the y screen coordinate
     * @param mouseButton
     *            the button held down
     * @return true if other handlers should be pre-empted
     */
    @Override
    public boolean handleMouseDown(int anX, int aY, int button) {
        return false;
    }

    /**
     * Handle a mouse down move event
     * 
     * @param x
     *            the x screen coordinate
     * @param y
     *            the y screen coordinate
     * @param mouseButton
     *            the button held down
     * @return true if other handlers should be pre-empted
     */
    @Override
    public boolean handleMouseDownMove(int x, int y, int mouseButton) {
        return false;
    }

    /**
     * Close any attribute dialogs, then delete the label
     */
    @Override
    public void preprocess() {

        if (pgenrsc.getSelectedComp() != null) {

            if (attrDlg != null) {
                AbstractDrawableComponent adc = pgenrsc.getSelectedComp()
                        .getParent();

                if ((adc instanceof Layer) || adc.isLabeledSymbol()) {
                    attrDlg.close();
                }

            }

            doDelete();
            tool.resetMouseHandler();
        }
    }

    /**
     * Deletes the label from a non-met,non-contour symbol
     */
    private void doDelete() {
        AbstractDrawableComponent adc = pgenrsc.getSelectedComp();

        if (adc.getParent() instanceof ContourMinmax || adc.isLabeledSymbol()) {
            deleteFirstLabelFound((DECollection) adc.getParent());
        } else {
            pgenrsc.removeElement(adc);
        }

        // Set the selected element ( now removed ) as null
        pgenrsc.removeSelected();
        mapEditor.refresh();
    }

    /**
     * Remove the first Text object found in a DECollection object
     * 
     * @param labeledSymbol
     *            a collection holding at least 2 drawable elements
     */
    private void deleteFirstLabelFound(DECollection labeledSymbol) {

        Iterator<AbstractDrawableComponent> it = labeledSymbol
                .getComponentIterator();

        while (it.hasNext()) {
            AbstractDrawableComponent item = it.next();
            if (item instanceof Text) {
                // Remove the label from the screen
                pgenrsc.removeElement(item);
                return;
            }
        }

    }

    /**
     * Get the mapEditor object
     * 
     * @return AbstractEditor mapEditor
     */
    public AbstractEditor getMapEditor() {
        return mapEditor;
    }

    /**
     * Get the PgenResource object
     * 
     * @return PgenResource pgenrsc
     */
    public PgenResource getPgenrsc() {
        return pgenrsc;
    }

}
