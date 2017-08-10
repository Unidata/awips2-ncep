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

import com.raytheon.uf.viz.core.exception.VizException;

import gov.noaa.nws.ncep.viz.rsc.ntrans.rsc.ImageBuilder;

/**
 * INcCommand: Interface implemented by selected CGM command classes that have
 * been extended to allow themselves to contribute to an AWIPS II image.
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
public interface INcCommand {

    public abstract void contributeToPaintableImage(ImageBuilder ib)
            throws VizException;

}
