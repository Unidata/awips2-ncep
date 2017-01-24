package gov.noaa.nws.ncep.viz.rsc.ntrans.ncgm;

import java.awt.geom.PathIterator;
import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.raytheon.uf.viz.core.exception.VizException;

import gov.noaa.nws.ncep.viz.rsc.ntrans.jcgm.Polyline;
import gov.noaa.nws.ncep.viz.rsc.ntrans.rsc.ImageBuilder;

/**
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------
 * Oct 24, 2016  R22550   bsteffen  Simplify INcCommand
 * 
 * </pre>
 */
public class NcPolyline extends Polyline implements INcCommand {

    public NcPolyline(int ec, int eid, int l, DataInput in) throws IOException {
        super(ec, eid, l, in);
    }

    @Override
    public void contributeToPaintableImage(ImageBuilder ib)
            throws VizException {

        List<double[]> currentPoints = new ArrayList<>();

        /*
         * Process individual segments in the CGM polyline, adding them to the
         * wireframe under construction.
         */
        PathIterator pi = this.path.getPathIterator(null);
        double[] coordinates = new double[6];
        while (pi.isDone() == false) {
            int type = pi.currentSegment(coordinates);
            switch (type) {
            case PathIterator.SEG_MOVETO:
                if (currentPoints.size() > 1) {
                    ib.addLineSegment(currentPoints.toArray(new double[0][]));
                }
                currentPoints.clear();
                currentPoints.add(ib.scalePoint(coordinates));
                break;
            case PathIterator.SEG_LINETO:

                currentPoints.add(ib.scalePoint(coordinates));
                break;
            case PathIterator.SEG_QUADTO:
                // TODO -- error / not supported
                break;
            case PathIterator.SEG_CUBICTO:
                // TODO -- error / not supported
                break;
            case PathIterator.SEG_CLOSE:
                if (currentPoints.size() > 1) {
                    ib.addLineSegment(currentPoints.toArray(new double[0][]));
                }
                currentPoints.clear();
                break;
            default:
                break;
            }
            pi.next();
        }

        // if no close command
        if (currentPoints.size() > 1) {
            ib.addLineSegment(currentPoints.toArray(new double[0][]));
        }
    }

}
