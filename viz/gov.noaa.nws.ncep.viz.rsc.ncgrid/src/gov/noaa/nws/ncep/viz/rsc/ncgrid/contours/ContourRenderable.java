package gov.noaa.nws.ncep.viz.rsc.ncgrid.contours;

import gov.noaa.nws.ncep.gempak.parameters.line.LineDataStringParser;
import gov.noaa.nws.ncep.viz.common.ui.color.GempakColor;
import gov.noaa.nws.ncep.viz.resources.colorBar.ColorBarResource;
import gov.noaa.nws.ncep.viz.resources.colorBar.ColorBarResourceData;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.FloatGridData;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.contours.ContourSupport.ContourGroup;
import gov.noaa.nws.ncep.viz.rsc.ncgrid.rsc.NcgridResource;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;

import org.eclipse.swt.graphics.RGB;
import org.geotools.coverage.grid.GeneralGridGeometry;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;

import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.LineStyle;
import com.raytheon.uf.viz.core.drawables.IRenderable;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.IMapDescriptor;
import com.raytheon.viz.ui.editor.AbstractEditor;

/**
 * Generalized contour renderable. Also used for Fill images and streamlines.
 * 
 * May be embedded in other renderable displays or form the basis of contour
 * resources
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------------------------------------------
 * Jul 10, 2008 #1233	    chammack    Initial creation
 * Mar 01, 2010 #164        M. Li       Applied to NC Perspective
 * Mar 08, 2011             M. Li       abstract class -> general class 
 * Apr 05, 2011             M. Li       increase threading retries from 10 to 100
 * Nov,02, 2011             X. Guo      Added HILO relative
 * Feb,15, 2012             X. Guo      Cached contour information
 * Mar,01, 2012             X. Guo      Handle five zoom levels
 * Mar,13, 2012             X. Guo      Added createContours()
 * Mar,15, 2012             X. Guo      Set synchronized block in ContoutSupport
 * Aug 19, 2013  #743       S. Gurung   Added code to display the colorbar for gridded fills (from Archana's branch) 
 * Sep 11, 2013  #1036      S. Gurung   Added TEXT attribute related code changes (for contour labels)
 * Jul 17, 2015  R6916      B. Yin/rkean Changes for Contour fill images
 * Mar 25, 2016  R16999     J. Beck/R. Kean Changed logic to parse GRID Type: attribute (C,F)
 * Apr 21, 2016  R17741     S. Gilbert  Change to use one ContourGroup instead of an array
 * 
 * </pre>
 * 
 * @author chammack
 * @version 1.0
 */

public class ContourRenderable implements IRenderable {

    private ContourGroup contourGroup;

    private IMapDescriptor descriptor;

    private LineStyle lineStyle;

    private RGB color;

    private int outlineWidth;

    private static final int NUMBER_CONTOURING_LEVELS = 5;

    private int numberZoomLevels = 1;

    private boolean reproj;

    protected ContourAttributes contourAttributes;

    protected FloatGridData data;

    protected GeneralGridGeometry gridGeometry;

    private NcgridResource resource = null;

    protected GridRelativeHiLoDisplay gridRelativeHiLoDisplay;

    private String name;

    private String cint;

    private double defaultZoomLevel;

    private double zoomLevelInterval;

    private ColorBarResource cBarResource = null;

