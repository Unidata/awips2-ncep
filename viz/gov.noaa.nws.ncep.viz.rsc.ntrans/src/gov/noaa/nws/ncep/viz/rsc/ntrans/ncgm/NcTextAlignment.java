/**
 * 
 */
package gov.noaa.nws.ncep.viz.rsc.ntrans.ncgm;

import java.io.DataInput;
import java.io.IOException;

import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.exception.VizException;

import gov.noaa.nws.ncep.viz.rsc.ntrans.jcgm.TextAlignment;
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
public class NcTextAlignment extends TextAlignment implements INcCommand {


    public NcTextAlignment(int ec, int eid, int l, DataInput in)
            throws IOException {
        super(ec, eid, l, in);
    }

    @Override
    public void contributeToPaintableImage(ImageBuilder ib)
            throws VizException {

        // Map/convert CGM-style text alignments to their IGraphicsTarget
        // equivalents.

        IGraphicsTarget.HorizontalAlignment horizontalAlignment = null;
        IGraphicsTarget.VerticalAlignment verticalAlignment = null;

        switch (this.horizontalAlignment) {
        case NORMAL_HORIZONTAL:
            // TODO: Following is a bit of a hack, to deal with the way
            // legacy NTRANS metafiles are created by the NC driver code.
            // A horizontal alignment of CENTER appears to be coded
            // (intentionally or otherwise) in the legacy generated CGM
            // by a *vertical* alignment value of CAP. Might want to
            // investigate, and possibly bring legacy code to CGM
            // compliance.
            if (this.verticalAlignment == TextAlignment.VerticalAlignment.CAP) {
                horizontalAlignment = IGraphicsTarget.HorizontalAlignment.CENTER;
            } else {
                horizontalAlignment = IGraphicsTarget.HorizontalAlignment.LEFT;
            }
            break;
        case LEFT:
            horizontalAlignment = IGraphicsTarget.HorizontalAlignment.LEFT;
            break;
        case CONTINOUS_HORIZONTAL: // TODO??
        case CENTRE:
            horizontalAlignment = IGraphicsTarget.HorizontalAlignment.CENTER;
            break;
        case RIGHT:
            horizontalAlignment = IGraphicsTarget.HorizontalAlignment.RIGHT;
            break;
        default:
            // TODO fail
            horizontalAlignment = IGraphicsTarget.HorizontalAlignment.CENTER;
            break;
        }

        switch (this.verticalAlignment) {
        case TOP:
        case CAP: // TODO??
            verticalAlignment = IGraphicsTarget.VerticalAlignment.TOP;
            verticalAlignment = IGraphicsTarget.VerticalAlignment.BOTTOM;
            break;
        case HALF:
            verticalAlignment = IGraphicsTarget.VerticalAlignment.MIDDLE;
            break;
        case NORMAL_VERTICAL:
        case CONTINOUS_VERTICAL: // TODO??
        case BASE: // TODO??
        case BOTTOM:
            verticalAlignment = IGraphicsTarget.VerticalAlignment.BOTTOM;
            break;
        default:
            // TODO fail
            verticalAlignment = IGraphicsTarget.VerticalAlignment.BOTTOM;
            break;
        }

        ib.setTextAlignment(horizontalAlignment, verticalAlignment);

    }

}
