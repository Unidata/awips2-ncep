package gov.noaa.nws.ncep.viz.resources.manager;

import gov.noaa.nws.ncep.viz.common.area.AreaMenus.AreaMenuItem;
import gov.noaa.nws.ncep.viz.common.area.IAreaProviderCapable;
import gov.noaa.nws.ncep.viz.common.area.PredefinedArea;
import gov.noaa.nws.ncep.viz.common.area.PredefinedAreaFactory;
import gov.noaa.nws.ncep.viz.resources.groupresource.GroupResourceData;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceFactory.ResourceSelection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.exception.VizException;

/**
 * Tree based structure to hold AreaMenuItems for a nested menu for IMenuManager
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 6/22/2016    R8878       J. Lopez    Initial Creation
 * </pre>
 * 
 * @author Josh Lopez
 * @version 1
 */
public class AreaMenuTree {

    // Parent node
    private AreaMenuTree menu;

    // Can have multiple child nodes
    private List<AreaMenuTree> submenu;

    // Stores the AreaMenuItem
    private AreaMenuItem areaItem;

    // Name of the menu if it is not a child node
    private String menuName;

    // Name of the predefined area if it is not a parent node
    private String areaName;

    // Regular expression for empty line
    private static final String EMPTYLINEREGEX = "^[ \n]*$";

    // Status handler for errors and debugging
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(AreaMenuTree.class);

    public AreaMenuTree() {
        submenu = new ArrayList<AreaMenuTree>();
    }

    public AreaMenuTree(String menuName) {
        submenu = new ArrayList<AreaMenuTree>();
        this.menuName = menuName;
    }

    public AreaMenuTree(AreaMenuItem areaItem) {
        this.areaItem = areaItem;
        areaName = areaItem.getAreaName();
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String name) {
        menuName = name;
    }

    public List<AreaMenuTree> getSubMenu() {
        return submenu;
    }

    /**
     * Adds a subMenu to a menu
     * 
     * @param menu
     * @param subMenu
     */
    public void addSubMenu(AreaMenuTree subMenu) {
        if (subMenu != null) {
            this.submenu.add(subMenu);
            subMenu.menu = this.menu;
        }
    }

    /**
     * Adds a sub-menu and populates it from List<AreaMenuItem>
     * 
     * @param areaMenuItems
     */
    public void addSubMenu(List<AreaMenuItem> areaMenuItems) {
        if (areaMenuItems.size() > 0) {
            AreaMenuTree subMenu = new AreaMenuTree(areaMenuItems.get(0)
                    .getSubMenuName());
            for (AreaMenuItem item : areaMenuItems) {
                subMenu.addAreaMenuItem(item);
            }
            submenu.add(subMenu);
        }
    }

    /**
     * Adds a sub-menu and populates it from Map<String, List<AreaMenuItem>>
     * 
     * @param areaMenusMap
     */
    public void addSubMenu(Map<String, List<AreaMenuItem>> areaMenusMap) {

        for (List<AreaMenuItem> areaList : areaMenusMap.values()) {
            if (areaList == null || areaList.isEmpty()
                    || areaList.get(0).getSubMenuName().isEmpty()) {
                continue;
            }
            addSubMenu(areaList);
        }
    }

    /**
     * Adds a sub-menu and populates it from ResourceSelection[]
     * 
     * @param selectedResources
     * @param subMenuName
     */
    public void addSubMenu(ResourceSelection[] selectedResources,
            String subMenuName) {

        AreaMenuTree subMenu = new AreaMenuTree(subMenuName);
        AreaMenuTree groupMenu = null;
        for (ResourceSelection resources : selectedResources) {

            // If there's a group resource present, iterate through each group
            // resources and add each resource in the group to the resourceList
            if (resources.getResourceData() instanceof GroupResourceData) {

                String groupName = ((GroupResourceData) resources
                        .getResourceData()).getGroupName();

                groupMenu = new AreaMenuTree(groupName);
                GroupResourceData groupResourceData = (GroupResourceData) resources
                        .getResourceData();
                int lengthofGroup = groupResourceData.getResourceList().size();

                if (groupResourceData.getGroupName().equals(groupName)) {

                    // For each ResourceSelection in a Group
                    // add it the resourceList
                    for (int i = 0; i < lengthofGroup; i++) {
                        try {
                            ResourceSelection resource = ResourceFactory
                                    .createResource(groupResourceData
                                            .getResourceList().get(
                                                    lengthofGroup - i - 1));

                            groupMenu.addAreaMenuItem(areaMenuItemCreator(
                                    resource, groupName));

                        } catch (VizException e) {

                            statusHandler.handle(Priority.PROBLEM,
                                    "Unable to get resources in" + groupName
                                            + " Group");
                        }
                    }

                }
                this.addSubMenu(groupMenu);

            } else {

                subMenu.addAreaMenuItem(areaMenuItemCreator(resources,
                        subMenuName));

            }

        }
        if (subMenu.submenu.size() != 0) {
            this.addSubMenu(subMenu);
        }
    }

