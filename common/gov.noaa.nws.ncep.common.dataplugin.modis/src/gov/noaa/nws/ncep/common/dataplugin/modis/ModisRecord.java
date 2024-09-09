/**
 * This class performs mapping to database for MODIS file plug-in.
 *
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date         Ticket#     Engineer     Description
 * ------------ ----------  -----------  --------------------------
 * 10/01/2014   R5116       kbugenhagen  Created
 * 11/07/2018   #7552       dgilling     Removed unused function, implement
 *                                       IGridGeometryProvider.
 * Sep 09, 2024 2037799     njensen      Moved constants from ModisRecord
 * </pre>
 *
 * @author kbugenhagen
 * @version 1.0
 */

package gov.noaa.nws.ncep.common.dataplugin.modis;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.geotools.coverage.grid.GridGeometry2D;
import org.hibernate.annotations.Index;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.annotations.DataURI;
import com.raytheon.uf.common.dataplugin.persist.PersistablePluginDataObject;
import com.raytheon.uf.common.datastorage.DataStoreFactory;
import com.raytheon.uf.common.geospatial.IGridGeometryProvider;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

@Entity
@SequenceGenerator(initialValue = 1, name = PluginDataObject.ID_GEN, sequenceName = "modisseq")
@Table(name = "modis", uniqueConstraints = { @UniqueConstraint(columnNames = { "dataURI" }) })
/*
 * Both refTime and forecastTime are included in the refTimeIndex since
 * forecastTime is unlikely to be used.
 */
@org.hibernate.annotations.Table(appliesTo = "modis", indexes = { @Index(name = "modis_refTimeIndex", columnNames = {
        "refTime", "forecastTime" }) })
@DynamicSerialize
public class ModisRecord extends PersistablePluginDataObject implements
        IGridGeometryProvider {

    public static final String MISSING_VALUE_ID = "missing_value";

    public static final String SCALE_ID = "scale_factor";

    public static final String OFFSET_ID = "add_offset";

    public static final String UNIT_ID = "units";

    private static final String CREATING_ENTITY = "MODIS";

    public String getCreatingEntity() {
        return CREATING_ENTITY;
    }

    /**
     * The default dataset name to use for persisted satellite data.
     */
    public static final String SAT_DATASET_NAME = DataStoreFactory.DEF_DATASET_NAME;

    private static final long serialVersionUID = 4920123282595760202L;

    public static final String LONGITUDE_DATASET_NAME = "longitudes";

    public static final String LATITUDE_DATASET_NAME = "latitudes";

    @Column
    @DataURI(position = 1)
    @DynamicSerializeElement
    private String parameter;

    @Column
    @DataURI(position = 2)
    @DynamicSerializeElement
    private int levels;

    @Column
    @DataURI(position = 3)
    @DynamicSerializeElement
    private Double startTime;

    @ManyToOne
    @PrimaryKeyJoinColumn
    @DynamicSerializeElement
    private ModisSpatialCoverage coverage;

    public ModisRecord() {
    }

    /**
     * @return the coverage
     */
    public ModisSpatialCoverage getCoverage() {
        return coverage;
    }

    /**
     * @param coverage
     *            the coverage to set
     */
    public void setCoverage(ModisSpatialCoverage coverage) {
        this.coverage = coverage;
    }

    /**
     * Get the name of the dataset for the level
     *
     * @param level
     * @return
     */
    public static String getDataSet(int level) {
        return "Data-" + level;
    }

    /**
     * @return the parameter
     */
    public String getParameter() {
        return parameter;
    }

    /**
     * @param parameter
     *            the parameter to set
     */
    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    @Override
    @Column
    @Access(AccessType.PROPERTY)
    public String getDataURI() {
        return super.getDataURI();
    }

    @Override
    public String getPluginName() {
        return "modis";
    }

    public int getLevels() {
        return levels;
    }

    public void setLevels(int levels) {
        this.levels = levels;
    }

    public Double getStartTime() {
        return startTime;
    }

    public void setStartTime(Double startTime) {
        this.startTime = startTime;
    }

    @Override
    public GridGeometry2D getGridGeometry() {
        return coverage.getGridGeometry();
    }
}