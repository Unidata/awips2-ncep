package gov.noaa.nws.ncep.edex.common.metparameters;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;

import javax.measure.quantity.Dimensionless;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

/**
 * cloud max coverage elevation and short code (AKA CLDT)
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
public class CombinedCloudMaxCoverage extends AbstractMetParameter implements
        Dimensionless, ISerializableObject {

    private static final long serialVersionUID = -6058298343743033424L;

    public CombinedCloudMaxCoverage() {
        super(UNIT);
    }

    @Override
    public String getParameterDescription() {
        return "cloud max coverage elevation and short code (AKA CLDT)";
    }

    private static final String[][] coverageCodes = new String[][] {
            { "OVC", "O" }, { "BKN", "B" }, { "SCT", "S" }, { "FEW", "-S" },
            { "SKC", "C" }, { "CLR", "C" }, { "VV", "X" } };

    String val = "";

    private int elevation = 0;

    @DeriveMethod
    AbstractMetParameter derive(SkyCoverage sc, CloudLayerBase clb)
            throws InvalidValueException, NullPointerException {

        String dbValsString[] = sc.getDbValsString(); // sky coverage
        Number dbValsNumber[] = clb.getDbValsNumber(); // cloud layer base

        setValueIsString();
        setValueToMissing();

        int lowestElevation = -9999;
        int lowestIndex = coverageCodes.length;

        for (int i = 0; i < dbValsString.length; i++) { // loop over sky
                                                        // coverage, low to
                                                        // high.
            elevation = dbValsNumber[i].intValue();

            for (int j = 0; j < coverageCodes.length; j++) {
                if (dbValsString[i].startsWith(coverageCodes[j][0])) {
                    if (j < lowestIndex) {
                        lowestIndex = j;
                        lowestElevation = elevation;
                    }
                }

            }

        }

        if (lowestElevation != -9999) {
            val = Integer.toString((int) (lowestElevation * .01))
                    + coverageCodes[lowestIndex][1];
        } else {
            val = coverageCodes[lowestIndex][1];
        }
        this.setStringValue(val);

        return this;
    }

}
