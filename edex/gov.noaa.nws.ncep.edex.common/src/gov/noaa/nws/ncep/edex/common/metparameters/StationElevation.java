package gov.noaa.nws.ncep.edex.common.metparameters;




//import com.raytheon.uf.viz.core.catalog.DirectDbQuery;

//import com.raytheon.uf.viz.core.catalog.DirectDbQuery.QueryLanguage;
//import com.raytheon.uf.viz.core.exception.VizException;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.adapters.UnitAdapter;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary;
//import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidRangeException; 
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;

/**
 * Maps to the GEMPAK parameter SELV
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize



public class StationElevation extends AbstractMetParameter implements javax.measure.quantity.Length,
  ISerializableObject{

     /**
	 * 
	 */
	private static final long serialVersionUID = 2767610303034472411L;

	public StationElevation() throws Exception {
		 super( new UnitAdapter().marshal(UNIT) );
	}	
	
//  	// TODO : not implemented, hard code to test derived parameters
//  	@DeriveMethod
//     AbstractMetParameter derive( StationID sid ) throws InvalidValueException, NullPointerException {
//  		if( sid.hasValidValue() ) {
//  			
//  			
//  		}
//  		
//  		this.setValue( 10, SI.METER );
//  		
//  		return this;
//     }

     // TODO : check that this is stationPressure is correct here  
 	@DeriveMethod
     AbstractMetParameter derive( SeaLevelPressure alti, SurfacePressure pres ) throws InvalidValueException, NullPointerException {
    	  if ( alti.hasValidValue() && pres.hasValidValue() ){
 		       Amount val = PRLibrary.prZalt(alti, pres );
    	       setValue(val);
    	  }else
    		  setValueToMissing();
    	 return this;
     }
//TODO check if this functionality can actually be moved into the plot resource project
// 	@DeriveMethod
//  AbstractMetParameter derive( StationID sid ) throws InvalidValueException, NullPointerException {
//		// TODO : look up the lat from the station name.
//		//    	 this.setValue(val);
//
//     StringBuilder query = new StringBuilder("select elevation from ");
// 	query.append("obs ");
// 	query.append("where stationid = '");
// 	query.append(sid.getStringValue());
// 	query.append("' ");
// 	query.append(" and reporttype = 'METAR';");
// 	try {
//			List<Object[]> results = DirectDbQuery.executeQuery(query.toString(), "metadata", QueryLanguage.SQL);
//			if(results != null && results.size() > 0){
//				
//				Object[] theObjectArray = results.get(0);
// 		        Integer selv = ( Integer ) theObjectArray[0];
//				setValue(new Amount ( selv, SI.METER ) ) ;
//
//			}
//		} catch (VizException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
// 	return this;
//  }
 	
 	
 }

 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 