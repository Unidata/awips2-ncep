package gov.noaa.nws.ncep.viz.rsc.ncgrid.actions;

import gov.noaa.nws.ncep.viz.gempak.util.CommonDateFormatUtil;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceDefnsMngr;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.rsc.NcEnsembleResourceData;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.rsc.NcgridResource;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.rsc.NcgridResourceData;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.rsc.SaveGridDialog;

import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.viz.ui.cmenu.AbstractRightClickAction;

/**
 * Right-click action handler for saving blended grid results.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer      Description
 * ------------ ---------- -----------   --------------------------
 * 03/01/2016   R6821      kbugenhagen   Initial creation
 * 04/05/2016   R15715     dgilling      Refactored for new AbstractEditResourceAttrsDialog constructor.
 * 08/18/2016   R17569     K Bugenhagen  Modified calls to NcEnsembleResourceData methods 
 *                                       since they are no longer static.
 * 
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1.0
 */

public class SaveGridAction extends AbstractRightClickAction {

    protected static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(SaveGridAction.class);

    private NcgridResource gridRsc;

    public SaveGridAction() {
        super(Action.AS_PUSH_BUTTON);
    }

    /**
     * @see org.eclipse.jface.action.Action#run()
     */
    @Override
    public void run() {
        ResourceDefnsMngr mgr = null;
        try {
            mgr = ResourceDefnsMngr.getInstance();
        } catch (VizException e) {
            statusHandler.handle(
                    Priority.PROBLEM,
                    "Error getting ResourceDefnsMngr instance: "
                            + e.getLocalizedMessage(), e);
        }

        NcgridResourceData gridRscData = gridRsc.getResourceData();
        Map<String, String> attributes = mgr.getAttrSet(
                gridRscData.getResourceName()).getAttributes();
        String gdfileWithTimeCycles = ((NcEnsembleResourceData) gridRscData)
                .convertGdfileToCycleTimeString(gridRscData.getGdfile(),
                        gridRscData.getResourceName().getCycleTime());
        SaveGridInput defaultSaveInput = new SaveGridInput(
                gdfileWithTimeCycles, attributes);
        defaultSaveInput.setGdattim(CommonDateFormatUtil.dbtimeToDattim(gridRsc
                .getCurrentFrameTime().toString()));

        Object userSaveInput = null;

        Shell parentShell = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getShell();
        SaveGridDialog sgd = new SaveGridDialog(defaultSaveInput, parentShell,
                gridRscData, gridRsc.getCapabilities());
        userSaveInput = sgd.open();

        while (userSaveInput != null && userSaveInput instanceof SaveGridInput) {
            defaultSaveInput = (SaveGridInput) userSaveInput;
            if (defaultSaveInput.validInput()) {
                if (gridRsc.saveGridAs(defaultSaveInput)) {
                    userSaveInput = null;
                } else {
                    userSaveInput = sgd.open();
                }
            } else {
                userSaveInput = sgd.open();
            }
        }
    }

    /**
     * @see com.raytheon.viz.ui.cmenu.AbstractRightClickAction#setSelectedRsc(com
     *      .raytheon.uf.viz.core.rsc.AbstractVizResource)
     */
    @Override
    public void setSelectedRsc(ResourcePair selectedRsc) {
        super.setSelectedRsc(selectedRsc);
        AbstractVizResource<?, ?> rsc = getSelectedRsc();
        if (rsc instanceof NcgridResource) {
            gridRsc = (NcgridResource) rsc;
        }
    }

    /**
     * @see org.eclipse.jface.action.Action#getText()
     */
    @Override
    public String getText() {
        return "Save grid to database...";
    }

}
