package gov.noaa.nws.ncep.ui.pgen.display;

import gov.noaa.nws.ncep.ui.pgen.display.IText.DisplayType;

import java.util.ArrayList;
import java.util.List;

import com.raytheon.uf.viz.core.DrawableString;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.TextStyle;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;

/**
 * TextDisplayElement
 * 
 * Contains information needed to readily display "text" information to a
 * graphics target.
 * <P>
 * Objects of this class are typically created from PGEN "drawable elements"
 * using the DisplayElementFactory class.
 * 
 * 
 * SOFTWARE HISTORY
 * 
 * <pre>
 * Date        Ticket#      Engineer    Description 
 * --------    --------     --------    ----------- 
 * 01/23/2009                           Initial Creation. 
 * ??????????  TTR 737      dsushon     is dependent on a code-fix from 
 *                                      Raytheon, the class GLTarget needs a 
 *                                      mask flag implemented
 * 03/07/2016  R15939       SRussell    Replaced deprecated method calls, and
 *                                      obsolete code in the switch in draw()
 * </pre>
 * 
 * @author sgilbert
 */

public class TextDisplayElement implements IDisplayable {

    private boolean hasBackgroundMask;

    private DisplayType displayType;

    private DrawableString dstring;

    /**
     * 
     * @param dstring
     * @param mask
     * @param dType
     * @param box
     */
    public TextDisplayElement(DrawableString dstring, boolean mask,
            DisplayType dType, IExtent box) {

        this.hasBackgroundMask = mask;
        this.displayType = dType;
        this.dstring = dstring;
    }

    /**
     * Disposes any graphic resources held by this object.
     * 
     * @see gov.noaa.nws.ncep.ui.pgen.display.IDisplayable#dispose()
     */
    @Override
    public void dispose() {
        dstring.font.dispose();
    }

    /**
     * Draws the text strings to the specified graphics target
     * 
     * @param target
     *            Destination graphics target
     * @see gov.noaa.nws.ncep.ui.pgen.display.IDisplayable#draw(com.raytheon.viz.core.IGraphicsTarget)
     */
    @Override
    public void draw(IGraphicsTarget target, PaintProperties paintProps) {

        List<DrawableString> listDrawableStrings = new ArrayList<DrawableString>();

        try {

            switch (displayType) {

            case BOX: {
                dstring.addTextStyle(TextStyle.BOXED);
                break;
            }

            case OVERLINE: {
                dstring.addTextStyle(TextStyle.OVERLINE);
                break;
            }

            case UNDERLINE: {
                dstring.addTextStyle(TextStyle.UNDERLINE);
                break;
            }

            case NORMAL:
            default: {
                /*-
                 * case NORMAL:
                 * TextStyle.Normal is indicated by adding no other styles to a
                 * {@link DrawableString}
                 */
                break;
            }

            }// end switch

            // If the text string is to have a blank background behind it
            if (hasBackgroundMask) {
                dstring.addTextStyle(TextStyle.BLANKED);
            }

            listDrawableStrings.add(dstring);
            target.drawStrings(listDrawableStrings);

        } catch (VizException ve) {
            ve.printStackTrace();
        }

    }

}
