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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.raytheon.uf.viz.core.drawables.IWireframeShape;

/**
 * Temporary container which holds all the data needed to create an
 * {@link IWireframeShape}. This class allows for two important performance
 * optimizations. First it keeps track of the number of points in the shape so
 * that when an IWireframeShape is created it is possible to use
 * {@link IWireframeShape#allocate(int)} to avoid repeated allocation costs. For
 * very large shapes the cost of reallocation can be significant. The second
 * optimization is that it can be used as a key for the
 * {@link SharedWireframeGenerator} to reuse redundant shapes across multiple
 * frames.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Oct 24, 2016  R22550   bsteffen  Initial creation
 * 
 * </pre>
 *
 * @author bsteffen
 */
public class WireframeShapeBuilder {

    private int pointCount = 0;

    private int hashCode = 1;

    private List<double[][]> segments = new ArrayList<>();

    public void addLineSegment(double[][] segment) {
        segments.add(segment);
        pointCount += segment.length;
        hashCode = 31 * hashCode + Arrays.deepHashCode(segment);
    }

    public void compile(IWireframeShape shape) {
        shape.allocate(pointCount);
        for (double[][] segment : segments) {
            shape.addLineSegment(segment);
        }
        shape.compile();
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        WireframeShapeBuilder other = (WireframeShapeBuilder) obj;
        if (hashCode != other.hashCode)
            return false;
        if (pointCount != other.pointCount)
            return false;
        if (segments == null) {
            if (other.segments != null)
                return false;
        } else if (segments.size() != other.segments.size()) {
            return false;
        } else {
            for (int i = 0; i < segments.size(); i += 1) {
                if (!Arrays.deepEquals(segments.get(i),
                        other.segments.get(i))) {
                    return false;
                }
            }
        }
        return true;
    }

}