    /**
     * Renderable to create and display contours, fill, and/or streamlines
     * 
     * @param descriptor
     */
    public ContourRenderable(IMapDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public ContourRenderable(FloatGridData data, IMapDescriptor descriptor,
            GeneralGridGeometry gridGeometry,
            ContourAttributes contourAttributes, String fullName) {
        this.descriptor = descriptor;
        this.data = data;
        this.gridGeometry = gridGeometry;
        this.contourAttributes = contourAttributes;
        this.name = fullName;
        this.defaultZoomLevel = 0.0;
        this.zoomLevelInterval = 0.0;
    }

    /*
     * (non-Javadoc) .
     * 
     * @see
     * com.raytheon.viz.core.drawables.IRenderable#paint(com.raytheon.viz.core
     * .IGraphicsTarget, com.raytheon.viz.core.drawables.PaintProperties)
     */
    @Override
    public void paint(IGraphicsTarget target, PaintProperties paintProps)
            throws VizException {

        initContourGroup(paintProps);

        if (contourGroup != null) {

            int level = calculateZoomLevel(paintProps);
            int contourLevel = numberZoomLevels - level - 1;

            // Create contours/fills/streamlines if:
            // 1. Contours was never ran before -or-
            // 2. The Map Projection/area was changed
            // 3. The Zoom level has changed
            if (contourGroup == null || getMapProject()
                    || contourGroup.zoomLevel != contourLevel) {
                createContours(target, paintProps);
            }

            LineStyle posLineStyle = null;
            LineStyle negLineStyle = null;
            if (this.lineStyle == null) {
                posLineStyle = LineStyle.SOLID;
                negLineStyle = LineStyle.DASHED_LARGE;
            } else {
                posLineStyle = this.lineStyle;
                negLineStyle = this.lineStyle;
            }

            if (contourGroup.colorFillImage != null) {
                contourGroup.colorFillImage.paint(target, paintProps);
            }
            if (contourGroup.streamlines != null)
                target.drawWireframeShape(contourGroup.streamlines, this.color,
                        this.outlineWidth, posLineStyle,
                        contourGroup.labelParms.font);
            if (contourGroup.contours != null)
                target.drawWireframeShape(contourGroup.contours, this.color,
                        this.outlineWidth, negLineStyle,
                        contourGroup.labelParms.font);
            if (contourGroup.labels != null) {
                target.drawStrings(contourGroup.labels);
            }

        }

        if (cBarResource != null)
            cBarResource.paint(target, paintProps);

    }

    public void createContours(IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {

        initContourGroup(paintProps);

        double density = 1.0;

        int level = calculateZoomLevel(paintProps);
        int contourLevel = numberZoomLevels - level - 1;

        MathTransform mathTransformFromGrid = gridGeometry
                .getGridToCRS(PixelInCell.CELL_CORNER);

        // If required data unavailable, quit now
        if (mathTransformFromGrid == null || data == null
                || gridGeometry == null) {
            return;
        }

        ContourGroup tempCG = null;

        float pixelDensity = (float) (paintProps.getCanvasBounds().width / paintProps
                .getView().getExtent().getWidth());

        ContourSupport cntrSp = new ContourSupport(resource, data,
                contourLevel, paintProps.getView().getExtent(), density,
                mathTransformFromGrid, gridGeometry,
                descriptor.getGridGeometry(), target, descriptor,
                contourAttributes, name, pixelDensity, contourGroup);

        cntrSp.createContours();
        tempCG = cntrSp.getContours();

        if (tempCG != null) {
            // Dispose old wireframe and shaded shapes
            disposeContourGroup(contourGroup);
            contourGroup = tempCG;

            if (contourGroup.colorBarForGriddedFill != null) {

                AbstractEditor editor = NcDisplayMngr
                        .getActiveNatlCntrsEditor();

                IRenderableDisplay disp = editor.getActiveDisplayPane()
                        .getRenderableDisplay();

                if (disp != null) {

                    ResourcePair rp = ResourcePair
                            .constructSystemResourcePair(new ColorBarResourceData(
                                    contourGroup.colorBarForGriddedFill));
                    cBarResource = (ColorBarResource) rp.getResourceData()
                            .construct(rp.getLoadProperties(),
                                    disp.getDescriptor());
                    if (cBarResource != null) {
                        cBarResource
                                .setColorBar(contourGroup.colorBarForGriddedFill);
                        cBarResource.init(target);

                    }
                }
            } else {
                cBarResource = null;
            }
        }
    }

    private void initContourGroup(PaintProperties paintProps) {
        LineDataStringParser lineAttr = new LineDataStringParser(
                contourAttributes.getLine());
        this.color = GempakColor.convertToRGB(lineAttr
                .getInstanceOfLineBuilder().getLineColorsList().get(0));
        this.lineStyle = lineAttr.getInstanceOfLineBuilder().getLineStyleList()
                .get(0);
        this.outlineWidth = lineAttr.getInstanceOfLineBuilder()
                .getLineWidthList().get(0);

        if (contourGroup == null) {
            this.defaultZoomLevel = paintProps.getView().getExtent().getWidth()
                    / paintProps.getCanvasBounds().width;
            if (contourAttributes.getCint() != null) {
                String[] cintArray = contourAttributes.getCint().trim()
                        .split(">");
                numberZoomLevels = cintArray.length;
                this.cint = contourAttributes.getCint();
            }

            if (numberZoomLevels > NUMBER_CONTOURING_LEVELS)
                numberZoomLevels = NUMBER_CONTOURING_LEVELS;

            if (numberZoomLevels > 1)
                this.zoomLevelInterval = this.defaultZoomLevel
                        / (numberZoomLevels - 1);
        } else {
            if (this.defaultZoomLevel == 0.0) {
                this.defaultZoomLevel = paintProps.getView().getExtent()
                        .getWidth()
                        / paintProps.getCanvasBounds().width;
                if (numberZoomLevels > 1)
                    this.zoomLevelInterval = this.defaultZoomLevel
                            / (numberZoomLevels - 1);
            }
        }
    }

    private int calculateZoomLevel(PaintProperties paintProps) {

        int i = numberZoomLevels - 1;
        double rangeHi, rangeLo, zoomlvl;
        zoomlvl = paintProps.getView().getExtent().getWidth()
                / paintProps.getCanvasBounds().width;

        while (i >= 0) {
            rangeHi = Double.MAX_VALUE;
            rangeLo = 0.0;
            if (numberZoomLevels > 1) {
                if (i == numberZoomLevels - 1) {
                    rangeLo = zoomLevelInterval / 2 + (i - 1)
                            * zoomLevelInterval;
                } else if (i == 0) {
                    rangeHi = zoomLevelInterval / 2;
                } else {
                    rangeHi = zoomLevelInterval / 2 + i * zoomLevelInterval;
                    rangeLo = zoomLevelInterval / 2 + (i - 1)
                            * zoomLevelInterval;
                }
            }

            if (rangeHi >= zoomlvl && zoomlvl > rangeLo) {
                return i;
            }
            i--;
        }
        return i;
    }

    /**
     * Dispose the ContourGroup in this renderable
     */
    public void dispose() {
        disposeInternal();
    }

    private void disposeInternal() {
        if (contourGroup != null) {
            disposeContourGroup(contourGroup);
        }
    }

    /**
     * Dispose the contour group
     */
    private void disposeContourGroup(ContourGroup contourGp) {
        if (contourGp != null) {
            contourGp.dispose();
        }
    }

    public ContourAttributes getContourAttributes() {
        return contourAttributes;
    }

    public void setContourAttributes(ContourAttributes contourAttributes) {
        this.contourAttributes = contourAttributes;
    }

    public void updatedContourRenderable() {

        if (this.cint == null) {
            if (contourAttributes.getCint() != null) {
                String[] cintArray = contourAttributes.getCint().trim()
                        .split(">");
                numberZoomLevels = cintArray.length;
                this.cint = contourAttributes.getCint();
            }

            if (numberZoomLevels > NUMBER_CONTOURING_LEVELS)
                numberZoomLevels = NUMBER_CONTOURING_LEVELS;

            disposeInternal();

            if (numberZoomLevels > 1)
                this.zoomLevelInterval = this.defaultZoomLevel
                        / (numberZoomLevels - 1);
        } else {
            if (contourAttributes.getCint() == null) {
                numberZoomLevels = 1;
                this.cint = null;
                disposeInternal();

            } else {
                if (!this.cint.equalsIgnoreCase(contourAttributes.getCint())) {
                    disposeInternal();
                    String[] cintArray = contourAttributes.getCint().trim()
                            .split(">");
                    numberZoomLevels = cintArray.length;
                    this.cint = contourAttributes.getCint();
                    if (numberZoomLevels > NUMBER_CONTOURING_LEVELS)
                        numberZoomLevels = NUMBER_CONTOURING_LEVELS;

                    if (numberZoomLevels > 1)
                        this.zoomLevelInterval = this.defaultZoomLevel
                                / (numberZoomLevels - 1);
                }
            }
        }

    }

    public FloatGridData getData() {
        return data;
    }

    public void setData(FloatGridData data) {
        this.data = data;
    }

    public GeneralGridGeometry getGridGeometry() {
        return gridGeometry;
    }

    public void setGridGeometry(GeneralGridGeometry gridGeometry) {
        this.gridGeometry = gridGeometry;
    }

    public GridRelativeHiLoDisplay getGridRelativeHiLo() {
        return gridRelativeHiLoDisplay;
    }

    public void setGridRelativeHiLo(
            GridRelativeHiLoDisplay gridRelativeHiLoDisplay) {
        this.gridRelativeHiLoDisplay = gridRelativeHiLoDisplay;
    }

    public void setMapProject(boolean proj) {
        this.reproj = proj;
    }

    private boolean getMapProject() {
        return this.reproj;
    }

    public void setIMapDescriptor(IMapDescriptor descriptor) {
        this.descriptor = descriptor;
        this.defaultZoomLevel = 0.0;
    }

    public boolean isMatch(ContourAttributes attr) {
        boolean match = false;

        if (this.contourAttributes.getGlevel().equalsIgnoreCase(
                attr.getGlevel())
                && this.contourAttributes.getGvcord().equalsIgnoreCase(
                        attr.getGvcord())
                && this.contourAttributes.getScale().equalsIgnoreCase(
                        attr.getScale())
                && this.contourAttributes.getGdpfun().equalsIgnoreCase(
                        attr.getGdpfun())) {
            match = true;
        }
        return match;
    }

    public NcgridResource getResource() {
        return resource;
    }

    public void setResource(NcgridResource resource) {
        this.resource = resource;
    }
}
