/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package gov.noaa.nws.ncep.ui.nsharp.display.map;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.locationtech.jts.geom.Coordinate;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.PixelExtent;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.IMapDescriptor;
import com.raytheon.uf.viz.core.map.MapDescriptor;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.IInputHandler;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.ResourceList.RemoveListener;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.EditableCapability;
import com.raytheon.viz.ui.EditorUtil;
import com.raytheon.viz.ui.editor.AbstractEditor;
import com.raytheon.viz.ui.input.EditableManager;

import gov.noaa.nws.ncep.ui.nsharp.NsharpConstants;
import gov.noaa.nws.ncep.ui.nsharp.NsharpStationInfo;
import gov.noaa.nws.ncep.ui.nsharp.view.NsharpPaletteWindow;
import gov.noaa.nws.ncep.ui.pgen.display.DisplayElementFactory;
import gov.noaa.nws.ncep.ui.pgen.display.IDisplayable;
import gov.noaa.nws.ncep.ui.pgen.elements.SymbolLocationSet;

/**
 *
 * Provides a base implementation for NSHARP Sounding Load Dialog.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Mar 23, 2020  73571    smanoj   Initial creation
 * Jun 22, 2020  79556    smanoj   Fixing some errors.
 * Nov 20, 2020  84061    smanoj   NSHARP artifacts(green diamonds) to plot
 *                                 in the D2D "Map" Editor.
 * Dec 07, 2020  85537    smanoj   Fix issue with disable NSHARP resource
 *                                 by unchecking the "Editable" check box
 *                                 in the resource menu.
 * 
 * </pre>
 *
 * @author smanoj
 */
