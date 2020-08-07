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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * Object for converting outlooktimes.xml tags for range to range java objects.
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
public class OutlookTimeRange {

    @XmlAttribute
    private String from;

    @XmlAttribute
    private String to;

    @XmlAttribute
    private String init;

    @XmlAttribute
    private String initAdj;

    @XmlAttribute
    private String exp;

    @XmlAttribute
    private String expAdj;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getInit() {
        return init;
    }

    public void setInit(String init) {
        this.init = init;
    }

    public String getInitAdj() {
        return initAdj;
    }

    public void setInitAdj(String initAdj) {
        this.initAdj = initAdj;
    }

    public String getExp() {
        return exp;
    }

    public void setExp(String exp) {
        this.exp = exp;
    }

    public String getExpAdj() {
        return expAdj;
    }

    public void setExpAdj(String expAdj) {
        this.expAdj = expAdj;
    }

}
