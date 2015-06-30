package gov.noaa.nws.ncep.ui.pgen.tools;

import com.raytheon.uf.viz.core.rsc.IInputHandler;

/**
 * Implements a modal map tool for PGEN deleting part function for labels of
 * non-met contour symbols only.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------	----------	-----------	--------------------------
 * 2015 June 09 R 8199      S Russell   Initial Creation.
 * 
 * </pre>
 * 
 * @author Steve Russell
 */

public class PgenDeleteLabel extends AbstractPgenTool {

    protected IInputHandler delLabelHandler = null;

    public PgenDeleteLabel() {
        super();
    }

    /**
     * Returns the current mouse handler.
     * 
     * @return IInputHandler the current mouse handler
     */

    public IInputHandler getMouseHandler() {

        if (this.delLabelHandler == null
                || this.mapEditor != ((PgenDeleteLabelHandler) delLabelHandler)
                        .getMapEditor()
                || this.drawingLayer != ((PgenDeleteLabelHandler) delLabelHandler)
                        .getPgenrsc()) {
            this.delLabelHandler = new PgenDeleteLabelHandler(this);

        }

        return this.delLabelHandler;

    }
}
