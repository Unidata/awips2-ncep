/*
 * gov.noaa.nws.ncep.ui.pgen.rsc.PgenLineDrawingFlipElement
 * 
 * 28 April 2009
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.ui.pgen.tools;

import com.raytheon.uf.viz.core.rsc.IInputHandler;
import com.raytheon.viz.ui.editor.AbstractEditor;
import org.locationtech.jts.geom.Coordinate;

import gov.noaa.nws.ncep.ui.pgen.PgenUtil;
import gov.noaa.nws.ncep.ui.pgen.annotation.Operation;
import gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableElement;
import gov.noaa.nws.ncep.ui.pgen.elements.Line;
import gov.noaa.nws.ncep.ui.pgen.elements.labeledlines.Cloud;
import gov.noaa.nws.ncep.ui.pgen.filter.OperationFilter;
import gov.noaa.nws.ncep.ui.pgen.rsc.PgenResource;

/**
 * Implements a modal map tool for the PGEN flip element function.
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * -----------  --------    ----------- --------------------------
 * 04/28        ??          M. Gao      Initial Creation.
 * 04/29        103         B. Yin      Extends from AbstractPgenTool
 * 09/30        169         G. Hull     NCMapEditor
 * 09/09/2019   68192       K. Sunil    Implement deactivateTool, set fliphanlder to null when Pgen is closed.
 *
 * </pre>
 *
 * @author M. Gao
 */

public class PgenFlipDrawingElement extends AbstractPgenTool {

    /**
     * Input handler for mouse events.
     */
    protected IInputHandler flipHandler;

    public PgenFlipDrawingElement() {

        super();

    }

    /**
     * Returns the current mouse handler.
     *
     * @return
     */
    public IInputHandler getMouseHandler() {
        if (this.flipHandler == null) {
            this.flipHandler = new PgenFlipHandler(drawingLayer, mapEditor);
        }
        return this.flipHandler;
    }

    @Override
    public void deactivateTool() {
        super.deactivateTool();
        /*
         * set to null and force a "new" flipHandler when PGen is reopened,
         * (possibly with a new new drawing layer and/or editor). If not, the
         * FLIP operation won't work properly when Pgen is reopened.
         */
        this.flipHandler = null;
    }

    /**
     * Implements input handler for mouse events.
     *
     * @author Michael Gao
     *
     */
    public class PgenFlipHandler extends InputHandlerDefaultImpl {

        private PgenResource flipPgenSource;

        private AbstractEditor flipNCMapEditor;

        private boolean preempt;

        private OperationFilter flipFilter;

        public PgenFlipHandler(PgenResource _flipPgenSource,
                AbstractEditor _flipNCMapEditor) {
            flipPgenSource = _flipPgenSource;
            flipNCMapEditor = _flipNCMapEditor;
            flipFilter = new OperationFilter(Operation.FLIP);
        }

        @Override
        public boolean handleMouseDown(int anX, int aY, int button) {
            if (!isResourceEditable())
                return false;

            preempt = false;
            // Check if mouse is in geographic extent
            Coordinate loc = flipNCMapEditor.translateClick(anX, aY);
            if (loc == null || shiftDown)
                return false;

            AbstractDrawableComponent selectedDrawableElement = flipPgenSource
                    .getSelectedComp();

            if (button == 1) {
                /*
                 * create a new DrawableElement with reversed points based on
                 * the selectedDrawableElement
                 */
                AbstractDrawableComponent reversedDrawableElement = null;
                if (selectedDrawableElement instanceof Cloud) {
                    reversedDrawableElement = selectedDrawableElement.copy();
                    DrawableElement de = flipPgenSource.getNearestElement(loc,
                            (Cloud) reversedDrawableElement);
                    if (de != null && de instanceof Line) {
                        ((Cloud) reversedDrawableElement).add(PgenToolUtils
                                .createReversedDrawableElement(de));
                        ((Cloud) reversedDrawableElement).remove(de);
                    } else {
                        return false;
                    }
                } else {
                    reversedDrawableElement = PgenToolUtils
                            .createReversedDrawableElement(
                                    selectedDrawableElement);
                }

                leftMouseButtonDownHandler(flipPgenSource,
                        selectedDrawableElement, reversedDrawableElement, loc);
                flipNCMapEditor.refresh();
                if (selectedDrawableElement != null)
                    preempt = true;
            } else if (button == 3) {
                return true;
            }
            return preempt;
        }

        @Override
        public boolean handleMouseDownMove(int x, int y, int mouseButton) {
            if (!isResourceEditable() || shiftDown)
                return false;
            return preempt;
        }

        /*
         * overrides the function in selecting tool
         */
        @Override
        public boolean handleMouseUp(int x, int y, int button) {
            if (!isResourceEditable())
                return false;

            if (button == 3) {
                AbstractDrawableComponent selectedDrawableElement = flipPgenSource
                        .getSelectedComp();
                rightMouseButtonDownHandler(flipPgenSource,
                        selectedDrawableElement, flipNCMapEditor);

                return true;
            } else {
                return false;
            }
        }

        /*
         * If the selectedDrawableElement is valid, reverse the Coordinate
         * points of the DrawableElement and then set the reversed points back
         * to the DrawableElement. Otherwise, retrieve the nearest
         * DrawableElement object using the current mouse location
         *
         * @param thePpgenSource, the PgenResource object
         *
         * @param selectedDrawableElement, the selected DrawableElement
         *
         * @param currentMouselocation, the Coordinate object to represent the
         * current mouse location
         *
         * @return
         */
        private void leftMouseButtonDownHandler(PgenResource thePgenSource,
                AbstractDrawableComponent selectedDrawableElement,
                AbstractDrawableComponent reversedDrawableElement,
                Coordinate currentMouselocation) {
            if (selectedDrawableElement == null) {
                // Get the nearest element and set it as the selected element.
                selectedDrawableElement = thePgenSource.getNearestComponent(
                        currentMouselocation, flipFilter, true);
                if (selectedDrawableElement == null)
                    return;
                thePgenSource.setSelected(selectedDrawableElement);
            } else {
                thePgenSource.replaceElement(selectedDrawableElement,
                        reversedDrawableElement);
                thePgenSource.setSelected(reversedDrawableElement);
            }
        }

        /*
         * If a valid selectedDrawableElement still exists, de-select the
         * element, Otherwise, change the action mode to Selecting Mode from the
         * Flip Mode
         *
         * @param thePpgenSource, the PgenResource object
         *
         * @param selectedDrawableElement, the selected DrawableElement
         *
         * @param theNCMapEditor, the NCMapEditor object used to redraw the map
         *
         * @return
         */
        private void rightMouseButtonDownHandler(PgenResource thePpgenSource,

                AbstractDrawableComponent selectedDrawableElement,
                AbstractEditor theNCMapEditor) {
            if (selectedDrawableElement != null) {
                // de-select element
                thePpgenSource.removeSelected();
                selectedDrawableElement = null;
                theNCMapEditor.refresh();
            } else {
                // set selecting mode
                PgenUtil.setSelectingMode();
            }
        }

    }

}
