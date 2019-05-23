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
package gov.noaa.nws.ncep.viz.rsc.ncgrid.contours;

import gov.noaa.nws.ncep.viz.rsc.ncgrid.contours.GridProgressiveDisclosureCache.ProgressiveDisclosure;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.rsc.NcgridResourceData;

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.graphics.RGB;
import org.geotools.coverage.grid.GeneralGridGeometry;

import com.raytheon.uf.common.geospatial.ReferencedCoordinate;
import com.raytheon.uf.common.geospatial.ReferencedObject.Type;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.IMapDescriptor;
import org.locationtech.jts.geom.Coordinate;

//import com.raytheon.uf.viz.core.drawables.IRenderable;

/**
 * An abstract resource for displays where each grid cell is an individual
 * IImage. Handles progressive disclosure algorithm.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 23, 2010            bsteffen     Initial creation
 * Nov 22, 2010			   M. Li		modified from RTS for NCGRID
 * Dec 03, 2010			   M. Li		Converted negative longitude
 * Dec 07, 2010			   M. Li		Modified wind plot algorithm
 * Nov 02, 2011            X. Guo       Added nx/ny parameters
 * Feb 06, 2012  #538      Q. Zhou      Changed density to filter. Get filter from resource attribute
 * Dec 11, 2014  R5113     J. Wu        Set default filter to 0.3 for higher grids to speed loading.
 * May 14, 2015  R7058     S. Russell   added a call to save target in paint()
 * Aug 23, 2016  R15955    bsteffen     Use GridProgressiveDisclosure
 * 
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */

public abstract class AbstractGriddedDisplay<T> { // implements IRenderable

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(AbstractGriddedDisplay.class);

    private final Queue<Coordinate> calculationQueue;

    private CalculationJob calculationJob;

    protected final IMapDescriptor descriptor;

    protected final GeneralGridGeometry gridGeometryOfGrid;

    protected final int[] gridDims;

    protected IGraphicsTarget target;

    protected float size = 64;

    protected RGB color;

    protected int skipx;

    protected int skipy;

    protected double filter;

    protected double magnification = 1.0;

    private boolean async = true;

    protected boolean[] isPlotted;

    private ProgressiveDisclosure disclosure;

    /**
     * 
     * @param descriptor
     * @param gridGeometryOfGrid
     * @param size
     */
    public AbstractGriddedDisplay(IMapDescriptor descriptor,
            GeneralGridGeometry gridGeometryOfGrid, int nx, int ny) {

        this.calculationQueue = new ConcurrentLinkedQueue<>();

        this.descriptor = descriptor;
        this.gridGeometryOfGrid = gridGeometryOfGrid;

        // this.size = size;

        this.gridDims = new int[] { nx, ny };

        isPlotted = new boolean[gridDims[0] * gridDims[1]];

        this.disclosure = GridProgressiveDisclosureCache.getDisclosure(
                gridGeometryOfGrid, descriptor.getGridGeometry());
    }

