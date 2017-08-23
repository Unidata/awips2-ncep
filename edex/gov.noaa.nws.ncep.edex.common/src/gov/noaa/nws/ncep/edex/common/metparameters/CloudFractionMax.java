/**
 * 
 */
package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Dimensionless;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;

/**
 * Maps to the GEMPAK parameter CFRT
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * -------      -------     --------    -----------
 * 11/17/2016   R26156      J. Huber    Initial creation
 * </pre>
 * 
 * @author Joshua Huber
 * @version 1.0
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class CloudFractionMax extends AbstractMetParameter
        implements Dimensionless {
    /**
     * 
     */

    public CloudFractionMax() {
        super(UNIT);
    }

    private int getIndexOfCloudCover(String coverage) {

        if (coverage.trim().startsWith("SKC")
                || coverage.trim().startsWith("CLR")) {
            return 0;
        } else if (coverage.trim().startsWith("FEW")) {
            return 2;
        } else if (coverage.trim().startsWith("SCT")) {
            return 3;
        } else if (coverage.trim().startsWith("BKN")) {
            return 6;
        } else if (coverage.trim().startsWith("OVC")) {
            return 8;
        } else if (coverage.trim().startsWith("VV")) {
            return 9;
        } else if (coverage.trim().isEmpty()) {
            return 7;
        } else {
            return 7;
        }

    }

    @DeriveMethod
    AbstractMetParameter derive(SkyCoverage sc)
            throws InvalidValueException, NullPointerException {

        String dbValsString[] = sc.getDbValsString(); // sky coverage

        setValueToMissing();

        int indexOfLowestCloud = this.getValue().intValue();

        for (int i = 0; i < dbValsString.length; i++) {

            // Only iterate if there is some cloud coverage reported in the
            // specific array element.

            if (!dbValsString[i].isEmpty()) {
                int cloudCoverage = getIndexOfCloudCover(dbValsString[i]);

                // Since we want to find the maximum coverage, the first time
                // through we set the cloud coverage to first level and then
                // only reset it if a higher amount of cloud coverage is found.

                if (i == 0) {
                    indexOfLowestCloud = cloudCoverage;
                } else if (cloudCoverage > indexOfLowestCloud) {
                    indexOfLowestCloud = cloudCoverage;
                }
            }

        }
        this.setValue(indexOfLowestCloud);
        return this;

    }
}