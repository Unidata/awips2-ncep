/*
 * gov.noaa.nws.ncep.ui.pgen.tools.PgenGfaFormatTool
 *
 * June 2009
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.ui.pgen.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.JAXBException;

import com.raytheon.uf.viz.core.rsc.IInputHandler;

import gov.noaa.nws.ncep.ui.pgen.PgenUtil;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.GfaFormatAttrDlg;
import gov.noaa.nws.ncep.ui.pgen.gfa.Gfa;
import gov.noaa.nws.ncep.ui.pgen.gfa.GfaGenerate;

/**
 * Implements a modal map tool for PGEN format.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer     Description
 * ------------- -------- ------------ -----------------------------------------
 * 06/10         263      M.Laryukhin  Created
 * 04/13         977      S. Gilbert   PGEN Database support
 * Dec 02, 2021  95362    tjensen      Refactor PGEN Resource management to
 *                                     support multi-panel displays
 *
 * </pre>
 *
 * @author M.Laryukhin
 */
public class PgenGfaFormatTool extends AbstractPgenDrawingTool {

    private GfaGenerate gfaGenerate;

    /**
     * Input handler for mouse events.
     */
    public PgenGfaFormatTool() {

        super();

    }

    /*
     * Invoked by the CommandService when starting this tool
     *
     * @see com.raytheon.viz.ui.tools.AbstractTool#runTool()
     */
    @Override
    protected void activateTool() {

        super.activateTool();

        if (attrDlg instanceof GfaFormatAttrDlg) {
            GfaFormatAttrDlg gfaDlg = (GfaFormatAttrDlg) attrDlg;
            gfaDlg.setGfaFormatTool(this);
        }

        return;
    }

    /**
     * Returns the current mouse handler.
     *
     * @return
     */
    @Override
    public IInputHandler getMouseHandler() {

        if (this.mouseHandler == null) {
            this.mouseHandler = new PgenGfaFormatHandler();
        }

        return this.mouseHandler;

    }

    /**
     * Implements input handler for mouse events.
     *
     * @author jwu
     *
     */
    public class PgenGfaFormatHandler extends InputHandlerDefaultImpl {

        @Override
        public boolean handleMouseDown(int anX, int aY, int button) {
            if (!isResourceEditable()) {
                return false;
            }

            if (button == 1) {
                mapEditor.refresh();
                return true;
            } else if (button == 3) {
                return true;
            } else {
                return false;
            }
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
            if (!isResourceEditable()) {
                return false;
            }

            if (button == 3) {

                PgenUtil.setSelectingMode();
                return true;
            } else {
                return false;
            }
        }
    }

    public StringBuilder generate(ArrayList<Gfa> all, List<String> areas,
            List<String> categories, String dataURI)
            throws IOException, JAXBException {

        // delegate to GfaGenerate
        StringBuilder sb = getGfaGenerate().generate(all, areas, categories,
                dataURI);

        return sb;
    }

    private GfaGenerate getGfaGenerate() {
        if (gfaGenerate == null) {
            gfaGenerate = new GfaGenerate();
        }
        return gfaGenerate;
    }
}
