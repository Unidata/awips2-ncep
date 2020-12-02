package gov.noaa.nws.ncep.ui.pgen.rsc;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.geotools.referencing.GeodeticCalculator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.PixelExtent;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.MapDescriptor;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.IResourceDataChanged;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.ResourceList.RemoveListener;
import com.raytheon.uf.viz.core.rsc.capabilities.EditableCapability;
import com.raytheon.viz.ui.cmenu.IContextMenuProvider;
import com.raytheon.viz.ui.editor.AbstractEditor;
import com.raytheon.viz.ui.editor.IMultiPaneEditor;
import com.raytheon.viz.ui.input.EditableManager;
import com.raytheon.viz.ui.perspectives.AbstractVizPerspectiveManager;
import com.raytheon.viz.ui.perspectives.VizPerspectiveListener;
import com.raytheon.viz.ui.tools.AbstractModalTool;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateArrays;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.Point;

import gov.noaa.nws.ncep.ui.pgen.Activator;
import gov.noaa.nws.ncep.ui.pgen.PgenConstant;
import gov.noaa.nws.ncep.ui.pgen.PgenPreferences;
import gov.noaa.nws.ncep.ui.pgen.PgenSession;
import gov.noaa.nws.ncep.ui.pgen.PgenUtil;
import gov.noaa.nws.ncep.ui.pgen.PgenUtil.PgenMode;
import gov.noaa.nws.ncep.ui.pgen.action.PgenAction;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.AttrDlg;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.ContoursAttrDlg;
import gov.noaa.nws.ncep.ui.pgen.contours.ContourLine;
import gov.noaa.nws.ncep.ui.pgen.contours.ContourMinmax;
import gov.noaa.nws.ncep.ui.pgen.contours.Contours;
import gov.noaa.nws.ncep.ui.pgen.controls.PgenCommandManager;
import gov.noaa.nws.ncep.ui.pgen.controls.PgenFileNameDisplay;
import gov.noaa.nws.ncep.ui.pgen.display.AbstractElementContainer;
import gov.noaa.nws.ncep.ui.pgen.display.DisplayElementFactory;
import gov.noaa.nws.ncep.ui.pgen.display.DisplayProperties;
import gov.noaa.nws.ncep.ui.pgen.display.ElementContainerFactory;
import gov.noaa.nws.ncep.ui.pgen.display.IDisplayable;
import gov.noaa.nws.ncep.ui.pgen.display.ILine;
import gov.noaa.nws.ncep.ui.pgen.display.IText;
import gov.noaa.nws.ncep.ui.pgen.display.IText.DisplayType;
import gov.noaa.nws.ncep.ui.pgen.display.IText.FontStyle;
import gov.noaa.nws.ncep.ui.pgen.display.IText.TextJustification;
import gov.noaa.nws.ncep.ui.pgen.display.IText.TextRotation;
import gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent;
import gov.noaa.nws.ncep.ui.pgen.elements.Arc;
import gov.noaa.nws.ncep.ui.pgen.elements.DECollection;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableElement;
import gov.noaa.nws.ncep.ui.pgen.elements.IJetTools;
import gov.noaa.nws.ncep.ui.pgen.elements.Jet;
import gov.noaa.nws.ncep.ui.pgen.elements.Layer;
import gov.noaa.nws.ncep.ui.pgen.elements.Line;
import gov.noaa.nws.ncep.ui.pgen.elements.MultiPointElement;
import gov.noaa.nws.ncep.ui.pgen.elements.Product;
import gov.noaa.nws.ncep.ui.pgen.elements.SinglePointElement;
import gov.noaa.nws.ncep.ui.pgen.elements.Symbol;
import gov.noaa.nws.ncep.ui.pgen.elements.SymbolLocationSet;
import gov.noaa.nws.ncep.ui.pgen.elements.Text;
import gov.noaa.nws.ncep.ui.pgen.elements.WatchBox;
import gov.noaa.nws.ncep.ui.pgen.elements.tcm.Tcm;
import gov.noaa.nws.ncep.ui.pgen.filter.AcceptFilter;
import gov.noaa.nws.ncep.ui.pgen.filter.CategoryFilter;
import gov.noaa.nws.ncep.ui.pgen.filter.ElementFilter;
import gov.noaa.nws.ncep.ui.pgen.filter.ElementFilterCollection;
import gov.noaa.nws.ncep.ui.pgen.gfa.Gfa;
import gov.noaa.nws.ncep.ui.pgen.layering.PgenLayeringControlDialog;
import gov.noaa.nws.ncep.ui.pgen.productmanage.ProductManageDialog;
import gov.noaa.nws.ncep.ui.pgen.sigmet.Sigmet;
import gov.noaa.nws.ncep.ui.pgen.tca.TCAElement;
import gov.noaa.nws.ncep.ui.pgen.tca.TropicalCycloneAdvisory;
import gov.noaa.nws.ncep.ui.pgen.tools.AbstractPgenDrawingTool;
import gov.noaa.nws.ncep.ui.pgen.tools.AbstractPgenTool;
import gov.noaa.nws.ncep.ui.pgen.tools.PgenContoursTool;
import gov.noaa.nws.ncep.ui.pgen.tools.PgenSnapJet;

/**
 * gov.noaa.nws.ncep.ui.pgen.rsc.PgenResource This code has been developed by
 * the NCEP/SIB for use in the AWIPS2 system.
 *
 * Implements a drawing layer for PGEN products.
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 02/09                    B. Yin      Initial Creation.
 * 04/09                    S. Gilbert  Added PgenCommand for undo/redo.
 * 04/09        #88         J. Wu       Added Text.
 * 04/09        #89         J. Wu       Added Arc.
 * 05/09        #79         B. Yin      Added a List for points selected
 * 05/09        #89         J. Wu       Added Vector
 * 06/09        #116        B. Yin      Use AbstractDrawableComponent
 * 07/09        #131        J. Wu       Made all commands work only on active layer
 * 07/09        #131        J. Wu       Drew layers in mono color & filled mode.
 * 07/09        #131        J. Wu       Initialize product list when a PgenResource
 *                                      is created.
 * 07/09        #141        J. Wu       Added "replaceElements"
 * 08/09        #142        S. Gilbert
 * 09/09        #151        J. Wu       Added product management dialog
 * 09/30/09     #169        Greg Hull  NCMapEditor
 * 12/09        #167        J. Wu       Made getNearestElement work for a given DECollection
 * 12/09        #267        B. Yin      Fixed the delObj bug
 * 03/10        #223        M.Laryukhin Added Gfa
 * 04/10        #165        G.Zhang     Added the two setSelected( ) null arguments handling
 * 03/10        #223        M.Laryukhin Added Gfa
 * 04/10        #165        G.Zhang     Added the two setSelected( ) null arguments handling
 * 03/10        #265        B. Yin      Added filters for forecast hours
 * 09/10        #290        B. Yin      Calculate distance from line segment
 * 09/10        #151        J. Wu       Save product in LPF-style
 * 10/10        #310        S. Gilbert  Modified to support PGEN SINGLE mode
 * 02/11        ?           B. Yin      Select elements only in certain distance.
 * 04/11        ?           B. Yin      Re-factor IAttribute
 * 09/11        ?           B. Yin      Added Circle symbol for Inc/Dec selection.
 * 01/12        ?           J. Wu       TTR 444-Always display active product's active layer.
 * 03/12        ?           B. Yin      Make VAA text editable
 * 04/12        ?           B. Hebbard  Per B. Yin; in paintInternal(), add makeContextCurrent()
 *                                       on IGLTarget after screenshot to avoid GLException:
 *                                      "No OpenGL context current on this thread";
 *                                      workaround pending RTS regression fix.
 * 04/12        #705        J. Wu       TTR 542 - Draw elements in specific sequences.
 * 05/12        #610        J. Wu       TTR 397 - Select GFA by text box.
 * 07/12        #695        B. Yin      TTR 261 - Add Pgen resource editable capability.
 * 08/12        #655        B. Hebbard  TTR 382 - Add paintProps as parameter to IDisplayable draw
 * 09/12                    B. Hebbard  Merge out RTS changes from OB12.9.1
 * 03/13        #927        B. Yin      Implemented IContextMenuProvider interface
 * 04/13        #874        B. Yin      Added a method replaceElements with parameter parent.
 * 04/13        #977        S. Gilbert  PGEN Database support
 * 11/13        TTR 752     J. Wu       Add methods for CCFP text auto placement.
 * 09/14        TTR972      J. Wu       "Filled" object on the active layer should be
 *                                      drawn as "filled" even if the "filled" flag for
 *                                      the layer is "false".
 * 11/14        R5413       B. Yin      Display PGEN in side view in D2D
 * 12/14     R5198/TTR1057  J. Wu       Adding a method to select label over a line for Contours.
 * 06/15        R8199       S. Russell  Alter fillContextMenu() to NOT add a
 *                                      "Delete Label" option where not appropriate
 *
 * 07/13/2015   R8198       S. Russell  Altered fillContextMenu(),  added an
 *                                      argument to getActionList(). Moved
 *                                      several methods for 8199 into a the
 *                                      new class PgenActionXtra
 * 12/16/2105   R12597      B. Yin      Added context menu item to add line to contours
 * 01/27/2016   R13166      J. Wu       Add context menu item to add Text to contours as label-only min/max.
 *
 * 05/11/2016   R13560      S. Russell  Updated PaintInternal() to no longer
 *                                      take screenshots.  That functionality
 *                                      was moved to PgenPaletteWindow where
 *                                      it is used to make an exit dialog with
 *                                      a picture of CAVE.  Removed member
 *                                      variable paneImage ( the screen shot )
 * 06/15/2016   R13559      bkowal      File cleanup. Removed commented code.
 * 07/11/2016   R17943      J. Wu       Display all objects in a non-active activity if its display flag is on.
 * 07/21/2016   R16077      J. Wu       Add context menu for contour lines to remove labels.
 * 01/07/2020   71971       smanoj      Modified code to use PgenConstants
 * 01/09/2020   71072       smanoj      Fix some NullPointerException issues
 * Apr 06, 2020 77420       tjensen     Allow delete of specific contour labels
 * Oct 07, 2020 81798       smanoj      Add New Label option should stop showing up
 *                                      if 10 labels already exist on the Contour.
 * Dec 09, 2020 85217       smanoj      Moving ADD_NEW_LABEL to PgenConstant.
 *
 * </pre>
 *
 * @author B. Yin
 */