public abstract class AbstractNsharpMapResource extends
        AbstractVizResource<AbstractNsharpMapResourceData, MapDescriptor>
        implements RemoveListener {

    private static final String MAP_EDITOR_ID = "com.raytheon.viz.ui.glmap.GLMapEditor";

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(AbstractNsharpMapResource.class);

    protected AbstractNsharpMapResource mapRsc = null;

    protected AbstractNsharpMapResourceData mapRscData = null;

    protected AbstractEditor mapEditor = null;

    protected IInputHandler mouseHandler = null;

    protected Cursor waitCursor = null;

    protected Control cursorControl;

    protected boolean mouseHandlerRegistered = false;

    /** The set of symbols with similar attributes across many locations */
    protected SymbolLocationSet symbolSet = null;

    protected SymbolLocationSet symbolToMark = null;

    protected List<NsharpStationInfo> points = new ArrayList<NsharpStationInfo>();

    protected List<NsharpStationInfo> pickedPoint = new ArrayList<NsharpStationInfo>();

    private static final Map<AbstractEditor, AbstractNsharpMapResource> mapRrcs = new HashMap<>();

    protected AbstractNsharpMapResource(
            AbstractNsharpMapResourceData resourceData,
            LoadProperties loadProperties) {
        super(resourceData, loadProperties);
        // set the editable capability
        getCapability(EditableCapability.class).setEditable(true);
        this.mapRscData = resourceData;
    }

    public void createMapEditor() {
        // Find the correct Map Editor to load NSHARP artifacts(green diamonds)
        if (EditorUtil.findEditor(MAP_EDITOR_ID) != null) {
            mapEditor = ((AbstractEditor) EditorUtil.findEditor(MAP_EDITOR_ID));
            mapRrcs.put(mapEditor, this);
        }

    }

    public AbstractEditor getMapEditor() {
        return mapEditor;
    }

    public static AbstractNsharpMapResource getMapResource(
            AbstractEditor editor) {
        return mapRrcs.get(editor);
    }

    public void bringMapEditorToTop() {
        try {
            if (mapEditor != null && PlatformUI.getWorkbench() != null
                    && PlatformUI.getWorkbench()
                            .getActiveWorkbenchWindow() != null
                    && PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                            .getActivePage() != null) {
                PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                        .getActivePage().bringToTop(mapEditor);
                mapEditor.refresh();
            }
        } catch (Exception e) {
            statusHandler.handle(Priority.ERROR,
                    "Could not bring map editor to top:", e.getMessage());
        }
    }

    public void setPickedPoint(NsharpStationInfo point) {
        this.pickedPoint.add(point);
    }

    public List<NsharpStationInfo> getPoints() {
        return points;
    }

    public void setPoints(List<NsharpStationInfo> points) {
        if (points == null) {
            if ((this.pickedPoint != null) && (!this.pickedPoint.isEmpty())) {
                this.pickedPoint.clear();
            }
            if ((this.points != null) && (!this.points.isEmpty())) {
                this.points.clear();
            }
            symbolToMark = null;
            symbolSet = null;
        } else {
            this.points = points;
        }
    }

    public void addPoint(NsharpStationInfo point) {
        points.add(point);
    }

    public void startWaitCursor() {
        waitCursor = new Cursor(Display.getCurrent(), SWT.CURSOR_WAIT);
        cursorControl = Display.getCurrent().getCursorControl();
        if (cursorControl != null && waitCursor != null) {
            cursorControl.setCursor(waitCursor);
        }
    }

    public void stopWaitCursor() {
        if (cursorControl != null && waitCursor != null) {
            cursorControl.setCursor(null);
        }
        if (waitCursor != null) {
            waitCursor.dispose();
            waitCursor = null;
        }
    }

    public void registerMouseHandler() {
        if (mouseHandlerRegistered) {
            return;
        }

        mouseHandler = getMouseHandler();
        if (mapEditor != null && mouseHandler != null) {
            mapEditor.registerMouseHandler((IInputHandler) mouseHandler);
            mouseHandlerRegistered = true;
        }
    }

    public void unregisterMouseHandler() {
        if (!mouseHandlerRegistered) {
            return;
        }
        mouseHandler = getMouseHandler();
        if (mapEditor != null && mouseHandler != null) {
            mapEditor.unregisterMouseHandler((IInputHandler) mouseHandler);
            mouseHandlerRegistered = false;
        }
    }

    /**
     * Called when resource is disposed
     * 
     * @see com.raytheon.viz.core.rsc.IVizResource#dispose()
     */
    @Override
    public void disposeInternal() {
        if (mapEditor != null) {
            mapEditor.unregisterMouseHandler(mouseHandler);
            mouseHandler = null;
            mapEditor = null;
        }

        pickedPoint = null;
        points = null;
        symbolSet = null;
        symbolToMark = null;
        mapRscData = null;

        if (waitCursor != null) {
            waitCursor.dispose();
        }

        waitCursor = null;
        mouseHandlerRegistered = false;
    }

    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        if (descriptor == null) {
            return null;
        }
        return descriptor.getCRS();
    }

    @Override
    public String getName() {
        return "NSHARP Resource";
    }

    @Override
    public void initInternal(IGraphicsTarget target) throws VizException {

        // make the nsharp map resource editable
        EditableManager.makeEditable(this,
                getCapability(EditableCapability.class).isEditable());
    }

    public boolean isApplicable(PixelExtent extent) {
        return true;
    }

    public void generateSymbolForDrawing() {
        String type;
        float lineWidth = mapRscData.getMarkerWidth();
        Boolean clear = false;

        String category = new String("Marker");
        double sizeScale = mapRscData.getMarkerSize();

        if (points.isEmpty() == true) {
            symbolSet = null;
        } else {
            // SymbolLocationSet constructor requires a positive-length array of
            // Coordinate
            Coordinate[] locations = new Coordinate[points.size()];
            Color[] colors = new Color[] {
                    new Color(NsharpConstants.color_green.red,
                            NsharpConstants.color_green.green,
                            NsharpConstants.color_green.blue) };
            int i = 0;
            for (NsharpStationInfo p : points) {
                double lon, lat;
                lon = p.getLongitude();
                lat = p.getLatitude();
                locations[i++] = new Coordinate(lon, lat);
            }
            type = mapRscData.getMarkerType().toString();
            symbolSet = new SymbolLocationSet(null, colors, lineWidth,
                    sizeScale, clear, locations, category, type);

        }

        symbolToMark = null;
        if (isEditable()) {
            // generate symbol for picked stn to mark X
            if (pickedPoint != null && pickedPoint.size() > 0) {
                Coordinate[] locations = new Coordinate[pickedPoint.size()];
                int i = 0;
                for (NsharpStationInfo p : pickedPoint) {
                    double lon, lat;
                    lon = p.getLongitude();
                    lat = p.getLatitude();
                    locations[i++] = new Coordinate(lon, lat);
                }
                type = mapRscData.getStnMarkerType().toString();
                Color[] colors = new Color[] { new Color(NsharpConstants.color_red.red, NsharpConstants.color_red.green,
                        NsharpConstants.color_red.blue) };
                symbolToMark = new SymbolLocationSet(null, colors, lineWidth, sizeScale * 2, clear, locations, category,
                        type);
            } else {
                symbolToMark = null;
            }
        }
    }

    @Override
    public void paintInternal(IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {

        generateSymbolForDrawing();
        DisplayElementFactory df = new DisplayElementFactory(target,
                this.descriptor);
        if (symbolSet != null) {
            ArrayList<IDisplayable> elements = df
                    .createDisplayElements(symbolSet, paintProps);
            for (IDisplayable each : elements) {
                try {
                    each.draw(target, paintProps);
                    each.dispose();
                } catch (Exception e) {
                    statusHandler.handle(Priority.ERROR, "Error paint",
                            e.getMessage());
                }
            }
        }
        if (symbolToMark != null) {
            ArrayList<IDisplayable> elements = df
                    .createDisplayElements(symbolToMark, paintProps);
            for (IDisplayable each : elements) {
                try {
                    each.draw(target, paintProps);
                    each.dispose();
                } catch (Exception e) {
                    statusHandler.handle(Priority.ERROR, "Error Drawing",
                            e.getMessage());
                }
            }
        }
    }

    public boolean isProjectable(CoordinateReferenceSystem mapData) {
        return true;
    }

    @Override
    public void project(CoordinateReferenceSystem mapData) throws VizException {

    }

    @Override
    public boolean okToUnload() {
        /*
         * DisAllow unloading of Resource
         */
        return false;
    }

    @Override
    public void notifyRemove(ResourcePair rp) throws VizException {
    }

    @Override
    public void propertiesChanged(ResourceProperties updatedProps) {
        if (updatedProps.isVisible()) {
            reopenTextView();
        } else {
            hideTextView();
        }
    }

    public void hideTextView() {
        IWorkbenchPage wpage = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage();

        IViewPart vpart = wpage.findView("gov.noaa.nws.ncep.ui.nsharp");
        if (wpage.isPartVisible(vpart)) {
            NsharpPaletteWindow paletteWin = NsharpPaletteWindow.getInstance();
            if (paletteWin != null) {
                paletteWin.setEditorVisible(false);
                wpage.hideView(vpart);
            }
        }
    }

    public void reopenTextView() {
        IWorkbenchPage wpage = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage();

        IViewPart vpart = wpage.findView("gov.noaa.nws.ncep.ui.nsharp");
        if (!wpage.isPartVisible(vpart)) {
            NsharpPaletteWindow paletteWin = NsharpPaletteWindow.getInstance();
            if (paletteWin != null) {
                paletteWin.setEditorVisible(true);
                try {
                    vpart = wpage.showView("gov.noaa.nws.ncep.ui.nsharp");
                } catch (Exception e) {
                    statusHandler.handle(Priority.ERROR,
                            "Error opening NSHARP window", e.getMessage());
                }
            }
        }
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

    public AbstractNsharpMapResource getOrCreateNsharpMapResource() {
        if (mapRsc == null) {
            if (mapEditor == null) {
                createMapEditor();
            }

            if (mapEditor != null) {
                IMapDescriptor desc = (IMapDescriptor) mapEditor
                        .getActiveDisplayPane().getRenderableDisplay()
                        .getDescriptor();
                try {
                    if (mapRscData == null) {
                        mapRscData = getNewMapResourceData();
                    }
                    mapRsc = (AbstractNsharpMapResource) mapRscData
                            .construct(new LoadProperties(), desc);
                    desc.getResourceList().add(mapRsc);
                    mapRsc.init(mapEditor.getActiveDisplayPane().getTarget());

                    // register mouse handler
                    mouseHandler = getMouseHandler();
                    mapEditor
                            .registerMouseHandler((IInputHandler) mouseHandler);
                } catch (Exception e) {
                    statusHandler.handle(Priority.ERROR,
                            "Error registering mouse handler", e.getMessage());
                }
            }
        }
        return mapRsc;
    }

    public abstract IInputHandler getMouseHandler();

    public abstract AbstractNsharpMapResourceData getNewMapResourceData();

}
