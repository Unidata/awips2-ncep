package gov.noaa.nws.ncep.viz.ui.display;

import gov.noaa.nws.ncep.viz.common.display.IPowerLegend;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;

import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.BlendableCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.BlendedCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.EditableCapability;
import com.raytheon.viz.ui.editor.AbstractEditor;
import com.raytheon.viz.ui.input.EditableManager;

/**
 * This handler is responsible for picking up mouse clicks and key press events
 * on resources in the legend
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 02/03/2012              S. Gurung     Initial creation
 * 06/25/2012    827       Archana       Updated handleKeyUp() to
 *                                       toggle the display of the
 *                                       resources based on the
 *                                       UP/DOWN arrow key pressed.
 * 07/27/2012	695			B. Yin		 Added middle mouse click to toggle editable resource.
 * 08/09/2012   839       Archana        Updated to toggle the colorbar when 
 *                                       its corresponding resource is toggled on/off.
 * 10/19/2012   897         S. Gurung    Updated handleKeyUp() to not toggle PgenResource and added code to 
 * 										 refresh the editor after handling events.
 * 12/19/2012   960       G. Hull        use propertiesChanged() to toggle colorBar resources
 * 08/18/2014   ?         B. Yin         Handle GroupResource.
 * 11/06/2015   R9398     Edwin Brown    Corrected issue with hiding the color bar, broke up/ clarified,
 *                                       toggleVisibility, misc. clean up work
 * 11/25/2015   12830     B. Yin         Up/Down hot keys rotate resources in group
 * 05/04/2016   R17249    S. Russell     Updated handleMouseUp(), created the
 *                                       method getClickedNcLegendResource(),
 *                                       getResourcesClickedList(),
 *                                       isAnyVisibleLegendGroupExpanded(), and
 *                                       isAProperLegendResource()
 * </pre>
 * 
 * @author sgurung
 * @version 1.0
 */

public class NCLegendHandler extends AbstractNCLegendInputHandler {

    /**
     * @param resource
     */
    protected NCLegendHandler(NCLegendResource resource) {
        super(resource);
    }

    private ResourcePair mouseDownResourcePair = null;

    private static int currentResourceIndex = 0;

    private static int currentIndexInGroup = 0;

    private static ResourcePair currentGrp = null;

    private static ResourcePair lastClickedResourcePair = null;

    private boolean isShiftDown = false;

    private static boolean isFirstTime = true;

    private boolean doubleClick = false;

    private int doubleClickInterval = 300;

    private Job singleClickJob;

    protected final Object singleClickJobLock = new Object();

    private boolean isCtrlDown = false;

    private boolean ctrlDown = false;

    @Override
    public boolean handleMouseDown(int x, int y, int mouseButton) {

        if (mouseButton == 1 || mouseButton == 2) {
            mouseDownResourcePair = this.getClickedNcLegendResource(x, y);
        }
        return false;
    }

    @Override
    public boolean handleMouseUp(final int x, final int y, final int mouseButton) {

        // Because we wait certain milliseconds for double click,
        // we have to save the status of the ctrl key.
        if (this.isCtrlDown) {
            ctrlDown = true;
        } else {
            ctrlDown = false;
        }

        // Update this member variable since it is used as a flag
        mouseDownResourcePair = getClickedNcLegendResource(x, y);

        // Is an NcLegendResource still clicked/selected ?
        if (mouseDownResourcePair != null) {

            if (doubleClick) {
                synchronized (singleClickJobLock) {
                    if (singleClickJob != null) {
                        singleClickJob.cancel();
                    }
                }
                doubleClick = false;
                return doubleClickMouseUp(x, y, mouseButton);

            }

            singleClickJob = new Job("SingleClickMouseUp") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    VizApp.runSync(new Runnable() {
                        @Override
                        public void run() {
                            singleClickMouseUp(x, y, mouseButton);
                        }
                    });
                    synchronized (singleClickJobLock) {
                        singleClickJob = null;
                    }
                    return Status.OK_STATUS;
                }
            };
            singleClickJob.schedule(doubleClickInterval);

