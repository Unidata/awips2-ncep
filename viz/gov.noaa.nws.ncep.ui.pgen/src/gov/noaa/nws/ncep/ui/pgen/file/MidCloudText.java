//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.3 in JDK 1.6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2010.01.20 at 04:50:51 PM EST 
//

package gov.noaa.nws.ncep.ui.pgen.file;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for anonymous complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}Color" maxOccurs="unbounded"/>
 *         &lt;element ref="{}Point"/>
 *       &lt;/sequence>
 *       &lt;attribute name="cloudTypes" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="cloudAmounts" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="turbulenceType" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="turbulenceLevels" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="icingType" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="icingLevels" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="tstormTypes" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="tstormLevels" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="justification" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="style" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="fontName" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="fontSize" type="{http://www.w3.org/2001/XMLSchema}float" />
 *       &lt;attribute name="pgenType" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="pgenCategory" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "color", "point" })
@XmlRootElement(name = "MidCloudText")
public class MidCloudText {

    @XmlElement(name = "Color", required = true)
    protected List<Color> color;

    @XmlElement(name = "Point", required = true)
    protected Point point;

    @XmlAttribute
    protected String cloudTypes;

    @XmlAttribute
    protected String cloudAmounts;

    @XmlAttribute
    protected String turbulenceType;

    @XmlAttribute
    protected String turbulenceLevels;

    @XmlAttribute
    protected String icingType;

    @XmlAttribute
    protected String icingLevels;

    @XmlAttribute
    protected String tstormTypes;

    @XmlAttribute
    protected String tstormLevels;

    @XmlAttribute
    protected Integer ithw;

    @XmlAttribute
    protected Integer iwidth;

    @XmlAttribute
    protected String justification;

    @XmlAttribute
    protected String style;

    @XmlAttribute
    protected String fontName;

    @XmlAttribute
    protected Float fontSize;

    @XmlAttribute
    protected String pgenType;

    @XmlAttribute
    protected String pgenCategory;

    /**
     * Gets the value of the color property.
     * 
     * <p>
     * This accessor method returns a reference to the live list, not a
     * snapshot. Therefore any modification you make to the returned list will
     * be present inside the JAXB object. This is why there is not a
     * <CODE>set</CODE> method for the color property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * 
     * <pre>
     * getColor().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link Color }
     * 
     * 
     */
    public List<Color> getColor() {
        if (color == null) {
            color = new ArrayList<Color>();
        }
        return this.color;
    }

    /**
     * Gets the value of the point property.
     * 
     * @return possible object is {@link Point }
     * 
     */
    public Point getPoint() {
        return point;
    }

    /**
     * Sets the value of the point property.
     * 
     * @param value
     *            allowed object is {@link Point }
     * 
     */
    public void setPoint(Point value) {
        this.point = value;
    }

    /**
     * Gets the value of the cloudTypes property.
     * 
     * @return possible object is {@link String }
     * 
     */
    public String getCloudTypes() {
        return cloudTypes;
    }

    /**
     * Sets the value of the cloudTypes property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setCloudTypes(String value) {
        this.cloudTypes = value;
    }

    /**
     * Gets the value of the cloudAmounts property.
     * 
     * @return possible object is {@link String }
     * 
     */
    public String getCloudAmounts() {
        return cloudAmounts;
    }

    /**
     * Sets the value of the cloudAmounts property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setCloudAmounts(String value) {
        this.cloudAmounts = value;
    }

    /**
     * Gets the value of the turbulenceType property.
     * 
     * @return possible object is {@link String }
     * 
     */
    public String getTurbulenceType() {
        return turbulenceType;
    }

    /**
     * Sets the value of the turbulenceType property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setTurbulenceType(String value) {
        this.turbulenceType = value;
    }

    /**
     * Gets the value of the turbulenceLevels property.
     * 
     * @return possible object is {@link String }
     * 
     */
    public String getTurbulenceLevels() {
        return turbulenceLevels;
    }

