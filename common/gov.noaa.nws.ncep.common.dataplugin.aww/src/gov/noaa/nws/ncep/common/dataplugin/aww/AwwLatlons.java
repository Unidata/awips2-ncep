package gov.noaa.nws.ncep.common.dataplugin.aww;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

import gov.noaa.nws.ncep.common.tools.IDecoderConstantsN;

/**
 * AwwLatlons
 *
 * This java class represents the lat/lons for an Aww record.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Author      Description
 * ------------ ----------  ----------- --------------------------
 * 12/2008      L. Lin      Initial creation
 * 04/2009      L. Lin      Convert to to10
 * 09/2011      Chin Chen   changed to improve purge performance and
 *                          removed xml serialization as well
 * Mar 05, 2019 tgurney     Hibernate 5 @Index fix
 *
 * </pre>
 *
 * This code has been developed by the SIB for use in the AWIPS2 system.
 */

@Entity
@Table(name = "aww_latlons", indexes = {
        @Index(name = "awwLatLon_parentid_idex", columnList = "parentID") })
@DynamicSerialize
public class AwwLatlons implements Serializable, ISerializableObject {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Integer recordId = null;

    // Latitude
    @Column
    @DynamicSerializeElement
    private Float lat;

    // longitude
    @Column
    @DynamicSerializeElement
    private Float lon;

    // Index for the order of a complete "lat/lon" set
    @Column
    @DynamicSerializeElement
    private int index;

    /**
     * No-Arg Constructor.
     */
    public AwwLatlons() {
        this.lat = IDecoderConstantsN.FLOAT_MISSING;
        this.lon = IDecoderConstantsN.FLOAT_MISSING;
        this.index = -9999;
    }

    /**
     * @return the serialVersionUID
     */
    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    /**
     * Get the record id.
     *
     * @return The recordId. If not set returns null.
     */
    public Integer getRecordId() {
        return recordId;
    }

    /**
     * Set the record id.
     * 
     * @param record
     */
    public void setRecordId(Integer recordId) {
        this.recordId = recordId;
    }

    /**
     * @return the latitude
     */
    public float getLat() {
        return lat;
    }

    /**
     * @param latitude
     *            the latitude to set.
     */
    public void setLat(float latitude) {
        this.lat = latitude;
    }

    /**
     * @return the longitude
     */
    public float getLon() {
        return lon;
    }

    /**
     * @param longitude
     *            the longitude to set
     */
    public void setLon(float longitude) {
        this.lon = longitude;
    }

    /**
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * @param index
     *            to set
     */
    public void setIndex(int index) {
        this.index = index;
    }

}
