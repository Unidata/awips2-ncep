package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Dimensionless;

import com.raytheon.uf.common.serialization.ISerializableObject;

import tec.uom.se.AbstractUnit;

// The cloud coverage at a given height. Values are strings representing 
// metar observations FEW, OVC,
//
// A list of CloudCovers is used to determine the skyCoverage.
//
public class CloudCover
        extends AbstractMetParameter<Dimensionless>
        implements ISerializableObject {

    private static final long serialVersionUID = 3361597684903438954L;

    public CloudCover() {
        super(AbstractUnit.ONE);
    }

    @Override
    public Boolean hasStringValue() {
        return true;
    };
}