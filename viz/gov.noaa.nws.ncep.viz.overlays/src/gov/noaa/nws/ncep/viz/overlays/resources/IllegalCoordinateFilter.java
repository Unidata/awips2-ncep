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
package gov.noaa.nws.ncep.viz.overlays.resources;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateFilter;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;

/**
 * A CoordinateFilter that removes coordinates that are not considered valid. As
 * the filter is called over each Coordinate in the Geometry, it splits the
 * sequences of valid coordinates into separate LineStrings. Each time an
 * invalid coordinate is encountered, the previous LineString is halted and a
 * new LineString will be created upon discovery of the next valid Coordinate.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 04, 2015  10436     njensen     Initial creation
 * 
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public class IllegalCoordinateFilter implements CoordinateFilter {

    private static final GeometryFactory GEOM_FACTORY = new GeometryFactory();

    protected List<List<Coordinate>> lines = new ArrayList<>();

    protected List<Coordinate> currentLine;

    @Override
    public void filter(Coordinate c) {
        if (isValidCoordinate(c)) {
            if (currentLine == null) {
                currentLine = new ArrayList<>();
                lines.add(currentLine);
            }
            currentLine.add(c);
        } else {
            currentLine = null;
        }
    }

    public boolean isValidCoordinate(Coordinate c) {
        return !(Double.isNaN(c.x) || Double.isNaN(c.y)
                || Double.isInfinite(c.x) || Double.isInfinite(c.y));
    }

    /**
     * Builds and returns a MultiLineString of the valid coordinates from the
     * original geometry.
     * 
     * @return a new MultiLineString of valid coordinates
     */
    public MultiLineString getFilterResult() {
        LineString[] lineStrings = new LineString[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            lineStrings[i] = GEOM_FACTORY.createLineString(lines.get(i)
                    .toArray(new Coordinate[0]));
        }
        return GEOM_FACTORY.createMultiLineString(lineStrings);
    }

}
