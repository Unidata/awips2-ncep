package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Length;

import com.raytheon.uf.common.serialization.ISerializableObject;

import si.uom.SI;

public class Precipitation
        extends AbstractMetParameter<Length>
        implements ISerializableObject {

    private static final long serialVersionUID = 9118116421916778963L;

    public Precipitation() {
        super(SI.METRE);
    }

}
