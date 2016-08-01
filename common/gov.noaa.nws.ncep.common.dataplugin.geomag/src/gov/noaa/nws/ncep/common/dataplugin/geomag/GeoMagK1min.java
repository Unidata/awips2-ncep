package gov.noaa.nws.ncep.common.dataplugin.geomag;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.persist.PersistableDataObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Record implementation for geomag k 1 min.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer           Description
 * ------------ ---------- ----------------   --------------------------
 * 08/14/2013   T989       qzhou              Initial creation.
 * 03/03/2014   #1110      qzhou              modified get/set 
 * 04/05/2014   R4078      sgurung            Added method match().
 * 12/23/2014   R5412      sgurung            Change float to double
 * 01/26/2015   R7615      sgurung            change sequence name
 * 01/05/2016   R14697     sgurung,jtravis    Add unique constraint on refTime
 * 05/19/2016   R18351     sgurung            Add composite unique constraint consisting of "refTime" and "stationCode"
 * 
 * </pre>
 * 
 * @author qzhou
 * @version 1.0
 */

@Entity
@SequenceGenerator(initialValue = 1, name = PluginDataObject.ID_GEN, sequenceName = "geomagk1minseq")
@Table(name = "geomag_k1min", uniqueConstraints = { @UniqueConstraint(columnNames = { "refTime", "stationCode" }) })
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class GeoMagK1min extends PersistableDataObject<Object> {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    public static final String ID_GEN = "idgen";

    /** The id */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = ID_GEN)
    private Integer id;

    /**
     * station code
     */
    @Column
    @XmlAttribute
    @DynamicSerializeElement
    private String stationCode;

    /**
     * time tag
     */
    @Column
    @XmlAttribute
    @DynamicSerializeElement
    private Date refTime;

    /**
     * insert time tag
     */
    @Column
    @XmlAttribute
    @DynamicSerializeElement
    private Date lastUpdate;

    /**
     * H data Hour Average
     */
    @Column(length = 16)
    @DynamicSerializeElement
    private int kestIndex;

    /**
     * D data Hour Average
     */
    @Column(length = 16)
    @DynamicSerializeElement
    private double kestReal;

    /**
     * D data Hour Average
     */
    @Column(length = 16)
    @DynamicSerializeElement
    private double kestGamma;

    /**
     * H data Hour Average
     */
    @Column(length = 16)
    @DynamicSerializeElement
    private int hkIndex;

    /**
     * D data Hour Average
     */
    @Column(length = 16)
    @DynamicSerializeElement
    private double hkReal;

    /**
     * D data Hour Average
     */
    @Column(length = 16)
    @DynamicSerializeElement
    private double hkGamma;

    /**
     * H data Hour Average
     */
    @Column(length = 16)
    @DynamicSerializeElement
    private int dkIndex;

    /**
     * D data Hour Average
     */
    @Column(length = 16)
    @DynamicSerializeElement
    private double dkReal;

    /**
     * D data Hour Average
     */
    @Column(length = 16)
    @DynamicSerializeElement
    private double dkGamma;

    /**
     * D data Hour Average
     */
    @Column(length = 16)
    @DynamicSerializeElement
    private int hCount;

    /**
     * D data Hour Average
     */
    @Column(length = 16)
    @DynamicSerializeElement
    private int dCount;

    /**
     * D data Hour Average
     */
    @Column(length = 16)
    @DynamicSerializeElement
    private int aest;

    /**
     * D data Hour Average
     */
    @Column(length = 16)
    @DynamicSerializeElement
    private double ks;

    public GeoMagK1min() {

    }

    public void generateId() {
        this.id = hashCode();
    }

    /**
     * @return the kestIndex
     */
    public int getKestIndex() {
        return kestIndex;
    }

    public void setKestIndex(int kestIndex) {
        this.kestIndex = kestIndex;
    }

    /**
     * @return the kestReal
     */
    public double getKestReal() {
        return kestReal;
    }

    public void setKestReal(double kestReal) {
        this.kestReal = kestReal;
    }

    /**
     * @return the kestGamma
     */
    public double getKestGamma() {
        return kestGamma;
    }

    public void setKestGamma(double kestGamma) {
        this.kestGamma = kestGamma;
    }

    /**
     * @return the hkReal
     */
    public double getHkReal() {
        return hkReal;
    }

    public void setHkReal(double hkReal) {
        this.hkReal = hkReal;
    }

    /**
     * @return the hkGamma
     */
    public double getHkGamma() {
        return hkGamma;
    }

    public void setHkGamma(double hkGamma) {
        this.hkGamma = hkGamma;
    }

    /**
     * @return the hkIndex
     */
    public int getHkIndex() {
        return hkIndex;
    }

    public void setHkIndex(int hkIndex) {
        this.hkIndex = hkIndex;
    }

    /**
     * @return the dkIndex
     */
    public int getDkIndex() {
        return dkIndex;
    }

    public void setDkIndex(int dkIndex) {
        this.dkIndex = dkIndex;
    }

    /**
     * @return the dkReal
     */
    public double getDkReal() {
        return dkReal;
    }

    public void setDkReal(double dkReal) {
        this.dkReal = dkReal;
    }

    /**
     * @return the dkGamma
     */
    public double getDkGamma() {
        return dkGamma;
    }

    public void setDkGamma(double dkGamma) {
        this.dkGamma = dkGamma;
    }

    /**
     * @return the hCount
     */
    public int gethCount() {
        return hCount;
    }

    public void sethCount(int hCount) {
        this.hCount = hCount;
    }

    /**
     * @return the dCount
     */
    public int getdCount() {
        return dCount;
    }

    public void setdCount(int dCount) {
        this.dCount = dCount;
    }

    /**
     * @return the dHrAvg
     */
    public int getAest() {
        return aest;
    }

    public void setAest(int aest) {
        this.aest = aest;
    }

    /**
     * @return the dHrAvg
     */
    public double getKs() {
        return ks;
    }

    public void setKs(double ks) {
        this.ks = ks;
    }

    /**
     * @return The id
     */
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * @return the timeTag
     */
    public Date getRefTime() {
        return refTime;
    }

    public void setRefTime(Date refTime) {
        this.refTime = refTime;
    }

    /**
     * @return the timeTag
     */
    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    /**
     * @return the stationCode
     */
    public String getStationCode() {
        return stationCode;
    }

    public void setStationCode(String stationCode) {
        this.stationCode = stationCode;
    }

    public boolean match(Date refTime, String stationCode) {
        if (this.refTime.compareTo(refTime) == 0
                && this.stationCode.equals(stationCode)) {
            return true;
        } else {
            return false;
        }

    }
}
