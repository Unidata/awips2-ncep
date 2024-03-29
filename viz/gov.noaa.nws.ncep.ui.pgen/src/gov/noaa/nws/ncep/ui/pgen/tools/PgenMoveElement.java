/*
 * gov.noaa.nws.ncep.ui.pgen.rsc.PgenMoveElement
 * 
 * 22 April 2009
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.ui.pgen.tools;

import gov.noaa.nws.ncep.ui.pgen.PgenUtil;
import gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableElement;
import gov.noaa.nws.ncep.ui.pgen.elements.WatchBox;
import gov.noaa.nws.ncep.ui.pgen.gfa.Gfa;
import gov.noaa.nws.ncep.ui.pgen.gfa.GfaReducePoint;
import gov.noaa.nws.ncep.ui.pgen.sigmet.SigmetInfo;
import gov.noaa.nws.ncep.viz.common.SnapUtil;

import java.util.Iterator;

import com.raytheon.uf.viz.core.rsc.IInputHandler;
import org.locationtech.jts.geom.Coordinate;

/**
 * Implements a modal map tool for the PGEN copy element function.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 04/09            78      B. Yin      Initial Creation.
 * 06/09            116     B. Yin      Use AbstractDrawingComponent
 * 02/12            597     S. Gurung   Moved snap functionalities to SnapUtil from SigmetInfo.  
 * 02/12                    S. Gurung   Moved isSnapADC() and getNumOfCompassPts() to SigmeInfo.
 * 05/11            #808    J. Wu       Update Gfa vor text
 * 05/12            #610    J. Wu       Add warning when GFA FROM lines > 3
 * 12/14            R5413   B. Yin      PGEN in D2D Changes.
 * 06/15/2016       R13559  bkowal      File cleanup. No longer simulate mouse clicks.
 * </pre>
 * 
 * @author B. Yin
 */

public class PgenMoveElement extends PgenCopyElement {

    /**
     * Input handler for mouse events.
     */
    protected IInputHandler moveHandler = null;

    /**
     * Returns the current mouse handler.
     * 
     * @return
     */
    public IInputHandler getMouseHandler() {

        if (this.moveHandler == null) {

            this.moveHandler = new PgenMoveHandler();

        }

        return this.moveHandler;

    }

    public class PgenMoveHandler extends PgenCopyElement.PgenCopyHandler {

        /*
         * (non-Javadoc)
         * 
         * @see com.raytheon.viz.ui.input.IInputHandler#handleMouseUp(int, int,
         * int)
         */
        @Override
        public boolean handleMouseUp(int x, int y, int button) {
            if (!isResourceEditable() || shiftDown) {
                return false;
            }

            if (button == 3) {

                if (drawingLayer.getSelectedComp() != null) {
                    // de-select element
                    drawingLayer.removeSelected();
                    drawingLayer.removeGhostLine();
                    ghostEl = null;
                    mapEditor.refresh();
                } else {
                    // set selecting mode
                    PgenUtil.setSelectingMode();
                }

                return true;

            }

            if (ghostEl != null) {

                AbstractDrawableComponent comp = drawingLayer.getSelectedComp();
                // reset color for the el and add it to PGEN resource
                Iterator<DrawableElement> it1 = comp.createDEIterator();
                Iterator<DrawableElement> it2 = ghostEl.createDEIterator();

                while (it1.hasNext() && it2.hasNext()) {
                    it2.next().setColors(it1.next().getColors());
                }

                if (!(ghostEl instanceof WatchBox)
                        || ((ghostEl instanceof WatchBox) && PgenWatchBoxModifyTool
                                .resnapWatchBox(mapEditor, (WatchBox) ghostEl,
                                        (WatchBox) ghostEl))) {

                    if (SigmetInfo.isSnapADC(ghostEl)) {
                        java.util.ArrayList<Coordinate> list = SnapUtil
                                .getSnapWithStation(ghostEl.getPoints(),
                                        SnapUtil.VOR_STATION_LIST, 10,
                                        SigmetInfo.getNumOfCompassPts(ghostEl));
                        AbstractDrawableComponent ghostElCp = ghostEl.copy();
                        ((DrawableElement) ghostElCp).setPoints(list);

                        drawingLayer.replaceElement(comp, ghostElCp);
                        drawingLayer.setSelected(ghostElCp);
                    } else if (ghostEl instanceof Gfa) {

                        if (((Gfa) ghostEl).getGfaFcstHr().indexOf("-") > -1) {
                            // snap
                            ((Gfa) ghostEl).snap();
                            GfaReducePoint
                                    .WarningForOverThreeLines((Gfa) ghostEl);
                        }

                        ((Gfa) ghostEl).setGfaVorText(Gfa
                                .buildVorText((Gfa) ghostEl));

                        drawingLayer.replaceElement(comp, ghostEl);
                        drawingLayer.setSelected(ghostEl);

                    }

                    else {
                        drawingLayer.replaceElement(comp, ghostEl);
                        drawingLayer.setSelected(ghostEl);
                    }

                }

                drawingLayer.removeGhostLine();
                ghostEl = null;
                mapEditor.refresh();

            }

            return true;

        }
    }
}
