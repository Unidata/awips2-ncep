package gov.noaa.nws.ncep.viz.rsc.modis.rsc;

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
import com.vividsolutions.jts.geom.Coordinate;

/**
 * Default input handler for modis sampling
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket# Engineer        Description
 * ------------ ------- -----------     --------------------------
 * 11/13/2015   R13133  kbugenhagen     Initial creation
 * 12/15/2015   R13133  kbugenhagen     Took out generics; unnecessary.
 * 
 * 
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1.0
 */

public class ModisInputAdapter extends InputAdapter {

    private ModisResource resource;

    public ModisInputAdapter(ModisResource resource) {
        this.resource = resource;
    }

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
                    && activeRsc instanceof ModisResource
                    && rp.getProperties().isVisible()
                    && !((ModisResource) activeRsc).getLegendStr().equals(
                            "No Data")) {
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

                        if (rsc != null && rsc instanceof ModisResource
                                && rp.getProperties().isVisible()) {
                            ((VizDisplayPane) pane).setVirtualCursor(c);
                            ((ModisResource) rsc).issueRefresh();
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

    @Override
    public boolean handleMouseDownMove(int x, int y, int mouseButton) {
        return handleMouseMove(x, y);
    }

    public boolean handleMouseExit(Event event) {
        resource.sampleCoord = null;
        resource.virtualCursor = null;
        if (resource.isSampling()) {
            resource.issueRefresh();
        }
        return false;
    }

    public boolean handleMouseEnter(Event event) {
        return handleMouseMove(event.x, event.y);
    }
}
