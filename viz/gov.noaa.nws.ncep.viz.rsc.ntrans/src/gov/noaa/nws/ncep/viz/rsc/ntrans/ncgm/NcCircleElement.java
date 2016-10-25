/**
 * 
 */
package gov.noaa.nws.ncep.viz.rsc.ntrans.ncgm;

import java.io.DataInput;
import java.io.IOException;

import com.raytheon.uf.viz.core.DrawableCircle;
import com.raytheon.uf.viz.core.exception.VizException;

import gov.noaa.nws.ncep.viz.rsc.ntrans.jcgm.CircleElement;
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
public class NcCircleElement extends CircleElement implements INcCommand {

    public NcCircleElement(int ec, int eid, int l, DataInput in)
            throws IOException {
        super(ec, eid, l, in);
    }

    @Override
    public void contributeToPaintableImage(ImageBuilder ib)
            throws VizException {

        /*
         * TODO Used only to draw (teeny) circles to mark lat/lon lines? If not,
         * will need to revisit assumptions below...
         */

        DrawableCircle dc = new DrawableCircle();
        double[] newpoint = ib.scalePoint(this.center.x, this.center.y);
        dc.setCoordinates(newpoint[0], newpoint[1]);
        dc.radius = this.radius;
        /* TODO -- SJ says this is what was intended; not encoded in CGM? */
        dc.filled = true;
        dc.numberOfPoints = 6;
        dc.basics.color = ib.getCurrentLineColor();
        ib.addCircle(dc);
    }

}
