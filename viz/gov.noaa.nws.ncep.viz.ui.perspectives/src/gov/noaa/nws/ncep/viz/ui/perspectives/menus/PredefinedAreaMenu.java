package gov.noaa.nws.ncep.viz.ui.perspectives.menus;

import gov.noaa.nws.ncep.viz.common.area.AreaMenus.AreaMenuItem;
import gov.noaa.nws.ncep.viz.common.area.AreaMenusMngr;
import gov.noaa.nws.ncep.viz.common.area.IAreaProviderCapable;
import gov.noaa.nws.ncep.viz.common.display.NcDisplayType;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;
import gov.noaa.nws.ncep.viz.ui.display.NcEditorUtil;

import java.util.ArrayList;
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

import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.viz.ui.editor.AbstractEditor;

/**
 * Create the Menu Items for the Predefined Area menu
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 *  4/15/11                  G. Hull      created.
 * 07/28/11       450        G. Hull      Use PredefinedAreasMngr
 * 03/29/12                  B. Hebbard   refactor courtesy of Matt Nash (RTS)
 *                                        to extend CompoundContributionItem
 *                                        (instead of ContributionItem); fixes
 *                                        menu items disappearing (after first
 *                                        menu use) in OB12.4
 * 11/28/12       630        G. Hull      add areaType parameter to the command     
 * 04/17/13       #863       G. Hull      moved code from static Menus to be
 *                                        dynamic MenuManagers here                               
 * 05/15/13       #862       G. Hull      support areaSources from new AreaMenus file.
 * 09/23/2015     R9397      N. Jensen    Changed command style to radio
 * 
 * </pre>
 * 
 * @author ghull
 * @version 1
 */
public class PredefinedAreaMenu extends CompoundContributionItem {

    private static String commandId = "gov.noaa.nws.ncep.viz.ui.actions.loadPredefinedArea";

    @Override
    protected IContributionItem[] getContributionItems() {
        IMenuManager areaMenuMngr = new MenuManager("Areas",
                PredefinedAreaMenu.class.getName());

        AbstractEditor editor = NcDisplayMngr.getActiveNatlCntrsEditor();
        NcDisplayType displayType = NcEditorUtil.getNcDisplayType(editor);
        if (displayType != NcDisplayType.NMAP_DISPLAY) {
            return areaMenuMngr.getItems();
        }

        addResourceAreaMenuItems(areaMenuMngr);
        addOtherDisplayAreasMenuItems(areaMenuMngr);
        addAreaMenuItems(areaMenuMngr);

        return areaMenuMngr.getItems();
    }

    private void addAreaMenuItems(IMenuManager areaMenuMngr) {

        Map<String, List<AreaMenuItem>> areaMenuItems = AreaMenusMngr
                .getInstance().getPredefinedAreasForMenus();

        List<String> subMenus = new ArrayList<String>(areaMenuItems.keySet());

        for (String subMenuName : subMenus) {
            if (subMenuName.isEmpty()) {
                continue;
            }

            List<AreaMenuItem> menuItems = areaMenuItems.get(subMenuName);
            if (menuItems == null || menuItems.isEmpty()) {
                continue;
            }
            IMenuManager subMenu = new MenuManager(subMenuName,
                    areaMenuMngr.getId() + "." + subMenuName);
            areaMenuMngr.add(subMenu);

            addAreaMenuItems(subMenu, menuItems);
        }

        if (areaMenuItems.containsKey("")) {
            addAreaMenuItems(areaMenuMngr, areaMenuItems.get(""));
        }
    }

