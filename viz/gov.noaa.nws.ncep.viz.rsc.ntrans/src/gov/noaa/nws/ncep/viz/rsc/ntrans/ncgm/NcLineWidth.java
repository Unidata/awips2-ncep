/**
 * 
 */
package gov.noaa.nws.ncep.viz.rsc.ntrans.ncgm;

import java.io.DataInput;
import java.io.IOException;

import com.raytheon.uf.viz.core.exception.VizException;

import gov.noaa.nws.ncep.viz.rsc.ntrans.jcgm.LineWidth;
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
public class NcLineWidth extends LineWidth implements INcCommand {

    public NcLineWidth(int ec, int eid, int l, DataInput in) throws IOException {
        super(ec, eid, l, in);
    }

    @Override
    public void contributeToPaintableImage(ImageBuilder ib)
            throws VizException {
        ib.setCurrentLineWidth(this.width);
    }

}
