package gov.noaa.nws.ncep.ui.pgen.tools;

import gov.noaa.nws.ncep.ui.pgen.PgenConstant;
import gov.noaa.nws.ncep.ui.pgen.PgenUtil;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.AttrDlg;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.LabeledSymbolAttrDlg;
import gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent;
import gov.noaa.nws.ncep.ui.pgen.elements.DECollection;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableElement;
import gov.noaa.nws.ncep.ui.pgen.rsc.PgenResource;

import com.raytheon.viz.ui.editor.AbstractEditor;

/**
 * Action ("event" ) handler for the context menu of a non-met, non-contour
 * symbol -- adding labels to the symbol. A "non met, non-contour" symbol is a
 * symbol chosen by pressing the "Symbol" button on the Pgen palatte and then
 * pressing a button for a symbol. A met contour symbol is chosen from the Pgen
 * palette by pressing the "Met" button, then the contour button, then choosing
 * a symbol from that dialog box
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 8, 2015  R8198      srussell    Initial creation
 * 
 * </pre>
 * 
 * @author srussell
 * @version 1.0
 */

public class PgenAddSymbolLabelHandler extends InputHandlerDefaultImpl {

    protected AbstractEditor mapEditor = null;

    protected PgenResource pgenrsc = null;

    protected AbstractPgenTool tool = null;

    protected AttrDlg attrDlg = null;

    /**
     * Constructor
     * 
     * @param AbstractPgenTool
     *            tool
     */
    public PgenAddSymbolLabelHandler(AbstractPgenTool tool) {
        this.tool = tool;
        pgenrsc = tool.getDrawingLayer();
        mapEditor = tool.mapEditor;

        if (tool instanceof AbstractPgenDrawingTool) {
            attrDlg = ((AbstractPgenDrawingTool) tool).getAttrDlg();
        }

    }

    /**
     * Add labels to a non-met, non-contour symbol
     */
    @Override
    public void preprocess() {

        if (pgenrsc.getSelectedComp() != null) {
            doAdd();
        }
    }

    /**
     * Add a label to a non-met,non-contour symbol
     */
    private void doAdd() {

        AbstractDrawableComponent adc = pgenrsc.getSelectedComp();
        boolean useSymbolColor = ((LabeledSymbolAttrDlg) attrDlg)
                .useSymbolColor();

        // If an unlabeled symbol represented by a DrawableElement object
        // ( no label, not ready yet for a label )
        if (!(adc instanceof DECollection)) {

            // Get symbol (DrawableElement obj ) ready for a label by putting
            // it into a DECollection object
            adc = wrapDEInADECollection(adc);

        }

        // Shift control to PgenTextDrawingTool, launch Text Attributes dialog
        PgenUtil.setDrawingTextMode(true, useSymbolColor, "", adc);

        mapEditor.refresh();
    }

    /**
     * Convert an unlabeled, non-contour, single point Symbol represented as a
     * DrawableElement object to a labeled symbol represented as a DECollection
     * object that holds both the symbol and the label. The DECollection object
     * holds the symbol as a DrawableElement object and the label as a String[]
     * in a Text object
     * 
     * @param AbstractDrawableComponent
     *            adc - the currently selected symbol
     * @return AbstractDrawableComponent adc - the currently selected symbol
     *         converted into a DECollection obj which is capable of recieving a
     *         label
     * 
     */

    public AbstractDrawableComponent wrapDEInADECollection(
            AbstractDrawableComponent adc) {

        DrawableElement elem = ((DrawableElement) adc);
        DECollection dec = new DECollection(PgenConstant.LABELED_SYMBOL);
        dec.setPgenCategory(elem.getPgenCategory());
        dec.setPgenType(elem.getPgenType());
        dec.addElement(elem);

        // PgenTextDrawingTool will do the work of converting a symbol as a
        // DrawableElement object into a "labeledSymbol" as a DECollection obj.
        // To do this it needs to see the DECollection obj as the "prevElem",
        // previous element. So replace adc, with dec.
        pgenrsc.replaceElement(adc, dec);
        adc = dec;

        return adc;
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