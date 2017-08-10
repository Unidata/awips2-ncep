/**
 * 
 */
package gov.noaa.nws.ncep.viz.rsc.ntrans.ncgm;

import java.io.DataInput;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.raytheon.uf.viz.core.exception.VizException;

import gov.noaa.nws.ncep.viz.rsc.ntrans.jcgm.InteriorStyle;
import gov.noaa.nws.ncep.viz.rsc.ntrans.rsc.ImageBuilder;

/**
 * 
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
public class NcInteriorStyle extends InteriorStyle implements INcCommand {

    private final Log logger = LogFactory.getLog(this.getClass());

    public NcInteriorStyle(int ec, int eid, int l, DataInput in)
            throws IOException {
        super(ec, eid, l, in);
    }

    @Override
    public void contributeToPaintableImage(ImageBuilder ib)
            throws VizException {
        switch (this.style) {
        case SOLID: // TODO: For now, SOLID is assumed for all filled polygons
            break;
        case HOLLOW:
        case PATTERN:
        case HATCH:
        case EMPTY:
        case GEOMETRIC_PATTERN:
        case INTERPOLATED:
            logger.warn("Paint not implemented for CGM command:  " + this);
            break;
        }
    }

}
