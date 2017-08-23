/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package gov.noaa.nws.ncep.viz.rsc.ntrans.wireframe;

import org.eclipse.swt.graphics.RGB;

import com.raytheon.uf.viz.core.drawables.IWireframeShape;

/**
 * An object of this class forms a unique key to an {@link IWireframeShape}
 * which can be drawn in a single operation. As such, it contains as fields all
 * characteristics that must be held constant in a single such wireframe draw
 * operation.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ----------------------------
 * Oct 24, 2016  R22550   bsteffen  Extracted from ImageBuilder
 * 
 * </pre>
 *
 * @author bsteffen
 */
public class WireframeKey {

    public RGB color;

    public double width;

    public WireframeKey(RGB color, double width) {
        this.color = color;
        this.width = width;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((color == null) ? 0 : color.hashCode());
        long temp;
        temp = Double.doubleToLongBits(width);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        WireframeKey other = (WireframeKey) obj;
        if (color == null) {
            if (other.color != null)
                return false;
        } else if (!color.equals(other.color))
            return false;
        if (Double.doubleToLongBits(width) != Double
                .doubleToLongBits(other.width))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return (color.toString() + " Line Width " + width);
    }
}