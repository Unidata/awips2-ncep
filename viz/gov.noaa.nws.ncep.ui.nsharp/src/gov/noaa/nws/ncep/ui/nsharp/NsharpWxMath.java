package gov.noaa.nws.ncep.ui.nsharp;
import org.eclipse.swt.graphics.Rectangle;
import org.locationtech.jts.geom.Coordinate;

/**
 * 
 * gov.noaa.nws.ncep.ui.nsharp.NsharpWxMath
 * 
 * This java class performs the surface station locator functions.
 * This code has been developed by the SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    	Engineer    Description
 * -------		------- 	-------- 	-----------
 * 03/13/2012   			Chin Chen	Initial coding, port from WxMath class
 * 										using Nsharp own configured tempOffset
 *
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 */

public class NsharpWxMath {
	public static int tempOffset = 0;
	
	public static void setTempOffset(int tempOffset) {
		NsharpWxMath.tempOffset = tempOffset;
	}

	/**
     * Convert a pressure and temperature to a skew-t x,y coordinate in
     * centimeters where 0,0 occurs at 1000 hPa and 0 degrees Celsius.
     * 
     * @param pressure
     *            The pressure in hectoPascals (millibars).
     * @param temperature
     *            The temperature in degrees Celsius.
     * @return The calculated coordinate in centimeters.
     */
    public static final Coordinate getSkewTXY(double pressure,
            double temperature) {
        temperature -= tempOffset;
        Coordinate point = new Coordinate();

        point.y = 132.182 - 44.061 * Math.log10(pressure);
        point.x = (0.54 * temperature) + (0.90692 * point.y);

        return point;
    }

    /**
     * Reverse a skewT coordinate (in centimeters) to the corresponding
     * temperature and pressure.
     * 
     * @param point
     * @return The temperature and pressure. coordinate.x = temperature in
     *         Celsius, coordinate.y = the pressure in hectoPascals (millibars).
     */
    public static final Coordinate reverseSkewTXY(Coordinate point) {
        Coordinate tempPressure = new Coordinate();
        tempPressure.y = Math.pow(10, ((point.y - 132.182) / -44.061));
        tempPressure.x = (point.x - (0.90692 * point.y)) / 0.54;

        tempPressure.x += tempOffset;
        
        return tempPressure;
    }
    
    /*
     * Get pressure Y coordinate from available temp and temp's X coordinate
     */
    public static double getPressureYFromTemp(double temp, double tempX) {
    	double pressureY;
    	temp -= tempOffset;  //TT605593
    	pressureY = (tempX - (temp * 0.54))/0.90692;
    	return pressureY;
    }
}
 class WGraphics {

    private double worldXmin = -1;

    /**
     * @return the worldXmin
     */
    public double getWorldXmin() {
        return worldXmin;
    }

    /**
     * @return the worldYmin
     */
    public double getWorldYmin() {
        return worldYmin;
    }

    /**
     * @return the worldXmax
     */
    public double getWorldXmax() {
        return worldXmax;
    }

    /**
     * @return the worldYmax
     */
    public double getWorldYmax() {
        return worldYmax;
    }

    private double worldYmin = 1;

    private double worldXmax = 1;

    private double worldYmax = -1;

    private double xk1;

    private double yk1;

    private double viewXmin;

    private double viewYmin;

    private double viewXmax;

    private double viewYmax;

    // private IGraphicsTarget graphicsContext;

    /**
     * Create a World coordinates graph
     * 
     * @param x1
     *            Upper left output x.
     * @param y1
     *            Upper left output y.
     * @param x2
     *            Lower right output x.
     * @param y2
     *            Lower right output y.
     * @param graphContext
     *            The graphics target that defines the output.
     */
    public WGraphics(double x1, double y1, double x2, double y2) {

        viewXmin = x1;
        viewYmin = y1;
        viewXmax = x2;
        viewYmax = y2;
        setCoordinateMapping();
    }

    public WGraphics(Rectangle rect) {
        this(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height);
    }

    /**
     * Calculate the scaling factors for the x,y mapping.
     */
    private void setCoordinateMapping() {
        xk1 = (viewXmax - viewXmin) / (worldXmax - worldXmin);
        yk1 = (viewYmax - viewYmin) / (worldYmax - worldYmin);
    }

    public void setWorldCoordinates(double x1, double y1, double x2, double y2) {
        worldXmin = x1;
        worldYmin = y1;
        worldXmax = x2;
        worldYmax = y2;
        setCoordinateMapping();
    }

    /**
     * Map a world value to its viewport coordinate
     * 
     * @param x
     *            The world x value.
     * @return The viewport x value.
     */
    public double mapX(double x) {
        return viewXmin + (x - worldXmin) * xk1;
    } // mapX()

    /**
     * Map a world value to its viewport coordinate
     * 
     * @param y
     *            The world y value.
     * @return The viewport y value.
     */
    public double mapY(double y) {
        return viewYmin + (y - worldYmin) * yk1;
    } // mapY()

    /**
     * Map a world coordinate to its viewport coordinate
     * 
     * @param cIn
     *            the world coordinate
     * @return the viewport coordinate
     */
    public Coordinate map(Coordinate cIn) {
        Coordinate cOut = new Coordinate();

        cOut.x = mapX(cIn.x);
        cOut.y = mapY(cIn.y);

        return cOut;
    }

    /**
     * Take an viewport coordinate and map back to the corresponding world
     * coordinate values using the current view.
     * 
     * @param the
     *            viewport coordinate
     * @return the world coordinate
     */
    public Coordinate unMap(Coordinate cIn) {
        return unMap(cIn.x, cIn.y);
    }

    /**
     * Take an output x,y position and map back to the corresponding world
     * coordinate values using the current view.
     * 
     * @param xPos
     *            The viewport x value.
     * @param yPos
     *            The viewport y value.
     * @return The unmapped coordinate.
     */
    public Coordinate unMap(double xPos, double yPos) {
        Coordinate dataPoint = new Coordinate();

        dataPoint.x = ((xPos - viewXmin) / xk1) + worldXmin;
        dataPoint.y = ((yPos - viewYmin) / yk1) + worldYmin;

        return dataPoint;
    }

    public double getViewXmin() {
        return viewXmin;
    }

    public double getViewYmin() {
        return viewYmin;
    }

    public double getViewXmax() {
        return viewXmax;
    }

    public double getViewYmax() {
        return viewYmax;
    }
}