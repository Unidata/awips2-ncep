/**
 * 
 */
package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Dimensionless;

import com.raytheon.uf.common.serialization.ISerializableObject;

import tec.uom.se.AbstractUnit;

/**
 * @author archana
 *
 */
public class StationNumber
        extends AbstractMetParameter<Dimensionless>
        implements ISerializableObject {

    private static final long serialVersionUID = 2038229143862266928L;

    public StationNumber() throws Exception {
        super(AbstractUnit.ONE);
        setValueIsString();
    }

}
