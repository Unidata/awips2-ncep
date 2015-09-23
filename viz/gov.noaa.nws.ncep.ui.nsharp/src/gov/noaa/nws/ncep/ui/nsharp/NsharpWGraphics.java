package gov.noaa.nws.ncep.ui.nsharp;
import org.eclipse.swt.graphics.Rectangle;
import com.vividsolutions.jts.geom.Coordinate;

public class NsharpWGraphics {

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
    public NsharpWGraphics(double x1, double y1, double x2, double y2) {

        viewXmin = x1;
        viewYmin = y1;
        viewXmax = x2;
        viewYmax = y2;
        setCoordinateMapping();
    }

    public NsharpWGraphics(Rectangle rect) {
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
