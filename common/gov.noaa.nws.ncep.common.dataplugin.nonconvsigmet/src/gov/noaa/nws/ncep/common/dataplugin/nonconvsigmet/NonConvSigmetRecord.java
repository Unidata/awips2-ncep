package gov.noaa.nws.ncep.common.dataplugin.nonconvsigmet;

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
 * NonConvsigmetRecord
 *
 * This java class performs the mapping to the database table for NONCONVSIGMET
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket Engineer     Description
 * ------------ ------ --------     -------------------------------------
 * 06/2009             Uma Josyula  Initial creation
 * 09/2011             Chin Chen    changed to improve purge performance and
 *                                  removed xml serialization as well
 * Apr 4, 2013  1846   bkowal       Added an index on refTime and forecastTime
 * Apr 12, 2013 1857   bgonzale     Added SequenceGenerator annotation.
 * May 07, 2013        bsteffen     Remove dataURI column from PluginDataObject.
 * Feb 11, 2014 2784   rferrel      Remove override of setIdentifier.
 * Jun 11, 2014 2061   bsteffen     Remove IDecoderGettable
 * Mar 06, 2019 6140   tgurney      Hibernate 5 @Index fix
 *
 * </pre>
 *
 * This code has been developed by the SIB for use in the AWIPS2 system.
 */

@Entity
@SequenceGenerator(initialValue = 1, name = PluginDataObject.ID_GEN,
        sequenceName = "nonconvsigmetseq")
/*
 * Both refTime and forecastTime are included in the refTimeIndex since
 * forecastTime is unlikely to be used.
 */
@Table(name = "nonconvsigmet",
        uniqueConstraints = { @UniqueConstraint(columnNames = { "dataURI" }) },
        indexes = { @Index(name = "nonconvsigmet_refTimeIndex",
                columnList = "refTime,forecastTime") })
@DynamicSerialize
public class NonConvSigmetRecord extends PluginDataObject {

    private static final long serialVersionUID = 1L;

    // reportType is "non-convective sigmet".
    @Column(length = 32)
    @DataURI(position = 5)
    @DynamicSerializeElement
    private String reportType;

    // WMO header
    @Column(length = 32)
    @DynamicSerializeElement
    private String wmoHeader;

    // forecastRegion as: SL
    @Column(length = 8)
    @DataURI(position = 2)
    @DynamicSerializeElement
    private String forecastRegion;

    // The issue office where the report from
    @Column(length = 32)
    @DataURI(position = 1)
    @DynamicSerializeElement
    private String issueOffice;

    // Issue time of the report
    @Column
    @DataURI(position = 3)
    @DynamicSerializeElement
    private Calendar issueTime;

    // The designator
    @Column(length = 8)
    @DynamicSerializeElement
    private String designatorBBB;

    // CorrectionFlag is a flag with values (1 or 2 or 3)
    @Column(length = 8)
    @DynamicSerializeElement
    private String correctionRemarks;

    // The awipsId from the report
    @Column(length = 32)
    @DataURI(position = 4)
    @DynamicSerializeElement
    private String awipsId;

    // The state list from the report
    @Column(length = 256)
    @DynamicSerializeElement
    private String stateList;

    // Start time of the report
    @Column
    @DynamicSerializeElement
    private Calendar startTime;

    // End time of the report
    @Column
    @DynamicSerializeElement
    private Calendar endTime;

    // The type of the hazard from the report
    @Column(length = 16)
    @DynamicSerializeElement
    private String hazardType;

    // The intensity of the hazard from the report
    @Column(length = 64)
    @DynamicSerializeElement
    private String hazardIntensity;

    // The cause for the hazard from the report
    @Column(length = 128)
    @DynamicSerializeElement
    private String hazardCause;

    // The conditions stated about the hazard from the report
    @Column(length = 128)
    @DynamicSerializeElement
    private String hazardCondition;

    // The lower flight level from the report
    @Column
    @DynamicSerializeElement
    private int flightLevel1;

    // The upper flight level from the report
    @Column
    @DynamicSerializeElement
    private int flightLevel2;

    // The sigmet Identifier from the report
    @Column(length = 32)
    @DynamicSerializeElement
    private String sigmetId;

    // The entire report
    @Column(length = 3000)
    @DynamicSerializeElement
    private String bullMessage;

