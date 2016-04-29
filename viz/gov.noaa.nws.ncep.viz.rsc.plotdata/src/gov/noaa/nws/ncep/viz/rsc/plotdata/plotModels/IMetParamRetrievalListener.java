package gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels;

import gov.noaa.nws.ncep.viz.rsc.plotdata.rsc.Station;

import java.util.Collection;

import com.raytheon.uf.common.time.DataTime;

/**
 * Interface via which NcPlotDataRequestor reports its completed results back to
 * NcPlotResource2.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 08/31/2015              bhebbard    Initial creation - spun out of NcPlotDataRequestor
 * 11/17/2015    R9579     bhebbard    Change import of Station class - no longer an inner class
 * 
 * 
 * </pre>
 * 
 * @author bhebbard
 * @version 1.0
 */

public interface IMetParamRetrievalListener {

    public void retrievalAborted(DataTime time);

    public void retrievalComplete(DataTime time,
            Collection<Station> retrievedStations,
            int stationsRetrievedThisCallCount,
            boolean isThereAConditionalFilter);

}
