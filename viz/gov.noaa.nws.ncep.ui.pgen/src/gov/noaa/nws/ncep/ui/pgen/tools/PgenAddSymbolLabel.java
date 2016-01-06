package gov.noaa.nws.ncep.ui.pgen.tools;

import com.raytheon.uf.viz.core.rsc.IInputHandler;

/**
 * Action handler routing for adding labels to non-met, non-contour symbols
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 8, 2015  R8198      srussell     Initial creation
 * 
 * </pre>
 * 
 * @author Steve Russell
 * @version 1.0
 */

public class PgenAddSymbolLabel extends AbstractPgenTool {

    protected IInputHandler addLabelHandler = null;

    /**
     * Constructor
     */

    public PgenAddSymbolLabel() {
        super();
    }

    /**
     * Returns the current mouse handler.
     * 
     * @return IInputHandler the current mouse handler
     */

    public IInputHandler getMouseHandler() {

        if (this.addLabelHandler == null
                || this.mapEditor != ((PgenAddSymbolLabelHandler) addLabelHandler)
                        .getMapEditor()
                || this.drawingLayer != ((PgenAddSymbolLabelHandler) addLabelHandler)
                        .getPgenrsc()) {
            this.addLabelHandler = new PgenAddSymbolLabelHandler(this);

        }

        return this.addLabelHandler;

    }

}
