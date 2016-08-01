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
 * Record implementation for geomag avg.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer           Description
 * ------------ ---------- ----------------   --------------------------
 * 08/14/2013   T989       qzhou              Initial creation.
 * 03/03/2014              qzhou              modified get/set
 * 12/23/2014   R5412      sgurung            Change float to double
 * 01/26/2015   R7615      sgurung            Change sequence name
 * 01/05/2016   R14697     sgurung,jtravis    Add unique constraint on avgTime, add column lastMinuteUsed
 * 05/19/2016   R18351     sgurung            Add composite unique constraint consisting of "avgTime" and "stationCode" 
 * 
 * </pre>
 * 
 * @author qzhou
 * @version 1.0
 */

@Entity
@SequenceGenerator(initialValue = 1, name = PluginDataObject.ID_GEN, sequenceName = "geomagavgseq")
@Table(name = "geomag_houravg", uniqueConstraints = { @UniqueConstraint(columnNames = { "avgTime", "stationCode" }) })
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class GeoMagAvg extends PersistableDataObject<Object> {

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
    private Date avgTime;

    /**
     * insert time tag
     */
    @Column
    @XmlAttribute
    @DynamicSerializeElement
    private Date insertTime;

    /**
     * H data Hour Average
     */
    @Column
    @XmlAttribute
    @DynamicSerializeElement
    private double hHrAvg;

    /**
     * D data Hour Average
     */
    @Column
    @XmlAttribute
    @DynamicSerializeElement
    private double dHrAvg;

    /**
     * Latest minute used in the hourly average calculation
     */
    @Column
    @XmlAttribute
    @DynamicSerializeElement
    private int lastMinuteUsed;

    public GeoMagAvg() {

    }

    public void generateId() {
        this.id = hashCode();
    }

    /**
     * @return the hHrAvg
     */
    public double gethHrAvg() {
        return hHrAvg;
    }

    public void sethHrAvg(double hHrAvg) {
        this.hHrAvg = hHrAvg;
    }

    /**
     * @return the dHrAvg
     */
    public double getdHrAvg() {
        return dHrAvg;
    }

    public void setdHrAvg(double dHrAvg) {
        this.dHrAvg = dHrAvg;
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
    public Date getAvgTime() {
        return avgTime;
    }

    public void setAvgTime(Date avgTime) {
        this.avgTime = avgTime;
    }

    /**
     * @return the insert time
     */
    public Date getInsertTime() {
        return insertTime;
    }

    public void setInsertTime(Date insertTime) {
        this.insertTime = insertTime;
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

    /**
     * @return the lastMinuteUsed
     */
    public int getLastMinuteUsed() {
        return lastMinuteUsed;
    }

    /**
     * Set the last minute used
     * 
     * @param lastMinuteUsed
     */
    public void setLastMinuteUsed(int lastMinuteUsed) {
        this.lastMinuteUsed = lastMinuteUsed;
    }
}
