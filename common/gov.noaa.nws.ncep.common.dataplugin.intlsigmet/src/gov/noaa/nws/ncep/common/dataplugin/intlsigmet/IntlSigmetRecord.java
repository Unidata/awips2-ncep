package gov.noaa.nws.ncep.common.dataplugin.intlsigmet;

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
 * IntlsigmetRecord
 *
 * This java class performs the mapping to the database table for ITNLSIGMET
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * This code has been developed by the SIB for use in the AWIPS2 system.
 * Date         Ticket#         Engineer    Description
 * ------------ ----------      ----------- --------------------------
 * 06/2009      113             L. Lin      Initial coding
 * 07/2009      113             L. Lin      Migration to TO11
 * 05/2010      113             L. Lin      Migration to TO11DR11
 * 09/2011                      Chin Chen   changed to improve purge performance and
 *                                          removed xml serialization as well
 * Apr 4, 2013        1846 bkowal      Added an index on refTime and forecastTime
 * Apr 12, 2013 1857            bgonzale    Added SequenceGenerator annotation.
 * May 07, 2013 1869            bsteffen    Remove dataURI column from
 *                                          PluginDataObject.
 * Feb 11, 2014 2784            rferrel     Remove override of setIdentifier.
 * Jun 11, 2014 2061            bsteffen    Remove IDecoderGettable
 * Mar 06, 2019 6140            tgurney     Hibernate 5 @Index fix
 *
 * </pre>
 *
 * This code has been developed by the SIB for use in the AWIPS2 system.
 */
@Entity
@SequenceGenerator(initialValue = 1, name = PluginDataObject.ID_GEN,
        sequenceName = "intlsigmetseq")
/*
 * Both refTime and forecastTime are included in the refTimeIndex since
 * forecastTime is unlikely to be used.
 */
@Table(name = "intlsigmet",
        uniqueConstraints = { @UniqueConstraint(columnNames = { "dataURI" }) },
        indexes = { @Index(name = "intlsigmet_refTimeIndex",
                columnList = "refTime,forecastTime") })
@DynamicSerialize
public class IntlSigmetRecord extends PluginDataObject {

    private static final long serialVersionUID = 1L;

    // reportType is "international sigmet".
    @Column(length = 32)
    @DataURI(position = 1)
    @DynamicSerializeElement
    private String reportType;

    // hazardType is weather phenomena.
    @Column(length = 48)
    @DataURI(position = 2)
    @DynamicSerializeElement
    private String hazardType;

    // WMO header
    @Column(length = 32)
    @DynamicSerializeElement
    private String wmoHeader;

    // The issue office where the report from
    @Column(length = 32)
    @DynamicSerializeElement
    private String issueOffice;

    // Issue time of the report
    @Column
    @DynamicSerializeElement
    private Calendar issueTime;

    // Start time of the report
    @Column
    @DynamicSerializeElement
    private Calendar startTime;

    // End time of the report
    @Column
    @DynamicSerializeElement
    private Calendar endTime;

    // The message ID
    @Column(length = 16)
    @DataURI(position = 3)
    @DynamicSerializeElement
    private String messageID;

    // The sequence number
    @Column(length = 8)
    @DataURI(position = 4)
    @DynamicSerializeElement
    private String sequenceNumber;

    // The air traffic services unit
    @Column(length = 16)
    @DynamicSerializeElement
    private String atsu;

    // The location indicator of the meteorological watch office originator
    @Column(length = 16)
    @DynamicSerializeElement
    private String omwo;

    // Flight level 1
    @Column
    @DynamicSerializeElement
    private Integer flightlevel1;

    // Flight level 2
    @Column
    @DynamicSerializeElement
    private Integer flightlevel2;

    // Distance
    @Column
    @DynamicSerializeElement
    private Integer distance;

    // Direction
    @Column(length = 16)
    @DynamicSerializeElement
    private String direction;

    // Speed
    @Column
    @DynamicSerializeElement
    private Integer speed;

    /*
     * the name of the storm, where applicable, or location of the volcano,
     * where applicable, or the word, OTHER, for reports not from CONUS, Hawaii,
     * Guam, Japan, UK, Tahiti, and Cuba
     */
    @Column(length = 48)
    @DynamicSerializeElement
    private String nameLocation;

    /*
     * remarks such as: correction, remarks, ...etc.
     */
    @Column(length = 32)
    @DynamicSerializeElement
    private String remarks;

