package gov.noaa.nws.ncep.common.dataplugin.aww;

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

/**
 * AwwRecord
 *
 * This java class performs the mapping to the database tables for AWW.
 *
 * SOFTWARE HISTORY
 *
 * <pre>
 * Date         Ticket#         Engineer    Description
 * ------------ ----------      ----------- --------------------------
 * 12/2008      38              L. Lin      Initial coding
 * 04/2009      38              L. Lin      Convert to TO10.
 * 07/2009      38              L. Lin      Migration to TO11
 * 05/2010      38              L. Lin      Migration to TO11DR11
 * 01/11/2011   N/A             M. Gao      Add mndTime as the 5th element to construct
 *                                          dataUri value that is used as a unique constraint
 *                                          when the aww record is inserted into relational DB
 *                                          The reason mndTime is used is because the combination
 *                                          of original 4 elements is not unique in some scenarios.
 * 01/26/2011   N/A             M. Gao      Add designatorBBB as the 6th (No.4) element to construct
 *                                          dataUri value that is used as a unique constraint
 *                                          when the aww record is inserted into relational DB
 *                                          The reason mndTime is used is because the combination
 *                                          of original 5 elements is not unique in some scenarios.
 * 09/2011                      Chin Chen   changed to improve purge performance and
 *                                          removed xml serialization as well
 * Apr 4, 2013        1846 bkowal      Added an index on refTime and forecastTime
 * Apr 12, 2013 1857            bgonzale    Added SequenceGenerator annotation.
 * May 07, 2013 1869            bsteffen    Remove dataURI column from
 *                                          PluginDataObject.
 * July 29, 2013 1028           ghull       add AwwReportType enum
 * Feb 11, 2014 2784            rferrel     Remove override of setIdentifier.
 * Jun 11, 2014 2061            bsteffen    Remove IDecoderGettable
 * July 07, 2014 ???            D. Sushon   add handling for TORNADO_WATCH in getReportType(..)
 * November 07, 2014 5125       J. Huber    added WINTER_WEATHER reportType to enum and removed
 *                                          WINTER_STORM_WARNING, WINTER_STORM_WATCH, and
 *                                          WINTER_WEATHER_ADVISORY as they are now no longer needed.
 * Mar 05, 2019       6140      tgurney     Hibernate 5 @Index fix
 * </pre>
 *
 * This code has been developed by the SIB for use in the AWIPS2 system.
 */
@Entity
@SequenceGenerator(initialValue = 1, name = PluginDataObject.ID_GEN,
        sequenceName = "awwseq")
/*
 * Both refTime and forecastTime are included in the refTimeIndex since
 * forecastTime is unlikely to be used.
 */
@Table(name = "aww",
        uniqueConstraints = { @UniqueConstraint(columnNames = { "dataURI" }) },
        indexes = { @Index(name = "aww_refTimeIndex",
                columnList = "refTime,forecastTime") })

@DynamicSerialize
public class AwwRecord extends PluginDataObject {

    private static final long serialVersionUID = 1L;

    // RM 5125 add report types WINTER_WEATHER (for decoding purposes) and all
    // specific winter products .
    public static enum AwwReportType {
        SEVERE_THUNDERSTORM_WARNING, SEVERE_THUNDERSTORM_WATCH, TORNADO_WARNING,
        TORNADO_WATCH, SEVERE_THUNDERSTORM_OUTLINE_UPDATE,
        TORNADO_WATCH_OUTLINE_UPDATE, FLASH_FLOOD_WARNING, FLASH_FLOOD_WATCH,
        FLOOD_WARNING, FLOOD_WATCH, FLOOD_STATEMENT, WINTER_WEATHER_ADVISORY,
        WINTER_STORM_WATCH, WINTER_STORM_WARNING, WATCH_COUNTY_NOTIFICATION,
        SEVERE_WEATHER_STATEMENT, WIND_ADVISORY, FOG_ADVISORY, HEAT_ADVISORY,
        FROST_ADVISORY, SMOKE_ADVISORY, WEATHER_ADVISORY,
        SIGNIGICANT_WEATHER_ADVISORY, SPECIAL_WEATHER_STATEMENT,
        RED_FLAG_WARNING, TORNADO_REPORT, HIGH_WIND_WARNING, FREEZE_WARNING,
        ADVERTENCIA_DE_INUNDACIONES, HYDROLOGIC_STATEMENT,
        URGENT_WEATHER_MESSAGE, UNKNOWN_AWW_REPORT_TYPE, STATUS_REPORT, // RM5125
        WINTER_WEATHER, BLIZZARD_WATCH, BLIZZARD_WARNING, ICE_STORM_WARNING,
        LAKE_EFFECT_SNOW_ADVISORY, LAKE_EFFECT_SNOW_WATCH,
        LAKE_EFFECT_SNOW_WARNING, FREEZING_RAIN_ADVISORY, WIND_CHILL_ADVISORY,
        WIND_CHILL_WARNING;

