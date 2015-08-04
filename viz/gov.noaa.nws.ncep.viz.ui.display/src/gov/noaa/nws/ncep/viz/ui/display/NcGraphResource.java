/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package gov.noaa.nws.ncep.viz.ui.display;

import java.util.ArrayList;
import java.util.HashMap;

import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.PixelExtent;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.xy.graph.GraphProperties;
import com.raytheon.uf.viz.xy.graph.IGraph;
import com.raytheon.uf.viz.xy.map.rsc.GraphResource;
import com.raytheon.uf.viz.xy.map.rsc.GraphResourceData;

/**
 * The graph resource is a resource that contains 1-N graphs, lays them out and
 * provides functionality for drawing to them / sampling them
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 29, 2009            mschenke     Initial creation
 * Oct 28, 2014 R4508      sgurung      Create NCEP version to override paintInternal() method
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public class NcGraphResource extends GraphResource {

    /** The distance between graphs */
    private int graphDistance = 60;

    protected NcGraphResource(GraphResourceData resourceData,
            LoadProperties loadProperties) {
        super(resourceData, loadProperties);
        graphs = new ArrayList<IGraph>();
        graphMap = new HashMap<Object, IGraph>();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.xy.map.rsc.GraphResource#paintInternal(com.raytheon
     * .uf.viz.core.IGraphicsTarget,
     * com.raytheon.uf.viz.core.drawables.PaintProperties)
     */
    @Override
    protected void paintInternal(IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {
        GraphProperties props = (GraphProperties) paintProps;
        // TODO Layout graphs in horizontal fashion and paint them
        if (graphs.size() == 0) {
            return;
        }

        if (newGraphs) {
            totalExtent = props.getWorldExtent();
            // For NCP plots with multipgraphs, raytheon's totalGraphExtent was
            // not suitable,
            // therefore, overriding this method to change the totalGraphExtent
            IExtent totalGraphExtent = new PixelExtent(
                    totalExtent.getMinX() + 25, totalExtent.getMaxX() - 1,
                    totalExtent.getMinY() + 45, totalExtent.getMaxY() - 100);
            double minX, maxX, minY, maxY;
            switch (resourceData.getOverlayMode()) {
            case VERTICAL:
                int numGraphs = visibleGraphCount();
                double graphHeight = (totalGraphExtent.getHeight() / numGraphs)
                        - graphDistance;

                minX = totalGraphExtent.getMinX();
                maxX = totalGraphExtent.getMaxX();
                minY = totalGraphExtent.getMinY();
                maxY = minY + graphHeight;
                for (int i = graphs.size() - 1; i >= 0; --i) {
                    IGraph graph = graphs.get(i);
                    if (graph.isDisplayed()) {
                        graph.updateExtent(new PixelExtent(minX, maxX, minY,
                                maxY));
                        graph.paint(target, paintProps);
                        minY = maxY + graphDistance;
                        maxY = minY + graphHeight;
                    }
                }
                break;
            case OVERLAY:
            default:
                minX = totalGraphExtent.getMinX();
                maxX = totalGraphExtent.getMaxX();
                minY = totalGraphExtent.getMinY();
                maxY = totalGraphExtent.getMaxY() - graphDistance;
                for (int i = graphs.size() - 1; i >= 0; --i) {
                    IGraph graph = graphs.get(i);
                    if (graph.isDisplayed()) {
                        graph.updateExtent(new PixelExtent(minX, maxX, minY,
                                maxY));
                        graph.paint(target, paintProps);
                    }
                }
                break;
            }

            newGraphs = false;
        } else {
            for (IGraph graph : graphs) {
                if (graph.isDisplayed() == true) {
                    graph.paint(target, paintProps);
                }
            }
        }
    }

    public int getGraphDistance() {
        return graphDistance;
    }

    public void setGraphDistance(int graphDistance) {
        this.graphDistance = graphDistance;
    }

}
