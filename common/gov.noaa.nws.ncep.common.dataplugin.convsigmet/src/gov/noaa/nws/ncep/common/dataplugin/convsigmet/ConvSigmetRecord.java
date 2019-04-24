package gov.noaa.nws.ncep.common.dataplugin.convsigmet;

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
 * ConvsigmetRecord
 *
 * This java class performs the mapping to the database table for CONVSIGMET
 *
 * SOFTWARE HISTORY
 *
 * <pre>
 * Date         Ticket#         Engineer    Description
 * ------------ ----------      ----------- --------------------------
 * 03/2009      87/114          L. Lin      Initial coding
 * 07/2009      87/114          L. Lin      Migration to TO11
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
        sequenceName = "convsigmetseq")
/*
 * Both refTime and forecastTime are included in the refTimeIndex since
 * forecastTime is unlikely to be used.
 */
@Table(name = "convsigmet",
        uniqueConstraints = { @UniqueConstraint(columnNames = { "dataURI" }) },
        indexes = { @Index(name = "convsigmet_refTimeIndex",
                columnList = "refTime,forecastTime") })
@DynamicSerialize
public class ConvSigmetRecord extends PluginDataObject {

    private static final long serialVersionUID = 1L;

    // reportType is "convective sigmet".
    @Column(length = 32)
    @DataURI(position = 1)
    @DynamicSerializeElement
    private String reportType;

    // WMO header
    @Column(length = 32)
    @DataURI(position = 2)
    @DynamicSerializeElement
    private String wmoHeader;

    // forecastRegion as: SIGW, SIGC, or SIGE
    @Column(length = 8)
    @DataURI(position = 3)
    @DynamicSerializeElement
    private String forecastRegion;

    // The issue office where the report from
    @Column(length = 32)
    @DynamicSerializeElement
    private String issueOffice;

    // Issue time of the report
    @Column
    @DynamicSerializeElement
    private Calendar issueTime;

    // The designator
    @Column(length = 8)
    @DynamicSerializeElement
    private String designatorBBB;

    // CorrectionFlag is a flag indicating a cancellation (0 or 1)
    @Column
    @DynamicSerializeElement
    private Integer correctionFlag;

    // The entire report
    @Column(length = 15_000)
    @DynamicSerializeElement
    private String bullMessage;

    @DynamicSerializeElement
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "parentID", nullable = false)
    private Set<ConvSigmetSection> convSigmetSection = new HashSet<>();

    public ConvSigmetRecord() {
        this.issueOffice = "";
        this.wmoHeader = "";
        this.bullMessage = "";
        this.designatorBBB = "";
        this.forecastRegion = "";
        this.reportType = "";
        this.correctionFlag = IDecoderConstantsN.INTEGER_MISSING;
    }

    /**
     * Constructs a consigmet record from a dataURI
     *
     * @param uri
     *            The dataURI
     */
    public ConvSigmetRecord(String uri) {
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

    public String getDesignatorBBB() {
        return designatorBBB;
    }

    public void setDesignatorBBB(String designatorBBB) {
        this.designatorBBB = designatorBBB;
    }

    public Integer getCorrectionFlag() {
        return correctionFlag;
    }

    public void setCorrectionFlag(Integer correctionFlag) {
        this.correctionFlag = correctionFlag;
    }

    public String getForecastRegion() {
        return forecastRegion;
    }

    public void setForecastRegion(String forecastRegion) {
        this.forecastRegion = forecastRegion;
    }

    public String getBullMessage() {
        return bullMessage;
    }

    public void setBullMessage(String bullMessage) {
        this.bullMessage = bullMessage;
    }

    public Set<ConvSigmetSection> getConvSigmetSection() {
        return convSigmetSection;
    }

    public void setConvSigmetSection(Set<ConvSigmetSection> convSection) {
        this.convSigmetSection = convSection;
    }

    /**
     * @param add
     *            convective Sigmet Section to set
     */
    public void addConvSigmetSection(ConvSigmetSection psection) {
        convSigmetSection.add(psection);

    }

    @Override
    @Column
    @Access(AccessType.PROPERTY)
    public String getDataURI() {
        return super.getDataURI();
    }

    @Override
    public String getPluginName() {
        return "convsigmet";
    }
}
