package gov.noaa.nws.ncep.viz.rsc.satellite.rsc;

import gov.noaa.nws.ncep.viz.resources.AbstractDataLoader;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData;
import gov.noaa.nws.ncep.viz.resources.IDataLoader;

import com.raytheon.uf.viz.core.exception.VizException;

/**
 * TODO Add Description
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 21, 2016 R15954     SRussell    Initial creation
 * 
 * </pre>
 * 
 * @author srussell
 * @version 1.0
 */

public class NcSatelliteDataLoader extends AbstractDataLoader implements
        IDataLoader {

    public NcSatelliteDataLoader() {
    }

    public NcSatelliteDataLoader(
            AbstractNatlCntrsRequestableResourceData resourceData) {
        this.resourceData = resourceData;
    }

    @Override
    public void setResourceData(
            AbstractNatlCntrsRequestableResourceData resourceData) {
        this.resourceData = resourceData;
    }

    @Override
    public void loadData() {
        try {
            queryRecords();
        } catch (VizException e) {
            statusHandler.error("Error Querying Records:  ", e);
        }
        processNewRscDataList();
    }

}// end class NcSatelliteDataLoader