    // The changes in intensity; using as "INTSF", "WKN", or "NC".
    @Column(length = 16)
    @DynamicSerializeElement
    private String intensity;

    // The polygon indicator as "WI", "WTN", "EITHER SIDE", or "E OF".
    @Column(length = 16)
    @DynamicSerializeElement
    private String polygonExtent;

    // The entire report
    @Column(length = 5000)
    @DynamicSerializeElement
    private String bullMessage;

    @DynamicSerializeElement
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "parentID", nullable = false)
    private Set<IntlSigmetLocation> intlSigmetLocation = new HashSet<>();

    public IntlSigmetRecord() {
        this.issueOffice = null;
        this.wmoHeader = null;
        this.bullMessage = null;
        this.hazardType = null;
        this.messageID = null;
        this.reportType = "INTLSIGMET";
        this.sequenceNumber = null;
        this.atsu = null;
        this.omwo = null;
        this.nameLocation = null;
        this.intensity = null;
        this.remarks = null;
        this.flightlevel1 = IDecoderConstantsN.INTEGER_MISSING;
        this.flightlevel2 = IDecoderConstantsN.INTEGER_MISSING;
        this.direction = null;
        this.distance = IDecoderConstantsN.INTEGER_MISSING;
        this.speed = IDecoderConstantsN.INTEGER_MISSING;
        this.polygonExtent = null;
    }

    /**
     * Constructs a consigmet record from a dataURI
     *
     * @param uri
     *            The dataURI
     */
    public IntlSigmetRecord(String uri) {
        super(uri);
    }

    public String getIssueOffice() {
        return issueOffice;
    }

    public void setIssueOffice(String issueOffice) {
        this.issueOffice = issueOffice;
    }

    public String getWmoHeader() {
        return wmoHeader;
    }

    public void setWmoHeader(String wmoHeader) {
        this.wmoHeader = wmoHeader;
    }

    public Calendar getIssueTime() {
        return issueTime;
    }

    public void setIssueTime(Calendar issueTime) {
        this.issueTime = issueTime;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getBullMessage() {
        return bullMessage;
    }

    public void setBullMessage(String bullMessage) {
        this.bullMessage = bullMessage;
    }

    public String getHazardType() {
        return hazardType;
    }

    public void setHazardType(String hazardType) {
        this.hazardType = hazardType;
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

    public String getMessageID() {
        return messageID;
    }

    public void setMessageID(String messageID) {
        this.messageID = messageID;
    }

    public String getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(String sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getAtsu() {
        return atsu;
    }

    public void setAtsu(String atsu) {
        this.atsu = atsu;
    }

    public String getOmwo() {
        return omwo;
    }

    public void setOmwo(String omwo) {
        this.omwo = omwo;
    }

    public Integer getFlightlevel1() {
        return flightlevel1;
    }

    public void setFlightlevel1(Integer flightlevel1) {
        this.flightlevel1 = flightlevel1;
    }

    public Integer getFlightlevel2() {
        return flightlevel2;
    }

    public void setFlightlevel2(Integer flightlevel2) {
        this.flightlevel2 = flightlevel2;
    }

    public Integer getDistance() {
        return distance;
    }

    public void setDistance(Integer distance) {
        this.distance = distance;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public Integer getSpeed() {
        return speed;
    }

    public void setSpeed(Integer speed) {
        this.speed = speed;
    }

    public String getNameLocation() {
        return nameLocation;
    }

    public void setNameLocation(String nameLocation) {
        this.nameLocation = nameLocation;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getIntensity() {
        return intensity;
    }

    public void setIntensity(String intensity) {
        this.intensity = intensity;
    }

    public String getPolygonExtent() {
        return polygonExtent;
    }

    public void setPolygonExtent(String polygonExtent) {
        this.polygonExtent = polygonExtent;
    }

    public Set<IntlSigmetLocation> getIntlSigmetLocation() {
        return intlSigmetLocation;
    }

    public void setIntlSigmetLocation(
            Set<IntlSigmetLocation> intlSigmetLocation) {
        this.intlSigmetLocation = intlSigmetLocation;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    /**
     * @param add
     *            international sigmet Location to set
     */
    public void addIntlSigmetLocation(IntlSigmetLocation psection) {
        intlSigmetLocation.add(psection);

    }

    @Override
    @Column
    @Access(AccessType.PROPERTY)
    public String getDataURI() {
        return super.getDataURI();
    }

    @Override
    public String getPluginName() {
        return "intlsigmet";
    }
}
