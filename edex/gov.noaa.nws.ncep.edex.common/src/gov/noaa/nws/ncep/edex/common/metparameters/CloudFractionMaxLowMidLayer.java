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
 * Maps to the GEMPAK parameter CFRL
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
public class CloudFractionMaxLowMidLayer extends AbstractMetParameter
        implements Dimensionless {
    /**
     * 
     */

    public CloudFractionMaxLowMidLayer() {
        super(UNIT);
    }

    // Low: 0 < h <= 6300 (ft)
    // Mid: 6300 < h < 17900 (ft)

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
    AbstractMetParameter derive(SkyCoverage sc, CloudLayerBase clb)
            throws InvalidValueException, NullPointerException {

        int elevation = 0;

        String dbValsString[] = sc.getDbValsString(); // sky coverage
        Number dbValsNumber[] = clb.getDbValsNumber(); // cloud layer base
        setValueToMissing();
        if ((dbValsString == null && dbValsNumber == null)
                || dbValsString == null) {
            return this;
        }

        int indexOfLowestCloud = this.getValue().intValue();

        for (int i = 0; i < dbValsString.length; i++) { // loop over sky
                                                        // coverage... OVC,
                                                        // BKN...

            if (dbValsNumber != null) {
                elevation = dbValsNumber[i].intValue(); // get corresponding
                                                        // elevation

                if (elevation != this.getMissing_data_value().intValue()) { // if
                                                                            // elevation
                                                                            // is
                                                                            // not
                                                                            // a
                                                                            // missing
                                                                            // value
                    // -9999
                    if (elevation >= 0 && elevation < 17900) {
                        indexOfLowestCloud = getIndexOfCloudCover(
                                dbValsString[i]);
                        if (!dbValsString[i].isEmpty()) {
                            int cloudCoverage = getIndexOfCloudCover(
                                    dbValsString[i]);

                            // Since we want to find the maximum coverage, the
                            // first time through we set the cloud coverage to
                            // first level
                            // and then only reset it if a higher amount of
                            // cloud
                            // coverage is found.

                            if (i == 0) {
                                indexOfLowestCloud = cloudCoverage;
                            } else if (cloudCoverage > indexOfLowestCloud) {
                                indexOfLowestCloud = cloudCoverage;
                            }
                        }
                    }
                }
            } else {
                if (!dbValsString[i].isEmpty()) {
                    int cloudCoverage = getIndexOfCloudCover(dbValsString[i]);

                    // Since we want to find the maximum coverage, the
                    // first time through we set the cloud coverage to first
                    // level
                    // and then only reset it if a higher amount of cloud
                    // coverage is found.

                    if (i == 0) {
                        indexOfLowestCloud = cloudCoverage;
                    } else if (cloudCoverage > indexOfLowestCloud) {
                        indexOfLowestCloud = cloudCoverage;
                    }
                }
            }
        }
        this.setValue(indexOfLowestCloud);
        return this;

    }
}
