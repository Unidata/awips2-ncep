package gov.noaa.nws.ncep.viz.ui.perspectives.menus;

import gov.noaa.nws.ncep.viz.common.display.NcDisplayType;
import gov.noaa.nws.ncep.viz.resources.manager.AbstractRBD;
import gov.noaa.nws.ncep.viz.resources.manager.SpfsManager;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;
import gov.noaa.nws.ncep.viz.ui.display.NcEditorUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;

import com.raytheon.uf.viz.core.exception.VizException;

/**
 * Create the Menu Items for the Blender menu
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer       Description
 * ------------ ----------  -----------    --------------------------
 * 03/01/2016   R6821       K. Bugenhagen  Created.
 * 
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1
 */
public class BlendMenu extends CompoundContributionItem {

    private final static String BLENDER_SPF_GROUP_TAG = "BLENDER";

    @Override
    protected IContributionItem[] getContributionItems() {
        IMenuManager blendMenuMngr = new MenuManager("Blend",
                BlendMenu.class.getName());

        NcDisplayType dispType = NcEditorUtil.getNcDisplayType(NcDisplayMngr
                .getActiveNatlCntrsEditor());
        if (dispType != NcDisplayType.NMAP_DISPLAY) {
            return new IContributionItem[0];
        }
        SpfsManager spfs = SpfsManager.getInstance();

        try {
            String[] spfNames = spfs.getSpfNamesForGroup(BLENDER_SPF_GROUP_TAG);
            for (String spfName : spfNames) {
                IMenuManager spfMenu = new MenuManager(spfName,
                        blendMenuMngr.getId() + "." + spfName);
                List<AbstractRBD<?>> resourceBundlesForSpf = spfs
                        .getRbdsFromSpf(BLENDER_SPF_GROUP_TAG, spfName, true);
                for (AbstractRBD<?> resourceBundle : resourceBundlesForSpf) {
                    spfMenu.add(createBlendMenuItem(spfName,
                            resourceBundle.getRbdName()));
                }
                blendMenuMngr.add(spfMenu);
            }
            return blendMenuMngr.getItems();
        } catch (VizException e) {
            return new IContributionItem[0];
        }
    }

    private CommandContributionItem createBlendMenuItem(String spfName,
            String bundleName) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("spfName", spfName);
        params.put("bundleName", bundleName);
        CommandContributionItemParameter param = new CommandContributionItemParameter(
                PlatformUI.getWorkbench(), null,
                "gov.noaa.nws.ncep.viz.ui.actions.loadBlend", params, null,
                null, null, bundleName, null, null,
                CommandContributionItem.STYLE_PUSH, null, true);

        return new CommandContributionItem(param);
    }
}
