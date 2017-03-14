/**
 * This software was developed and / or modified by NOAA/NWS/OCP/ASDT
 **/
package gov.noaa.nws.ncep.ui.pgen.tools;

import gov.noaa.nws.ncep.ui.pgen.PgenUtil;
import com.raytheon.uf.viz.core.rsc.IInputHandler;

/**
 * Implements a modal map tool for PGEN format.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer        Description
 * ------------ ----------  -------------   --------------------------
 * 12/16/2016   17469       wkwock          Initial create
 * 
 * </pre>
 * 
 * @author wkwock
 */
public class PgenCwaFormatTool extends AbstractPgenDrawingTool {

    /**
     * Input handler for mouse events.
     */
    public PgenCwaFormatTool() {
        super();
    }

    @Override
    protected void activateTool() {
        super.activateTool();

        return;
    }

    /**
     * Returns the current mouse handler.
     * 
     * @return
     */
    public IInputHandler getMouseHandler() {
        if (this.mouseHandler == null) {
            this.mouseHandler = new PgenCwaFormatHandler();
        }

        return this.mouseHandler;
    }

    /**
     * Implements input handler for mouse events.
     * 
     */
    public class PgenCwaFormatHandler extends InputHandlerDefaultImpl {

        @Override
        public boolean handleMouseDown(int anX, int aY, int button) {
            if (!isResourceEditable())
                return false;

            if (button == 1) {
                mapEditor.refresh();
                return true;
            } else if (button == 3) {
                return true;
            }
            return false;
        }

        @Override
        public boolean handleMouseDownMove(int x, int y, int mouseButton) {
            return false;
        }

        /*
         * overrides the function in selecting tool
         */
        @Override
        public boolean handleMouseUp(int x, int y, int button) {
            if (!isResourceEditable())
                return false;

            if (button == 3) {
                PgenUtil.setSelectingMode();
                return true;
            }
            return false;
        }
    }
}
