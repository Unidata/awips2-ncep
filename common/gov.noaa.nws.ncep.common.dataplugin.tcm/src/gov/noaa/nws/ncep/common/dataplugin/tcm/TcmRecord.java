package gov.noaa.nws.ncep.common.dataplugin.tcm;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.annotations.DataURI;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

import gov.noaa.nws.ncep.common.tools.IDecoderConstantsN;

/**
 * TcmRecord
 *
 * This java class performs the mapping to the database tables for TCM.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket# Engineer   Description
 * ------------ ------- ---------- ---------------------------------
 * 06/2009      128     T. Lee     Initial coding
 * 07/2009      128     T. Lee     Migrated to TO11
 * 11/2009      128     T. Lee     Migrated to TO11D6
 * 09/2011              Chin Chen  changed to improve purge performance and
 *                                 removed xml serialization as well
 * 07/2012      606     Greg Hull  added reportType to the dataURI
 * Apr 04, 2013 1846    bkowal     Added an index on refTime and forecastTime
 * Apr 12, 2013 1857    bgonzale   Added SequenceGenerator annotation.
 * May 07, 2013 1869    bsteffen   Remove dataURI column from PluginDataObject.
 * Aug 30, 2013 2298   rjpeter Make getPluginName abstract
 * Feb 11, 2014 2784    rferrel    Remove override of setIdentifier.
 * Jun 11, 2014 2061    bsteffen   Remove IDecoderGettable
 * Jan 25, 2019 7717    ksunil     Remove 8000 char limitation from bullMessage
 * Mar 06, 2019 6140    tgurney    Hibernate 5 @Index fix
 * Jun 16, 2021 8479    tgurney    Fix bullmessage data type
 * 
 * </pre>
 *
 * @author T.Lee
 */
@Entity
@SequenceGenerator(initialValue = 1, name = PluginDataObject.ID_GEN,
        sequenceName = "tcmseq")
/*
 * Both refTime and forecastTime are included in the refTimeIndex since
 * forecastTime is unlikely to be used.
 */
@Table(name = "tcm",
        uniqueConstraints = { @UniqueConstraint(columnNames = { "dataURI" }) },
        indexes = { @Index(name = "tcm_refTimeIndex",
                columnList = "refTime,forecastTime") })
@DynamicSerialize
public class TcmRecord extends PluginDataObject {

    private static final long serialVersionUID = 1L;

    /** Report type */
    @Column(length = 32)
    @DataURI(position = 6)
    @DynamicSerializeElement
    private String reportType;

    /** Storm name */
    @Column(length = 32)
    @DataURI(position = 2)
    @DynamicSerializeElement
    private String stormName;

    /** Tropical storm basin */
    @Column(length = 8)
    @DataURI(position = 1)
    @DynamicSerializeElement
    private String basin;

    /** Storm number */
    @Column(length = 8)
    @DataURI(position = 3)
    @DynamicSerializeElement
    private String stormNumber;

    /** Advisory number */
    @Column(length = 8)
    @DataURI(position = 4)
    @DynamicSerializeElement
    private String advisoryNumber;

    /** Correction flag */
    @Column
    @DataURI(position = 5)
    @DynamicSerializeElement
    private Boolean corr;

    /** Bulletin insurance time */
    @Column
    @DynamicSerializeElement
    private Calendar issueTime;

    /** Storm observation time **/
    @Column
    @DynamicSerializeElement
    private Calendar obsTime;

    /** Storm type */
    @Column(length = 32)
    @DynamicSerializeElement
    private String stormType;

    /** Eye size */
    @Column
    @DynamicSerializeElement
    private Integer eyeSize;

    /** Minimum central pressure */
    @Column
    @DynamicSerializeElement
    private Integer centralPressure;

    /** Position accuracy */
    @Column
    @DynamicSerializeElement
    private Integer positionAccuracy;

    /** Twelve-foot wave height radii at the NE quadrant */
    @Column
    @DynamicSerializeElement
    private String ne12ft;

    /** Twelve-foot wave height radii at the SE quadrant */
    @Column
    @DynamicSerializeElement
    private String se12ft;

    /** Twelve-foot wave height radii at the SW quadrant */
    @Column
    @DynamicSerializeElement
    private String sw12ft;

    /** Twelve-foot wave height radii at the NW quadrant */
    @Column
    @DynamicSerializeElement
    private String nw12ft;

    /** Mass news disseminator (MND) */
    @Column(length = 72)
    @DynamicSerializeElement
    private String mndTime;

    /** Bulletin messages */
    @Column(columnDefinition="text")
    @DynamicSerializeElement
    private String bullMessage;

