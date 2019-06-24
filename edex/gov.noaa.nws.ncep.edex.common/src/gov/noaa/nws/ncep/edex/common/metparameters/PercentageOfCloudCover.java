package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Dimensionless;

import com.raytheon.uf.common.serialization.ISerializableObject;

import tec.uom.se.AbstractUnit;

public class PercentageOfCloudCover
        extends AbstractMetParameter<Dimensionless>
        implements ISerializableObject {

    private static final long serialVersionUID = -3939330897502523677L;

    public PercentageOfCloudCover() {
        super(AbstractUnit.ONE);
    }

}