/**
 * This code has unlimited rights, and is provided "as is" by the National Centers 
 * for Environmental Prediction, without warranty of any kind, either expressed or implied, 
 * including but not limited to the implied warranties of merchantability and/or fitness 
 * for a particular purpose.
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 **/
package gov.noaa.nws.ncep.viz.rsc.ghcd;

import gov.noaa.nws.ncep.viz.common.display.INatlCntrsDescriptor;
import gov.noaa.nws.ncep.viz.resources.time_match.NCTimeMatcher;
import gov.noaa.nws.ncep.viz.ui.display.NCTimeSeriesDescriptor;
import gov.noaa.nws.ncep.viz.ui.display.NCTimeSeriesRenderableDisplay;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.PixelExtent;
import com.raytheon.uf.viz.core.drawables.AbstractDescriptor;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.xy.graph.IGraph;
import com.raytheon.viz.ui.editor.AbstractEditor;

/**
 * Ghcd descriptor, needed so loading bundles know what editor to load with this
 * descriptor.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#    Engineer    Description
 * ------------  ---------- ----------- --------------------------
 * Sep 5, 2014   R4508       sgurung     Initial creation
 * 
 * </pre>
 * 
 * @author sgurung
 * @version 1.0
 */

@XmlAccessorType(XmlAccessType.NONE)
public class GhcdDescriptor extends NCTimeSeriesDescriptor implements
        INatlCntrsDescriptor {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(AbstractDescriptor.class);

    public GhcdDescriptor() {
        super();
    }

    public GhcdDescriptor(PixelExtent pixelExtent) {
        super(pixelExtent);
    }

    public GhcdDescriptor(NCTimeSeriesDescriptor desc) throws VizException {
        super();

        List<ResourcePair> rlist = desc.getResourceList();
        if (rlist != null) {
            ResourcePair[] rp = rlist.toArray(new ResourcePair[rlist.size()]);
            this.setSerializableResources(rp);
        }

        IRenderableDisplay rendDisp = desc.getRenderableDisplay();
        if (!(rendDisp instanceof NCTimeSeriesRenderableDisplay)) {
            throw new VizException("Error: Renderable display is not of type "
                    + rendDisp.getClass().getName());
        }

        rendDisp.setDescriptor(desc);
        this.setRenderableDisplay(rendDisp);

        NCTimeMatcher tm = (NCTimeMatcher) desc.getTimeMatcher();

        if (tm == null) {
            tm = (NCTimeMatcher) desc.getRenderableDisplay().getDescriptor()
                    .getTimeMatcher();
        }

        desc.setTimeMatcher(tm);

        this.setAutoUpdate(true);
        // this.setDataTimes(desc.getDataTimes());
        this.setFramesInfo(desc.getFramesInfo());
        this.setNumberOfFrames(desc.getNumberOfFrames());
        this.setGridGeometry(desc.getGridGeometry());

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.xy.graph.XyGraphDescriptor#constructGraph()
     */
    @Override
    public IGraph constructGraph() {
        return new GhcdGraph(this);
    }

    public static IDisplayPane[] getDisplayPane() {
        // get the pane of the selected resource.
        AbstractEditor editor = NcDisplayMngr.getActiveNatlCntrsEditor();

        return (IDisplayPane[]) editor.getDisplayPanes();

    }

    public void addDescriptor(GhcdDescriptor desc, IDisplayPane pane) {

        NCTimeSeriesRenderableDisplay rendDisp = (NCTimeSeriesRenderableDisplay) pane
                .getRenderableDisplay();

        if (!(rendDisp instanceof NCTimeSeriesRenderableDisplay)) {
            try {
                throw new VizException(
                        "Error: can't zoom to resource in the renderable display : "
                                + rendDisp.getClass().getName());
            } catch (VizException e) {
                statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(),
                        e);
            }
        }

        rendDisp.setDescriptor(desc);

    }

    public void addDescriptor(NCTimeSeriesDescriptor desc,
            IRenderableDisplay rendDisp) {

        if (!(rendDisp instanceof NCTimeSeriesRenderableDisplay)) {
            try {
                throw new VizException(
                        "Error: can't zoom to resource in the renderable display : "
                                + rendDisp.getClass().getName());
            } catch (VizException e) {
                statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(),
                        e);
            }
        }

        rendDisp.setDescriptor(desc);

    }

    public void setResourcePair(GhcdDescriptor desc, IDisplayPane pane) {

        List<ResourcePair> rlist = pane.getRenderableDisplay().getDescriptor()
                .getResourceList();

        if (rlist != null) {

            ResourcePair[] rp = rlist.toArray(new ResourcePair[rlist.size()]);
            desc.setSerializableResources(rp);
        }

    }

    public void setResourcePair(List<ResourcePair> rlist) {

        if (rlist != null) {

            ResourcePair[] rp = rlist.toArray(new ResourcePair[rlist.size()]);
            this.setSerializableResources(rp);
        }

    }

    public void setNCTimeMatcher(GhcdDescriptor desc, IDisplayPane pane) {

        NCTimeMatcher tm = (NCTimeMatcher) pane.getDescriptor()
                .getTimeMatcher();

        desc.setTimeMatcher(tm);
    }

    public void setNCTimeMatcher(NCTimeMatcher tm) {
        this.setTimeMatcher(tm);
    }

}
