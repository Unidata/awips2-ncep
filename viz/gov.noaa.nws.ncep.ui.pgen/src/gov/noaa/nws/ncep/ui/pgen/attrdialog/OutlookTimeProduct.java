/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package gov.noaa.nws.ncep.ui.pgen.attrdialog;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

/**
 * Object for converting outlooktimes.xml tags for ProdcutType to product java
 * objects.
 * 
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#     Engineer  Description
 * ------------- ----------- --------- -----------------------------------------
 * Aug 20,2020   80844       pbutler   Update for changing default Days/Prods activity and days from outlooktimes.xml config file.
 * 
 * </pre>
 *
 * @author pbutler
 */

@XmlAccessorType(XmlAccessType.NONE)
public class OutlookTimeProduct {

    @XmlElement(name = "ProductType")
    private String productType;

    @XmlAttribute
    private String subType;

    @XmlAttribute
    private String name;

    @XmlAttribute
    private String type;

    @XmlElements({ @XmlElement(name = "days", type = OutlookTimeDays.class) })
    private List<OutlookTimeDays> daysList;

    private boolean isDefault;

    /**
     * Get a list of day names.
     * 
     * @param days
     * @return List<String>
     */
    public List<String> getDayNames(List<OutlookTimeDays> days) {
        List<String> dayNames = new ArrayList<String>();

        for (OutlookTimeDays day : days) {
            dayNames.add(day.getName());
        }

        return dayNames;
    }

    /**
     * Return index of default day in day list.
     * 
     * @param days
     * @return int
     */
    public int getDaysIndex(List<OutlookTimeDays> days) {
        int index = 0;

        for (OutlookTimeDays day : days) {
            if (null != day.getDefaultSelection()) {
                if (day.getDefaultSelection().equals("true")) {
                    index = days.indexOf(day);
                }
            }
        }

        return index;
    }

    // - Getters/Setters
    public List<OutlookTimeDays> getDays() {
        return daysList;
    }

    public void setDays(List<OutlookTimeDays> days) {
        this.daysList = days;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

}
