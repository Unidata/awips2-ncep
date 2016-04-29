package gov.noaa.nws.ncep.viz.resources.misc;

import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.sampling.ISamplingResource;
import com.raytheon.viz.ui.cmenu.AbstractRightClickAction;

/**
 * 
 * Enables/disables sampling.
 * 
 * <pre>
 * 
 *  SOFTWARE HISTORY
 * 
 *  Date         Ticket#     Engineer     Description
 *  ------------ ----------  -----------  --------------------------
 * 11/13/2015    R13133      kbugenhagen  Initial Creation.
 * 12/15/2015    R13133      kbugenhagen  Refactored and moved to resources.misc project.
 * 
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1
 */

public class EnableDisableSamplingAction extends AbstractRightClickAction {

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.action.Action#run()
     */
    @Override
    public void run() {
        AbstractVizResource<?, ?> rsc = getSelectedRsc();
        if (rsc instanceof ISamplingResource) {
            boolean isEnabled = ((ISamplingResource) rsc).isSampling();
            ((ISamplingResource) rsc).setSampling(!isEnabled);
            rsc.issueRefresh();

            this.setChecked(!isEnabled);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.viz.ui.cmenu.AbstractRightClickAction#setSelectedRsc(com
     * .raytheon.uf.viz.core.rsc.AbstractVizResource)
     */
    @Override
    public void setSelectedRsc(ResourcePair selectedRsc) {
        super.setSelectedRsc(selectedRsc);
        AbstractVizResource<?, ?> rsc = getSelectedRsc();
        if (rsc instanceof ISamplingResource) {
            boolean isEnabled = ((ISamplingResource) rsc).isSampling();
            this.setChecked(isEnabled);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.action.Action#getText()
     */
    @Override
    public String getText() {
        return "Sample";
    }

}
