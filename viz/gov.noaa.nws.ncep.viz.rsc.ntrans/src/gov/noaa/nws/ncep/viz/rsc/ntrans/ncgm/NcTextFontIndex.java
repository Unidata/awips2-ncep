/**
 * 
 */
package gov.noaa.nws.ncep.viz.rsc.ntrans.ncgm;

import java.io.DataInput;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.raytheon.uf.viz.core.exception.VizException;

import gov.noaa.nws.ncep.viz.rsc.ntrans.jcgm.TextFontIndex;
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
public class NcTextFontIndex extends TextFontIndex implements INcCommand {

    private final Log logger = LogFactory.getLog(this.getClass());

    private static boolean notWarned = true;

    public NcTextFontIndex(int ec, int eid, int l, DataInput in)
            throws IOException {
        super(ec, eid, l, in);
    }

    @Override
    public void contributeToPaintableImage(ImageBuilder ib)
            throws VizException {
        if (notWarned) {
            logger.warn("Paint not implemented for CGM command:  " + this);
            notWarned = false;
        }
    }

}
