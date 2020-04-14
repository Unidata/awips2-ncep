package gov.noaa.nws.ncep.viz.ui.locator.resource;

import java.awt.geom.Rectangle2D;

import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.IMapDescriptor;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.IInputHandler;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.viz.ui.EditorUtil;
import com.raytheon.viz.ui.editor.AbstractEditor;
import com.raytheon.viz.ui.input.InputAdapter;

import gov.noaa.nws.ncep.viz.resources.INatlCntrsResource;

import org.locationtech.jts.geom.Coordinate;

/**
 * This class displays the Locator .
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer   Description
 * ------------- -------- ---------- ------------------------------------------
 * Dec 10, 2011  561      Greg Hull  make this a real nc resource
 * Dec 14, 1212  903      Greg Hull  fontSize attribute, and color
 * Apr 29, 2019  62919    K Sunil    changes to make Locator tool work on D2D.
 * Jun 11, 2019  63680    tjensen    Move registerMouseHandler call to init
 * Jan 21  2020  73596    smanoj     Fix lat/lon in side panes for locator tool
 * 
 * </pre>
 *
 * @author ghull
 *
 */
public class LocatorResource
        extends AbstractVizResource<LocatorResourceData, IMapDescriptor>
        implements INatlCntrsResource {

    private final LocatorResourceData locRscData;

    protected Coordinate currCoor = null;

    private IFont font = null;

    private final LocatorDataSource locDataSources[] = new LocatorDataSource[LocatorResourceData.MAX_NUM_SOURCES];

    private final LocatorDisplayAttributes dispAttrs[] = new LocatorDisplayAttributes[LocatorResourceData.MAX_NUM_SOURCES];

    private IGraphicsTarget currTarget = null;

    private PaintProperties currPaintProps = null;

    protected AbstractEditor editor;

    protected IInputHandler handler;

    public LocatorResource(LocatorResourceData resourceData,
            LoadProperties loadProperties) {
        super(resourceData, loadProperties);
        locRscData = resourceData;
        handler = new LocatorMouseAdapter();
    }

    @Override
    protected void initInternal(IGraphicsTarget target) throws VizException {

        LocatorDataSourceMngr dataSourceMngr = LocatorDataSourceMngr
                .getInstance();

        if (dataSourceMngr == null) {
            throw new VizException("Error creating Locator Data Sources");
        }

        getDisplayAttributes();

        // we are in the init thread but we could always spawn a new thread for
        // each
        for (int p = 0; p < LocatorResourceData.MAX_NUM_SOURCES; p++) {
            if (dispAttrs[p] != null) {
                LocatorDataSource locDataSrc = LocatorDataSourceMngr
                        .getInstance()
                        .getLocatorDataSource(dispAttrs[p].getLocatorSource());
                if (locDataSrc != null) {
                    locDataSrc.loadSourceData();
                }

                locDataSources[p] = locDataSrc;
            }
        }

        // Wait until initialization to get the active editor to ensure editor
        // not null
        editor = ((AbstractEditor) EditorUtil.getActiveEditor());
        if (editor != null) {
            editor.registerMouseHandler(handler);
        } else {
            statusHandler.error(
                    "Unable to determine editor to register Locator resource.");
        }
    }

    @Override
    protected void paintInternal(IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {
        currTarget = target;
        currPaintProps = paintProps;

        if (font == null) {
            font = target.initializeFont(locRscData.getFontName(),
                    1.0f * locRscData.getFontSize(), null);
            if (font == null) {
                font = target.initializeFont(
                        target.getDefaultFont().getFontName(),
                        1.0f * locRscData.getFontSize(), null);
            }
        }
        drawLocatorInfo();
    }

    private void drawLocatorInfo() throws VizException {

        if (currCoor == null) {
            return;
        }
        currTarget.clearClippingPlane();

        double r = currPaintProps.getView().getExtent().getWidth()
                / currPaintProps.getCanvasBounds().width;
        double y0 = currPaintProps.getView().getExtent().getMaxY() - (7 * r);
        double x0 = currPaintProps.getView().getExtent().getMinX() + (18 * r);

        for (int pos = 0; pos < LocatorResourceData.MAX_NUM_SOURCES; pos++) {

            double maxHeight = 0.0;

            String locLabelStr = "";

            if (locDataSources[pos] == null) {
                continue;
            }

            locLabelStr = locDataSources[pos].getLocatorString(currCoor,
                    dispAttrs[pos]);

            Rectangle2D textBounds = currTarget.getStringBounds(font,
                    locLabelStr);

            if (textBounds.getHeight() > maxHeight) {
                if (locLabelStr != null && (!locLabelStr.isEmpty())) {
                    maxHeight = currTarget.getStringBounds(font, "J_/")
                            .getHeight();
                }
            }

            currTarget.drawString(font, locLabelStr, x0, y0, 0,
                    IGraphicsTarget.TextStyle.BLANKED, locRscData.getColor(),
                    HorizontalAlignment.LEFT, 0.0);
            y0 -= (maxHeight * r);
        }

        currTarget.setupClippingPlane(currPaintProps.getClippingPane());
    }

    private void getDisplayAttributes() throws VizException {
        // create a dataSource for each enabled LocatorDataSource
        for (int p = 0; p < LocatorResourceData.MAX_NUM_SOURCES; p++) {

            dispAttrs[p] = locRscData.getDisplayAttributesForPosition(p);

            if (dispAttrs[p] == null) {
                locDataSources[p] = null;
            } else {
                if (dispAttrs[p].getLocatorSource().isEmpty()) {
                    locDataSources[p] = null;
                } else {
                    locDataSources[p] = LocatorDataSourceMngr.getInstance()
                            .getLocatorDataSource(
                                    dispAttrs[p].getLocatorSource());
                    if (locDataSources[p] != null
                            && !locDataSources[p].isDataLoaded()) {
                        locDataSources[p].loadSourceData();
                    }
                }
            }
        }
    }

    @Override
    public void resourceAttrsModified() {
        try {
            getDisplayAttributes();

            if (font != null) {
                font.dispose();
                font = null;
            }

        } catch (VizException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    protected void disposeInternal() {
        if (font != null) {
            font.dispose();
            font = null;
        }

        if (editor != null) {
            editor.unregisterMouseHandler(handler);
        }
    }

    protected class LocatorMouseAdapter extends InputAdapter {

        @Override
        public boolean handleMouseMove(int x, int y) {

            IDisplayPaneContainer container = getResourceContainer();
            if (container == null) {
                return false;
            }

            currCoor = container.translateClick(x, y);
            if (currTarget != null && currPaintProps != null) {
                issueRefresh();
            }
            return false;
        }
    }

}