    private void addAreaMenuItems(IMenuManager menuMngr, String subMenuName,
            List<IAreaProviderCapable> areaProvList) {
        IMenuManager subMenu = new MenuManager(subMenuName, menuMngr.getId()
                + "." + subMenuName);
        menuMngr.add(subMenu);

        List<AreaMenuItem> menuItemList = new ArrayList<AreaMenuItem>();
        for (IAreaProviderCapable areaProvider : areaProvList) {
            menuItemList.add(new AreaMenuItem(areaProvider.getAreaName(), subMenuName, areaProvider
                    .getAreaName(), areaProvider.getSourceProvider().toString()));
        }

        addAreaMenuItems(subMenu, menuItemList);
    }

    private void addAreaMenuItems(IMenuManager subMenuMngr,
            List<AreaMenuItem> menuItems) {
        Map<String, String> cmdParams = new HashMap<String, String>();

        for (AreaMenuItem menuItem : menuItems) {
            cmdParams.put("areaName", menuItem.getAreaName());
            cmdParams.put("areaSource", menuItem.getSource());

            subMenuMngr.add(new CommandContributionItem(
                    new CommandContributionItemParameter(PlatformUI
                            .getWorkbench(), null, commandId, cmdParams, null,
                            null, null, menuItem.getMenuName(), null, null,
                            CommandContributionItem.STYLE_RADIO, null, true)));
        }
    }

    private void addResourceAreaMenuItems(IMenuManager areaMenuMngr) {
        if (!AreaMenusMngr.getInstance().showImageBasedResourceAreas()) {
            return;
        }

        AbstractEditor currEditor = NcDisplayMngr.getActiveNatlCntrsEditor();
        if (currEditor == null) {
            return;
        }

        /*
         * if geoSynced then let the user choose from all panes and otherwise
         * only from the currently selected panes.
         */
        IDisplayPane[] panes = (NcEditorUtil.arePanesGeoSynced(currEditor) ? currEditor
                .getDisplayPanes() : NcEditorUtil.getSelectedPanes(currEditor));

        List<IAreaProviderCapable> capableList = new ArrayList<IAreaProviderCapable>();

        for (IDisplayPane pane : panes) {
            ResourceList rscList = pane.getDescriptor().getResourceList();
            for (int rsc = 0; rsc < rscList.size(); rsc++) {
                if (rscList.get(rsc).getResourceData() instanceof IAreaProviderCapable) {
                    capableList.add((IAreaProviderCapable) rscList.get(rsc)
                            .getResourceData());
                }
            }
        } // end loop thru panes

        if (!capableList.isEmpty()) {
            addAreaMenuItems(areaMenuMngr, "From Resource", capableList);
        }
    }

    private void addOtherDisplayAreasMenuItems(IMenuManager areaMenuMngr) {
        if (!AreaMenusMngr.getInstance().showDisplayAreas()) {
            return;
        }

        AbstractEditor currEditor = NcDisplayMngr.getActiveNatlCntrsEditor();
        List<IAreaProviderCapable> capableList = new ArrayList<IAreaProviderCapable>();

        for (AbstractEditor editor : NcDisplayMngr.getAllNcDisplays()) {
            Boolean panesSynced = NcEditorUtil.arePanesGeoSynced(editor);
            IDisplayPane[] panes = (panesSynced ? NcEditorUtil
                    .getSelectedPanes(editor) : editor.getDisplayPanes());

            int numPaneMenus = panes.length;
            if (numPaneMenus == 1) {
                /*
                 * if this is the current editor then we only need to show an
                 * area option if there are multiple, non-geosynced panes.
                 */
                if (editor != currEditor
                        && panes[0].getRenderableDisplay() instanceof IAreaProviderCapable) {
                    capableList.add((IAreaProviderCapable) panes[0]
                            .getRenderableDisplay());
                }
            } else {
                for (IDisplayPane pane : panes) {
                    if (pane.getRenderableDisplay() instanceof IAreaProviderCapable) {
                        capableList.add((IAreaProviderCapable) pane
                                .getRenderableDisplay());
                    }
                }
            }
        }

        if (!capableList.isEmpty()) {
            addAreaMenuItems(areaMenuMngr, "From Display", capableList);
        }
    }
}
