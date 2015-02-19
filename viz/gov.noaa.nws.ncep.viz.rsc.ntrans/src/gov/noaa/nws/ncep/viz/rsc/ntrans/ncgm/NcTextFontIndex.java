/**
 * 
 */
package gov.noaa.nws.ncep.viz.rsc.ntrans.ncgm;

import gov.noaa.nws.ncep.viz.rsc.ntrans.jcgm.TextFontIndex;
import gov.noaa.nws.ncep.viz.rsc.ntrans.rsc.ImageBuilder;

import java.io.DataInput;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;

/**
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
    public void contributeToPaintableImage(ImageBuilder ib, IGraphicsTarget target,
            PaintProperties paintProps, IDescriptor descriptor) throws VizException {
        if (notWarned) {
            logger.warn("Paint not implemented for CGM command:  " + this);
            notWarned = false;
        }
    }

}
