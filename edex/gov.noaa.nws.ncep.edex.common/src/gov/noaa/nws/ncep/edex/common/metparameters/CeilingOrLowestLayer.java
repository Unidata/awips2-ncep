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
 * CEIL or lowest level if no CEIL (AKA CLDB)
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
public class CeilingOrLowestLayer extends AbstractMetParameter implements
        Dimensionless, ISerializableObject {

    private static final long serialVersionUID = -6058298343743033424L;

    public CeilingOrLowestLayer() {
        super(UNIT);
    }

    @Override
    public String getParameterDescription() {
        return "CEIL or lowest level if no CEIL (AKA CLDB)";
    }

    String[][] cloudCoverage = new String[][] { { "CLR", "C" }, { "SKC", "C" },
            { "FEW", "-S" }, { "SCT", "S" }, { "BKN", "B" }, { "OVC", "O" },
            { "VV", "X" } };

    private String getShortCode(String cc) {

        for (int i = 0; i < cloudCoverage.length; i++) {
            if (cc.startsWith(cloudCoverage[i][0])) {
                return cloudCoverage[i][1];
            }
        }
        return "_"; // return this if no coverage is found

    }

    @DeriveMethod
    AbstractMetParameter derive(CeilingFromSurface cfs, SkyCoverage sc,
            CloudLayerBase clb) throws InvalidValueException,
            NullPointerException {

        String dbValsString[] = sc.getDbValsString();
        Number dbValsNumber[] = clb.getDbValsNumber();

        setValueIsString();
        // setValueToMissing(); // default is no ceiling
        String val = "";

        this.setStringValue(val);
        if (cfs.hasValidValue() && cfs.getValue().intValue() > -1) {
            // got a good ceiling value, find its short code
            for (int i = 0; i < dbValsNumber.length; i++) {
                // find ceil's elevation
                if (cfs.getValue().intValue() == dbValsNumber[i].intValue() * .01) {
                    val = Integer.toString(cfs.getValue().intValue())
                            + getShortCode(dbValsString[i]);

                    this.setStringValue(val);
                    return this;
                }
            }

        } else {
            // no ceil found so get lowest layer regardless of coverage.
            String dbvs = "";
            for (int i = 0; i < dbValsString.length; i++) {
                dbvs = dbValsString[i];
                if (dbvs.startsWith("CLR") || dbvs.startsWith("SKC")
                        || dbvs.startsWith("FEW") || dbvs.startsWith("SCT")
                        || dbvs.startsWith("BKN") || dbvs.startsWith("OVC")
                        || dbvs.startsWith("VV")) {

                    if (dbValsNumber[i].intValue() > -1) {
                        // add cloud elevation if not CLR or SKC
                        if (!dbvs.startsWith("CLR") || !dbvs.startsWith("SKC")) {
                            val = Integer.toString((int) (dbValsNumber[i]
                                    .intValue() * .01)); // elevation
                        }
                    }
                    val += getShortCode(dbvs); // short code

                    this.setStringValue(val);

                    return this;

                }

            }

        }

        return this;
    }
}
