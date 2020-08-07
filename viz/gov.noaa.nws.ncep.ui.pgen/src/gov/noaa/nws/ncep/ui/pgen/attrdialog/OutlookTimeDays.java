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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

/**
 * Object for converting outlooktimes.xml tags for days to day java objects.
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
public class OutlookTimeDays {

    @XmlElements({ @XmlElement(name = "range", type = OutlookTimeRange.class) })
    private List<OutlookTimeRange> rangeList;

    @XmlAttribute
    private String name;

    @XmlAttribute
    private String defaultSelection;

    // - Getters/Setters
    public String getDefaultSelection() {
        return defaultSelection;
    }

    public void setDefaultSelection(String defaultSelection) {
        this.defaultSelection = defaultSelection;
    }

    public List<OutlookTimeRange> getRangeList() {
        return rangeList;
    }

    public void setRangeList(List<OutlookTimeRange> rangeList) {
        this.rangeList = rangeList;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
