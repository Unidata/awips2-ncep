/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels;

import gov.noaa.nws.ncep.ui.pgen.display.IVector;
import gov.noaa.nws.ncep.ui.pgen.elements.SymbolLocationSet;
import gov.noaa.nws.ncep.viz.rsc.plotdata.rsc.NcPlotResource2.Station;

import java.util.Collection;
import java.util.List;

import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.DrawableString;

/**
 * Interface via which NcPlotImageCreeator reports its completed results back to
 * NcPlotResource2.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 31, 2015            bhebbard     Initial creation -- spun out of NcPlotImageCreator
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
