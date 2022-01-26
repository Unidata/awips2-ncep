package gov.noaa.nws.ncep.ui.pgen.sigmet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * 
 * Implementation of CarSamBackupWmoHeader xml element
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- ---------------------------------------
 * Apr 05, 2021  90325      smanoj      CARSAM Backup WMO headers configurable.
 * 
 * </pre>
 *
 * @author 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "CarSamBackupWmoHeader")
public class CarSamBackupWmoHeader {

    @XmlAttribute(name = "firID")
    private String firID;

    @XmlAttribute(name = "icaoID")
    private String icaoID;

    @XmlAttribute(name = "wmoID")
    private String wmoID;

    @XmlAttribute(name = "wmoHeaderForVA")
    private String wmoHeaderForVA;

    @XmlAttribute(name = "wmoHeaderForTC")
    private String wmoHeaderForTC;

    @XmlAttribute(name = "wmoHeaderForOther")
    private String wmoHeaderForOther;

    public CarSamBackupWmoHeader() {

    }

    public CarSamBackupWmoHeader(String pFirID, String pIcaoID, String pWmoID,
            String pWmoHeaderForVA, String pWmoHeaderForTC,
            String pWmoHeaderForOther) {

        firID = pFirID;
        icaoID = pIcaoID;
        wmoID = pWmoID;
        wmoHeaderForVA = pWmoHeaderForVA;
        wmoHeaderForTC = pWmoHeaderForTC;
        wmoHeaderForOther = pWmoHeaderForOther;
    }

    public String getFirID() {
        return firID;
    }

    public String getIcaoID() {
        return icaoID;
    }

    public String getWmoID() {
        return wmoID;
    }

    public String getWmoHeaderForVA() {
        return wmoHeaderForVA;
    }

    public String getWmoHeaderForTC() {
        return wmoHeaderForTC;
    }

    public String getWmoHeaderForOther() {
        return wmoHeaderForOther;
    }

    public void setFirID(String firID) {
        this.firID = firID;
    }

    public void setIcaoID(String icaoID) {
        this.icaoID = icaoID;
    }

    public void setWmoID(String wmoID) {
        this.wmoID = wmoID;
    }

    public void setWmoHeaderForVA(String wmoHeaderForVA) {
        this.wmoHeaderForVA = wmoHeaderForVA;
    }

    public void setWmoHeaderForTC(String wmoHeaderForTC) {
        this.wmoHeaderForTC = wmoHeaderForTC;
    }

    public void setWmoHeaderForOther(String wmoHeaderForOther) {
        this.wmoHeaderForOther = wmoHeaderForOther;
    }

    @Override
    public String toString() {
        return "CarSamBackupWmoHeader [firID=" + firID + ", wmoID=" + wmoID + "]";
    }

}
