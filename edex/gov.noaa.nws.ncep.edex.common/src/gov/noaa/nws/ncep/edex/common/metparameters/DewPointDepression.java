package gov.noaa.nws.ncep.edex.common.metparameters;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;

import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.units.UnitAdapter;
/**
 * Maps to any of  the GEMPAK parameters DPDC or DPDK or DPDF 
 * depending on the unit used to measure the dewpoint depression.
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 22 Oct 2014   R4979      Dr. A Yuk   Correct Depression Temperature.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize

public class DewPointDepression extends AbstractMetParameter 
implements javax.measure.quantity.Temperature , ISerializableObject {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8246047973579792393L;

	public DewPointDepression() throws Exception {
		 super( UNIT );
	}
	
	@DeriveMethod
	public DewPointDepression derive( AirTemperature t, DewPointTemp d) throws Exception {
// Dr. Yuk  R4947  Oct. 20, 2014
   double offSetK=0; // R4947 inserted
		if ( t.hasValidValue() &&  d.hasValidValue() ){
			String unitStrNeeded = getUnitStr();
			
           if (unitStrNeeded.equalsIgnoreCase("Fahrenheit"))  { unitStrNeeded="C";} // R4947 inserted
            
			UnitAdapter ua = new UnitAdapter();

			Unit<?> unit = ua.unmarshal(unitStrNeeded);

			Amount tempAmount = new Amount(t.getValueAs( unit ), unit );

			Amount dewPointAmount = new Amount(d.getValueAs( unit ), unit );

			if (unitStrNeeded.equalsIgnoreCase("K") && tempAmount.doubleValue() > 200 ) offSetK= 273.15; // R4947 inserted
      
			Amount dwdpFinal = new Amount(tempAmount.doubleValue() - dewPointAmount.doubleValue()+offSetK,unit);    // R4947 inserted
			setValue(dwdpFinal );
			setUnit(unit);
		}
		return this;
    }
	
	// TODO : could change this to pass along the threshhold instead of hardcoding 30.
	@Override
	public String getFormattedString( String formatStr ) {
		
		if( formatStr == null || formatStr.isEmpty() ||
			formatStr.startsWith("%" ) ) {
			return super.getFormattedString( formatStr );
		}
		else if ( ( formatStr.compareToIgnoreCase("DPDX") == 0 ) ){			
			if( getValueAs( SI.CELSIUS ).doubleValue() >= 30.0 ) {
				return "X";
			}
			else {
				return super.getFormattedString( "%3.0f" );
			}
	    }
		else {
			return getValue().toString();
		}
	}
}

