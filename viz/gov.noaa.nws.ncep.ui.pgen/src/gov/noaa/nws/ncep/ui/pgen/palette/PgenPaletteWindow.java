/*
 * gov.noaa.nws.ncep.ui.pgen.palette.PgenPaletteWindow
 * 
 * 25 November 2008
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.ui.pgen.palette;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.IParameter;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.ViewPart;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.maps.display.VizMapEditor;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.viz.ui.EditorUtil;
import com.raytheon.viz.ui.UiUtil;
import com.raytheon.viz.ui.VizWorkbenchManager;
import com.raytheon.viz.ui.editor.AbstractEditor;
import com.raytheon.viz.ui.editor.ISelectedPanesChangedListener;
import com.raytheon.viz.ui.perspectives.AbstractVizPerspectiveManager;
import com.raytheon.viz.ui.perspectives.VizPerspectiveListener;
import com.raytheon.viz.ui.tools.AbstractModalTool;

import gov.noaa.nws.ncep.ui.pgen.Activator;
import gov.noaa.nws.ncep.ui.pgen.PgenConstant;
import gov.noaa.nws.ncep.ui.pgen.PgenPreferences;
import gov.noaa.nws.ncep.ui.pgen.PgenSession;
import gov.noaa.nws.ncep.ui.pgen.PgenUtil;
import gov.noaa.nws.ncep.ui.pgen.PgenUtil.PgenMode;
import gov.noaa.nws.ncep.ui.pgen.controls.CommandStackListener;
import gov.noaa.nws.ncep.ui.pgen.controls.StoreActivityDialog;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableElement;
import gov.noaa.nws.ncep.ui.pgen.elements.Layer;
import gov.noaa.nws.ncep.ui.pgen.elements.Product;
import gov.noaa.nws.ncep.ui.pgen.filter.CategoryFilter;
import gov.noaa.nws.ncep.ui.pgen.gfa.PreloadGfaDataThread;
import gov.noaa.nws.ncep.ui.pgen.productmanage.ProductDialogStarter;
import gov.noaa.nws.ncep.ui.pgen.rsc.PgenResource;
import gov.noaa.nws.ncep.ui.pgen.rsc.PgenResourceData;
import gov.noaa.nws.ncep.ui.pgen.tools.PgenCycleTool;
import gov.noaa.nws.ncep.ui.pgen.tools.PgenSelectingTool;
import gov.noaa.nws.ncep.viz.common.display.NcDisplayName;
import gov.noaa.nws.ncep.viz.common.ui.NmapCommon;

/**
 * The PGEN View is used for all interaction with the objects in the PGEN
 * Resource, and it represents a current interactive "session". It is
 * responsible for managing many listeners as well as loading/unloading the
 * appropriate modal tools required to create and modify PGEN drawable objects
 * in the resource.
 * 
 * The Display of the View consists of many buttons representing a drawing
 * Palette. They allow users to pick specific objects, modify their attributes,
 * and create various products based on the geographic objects created.
 * 
 * PGEN can be run in one of two modes. SINGLE mode mimics the behavior in
 * legacy NAWIPS application NMAP, where a single PGEN ResourceData is displayed
 * on every editor. Any change made to the data objects in any one map editor
 * are reflected in all the others as well. Optionally, PGEN can be run in
 * MULTIPLE mode which allows any map editor to contain its own unique instance
 * of a PGEN resource.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ -----------------------------------------------------------------
 * 01/10        ?           S. Gilbert  Initial Creation.
 * 08/13        TTR696/774  J. Wu       Reset title/Close product manage dialog.
 * 11/13        #1081       B. Yin      Get selected DE to change front/line type.
 * 04/15        R7805       J. Wu       Highlight only one PGEN action mode at a time.
 * 06/15        R8354       J. Wu       Deactivate Pgen Context when palette is closed, 
 *                                      hidden, or deactivated.
 * 06/15        R8199       S. Russell  Updated createPaletteSection() to suppress
 *                                      unwanted button creation. Converted literals
 *                                      from the legacy into constants there.
 * 09/04/2015   RM 11495    S. Russell  Update a loop in createPaletteSection()
 *                                      to fix a merge conflict
 * 
 * 11/09/2015   R9399       J. Lopez    Added the ability to specify the number
 *                                      of buttons per row
 * 12/21/2015   R12964      J. Lopez    Layers remember the last selected class
 * 05/10/2016   R13560      S. Russell  Updated class declaration to implement
 *                                      ISaveablePart2.  Added the Interface
 *                                      methods for ISaveablePart and 
 *                                      ISaveablePart2 *
 * 05/16/2016   R18388      J. Wu       Show all classes for MULTI-SELECT.
 * 05/17/2016   5641        njensen     Don't activate context outside of NCP 
 * 06/02/2016   R19326      S. Russell  updated method isDirty()
 * 06/13/2016   5640        bsteffen    Delay opening of remind dialog during close.
 * 06/15/2016   R19326      S. Russell  updated method isDirty()
 * 06/29/2016   R18611      S. Russell  updated method isDirty() to avoid a
 *                                      possible null pointer situation
 * 06/30/2016   R17964      J. Wu       Update filter after setting category.
 * 08/05/2016   R17973      B. Yin      Added setCurrentAction method.
 * 07/28/2016   R17954      B. Yin      handle CANCEL status of the SAVE dialog.
 * 11/30/2016   R17954      Bugenhagen  Changed promptToSaveOnClose to only prompt
 *                                      if editor has any elements drawn.
 * 12/27/2016   R27572      B. Yin      Fixed an undo/redo exception.
 * 05/04/2017   R27242      B. Yin      Use ParameterizedComand to pass parameters.
 * 
 * </pre>
 * 
 * @author sgilbert
 * 
 */