    /** TCM position and winds */
    @DynamicSerializeElement
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "parentID", nullable = false)
    private Set<TcmPositionWinds> tcmPosWinds = new HashSet<>();

    public TcmRecord() {
        basin = null;
        stormName = null;
        stormNumber = null;
        issueTime = null;
        obsTime = null;
        corr = false;
        mndTime = null;
        advisoryNumber = null;
        centralPressure = IDecoderConstantsN.INTEGER_MISSING;
        positionAccuracy = IDecoderConstantsN.INTEGER_MISSING;
        ne12ft = null;
        se12ft = null;
        sw12ft = null;
        nw12ft = null;
        bullMessage = null;
    }

    /**
     * Constructs a TCM record from a dataURI
     *
     * @param uri
     *            the dataURI
     */
    public TcmRecord(String uri) {
        super(uri);
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    /**
     * Return the basin, e.g., AL/EP
     */
    public String getBasin() {
        return basin;
    }

    /**
     * @param basin
     *            the basin name to set
     */
    public void setBasin(String basin) {
        this.basin = basin;
    }

    public String getStormName() {
        return stormName;
    }

    public void setStormName(String stormName) {
        this.stormName = stormName;
    }

    public String getStormNumber() {
        return stormNumber;
    }

    public void setStormNumber(String stormNumber) {
        this.stormNumber = stormNumber;
    }

    public String getAdvisoryNumber() {
        return advisoryNumber;
    }

    public void setAdvisoryNumber(String advisoryNumber) {
        this.advisoryNumber = advisoryNumber;
    }

    /**
     * Return the correction flag
     */
    public Boolean getCorr() {
        return corr;
    }

    /**
     * @param corr
     *            the correction flag to set
     */
    public void setCorr(Boolean corr) {
        this.corr = corr;
    }

    public String getStormType() {
        return stormType;
    }

    public void setStormType(String stormType) {
        this.stormType = stormType;
    }

    public Integer getEyeSize() {
        return eyeSize;
    }

    public void setEyeSize(Integer eyeSize) {
        this.eyeSize = eyeSize;
    }

    /**
     * Return the minimum central pressure
     */
    public Integer getCentralPressure() {
        return centralPressure;
    }

    /**
     * @param centralPressure
     *            the minimum central pressure to set
     */
    public void setCentralPressure(Integer centralPressure) {
        this.centralPressure = centralPressure;
    }

    public Integer getPositionAccuracy() {
        return positionAccuracy;
    }

    public void setPositionAccuracy(Integer positionAccuracy) {
        this.positionAccuracy = positionAccuracy;
    }

    /**
     * Return the twelve-foot wave height radii at the NE quadrant
     */
    public String getNe12ft() {
        return ne12ft;
    }

    /**
     * @param ne12ft
     *            the twelve-foot wave height radii at the NE quadrant to set
     */
    public void setNe12ft(String ne12ft) {
        this.ne12ft = ne12ft;
    }

    /**
     * Return the twelve-foot wave height radii at the SE quadrant
     */
    public String getSe12ft() {
        return se12ft;
    }

    /**
     * @param se12ft
     *            the twelve-foot wave height radii at the SE quadrant to set
     */
    public void setSe12ft(String se12ft) {
        this.se12ft = se12ft;
    }

    /**
     * Return the twelve-foot wave height radii at the SW quadrant
     */
    public String getSw12ft() {
        return sw12ft;
    }

    /**
     * @param sw12ft
     *            The twelve-foot wave height radii at the SW quadrant to set
     */
    public void setSw12ft(String sw12ft) {
        this.sw12ft = sw12ft;
    }

    /**
     * Return the twelve-foot wave height radii at the NW quadrant
     */
    public String getNw12ft() {
        return nw12ft;
    }

    /**
     * @param nw12ft
     *            The twelve-foot wave height radii at the NW quadrant to set
     */
    public void setNw12ft(String nw12ft) {
        this.nw12ft = nw12ft;
    }

    public Calendar getIssueTime() {
        return issueTime;
    }

    public void setObsTime(Calendar obsTime) {
        this.obsTime = obsTime;
    }

    public Calendar getObsTime() {
        return obsTime;
    }

    public void setIssueTime(Calendar issueTime) {
        this.issueTime = issueTime;
    }

    public String getBullMessage() {
        return bullMessage;
    }

    public void setBullMessage(String bullMessage) {
        this.bullMessage = bullMessage;
    }

    public String getMndTime() {
        return mndTime;
    }

    public void setMndTime(String mndTime) {
        this.mndTime = mndTime;
    }

    public Set<TcmPositionWinds> getTcmPosWinds() {
        return tcmPosWinds;
    }

    public void setTcmPosWinds(Set<TcmPositionWinds> tcmPosWinds) {
        this.tcmPosWinds = tcmPosWinds;
    }

    /**
     * Add TcmPosWinds to set
     */
    public void addPosWinds(TcmPositionWinds poswinds) {
        tcmPosWinds.add(poswinds);

    }

    @Override
    @Column
    @Access(AccessType.PROPERTY)
    public String getDataURI() {
        return super.getDataURI();
    }

    @Override
    public String getPluginName() {
        return "tcm";
    }
}
