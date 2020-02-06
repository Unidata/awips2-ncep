/**
 * 
 */
package gov.noaa.nws.ncep.edex.common.metparameters;

import javax.measure.quantity.Temperature;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

import gov.noaa.nws.ncep.edex.common.metparameters.MetParameterFactory.DeriveMethod;
import gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.PRLibrary.InvalidValueException;
import si.uom.SI;

/**
 * Maps to the GEMPAK parameter TNCF
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class ClimNightTemp extends AbstractMetParameter<Temperature>
        implements ISerializableObject {

    /**
    * 
    */
    private static final long serialVersionUID = 7198420674894755646L;

    public ClimNightTemp() {
        super(SI.KELVIN);
    }

    @Override
    public String getParameterDescription() {
        return "Climatological Night-time temperature.";
    }

    @DeriveMethod
    public ClimNightTemp derive(StationID stid)
            throws InvalidValueException, NullPointerException {
        /*
         * Commented out the code to refrain from accessing 'viz core ' code
         * from 'edex common'.
         */
        // String stidStr = stid.getStringValue();
        // if (stidStr != null ){
        // if ( stidStr.length() == 4 && stidStr.charAt(0) == 'K'){
        // stidStr = stidStr.substring(1);
        // }
        // Calendar cal = Calendar.getInstance();
        // int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        // int month = cal.get(Calendar.MONTH);
        // String queryString = "select tntf from stns.climo_data where
        // station_id ='"+stidStr
        // + "' and month ="+ month
        // + " and day ="
        // + dayOfMonth+";";
        // try {
        //
        // QueryResult qr = DirectDbQuery.executeMappedQuery(queryString,
        // "ncep", QueryLanguage.SQL);
        // if ( qr != null ){
        // if ( qr.getResultCount() > 0){
        // QueryResultRow[] queryResultsArray = qr.getRows();
        // for ( QueryResultRow eachRow : queryResultsArray ){
        // Double tempInF = (Double) eachRow.getColumn(0);
        // setValueAs(tempInF, "°F");
        // }
        // }
        // }
        //
        // } catch (VizException e) {
        //
        // return this;
        // }
        //
        //
        // }
        return this;
    }
}