    /**
     * Sets the value of the turbulenceLevels property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setTurbulenceLevels(String value) {
        this.turbulenceLevels = value;
    }

    /**
     * Gets the value of the icingType property.
     * 
     * @return possible object is {@link String }
     * 
     */
    public String getIcingType() {
        return icingType;
    }

    /**
     * Sets the value of the icingType property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setIcingType(String value) {
        this.icingType = value;
    }

    /**
     * Gets the value of the icingLevels property.
     * 
     * @return possible object is {@link String }
     * 
     */
    public String getIcingLevels() {
        return icingLevels;
    }

    /**
     * Sets the value of the icingLevels property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setIcingLevels(String value) {
        this.icingLevels = value;
    }

    /**
     * Gets the value of the tstormTypes property.
     * 
     * @return possible object is {@link String }
     * 
     */
    public String getTstormTypes() {
        return tstormTypes;
    }

    /**
     * Sets the value of the tstormTypes property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setTstormTypes(String value) {
        this.tstormTypes = value;
    }

    /**
     * Gets the value of the tstormLevels property.
     * 
     * @return possible object is {@link String }
     * 
     */
    public String getTstormLevels() {
        return tstormLevels;
    }

    /**
     * Sets the value of the tstormLevels property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setTstormLevels(String value) {
        this.tstormLevels = value;
    }

    /**
     * Gets the value of the ithw property.
     * 
     * @return possible object is {@link Integer }
     * 
     */
    public Integer getIthw() {
        return ithw;
    }

    /**
     * Sets the value of the ithw property.
     * 
     * @param value
     *            allowed object is {@link Integer }
     * 
     */
    public void setIthw(Integer value) {
        this.ithw = value;
    }

    /**
     * Gets the value of the iwidth property.
     * 
     * @return possible object is {@link Integer }
     * 
     */
    public Integer getIwidth() {
        return iwidth;
    }

    /**
     * Sets the value of the iwidth property.
     * 
     * @param value
     *            allowed object is {@link Integer }
     * 
     */
    public void setIwidth(Integer value) {
        this.iwidth = value;
    }

    /**
     * Gets the value of the justification property.
     * 
     * @return possible object is {@link String }
     * 
     */
    public String getJustification() {
        return justification;
    }

    /**
     * Sets the value of the justification property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setJustification(String value) {
        this.justification = value;
    }

    /**
     * Gets the value of the style property.
     * 
     * @return possible object is {@link String }
     * 
     */
    public String getStyle() {
        return style;
    }

    /**
     * Sets the value of the style property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setStyle(String value) {
        this.style = value;
    }

    /**
     * Gets the value of the fontName property.
     * 
     * @return possible object is {@link String }
     * 
     */
    public String getFontName() {
        return fontName;
    }

    /**
     * Sets the value of the fontName property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setFontName(String value) {
        this.fontName = value;
    }

    /**
     * Gets the value of the fontSize property.
     * 
     * @return possible object is {@link Float }
     * 
     */
    public Float getFontSize() {
        return fontSize;
    }

    /**
     * Sets the value of the fontSize property.
     * 
     * @param value
     *            allowed object is {@link Float }
     * 
     */
    public void setFontSize(Float value) {
        this.fontSize = value;
    }

    /**
     * Gets the value of the pgenType property.
     * 
     * @return possible object is {@link String }
     * 
     */
    public String getPgenType() {
        return pgenType;
    }

    /**
     * Sets the value of the pgenType property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setPgenType(String value) {
        this.pgenType = value;
    }

    /**
     * Gets the value of the pgenCategory property.
     * 
     * @return possible object is {@link String }
     * 
     */
    public String getPgenCategory() {
        return pgenCategory;
    }

    /**
     * Sets the value of the pgenCategory property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setPgenCategory(String value) {
        this.pgenCategory = value;
    }

}