    public void setASync(boolean async) {
        this.async = async;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.viz.core.drawables.IRenderable#paint(com.raytheon.viz.core
     * .IGraphicsTarget, com.raytheon.viz.core.drawables.PaintProperties)
     */
    // @Override
    public void paint(NcgridResourceData gridRscData, IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {

        // Redmine 7058
        this.target = target;

        boolean globalModel = isGlobalModel();

        /**
         * Get filter attribute
         */
        filter = parseFilter(gridRscData.getFilter());

        /**
         * Get skip attribute
         */
        // String noFilterStr = gridRscData.getFilter().trim().toUpperCase();
        // boolean noFilter = (noFilterStr.startsWith("N") || noFilterStr
        // .equals("0")) ? true : false;
        // int[] skipf = parseSkip( gridRscData.getSkip(), true );

        for (int i = 0; i < (gridDims[0] * gridDims[1]); i++)
            isPlotted[i] = false;

        // Controls whether to draw images or debugging output on the map
        // boolean debug = false;
        this.target = target;

        PaintProperties pp = new PaintProperties(paintProps);
        pp.setAlpha(1.0f);

        IExtent viewPixelExtent = paintProps.getView().getExtent();
        double ratio = viewPixelExtent.getWidth()
                / paintProps.getCanvasBounds().width;

        /*
         * 0.58 was chosen to get about the same number of points as a previous
         * disclosure algorithm.
         */
        double interval = size * .58 * ratio * filter;

        double adjSize = size * ratio * magnification;

        Collection<Coordinate> items = disclosure.runDisclosure(
                viewPixelExtent, interval);
        try {
            for (Coordinate item : items) {
                Coordinate gridCell = item;

                T oldImage = getImage(gridCell);
                if (oldImage != null) {
                    if (globalModel) {
                        paintGlobalImage((int) gridCell.x, (int) gridCell.y,
                                pp, adjSize);
                    } else {
                        paintImage((int) gridCell.x, (int) gridCell.y, pp,
                                adjSize);
                    }
                } else {
                    if (async) {
                        if (!this.calculationQueue.contains(gridCell)) {
                            this.calculationQueue.add(gridCell);
                        }
                    } else {
                        T image = createImage(gridCell);
                        if (image != null) {
                            if (globalModel) {
                                paintGlobalImage((int) gridCell.x,
                                        (int) gridCell.y, pp, adjSize);
                            } else {
                                paintImage((int) gridCell.x, (int) gridCell.y,
                                        pp, adjSize);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new VizException("Error occured during paint", e);
        }

        if (calculationQueue.size() > 0) {
            if (this.calculationJob == null) {
                this.calculationJob = new CalculationJob();
                this.calculationJob.schedule();
            } else if (!this.calculationJob.isRunning()) {
                this.calculationJob.schedule();
            }
        }
    }

    /**
     * Should return a cached image if it is available, if this returns null
     * createImage will be called on the same point(possible asynchronously)
     * 
     * @param coord
     * @return
     */
    protected abstract T getImage(Coordinate coord);

    /**
     * Create an image for the given coordinate.
     * 
     * @param coord
     * @return
     * @throws VizException
     */
    protected abstract T createImage(Coordinate coord) throws VizException;

    /**
     * Should dispose of all images and clear a cache. Called whenever the color
     * is changed, or when the display is disposed.
     */
    protected abstract void disposeImages();

    protected abstract void paintImage(int x, int y,
            PaintProperties paintProps, double adjustedSize)
            throws VizException;

    protected abstract void paintGlobalImage(int x, int y,
            PaintProperties paintProps, double adjustedSize)
            throws VizException;

    public void dispose() {
        disposeImages();
    }

    /**
     * Set the color of the images
     * 
     * @param color
     */
    public boolean setColor(RGB color) {
        if (this.color == null || !this.color.equals(color)) {
            this.color = color;
            return true;
        }
        return false;
    }

    /**
     * @param filter
     *            the filter to set. Changed from density.
     */
    public boolean setFilter(double filter) {
        if (this.filter != filter) {
            this.filter = filter;
            return true;
        }
        return false;
    }

    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }

    /**
     * @param magnification
     *            the magnification to set
     */
    public boolean setMagnification(double magnification) {
        if (this.magnification != magnification) {
            this.magnification = magnification;
            return true;
        }
        return false;
    }

    private boolean isGlobalModel() throws VizException {

        ReferencedCoordinate newrco0 = new ReferencedCoordinate(new Coordinate(
                0, 0), this.gridGeometryOfGrid, Type.GRID_CORNER);
        ReferencedCoordinate newrco1 = new ReferencedCoordinate(new Coordinate(
                gridDims[0] - 1, 0), this.gridGeometryOfGrid, Type.GRID_CORNER);
        ReferencedCoordinate newrco2 = new ReferencedCoordinate(new Coordinate(
                1, 0), this.gridGeometryOfGrid, Type.GRID_CORNER);

        try {
            Coordinate latLon0 = newrco0.asLatLon();
            Coordinate latLon1 = newrco1.asLatLon();
            Coordinate latLon2 = newrco2.asLatLon();

            double dx1 = latLon2.x - latLon0.x;
            double dx2 = (360 - latLon1.x) + latLon0.x;

            int dx = (int) Math.round(dx2 / dx1);
            int dlat = (int) Math.round(latLon1.y - latLon0.y);

            if (dx <= 2 && dlat == 0)
                return true;

        } catch (Exception e) {
            throw new VizException(e);
        }

        return false;
    }

    /**
     * Off UI Thread job for calculating the wind images
     * 
     * @author chammack
     * @version 1.0
     */
    private class CalculationJob extends Job {

        private boolean running = false;

        public CalculationJob() {
            super("Grid Image Calculation");
        }

        /*
         * (non-Javadoc)
         * 
         * @seeorg.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.
         * IProgressMonitor)
         */
        @Override
        protected IStatus run(IProgressMonitor monitor) {
            boolean loggedError = false;
            running = true;
            while (!calculationQueue.isEmpty()) {

                Coordinate coord = calculationQueue.remove();

                try {
                    createImage(coord);
                } catch (VizException e) {
                    if (!loggedError) {
                        statusHandler.handle(Priority.PROBLEM,
                                "Generating the grid image failed.", e);
                        loggedError = true;
                    }
                }

            }

            target.setNeedsRefresh(true);
            running = false;
            return Status.OK_STATUS;
        }

        /**
         * @return the running
         */
        public boolean isRunning() {
            return running;
        }

    }

    /*
     * Parse "filter" value.
     * 
     * Note: R5113 - By default, a minimum of 0.1 is enforced. But for dense
     * grids, a minimum filter value of 0.3 is forced to ensure a reasonable
     * loading time.
     */
    private double parseFilter(String filterStr) {
        double fil = 0.0;
        String den = filterStr;
        if (den != null) {
            try {
                if (den.equalsIgnoreCase("YES") || den.equalsIgnoreCase("Y")) {
                    fil = 1.0;
                } else if (den.equalsIgnoreCase("NO")
                        || den.equalsIgnoreCase("N")
                        || den.equalsIgnoreCase("")) {
                    fil = 0.0;
                } else {
                    fil = Double.parseDouble(den);
                }

                if (fil < 0.1)
                    fil = 0.1;
            } catch (NumberFormatException e) {
                System.out.println("The filter is not a double number");
                fil = 1.0;
            }
        } else {
            fil = 1.0;
        }

        // Set a higher miminum filter value of 0.3 for performance.
        if (fil < 0.3 && gridDims[0] * gridDims[1] > 10000) {
            fil = 0.3;
        }

        return fil;

    }

    /*
     * Parse skip factor.
     */
    private int[] parseSkip(String skipStr, boolean noFilter) {

        String[] skip = null;
        int skipx = 0;
        int skipy = 0;

        String skipString = skipStr; // now for positive skip
        // if (skipString != null && noFilter.equalsIgnoreCase("NO")) {
        if (skipString != null && noFilter) {
            int ind = skipString.indexOf("/");
            if (ind != -1) {
                skipString = skipString.substring(ind + 1);

                if (skipString.trim().startsWith("-")) // temp fix for negative
                                                       // value
                    skipString = skipString.substring(1);

                skip = skipString.split(";");

                if (skip != null && skip.length != 0) {
                    try {
                        skipx = Integer.parseInt(skip[0]);
                    } catch (NumberFormatException e) {
                        System.out.println("The skip is not an interger");
                        skipx = 0;
                    }

                    if (skip.length == 1) {
                        skipy = skipx;
                    }

                    if (skip.length > 1 && skip[0] != skip[1]) {
                        try {
                            skipy = Integer.parseInt(skip[1]);
                        } catch (NumberFormatException e) {
                            System.out.println("The skip is not an interger");
                            skipy = skipx;
                        }
                    }
                } else {
                    skipx = 0;
                    skipy = 0;
                }
            } else {
                skipx = 0;
                skipy = 0;
            }
        } else {
            skipx = 0;
            skipy = 0;
        }

        return new int[] { skipx, skipy };
    }

    public void reproject() {
        this.disclosure = GridProgressiveDisclosureCache.getDisclosure(
                gridGeometryOfGrid, descriptor.getGridGeometry());
    }

}
