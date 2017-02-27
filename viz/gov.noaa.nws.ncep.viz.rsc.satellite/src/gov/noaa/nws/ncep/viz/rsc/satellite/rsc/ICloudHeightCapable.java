package gov.noaa.nws.ncep.viz.rsc.satellite.rsc;

import javax.measure.quantity.Temperature;
import javax.measure.unit.Unit;

import org.geotools.coverage.grid.GridGeometry2D;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Provides Mcidas satellite raster rendering support.
 * 
 * Also allows an IR satellite resource to use the Cloud Height Tool, by that
 * resource implementing this interface.
 * 
 * <pre>
 * 
 *  SOFTWARE HISTORY
 * 
 * Date        Ticket#     Engineer    Description
 * ----------  ----------  ----------- --------------------------
 * Unknown                             Initial creation
 * 09/16/2016  R15716      SRussell    added method getGridGeometry()
 * 
 */

public interface ICloudHeightCapable {

    // Is this satellite image an IR image?
    abstract public boolean isCloudHeightCompatible();

    abstract public Double getRawIRImageValue(Coordinate latlon);

    // Returns the Temperature in the Units returned from getDisplayUnits.
    abstract public Double getSatIRTemperature(Coordinate latlon);

    abstract public Unit<Temperature> getTemperatureUnits();

    abstract public GridGeometry2D getGridGeometry();

}