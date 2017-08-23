package gov.noaa.nws.ncep.viz.overlays;

import gov.noaa.nws.ncep.viz.resources.manager.AbstractRBD;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceBndlLoader;
import gov.noaa.nws.ncep.viz.resources.manager.SpfsManager;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;
import gov.noaa.nws.ncep.viz.ui.display.NcEditorUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.ui.progress.UIJob;

import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.IMapDescriptor;
import com.raytheon.uf.viz.core.maps.MapManager;
import com.raytheon.viz.ui.UiPlugin;
import com.raytheon.viz.ui.editor.AbstractEditor;

/**
 * 
 * Blend Menu Handler
 * 
 * Loads blend SPFs from the Blend menu
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer     Description
 * ------------ ---------- -----------  --------------------------
 * 06/23/15     R6007      kbugenhagen  Created
 * 
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1.0
 */
public class BlendMenuAction extends AbstractHandler implements IElementUpdater {

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands
     * .ExecutionEvent)
     */
    @Override
    public Object execute(final ExecutionEvent arg0) throws ExecutionException {

        if (arg0.getCommand() == null) {
            return null;
        }

        UIJob uiJ = new UIJob("Loading Blends...") {

            @SuppressWarnings("unchecked")
            @Override
            public IStatus runInUIThread(IProgressMonitor monitor) {
                long elapseTime = -System.nanoTime();
                String spfName = arg0.getParameter("spfName");
                String bundleName = arg0.getParameter("bundleName");

                if (spfName == null || bundleName == null) {
                    return new Status(IStatus.ERROR, UiPlugin.PLUGIN_ID,
                            "spfName was " + spfName + " and bundleName was "
                                    + bundleName);
                }

                try {
                    ResourceBndlLoader rbdLoader = new ResourceBndlLoader(
                            "RBD Previewer");
                    List<AbstractRBD<?>> rbds = SpfsManager.getInstance()
                            .getRbdsFromSpf("BLENDER", spfName, bundleName,
                                    true);

                    for (AbstractRBD<?> rbdBndl : rbds) {
                        rbdBndl = AbstractRBD.clone(rbdBndl);
                        rbdBndl.initTimeline();
                        String rbdName = rbdBndl.getRbdName();

                        AbstractEditor editor = NcDisplayMngr
                                .createNatlCntrsEditor(
                                        rbdBndl.getDisplayType(), rbdName,
                                        rbdBndl.getPaneLayout());

                        if (editor == null) {
                            throw new VizException(
                                    "Unable to find or create an Editor for RDB: "
                                            + rbdBndl);
                        }
                        NcEditorUtil.setGeoSyncPanesEnabled(editor,
                                rbdBndl.isGeoSyncedPanes());

                        rbdLoader.addRBD(rbdBndl, editor);
                    }
                    VizApp.runSync(rbdLoader);
                } catch (VizException e) {
                    return new Status(IStatus.ERROR, UiPlugin.PLUGIN_ID,
                            "Error Loading SPF: " + spfName + " RBD: "
                                    + bundleName, e);
                }
                elapseTime += System.nanoTime();

                System.out.println("Bundle load time in millis: "
                        + TimeUnit.NANOSECONDS.toMillis(elapseTime));
                return Status.OK_STATUS;
            }
        };
        uiJ.schedule();

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.commands.IElementUpdater#updateElement(org.eclipse.ui.
     * menus.UIElement, java.util.Map)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void updateElement(UIElement element, Map parameters) {
        AbstractEditor editor = NcDisplayMngr.getActiveNatlCntrsEditor();
        if (editor == null) {
            return;
        }

        IDescriptor descriptor = editor.getActiveDisplayPane().getDescriptor();

        if (descriptor instanceof IMapDescriptor) {
            element.setChecked(MapManager.getInstance(
                    (IMapDescriptor) descriptor).isMapLoaded(
                    (String) parameters.get("mapName")));
        }
    }
}