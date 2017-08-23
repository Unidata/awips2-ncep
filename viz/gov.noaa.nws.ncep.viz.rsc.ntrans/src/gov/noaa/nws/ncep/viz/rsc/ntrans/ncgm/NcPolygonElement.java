/**
 * 
 */
package gov.noaa.nws.ncep.viz.rsc.ntrans.ncgm;

import java.awt.geom.PathIterator;
import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.raytheon.uf.viz.core.exception.VizException;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import gov.noaa.nws.ncep.viz.rsc.ntrans.jcgm.PolygonElement;
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
 * 
 * @author bhebbard
 * 
 */
public class NcPolygonElement extends PolygonElement implements INcCommand {

    private GeometryFactory gf;

    public NcPolygonElement(int ec, int eid, int l, DataInput in)
            throws IOException {
        super(ec, eid, l, in);
        gf = new GeometryFactory();
    }

    @Override
    public void contributeToPaintableImage(ImageBuilder ib)
            throws VizException {

        PathIterator pi = this.polygon.getPathIterator(null);

        List<Coordinate> currentPoints = new ArrayList<>();

        double[] coordinates = new double[6];

        while (pi.isDone() == false) {
            int type = pi.currentSegment(coordinates);
            switch (type) {
            case PathIterator.SEG_MOVETO:
                terminatePolygon(ib, currentPoints);
            case PathIterator.SEG_LINETO:
                double[] point = ib.scalePoint(coordinates);
                currentPoints.add(new Coordinate(point[0], point[1]));
                break;
            case PathIterator.SEG_QUADTO:
                // TODO -- error / not supported
                break;
            case PathIterator.SEG_CUBICTO:
                // TODO -- error / not supported
                break;
            case PathIterator.SEG_CLOSE:
                terminatePolygon(ib, currentPoints);
                break;
            default:
                break;
            }
            pi.next();
        }

        if (currentPoints.size() > 1) {
            // Just in case SEG_CLOSE missing at end
            terminatePolygon(ib, currentPoints);
        }

    }

    private void terminatePolygon(ImageBuilder ib,
            List<Coordinate> currentPoints) {
        if (currentPoints.size() > 1) {
            Coordinate[] coords = currentPoints.toArray(new Coordinate[0]);
            LineString[] lineStrings = new LineString[] { gf
                    .createLineString(coords) };
            ib.addPolygon(lineStrings);
        }
        currentPoints.clear();
    }

}
