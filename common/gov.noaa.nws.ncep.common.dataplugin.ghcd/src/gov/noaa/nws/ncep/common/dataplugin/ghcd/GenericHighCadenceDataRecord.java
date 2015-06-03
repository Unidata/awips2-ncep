/**
 * This code has unlimited rights, and is provided "as is" by the National Centers 
 * for Environmental Prediction, without warranty of any kind, either expressed or implied, 
 * including but not limited to the implied warranties of merchantability and/or fitness 
 * for a particular purpose.
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 */
package gov.noaa.nws.ncep.common.dataplugin.ghcd;

import gov.noaa.nws.ncep.common.dataplugin.ghcd.product.GenericHighCadenceDataField;
import gov.noaa.nws.ncep.common.dataplugin.ghcd.product.GenericHighCadenceDataTypeInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
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

import org.hibernate.annotations.Index;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.annotations.DataURI;
import com.raytheon.uf.common.dataplugin.persist.IPersistable;
import com.raytheon.uf.common.dataplugin.persist.PersistablePluginDataObject;
import com.raytheon.uf.common.pointdata.IPointData;
import com.raytheon.uf.common.pointdata.PointDataView;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.time.DataTime;

/**
 * 
 * Record implementation for the high cadence data plugin.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    DescriptionR
 * -------      -------     --------    -----------
 * 01/22/2014   1100        sgurung     Initial creation
 * 07/15/2014   1100        sgurung     Modified column names in uniqueConstraints
 * 
 * </pre>
 * 
 * @author sgurung
 * @version 1.0
 */
@Entity
@SequenceGenerator(initialValue = 1, name = PluginDataObject.ID_GEN, sequenceName = "ghcdseq")
@Table(name = "ghcd", uniqueConstraints = { @UniqueConstraint(columnNames = { "dataURI" }) })
// uniqueConstraints = { @UniqueConstraint(columnNames = {
// "refTime", "typeInfo_source", "typeInfo_instrument",
// "typeInfo_datatype", "dataResolUnits", "dataResolVal" }) })
/*
 * Both refTime and forecastTime are included in the refTimeIndex since
 * forecastTime is unlikely to be used.
 */
@org.hibernate.annotations.Table(appliesTo = "ghcd", indexes = { @Index(name = "ghcd_refTimeIndex", columnNames = {
        "refTime", "forecastTime" }) })
@DynamicSerialize
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement
public class GenericHighCadenceDataRecord extends PersistablePluginDataObject
        implements IPointData, IPersistable {

    private static final long serialVersionUID = 1L;

    @DataURI(position = 1)
    @Column
    @DynamicSerializeElement
    @XmlAttribute
    private String reportType;

    @Embedded
    @ManyToOne(cascade = { CascadeType.REFRESH })
    @PrimaryKeyJoinColumn
    @DataURI(position = 2, embedded = true)
    @DynamicSerializeElement
    @XmlElement
    private GenericHighCadenceDataTypeInfo typeInfo;

    @DataURI(position = 3)
    @Column
    @DynamicSerializeElement
    @XmlAttribute
    private String dataResolUnits;

    @DataURI(position = 4)
    @Column
    @DynamicSerializeElement
    @XmlAttribute
    private Integer dataResolVal;

    @Transient
    @DynamicSerializeElement
    @XmlAttribute
    private String source;

    @Embedded
    @DynamicSerializeElement
    private PointDataView pointDataView;

    @Transient
    private List<GenericHighCadenceDataField> fieldLst = new ArrayList<GenericHighCadenceDataField>();

    public GenericHighCadenceDataRecord() {
        super();
    }

    public GenericHighCadenceDataRecord(String uri) {
        super(uri);
    }

    public GenericHighCadenceDataRecord(String uri,
            GenericHighCadenceDataTypeInfo ghcdTypeInfo, String source,
            String reportType, String dataResolUnits, Integer dataResolVal,
            PointDataView pointDataView) {
        super(uri);
        this.typeInfo = ghcdTypeInfo;
        this.source = source;
        this.reportType = reportType;
        this.dataResolUnits = dataResolUnits;
        this.dataResolVal = dataResolVal;
        this.pointDataView = pointDataView;
    }

    public GenericHighCadenceDataRecord(
            GenericHighCadenceDataTypeInfo ghcdTypeInfo, String source,
            String reportType, String dataResolUnits, Integer dataResolVal,
            PointDataView pointDataView, DataTime dataTime) {
        this.typeInfo = ghcdTypeInfo;
        this.source = source;
        this.reportType = reportType;
        this.dataResolUnits = dataResolUnits;
        this.dataResolVal = dataResolVal;
        this.pointDataView = pointDataView;
        this.dataTime = dataTime;
    }

    public GenericHighCadenceDataTypeInfo getTypeInfo() {
        return typeInfo;
    }

    public void setTypeInfo(GenericHighCadenceDataTypeInfo ghcdTypeInfo) {
        this.typeInfo = ghcdTypeInfo;
    }

    @Override
    public PointDataView getPointDataView() {
        return this.pointDataView;
    }

    @Override
    public void setPointDataView(PointDataView pointDataView) {
        this.pointDataView = pointDataView;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getDataResolUnits() {
        return dataResolUnits;
    }

    public void setDataResolUnits(String dataResolUnits) {
        this.dataResolUnits = dataResolUnits;
    }

    public Integer getDataResolVal() {
        return dataResolVal;
    }

    public void setDataResolVal(Integer dataResolVal) {
        this.dataResolVal = dataResolVal;
    }

    public List<GenericHighCadenceDataField> getFieldLst() {
        if (fieldLst == null) {
            fieldLst = new ArrayList<GenericHighCadenceDataField>();
        }
        return fieldLst;
    }

    public void setFieldLst(List<GenericHighCadenceDataField> fieldLst) {
        this.fieldLst = fieldLst;
    }

    @Override
    public String getPluginName() {
        return GenericHighCadenceDataConstants.PLUGIN_NAME;
    }

    @Override
    @Column
    @Access(AccessType.PROPERTY)
    public String getDataURI() {
        return super.getDataURI();
    }

}
