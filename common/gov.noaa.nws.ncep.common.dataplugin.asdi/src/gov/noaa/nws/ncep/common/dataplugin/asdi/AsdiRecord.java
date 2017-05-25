package gov.noaa.nws.ncep.common.dataplugin.asdi;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.annotations.Index;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * AsdiRecord is the Data Access component for Aircraft Situational Display to
 * Industry (ASDI) data. This contains getters and setters for the main parent
 * table asdi. Information here from the key for getting the HDF5 data.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 03/07/2017   R28579     R.Reynolds  Initial coding.
 * </pre>
 * 
 * @author R.C.Reynolds
 * @version 1.0
 */
@Entity
@SequenceGenerator(initialValue = 1, name = PluginDataObject.ID_GEN, sequenceName = "asdiseq")
@Table(name = "asdi", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "refTime", "flightNumber" }) })
/*
 * Both refTime and forecastTime are included in the refTimeIndex since
 * forecastTime is unlikely to be used.
 */
@org.hibernate.annotations.Table(appliesTo = "asdi", indexes = {
        @Index(name = "asdi_refTimeIndex", columnNames = { "refTime",
                "forecastTime" }) })

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class AsdiRecord extends PluginDataObject {

    private static final long serialVersionUID = 1L;

    /**
     * ASDI reportType
     */
    @Column(length = 32)
    @DynamicSerializeElement
    @XmlElement
    private String reportType = "ASDI";

    /**
     * Flight Number (unique ID of flight)
     */
    @Column(length = 40)
    @DynamicSerializeElement
    @XmlElement
    private String flightNumber = "";

    /**
     * Flight type e.g. COM, TAX, GA
     */
    @DynamicSerializeElement
    @Column(length = 8)
    @XmlElement
    private String flightType = "";

    /**
     * Aircraft type
     */
    @DynamicSerializeElement
    @Column(length = 8)
    @XmlElement
    private String aircraftType = "";

    /**
     * Latitude of aircraft (degrees) for the DTG: -900 through 900
     */
    @DynamicSerializeElement
    @XmlElement
    private float latitude = 0;

    /**
     * Longitude of aircraft (degrees) for the DTG: -1800 through 1800
     */
    @DynamicSerializeElement
    @XmlElement
    private float longitude = 0;

    /**
     * Aircraft altitude (hundreds of feet)
     */
    @DynamicSerializeElement
    @XmlElement
    private String aircraftAltitude = "";

    /**
     * Departure airport
     */
    @Column(length = 8)
    @DynamicSerializeElement
    @XmlElement
    private String departureAirport = "";

    /**
     * Departure airport time. Not used but stored in DB anyways for
     * completeness.
     */
    @Column(length = 8)
    @DynamicSerializeElement
    @XmlElement
    private String departureTime = "";

    /**
     * Destination airport
     */
    @Column(length = 8)
    @DynamicSerializeElement
    @XmlElement
    private String arrivalAirport = "";

    /**
     * Time of arrival at destination airport. Not used but stored in DB for
     * completeness.
     */
    @Column(length = 8)
    @DynamicSerializeElement
    @XmlElement
    private String arrivalTime = "";

    /**
     * Estimated flight direction. Not used but stored in DB for completeness.
     */
    @Column(length = 8)
    @DynamicSerializeElement
    @XmlElement
    private String estimatedFlightDirection = "";

    /**
     * Aircraft speed. Not used but stored in DB anyways for completeness.
     */
    @Column(length = 8)
    @DynamicSerializeElement
    @XmlElement
    private int aircraftSpeed = 0;

    /**
     * Flight status. Value is needed to determine if record is useful. For our
     * purposes it should be an "E". Can be used as validation check in DB;
     * should always be an "E".
     */
    @Column(length = 8)
    @DynamicSerializeElement
    @XmlElement
    private String flightStatus = "";

    /**
     * Age of flight report. Value is needed to determiner if record is useful.
     * For our purposes it should be a "CUR". Can be used as validation check in
     * DB; should always be an "CUR".
     */
    @Column(length = 8)
    @DynamicSerializeElement
    @XmlElement
    private String lastReportAge = "";

    /**
     * Default Constructor
     */
    public AsdiRecord() {
        this.reportType = "ASDI";
    }

    @Override
    public String getPluginName() {
        return "asdi";
    }

    public String getAircraftAltitude() {
        return aircraftAltitude;
    }

    public void setAircraftAltitude(String aircraftAltitude) {
        this.aircraftAltitude = aircraftAltitude;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public String getFlightType() {
        return flightType;
    }

    public void setFlightType(String flightType) {
        this.flightType = flightType;
    }

    public String getAircraftType() {
        return aircraftType;
    }

    public void setAircraftType(String aircraftType) {
        this.aircraftType = aircraftType;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public String getDepartureAirport() {
        return departureAirport;
    }

    public void setDepartureAirport(String departureAirport) {
        this.departureAirport = departureAirport;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }

    public String getArrivalAirport() {
        return arrivalAirport;
    }

    public void setArrivalAirport(String arrivalAirport) {
        this.arrivalAirport = arrivalAirport;
    }

    public String getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(String arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public String getEstimatedFlightDirection() {
        return estimatedFlightDirection;
    }

    public void setEstimatedFlightDirection(String estimatedFlightDirection) {
        this.estimatedFlightDirection = estimatedFlightDirection;
    }

    public int getAircraftSpeed() {
        return aircraftSpeed;
    }

    public void setAircraftSpeed(int aircraftSpeed) {
        this.aircraftSpeed = aircraftSpeed;
    }

    public String getFlightStatus() {
        return flightStatus;
    }

    public void setFlightStatus(String flightStatus) {
        this.flightStatus = flightStatus;
    }

    public String getLastReportAge() {
        return lastReportAge;
    }

    public void setLastReportAge(String lastReportAge) {
        this.lastReportAge = lastReportAge;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getReportType() {
        return reportType;
    }

}