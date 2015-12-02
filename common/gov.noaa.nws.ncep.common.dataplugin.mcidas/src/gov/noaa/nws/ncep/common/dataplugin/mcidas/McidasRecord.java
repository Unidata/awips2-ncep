/**
 * This class performs mapping to database for McIDAS area file plug-in.
 * 
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 08/2009      144         T. Lee      Created
 * 11/2009      144         T. Lee      Implemented area name and  added file
 *                                      name
 * 12/2009      144         T. Lee      Added calType, satelliteId  and
 *                                      imageTypeNumber
 * 05/2010      144         L. Lin      Migration to TO11DR11.
 * 09/2012                  B. Hebbard  Merge out RTS changes from OB12.9.1
 * Apr 04, 2013 1846        bkowal      Added an index on refTime and
 *                                      forecastTime
 * Apr 12, 2013 1857        bgonzale    Added SequenceGenerator annotation.
 * May 07, 2013 1869        bsteffen    Remove dataURI column from
 *                                      PluginDataObject.
 * Aug 30, 2013 2298        rjpeter     Make getPluginName abstract
 * Jun 11, 2014 2061        bsteffen    Remove IDecoderGettable
 * 
 * </pre>
 * 
 * @author tlee
 * @version 1
 */

package gov.noaa.nws.ncep.common.dataplugin.mcidas;

import java.util.Calendar;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Index;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.annotations.DataURI;
import com.raytheon.uf.common.dataplugin.persist.IPersistable;
import com.raytheon.uf.common.dataplugin.persist.PersistablePluginDataObject;
import com.raytheon.uf.common.geospatial.ISpatialEnabled;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

@Entity
@SequenceGenerator(initialValue = 1, name = PluginDataObject.ID_GEN, sequenceName = "mcidasseq")
@Table(name = "mcidas", uniqueConstraints = { @UniqueConstraint(columnNames = {
        "refTime", "satelliteId", "areaId", "resolution", "imageTypeId" }) })
/*
 * Both refTime and forecastTime are included in the refTimeIndex since
 * forecastTime is unlikely to be used.
 */
@org.hibernate.annotations.Table(appliesTo = "mcidas", indexes = { @Index(name = "mcidas_refTimeIndex", columnNames = {
        "refTime", "forecastTime" }) })
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class McidasRecord extends PersistablePluginDataObject implements
        IPersistable, ISpatialEnabled {

    private static final long serialVersionUID = 1L;

    /** The satellite ID */
    @Column(length = 32)
    @DataURI(position = 1)
    @XmlAttribute
    @DynamicSerializeElement
    private String satelliteId;

    /** The area ID */
    @Column(length = 64)
    @DataURI(position = 2)
    @XmlAttribute
    @DynamicSerializeElement
    private String areaId;

    /** The resolution */
    @Column
    @DataURI(position = 3)
    @XmlAttribute
    @DynamicSerializeElement
    private Integer resolution;

    /** The image type */
    @Column(length = 32)
    @DataURI(position = 4)
    @XmlAttribute
    @DynamicSerializeElement
    private String imageTypeId;

    /**
     * The creation time
     */
    @Column
    @XmlAttribute
    @DynamicSerializeElement
    private Calendar creationTime;

    /**
     * The image time
     */
    @Column
    @XmlAttribute
    @DynamicSerializeElement
    private Calendar imageTime;

    /**
     * Size of logical records in bytes for product.
     */
    @Column
    @XmlAttribute
    @DynamicSerializeElement
    private Integer sizeRecords;

    /** Satellite projection */
    @Column(length = 16)
    @XmlAttribute
    @DynamicSerializeElement
    private String projection;

    /** The report type */
    @Column(length = 16)
    @XmlAttribute
    @DynamicSerializeElement
    private String reportType;

    /** The calibration type */
    @Column(length = 16)
    @XmlAttribute
    @DynamicSerializeElement
    private String calType;

    /*
     * Length of prefix in bytes.
     */
    private Integer prefix;

    /*
     * Validation code. if these bytes are non-zero, they must match the first
     * four bytes of each DATA block line prefix or the line's data is ignored.
     */
    private Integer validCode;

    /*
     * File name ingested to the end point.
     */
    private String inputFileName;

    @ManyToOne
    @PrimaryKeyJoinColumn
    @XmlElement
    @DynamicSerializeElement
    private McidasMapCoverage coverage;

    /** Area file header block */
    @Transient
    private byte[] headerBlock;

    @Override
    public McidasMapCoverage getSpatialObject() {
        return coverage;
    }

    public McidasMapCoverage getCoverage() {
        return coverage;
    }

    public void setCoverage(McidasMapCoverage coverage) {
        this.coverage = coverage;
    }

    /**
     * No-arg constructor.
     */
    public McidasRecord() {
        satelliteId = null;
        imageTypeId = null;
        resolution = null;
        projection = null;
        imageTime = null;
        areaId = null;
    }

    /**
     * Constructs a McIDAS satellite record from a dataURI
     * 
     * @param uri
     *            The dataURI
     */
    public McidasRecord(String uri) {
        super(uri);
    }

    /**
     * Set the time to be used for the persistence time for this object.
     * 
     * @param persistTime
     *            The persistence time to be used.
     */
    public void setPersistenceTime(Calendar persistTime) {
        setInsertTime(persistTime);
    }

    public Integer getSizeRecords() {
        return sizeRecords;
    }

    public void setSizeRecords(Integer sizeRecords) {
        this.sizeRecords = sizeRecords;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getSatelliteId() {
        return satelliteId;
    }

    public Calendar getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Calendar creationTime) {
        this.creationTime = creationTime;
    }

    public Calendar getImageTime() {
        return imageTime;
    }

    public void setImageTime(Calendar imageTime) {
        this.imageTime = imageTime;
    }

    public String getProjection() {
        return projection;
    }

    public void setProjection(String projection) {
        this.projection = projection;
    }

    public void setSatelliteId(String satelliteId) {
        this.satelliteId = satelliteId;
    }

    public String getImageTypeId() {
        return imageTypeId;
    }

    public void setImageTypeId(String imageTypeId) {
        this.imageTypeId = imageTypeId;
    }

    public Integer getResolution() {
        return resolution;
    }

    public void setResolution(Integer resolution) {
        this.resolution = resolution;
    }

    public byte[] getHeaderBlock() {
        return headerBlock;
    }

    public void setHeaderBlock(byte[] headerBlock) {
        this.headerBlock = headerBlock;
    }

    public String getAreaId() {
        return areaId;
    }

    public void setAreaId(String areaId) {
        this.areaId = areaId;
    }

    public Integer getPrefix() {
        return prefix;
    }

    public void setPrefix(Integer prefix) {
        this.prefix = prefix;
    }

    public Integer getValidCode() {
        return validCode;
    }

    public void setValidCode(Integer validCode) {
        this.validCode = validCode;
    }

    public String getInputFileName() {
        return inputFileName;
    }

    public void setInputFileName(String inputFileName) {
        this.inputFileName = inputFileName;
    }

    public String getCalType() {
        return calType;
    }

    public void setCalType(String calType) {
        this.calType = calType;
    }

    @Override
    public String getPluginName() {
        return "mcidas";
    }
}