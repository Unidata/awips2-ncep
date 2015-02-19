/**
 *  Interface INcCommand
 *  
 *  Most classes in this package extend corresponding classes in the
 *  open source "jcgm" package, to implement this interface.
 *  
 *  This gives them the ability to draw themselves in the AWIPS II
 *  (IGraphicsTarget) world.  In many cases, instead of drawing 
 *  themselves immediately, they contribute themselves to an image
 *  being built, whose state is kept in the ImageBuilder passed in.
 */
package gov.noaa.nws.ncep.viz.rsc.ntrans.ncgm;

import gov.noaa.nws.ncep.viz.rsc.ntrans.rsc.ImageBuilder;

import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;

/**
 * INcCommand: Interface implemented by selected CGM command classes that have
 * been extended to allow themselves to contribute to an AWIPS II image.
 * 
 * @author bhebbard
 * 
 */

public interface INcCommand {

    public abstract void contributeToPaintableImage(ImageBuilder ib,
            IGraphicsTarget target, PaintProperties paintProps,
            IDescriptor descriptor) throws VizException;

}
