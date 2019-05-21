package gov.noaa.nws.ncep.viz.rsc.plotdata.rsc;


import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.raytheon.uf.viz.core.DrawableBasics;
import com.raytheon.viz.pointdata.PlotInfo;
import com.vividsolutions.jts.geom.Coordinate;

import gov.noaa.nws.ncep.edex.common.metparameters.AbstractMetParameter;
import gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels.NcPlotImageCreator.Position;

/**
 * Station
 * 
 * Class which wraps a PlotInfo object (representing one location at one data
 * time) to store further information such as the actual observed parameter
 * data, progressive disclosure parameters, etc.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 11/17/2015    R9579     bhebbard    Moved out on its own.  Formerly an
 *                                     inner class of NcPlotResource2.
 * 
 * </pre>
 * 
 * @author bhebbard
 * @version 1.0
 */
public class Station {

    public PlotInfo info;

    public Double distanceValue;

    public Double originalDistanceValue;

    public Coordinate pixelLocation;

    public Integer goodnessValue;

    public double[] projCoords;

    public Set<AbstractMetParameter> parametersToPlot;

    public Set<AbstractMetParameter> conditionalColorParameters;

    public Map<Position, DrawableBasics> positionToLocationMap = null;

    public Station() {
        parametersToPlot = new HashSet<>(0);
        conditionalColorParameters = new HashSet<>(0);
        positionToLocationMap = new EnumMap<>(
                Position.class);
    }

}