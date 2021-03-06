//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.3 in JDK 1.6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.06.08 at 02:36:43 PM EDT 
//


package gov.noaa.nws.ncep.edex.common.stationTables;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the gov.noaa.nws.ncep.viz.common.stnTables package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Wfo_QNAME = new QName("", "wfo");
    private final static QName _Stid_QNAME = new QName("", "stid");
    private final static QName _Stnnum_QNAME = new QName("", "stnnum");
    private final static QName _Location_QNAME = new QName("", "location");
    private final static QName _Priority_QNAME = new QName("", "priority");
    private final static QName _Elevation_QNAME = new QName("", "elevation");
    private final static QName _State_QNAME = new QName("", "state");
    private final static QName _Longitude_QNAME = new QName("", "longitude");
    private final static QName _Stnname_QNAME = new QName("", "stnname");
    private final static QName _Latitude_QNAME = new QName("", "latitude");
    private final static QName _Country_QNAME = new QName("", "country");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: gov.noaa.nws.ncep.viz.common.stnTables
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link StationList }
     * 
     */
    public StationList createStationList() {
        return new StationList();
    }

    /**
     * Create an instance of {@link Station }
     * 
     */
    public Station createStation() {
        return new Station();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "wfo")
    public JAXBElement<String> createWfo(String value) {
        return new JAXBElement<String>(_Wfo_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "stid")
    public JAXBElement<String> createStid(String value) {
        return new JAXBElement<String>(_Stid_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "stnnum")
    public JAXBElement<String> createStnnum(String value) {
        return new JAXBElement<String>(_Stnnum_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "location")
    public JAXBElement<String> createLocation(String value) {
        return new JAXBElement<String>(_Location_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Integer }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "priority")
    public JAXBElement<Integer> createPriority(Integer value) {
        return new JAXBElement<Integer>(_Priority_QNAME, Integer.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Integer }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "elevation")
    public JAXBElement<Integer> createElevation(Integer value) {
        return new JAXBElement<Integer>(_Elevation_QNAME, Integer.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "state")
    public JAXBElement<String> createState(String value) {
        return new JAXBElement<String>(_State_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Float }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "longitude")
    public JAXBElement<Float> createLongitude(Float value) {
        return new JAXBElement<Float>(_Longitude_QNAME, Float.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "stnname")
    public JAXBElement<String> createStnname(String value) {
        return new JAXBElement<String>(_Stnname_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Float }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "latitude")
    public JAXBElement<Float> createLatitude(Float value) {
        return new JAXBElement<Float>(_Latitude_QNAME, Float.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "country")
    public JAXBElement<String> createCountry(String value) {
        return new JAXBElement<String>(_Country_QNAME, String.class, null, value);
    }

}
