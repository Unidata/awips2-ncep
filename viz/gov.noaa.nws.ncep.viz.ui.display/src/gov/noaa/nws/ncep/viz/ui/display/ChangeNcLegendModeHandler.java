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
package gov.noaa.nws.ncep.viz.ui.display;

import gov.noaa.nws.ncep.viz.ui.display.NCLegendResource.LegendMode;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.viz.ui.EditorUtil;

/**
 * Cycles through the NCLegendResource's modes
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 9/25/2015    R8833      N.Jensen    Initial creation
 * 
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public class ChangeNcLegendModeHandler extends AbstractHandler {

    // code borrowed from GFE's ToggleLegend

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IDisplayPaneContainer container = EditorUtil.getActiveVizContainer();
        // if there are displays present
        if (container != null) {
            List<NCLegendResource> decors = container.getActiveDisplayPane()
                    .getDescriptor().getResourceList()
                    .getResourcesByTypeAsType(NCLegendResource.class);
            for (NCLegendResource nclegend : decors) {
                LegendMode mode = nclegend.getLegendMode();

                int position = mode.ordinal();
                position++;
                if (position >= LegendMode.values().length) {
                    position = 0;
                }

                nclegend.setLegendMode(LegendMode.values()[position]);

            }
            container.refresh();
        }
        return null;

    }

}
