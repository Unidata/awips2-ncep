package gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels;

import gov.noaa.nws.ncep.ui.pgen.display.IVector;
import gov.noaa.nws.ncep.ui.pgen.elements.SymbolLocationSet;
import gov.noaa.nws.ncep.viz.rsc.plotdata.rsc.Station;

import java.util.Collection;
import java.util.List;

import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.DrawableString;

/**
 * Interface via which NcPlotImageCreator reports its completed results back to
 * NcPlotResource2.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 08/31/2015              bhebbard    Initial creation -- spun out of NcPlotImageCreator
 * 11/17/2015    R9579     bhebbard    Fix header and import of Station class
 * 
 * </pre>
 * 
 * @author bhebbard
 * @version 1.0
 */

public interface IPointInfoRenderingListener {

    public void renderingComplete(DataTime time,
            Collection<Station> stationsRendered,
            List<DrawableString> stringsToDraw, List<IVector> vectorsToDraw,
            List<SymbolLocationSet> listOfSymbolLocSet);

    public void renderingAborted(DataTime dataTime);

}