public class PgenPaletteWindow extends ViewPart
        implements SelectionListener, DisposeListener, CommandStackListener,
        IPartListener2, ISelectedPanesChangedListener, ISaveablePart2 {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PgenPaletteWindow.class);

    /*-
     * 1. Constants should be put in one place, probably in Utils.java.
     *
     * 2. The  number of column is a configurable constant. User should be 
     * able to change it. 
     *
     * TODO: We need to find a way to deal with these constants.
     */

    // blue
    private final int bgcolor = (0 * 65536) + (0 * 256) + 255;

    // white
    private final int fgcolor = (255 * 65536) + (255 * 256) + 255;

    private final String EXTENSION_POINT = "gov.noaa.nws.ncep.ui.pgen.palette";

    private final String CONTROL_SECTION = "control";

    private final String ACTION_SECTION = "action";

    private final String CLASS_SECTION = "class";

    private final String OBJECT_SECTION = "object";

    private final String CONTROL_LABEL = "Controls:";

    private final String ACTION_LABEL = "Actions:";

    private final String CLASS_LABEL = "Classes:";

    private final String OBJECT_LABEL = "Objects:";

    private IWorkbenchPage page;

    private Composite mainComp;

    private Composite paletteComp;

    private ScrolledComposite scroll;

    // Registered items with pgen.palette extension point
    private static IConfigurationElement[] paletteElements = null;

    // Map of all registered items with their name as the key
    private static HashMap<String, IConfigurationElement> itemMap = null;

    // List of items registered with the Control Section
    private ArrayList<String> controlNames = null;

    // List of items registered with the Action Section
    private ArrayList<String> actionNames = null;

    // List of items registered with the Class Section
    private ArrayList<String> classNames = null;

    // List of items registered with the Object Section
    private ArrayList<String> objectNames = null;

    private static Group objectBox;

    private Button undoButton = null;

    private Button redoButton = null;

    private String currentCategory = null;

    private String currentObject = null;

    private String currentAction = "";

    // map of buttons currently displayed on palette
    private HashMap<String, Button> buttonMap = null;

    // map of available icons
    private HashMap<String, Image> iconMap = null;

    // map of available "active" icons

    private HashMap<String, Image> activeIconMap = null;

    // Names of items that should appear on the palette

    private List<String> buttonList = null;

    private List<String> prevButtonList = null;

    private IContextActivation pgenContextActivation;

    private AbstractEditor currentIsMultiPane = null;

    // List of items thats control the palette size
    private static Group groupOutline;

    private static GridLayout gridLayout;

    private static int paletteSize;

    private static int rowSizePreference;

    /**
     * Constructor
     * 
     */
    public PgenPaletteWindow() {

        super();

    }

    /**
     * Invoked by the workbench to initialize this View.
     */
    public void init(IViewSite site) {

        try {

            super.init(site);

        } catch (PartInitException pie) {

            pie.printStackTrace();

        }

        page = site.getPage();
        page.addPartListener(this);

        // Get a list from registry of all elements that registered with the
        // gov.noaa.nws.ncep.ui.pgen.palette extension point

        if (paletteElements == null) {

            IExtensionRegistry registry = Platform.getExtensionRegistry();
            IExtensionPoint epoint = registry
                    .getExtensionPoint(EXTENSION_POINT);
            paletteElements = epoint.getConfigurationElements();

        }

        /*
         * create a hash map of the items registered with the
         * gov.noaa.nws.ncep.ui.pgen.palette extension point, using the item's
         * name attribute as the key
         */
        itemMap = new LinkedHashMap<>(paletteElements.length);
        controlNames = new ArrayList<>();
        actionNames = new ArrayList<>();
        classNames = new ArrayList<>();
        objectNames = new ArrayList<>();

        for (int i = 0; i < paletteElements.length; i++) {

            // Add item to hash map
            String itemName = paletteElements[i]
                    .getAttribute(PgenConstant.NAME);
            itemMap.put(itemName, paletteElements[i]);

            // create a list of item names that have been registered with each
            // section of the palette
            String type = paletteElements[i].getName();
            if (type.equals(CONTROL_SECTION)) {
                controlNames.add(itemName);
            } else if (type.equals(ACTION_SECTION)) {
                actionNames.add(itemName);
            } else if (type.equals(CLASS_SECTION)) {
                classNames.add(itemName);
            } else if (type.equals(OBJECT_SECTION)) {
                objectNames.add(itemName);
            }

        }

        // create hashmaps that will keep track of the buttons that appear on
        // the palette along with their images.
        buttonMap = new HashMap<>();
        iconMap = new HashMap<>();
        activeIconMap = new HashMap<>();

        // change the title to show cycle day and cycle hour
        PgenCycleTool.updateTitle();

    }

    /**
     * Disposes resource. invoked by the workbench
     */
    public void dispose() {

        super.dispose();

        // remove this palette from Pgen Session
        PgenSession.getInstance().removePalette();

        // remove the workbench part listener
        page.removePartListener(this);

        // clear map of SWT buttons on the palette
        buttonMap.clear();

        // dispose of icons
        for (Image icon : iconMap.values()) {
            icon.dispose();
        }
        for (Image icon : activeIconMap.values()) {
            icon.dispose();
        }

        // clear icon maps
        iconMap.clear();
        activeIconMap.clear();

        // change the title back to "CAVE"
        PgenUtil.resetCaveTitle();
    }

    /**
     * Invoked by the workbench, this method sets up the SWT controls for the
     * PGEN palette
     */
    @Override
    public void createPartControl(Composite comp) {
        // Gets the number of icons per row from preferences
        rowSizePreference = Activator.getDefault().getPreferenceStore()
                .getInt(PgenPreferences.P_ICONS_PER_ROW);
        gridLayout = new GridLayout(rowSizePreference, true);

        mainComp = comp;
        scroll = new ScrolledComposite(comp,
                SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        paletteComp = new Composite(scroll, SWT.NONE);

        scroll.setContent(paletteComp);

        // Single column, no equal width.
        paletteComp.setLayout(new GridLayout(1, false));

        // Add listener to scrolled composite to
        // change palette size when scroll size changes
        scroll.addControlListener(new ControlAdapter() {
            public void controlResized(ControlEvent e) {
                paletteResize();
            }
        });

        // create each section of the palette
        resetPalette(null);

        // Set this palette with the Pgen Session
        PgenSession.getInstance().setPalette(this);

        // Check current editor for a PgenResource. If found, register it with
        // the PgenSession

        PgenResource current = PgenUtil.findPgenResource(null);
        if (current != null) {
            PgenSession.getInstance().setResource(current);
            PgenUtil.setSelectingMode();

            AbstractEditor actEditor = PgenUtil.getActiveEditor();
            if (actEditor != null && !PgenSession.getInstance().getEditors()
                    .contains(actEditor)) {
                PgenSession.getInstance().getEditors().add(actEditor);
            }
        }

    }

    /**
     * create section of Palette where Objects will be displayed later
     * 
     * @param parent
     *            parent widget
     */
    private void createObjectSection(Composite parent) {

        Label control = new Label(parent, SWT.NONE);
        control.setText(OBJECT_LABEL);
        objectBox = new Group(parent, SWT.SHADOW_IN);

        // Uses grid layout if the row size is 1
        // to accommodate the larger symbol buttons
        if (rowSizePreference == 1) {
            objectBox.setLayout(gridLayout);
            objectBox.setLayoutData(new GridData());

        } else {
            RowLayout rowLayout = new RowLayout(SWT.HORIZONTAL);
            rowLayout.marginLeft = gridLayout.horizontalSpacing;
            objectBox.setLayout(rowLayout);
            objectBox.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        }

    }

    /**
     * Creates a section of the palette window and adds buttons for each item
     * that was registered with the specified section
     * 
     * @param parent
     *            - parent Widget
     * @param section
     *            - string indicating which section of the palette is being
     *            built
     */
    private void createPaletteSection(Composite parent, String section) {

        // Should attribute always be displayed in palette
        boolean isAlwaysVisible = true;

        // Create label for the section
        Label control = new Label(parent, SWT.NONE);
        control.setText(section);

        // Creates the box around the buttons and set it as GridLayout
        groupOutline = new Group(parent, SWT.SHADOW_IN);
        groupOutline.setLayout(gridLayout);
        groupOutline.setLayoutData(new GridData());

        // Get a list of buttons that are registered
        // for this section of the palette
        List<String> buttons = null;
        if (section.equals(CONTROL_LABEL)) {
            buttons = getControlNames();
        } else if (section.equals(ACTION_LABEL)) {
            buttons = getActionNames();
        } else if (section.equals(CLASS_LABEL)) {
            buttons = getClassNames();
        }

        /*
         * Loop through each item registered with this section, and add the item
         * to the palate if it is also in the buttonList. If the buttonList is
         * null, add all items.
         */
        for (String bname : buttons) {

            IConfigurationElement element = itemMap.get(bname);

            // Determine if button should be added to palette
            String always = element.getAttribute(PgenConstant.ALWAYS_VISIBLE);

            // IF ALWAYS_VISIBLE was not specified
            if (always == null) {

                // Skip this loop iteration
                // If the buttonList doesn't list this button
                if (buttonList != null) {
                    if (!buttonList.contains(bname)) {
                        continue;
                    }
                }

                // Set it as true
                isAlwaysVisible = true;

            } // If ALWAYS_VISIBLE is FALSE
            else if (always.equalsIgnoreCase(PgenConstant.FALSE)) {
                // Set it as FALSE
                isAlwaysVisible = false;

                // Skip this loop iteration if
                // buttonList doesn't have this button name
                if (buttonList != null) {
                    if (!buttonList.contains(bname)) {
                        continue;
                    }
                }

            } // If ALWAYS_VISIBLE is TRUE
            else if (always.equalsIgnoreCase(PgenConstant.TRUE)) {
                isAlwaysVisible = true;
            }

            // Skip this loop iteration, no button
            if (!isAlwaysVisible) {
                continue;
            }

            Button item = new Button(groupOutline, SWT.PUSH);

            // set label of button
            if (element.getAttribute(PgenConstant.LABEL) != null)
                item.setToolTipText(element.getAttribute(PgenConstant.LABEL));

            // create an icon image for the button, if an icon was specified in
            // the registered item.
            if (element.getAttribute(PgenConstant.ICON) != null) {

                Image icon = getIcon(element.getAttribute(PgenConstant.ICON));

                if (icon != null) {

                    item.setImage(icon);
                    item.addDisposeListener(this);

                } else {

                    // No icon available. Set text to display on button
                    item.setText(element.getAttribute(PgenConstant.NAME));

                }
            } else {

                // No icon available. Set text to display on button
                item.setText(element.getAttribute(PgenConstant.NAME));

            }

            // set the ConfigurationElement name in the button, so that all the
            // endpoint info can be accessed by the widgetSelected listener
            item.setData(element.getAttribute(PgenConstant.NAME));
            item.addSelectionListener(this);

            // Add button name to map of all
            // buttons currently displayed in the palette
            buttonMap.put(element.getAttribute(PgenConstant.NAME), item);

            // Save references to Undo and Redo buttons for future use
            if (item.getData().equals(PgenConstant.UNDO)) {
                undoButton = item;
            }
            if (item.getData().equals(PgenConstant.REDO)) {
                redoButton = item;
            }

        }
        groupOutline.pack();
        groupOutline.redraw();

    }

    /**
     * Resizes the palette to match the size of each section
     */
    private void paletteResize() {

        // Resizes the palette when the object box is larger than the palette
        // Only occurs when the icons per row is 1
        if (objectBox.getSize().x > groupOutline.getSize().x
                && rowSizePreference == 1) {
            paletteSize = objectBox.getSize().x
                    + (gridLayout.horizontalSpacing + gridLayout.marginWidth);
        } else {
            // matches the spacing of the object section
            gridLayout.horizontalSpacing = new RowLayout().spacing;
            paletteSize = groupOutline.getSize().x
                    + gridLayout.horizontalSpacing + gridLayout.marginWidth;

        }

        paletteComp.setSize(paletteComp.computeSize(paletteSize, SWT.DEFAULT));
        paletteComp.layout();

    }

    /**
     * Resets the Pgen Palette to display only the buttons specified in the
     * buttonNames list
     * 
     * @param buttonNames
     *            list of item Names that should be displayed on the palette. If
     *            null, display all possible buttons.
     */
    public void resetPalette(List<String> buttonNames) {

        // save for later use
        buttonList = buttonNames;

        // Dispose of all widgets currently in the palette
        Control[] kids = paletteComp.getChildren();
        for (int j = 0; j < kids.length; j++) {
            kids[j].dispose();
        }

        // create each section of the palette
        createPaletteSection(paletteComp, CONTROL_LABEL);
        createPaletteSection(paletteComp, ACTION_LABEL);
        createPaletteSection(paletteComp, CLASS_LABEL);

        createObjectSection(paletteComp);

        // Force a resize
        paletteResize();

        // wait for buttons to be created
        disableUndoRedo();

    }

    /**
     * Disable the Undo and Redo buttons on the palette
     */
    public void disableUndoRedo() {
        undoButton.setEnabled(false);
        redoButton.setEnabled(false);
    }

    /**
     * Invoked by the workbench when needed
     */
    @Override
    public void setFocus() {

        mainComp.setFocus();

    }

    /**
     * Invoked when SWT item is selected
     */
    public void widgetSelected(SelectionEvent se) {

        IEditorPart editor = VizWorkbenchManager.getInstance()
                .getActiveEditor();

        // Set perspective ID in session
        if (PgenSession.getInstance().getPerspectiveId() == null
                || PgenSession.getInstance().getPerspectiveId().isEmpty()) {
            AbstractVizPerspectiveManager pMngr = VizPerspectiveListener
                    .getCurrentPerspectiveManager();
            if (pMngr != null) {
                PgenSession.getInstance()
                        .setPerspectiveId(pMngr.getPerspectiveId());
            }
        }

        if (editor instanceof AbstractEditor) {

            // get the endpoint information associated with this button.
            Button btn = (Button) se.getSource();
            IConfigurationElement elem = itemMap.get(btn.getData());

            // get the section of the palette that this item was registered with
            String point = elem.getName();
            String btnName = btn.getData().toString();

            /*
             * When enter from "non-Multi_Select" to "Multi-Select", save off
             * the buttons in current palette and set palette to show all PGEN
             * classes.
             * 
             * When exit from Multi_Select to "non-Multi_Select", reset to show
             * classes defined in the current activity - what saved off in
             * "prevButtonList".
             */
            if (point.equals(ACTION_SECTION)) {
                if (!currentAction
                        .equalsIgnoreCase(PgenConstant.ACTION_MULTISELECT)) {
                    if (btnName.equalsIgnoreCase(
                            PgenConstant.ACTION_MULTISELECT)) {
                        // save away buttonList
                        prevButtonList = buttonList;

                        resetPalette(getBtnListWithAllClasses(buttonList));
                        setCurrentCategory(currentCategory, true);
                    }
                } else {
                    if (!btnName.equalsIgnoreCase(
                            PgenConstant.ACTION_MULTISELECT)) {
                        resetPalette(prevButtonList);
                        setCurrentCategory(currentCategory, true);
                    }
                }
            }

            /*
             * If the button selected is in the "control", "action", or "object"
             * section of the palette, then execute the command that is
             * registered with the commandId set for this button.
             */
            if (point.equals(CONTROL_SECTION) || point.equals(ACTION_SECTION)
                    || point.equals(OBJECT_SECTION)) {

                if (point.equals(OBJECT_SECTION) && currentAction
                        .equalsIgnoreCase(PgenConstant.ACTION_MULTISELECT)) {
                    if (currentCategory != null && currentCategory
                            .equalsIgnoreCase(PgenConstant.CATEGORY_MET)) {
                        if (currentObject != null) {
                            resetIcon(currentObject);
                        }

                        currentObject = elem.getAttribute(PgenConstant.NAME);
                        setActiveIcon(currentObject);

                    }
                    elem = itemMap.get(PgenConstant.ACTION_MULTISELECT);
                } else if (currentObject != null) {
                    resetIcon(currentObject);
                }

                // reset current action if it is different from the
                // newly-selected one.
                if (point.equals(ACTION_SECTION)) {
                    if (!btnName.equals(currentAction)) {
                        resetIcon(currentAction);
                    }
                }

                // change front/line type
                PgenSelectingTool selTool = null;
                if (point.equals(OBJECT_SECTION)
                        && currentAction
                                .equalsIgnoreCase(PgenConstant.ACTION_SELECT)
                        && (currentCategory
                                .equalsIgnoreCase(PgenConstant.CATEGORY_FRONT)
                                || currentCategory.equalsIgnoreCase(
                                        PgenConstant.CATEGORY_LINES))) {

                    AbstractVizPerspectiveManager mgr = VizPerspectiveListener
                            .getCurrentPerspectiveManager();

                    for (AbstractModalTool tool : mgr.getToolManager()
                            .getSelectedModalTools()) {

                        // check selecting tool and change front/line type
                        if (tool instanceof PgenSelectingTool) {
                            DrawableElement currentDe = ((PgenSelectingTool) tool)
                                    .getSelectedDE();
                            if (currentDe != null && (currentDe
                                    .getPgenCategory().equalsIgnoreCase(
                                            PgenConstant.CATEGORY_LINES)
                                    || currentDe.getPgenCategory()
                                            .equalsIgnoreCase(
                                                    PgenConstant.CATEGORY_FRONT))) {
                                selTool = (PgenSelectingTool) tool;
                            }
                            break;
                        }
                    }
                }

                if (selTool != null) {
                    selTool.changeSelectedLineType(
                            elem.getAttribute(PgenConstant.NAME));
                } else {
                    // clean up
                    PgenResource pgen = PgenUtil
                            .findPgenResource((AbstractEditor) editor);
                    if (pgen != null) {
                        pgen.removeGhostLine();
                        pgen.removeSelected();
                        pgen.deactivatePgenTools();
                    }

                    exeCommand(elem);

                    /*
                     * Sets current action. Don't set current action to
                     * undo/redo. Undo/redo button never gets highlighted.
                     */
                    if (point.equals(ACTION_SECTION)
                            && !elem.getAttribute(PgenConstant.NAME)
                                    .equalsIgnoreCase(PgenConstant.UNDO)
                            && !elem.getAttribute(PgenConstant.NAME)
                                    .equalsIgnoreCase(PgenConstant.REDO)) {
                        currentAction = elem.getAttribute(PgenConstant.NAME);
                    }
                }

            }
            /*
             * If a button in the "Class" section of the palette was pressed,
             * unload the current set of buttons in the Object section, and load
             * the object buttons registered as part of the class selected.
             */
            else if (point.equals(CLASS_SECTION)) {
                populateObjectSection(elem.getAttribute(PgenConstant.NAME));
            }
        } else {
            Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                    .getShell();

            // The data not loaded yet
            MessageBox mb = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);

            mb.setMessage(
                    "Pgen is not supported in this editor. Please select a mapEditor for Pgen to use first!");
            mb.open();
        }
    }

    public void widgetDefaultSelected(SelectionEvent se) {

    }

    /**
     * invoked when widget is disposed
     * 
     * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt
     *      .events.DisposeEvent)
     */
    public void widgetDisposed(DisposeEvent event) {

        // If a button is being disposed, remove it from the map of currently
        // displayed items in the palette
        if (event.getSource() instanceof Button) {

            Button btn = (Button) event.getSource();
            buttonMap.remove(btn.getData());

        }

    }

    /**
     * Called by the PgenCommandManager when its stack sizes change, when this
     * object is registered with the PgenCommandManager. Disables Undo and/or
     * Redo button when the stack is empty. Enables the button otherwise.
     */
    public void stacksUpdated(int undoSize, int redoSize) {

        if (undoButton != null) {
            if (undoSize <= 0)
                undoButton.setEnabled(false);
            else
                undoButton.setEnabled(true);
        }

        if (redoButton != null) {
            if (redoSize <= 0)
                redoButton.setEnabled(false);
            else
                redoButton.setEnabled(true);
        }

    }

    /**
     * Workbench part was activated. If it was an instance of NCMapEditor and
     * there is an instance of PgenResource for that editor, register it with
     * the PgenSession singleton.
     */
    @Override
    public void partActivated(IWorkbenchPartReference partRef) {
        IWorkbenchPart part = partRef.getPart(false);

        if (PgenUtil.isNatlCntrsEditor(part) || part instanceof VizMapEditor) {

            // Prevent PGEN going to another perspective
            AbstractVizPerspectiveManager pMngr = VizPerspectiveListener
                    .getCurrentPerspectiveManager();
            if (pMngr != null && pMngr.getPerspectiveId() != PgenSession
                    .getInstance().getPerspectiveId()) {
                return;
            }

            PgenResource rsc = PgenUtil.findPgenResource((AbstractEditor) part);

            if ((rsc == null) && (PgenUtil.getPgenMode() == PgenMode.SINGLE)) {
                rsc = PgenUtil.createNewResource();
            }

            if (rsc != null) {
                rsc.setCatFilter(new CategoryFilter((currentCategory == null)
                        ? PgenConstant.CATEGORY_ANY : currentCategory));
            }

            PgenSession.getInstance().setResource(rsc);

            AbstractEditor editor = (AbstractEditor) part;
            if (PgenUtil.getNumberofPanes(editor) > 1) {
                currentIsMultiPane = editor;
                PgenUtil.addSelectedPaneChangedListener(editor, this);
            }
            activatePGENContext();
        } else if (part instanceof PgenPaletteWindow) {
            activatePGENContext();

            // found NCMapEditor
            AbstractEditor editor = PgenUtil.getActiveEditor();
            if (editor != null) {
                IRenderableDisplay display = editor.getActiveDisplayPane()
                        .getRenderableDisplay();
                ResourceList rscList = display.getDescriptor()
                        .getResourceList();

                for (ResourcePair rp : rscList) {

                    if (rp != null
                            && rp.getResource() instanceof PgenResource) {
                        ((PgenResource) (rp.getResource())).setEditable(true);
                        if (!rp.getProperties().isVisible())
                            rp.getProperties().setVisible(true);
                    }
                }
                editor.refresh();
            }
        }

    }

    /**
     * Workbench part was brought on top. If it was an instance of NCMapEditor
     * and there is an instance of PgenResource for that editor, register it
     * with the PgenSession singleton.
     * 
     */
    @Override
    public void partBroughtToTop(IWorkbenchPartReference partRef) {
        IWorkbenchPart part = partRef.getPart(false);
        partActivated(partRef);

        if (PgenUtil.isNatlCntrsEditor(part) || part instanceof VizMapEditor) {
            AbstractEditor editor = (AbstractEditor) part;
            PgenResource rsc = PgenUtil.findPgenResource((AbstractEditor) part);

            if ((rsc != null) && (PgenUtil.getPgenMode() == PgenMode.SINGLE)
                    && (PgenUtil.doesLayerLink())) {

                NcDisplayName dispName = PgenUtil.getDisplayName(editor);

                if (dispName != null) { // sanity check
                    if (dispName.getId() > 0 && rsc != null) {

                        Product prod = rsc.getActiveProduct();
                        if (dispName.getId() <= prod.getLayers().size()) {
                            rsc.setActiveLayer(
                                    prod.getLayer(dispName.getId() - 1));
                        }
                    }
                }
            }

            // Open Product or Layer management dialog if necessary
            if (rsc != null)
                VizApp.runAsync(new ProductDialogStarter(rsc));
        }

    }

    @Override
    public void partClosed(IWorkbenchPartReference partRef) {
        IWorkbenchPart part = partRef.getPart(false);

        if (part instanceof PgenPaletteWindow) {
            /**
             * Perform cleanup async, after the part has fully closed.
             * Triggering other UI interaction during close can lead to errors,
             * specifically this has been seen with the PgenRemindDialog.
             */
            VizApp.runAsync(new Runnable() {

                @Override
                public void run() {
                    if (PgenUtil.getPgenMode() == PgenMode.SINGLE) {
                        PgenUtil.resetResourceData();
                        if (VizPerspectiveListener
                                .getCurrentPerspectiveManager() == null) {
                            return;
                        }
                        for (AbstractEditor editor : PgenSession.getInstance()
                                .getEditors()) {
                            unloadPgenResource(editor);
                        }

                        PgenSession.getInstance().endSession();

                        // R8354.
                        deactivatePGENContext();

                    } else if (PgenUtil.getPgenMode() == PgenMode.MULTIPLE
                            && PgenUtil.getActiveEditor() != null) {
                        unloadPgenResource(PgenUtil.getActiveEditor());
                    }

                    if (currentIsMultiPane != null) {
                        PgenUtil.removeSelectedPaneChangedListener(
                                currentIsMultiPane, PgenPaletteWindow.this);
                    }
                }
            });
        } else if (PgenUtil.isNatlCntrsEditor(part)) {
            PgenResource pgen = PgenUtil
                    .findPgenResource((AbstractEditor) part);
            if (pgen != null) {
                pgen.closeDialogs();

                // R8354
                deactivatePGENContext();
                ((AbstractEditor) part).refresh();
            }
        }
    }

    @Override
    public void partDeactivated(IWorkbenchPartReference partRef) {
        IWorkbenchPart part = partRef.getPart(false);

        if (PgenUtil.isNatlCntrsEditor(part)) {

            PgenResource pgen = PgenUtil
                    .findPgenResource((AbstractEditor) part);
            if (pgen != null) {

                // Comment out the following three lines to keep the drawing
                // tool and to keep the attribute dialog up
                // when user clicks on the blank space on PGEN pallete.
                // --bingfan 4/20/12

                // pgen.removeGhostLine();
                // pgen.removeSelected();
                // pgen.deactivatePgenTools();

                // not sure why closeDialogs() is put here and not sure why it's
                // commented out. --bingfan
                // pgen.closeDialogs();

                deactivatePGENContext();
                ((AbstractEditor) part).refresh();
            }

            AbstractEditor editor = (AbstractEditor) part;
            if (PgenUtil.getNumberofPanes(editor) > 1) {
                currentIsMultiPane = null;
                PgenUtil.removeSelectedPaneChangedListener(editor, this);
            }

        }

        else if (part instanceof PgenPaletteWindow) {
            deactivatePGENContext();
        }

    }

    @Override
    public void partOpened(IWorkbenchPartReference partRef) {
        IWorkbenchPart part = partRef.getPart(false);
        if (part instanceof PgenPaletteWindow) {
            ((PgenPaletteWindow) part).setPartName("PGEN");
        }
    }

    @Override
    public void partHidden(IWorkbenchPartReference partRef) {
        IWorkbenchPart part = partRef.getPart(false);

        if (PgenUtil.isNatlCntrsEditor(part) || part instanceof VizMapEditor) {
            PgenResource pgen = PgenUtil
                    .findPgenResource((AbstractEditor) part);
            if (pgen != null) {
                pgen.closeDialogs();
                pgen.deactivatePgenTools();
                deactivatePGENContext();
                ((AbstractEditor) part).refresh();
            }
        } else if (part instanceof PgenPaletteWindow) {
            PgenResource pgen = PgenSession.getInstance().getCurrentResource();
            if (pgen != null) {
                pgen.closeDialogs();
                pgen.deactivatePgenTools();
                deactivatePGENContext();
            }
        }
    }

    @Override
    public void partInputChanged(IWorkbenchPartReference partRef) {

    }

    @Override
    public void partVisible(IWorkbenchPartReference partRef) {
        IWorkbenchPart part = partRef.getPart(false);
        if (PgenUtil.isNatlCntrsEditor(part) && !PreloadGfaDataThread.loaded) {
            // preload the classes to reduce the first GFA format time
            new PreloadGfaDataThread().start();
        }

    }

    private void unloadPgenResource(AbstractEditor editor) {

        for (IRenderableDisplay display : UiUtil
                .getDisplaysFromContainer(editor)) {
            for (ResourcePair rp : display.getDescriptor().getResourceList()) {
                if (rp.getResource() instanceof PgenResource) {
                    PgenResource rsc = (PgenResource) rp.getResource();
                    rsc.unload();
                    display.getDescriptor().getResourceList()
                            .removePreRemoveListener(rsc);
                }
            }
        }
    }

    /**
     * 
     * @return the currently selected category on the palette
     */
    public String getCurrentCategory() {
        return currentCategory;
    }

    /*
     * Returns an icon image based on it's location
     */
    private Image getIcon(String iconLocation) {

        /*
         * If icon already loaded, use it
         */
        if (iconMap.containsKey(iconLocation))
            return iconMap.get(iconLocation);

        else {
            /*
             * load icon image from location specified.
             */
            ImageDescriptor id = Activator.imageDescriptorFromPlugin(
                    Activator.PLUGIN_ID, iconLocation);
            if (id != null) {
                Image icon = id.createImage();
                // add it to the available icon map
                iconMap.put(iconLocation, icon);
                return icon;
            } else
                return null;
        }
    }

    /**
     * Sets current action string.
     */
    public void setCurrentAction(String action) {
        currentAction = action;
    }

    public String getCurrentAction() {
        return currentAction;
    }

    /**
     * Finds the button with the given name, and sets its image to an "active"
     * version of the icon
     */
    public void setActiveIcon(String name) {

        // if name not recognized, do nothing
        if (!itemMap.containsKey(name)) {
            return;
        }
        // if button not currently displayed on palette, do nothing
        if (!buttonMap.containsKey(name)) {
            return;
        }
        String iconLocation = itemMap.get(name).getAttribute(PgenConstant.ICON);

        // If an active version of the icon exists, use it
        if (activeIconMap.containsKey(iconLocation)) {
            Image im = activeIconMap.get(iconLocation);
            buttonMap.get(name).setImage(im);
        } else {

            // create an "active" version of the icon from the original.
            Image im = iconMap.get(iconLocation);
            ImageData id = im.getImageData();

            for (int y = 0; y < id.height; y++) {
                for (int x = 0; x < id.width; x++) {
                    if (id.getPixel(x, y) == 0)
                        id.setPixel(x, y, fgcolor);
                    else
                        id.setPixel(x, y, bgcolor);
                }
            }

            // set "active" icon on button, and save it for later use.
            Image icon = new Image(im.getDevice(), id);
            buttonMap.get(name).setImage(icon);
            activeIconMap.put(iconLocation, icon);
        }

    }

    // Finds the button with the given name, and sets its image to the original
    // icon specified with the extension point

    public void resetIcon(String name) {

        // if name not recognized, do nothing
        if (!itemMap.containsKey(name))
            return;
        // if button not currently displayed on palette, do nothing
        if (!buttonMap.containsKey(name))
            return;

        IConfigurationElement elem = itemMap.get(name);

        // reset to original icon
        Image icon = getIcon(elem.getAttribute(PgenConstant.ICON));
        if (icon != null) {
            buttonMap.get(name).setImage(icon);
        }
    }

    // Sets up an eclipse Command and ExecuteEvent
    // for a registered commandId, and then executes it.
    private void exeCommand(IConfigurationElement elem) {

        // Get the commandId for this item
        String commandId = elem.getAttribute("commandId");
        /*
         * This code taken directly from
         * com.raytheon.viz.ui.glmap.actions.ClearAction
         * 
         * Finds the AbstractHandler currently registered with this commandId
         */
        IEditorPart part = VizWorkbenchManager.getInstance().getActiveEditor();
        ICommandService service = (ICommandService) part.getSite()
                .getService(ICommandService.class);
        Command pgenCommand = service.getCommand(commandId);

        if (pgenCommand != null) {

            try {
                // Set up information to pass to the AbstractHandler
                HashMap<String, Object> params = new HashMap<>();
                params.put(PgenConstant.NAME,
                        elem.getAttribute(PgenConstant.NAME));
                params.put(PgenConstant.CLASSNAME,
                        elem.getAttribute(PgenConstant.CLASSNAME));
                ExecutionEvent exec = new ExecutionEvent(pgenCommand, params,
                        null, elem.getAttribute(PgenConstant.NAME));

                /*
                 * If the command needs parameters, use ParameterizedCommand
                 * to pass the parameters.
                 */
                boolean needParameters = false;
                IParameter[] commandParams = pgenCommand.getParameters();
                if ( commandParams != null && commandParams.length >= 2 ) {
                        ArrayList<String> paraIds = new ArrayList<>();
                        for ( IParameter para : commandParams ){
                            paraIds.add(para.getId());
                        }
                        needParameters = paraIds.contains(PgenConstant.NAME)
                                && paraIds.contains(PgenConstant.CLASSNAME); 
                }
              
                if ( !needParameters ){
                    pgenCommand.executeWithChecks(exec);
                } else {
                    ParameterizedCommand parameterizedCommand = ParameterizedCommand
                            .generateCommand(pgenCommand, params);
                    IHandlerService handlerService = part.getSite()
                            .getService(IHandlerService.class);
                    handlerService.executeCommand(parameterizedCommand, null);
                }

                // Update the GUI elements on the menus and toolbars
                for (String toolbarID : NmapCommon
                        .getGUIUpdateElementCommands()) {
                    service.refreshElements(toolbarID, null);
                }

            } catch (Exception e) {
                // Error executing Handler
                e.printStackTrace();
                String msg = "Could not set PGEN drawing mode for the current map";
                ErrorDialog.openError(Display.getCurrent().getActiveShell(),
                        "Error Activating PGEN" + " Tool", msg,
                        new Status(Status.ERROR, Activator.PLUGIN_ID, msg, e));
            }
        }

    }

    /**
     * 
     * @return A list of names for the available buttons in the Control Section
     *         of the Palette
     */
    public List<String> getControlNames() {
        return controlNames;
    }

    /**
     * 
     * @return A list of names for the available buttons in the Action Section
     *         of the Palette
     */
    public List<String> getActionNames() {
        return actionNames;
    }

    /**
     * 
     * @return A list of names for the available buttons in the Class Section of
     *         the Palette
     */
    public List<String> getClassNames() {
        return classNames;
    }

    /**
     * 
     * @return A list of names for the available buttons in the Object Section
     *         of the Palette
     */
    public List<String> getObjectNames() {
        return objectNames;
    }

    /**
     * @param Name
     *            of a category in the class section
     * @return A list of names for the available buttons in the Object Section
     *         of the Palette associated with the given Class/Category
     */
    public List<String> getObjectNames(String className) {
        ArrayList<String> objs = new ArrayList<>();
        for (String name : getObjectNames()) {
            if (itemMap.get(name).getAttribute(PgenConstant.CLASSNAME)
                    .equals(className))
                objs.add(name);
        }
        return objs;
    }

    /**
     * @param Name
     *            of a button in the palette
     * @return The icon image associated with the button
     */
    public Image getButtonImage(String bname) {

        return getIcon(itemMap.get(bname).getAttribute(PgenConstant.ICON));

    }

    /**
     * @param Name
     *            of a button in the palate
     * @return The icon image associated with the button
     */
    public Image createNewImage(Image im, int fg, int bg) {

        // create an "active" version of the icon from the original.
        ImageData id = im.getImageData();

        for (int ii = 0; ii < id.height; ii++) {
            for (int jj = 0; jj < id.width; jj++) {
                if (id.getPixel(jj, ii) == 0)
                    id.setPixel(jj, ii, fg);
                else
                    id.setPixel(jj, ii, bg);
            }
        }

        // create a new Image.
        return (new Image(im.getDevice(), id));
    }

    /**
     * @param none
     * @return itemMap
     */
    public HashMap<String, IConfigurationElement> getItemMap() {
        return itemMap;
    }

    private void deactivatePGENContext() {
        IEditorPart editor = EditorUtil.getActiveEditor();
        if (pgenContextActivation != null
                && PgenUtil.isNatlCntrsEditor(editor)) {
            IContextService ctxSvc = (IContextService) PlatformUI.getWorkbench()
                    .getService(IContextService.class);
            ctxSvc.deactivateContext(pgenContextActivation);
            pgenContextActivation = null;
        }
    }

    private void activatePGENContext() {
        /*
         * Don't activate context outside of NCP, key bindings will conflict
         * with D2D
         */
        IEditorPart editor = EditorUtil.getActiveEditor();
        if (pgenContextActivation == null
                && PgenUtil.isNatlCntrsEditor(editor)) {
            IContextService ctxSvc = (IContextService) PlatformUI.getWorkbench()
                    .getService(IContextService.class);
            pgenContextActivation = ctxSvc
                    .activateContext("gov.noaa.nws.ncep.ui.pgen.pgenContext");
        }
    }

    /**
     * Set PGEN default action as "Select"
     */
    public void setDefaultAction() {
        currentAction = PgenConstant.ACTION_SELECT;
    }

    @Override
    public void selectedPanesChanged(String id, IDisplayPane[] pane) {
    }

    /**
     * @return the currentObject
     */
    public String getCurrentObject() {
        return currentObject;
    }

    /**
     * Sets the category and its icon.
     */
    public void setCurrentCategory(String currentCategory) {
        setCurrentCategory(currentCategory, false);

    }

    /**
     * Sets the category and its icon.
     */
    public void setCurrentCategory(String currentCategory,
            boolean createObjectSection) {
        this.resetIcon(this.currentCategory);
        this.currentCategory = currentCategory;
        this.setActiveIcon(currentCategory);
        if (createObjectSection) {
            populateObjectSection(null);
        }

        /*
         * Update category filter
         */
        PgenResource rsc = PgenSession.getInstance().getPgenResource();
        if (rsc != null) {
            String catg = PgenConstant.CATEGORY_ANY;
            if (currentCategory != null) {
                catg = currentCategory;
            }

            rsc.setCatFilter(new CategoryFilter(catg));
        }

    }

    private void populateObjectSection(String elem) {
        // remove currently loaded buttons from the Object section
        org.eclipse.swt.widgets.Control[] kids = objectBox.getChildren();
        for (int j = 0; j < kids.length; j++) {
            kids[j].dispose();
        }

        // reset the previous category's button icon
        if (currentCategory != null) {
            resetIcon(currentCategory);
        }

        // if elem is passed, used element's category
        if (elem != null) {
            currentCategory = elem;
        }

        // display "active" icon on the current Class's button
        setActiveIcon(currentCategory);

        // Loop threough each object registered
        // with the currentClass/Category
        for (String bname : getObjectNames(currentCategory)) {

            IConfigurationElement element = itemMap.get(bname);

            // determine if button should be added to palette

            if (buttonList != null) {
                if (!buttonList.contains(bname))
                    continue;
            }

            Button item = new Button(objectBox, SWT.PUSH);

            // Add button label
            if (element.getAttribute(PgenConstant.ICON) != null)
                item.setToolTipText(element.getAttribute(PgenConstant.LABEL));

            // create an icon image for the button, if an icon was
            // specified in the registered item.

            if (element.getAttribute(PgenConstant.ICON) != null) {

                Image icon = getIcon(element.getAttribute(PgenConstant.ICON));
                item.setImage(icon);
                item.addDisposeListener(this);

            } else {

                // No icon available. Set text to display on button
                item.setText(element.getAttribute(PgenConstant.NAME));

            }

            // set the ConfigurationElement name in the button, add to
            // map of currently displayed buttons

            item.setData(element.getAttribute(PgenConstant.NAME));
            item.addSelectionListener(this);
            buttonMap.put(element.getAttribute(PgenConstant.NAME), item);

            objectBox.setSize(
                    objectBox.computeSize(paletteSize, SWT.DEFAULT, true));
            objectBox.pack();
            objectBox.layout(true);
            objectBox.redraw();

        }

        // if multiSelect is current tool,
        // reload it now after category selection
        if (currentAction != null) {
            if (currentAction.isEmpty()) {
                currentAction = PgenConstant.ACTION_SELECT;
            }
            if (currentAction.equalsIgnoreCase(PgenConstant.ACTION_SELECT)
                    || currentAction
                            .equalsIgnoreCase(PgenConstant.ACTION_MULTISELECT)
                    || currentAction.equalsIgnoreCase(PgenConstant.ACTION_COPY)
                    || currentAction.equalsIgnoreCase(PgenConstant.ACTION_MOVE)
                    || currentAction
                            .equalsIgnoreCase(PgenConstant.ACTION_MODIFY)
                    || currentAction
                            .equalsIgnoreCase(PgenConstant.ACTION_CONNECT)
                    || currentAction
                            .equalsIgnoreCase(PgenConstant.ACTION_ROTATE)
                    || currentAction.equalsIgnoreCase(PgenConstant.ACTION_FLIP)
                    || currentAction
                            .equalsIgnoreCase(PgenConstant.ACTION_EXTRAP)
                    || currentAction
                            .equalsIgnoreCase(PgenConstant.ACTION_INTERP)) {
                // IConfigurationElement elememt = itemMap.get(currentAction);
                if (elem != null) {
                    exeCommand(itemMap.get(currentAction));
                }
            }
        }

        paletteResize();

    }

    /*
     * (non-Javadoc)
     * 
     * After the user has pressed "Yes" to save their PGen work launch a
     * StoreActivityDialog for them to fill out the things in order to save it.
     * 
     * @see org.eclipse.ui.ISaveablePart#doSave(org.eclipse.core.runtime.
     * IProgressMonitor )
     */
    public void doSave(IProgressMonitor monitor) {

        StoreActivityDialog storeDialog = null;

        try {
            storeDialog = new StoreActivityDialog(PlatformUI.getWorkbench()
                    .getActiveWorkbenchWindow().getShell(), "Save As");
        } catch (VizException e) {
            statusHandler.error("ERROR",
                    "Failure To Save PGen Activty Upon Closing PGen", e);
        }
        if (storeDialog != null) {
            storeDialog.setBlockOnOpen(true);
            if (storeDialog.open() == Window.CANCEL) {
                monitor.setCanceled(true);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * Is there unsaved PGen work?
     * 
     * @see org.eclipse.ui.ISaveablePart#isDirty()
     */
    public boolean isDirty() {
        boolean needsSaving = false;
        PgenSession pgenSession = PgenSession.getInstance();
        if (pgenSession == null) {
            return false;
        }
        PgenResource pgenResource = pgenSession.getPgenResource();
        if (pgenResource == null) {
            return false;
        }
        PgenResourceData prd = pgenResource.getResourceData();

        if (prd == null) {
            needsSaving = false;
        } else {
            needsSaving = prd.isNeedsSaving();
        }

        return needsSaving;
    }

    /*
     * (non-Javadoc)
     * 
     * Launch a custom dialog box to ask the user if they want to save their
     * PGen work.
     * 
     * @see org.eclipse.ui.ISaveablePart2#promptToSaveOnClose()
     */
    public int promptToSaveOnClose() {

        int returnCode = 0;
        AbstractEditor editor = PgenUtil.getActiveEditor();

        BufferedImage screenshot = editor.getActiveDisplayPane().getTarget()
                .screenshot();

        PgenResourceData prd = PgenSession.getInstance().getPgenResource()
                .getResourceData();

        // if this editor actually has any elements drawn, need to save
        boolean needToSave = false;
        if (prd != null && !prd.getActiveLayer().isEmpty()) {
            Layer layer = prd.getActiveLayer();
            if (!layer.getDrawables().isEmpty()) {
                needToSave = true;
            }
        }

        if (prd != null && needToSave) {
            returnCode = prd.promptToSave(screenshot);
        } else {
            // do not prompt for save if nothing to save
            returnCode = IDialogConstants.NO_ID;
        }

        /*-
         ********************************
         Label   ISaveablePart2  IDialog
         ********************************
         YES     0               2
         NO      1               3
         CANCEL  2               1
         *********************************
         */

        switch (returnCode) {
        case IDialogConstants.YES_ID:
            returnCode = ISaveablePart2.YES;
            break;
        case IDialogConstants.NO_ID:
            PgenSession.getInstance().getPgenResource().getResourceData()
                    .setNeedsSaving(false);
            returnCode = ISaveablePart2.NO;
            break;
        case IDialogConstants.CANCEL_ID:
            returnCode = ISaveablePart2.CANCEL;
            break;
        default:
            returnCode = ISaveablePart2.YES;
        }

        return returnCode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.ISaveablePart#isSaveAsAllowed()
     */
    public boolean isSaveAsAllowed() {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.ISaveablePart#isSaveOnCloseNeeded()
     */
    public boolean isSaveOnCloseNeeded() {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.ISaveablePart#doSaveAs()
     */
    public void doSaveAs() {
    }

    /*
     * Returns a name list of current control/action/object buttons in the
     * palette, and ALL class buttons.
     * 
     * @param btnList The list of current buttons in the palette.
     * 
     * @return The name list of current control/action/object buttons in the
     * palette, and ALL class button names.
     */
    private List<String> getBtnListWithAllClasses(List<String> btnList) {
        List<String> listWthAllClassList = null;
        if (btnList != null) {
            listWthAllClassList = new ArrayList<>();
            for (String btn : btnList) {
                if (!classNames.contains(btn)) {
                    listWthAllClassList.add(btn);
                }
            }

            listWthAllClassList.addAll(classNames);
        }

        return listWthAllClassList;
    }

}
