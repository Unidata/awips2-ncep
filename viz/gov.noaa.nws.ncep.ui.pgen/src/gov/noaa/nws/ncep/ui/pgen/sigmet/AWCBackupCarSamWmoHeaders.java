package gov.noaa.nws.ncep.ui.pgen.sigmet;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Implementation of AWCBackupCarSamWmoHeaders
 *
 * <pre>
 *
 *  SOFTWARE HISTORY
 *  Date           Ticket#     Engineer    Description
 *  ------------   ----------  ----------- -----------------------------------------
 * Apr 05, 2021    90325       smanoj      CARSAM Backup WMO headers configurable
 *
 * </pre>
 *
 * @author 
 */
@XmlRootElement(name = "AWCBackupCarSamWmoHeaders")
@XmlAccessorType(XmlAccessType.NONE)
public class AWCBackupCarSamWmoHeaders {

    @XmlElements({
            @XmlElement(name = "CarSamBackupWmoHeader", type = CarSamBackupWmoHeader.class) })
    List<CarSamBackupWmoHeader> carSamBackupWmoHeader;

    public List<CarSamBackupWmoHeader> getCarSamBackupWmoHeader() {
        return carSamBackupWmoHeader;
    }

    public void setCarSamBackupWmoHeader(List<CarSamBackupWmoHeader> carSamBackupWmoHeader) {
        this.carSamBackupWmoHeader = carSamBackupWmoHeader;
    }

    /**
     * Constructor
     */
    public AWCBackupCarSamWmoHeaders() {
        this.carSamBackupWmoHeader = new ArrayList<>();
    }

    public String getWMOHeader(String firID, String hazardType) {
        String wmoHeader;
        for (CarSamBackupWmoHeader iCarSamFir : getCarSamBackupWmoHeader()) {
            if (firID.equals(iCarSamFir.getFirID())) {
                if (hazardType.contains("VA")) {
                    wmoHeader = iCarSamFir.getWmoHeaderForVA() + " "
                            + iCarSamFir.getWmoID();
                } else if (hazardType.contains("TC")) {
                    wmoHeader = iCarSamFir.getWmoHeaderForTC() + " "
                            + iCarSamFir.getWmoID();
                } else {
                    wmoHeader = iCarSamFir.getWmoHeaderForOther() + " "
                            + iCarSamFir.getWmoID();
                }

                return wmoHeader;
            }
        }
        return "";
    }

    @Override
    public String toString() {
        return "CarSamBackupWmoHeader [carSamBackupWmoHeader=" + carSamBackupWmoHeader + "]";
    }
}
