package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Dimensionless;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import tec.uom.se.AbstractUnit;

/**
 * combined cloud coverage short code from three levels (AKA CLDS)
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 08/20/2016   R18194     R.Reynolds  Creation
 * 
 * </pre>
 * 
 * @author rreynolds
 * @version 1.0
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class CombinedCloud
        extends AbstractMetParameter<Dimensionless>
        implements ISerializableObject {

    private static final long serialVersionUID = -6058298343743033424L;

    public CombinedCloud() {
        super(AbstractUnit.ONE);
    }

    @Override
    public String getParameterDescription() {
        return "combined cloud coverage short code from three levels (AKA CLDS)";
    }

    // Low: 0 < h <= 6300 (ft)
    // Mid: 6300 < h < 17900 (ft)
    // High: 17900 <= h (actually it's coded as 17800 < h, but no height (ft)
    // increments < 100)
    // X O B S C and -S if few

    private int getIndexOfCloudCover(String coverage) {

        if (coverage.trim().startsWith("OVC")) {
            return 0;
        } else if (coverage.trim().startsWith("BKN")) {
            return 1;
        } else if (coverage.trim().startsWith("SCT")) {
            return 2;
        } else if (coverage.trim().startsWith("FEW")) {
            return 3;
        } else if (coverage.trim().startsWith("SKC")) {
            return 4;
        } else if (coverage.trim().startsWith("CLR")) {
            return 5;
        } else if (coverage.trim().startsWith("VV")) {
            return 6;
        } else if (coverage.trim().isEmpty()) {
            return 7;
        } else {
            return 7;
        }

    }

    private static final String getCloudCoverShortCodeFromIndex(int i) {
        final String[][] coverageCodes = new String[][] { { "OVC", "O" },
                { "BKN", "B" }, { "SCT", "S" }, { "FEW", "-S" }, { "SKC", "C" },
                { "CLR", "C" }, { "VV", "X" }, { "", "_" } };

        return coverageCodes[i][1];
    }

    @DeriveMethod
    AbstractMetParameter derive(SkyCoverage sc, CloudLayerBase clb)
            throws InvalidValueException, NullPointerException {

        int elevation = 0;

        String dbValsString[] = sc.getDbValsString(); // sky coverage
        Number dbValsNumber[] = clb.getDbValsNumber(); // cloud layer base

        setValueIsString();
        setValueToMissing();

        int indexOfLowestCloud = 7;
        int[] cloudLevel = new int[3];
        cloudLevel[0] = 7;
        cloudLevel[1] = 7;
        cloudLevel[2] = 7;

        // case where coverage exists but elevation is missing... output
        // coverage short code + "__"
        if (!dbValsString[0].isEmpty() && dbValsNumber[0].intValue() < 0) {
            this.setStringValue(getCloudCoverShortCodeFromIndex(
                    getIndexOfCloudCover(dbValsString[0])) + "__");
            return this;
        }

        for (int i = 0; i < dbValsString.length; i++) { // loop over sky
                                                        // coverage... OVC,
                                                        // BKN...

            elevation = dbValsNumber[i].intValue(); // get corresponding
                                                    // elevation

            if (elevation != -9999) { // if elevation is not a missing value
                                      // -9999
                indexOfLowestCloud = getIndexOfCloudCover(dbValsString[i]);

                if (elevation >= 0 && elevation <= 6300
                        && indexOfLowestCloud < cloudLevel[0]) {
                    cloudLevel[0] = indexOfLowestCloud;
                } else if (elevation > 6300 && elevation < 17900
                        && indexOfLowestCloud < cloudLevel[1]) {
                    cloudLevel[1] = indexOfLowestCloud;
                } else if (elevation > 17800
                        && indexOfLowestCloud < cloudLevel[2]) {
                    cloudLevel[2] = indexOfLowestCloud;
                }

            }

        }

        String val = getCloudCoverShortCodeFromIndex(cloudLevel[0])
                + getCloudCoverShortCodeFromIndex(cloudLevel[1])
                + getCloudCoverShortCodeFromIndex(cloudLevel[2]);

        this.setStringValue(val);
        return this;

    }
}
