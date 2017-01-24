/**
 * 
 */
package gov.noaa.nws.ncep.viz.rsc.ntrans.ncgm;

import java.io.DataInput;
import java.io.IOException;

import com.raytheon.uf.viz.core.DrawableString;
import com.raytheon.uf.viz.core.exception.VizException;

import gov.noaa.nws.ncep.viz.rsc.ntrans.jcgm.Text;
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
public class NcText extends Text implements INcCommand {

    public NcText(int ec, int eid, int l, DataInput in) throws IOException {
        // To handle little-endian strings, we need to bump an odd length
        // ("l") parameter up one to make it even (ending on two-byte CGM
        // word boundary), so that we get the last character. (Will be
        // flipped into place later.) Note that special case of l=31
        // indicates "long form" (string length >=31 char, to be specified
        // in following 2-byte integer), so the parent constructor for
        // Command has also been modified to interpret l=32 fed up to
        // it as a signal to handle as l=31, then "bump" the long-form
        // length it reads (from next 2 bytes) up to even value if needed.
        super(ec, eid, (l + 1) / 2 * 2, in);
    }

    @Override
    public void contributeToPaintableImage(ImageBuilder ib)
            throws VizException {

        // TODO: Why currentLineColor and not currentTextColor? Legacy quirk?
        DrawableString ds = new DrawableString(this.string,
                ib.getCurrentLineColor());
        double[] newpoint = new double[] { 0.0, 0.0 };

        newpoint = ib.scalePoint(this.position.x, this.position.y);

        ds.setCoordinates(newpoint[0], newpoint[1]);

        ds.font = ib.getCurrentFont();
        ds.horizontalAlignment = ib.getHorizontalAlignment();
        ds.verticallAlignment = ib.getVerticalAlignment();

        ib.addString(ds);

    }

    public void flipString() {
        // Flip every even char with its odd sibling (endianess reversal)
        String oldString = this.string;
        char[] oldCharArray = oldString.toCharArray();
        // if odd length, discard last character (null)
        int lengthOfNewArray = oldCharArray.length / 2 * 2;
        char[] newCharArray = new char[lengthOfNewArray];
        for (int i = 0; i < lengthOfNewArray; i = i + 2) {
            newCharArray[i] = oldCharArray[i + 1];
            newCharArray[i + 1] = oldCharArray[i];
        }
        String newString = new String(newCharArray);
        this.string = newString.trim();
    }

}
