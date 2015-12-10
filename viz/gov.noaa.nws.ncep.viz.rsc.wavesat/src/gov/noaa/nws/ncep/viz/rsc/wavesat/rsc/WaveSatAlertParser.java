package gov.noaa.nws.ncep.viz.rsc.wavesat.rsc;

import gov.noaa.nws.ncep.viz.rsc.wavesat.rsc.WaveSatResource.WaveSatRscDataObj;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.alerts.AbstractAlertMessageParser;
import com.raytheon.uf.viz.core.alerts.AlertMessage;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractRequestableResourceData;

/**
 * Since WaveSat is using PointDataRequests to retrieve its data because a
 * normal BaseRequest script for SgwhRecords does not contain the data from
 * hdf5. But this also means that the default ParseNatlCntrsAlertMessage also
 * will not work for the same reason.
 * 
 * Instead of making a separate PointDataRequest call for every single new Sgwh
 * alert we will just save off some basic info that the WaveSat resource can use
 * later to decide if it needs to make another query.
 * 
 * This would be much easier and more efficient if the sgwh, windSpeed....
 * parameters were in the URI (or at least in the postgres DB) so we wouldn't
 * have to make another PointDataRequest.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Unknown      Unknow     Unknown     Initial creation
 * 12/03/2015   R11818     srussell	   Modified for AbstractWaveSatResource
 *                                     being renamed to WaveSatResource.
 *                                     The children of AbstractWaveSatResource,
 *                                     WaveSatResource and WaveSatVResource
 *                                     have been deleted.
 * 
 * </pre>
 * 
 * @author Unknown
 * @version 1.0
 */

@XmlAccessorType(XmlAccessType.NONE)
public class WaveSatAlertParser extends AbstractAlertMessageParser {

    @Override
    public Object parseAlertMessage(AlertMessage message,
            AbstractRequestableResourceData resourceData) throws VizException {

        WaveSatRscDataObj waveSatData = new WaveSatRscDataObj();
        waveSatData.dataTime = (DataTime) message.decodedAlert.get("dataTime");
        if (resourceData instanceof WaveSatResourceData) {
            // For LAT, LON, and SAT_ID, the param names depend on which EDEX
            // plugin -- and therefore which resource class -- we're using
            WaveSatResource resource = ((WaveSatResourceData) resourceData)
                    .getResource();
            waveSatData.lat = (Double) message.decodedAlert
                    .get(resource.latParamInPdo);
            waveSatData.lon = (Double) message.decodedAlert
                    .get(resource.lonParamInPdo);
            waveSatData.satelliteId = Long.toString((Long) message.decodedAlert
                    .get(resource.satIdParamInPdo));
        } else {
            // TODO error!
        }

        // TODO: Get the waveHeight, windSpeed, windDir from the message.
        // Until then, set a flag to let the resource know that it needs to
        // requery the data.
        waveSatData.createdFromAutoUpdate = true;

        return waveSatData;
    }

}
