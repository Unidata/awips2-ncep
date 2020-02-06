package gov.noaa.nws.ncep.viz.rsc.satellite.rsc;

import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;

import org.eclipse.swt.widgets.Event;

import com.raytheon.uf.common.geospatial.ReferencedCoordinate;
import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.viz.ui.editor.AbstractEditor;
import com.raytheon.viz.ui.input.InputAdapter;
import com.raytheon.viz.ui.panes.VizDisplayPane;
import org.locationtech.jts.geom.Coordinate;

/**
 * Default input handler for polar-orbiting satellite sampling.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket# Engineer        Description
 * ------------ ------- -----------     --------------------------
 * 05/24/2016   R18511  kbugenhagen     Initial creation.
 * 
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1.0
 * @param <R>
 *            satellite resource
 */

public class PolarOrbitSatInputAdapter<R extends AbstractPolarOrbitSatResource<?>>
        extends InputAdapter {

    private R resource;

    public PolarOrbitSatInputAdapter(R resource) {
        this.resource = resource;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.ui.input.InputAdapter#handleMouseMove(int, int)
     */
    @Override
    public boolean handleMouseMove(int x, int y) {

        IDisplayPaneContainer container = resource.getResourceContainer();
        Coordinate c = container.translateClick(x, y);

        boolean isActiveResource = false;

        AbstractEditor editor = NcDisplayMngr.getActiveNatlCntrsEditor();
        IDisplayPane activePane = editor.getActiveDisplayPane();

        ResourceList acResources = activePane.getDescriptor().getResourceList();
        int acRscSize = acResources.size();

        for (int i = acRscSize - 1; i >= 0; i--) {
            ResourcePair rp = acResources.get(i);
            AbstractVizResource<?, ?> activeRsc = rp.getResource();

            if (activeRsc != null
                    && activeRsc instanceof AbstractPolarOrbitSatResource<?>
                    && rp.getProperties().isVisible()
                    && !((AbstractPolarOrbitSatResource<?>) activeRsc)
                            .getLegendStr().equals("No Data")) {
                if (activeRsc.equals(resource)) {
                    isActiveResource = true;
                }
                break;
            }

        }

        if (resource.getResourceContainer().getDisplayPanes().length > 1) {
            for (IDisplayPane pane : resource.getResourceContainer()
                    .getDisplayPanes()) {
                if (!pane.equals(activePane) && isActiveResource) {
                    ResourceList resources = pane.getDescriptor()
                            .getResourceList();
                    int size = resources.size();
                    for (int i = 0; i < size && size > 1; i++) {
                        ResourcePair rp = resources.get(i);
                        AbstractVizResource<?, ?> rsc = rp.getResource();

                        if (rsc != null
                                && rsc instanceof AbstractPolarOrbitSatResource<?>
                                && rp.getProperties().isVisible()) {
                            ((VizDisplayPane) pane).setVirtualCursor(c);
                            ((AbstractPolarOrbitSatResource<?>) rsc)
                                    .issueRefresh();
                        }
                    }
                }
            }
        }

        if (isActiveResource) {
            if (c != null) {
                resource.sampleCoord = new ReferencedCoordinate(c);
            } else {
                resource.sampleCoord = null;
            }
        }

        if (resource.isSampling()) {
            resource.issueRefresh();
        }

        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.ui.input.InputAdapter#handleMouseDownMove(int, int,
     * int)
     */
    @Override
    public boolean handleMouseDownMove(int x, int y, int mouseButton) {
        return handleMouseMove(x, y);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.viz.ui.input.InputAdapter#handleMouseExit(org.eclipse.swt
     * .widgets.Event)
     */
    @Override
    public boolean handleMouseExit(Event event) {
        resource.sampleCoord = null;
        resource.virtualCursor = null;
        if (resource.isSampling()) {
            resource.issueRefresh();
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.viz.ui.input.InputAdapter#handleMouseEnter(org.eclipse.swt
     * .widgets.Event)
     */
    @Override
    public boolean handleMouseEnter(Event event) {
        return handleMouseMove(event.x, event.y);
    }
}