    @DynamicSerializeElement
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "parentID", nullable = false)
    private Set<NonConvSigmetLocation> nonConvSigmetLocation = new HashSet<>();

    public NonConvSigmetRecord() {
        this.issueOffice = "";
        this.wmoHeader = "";
        this.bullMessage = "";
        this.designatorBBB = "";
        this.forecastRegion = "";
        this.reportType = "";
        this.correctionRemarks = "";
        this.awipsId = "";
        this.flightLevel1 = IDecoderConstantsN.INTEGER_MISSING;
        this.flightLevel2 = IDecoderConstantsN.INTEGER_MISSING;
        this.hazardCause = "";
        this.hazardCondition = "";
        this.hazardIntensity = "";
        this.hazardType = "UNKNOWN";
        this.stateList = "";
        this.sigmetId = "";
    }

    /**
     * Constructs a non-consigmet record from a dataURI
     *
     * @param uri
     *            The dataURI
     */
    public NonConvSigmetRecord(String uri) {
        super(uri);
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getWmoHeader() {
        return wmoHeader;
    }

    public void setWmoHeader(String wmoHeader) {
        this.wmoHeader = wmoHeader;
    }

    public String getForecastRegion() {
        return forecastRegion;
    }

    public void setForecastRegion(String forecastRegion) {
        this.forecastRegion = forecastRegion;
    }

    public String getIssueOffice() {
        return issueOffice;
    }

    public void setIssueOffice(String issueOffice) {
        this.issueOffice = issueOffice;
    }

    public Calendar getIssueTime() {
        return issueTime;
    }

    public void setIssueTime(Calendar issueTime) {
        this.issueTime = issueTime;
    }

    public String getDesignatorBBB() {
        return designatorBBB;
    }

    public void setDesignatorBBB(String designatorBBB) {
        this.designatorBBB = designatorBBB;
    }

    public String getCorrectionRemarks() {
        return correctionRemarks;
    }

    public void setCorrectionRemarks(String correctionRemarks) {
        this.correctionRemarks = correctionRemarks;
    }

    public String getAwipsId() {
        return awipsId;
    }

    public void setAwipsId(String awipsId) {
        this.awipsId = awipsId;
    }

    public String getStateList() {
        return stateList;
    }

    public void setStateList(String stateList) {
        this.stateList = stateList;
    }

    public Calendar getStartTime() {
        return startTime;
    }

    public void setStartTime(Calendar startTime) {
        this.startTime = startTime;
    }

    public Calendar getEndTime() {
        return endTime;
    }

    public void setEndTime(Calendar endTime) {
        this.endTime = endTime;
    }

    public String getHazardType() {
        return hazardType;
    }

    public void setHazardType(String hazardType) {
        this.hazardType = hazardType;
    }

    public String getHazardIntensity() {
        return hazardIntensity;
    }

    public void setHazardIntensity(String hazardIntensity) {
        this.hazardIntensity = hazardIntensity;
    }

    public String getHazardCause() {
        return hazardCause;
    }

    public void setHazardCause(String hazardCause) {
        this.hazardCause = hazardCause;
    }

    public String getHazardCondition() {
        return hazardCondition;
    }

    public void setHazardCondition(String hazardCondition) {
        this.hazardCondition = hazardCondition;
    }

    public int getFlightLevel1() {
        return flightLevel1;
    }

    public void setFlightLevel1(int flightLevel1) {
        this.flightLevel1 = flightLevel1;
    }

    public int getFlightLevel2() {
        return flightLevel2;
    }

    public void setFlightLevel2(int flightLevel2) {
        this.flightLevel2 = flightLevel2;
    }

    public String getSigmetId() {
        return sigmetId;
    }

    public void setSigmetId(String sigmetId) {
        this.sigmetId = sigmetId;
    }

    public String getBullMessage() {
        return bullMessage;
    }

    public void setBullMessage(String bullMessage) {
        this.bullMessage = bullMessage;
    }

    public Set<NonConvSigmetLocation> getNonConvSigmetLocation() {
        return nonConvSigmetLocation;
    }

    public void setNonConvSigmetLocation(
            Set<NonConvSigmetLocation> nonConvLocation) {
        this.nonConvSigmetLocation = nonConvLocation;
    }

    /**
     * @param add
     *            conv Sigmet location to set
     */
    public void addNonConvSigmetLocation(NonConvSigmetLocation pLocation) {
        nonConvSigmetLocation.add(pLocation);

    }

    @Override
    @Column
    @Access(AccessType.PROPERTY)
    public String getDataURI() {
        return super.getDataURI();
    }

    @Override
    public String getPluginName() {
        return "nonconvsigmet";
    }
}
