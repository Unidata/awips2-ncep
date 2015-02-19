/**
 * 
 */
package gov.noaa.nws.ncep.viz.rsc.ntrans.ncgm;

import gov.noaa.nws.ncep.viz.common.ui.color.GempakColor;
import gov.noaa.nws.ncep.viz.rsc.ntrans.jcgm.FillColour;
import gov.noaa.nws.ncep.viz.rsc.ntrans.rsc.ImageBuilder;

import java.io.DataInput;
import java.io.IOException;

import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;

/**
 * @author bhebbard
 * 
 */
public class NcFillColour extends FillColour implements INcCommand {
    public NcFillColour(int ec, int eid, int l, DataInput in)
            throws IOException {
        super(ec, eid, l, in);
    }

    @Override
    public void contributeToPaintableImage(ImageBuilder ib, IGraphicsTarget target,
            PaintProperties paintProps, IDescriptor descriptor) throws VizException {
        ib.currentFillColor = GempakColor.convertToRGB(this.colorIndex);
    }

}