        public static AwwReportType getReportType(String rtStr) {
            rtStr = rtStr.trim().replace(" ", "_");
            for (AwwReportType rt : values()) {
                if (rt.toString().equals(rtStr)) {
                    return rt;
                }
            }
            // WTCH is looking for
            if ("THUNDERSTORM_REPORT".equals(rtStr)) {
                return SEVERE_THUNDERSTORM_WATCH;
            }
            if ("TORNADO_REPORT".equals(rtStr)) {
                return TORNADO_WATCH;
            }
            if (rtStr.endsWith("STATUS_REPORT")) {
                // ??? return AwwReportType.SEVERE_WEATHER_STATUS_NOTIFICATION
                return AwwReportType.STATUS_REPORT;
            }
            // WSTM is looking for
            if ("WINTER_STORM".equals(rtStr)) {
                // ???
            }
            if ("ADVISORY".equals(rtStr)) {
                // ???? WIND CHILL ADVISORY is getting decoded as "ADVISORY"???
            }
            return UNKNOWN_AWW_REPORT_TYPE;
        }
    }

    @Column(length = 40)
    @DataURI(position = 1)
    @DynamicSerializeElement
    private String reportType;

    // The issue office where the report from
    @Column(length = 32)
    @DataURI(position = 2)
    @DynamicSerializeElement
    private String issueOffice;

    // The collection of watch numbers in the report
    @Column(length = 160)
    @DataURI(position = 5)
    @DynamicSerializeElement
    private String watchNumber;

    // WMO header
    @Column(length = 32)
    @DynamicSerializeElement
    private String wmoHeader;

    // Issue time of the report
    @Column
    @DataURI(position = 3)
    @DynamicSerializeElement
    private Calendar issueTime;

    // The designator
    @Column(length = 8)
    @DataURI(position = 4)
    @DynamicSerializeElement
    private String designatorBBB;

    // The designator
    @Column(length = 72)
    @DataURI(position = 6)
    @DynamicSerializeElement
    private String mndTime;

    // Attention WFO
    @Column(length = 72)
    @DynamicSerializeElement
    private String attentionWFO;

    // The entire report
    @Column(length = 40_000)
    @DynamicSerializeElement
    private String bullMessage;

    // AWW UGC Table
    @DynamicSerializeElement
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "parentID", nullable = false)
    private Set<AwwUgc> awwUGC = new HashSet<>();

    public AwwRecord() {
        this.issueOffice = null;
        this.watchNumber = "0000";
        this.issueTime = null;
        this.attentionWFO = null;
        this.wmoHeader = null;
        this.designatorBBB = null;
        this.bullMessage = null;
        this.mndTime = null;
    }

    /**
     * Constructs a consigmet record from a dataURI
     *
     * @param uri
     *            The dataURI
     */
    public AwwRecord(String uri) {
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

    public Calendar getIssueTime() {
        return issueTime;
    }

    public void setIssueTime(Calendar issueTime) {
        this.issueTime = issueTime;
    }

    public Set<AwwUgc> getAwwUGC() {
        return awwUGC;
    }

    public void setAwwUGC(Set<AwwUgc> awwUgc) {
        this.awwUGC = awwUgc;
    }

    /**
     * @param add
     *            AWW UGC to set
     */
    public void addAwwUGC(AwwUgc pugc) {
        awwUGC.add(pugc);
    }

    public String getDesignatorBBB() {
        return designatorBBB;
    }

    public void setDesignatorBBB(String designatorBBB) {
        this.designatorBBB = designatorBBB;
    }

    public String getAttentionWFO() {
        return attentionWFO;
    }

    public void setAttentionWFO(String attentionWFO) {
        this.attentionWFO = attentionWFO;
    }

    public String getWatchNumber() {
        return watchNumber;
    }

    public void setWatchNumber(String watchNumber) {
        this.watchNumber = watchNumber;
    }

    public String getBullMessage() {
        return bullMessage;
    }

    public void setBullMessage(String bullMessage) {
        this.bullMessage = bullMessage;
    }

    public String getIssueOffice() {
        return issueOffice;
    }

    public void setIssueOffice(String issueOffice) {
        this.issueOffice = issueOffice;
    }

    public String getMndTime() {
        return mndTime;
    }

    public void setMndTime(String mndTime) {
        this.mndTime = mndTime;
    }

    @Override
    @Column
    @Access(AccessType.PROPERTY)
    public String getDataURI() {
        return super.getDataURI();
    }

    @Override
    public String getPluginName() {
        return "aww";
    }
}
