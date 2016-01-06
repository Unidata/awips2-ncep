package gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels;

import gov.noaa.nws.ncep.viz.rsc.plotdata.rsc.NcPlotResource2.Station;

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
 * Aug 31, 2015            bhebbard     Initial creation - spun out of NcPlotDataRequestor
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