    public boolean hasSubMenu() {
        return submenu != null;
    }

    public AreaMenuItem getAreaMenuItem() {
        return areaItem;
    }

    /**
     * Adds a AreaMenuItem to a menu
     * 
     * @param AreaMenuItem
     */
    public void addAreaMenuItem(AreaMenuItem areaItem) {
        if (areaItem != null) {
            AreaMenuTree areaMenuItem = new AreaMenuTree(areaItem);
            areaMenuItem.menu = this;
            submenu.add(areaMenuItem);
        }
    }

    /**
     * Creates AreaMenuItems from an array of ResourceSelection and add them to
     * a menu
     * 
     * @param selectionedResources
     * @param subMenuName
     */
    public void addAreaMenuItems(ResourceSelection[] selectionedResources,
            String subMenuName) {

        for (ResourceSelection resource : selectionedResources) {

            this.addAreaMenuItem(areaMenuItemCreator(resource, subMenuName));

        }

    }

    /**
     * Creates a AreaMenu sub-menu. Returns null if the resource has no area or
     * the area does not exist
     * 
     * @param ResourceSelection
     *            array
     * @param groupName
     *            name of sub-menu
     * @return returns null if the resource's Predefined Area is not valid or is
     *         blank
     */
    public static AreaMenuItem areaMenuItemCreator(ResourceSelection resource,
            String groupName) {

        ResourceDefinition resourceDefinition = null;
        AreaMenuItem menuItem = null;
        try {
            ResourceDefnsMngr resourceManager = ResourceDefnsMngr.getInstance();

            // Get resource definition of the selected resource
            resourceDefinition = resourceManager.getResourceDefinition(resource
                    .getResourceData().getResourceName());

            // if the area is not empty

            if (resourceDefinition != null
                    && !resourceDefinition.getDfltGeogArea().matches(
                            EMPTYLINEREGEX)) {

                if (resource.getResourceData() instanceof IAreaProviderCapable) {

                    IAreaProviderCapable areaRsc = (IAreaProviderCapable) resource
                            .getResourceData();
                    menuItem = new AreaMenuItem(resource.getResourceName()
                            + " (" + areaRsc.getAreaName() + ")", "Resource",
                            areaRsc.getAreaName(), areaRsc.getSourceProvider()
                                    .toString());

                } else {

                    // Get the PredefinedArea from Resource definition
                    PredefinedArea resourceArea = PredefinedAreaFactory
                            .getPredefinedArea(resourceDefinition
                                    .getDfltGeogArea());

                    // Create the menu item
                    menuItem = new AreaMenuItem(resource.getResourceName()
                            + " (" + resourceArea.getAreaName() + ")",
                            groupName, resourceArea.getAreaName(),
                            resourceArea.getAreaSource());
                }
            }

        } catch (VizException e) {
            if (resourceDefinition == null) {
                statusHandler
                        .handle(Priority.PROBLEM,
                                "There is an error getting resource definition manager instance");

            } else {
                statusHandler.handle(Priority.PROBLEM,
                        "There is an error getting the area for "
                                + resourceDefinition.getResourceDefnName()
                                + " Please check the Resource Definition file");
            }
            // Returns null if the Predefined area was not found
            return null;

        }

        return menuItem;
    }

    public String toString() {
        if (menu.menuName != null) {
            return "Menu: " + menu.menuName + ", AreaMenuItem: " + areaName;
        }
        return "Menu: root AreaMenuItem: " + areaName;
    }

    /**
     * Converts the tree to a List<AreaMenuItem>. Pass in an new/empty
     * List<AreaMenuItem>
     * 
     * @param areaMenuItems
     * @return
     */
    public List<AreaMenuItem> toList(List<AreaMenuItem> areaMenuItems) {

        for (AreaMenuTree subMenu : this.submenu) {

            if (!subMenu.hasSubMenu()) {
                areaMenuItems.add(subMenu.areaItem);
            }
            if (subMenu.hasSubMenu()) {
                subMenu.toList(areaMenuItems);
            }

        }

        return areaMenuItems;
    }

    public void print() {

        for (AreaMenuTree menu : this.submenu) {

            if (!menu.hasSubMenu()) {
                statusHandler.debug(menu.toString());
            }

            if (menu.hasSubMenu()) {
                statusHandler.debug("Menu: " + menu.menuName
                        + ", Number of children: " + menu.submenu.size());
                menu.print();

            }

        }

    }

    public void addAreaMenuItems(List<AreaMenuItem> areaMenuItems) {
        for (AreaMenuItem areaMenu : areaMenuItems) {
            addAreaMenuItem(areaMenu);
        }

    }

}
