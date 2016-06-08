package gov.noaa.nws.ncep.viz.tools.plotModelMngr;

import gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels.elements.PlotModel;
import gov.noaa.nws.ncep.viz.rsc.plotdata.rsc.EditPlotModelComposite;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * Provide utility methods to manage the list of actively edited Edit Plot Model
 * dialogs, including
 * 
 * <pre>
 *   (1) testing if a plot model is in the list, 
 *   (2) adding a plot model and the shell (window) of its Edit Plot Model dialog to the list, 
 *   (3) removing a plot model from the list, 
 *   (4) clearing the list, 
 *   (5) bringing the shell (window) of Edit Plot Model dialog for a plot mode to the front, and 
 *   (6) constructing the string of localization info.
 * </pre>
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date          Ticket#    Engineer      Description
 * ------------ ---------- ----------- --------------------------
 * 03/29/2016    R7567      A. Su         Initial creation.
 * 
 * </pre>
 * 
 * @author asu
 * @version 1.0
 * 
 */

public final class EditPlotModelDialogManager {

    /**
     * Alert Viz handler.
     */
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(EditPlotModelDialogManager.class);

    /**
     * Follow the Singleton design pattern.
     */
    private static EditPlotModelDialogManager instance = null;

    /**
     * A mapping from Plot Model to the shell (window) of its Edit Plot Model
     * dialog.
     */
    private static Map<String, Shell> activelyEditedPlotModels = new HashMap<String, Shell>();

    /**
     * The delimiter used to concatenate plugin name and plot model name.
     */
    private static final String DELIMITER = "%";

    /**
     * Follow the Singleton design pattern.
     */
    private EditPlotModelDialogManager() {

    }

    /**
     * Get an instance of this manager class.
     * 
     * @return an instance of EditPlotModelManager.
     */
    synchronized static EditPlotModelDialogManager getInstance() {
        if (instance == null) {
            instance = new EditPlotModelDialogManager();
        }
        return instance;
    }

    /**
     * Determine if a plot model is actively edited.
     * 
     * @param pluginName
     *            plugin name
     * @param modelName
     *            plot model name
     * @return true or false
     */
    synchronized public boolean isPlotModelActivelyEdited(String pluginName,
            String modelName) {

        if (pluginName == null || pluginName.isEmpty() || modelName == null
                || modelName.isEmpty()) {
            statusHandler
                    .handle(Priority.PROBLEM,
                            "invalid plugin or plot model name(s) <"
                                    + pluginName
                                    + ", "
                                    + modelName
                                    + "> in isPlotModelActivelyEdited() of the class EditPlotModelDialog.");
            return false;
        }

        boolean result = false;
        String pluginModelPair = pluginName + DELIMITER + modelName;
        Shell shell = activelyEditedPlotModels.get(pluginModelPair);

        if (shell != null) {
            if (shell.isDisposed()) {
                activelyEditedPlotModels.remove(pluginModelPair);
            } else {
                result = true;
            }
        }
        return result;
    }

    /**
     * Register a plot model and the shell of its associated Edit Plot Model
     * dialog with the list of actively edited plot models.
     * 
     * @param pluginName
     *            plugin name
     * @param modelName
     *            plot model name
     * @param shell
     *            its associated shell (Edit Plot Model dialog)
     * @return true if added to the list, otherwise, false
     */
    synchronized public boolean addToActivelyEditedPlotModelList(
            String pluginName, String modelName, Shell shell) {

        if (pluginName == null || pluginName.isEmpty() || modelName == null
                || modelName.isEmpty() || shell == null || shell.isDisposed()) {
            statusHandler
                    .handle(Priority.PROBLEM,
                            "invalid plugin or plot model name(s) <"
                                    + pluginName
                                    + ", "
                                    + modelName
                                    + ">, OR null or disposed shell in addToActivelyEditedPlotModelList() of the class EditPlotModelDialog.");
            return false;
        }

        String pluginModelPair = pluginName + DELIMITER + modelName;
        activelyEditedPlotModels.put(pluginModelPair, shell);
        return true;
    }

    /**
     * Remove a plot model from the list of actively edited plot models.
     * 
     * @param pluginName
     *            plugin name
     * @param modelName
     *            plot model name
     * @return true if removed from the list, otherwise, false
     */
    synchronized public boolean removeFromActivelyEditedPlotModelList(
            String pluginName, String modelName) {

        if (pluginName == null || pluginName.isEmpty() || modelName == null
                || modelName.isEmpty()) {
            statusHandler
                    .handle(Priority.PROBLEM,
                            "invalid plugin or plot model name(s) <"
                                    + pluginName
                                    + ", "
                                    + modelName
                                    + "> in removeFromActivelyEditedPlotModelList() of the class EditPlotModelDialog.");
            return false;
        }

        String pluginModelPair = pluginName + DELIMITER + modelName;
        activelyEditedPlotModels.remove(pluginModelPair);
        return true;
    }

    /**
     * Clear the list of actively edited plot models.
     */
    synchronized public void clearEditedList() {
        for (Shell child : activelyEditedPlotModels.values()) {
            if (child != null && !child.isDisposed()) {
                child.dispose();
            }
        }

        activelyEditedPlotModels = new HashMap<String, Shell>();
    }

    /**
     * Bring the shell (window) of Edit Plot Model dialog to the front.
     * 
     * @param pluginName
     *            plugin name
     * @param modelName
     *            plot model name
     * @return true if brought to the front, otherwise, false
     */
    synchronized public boolean bringShellToFront(String pluginName, String modelName) {

        if (pluginName == null || pluginName.isEmpty() || modelName == null
                || modelName.isEmpty()) {
            statusHandler
                    .handle(Priority.PROBLEM,
                            "invalid plugin or plot model name(s) <"
                                    + pluginName
                                    + ", "
                                    + modelName
                                    + "> in bringToFront() of the class EditPlotModelDialog.");
            return false;
        }

        String pluginModelPair = pluginName + DELIMITER + modelName;
        Shell shell = activelyEditedPlotModels.get(pluginModelPair);

        if (shell != null && !shell.isDisposed()) {
            shell.setActive();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Clear the position offset count of Advanced Options dialog.
     */
    public void clearAdvancedOptionsDialogPositionOffsetCount() {

        EditPlotModelComposite.clearAdvancedOptionsDialogPositionOffsetCount();
    }

    /**
     * Construct the string of localization info.
     * 
     * @param model
     *            the input plot model
     * @return localization info
     */
    public String getLocalizationString(PlotModel model) {
        if (model == null) {
            return null;
        }

        String localizationString = null;
        LocalizationContext context = model.getLocalizationFile().getContext();
        LocalizationLevel level = context.getLocalizationLevel();

        if (level == LocalizationLevel.BASE) {
            localizationString = " (" + level.name() + ")";
        } else {
            localizationString = " (" + level.name() + "="
                    + context.getContextName() + ")";
        }
        return localizationString;
    }
}
