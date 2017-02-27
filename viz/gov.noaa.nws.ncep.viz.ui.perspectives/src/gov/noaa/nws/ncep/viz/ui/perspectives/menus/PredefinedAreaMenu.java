package gov.noaa.nws.ncep.viz.ui.perspectives.menus;

import gov.noaa.nws.ncep.viz.common.area.AreaMenus.AreaMenuItem;
import gov.noaa.nws.ncep.viz.common.area.AreaMenusMngr;
import gov.noaa.nws.ncep.viz.common.area.IAreaProviderCapable;
import gov.noaa.nws.ncep.viz.common.display.NcDisplayType;
import gov.noaa.nws.ncep.viz.resources.groupresource.GroupResourceData;
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
import com.raytheon.uf.viz.core.rsc.AbstractResourceData;
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
 * 09/29/2016     R17223     J Beck       Add functionality: when resources are in a user defined group, 
 *                                        show them in the Area menu under "From Resource".
 * 
 * </pre>
 * 
 * @author ghull
 * @version 1
 */
public class PredefinedAreaMenu extends CompoundContributionItem {

    private static final String COMMAND_ID = "gov.noaa.nws.ncep.viz.ui.actions.loadPredefinedArea";

    private static final String AREA_MENU_ITEM_FROM_RESOURCE = "From Resource";

    private static final String AREA_MENU_ITEM_FROM_DISPLAY = "From Display";

    private static final String AREA_MENU_ITEM_AREAS = "Areas";

    private static final String AREA_MENU_ITEM_AREA_NAME = "areaName";

    private static final String AREA_MENU_ITEM_AREA_SOURCE = "areaSource";

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.actions.CompoundContributionItem#getContributionItems()
     */
    @Override
    protected IContributionItem[] getContributionItems() {
        IMenuManager areaMenuMngr = new MenuManager(AREA_MENU_ITEM_AREAS,
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

        List<String> subMenus = new ArrayList<>(areaMenuItems.keySet());

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

        List<AreaMenuItem> menuItemList = new ArrayList<>();
        for (IAreaProviderCapable areaProvider : areaProvList) {
            menuItemList.add(new AreaMenuItem(areaProvider.getAreaName(),
                    subMenuName, areaProvider.getAreaName(), areaProvider
                            .getSourceProvider().toString()));
        }

        addAreaMenuItems(subMenu, menuItemList);
    }

    private void addAreaMenuItems(IMenuManager subMenuMngr,
            List<AreaMenuItem> menuItems) {

        Map<String, String> cmdParams = new HashMap<>();

        for (AreaMenuItem menuItem : menuItems) {

            cmdParams.put(AREA_MENU_ITEM_AREA_NAME, menuItem.getAreaName());
            cmdParams.put(AREA_MENU_ITEM_AREA_SOURCE, menuItem.getSource());

            subMenuMngr.add(new CommandContributionItem(
                    new CommandContributionItemParameter(PlatformUI
                            .getWorkbench(), null, COMMAND_ID, cmdParams, null,
                            null, null, menuItem.getMenuName(), null, null,
                            CommandContributionItem.STYLE_RADIO, null, true)));
        }
    }

    /**
     * Add items to the Area menu. Items are resources found in user defined and
     * static groups.
     * 
     * Adds a menu item: "From Resource" to the Area menu. Resources are shown
     * to the right of the "From Resource" menu item. The criteria for resources
     * to be added is that they implement IAreaProviderCapable. Satellite
     * resources are a prominent example. If the resource is a group, it has to
     * be iterated over to get to the resources.
     * 
     * @param areaMenuMngr
     *            an instance of IMenuManager
     */
    private void addResourceAreaMenuItems(IMenuManager areaMenuMngr) {

        AbstractResourceData resourceData = null;
        AbstractResourceData data = null;
        ResourceList resourceList = null;
        IDisplayPane[] panes = null;
        List<IAreaProviderCapable> areaCapableResourceList = new ArrayList<>();

        if (!AreaMenusMngr.getInstance().showImageBasedResourceAreas()) {
            return;
        }

        AbstractEditor currEditor = NcDisplayMngr.getActiveNatlCntrsEditor();

        if (currEditor == null) {
            return;
        }

        if ((NcEditorUtil.arePanesGeoSynced(currEditor))) {
            panes = currEditor.getDisplayPanes();

        } else {
            panes = NcEditorUtil.getSelectedPanes(currEditor);
        }

        if (panes == null) {
            return;
        }

        // Build a list of qualifying resources, to add to the Area menu
        for (IDisplayPane pane : panes) {

            resourceList = pane.getDescriptor().getResourceList();

            if (resourceList == null) {
                // no resources to add for THIS pane
                continue;
            }

            for (int i = 0; i < resourceList.size(); i++) {

                resourceData = resourceList.get(i).getResourceData();

                // Add qualifying resources from the static group
                if (resourceData instanceof IAreaProviderCapable) {

                    areaCapableResourceList
                            .add((IAreaProviderCapable) resourceData);

                } else if (resourceData instanceof GroupResourceData) {

                    // Add qualifying resources from user defined group
                    ResourceList groupResourceList = ((GroupResourceData) resourceData)
                            .getResourceList();

                    for (int j = 0; j < groupResourceList.size(); j++) {

                        data = groupResourceList.get(j).getResourceData();

                        if (data instanceof IAreaProviderCapable) {

                            areaCapableResourceList
                                    .add((IAreaProviderCapable) data);
                        }
                    }
                }
            }
        }

        // Adds "From Resource" menu item to the Area menu, and adds resources
        // as sub-menu items of "From Resource"
        if (!areaCapableResourceList.isEmpty()) {

            addAreaMenuItems(areaMenuMngr, AREA_MENU_ITEM_FROM_RESOURCE,
                    areaCapableResourceList);
        }
    }

    private void addOtherDisplayAreasMenuItems(IMenuManager areaMenuMngr) {
        if (!AreaMenusMngr.getInstance().showDisplayAreas()) {
            return;
        }

        AbstractEditor currEditor = NcDisplayMngr.getActiveNatlCntrsEditor();
        List<IAreaProviderCapable> capableList = new ArrayList<>();

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
            addAreaMenuItems(areaMenuMngr, AREA_MENU_ITEM_FROM_DISPLAY,
                    capableList);
        }
    }
}