            // tell the InputManager class that the NCLegendHandler should
            // and will process this event
            return true;
        } else {
            return false;
        }

    }

    @Override
    public boolean handleDoubleClick(int x, int y, int mouseButton) {
        if (mouseDownResourcePair != null && mouseButton == 1) {
            doubleClick = true;
        }
        return true;
    }

    @Override
    public boolean handleMouseDownMove(int x, int y, int mouseButton) {
        return (mouseDownResourcePair != null);
    }

    public boolean handleMouseHover(int x, int y) {
        return false;
    }

    public boolean handleMouseMove(int x, int y) {
        return false;
    }

    public boolean handleMouseWheel(Event event, int x, int y) {
        return false;
    }

    public boolean handleMouseExit(Event event) {
        return false;
    }

    public boolean handleMouseEnter(Event event) {
        return false;
    }

    @Override
    public boolean handleKeyUp(int keyCode) {

        if (keyCode == SWT.CONTROL) {
            isCtrlDown = false;
            return true;
        }

        if (keyCode != SWT.SHIFT && keyCode != SWT.ARROW_UP
                && keyCode != SWT.ARROW_DOWN) {
            return false;
        }

        if (keyCode == SWT.SHIFT) {
            isShiftDown = true;
        }

        AbstractEditor editor = NcDisplayMngr.getActiveNatlCntrsEditor();
        ResourceList theMainList = editor.getActiveDisplayPane()
                .getDescriptor().getResourceList();

        List<ResourcePair> subListOfResourcesToToggle = new ArrayList<>(0);

        if (isShiftDown) {
            /*
             * Pressing the Shift key with either the up or the down arrow key
             * makes all the non-system/non map layer resources visible.
             */
            if ((keyCode == SWT.ARROW_UP || keyCode == SWT.ARROW_DOWN)) {
                for (ResourcePair resourcePair : theMainList) {
                    resourcePair.getProperties().setVisible(true);
                }
            }

        } else {

            /*
             * Create 2 sublists.One for the colorbar resources and one for the
             * requestable resources (non-system and non-map layer resources)
             * Set the visibility for all the resources in both lists to false.
             */
            boolean allVisible = true;
            boolean rotateInGroup = false;
            ResourcePair group = null;
            int rscIndex = 0;

            for (ResourcePair resourcePair : theMainList) {

                // If any visible group is expanded, set the rotateInGroup flag.
                if (isAnyVisibleLegendGroupExpanded(rotateInGroup, resourcePair)) {
                    rotateInGroup = true;
                    group = resourcePair;
                }

                // If NOT a system resource, PGen resource, or a Map Layer
                if (isAProperLegendResource(resourcePair)) {

                    subListOfResourcesToToggle.add(resourcePair);

                    allVisible = allVisible
                            && resourcePair.getProperties().isVisible();

                    resourcePair.getProperties().setVisible(false);

                    // Rotate from last clicked visible resource.
                    if (resourcePair == lastClickedResourcePair) {
                        currentResourceIndex = rscIndex;
                        lastClickedResourcePair = null;
                    }

                    rscIndex++;
                }
            }

            if (subListOfResourcesToToggle.isEmpty())
                return false;

            if (rotateInGroup) {
                if (group != currentGrp) {
                    isFirstTime = true;
                    currentGrp = group;
                } else {
                    isFirstTime = false;
                }

                lastClickedResourcePair = currentGrp;

                group.getProperties().setVisible(true);
                ((IPowerLegend) group.getResource()).setNameExpanded(true);
                ((IPowerLegend) group.getResource())
                        .setVisibleForAllResources(false);
                loopInGroup((IPowerLegend) group.getResource(), keyCode);
                editor.refresh();
                return true;
            }

            if (allVisible)
                isFirstTime = true;

            int listSize = subListOfResourcesToToggle.size();

            if (keyCode == SWT.ARROW_UP) {
                /*
                 * The navigation seems counter-intuitive. Yet this works since
                 * the elements displayed in the legend resource are listed from
                 * bottom-up
                 * 
                 * The very first time either the up arrow is pressed the
                 * currentRscIndex gets initialized to the first element in the
                 * listSubsequently, if the up arrow is pressed, the index is
                 * incremented.If it points beyond the index of the last
                 * resource,then it gets reset to the index of the first
                 * resource
                 */
                if (isFirstTime || isShiftDown)
                    currentResourceIndex = 0;
                else {
                    currentResourceIndex++;
                    if (currentResourceIndex > (listSize - 1))
                        currentResourceIndex = 0;
                }

            } else if (keyCode == SWT.ARROW_DOWN) {
                /*
                 * The very first time either the down arrow is pressed the
                 * currentRscIndex gets initialized to the index of the last
                 * resource in the listSubsequently, if the down arrow is
                 * pressed, the index is decremented.If it points beyond the
                 * index of the first resource,then it gets set to the index of
                 * the last resource
                 */

                if (isFirstTime || isShiftDown)
                    currentResourceIndex = listSize - 1;
                else {
                    currentResourceIndex--;
                    if (currentResourceIndex < 0)
                        currentResourceIndex = listSize - 1;
                }

            }

            /* Make the resource visible */
            ResourcePair resourceToSetVisible = subListOfResourcesToToggle
                    .get(currentResourceIndex);
            resourceToSetVisible.getProperties().setVisible(true);

            /*
             * Turn on all resources in a group
             */
            if (resourceToSetVisible.getResource() instanceof IPowerLegend) {
                ((IPowerLegend) resourceToSetVisible.getResource())
                        .setVisibleForAllResources(true);
            }

            // Some resources may have an associated colorBar resource. This
            // will be toggled when the resource's propertiesChanged() method is
            // called. This is triggered by setVisible();

            if (isFirstTime
                    && ((keyCode == SWT.ARROW_DOWN) || (keyCode == SWT.ARROW_UP)))
                isFirstTime = false;

        }

        editor.refresh();

        if (isShiftDown) {
            /*
             * If the shift key was used to make all the resources visible
             * again, the isFirstTime boolean is set to trueSo in effect the
             * currentRscIndex is reset to either the first or the last non
             * system/non map layer resource depending on which arrow key is
             * subsequently pressed.
             */
            isShiftDown = false;
            isFirstTime = true;
        }
        return false;
    }

    @Override
    public boolean handleKeyDown(int keyCode) {

        if (keyCode == SWT.SHIFT) {
            isShiftDown = true;
            return false;
        } else if (keyCode == SWT.CONTROL) {
            isCtrlDown = true;
        }
        return false;
    }

    private boolean isGroupedResource(ResourcePair resourcePair,
            ResourcePair resourcePairGroup) {

        if ((resourcePair != null) && (resourcePairGroup != null)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isBlendedResource(ResourcePair resourcePair) {
        AbstractVizResource<?, ?> resource = resourcePair.getResource();

        if ((resource != null)
                && (resource.hasCapability(BlendedCapability.class))) {
            return true;
        } else {
            return false;
        }

    }

    private boolean isPowerLegend(ResourcePair resourcePair) {
        if (resourcePair.getResource() instanceof IPowerLegend) {
            return true;
        } else {
            return false;
        }
    }

    private void toggleVisibility(ResourcePair resourcePair,
            ResourcePair resourcePairGroup) {
        /*-
         *  Determine what type of resource was clicked in legend and toggle it.
         *  resourcePair might be:
         *  - A single resource
         *  - A blended resource
         *  - A resource that's part of a resource group
         *  - A resource power legend (resource group)
         */

        if (isBlendedResource(resourcePair)) {
            // resourcePair is a blended resource
            toggleBlendedResource(resourcePair);
        } else {
            // If resourcePairGroup is not null, resourcePair is a resource in
            // that resource group
            if (isGroupedResource(resourcePair, resourcePairGroup)) {
                // resourcePair is a grouped resource
                toggleVisibilityForGroupedResource(resourcePair,
                        resourcePairGroup);
            } else {
                if (isPowerLegend(resourcePair)) {
                    // resourcePair is a power legend
                    toggleVisibilityForPowerLegend(resourcePair);
                } else {
                    // resourcePair is a single reosource
                    toggleVisibilityForSingleResource(resourcePair);
                }
            }
        }
    }

    private void toggleVisibilityForSingleResource(ResourcePair resourcePair) {
        // Toggles visibility of resourcePair
        resourcePair.getProperties().setVisible(
                !resourcePair.getProperties().isVisible());
    }

    private void toggleVisibilityForGroupedResource(ResourcePair resourcePair,
            ResourcePair resourcePairGroup) {
        // resourcePair is part of a resource group. Toggle the title for
        // resourcePair in the legend. If we are toggling to visible, turn it's
        // corresponding power legend to visible

        // Toggles resourcePair
        toggleVisibilityForSingleResource(resourcePair);

        if (resourcePairGroup != null
                && resourcePair.getProperties().isVisible()
                && (resourcePairGroup.getProperties().isVisible() == false)) {
            // If resourcePairGroup exists, and resourcePair was just set to
            // visible,
            // and resourcePairGroup is not visible Set visibility of power
            // legend for the grouped resource to true
            resourcePairGroup.getProperties().setVisible(true);
        }
    }

    private void toggleBlendedResource(ResourcePair resourcePair) {
        AbstractVizResource<?, ?> resource = resourcePair.getResource();

        ResourcePair parentResource = resource.getCapability(
                BlendedCapability.class).getBlendableResource();
        ResourceList childResources = parentResource.getResource()
                .getCapability(BlendableCapability.class).getResourceList();

        if (parentResource.getProperties().isVisible() == false) {
            parentResource.getProperties().setVisible(true);
            for (ResourcePair child : childResources) {
                child.getProperties().setVisible(true);
            }
        } else {
            // Topmost resource is visible, toggle us and other resource
            if (resourcePair.getProperties().isVisible() == false) {
                resourcePair.getProperties().setVisible(true);
                parentResource.getResource()
                        .getCapability(BlendableCapability.class)
                        .setAlphaStep(BlendableCapability.BLEND_MAX / 2);
            } else {
                parentResource.getResource()
                        .getCapability(BlendableCapability.class)
                        .toggle(resourcePair);
            }
        }
    }

    private void toggleVisibilityForPowerLegend(ResourcePair resourcePair) {
        // Toggle the visibility of the power legend and all of the children
        // resources in its resource group

        // Toggles resourcePair (which in this case is a power legend)
        toggleVisibilityForSingleResource(resourcePair);

        IPowerLegend groupResource = (IPowerLegend) resourcePair.getResource();
        // Even though this looks like it "sets Visible" for all
        // resources, it's kind of misleading. It actually sets the variable
        // "Visible" for all of the resources to what ever resourcePair's
        // visibility was set to above
        groupResource.setVisibleForAllResources(resourcePair.getProperties()
                .isVisible());

        // if CTRL is not down, disable all other groups
        if (!ctrlDown) {
            hideOtherResourceGroups(resourcePair);
        }
    }

    private void hideOtherResourceGroups(ResourcePair resourcePair) {
        // Disable all other resource groups other than the one that was
        // clicked. resourcePair passed in is the one that was clicked

        AbstractEditor editor = NcDisplayMngr.getActiveNatlCntrsEditor();

        if (editor != null && editor instanceof AbstractNcEditor) {
            // Get the display
            IRenderableDisplay display = editor.getActiveDisplayPane()
                    .getRenderableDisplay();

            // Iterate through all resources to find power legends
            for (ResourcePair pair : display.getDescriptor().getResourceList()) {

                // If resource is a power legend, disable it and all of its
                // children
                if (isPowerLegend(pair) && pair != resourcePair
                        && resourcePair.getProperties().isVisible()) {
                    // Disable power legend
                    pair.getProperties().setVisible(false);
                    // Disable its children
                    ((IPowerLegend) pair.getResource())
                            .setVisibleForAllResources(false);
                }
            }
        }

    }

    private boolean singleClickMouseUp(int x, int y, int mouseButton) {

        AbstractEditor editor = NcDisplayMngr.getActiveNatlCntrsEditor();

        if (mouseButton == 1) {

            ResourcePair resourcePair = null;
            ResourcePair resourcePairGroup = null;
            List<ResourcePair> resourcesClicked = null;

            resourcesClicked = getResourcesClickedList(x, y);

            if (resourcesClicked != null && !resourcesClicked.isEmpty()) {

                resourcePair = resourcesClicked
                        .get(resourcesClicked.size() - 1);

                if (resourcesClicked.size() > 1) {
                    resourcePairGroup = resourcesClicked.get(0);
                }
            }

            if (resourcePair != null && resourcePair == mouseDownResourcePair) {

                mouseDownResourcePair = null;

                /*
                 * Save the resource pair that is going to be turned on so we
                 * can use it as the starting point for hot keys.
                 */
                if (!resourcePair.getProperties().isVisible()) {
                    lastClickedResourcePair = resourcePair;
                }

                toggleVisibility(resourcePair, resourcePairGroup);
                editor.refresh();

                return true;
            }

        } else if (mouseButton == 2) {

            if (mouseDownResourcePair != null
                    && mouseDownResourcePair.getResource().hasCapability(
                            EditableCapability.class)) {
                // check / make editable
                EditableManager.makeEditable(
                        mouseDownResourcePair.getResource(),
                        !mouseDownResourcePair.getResource()
                                .getCapability(EditableCapability.class)
                                .isEditable());
                mouseDownResourcePair = null;

                editor.refresh();
                return true;
            }
        }

        return false;
    }

    private boolean doubleClickMouseUp(int x, int y, int mouseButton) {

        if (mouseDownResourcePair.getResource() instanceof IPowerLegend) {
            IPowerLegend groupResource = (IPowerLegend) mouseDownResourcePair
                    .getResource();
            if (mouseDownResourcePair.getResource().getProperties().isVisible()) {
                if (groupResource.isNameExpanded()) {
                    groupResource.setNameExpanded(false);
                } else {
                    groupResource.setNameExpanded(true);
                }
            }

            /*
             * Reset current resource index
             */
            currentIndexInGroup = 0;
            resource.issueRefresh();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Handles up/down hot keys when a group is expanded
     * 
     * @param groupRsc
     *            - the group resource that is expanded
     * @param keyCode
     *            - code of the pressed key
     */
    private void loopInGroup(IPowerLegend groupRsc, int keyCode) {

        ResourceList rscList = groupRsc.getResourceList();
        int listSize = rscList.size();

        if (keyCode == SWT.ARROW_UP) {
            if (isFirstTime)
                currentIndexInGroup = 0;
            else {
                currentIndexInGroup++;
                if (currentIndexInGroup > (listSize - 1))
                    currentIndexInGroup = 0;
            }

        } else if (keyCode == SWT.ARROW_DOWN) {
            if (isFirstTime)
                currentIndexInGroup = listSize - 1;
            else {
                currentIndexInGroup--;
                if (currentIndexInGroup < 0)
                    currentIndexInGroup = listSize - 1;
            }
        }

        rscList.get(currentIndexInGroup).getProperties().setVisible(true);

        if (isFirstTime
                && ((keyCode == SWT.ARROW_DOWN) || (keyCode == SWT.ARROW_UP))) {
            isFirstTime = false;
        }
    }

    private ResourcePair getClickedNcLegendResource(int x, int y) {

        ResourcePair rp = null;
        List<ResourcePair> resourcesClicked = null;

        resourcesClicked = this.getResourcesClickedList(x, y);

        if (resourcesClicked != null && !resourcesClicked.isEmpty()) {
            rp = resourcesClicked.get(resourcesClicked.size() - 1);
        } else {
            rp = null;
        }

        return rp;

    }

    private List<ResourcePair> getResourcesClickedList(int x, int y) {

        List<ResourcePair> resourcesClicked = null;
        AbstractEditor editor = NcDisplayMngr.getActiveNatlCntrsEditor();

        if (editor != null && editor instanceof AbstractNcEditor) {

            IDisplayPane activePane = editor.getActiveDisplayPane();

            IRenderableDisplay display = editor.getActiveDisplayPane()
                    .getRenderableDisplay();

            resourcesClicked = resource.getResourceClicked(
                    display.getDescriptor(), activePane.getTarget(), x, y);
        }

        return resourcesClicked;

    }

    private boolean isAnyVisibleLegendGroupExpanded(boolean rotateInGroup,
            ResourcePair resourcePair) {

        if (resourcePair == null) {
            return false;
        }

        boolean isGroupExpanded = false;
        AbstractVizResource<?, ?> resource = resourcePair.getResource();
        boolean isIPowerLegend = false;
        boolean isResourceListEmpty = false;
        boolean isResourceNameExpanded = false;
        boolean isVisible = false;

        // Is the resouce an IPowerLegend?
        if (resource instanceof IPowerLegend) {
            isIPowerLegend = true;
        }

        // Is the resource list empty?
        isResourceListEmpty = ((IPowerLegend) resource).getResourceList()
                .isEmpty();

        // Is the Group on the legend expanded?
        isResourceNameExpanded = ((IPowerLegend) resource).isNameExpanded();

        // Is the resource visible?
        isVisible = resourcePair.getProperties().isVisible();

        // Is any visible resource Group in the legend in Cave, expanded?
        if (!rotateInGroup && isIPowerLegend && !isResourceListEmpty
                && isResourceNameExpanded && isVisible) {
            isGroupExpanded = true;
        }

        return isGroupExpanded;
    }

    private boolean isAProperLegendResource(ResourcePair resourcePair) {

        if (resourcePair == null) {
            return false;
        }

        boolean isAProperLegendResource = false;
        ResourceProperties rprop = resourcePair.getProperties();
        boolean isASystemResource = rprop.isSystemResource();
        boolean isMapLayer = rprop.isMapLayer();
        String rscName = resourcePair.getResource().getClass().getSimpleName();

        // If NOT a System Resource NOR a PgenResource
        if (!isASystemResource && !isMapLayer && rscName.equals("PgenResource")) {
            isAProperLegendResource = true;
        }

        return isAProperLegendResource;
    }

}