public class PgenResource
        extends AbstractVizResource<PgenResourceData, MapDescriptor>
        implements RemoveListener, IResourceDataChanged, IContextMenuProvider {

    /**
     * Ghost line for multi-point element.
     */
    PgenResourceGhost ghost = null;

    /*
     * List of elements that should be displayed in "selected" mode
     */
    private List<AbstractDrawableComponent> elSelected = null;

    private final ConcurrentHashMap<DrawableElement, AbstractElementContainer> displayMap;

    /*
     * selected elements that should be displayed with a marker other than the
     * default gray "DOT"
     */
    private HashMap<AbstractDrawableComponent, Symbol> selectedSymbol = null;

    private List<Integer> ptsSelectedIndex = null;

    private Color ptsSelectedColor = null;

    // a collection of filters
    private final ElementFilterCollection filters;

    private CategoryFilter catFilter;

    private static final String RESOURCE_NAME = "PGEN Resource";

    // Scale factor to allow easy selection of Gfa by its text box.
    private static final double GFA_TEXTBOX_SELECT_SCALE = 2.0;

    private static final int LABEL_DIALOG_WIDTH = 400;

    private static final int LABEL_DIALOG_HEIGHT = 300;

    private static final int LABEL_DIALOG_OFFSET_X = 100;

    private static final int LABEL_DIALOG_OFFSET_Y = 100;

    private boolean needsDisplay = false;

    /**
     * Default constructor
     */
    protected PgenResource(PgenResourceData resourceData,
            LoadProperties loadProperties) {

        super(resourceData, loadProperties);
        getCapability(EditableCapability.class).setEditable(true);

        resourceData.addChangeListener(this); // we want to know when PGEN
                                              // objects change

        elSelected = new ArrayList<>();
        selectedSymbol = new HashMap<>();
        displayMap = new ConcurrentHashMap<>();
        filters = new ElementFilterCollection();
        setCatFilter(new CategoryFilter());

        // Register this new resource with the Session
        PgenSession.getInstance().setResource(this);

    }

    /**
     * Called when resource is disposed
     *
     * @see com.raytheon.viz.core.rsc.IVizResource#dispose()
     */
    @Override
    public void disposeInternal() {

        // remove this PGEN Resource from the PGEN Session
        if (PgenSession.getInstance().getCurrentResource() == this) {
            PgenSession.getInstance().removeResource();
        }

        /*
         * release IDisplayable resources
         */
        for (AbstractElementContainer disp : displayMap.values()) {
            disp.dispose();
        }
        displayMap.clear();

        resourceData.removeChangeListener(this);

        closeDialogs();

    }

    /**
     * Saves all elements in the productList in the resourceData to a given file
     *
     * @param filename
     */
    public void saveProducts(String filename) {

        if (filename == null) {
            return;
        }

        resourceData.saveProducts(filename, false);

    }

    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        if (descriptor == null) {
            return null;
        }

        return descriptor.getCRS();
    }

    @Override
    public String getName() {
        return RESOURCE_NAME;
    }

    /**
     * Gets the PgenResource's CommandManager
     *
     * @return the commandMgr
     */
    public PgenCommandManager getCommandMgr() {
        return resourceData.getCommandMgr();
    }

    public String getShortName() {
        return null;
    }

    @Override
    public void initInternal(IGraphicsTarget target) throws VizException {
        EditableManager.makeEditable(this,
                getCapability(EditableCapability.class).isEditable());
    }

    public boolean isApplicable(PixelExtent extent) {
        return true;
    }

    @Override
    public boolean okToUnload() {
        /*
         * Allow unloading of Resource only in MULTIPLE Mode
         */
        return !(PgenUtil.getPgenMode() == PgenMode.SINGLE);
    }

    @Override
    public void paintInternal(IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {
        IDisplayPaneContainer editor = getResourceContainer();

        // Draw in main editor and side view (IMultiPaneEditor)
        if (editor instanceof AbstractEditor
                || editor instanceof IMultiPaneEditor) {
            DisplayElementFactory df = new DisplayElementFactory(target,
                    descriptor);

            this.needsDisplay = resourceData.isNeedsDisplay();
            drawProduct(target, paintProps);
            resourceData.setNeedsDisplay(false);

            if (elSelected != null) {
                drawSelected(target, paintProps);
            }
            if (ghost != null) {
                ghost.draw(target, paintProps, df, descriptor);
            }
        }
    }

    public boolean isProjectable(CoordinateReferenceSystem mapData) {
        return true;
    }

    @Override
    public void project(CoordinateReferenceSystem mapData) throws VizException {

        // Snap jet when projection changes
        for (Product prod : resourceData.getProductList()) {
            for (Layer layer : prod.getLayers()) {

                Iterator<AbstractDrawableComponent> iterator = layer
                        .getComponentIterator();
                while (iterator.hasNext()) {
                    AbstractDrawableComponent adc = iterator.next();
                    if (adc.getName().equalsIgnoreCase("jet")) {
                        IJetTools snapTool = ((Jet) adc).getSnapTool();
                        if (snapTool != null) {

                            ((PgenSnapJet) snapTool)
                                    .setMapDescriptor(getDescriptor());
                            snapTool.snapJet((Jet) adc);

                        }
                    }
                }
            }
        }

        for (AbstractElementContainer disp : displayMap.values()) {
            disp.setMapDescriptor(getDescriptor());
        }
    }

    /**
     * @param autoSaveFilename
     *            the autoSaveFilename to set
     */
    public void setAutoSaveFilename(String autoSaveFilename) {
        this.resourceData.setAutoSaveFilename(autoSaveFilename);
    }

    /**
     * @param autosave
     *            the autosave to set
     */
    public void setAutosave(boolean autosave) {
        this.resourceData.setAutosave(autosave);
    }

    /**
     * Adds a product into the PGEN drawing layer.
     *
     * @param prd
     *            The product being added.
     */
    public void addProduct(Product prd) {
        resourceData.getProductList().add(prd);
    }

    /**
     * Sets the ghost line for the PGEN drawing layer.
     *
     * @param ghost
     */
    public void setGhostLine(AbstractDrawableComponent ghost) {

        if (this.ghost == null) {
            this.ghost = new PgenResourceGhost();
        } else {
            this.ghost.dispose();
        }
        this.ghost.setGhostLine(ghost);
    }

    /**
     * Removes the ghost line from the PGEN drawing layer.
     */
    public void removeGhostLine() {

        this.ghost = null;

    }

    /**
     * Finds the nearest element in the products to the input point.
     *
     * @param point
     * @return the nearest element
     */
    public DrawableElement getNearestElement(Coordinate point) {

        DrawableElement nearestGfabyTextBox = getNearestGfaByTextBox(point,
                catFilter);
        DrawableElement nearestElm = getNearestElement(point, catFilter);

        if (nearestElm != null && nearestElm.getParent() != null
                && nearestElm.getParent() instanceof ContourLine
                && nearestElm instanceof Line) {
            nearestElm = getNearestElement(point,
                    (ContourLine) nearestElm.getParent(), nearestElm);
        }

        // Selecting Gfa by its text box first.
        if (nearestGfabyTextBox != null
                && ((nearestElm == null) || (nearestElm instanceof Gfa))) {
            return nearestGfabyTextBox;
        }
        return nearestElm;
    }

    /**
     * Finds the nearest element in the products to the input point.
     *
     * @param point
     * @return the nearest element
     */
    public DrawableElement getNearestElement(Coordinate point,
            ElementFilter filter) {

        DrawableElement nearestElement = null;
        double minDistance = Double.MAX_VALUE;

        Iterator<DrawableElement> iterator = resourceData.getActiveLayer()
                .createDEIterator();
        while (iterator.hasNext()) {
            DrawableElement element = iterator.next();

            if ((!filter.accept(element) || !filters.acceptOnce(element))
                    || (!catFilter.accept(element))) {
                continue;
            }

            double dist = getDistance(element, point);
            if (dist < minDistance) {
                minDistance = dist;
                nearestElement = element;
            }
        }

        if (minDistance < this.getMaxDistToSelect()) {
            return nearestElement;
        }

        return null;
    }

    /**
     * Finds the nearest component(DE/DECollection) in the products to the input
     * point.
     *
     * @param point
     * @return the nearest component
     */
    public AbstractDrawableComponent getNearestComponent(Coordinate point) {
        return getNearestComponent(point, new AcceptFilter(), false);
    }

    /**
     * Finds the nearest component(DE/DECollection) in the products to the input
     * point.
     *
     * @param point
     * @return the nearest component
     */
    public AbstractDrawableComponent getNearestComponent(Coordinate point,
            ElementFilter filter, boolean applyCatFilter) {

        AbstractDrawableComponent nearestComponent = null;
        double minDistance = Double.MAX_VALUE;

        GeodeticCalculator gc = new GeodeticCalculator(descriptor.getCRS());

        gc.setStartingGeographicPoint(point.x, point.y);

        Iterator<AbstractDrawableComponent> iterator = resourceData
                .getActiveLayer().getComponentIterator();

        while (iterator.hasNext()) {
            AbstractDrawableComponent comp = iterator.next();

            if ((!filter.accept(comp) || !filters.acceptOnce(comp))
                    || (applyCatFilter && !catFilter.accept(comp))) {
                continue;
            }

            double dist = getDistance(comp, point);

            if (dist < minDistance) {
                minDistance = dist;
                nearestComponent = comp;
            }
        }

        if (minDistance < getMaxDistToSelect()) {
            return nearestComponent;
        }

        return null;

    }

    private void drawSelected(IGraphicsTarget target,
            PaintProperties paintProps) {

        if (!elSelected.isEmpty()
                && PgenSession.getInstance().getPgenPalette() != null) {
            DisplayElementFactory df = new DisplayElementFactory(target,
                    descriptor);
            List<IDisplayable> displayEls = new ArrayList<>();
            HashMap<Symbol, CoordinateList> map = new HashMap<>();

            Symbol defaultSymbol;
            if (PgenSession.getInstance().getPgenPalette().getCurrentAction()
                    .equalsIgnoreCase("IncDec")) {
                defaultSymbol = new Symbol(null, new Color[] { Color.MAGENTA },
                        2.5f, 0.7, false, null, "Symbol", "CIRCLE");
            } else {
                defaultSymbol = new Symbol(null,
                        new Color[] { Color.lightGray }, 2.5f, 7.5, false, null,
                        "Marker", "DOT");
            }

            Symbol selectSymbol = new Symbol(null,
                    new Color[] { getPtsSelectedColor() }, 2.5f, 7.5, false,
                    null, "Marker", "DOT");

            CoordinateList defaultPts = new CoordinateList();
            CoordinateList selectPts = new CoordinateList();

            for (AbstractDrawableComponent el : elSelected) {
                if (el == null) {
                    return;
                }
                if (selectedSymbol.containsKey(el)) {
                    Symbol currSym = selectedSymbol.get(el);
                    Coordinate[] pts = CoordinateArrays
                            .toCoordinateArray(el.getPoints());
                    if (map.containsKey(currSym)) {
                        map.get(currSym).add(pts, true);
                    } else {
                        map.put(currSym, new CoordinateList(pts));
                    }
                } else {
                    for (Coordinate point : el.getPoints()) {
                        int pointIdx = el.getPoints().indexOf(point);
                        if (inSelectedIndex(pointIdx)) {
                            selectPts.add(point, true);
                        } else {
                            defaultPts.add(point, true);
                        }
                    }
                }
            }

            if (!defaultPts.isEmpty()) {
                SymbolLocationSet symset = new SymbolLocationSet(defaultSymbol,
                        defaultPts.toCoordinateArray());
                displayEls.addAll(df.createDisplayElements(symset, paintProps));
            }
            if (!selectPts.isEmpty()) {
                SymbolLocationSet symset = new SymbolLocationSet(selectSymbol,
                        selectPts.toCoordinateArray());
                displayEls.addAll(df.createDisplayElements(symset, paintProps));
            }
            if (!map.isEmpty()) {
                for (Entry<Symbol, CoordinateList> mapEntry : map.entrySet()) {
                    SymbolLocationSet symset = new SymbolLocationSet(
                            mapEntry.getKey(),
                            mapEntry.getValue().toCoordinateArray());
                    displayEls.addAll(
                            df.createDisplayElements(symset, paintProps));
                }
            }

            for (IDisplayable each : displayEls) {
                each.draw(target, paintProps);
                each.dispose();
            }
        }
    }

    private boolean inSelectedIndex(int pointIdx) {
        if (ptsSelectedIndex != null && !ptsSelectedIndex.isEmpty()) {
            return ptsSelectedIndex.contains(new Integer(pointIdx));
        }
        return false;
    }

    /**
     * Sets the selected element to the input element.
     *
     * @param element
     */
    public void setSelected(AbstractDrawableComponent comp) {

        elSelected.clear();
        if (comp != null) {
            elSelected.add(comp);
        }

    }

    /**
     * @param adcList
     */
    public void setSelected(List<AbstractDrawableComponent> adcList) {

        elSelected.clear();
        if (adcList != null) {
            elSelected.addAll(adcList);
        }

    }

    /**
     * add an ADC to the selected list.
     *
     * @param adc
     */
    public void addSelected(AbstractDrawableComponent adc) {
        elSelected.add(adc);
    }

    /**
     * add an ADC to the selected list.
     *
     * @param adc
     */
    public void addSelected(List<? extends AbstractDrawableComponent> adcList) {
        elSelected.addAll(adcList);
    }

    /**
     * remove an ADC from the selected list.
     *
     * @param adc
     */
    public void removeSelected(AbstractDrawableComponent adc) {
        if (elSelected.contains(adc)) {
            elSelected.remove(adc);
            removeSelectedSymbol(adc); // remove element from selected
                                       // element/marker registry
        }
    }

    /**
     * Sets the selected element to null.
     */
    public void removeSelected() {

        elSelected.clear();
        clearSelectedSymbol(); // clear the selected element/marker registry

        removePtsSelected();

    }

    /**
     * Returns the selected element.
     *
     * @return
     */
    public DrawableElement getSelectedDE() {

        if (elSelected.isEmpty()) {
            return null;
        }
        return elSelected.get(0).getPrimaryDE();
    }

    /**
     * Returns the first item(DE or Collection) in the selected list.
     *
     * @return
     */
    public AbstractDrawableComponent getSelectedComp() {

        if (elSelected.isEmpty()) {
            return null;
        }
        return elSelected.get(0);
    }

    /*
     * returns the list of all selected elements
     */
    public List<AbstractDrawableComponent> getAllSelected() {
        return elSelected;
    }

    /*
     * Get the product list.
     */
    public List<Product> getProducts() {

        return resourceData.getProductList();

    }

    /**
     * Replace one drawable element in the product list with another drawable
     * element.
     *
     * @param old
     *            Element to replace
     * @param Element
     *            new drawable element
     */
    public void replaceElement(AbstractDrawableComponent old,
            AbstractDrawableComponent newde) {

        /*
         * displose of resources held by old componenet
         */
        resetADC(old);

        resourceData.replaceElement(old, newde);
    }

    /**
     * Replace a set of drawable element in the active layer with another set of
     * drawable elements.
     *
     * @param old
     *            Elements to replace
     * @param newde
     *            New drawable elements
     */
    public void replaceElements(List<AbstractDrawableComponent> old,
            List<AbstractDrawableComponent> newde) {

        /*
         * release resources held by all the "old" DEs
         */
        for (AbstractDrawableComponent adc : old) {
            resetADC(adc);
        }

        DECollection parent = null;
        if (!old.isEmpty()) {
            parent = (DECollection) old.get(0).getParent();
        }

        resourceData.replaceElements(parent, old, newde);
    }

    /**
     * Replace a set of drawable element with another set of drawable elements.
     * If the parent is not null, old elements will be removed from the parent
     * and new elements will be add in the parent. If the parent is null, the
     * old element list should have same number of elements as the new list has.
     * Loop through each of the elements in the old list, find the parent,
     * remove the old element and add the new element.
     *
     * @param parent
     *            parent collection of the old elements
     * @param old
     *            Elements to replace
     * @param newde
     *            New drawable elements
     */
    public void replaceElements(DECollection parent,
            List<AbstractDrawableComponent> old,
            List<AbstractDrawableComponent> newde) {
        /*
         * release resources held by all the "old" DEs
         */
        for (AbstractDrawableComponent adc : old) {
            resetADC(adc);
        }

        resourceData.replaceElements(parent, old, newde);
    }

    /**
     * Replace existing products with new products.
     */
    public void replaceProduct(List<Product> prds) {

        for (Layer layer : resourceData.getActiveProduct().getLayers()) {
            resetADC(layer);
        }

        resourceData.replaceProduct(prds);

    }

    /**
     * Add products to the existing products.
     */
    public void addProduct(List<Product> prds) {

        resourceData.addProduct(prds);

    }

    /**
     * Append products with the existing products.
     */
    public void appendProduct(List<Product> prds) {

        resourceData.appendProduct(prds);

    }

    /**
     * remove an element from the product list
     *
     * @param de
     *            Element to be removed
     */
    public void removeElement(AbstractDrawableComponent adc) {

        /*
         * reset/dispose elements in the displayMap
         */
        resetADC(adc);

        resourceData.removeElement(adc);

    }

    /**
     * remove all elements from the product list
     *
     * @param de
     *            Element to be removed
     */
    public void removeAllProducts() {

        /*
         * reset/dispose elements in the displayMap
         */
        for (Product prod : resourceData.getProductList()) {
            for (Layer layer : prod.getLayers()) {
                resetADC(layer);
            }
        }

        resourceData.removeAllProducts();

    }

    /**
     * add a DrawableElement to the productList.
     *
     * @param de
     *            The DrawableElement being added.
     */
    public void addElement(AbstractDrawableComponent de) {

        resourceData.addElement(de);

    }

    /**
     * add a List of DrawableElements to the productList.
     *
     * @param elems
     *            List of DrawableElement being added.
     */
    public void addElements(List<AbstractDrawableComponent> elems) {

        resourceData.addElements(elems);

    }

    /**
     * Add the selected point
     *
     * @param ptIdx
     *            - index of the selected point
     */
    public void addPtSelected(int ptIdx) {

        if (ptsSelectedIndex == null) {
            ptsSelectedIndex = new ArrayList<>();
        }

        ptsSelectedIndex.add(ptIdx);

    }

    /**
     * Clear the list of the selected points.
     */
    public void removePtsSelected() {
        if (ptsSelectedIndex != null && !ptsSelectedIndex.isEmpty()) {
            ptsSelectedIndex.clear();
        }
    }

    /**
     * Returns the ptsSelectedColor, if it is set. If not, returns RED by
     * default
     *
     * @return color
     */
    public Color getPtsSelectedColor() {
        if (ptsSelectedColor == null) {
            return Color.red;
        }
        return ptsSelectedColor;

    }

    public void setPtsSelectedColor(Color clr) {
        ptsSelectedColor = clr;
    }

    public void setDefaultPtsSelectedColor() {
        ptsSelectedColor = null;
    }

    /**
     * Delete the part between point 1 and point 2 from an muliti-point element
     *
     * @param mpe
     *            - multi-point element
     * @param pt1
     *            - the first point of the deleting part
     * @param pt2
     *            - the second point of the deleting part
     */
    public void deleteElementPart(Line mpe, Coordinate pt1, Coordinate pt2) {

        /*
         * release resources currently held by mpe before it is modified
         */
        resetADC(mpe);

        resourceData.deleteElementPart(mpe, pt1, pt2);

    }

    /**
     * Selects all elements with the input pgenType
     *
     * @param pgenType
     *            - Pgen type
     * @return - total elements selected
     */
    public int selectObj(String pgenType) {

        int total = 0;
        elSelected.clear();

        Iterator<AbstractDrawableComponent> iterator = resourceData
                .getActiveLayer().getComponentIterator();

        while (iterator.hasNext()) {
            AbstractDrawableComponent element = iterator.next();
            String elType = element.getPgenType();
            if (elType != null && elType.equalsIgnoreCase(pgenType)) {
                elSelected.add(element);
                total++;
            }
        }

        return total;
    }

    /**
     * Deletes all selected elements.
     */
    public void deleteSelectedElements() {

        resourceData.removeElements(elSelected);

    }

    /**
     * @param activeProduct
     *            the activeProduct to set
     */
    public void setActiveProduct(Product activeProduct) {
        resourceData.setActiveProduct(activeProduct);

        String fname = activeProduct.getInputFile();
        if (fname == null) {
            fname = activeProduct.getOutputFile();
        }

        if (fname == null && resourceData.getProductManageDlg() != null) {
            fname = resourceData.getProductManageDlg()
                    .getPrdOutputFile(activeProduct);
        }

        if (fname != null) {
            PgenFileNameDisplay.getInstance().setFileName(fname);
        }
    }

    /**
     * @return the activeProduct
     */
    public Product getActiveProduct() {
        return resourceData.getActiveProduct();
    }

    /**
     * @param activeLayer
     *            the activeLayer to set
     */
    public void setActiveLayer(Layer activeLayer) {
        resourceData.setActiveLayer(activeLayer);
    }

    /**
     * @return the activeLayer
     */
    public Layer getActiveLayer() {
        return resourceData.getActiveLayer();
    }

    /**
     * Activate layering control.
     */
    public void activateLayering() {

        resourceData.activateLayering();

    }

    /**
     * remove all elements from the active layer
     *
     * @param de
     *            Element to be removed
     */
    public void removeAllActiveDEs() {

        /*
         * release resources held by all DEs in the layer
         */
        resetADC(resourceData.getActiveLayer());

        resourceData.removeAllActiveDEs();

    }

    /**
     * Add a specific marker to use when displaying this element in selected
     * mode
     *
     * @param adc
     *            The selected element
     * @param sym
     *            marker to display
     */
    public void registerSelectedSymbol(AbstractDrawableComponent adc,
            Symbol sym) {
        selectedSymbol.put(adc, sym);
    }

    /**
     * Remove the special marker to use for this selected element.
     *
     * @param adc
     *            the selected element
     */
    public void removeSelectedSymbol(AbstractDrawableComponent adc) {
        selectedSymbol.remove(adc);
    }

    /**
     * remove all elements from the selected element/marker registry
     */
    public void clearSelectedSymbol() {
        selectedSymbol.clear();
    }

    /**
     * Start product management or layering if necessary.
     */
    public void startProductManage() {

        resourceData.startProductManage();

    }

    /**
     * Activate product management.
     */
    public void activateProductManage() {

        resourceData.activateProductManage();

    }

    /**
     * Remove a product.
     */
    public void removeProduct(Product prd) {

        /*
         * reset/dispose elements in the displayMap
         */
        for (Layer layer : prd.getLayers()) {
            resetADC(layer);
        }

        resourceData.getProductList().remove(prd);
    }

    /**
     * Return the current product management dialog.
     */
    public ProductManageDialog getProductManageDlg() {
        return resourceData.getProductManageDlg();
    }

    /**
     * Return the current layering control dialog.
     */
    public PgenLayeringControlDialog getLayeringControlDlg() {
        return resourceData.getLayeringControlDlg();
    }

    /**
     * closes any dialogs
     */
    public void closeDialogs() {
        resourceData.closeDialogs();
    }

    /**
     * Releases the resources held by a DrawableElement
     *
     * @param el
     */
    public void resetElement(DrawableElement el) {

        if (displayMap.containsKey(el)) {
            displayMap.get(el).dispose();
            displayMap.remove(el);
        }
    }

    /**
     * Releases the resources held by a DrawableComponent
     *
     * @param adc
     */
    public void resetADC(AbstractDrawableComponent adc) {

        Iterator<DrawableElement> iterator = adc.createDEIterator();
        while (iterator.hasNext()) {
            DrawableElement el = iterator.next();
            resetElement(el);
        }
    }

    /**
     * Releases the resources held by all DEs to refresh all.
     *
     * @param adc
     */
    public void resetAllElements() {
        for (Product prd : this.resourceData.getProductList()) {
            for (Layer layer : prd.getLayers()) {
                this.resetADC(layer);
            }
        }
    }

    /**
     * Finds the nearest element in the a DECollection to the input point.
     *
     * @param point
     * @return the nearest element in the collection
     */
    public DrawableElement getNearestElement(Coordinate point,
            DECollection dec) {

        DrawableElement nearestElement = null;
        double minDistance = Double.MAX_VALUE;

        Iterator<DrawableElement> iterator = dec.createDEIterator();
        while (iterator.hasNext()) {
            DrawableElement element = iterator.next();

            if (filters.acceptOnce(element)) {

                double dist = getDistance(element, point);
                if (minDistance < 0 || dist < minDistance) {

                    minDistance = dist;
                    nearestElement = element;

                }
            }
        }

        return nearestElement;

    }

    /**
     * Finds the nearest Text element in the a DECollection that is close to a
     * specified element (with MaxDistToSelect()/5) in the same DECollection.
     *
     * @param point
     * @param dec
     * @param nearestDe
     * @return the nearest element in the collection
     */
    public DrawableElement getNearestElement(Coordinate point, DECollection dec,
            DrawableElement nearestDe) {

        DrawableElement nearestElement = null;
        double minDistance = Double.MAX_VALUE;
        double distToLine = getDistance(nearestDe, point);

        // Find the closest Text element within "MaxDistToSelect".
        Iterator<DrawableElement> iterator = dec.createDEIterator();
        while (iterator.hasNext()) {
            DrawableElement element = iterator.next();

            if (element instanceof Text) {
                double dist = getDistance(element, point);
                if (dist < this.getMaxDistToSelect() && dist < minDistance) {
                    minDistance = dist;
                    nearestElement = element;
                }
            }
        }

        /*
         * If the closest Text element is not "close" enough to the specified
         * de, return the specified DE.
         */
        if (Math.abs(
                minDistance - distToLine) > (this.getMaxDistToSelect() / 5)) {
            nearestElement = nearestDe;
        }

        return nearestElement;

    }

    /**
     * Invoked by Pane before resource is removed. Checks if changes need to be
     * saved.
     */
    @Override
    public void notifyRemove(ResourcePair rp) throws VizException {

        if (rp.getResource() == this) {
            /*
             * this resource is about to be removed, allow resourceData chance
             * to clean up
             */
            resourceData.cleanup();
        }

    }

    /**
     * Get the filters
     *
     * @return the filter collection in pgen resource
     */
    public ElementFilterCollection getFilters() {
        return filters;
    }

    /**
     * Calculate the minimum screen distance from the input location 'loc' to
     * the input DrawableElement(or DECollection) 'adc'. If adc is a
     * MultiPointElement, distances are calculated from the input point to each
     * line segments.
     *
     * @param adc
     * @param loc
     * @return
     */
    public double getDistance(AbstractDrawableComponent adc, Coordinate loc) {

        double minDist = Double.MAX_VALUE;

        AbstractEditor mapEditor = PgenUtil.getActiveEditor();
        double[] locScreen = mapEditor.translateInverseClick(loc);

        if (adc instanceof SinglePointElement) {

            double[] pt = mapEditor.translateInverseClick(
                    ((SinglePointElement) adc).getLocation());

            Point ptScreen = new GeometryFactory()
                    .createPoint(new Coordinate(pt[0], pt[1]));
            minDist = ptScreen.distance(new GeometryFactory()
                    .createPoint(new Coordinate(locScreen[0], locScreen[1])));
        } else if (adc instanceof TCAElement) {
            TCAElement tca = (TCAElement) adc;
            double dist = Double.MAX_VALUE;

            for (TropicalCycloneAdvisory advisory : tca.getAdvisories()) {
                for (Coordinate[] coords : advisory.getSegment().getPaths()) {
                    for (int ii = 0; ii < coords.length - 1; ii++) {

                        dist = distanceFromLineSegment(loc, coords[ii],
                                coords[ii + 1]);
                        if (dist < minDist) {

                            minDist = dist;

                        }
                    }
                }
            }

        } else if (adc instanceof Gfa) {

            Gfa gfa = (Gfa) adc;

            // calculate distance from the text box
            double[] pt = mapEditor
                    .translateInverseClick(gfa.getGfaTextCoordinate());
            Point ptScreen = new GeometryFactory()
                    .createPoint(new Coordinate(pt[0], pt[1]));
            minDist = ptScreen.distance(new GeometryFactory()
                    .createPoint(new Coordinate(locScreen[0], locScreen[1])));

            double dist = Double.MAX_VALUE;

            Object[] pts = gfa.getPoints().toArray();

            for (int ii = 0; ii < pts.length; ii++) {

                if (ii == pts.length - 1) {
                    if (gfa.isClosedLine()) {
                        dist = distanceFromLineSegment(loc,
                                (Coordinate) pts[ii], (Coordinate) pts[0]);
                    } else {
                        break;
                    }
                } else {
                    dist = distanceFromLineSegment(loc, (Coordinate) pts[ii],
                            (Coordinate) pts[ii + 1]);
                }

                if (dist < minDist) {

                    minDist = dist;

                }
            }
        } else if (adc instanceof Arc) {
            Arc arc = (Arc) adc;

            double[] center = mapEditor
                    .translateInverseClick(arc.getCenterPoint());
            double[] circum = mapEditor
                    .translateInverseClick(arc.getCircumferencePoint());

            Point ctrScreen = new GeometryFactory()
                    .createPoint(new Coordinate(center[0], center[1]));

            minDist = ctrScreen.distance(new GeometryFactory()
                    .createPoint(new Coordinate(locScreen[0], locScreen[1])));

            /*
             * calculate angle of major axis
             */
            double axisAngle = Math.toDegrees(Math
                    .atan2((circum[1] - center[1]), (circum[0] - center[0])));
            double cosineAxis = Math.cos(Math.toRadians(axisAngle));
            double sineAxis = Math.sin(Math.toRadians(axisAngle));

            /*
             * calculate half lengths of major and minor axes
             */
            double[] diff = { circum[0] - center[0], circum[1] - center[1] };
            double major = Math.sqrt((diff[0] * diff[0]) + (diff[1] * diff[1]));
            double minor = major * arc.getAxisRatio();

            double angle = arc.getStartAngle();
            int numpts = (int) Math
                    .round(arc.getEndAngle() - arc.getStartAngle() + 1.0);

            for (int j = 0; j < numpts; j++) {
                double thisSine = Math.sin(Math.toRadians(angle));
                double thisCosine = Math.cos(Math.toRadians(angle));

                double xx = center[0] + (major * cosineAxis * thisCosine)
                        - (minor * sineAxis * thisSine);
                double yy = center[1] + (major * sineAxis * thisCosine)
                        + (minor * cosineAxis * thisSine);

                Point ptScreen = new GeometryFactory()
                        .createPoint(new Coordinate(xx, yy));
                double dist = ptScreen
                        .distance(new GeometryFactory().createPoint(
                                new Coordinate(locScreen[0], locScreen[1])));
                if (dist < minDist) {
                    minDist = dist;
                }

                angle += 1.0;
            }
        } else if (adc instanceof MultiPointElement) {

            if (adc instanceof gov.noaa.nws.ncep.ui.pgen.sigmet.Sigmet
                    && ("Isolated".equalsIgnoreCase(
                            ((gov.noaa.nws.ncep.ui.pgen.sigmet.Sigmet) adc)
                                    .getType())
                            || ((gov.noaa.nws.ncep.ui.pgen.sigmet.Sigmet) adc)
                                    .getType().contains("Text"))) {

                double[] pt = mapEditor.translateInverseClick(
                        ((gov.noaa.nws.ncep.ui.pgen.sigmet.Sigmet) adc)
                                .getLinePoints()[0]);
                Point ptScreen = new GeometryFactory()
                        .createPoint(new Coordinate(pt[0], pt[1]));
                minDist = ptScreen.distance(new GeometryFactory().createPoint(
                        new Coordinate(locScreen[0], locScreen[1])));

            }

            MultiPointElement mpe = (MultiPointElement) adc;

            double dist = Double.MAX_VALUE;

            Object[] pts = mpe.getPoints().toArray();

            for (int ii = 0; ii < pts.length; ii++) {

                if (mpe instanceof Tcm && pts.length == 1) {
                    double[] pt = mapEditor
                            .translateInverseClick(mpe.getLinePoints()[0]);
                    Point ptScreen = new GeometryFactory()
                            .createPoint(new Coordinate(pt[0], pt[1]));
                    minDist = ptScreen.distance(
                            new GeometryFactory().createPoint(new Coordinate(
                                    locScreen[0], locScreen[1])));
                    break;
                }

                if (ii == pts.length - 1) {
                    if ((mpe instanceof Line && ((Line) mpe).isClosedLine())
                            || mpe instanceof WatchBox
                            || (mpe instanceof gov.noaa.nws.ncep.ui.pgen.sigmet.Sigmet
                                    && Sigmet.AREA.equalsIgnoreCase(
                                            ((gov.noaa.nws.ncep.ui.pgen.sigmet.Sigmet) mpe)
                                                    .getType()))) {

                        dist = distanceFromLineSegment(loc,
                                (Coordinate) pts[ii], (Coordinate) pts[0]);
                    } else {
                        break;
                    }
                } else {
                    dist = distanceFromLineSegment(loc, (Coordinate) pts[ii],
                            (Coordinate) pts[ii + 1]);
                }
                if (dist < minDist) {
                    minDist = dist;
                }
            }
        } else if (adc instanceof DECollection) {
            Iterator<DrawableElement> it = ((DECollection) adc)
                    .createDEIterator();
            while (it.hasNext()) {
                double dist = getDistance(it.next(), loc);
                if (dist < minDist) {
                    minDist = dist;
                }
            }
        }

        return minDist;
    }

    /**
     * Calculate SCREEN distance from an input point to a line segment The
     * coordinate of the point and line are lat/lon.
     *
     * @param loc
     *            - input point
     * @param startPt
     *            - start point of the line segment
     * @param endPt
     *            - end point of the line segment
     * @return
     */
    public static double distanceFromLineSegment(Coordinate loc,
            Coordinate startPt, Coordinate endPt) {
        double dist = Double.MAX_VALUE;

        AbstractEditor mapEditor = PgenUtil.getActiveEditor();
        double[] locScreen = mapEditor.translateInverseClick(loc);

        double[] pt1 = mapEditor.translateInverseClick(startPt);
        double[] pt2 = mapEditor.translateInverseClick(endPt);
        LineSegment seg = new LineSegment(new Coordinate(pt1[0], pt1[1]),
                new Coordinate(pt2[0], pt2[1]));

        dist = seg.distance(new Coordinate(locScreen[0], locScreen[1]));

        return dist;
    }

    @Override
    public void resourceChanged(ChangeType type, Object object) {
        // no op
    }

    public void setCatFilter(CategoryFilter catFilter) {
        this.catFilter = catFilter;
    }

    public int getMaxDistToSelect() {
        IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
        int maxDist = prefs.getInt(PgenPreferences.P_MAX_DIST);
        if (maxDist <= 0) {
            maxDist = 30;
        }
        return maxDist;
    }

    /**
     * Saves the current product in the resourceData to its configured file
     *
     * @param filename
     */
    public void saveCurrentProduct(String filename) {
        resourceData.saveCurrentProduct(filename);
    }

    /**
     * Stores the current product in the resourceData to EDEX
     *
     * @param label
     */
    public void storeCurrentProduct(String label) {
        resourceData.storeCurrentProduct(label);
    }

    /**
     * Saves all products in the resourceData to their configured files.
     *
     */
    public void saveAllProducts() {

        resourceData.saveAllProducts();

    }

    /**
     * Stores all products in the resourceData to EDEX.
     *
     */
    public void storeAllProducts() {

        resourceData.storeAllProducts();

    }

    /**
     * Build a full path file name for a product's configured path/output file
     * name.
     */
    public String buildFileName(Product prd) {
        return resourceData.buildFileName(prd);
    }

    /**
     * Build an activity label for a product's configured path/output name.
     */
    public String buildActivityLabel(Product prd) {
        return resourceData.buildActivityLabel(prd);
    }

    /**
     * De-activate all PGEN tools for all perspectives
     */
    public void deactivatePgenTools() {

        for (String pid : VizPerspectiveListener.getManagedPerspectives()) {
            AbstractVizPerspectiveManager mgr = VizPerspectiveListener
                    .getInstance().getPerspectiveManager(pid);
            if (mgr != null) {
                Iterator<AbstractModalTool> it = mgr.getToolManager()
                        .getSelectedModalTools().iterator();
                while (it.hasNext()) {
                    AbstractModalTool tool = it.next();
                    if (tool instanceof AbstractPgenTool) {
                        tool.deactivate();
                        it.remove();
                    }
                }
            }
        }

    }

    /**
     * Loops through all products in the PGEN drawing layer to draw display
     * elements.
     *
     * The drawing sequence will be:
     *
     * First draw all filled elements starting from those in the non-active
     * products, then those in non-active layers in active product, finally
     * those in active layer.
     *
     * Then draw non-filled elements, starting from those in the non-active
     * products, then those in non-active layers in active product, finally
     * those in active layer. However, for each layer, if there are text
     * elements in a layer, the text elements will be the last ones to be drawn
     * so they could remain on the top.
     *
     * @param target
     *            Graphic target from the paint() method.
     * @param paintProps
     *            Paint properties from the paint() method.
     */
    private void drawProduct(IGraphicsTarget target,
            PaintProperties paintProps) {
        drawFilledElements(target, paintProps);

        drawNonFilledElements(target, paintProps);
    }

    /**
     * Loops through all products in the PGEN drawing layer to draw all filled
     * elements
     *
     * @param target
     *            Graphic target from the paint() method.
     *
     * @param paintProps
     *            Paint properties from the paint() method.
     */
    private void drawFilledElements(IGraphicsTarget target,
            PaintProperties paintProps) {

        // Draw filled elements in the non-active products
        for (Product prod : resourceData.getProductList()) {
            if (prod.isOnOff() && prod != resourceData.getActiveProduct()) {
                for (Layer layer : prod.getLayers()) {
                    drawFilledElements(layer, target, paintProps,
                            prod.isOnOff());
                }
            }
        }

        // Draw filled elements in the active product's non-active layers
        for (Layer layer : resourceData.getActiveProduct().getLayers()) {
            if (layer != resourceData.getActiveLayer()) {
                drawFilledElements(layer, target, paintProps, false);
            }
        }

        // Draw filled elements in the active layer
        drawFilledElements(resourceData.getActiveLayer(), target, paintProps,
                true);

    }

    /*
     * Draw all filled elements in a given layer.
     *
     * @param layer Layer to be drawn.
     *
     * @param target Graphic target from the paint() method.
     *
     * @param paintProps Paint properties from the paint() method.
     *
     * @param displayProduct flag to display this product regardless of layers.
     */
    private void drawFilledElements(Layer layer, IGraphicsTarget target,
            PaintProperties paintProps, boolean displayProduct) {

        /*
         * The layer will be displayed if the displayProduct flag is on or the
         * layer display flag is on or this layer is the active layer.
         */
        if (layer != null && (displayProduct || layer.isOnOff()
                || layer == resourceData.getActiveLayer())) {

            DisplayProperties dprops = new DisplayProperties();

            if (layer != resourceData.getActiveLayer()) {
                dprops.setLayerMonoColor(layer.isMonoColor());
                dprops.setLayerColor(layer.getColor());
                dprops.setLayerFilled(layer.isFilled());
            } else {
                /*
                 * "Filled" should always be set to "true" for displaying the
                 * active layer.
                 */
                dprops.setLayerFilled(true);
            }

            Iterator<DrawableElement> iterator = layer.createDEIterator();
            while (iterator.hasNext()) {
                DrawableElement el = iterator.next();

                if (el instanceof MultiPointElement
                        && ((MultiPointElement) el).getFilled()) {
                    drawElement(el, target, paintProps, dprops);
                }
            }
        }
    }

    /*
     * Loops through all products in the PGEN drawing layer to draw all
     * non-filled elements
     *
     * @param target Graphic target from the paint() method.
     *
     * @param paintProps Paint properties from the paint() method.
     */
    private void drawNonFilledElements(IGraphicsTarget target,
            PaintProperties paintProps) {

        // Draw non-filled elements in the non-active products
        for (Product prod : resourceData.getProductList()) {
            if (prod.isOnOff() && prod != resourceData.getActiveProduct()) {
                for (Layer layer : prod.getLayers()) {
                    drawNonFilledElements(layer, target, paintProps,
                            prod.isOnOff());
                }
            }
        }

        // Draw non-filled elements in the active product's non-active layers
        for (Layer layer : resourceData.getActiveProduct().getLayers()) {
            if (layer != resourceData.getActiveLayer()) {
                drawNonFilledElements(layer, target, paintProps, false);
            }
        }

        // Draw non-filled elements in the active layer
        drawNonFilledElements(resourceData.getActiveLayer(), target, paintProps,
                true);

    }

    /*
     * Draw all non-filled elements in a layer.
     *
     * Non-text elements will drawn FIRST, then text elements - so text elements
     * will be displayed on top of other element.
     *
     * @param layer a PGEN layer to be drawn
     *
     * @param target Graphic target from the paint() method.
     *
     * @param paintProps Paint properties from the paint() method.
     *
     * @param displayProduct flag to display this layer regardless of layers.
     */
    private void drawNonFilledElements(Layer layer, IGraphicsTarget target,
            PaintProperties paintProps, boolean displayProduct) {

        /*
         * The layer will be displayed if the displayProduct flag is on or the
         * layer display flag is on or this layer is the active layer.
         */
        if (layer != null && (displayProduct || layer.isOnOff()
                || layer == resourceData.getActiveLayer())) {

            DisplayProperties dprops = new DisplayProperties();
            if (layer != resourceData.getActiveLayer()) {
                dprops.setLayerMonoColor(layer.isMonoColor());
                dprops.setLayerColor(layer.getColor());
                dprops.setLayerFilled(layer.isFilled());
            }

            Iterator<DrawableElement> iterator = layer.createDEIterator();

            ArrayList<DrawableElement> filledElements = new ArrayList<>();
            ArrayList<DrawableElement> ccfpTextElements = new ArrayList<>();
            ArrayList<DrawableElement> nonCcfpTextElements = new ArrayList<>();
            ArrayList<DrawableElement> otherElements = new ArrayList<>();
            ArrayList<DrawableElement> ccfpArrowElements = new ArrayList<>();

            while (iterator.hasNext()) {
                DrawableElement el = iterator.next();

                if (el instanceof Text) {
                    if (isCCFPText(el)) {
                        ccfpTextElements.add(el);
                    } else {
                        nonCcfpTextElements.add(el);
                    }
                } else if (el instanceof MultiPointElement) {
                    if (((MultiPointElement) el).getFilled()) {
                        filledElements.add(el);
                    } else if (isCCFPArrow(el)) {
                        ccfpArrowElements.add(el);
                    } else {
                        otherElements.add(el);
                    }
                } else {
                    otherElements.add(el);
                }
            }

            drawElements(otherElements, target, paintProps, dprops);

            drawElements(nonCcfpTextElements, target, paintProps, dprops);
            drawElements(ccfpTextElements, target, paintProps, dprops);

            // CCFP arrows should be draw last if autoplacement is on?
            drawElements(ccfpArrowElements, target, paintProps, dprops);
        }

    }

    /*
     * Loops through all elements in a list to draw display elements.
     *
     * @param elements List of elements to be drawn
     *
     * @param target Graphic target from the paint() method.
     *
     * @param paintProps Paint properties from the paint() method.
     *
     * @param dispProps Display properties from the drawLayer() method.
     */
    private void drawElements(ArrayList<DrawableElement> elements,
            IGraphicsTarget target, PaintProperties paintProps,
            DisplayProperties dispProps) {

        for (DrawableElement de : elements) {
            drawElement(de, target, paintProps, dispProps);
        }
    }

    /*
     * Draw an element.
     *
     * @param de Element to be drawn
     *
     * @param target Graphic target from the paint() method.
     *
     * @param paintProps Paint properties from the paint() method.
     *
     * @param dispProps Display properties from the drawLayer() method.
     */
    private void drawElement(DrawableElement de, IGraphicsTarget target,
            PaintProperties paintProps, DisplayProperties dispProps) {

        if (filters.acceptOnce(de)) {
            if (!displayMap.containsKey(de)) {
                AbstractElementContainer container = ElementContainerFactory
                        .createContainer(de, descriptor, target);
                displayMap.put(de, container);
            }

            displayMap.get(de).draw(target, paintProps, dispProps);
        }

    }

    /**
     * Finds the nearest Gfa element in the active layer by the distance to the
     * center of the text box.
     *
     * @param point
     * @param filter
     * @return the nearest element
     */
    private DrawableElement getNearestGfaByTextBox(Coordinate point,
            ElementFilter filter) {

        DrawableElement nearestElement = null;

        double boxScale = GFA_TEXTBOX_SELECT_SCALE;
        double minDistance = this.getMaxDistToSelect() * boxScale;

        AbstractEditor mapEditor = PgenUtil.getActiveEditor();
        double[] locScreen = mapEditor.translateInverseClick(point);
        Point clickPt = new GeometryFactory()
                .createPoint(new Coordinate(locScreen[0], locScreen[1]));

        Iterator<DrawableElement> iterator = resourceData.getActiveLayer()
                .createDEIterator();
        while (iterator.hasNext()) {
            DrawableElement element = iterator.next();

            if ((!(element instanceof Gfa))
                    || (!filter.accept(element) || !filters.acceptOnce(element))
                    || (!catFilter.accept(element))) {
                continue;
            }

            Gfa gfa = (Gfa) element;

            // calculate distance from the text box
            double[] pt = mapEditor
                    .translateInverseClick(gfa.getGfaTextCoordinate());
            Point ptScreen = new GeometryFactory()
                    .createPoint(new Coordinate(pt[0], pt[1]));
            double distToBox = ptScreen.distance(clickPt);

            if (distToBox < minDistance) {
                minDistance = distToBox;
                nearestElement = element;
            }
        }

        return nearestElement;

    }

    /**
     * Check if the resource is currently editable
     *
     * @return editable
     */
    public boolean isEditable() {
        return getCapability(EditableCapability.class).isEditable();
    }

    public void setEditable(boolean enable) {
        getCapability(EditableCapability.class).setEditable(enable);
        EditableManager.makeEditable(this,
                getCapability(EditableCapability.class).isEditable());
    }

    /**
     * Finds the PGEN resource and fill the context menu.
     */
    @Override
    public void provideContextMenuItems(IMenuManager menuManager, int x,
            int y) {

        ResourcePair pgenPair = null;
        for (ResourcePair rp : descriptor.getResourceList()) {
            if (rp.getResource() == this) {
                pgenPair = rp;
                break;
            }
        }

        if (pgenPair != null && pgenPair.getResource() != null) {
            fillContextMenu(menuManager, pgenPair);
        }
    }

    /**
     * Gets the action list of the selected element and add them into the menu.
     *
     * @param menuManager
     * @param selectedResource
     */
    protected void fillContextMenu(IMenuManager menuManager,
            ResourcePair selectedResource) {

        AbstractDrawableComponent adc = getSelectedDE();
        if (adc != null && !(adc instanceof Jet.JetLine)) { // ignore jet

            String actionsxtra = PgenActionXtra.getActionsXtra(adc);

            List<String> actList = getActionList(
                    this.getSelectedDE().getPgenType(), actionsxtra);

            if (actList != null) {
                for (String act : actList) {
                    menuManager.add(new PgenAction(act.trim()));
                }
            }

            // Add an menu item "Add to Contours" for regular line (not Arc)
            if (getSelectedDE() instanceof Line
                    && getSelectedDE().getParent() instanceof Layer) {
                generateSubMenu(menuManager, (Line) getSelectedDE());
            }

            // Add an menu item "Add to Contours" for Text object
            if (adc instanceof Text && adc.getParent() instanceof Layer) {
                generateLabelSubMenu(menuManager, (Text) adc);
            }

            /*
             * Add two menu items for contour lines: (1) "Remove One Label" (if
             * there is at least one label); (2) "Remove All Labels" (if there
             * is at least one label)
             */
            if (adc instanceof Line && adc.getParent() instanceof ContourLine) {
                generateSubMenuForContourLine(menuManager,
                        (ContourLine) adc.getParent());
            }

            if (adc instanceof Text && adc.getParent() instanceof ContourLine) {
                generateSubMenuForContourLabel(menuManager, (Text) adc);
            }
        }
    }

    /**
     * Creates a submenu "Add To Contour" and its menu items
     *
     * @param menuManager
     *            - context menu manager
     * @param line
     *            - line selected
     */
    private void generateSubMenu(IMenuManager menuManager, final Line line) {
        IMenuManager subMenu = new MenuManager("Add To Contour", null);
        final Layer layer = getResourceData().getActiveLayer();
        Iterator<AbstractDrawableComponent> it = layer.getComponentIterator();

        // Find all contour sets in current layer and put them in the menu
        while (it.hasNext()) {
            final AbstractDrawableComponent adc = it.next();
            if (adc instanceof Contours) {
                final Contours contours = (Contours) adc;

                // Add action to menu items
                subMenu.add(new Action(contours.getParm() + contours.getLevel()
                        + contours.getForecastHour()) {
                    @Override
                    public void run() {
                        // Prompt user to type in label value
                        InputDialog inputDialog = new InputDialog(new Shell(),
                                "Please type in contour label",
                                "Please type in the label of the contour line:",
                                "", null) {
                            @Override
                            protected void configureShell(Shell shell) {
                                super.configureShell(shell);
                                shell.setBounds(
                                        shell.getBounds().x
                                                + LABEL_DIALOG_OFFSET_X,
                                        shell.getBounds().y
                                                + LABEL_DIALOG_OFFSET_Y,
                                        LABEL_DIALOG_WIDTH,
                                        LABEL_DIALOG_HEIGHT);
                            }
                        };

                        if (inputDialog.open() == Window.OK) {
                            addLineToContour(line, contours,
                                    inputDialog.getValue());

                            // clean up
                            layer.remove(line);
                            PgenUtil.setSelectingMode();
                            PgenResource.this.setSelected(
                                    (AbstractDrawableComponent) null);
                        }
                    }
                });
            }
        }
        menuManager.add(subMenu);
    }

    /**
     * Adds a line into a contour set
     *
     * @param line
     *            - line to be added in contour
     * @param contours
     *            - a contour set
     * @param label
     *            - label of the line
     */
    private void addLineToContour(Line line, Contours contours, String label) {

        // Create a default contour label
        Text lbl = new Text(null, "Courier", 14.0f, TextJustification.CENTER,
                line.getPoints().get(0), 0.0, TextRotation.SCREEN_RELATIVE,
                new String[] { label }, FontStyle.REGULAR, Color.RED, 0, 0,
                true, DisplayType.NORMAL, PgenConstant.CATEGORY_TEXT,
                PgenConstant.TYPE_GENERAL_TEXT);

        // set line and label attributes
        if (!contours.getContourLines().isEmpty()) {
            Line cline = contours.getContourLines().get(0).getLine();
            line.setAttr(cline.getAttr());
            line.setColors(cline.getColors());
            line.setPgenType(cline.getPgenType());

            List<Text> labels = contours.getContourLines().get(0).getLabels();
            if (!labels.isEmpty()) {
                lbl = (Text) labels.get(0).copy();
                lbl.setText(new String[] { label });
            }
        }

        // add the line in contours
        ContourLine cline = new ContourLine(line, lbl, 1);
        contours.add(cline);
        // Add the new ContourLine to the contour
        Contours newContour = contours.copy();
        newContour.add(cline);

        List<AbstractDrawableComponent> oldObjects = new ArrayList<>();
        oldObjects.add(line);
        oldObjects.add(contours);

        List<AbstractDrawableComponent> newObjects = new ArrayList<>();
        newObjects.add(newContour);

        // Replace and allow undo/redo.
        PgenResource.this.replaceElements(oldObjects, newObjects);
    }

    /**
     * Creates a submenu "Add To Contour" and its menu items for a Text.
     *
     * @param menuManager
     *            - context menu manager
     * @param Text
     *            - text selected
     */
    private void generateLabelSubMenu(IMenuManager menuManager,
            final Text labelText) {
        IMenuManager subMenu = new MenuManager("Add To Contour", null);
        final Layer layer = getResourceData().getActiveLayer();
        Iterator<AbstractDrawableComponent> it = layer.getComponentIterator();

        // Find all contour sets in current layer and put them in the menu
        while (it.hasNext()) {
            final AbstractDrawableComponent adc = it.next();
            if (adc instanceof Contours) {
                final Contours contours = (Contours) adc;

                // Add action to menu items
                subMenu.add(new Action(contours.getParm() + contours.getLevel()
                        + contours.getForecastHour()) {
                    @Override
                    public void run() {
                        addLabelToContour(labelText, contours);

                        // clean up
                        PgenUtil.setSelectingMode();
                        PgenResource.this
                                .setSelected((AbstractDrawableComponent) null);
                    }
                });
            }
        }

        menuManager.add(subMenu);
    }

    /**
     * Adds a Text into a contour set as a "Label Only" MinMax.
     *
     * @param labelText
     *            - text to be added in contour
     * @param contours
     *            - a contour set
     */
    private void addLabelToContour(Text labelText, Contours contours) {

        // Create a "Label Only" ContourMinMax
        ContourMinmax cmm = new ContourMinmax(labelText.getPosition(), null,
                null, labelText.getString(), false);
        cmm.getLabel().update(labelText);

        // Add the new ContourMinmax to the contour
        Contours newContour = contours.copy();
        newContour.add(cmm);

        List<AbstractDrawableComponent> oldObjects = new ArrayList<>();
        oldObjects.add(labelText);
        oldObjects.add(contours);

        List<AbstractDrawableComponent> newObjects = new ArrayList<>();
        newObjects.add(newContour);

        // Replace and allow undo/redo.
        PgenResource.this.replaceElements(oldObjects, newObjects);

    }

    /*
     * Add two menu items "Remove One Label" and "Remove All Labels" for contour
     * lines if there is at least one label.
     *
     * @param menuManager - context menu manager
     *
     * @param cline - contour line selected
     */
    private void generateSubMenuForContourLine(IMenuManager menuManager,
            final ContourLine cline) {

        final int nlabels = cline.getNumOfLabels();

        if (nlabels > 0) {
            menuManager.add(new Action("Remove One Label") {
                @Override
                public void run() {
                    removeContourLineLabel(false, cline);
                }
            });

            menuManager.add(new Action("Remove All Labels") {
                @Override
                public void run() {
                    removeContourLineLabel(true, cline);
                }
            });
        }

        // Remove "Add New Label" from the menu if the
        // number of labels greater than or equal to 10.
        if (nlabels >= 10) {
            int menuItemIndex = 0;
            List<String> actList = getActionList(
                    this.getSelectedDE().getPgenType(), PgenConstant.ADD_NEW_LABEL);

            if (actList != null) {
                for (String act : actList) {
                    if (PgenConstant.ADD_NEW_LABEL.equalsIgnoreCase(act)) {
                        IContributionItem[] items = menuManager.getItems();
                        IContributionItem item = items[menuItemIndex];
                        menuManager.remove(item);
                        break;
                    }
                    menuItemIndex++;
                }
            }
        }

    }

    private void generateSubMenuForContourLabel(IMenuManager menuManager,
            final Text label) {
        menuManager.add(new Action("Remove Label") {
            @Override
            public void run() {
                removeContourLineLabel(label);
            }
        });
    }

    /*
     * Removes all label or one label from a ContourLine.
     *
     * @param removeAll - flag to remove all labels. If false, remove only one.
     *
     * @param cline - the ContourLine
     */
    private void removeContourLineLabel(boolean removeAll,
            final ContourLine cline) {

        // Make a copy of the ContourLine and update the labels.
        int nlabels = cline.getNumOfLabels() - 1;
        if (removeAll) {
            nlabels = 0;
        }

        ContourLine newLine = cline.copy();
        newLine.updateNumOfLabels(nlabels);

        // Replace old ContourLine with a new ContourLine
        replaceContourLine(cline, newLine);
    }

    private void removeContourLineLabel(Text label) {

        ContourLine cline = (ContourLine) label.getParent();
        Contours myContour = (Contours) cline.getParent();
        // Make a copy of the ContourLine and update the labels.
        int nlabels = cline.getNumOfLabels() - 1;
        cline.setNumOfLabels(nlabels);

        PgenResource.this.removeSelected(label);
        PgenResource.this.removeElement(label);

        AbstractPgenTool thisTool = PgenSession.getInstance().getPgenTool();
        if (thisTool instanceof AbstractPgenDrawingTool) {
            AttrDlg thisDlg = ((AbstractPgenDrawingTool) thisTool).getAttrDlg();
            if (thisDlg instanceof ContoursAttrDlg
                    && thisTool instanceof PgenContoursTool) {
                PgenContoursTool pgenTool = (PgenContoursTool) thisTool;
                ContoursAttrDlg pgenDlg = (ContoursAttrDlg) thisDlg;

                pgenTool.setCurrentContour(myContour);
                pgenDlg.setDrawableElement(myContour);
                pgenDlg.setLabel(cline.getLabelString()[0]);
                pgenDlg.setNumOfLabels(cline.getNumOfLabels());
                pgenDlg.closeAttrEditDialogs();
            }
        }
        PgenResource.this.resetAllElements();
    }

    /*
     * Replaces an existing ContourLine with a new ContourLine in a Contour
     *
     * @param oldLine - contour line to be replaced in contour
     *
     * @param newLine - contour line to replace the old contour line
     */
    private void replaceContourLine(final ContourLine oldLine,
            final ContourLine newLine) {

        /*
         * Make a copy of the Contour without old contour line and add the new
         * ContourLine to the new Contour
         */
        Contours newContour = ((Contours) oldLine.getParent())
                .copyWithExclusion(oldLine);
        newContour.add(newLine);

        // Replace the old Contour and allow undo/redo.
        List<AbstractDrawableComponent> oldObjects = new ArrayList<>();
        oldObjects.add(oldLine.getParent());

        List<AbstractDrawableComponent> newObjects = new ArrayList<>();
        newObjects.add(newContour);

        PgenResource.this.replaceElements(oldObjects, newObjects);

        // Update tool and dialog & ready for select.
        AbstractPgenTool thisTool = PgenSession.getInstance().getPgenTool();
        if (thisTool instanceof AbstractPgenDrawingTool) {
            AttrDlg thisDlg = ((AbstractPgenDrawingTool) thisTool).getAttrDlg();
            if (thisDlg instanceof ContoursAttrDlg
                    && thisTool instanceof PgenContoursTool) {
                PgenContoursTool pgenTool = (PgenContoursTool) thisTool;
                ContoursAttrDlg pgenDlg = (ContoursAttrDlg) thisDlg;

                pgenTool.setCurrentContour(newContour);
                pgenDlg.setDrawableElement(newContour);
                pgenDlg.setLabel(newLine.getLabelString()[0]);
                pgenDlg.setNumOfLabels(newLine.getNumOfLabels());

                PgenResource.this.setSelected(newLine.getLine());
            }
        }
    }

    /**
     * Gets the action list for the specified object. If there is no action for
     * the object, then gets the actions for its class (pgen category). The
     * actions are defined in the plugin.xml.
     *
     * @param objName
     * @return
     */
    private List<String> getActionList(String objName, String actionsxtra) {
        HashMap<String, IConfigurationElement> itemMap = PgenSession
                .getInstance().getPgenPalette().getItemMap();

        IConfigurationElement ice = itemMap.get(objName);
        if (ice != null) {
            // get actions for the object
            String actList = ice.getAttribute(PgenConstant.ACTIONS);

            // if no actions for the object, get actions for the class/category
            if (actList == null) {
                String category = ice.getAttribute(PgenConstant.CLASSNAME);
                if (category != null) {
                    IConfigurationElement cat = itemMap.get(category);
                    if (cat != null) {
                        actList = cat.getAttribute(PgenConstant.ACTIONS);
                    }
                }
            }

            // Add conditional context menu choices to the context menu
            if (actionsxtra != null) {
                if (actList != null) {
                    actList = actList + "," + actionsxtra;
                } else {
                    actList = actionsxtra;
                }
            }

            if (actList != null && !actList.isEmpty()) {
                return Arrays.asList(actList
                        .split(PgenConstant.PLUGINXML_ATTRIBUTE_DELIMETER));
            }
        }

        return Collections.emptyList();
    }

    /**
     * Get a list of DrawableElements in current PGEN activity that need to be
     * displayed.
     */
    public List<DrawableElement> getActiveDrawableElements() {

        List<DrawableElement> des = new ArrayList<>();

        for (Layer layer : resourceData.getActiveProduct().getLayers()) {
            if (layer != null && layer.isOnOff()) {

                Iterator<DrawableElement> iterator = layer.createDEIterator();

                while (iterator.hasNext()) {
                    des.add(iterator.next());
                }
            }
        }

        return des;
    }

    /*
     * Check if a pointed arrow is part of a CCFP sigmet
     */
    protected boolean isCCFPArrow(DrawableElement de) {
        boolean isccfparrow = false;

        if (de instanceof ILine && (de.getParent() != null)
                && de.getParent().getParent() != null) {
            if (de.getParent().getParent().getPgenType() != null
                    && de.getParent().getParent().getPgenType()
                            .equalsIgnoreCase(PgenConstant.TYPE_CCFP_SIGMET)) {
                isccfparrow = true;
            }
        }

        return isccfparrow;

    }

    /*
     * Check if a Text is part of a CCFP sigmet
     */
    private boolean isCCFPText(DrawableElement de) {
        boolean isccfptext = false;

        if (de instanceof IText && (de.getParent() != null)
                && de.getParent().getParent() != null) {
            if (de.getParent().getParent().getPgenType() != null
                    && de.getParent().getParent().getPgenType()
                            .equalsIgnoreCase(PgenConstant.TYPE_CCFP_SIGMET)) {
                isccfptext = true;
            }
        }

        return isccfptext;
    }

    /**
     * @return the needsDisplay
     */
    public boolean isNeedsDisplay() {
        return needsDisplay;
    }

    /**
     * @param needsDisplay
     *            the needsDisplay to set
     */
    public void setNeedsDisplay(boolean needsDisplay) {
        this.needsDisplay = needsDisplay;
    }

    /**
     * Adds a product into the PGEN drawing layer.
     *
     * @return
     */
    public boolean removeEmptyDefaultProduct() {
        return resourceData.removeEmptyDefaultProduct();
    }

}
