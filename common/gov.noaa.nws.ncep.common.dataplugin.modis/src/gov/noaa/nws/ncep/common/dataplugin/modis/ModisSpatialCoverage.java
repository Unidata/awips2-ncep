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
package gov.noaa.nws.ncep.common.dataplugin.modis;

import gov.noaa.nws.ncep.common.dataplugin.modis.projection.ModisMapProjectionFactory;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.GeneralEnvelope;
import org.hibernate.annotations.Index;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.dataplugin.persist.PersistableDataObject;
import com.raytheon.uf.common.geospatial.IGridGeometryProvider;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.time.DataTime;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Modis geographic data record object
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 1, 2014             kbugenhagen Initial creation
 * 
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1.0
 */

@Entity
@Table(name = "modis_spatial")
/*
 * Both refTime and forecastTime are included in the refTimeIndex since
 * forecastTime is unlikely to be used.
 */
@org.hibernate.annotations.Table(appliesTo = "modis_spatial", indexes = { @Index(name = "modis_spatial_refTimeIndex", columnNames = {
        "refTime", "forecastTime" }) })
@DynamicSerialize
public class ModisSpatialCoverage extends PersistableDataObject implements
        IGridGeometryProvider {

    private static final long serialVersionUID = -2532225158997059309L;

    public static final Double HORIZONTAL_RESOLUTION = 1000.0;

    public static final Double VERTICAL_RESOLUTION = 1000.0;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @DynamicSerializeElement
    private int gid;

    @DynamicSerializeElement
    @Embedded
    private DataTime dataTime;

    /** The horizontal resolution of the grid */
    @Transient
    private final Double dx = HORIZONTAL_RESOLUTION;

    /** The vertical resolution of the grid */
    @Transient
    private final Double dy = VERTICAL_RESOLUTION;

    @Transient
    private float[] latitudes;

    @Transient
    private float[] longitudes;

    /** Number of points along the x-axis */
    @DynamicSerializeElement
    @Column
    private Integer nx;

    /** Number of points along the y-axis */
    @DynamicSerializeElement
    @Column
    private Integer ny;

    @Transient
    private CoordinateReferenceSystem crs;

    @Column
    @DynamicSerializeElement
    private Geometry envelope;

    private void logDuration(long startTime, String method) {
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000;
        if (duration > 0) {
            System.out.println(method + " took: " + duration + " ms");
        }
    }

    public CoordinateReferenceSystem getCrs() {
        if (crs == null) {
            try {
                crs = ModisMapProjectionFactory.construct(this, latitudes,
                        longitudes);
            } catch (FactoryException e) {
                crs = null;
            }
        }
        return crs;
    }

    public GridGeometry2D getGridGeometry(Object latitudes, Object longitudes) {

        // long startTime = System.nanoTime();

        if (crs == null) {
            this.latitudes = (float[]) latitudes;
            this.longitudes = (float[]) longitudes;
        }
        double width = getNx() * getDx();
        double[] lowRange = new double[] { 0, 0 };
        double[] highRange = new double[] { width, getNy() * getDy() };

        GeneralEnvelope env = new GeneralEnvelope(lowRange, highRange);
        env.setCoordinateReferenceSystem(getCrs());

        // logDuration(startTime, "ModisSpatialCoverage.getGridGeometry");

        return new GridGeometry2D(new GeneralGridEnvelope(new int[] { 0, 0 },
                new int[] { getNx(), getNy() }, false), env);
    }

    public Integer getNx() {
        return nx;
    }

    public Integer getNy() {
        return ny;
    }

    public int getGid() {
        return gid;
    }

    public void setGid(int gid) {
        this.gid = gid;
    }

    public DataTime getDataTime() {
        return dataTime;
    }

    public void setDataTime(DataTime dataTime) {
        this.dataTime = dataTime;
    }

    public void setNx(Integer nx) {
        this.nx = nx;
    }

    public void setNy(Integer ny) {
        this.ny = ny;
    }

    public Double getDx() {
        return dx;
    }

    public Double getDy() {
        return dy;
    }

    @Override
    public GridGeometry2D getGridGeometry() {
        return null;
    }

    public float[] getLatitudes() {
        return latitudes;
    }

    public void setLatitudes(float[] latitudes) {
        this.latitudes = latitudes;
    }

    public float[] getLongitudes() {
        return longitudes;
    }

    public void setLongitudes(float[] longitudes) {
        this.longitudes = longitudes;
    }

    public Geometry getEnvelope() {
        return envelope;
    }

    public void setEnvelope(Geometry envelope) {
        this.envelope = envelope;
    }

}
