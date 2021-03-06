//
//This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.0.5-b02-fcs 
//See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
//Any modifications to this file will be lost upon recompilation of the source schema. 
//Generated on: 2009.05.21 at 09:09:51 AM EDT 
//

package gov.noaa.nws.ncep.ui.pgen.file;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
* <p>Java class for TrackPoint complex type.
* 
* <p>The following schema fragment specifies the expected content contained within this class.
* 
* <pre>
* &lt;complexType name="TrackPoint">
*   &lt;complexContent>
*     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
*       &lt;sequence>
*         &lt;element name="time" type="{http://www.w3.org/2001/XMLSchema}dateTime"/>
*         &lt;element name="location">
*           &lt;complexType>
*             &lt;complexContent>
*               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
*                 &lt;attribute name="latitude" type="{http://www.w3.org/2001/XMLSchema}double" />
*                 &lt;attribute name="longitude" type="{http://www.w3.org/2001/XMLSchema}double" />
*               &lt;/restriction>
*             &lt;/complexContent>
*           &lt;/complexType>
*         &lt;/element>
*       &lt;/sequence>
*     &lt;/restriction>
*   &lt;/complexContent>
* &lt;/complexType>
* </pre>
* 
* 
*/
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TrackPoint", propOrder = {
 "time",
 "location"
})
public class TrackPoint {

 @XmlElement(required = true)
 protected XMLGregorianCalendar time;
 @XmlElement(required = true)
 protected TrackPoint.Location location;

 /**
  * Gets the value of the time property.
  * 
  * @return
  *     possible object is
  *     {@link XMLGregorianCalendar }
  *     
  */
 public XMLGregorianCalendar getTime() {
     return time;
 }

 /**
  * Sets the value of the time property.
  * 
  * @param value
  *     allowed object is
  *     {@link XMLGregorianCalendar }
  *     
  */
 public void setTime(XMLGregorianCalendar value) {
     this.time = value;
 }

 /**
  * Gets the value of the location property.
  * 
  * @return
  *     possible object is
  *     {@link TrackPoint.Location }
  *     
  */
 public TrackPoint.Location getLocation() {
     return location;
 }

 /**
  * Sets the value of the location property.
  * 
  * @param value
  *     allowed object is
  *     {@link TrackPoint.Location }
  *     
  */
 public void setLocation(TrackPoint.Location value) {
     this.location = value;
 }


 /**
  * <p>Java class for anonymous complex type.
  * 
  * <p>The following schema fragment specifies the expected content contained within this class.
  * 
  * <pre>
  * &lt;complexType>
  *   &lt;complexContent>
  *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
  *       &lt;attribute name="latitude" type="{http://www.w3.org/2001/XMLSchema}double" />
  *       &lt;attribute name="longitude" type="{http://www.w3.org/2001/XMLSchema}double" />
  *     &lt;/restriction>
  *   &lt;/complexContent>
  * &lt;/complexType>
  * </pre>
  * 
  * 
  */
 @XmlAccessorType(XmlAccessType.FIELD)
 @XmlType(name = "")
 public static class Location {

     @XmlAttribute
     protected Double latitude;
     @XmlAttribute
     protected Double longitude;

     /**
      * Gets the value of the latitude property.
      * 
      * @return
      *     possible object is
      *     {@link Double }
      *     
      */
     public Double getLatitude() {
         return latitude;
     }

     /**
      * Sets the value of the latitude property.
      * 
      * @param value
      *     allowed object is
      *     {@link Double }
      *     
      */
     public void setLatitude(Double value) {
         this.latitude = value;
     }

     /**
      * Gets the value of the longitude property.
      * 
      * @return
      *     possible object is
      *     {@link Double }
      *     
      */
     public Double getLongitude() {
         return longitude;
     }

     /**
      * Sets the value of the longitude property.
      * 
      * @param value
      *     allowed object is
      *     {@link Double }
      *     
      */
     public void setLongitude(Double value) {
         this.longitude = value;
     }

 }

}
