/*****************************************************************************************
 * COPYRIGHT (c), 2006-2008, RAYTHEON COMPANY
 * ALL RIGHTS RESERVED, An Unpublished Work 
 *
 * RAYTHEON PROPRIETARY
 * If the end user is not the U.S. Government or any agency thereof, use
 * or disclosure of data contained in this source code file is subject to
 * the proprietary restrictions set forth in the Master Rights File.
 *
 * U.S. GOVERNMENT PURPOSE RIGHTS NOTICE
 * If the end user is the U.S. Government or any agency thereof, this source
 * code is provided to the U.S. Government with Government Purpose Rights.
 * Use or disclosure of data contained in this source code file is subject to
 * the "Government Purpose Rights" restriction in the Master Rights File.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * Use or disclosure of data contained in this source code file is subject to
 * the export restrictions set forth in the Master Rights File.
 ******************************************************************************************/
package gov.noaa.nws.ncep.viz.rsc.ncgrid.contours;

import gov.noaa.nws.ncep.common.tools.IDecoderConstantsN;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.swt.graphics.RGB;

import com.raytheon.uf.common.geospatial.ISpatialObject;
import com.raytheon.uf.common.geospatial.MapUtil;
import com.raytheon.uf.viz.core.DrawableString;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.VerticalAlignment;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.grid.display.AbstractGriddedDisplay;
import com.raytheon.uf.viz.core.map.IMapDescriptor;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * Display grid point values
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * June, 2010    164        M. Li       Initial creation
 * October,2011             X. Guo      Display grid point in GRID_CENTER
 * Apr, 2013                B. Yin      Don't plot missing values
 * 02/04/2014   936         T. Lee      Implemented textSize 
 * Nov 17, 2015  12855      bsteffen    Rewrite to extend AbstractGriddedDisplay
 * Apr 18, 2016  16116      bsteffen    Don't plot missing values
 * 
 * </pre>
 * 
 * @author mli
 * @version 1.0
 */
public class GridPointValueDisplay extends
        AbstractGriddedDisplay<DrawableString> {

    private RGB color;

    private int textSize;

    private FloatBuffer displayValues;

    private IFont font;

    public GridPointValueDisplay(FloatBuffer values, RGB color, int textSize,
            IMapDescriptor descriptor, ISpatialObject gridLocation) {
        super(descriptor, MapUtil.getGridGeometry(gridLocation), textSize, 5.0);
        this.displayValues = values;
        this.color = color;
        this.textSize = textSize;
    }

    @Override
    public void paint(IGraphicsTarget target, PaintProperties paintProps)
            throws VizException {
        if (font == null) {
            font = target.initializeFont("Monospace", textSize,
                    new IFont.Style[] { IFont.Style.BOLD });
        }
        super.paint(target, paintProps);
    }

    @Override
    protected void paint(PaintProperties paintProps,
            Collection<GridCellRenderable> renderables) throws VizException {
        List<DrawableString> strings = new ArrayList<>(renderables.size());
        for (GridCellRenderable point : renderables) {
            DrawableString string = point.resource;
            string.setCoordinates(point.plotLocation.x, point.plotLocation.y);
            strings.add(string);
        }
        target.drawStrings(strings);
    }

    @Override
    protected DrawableString getResource(Coordinate coord) {
        String value = getValueString((int) coord.x, (int) coord.y);
        if (value == null) {
            return null;
        }
        DrawableString string = new DrawableString(value, color);
        string.verticallAlignment = VerticalAlignment.MIDDLE;
        string.horizontalAlignment = HorizontalAlignment.CENTER;
        return string;
    }

    @Override
    protected DrawableString createResource(Coordinate coord)
            throws VizException {
        /*
         * Since getResource can quickly create new objects this will never be
         * necessary.
         */
        return null;
    }

    @Override
    protected void disposeResources() {
        if (font != null) {
            font.dispose();
        }
    }

    private String getValueString(int x, int y) {
        if (x >= this.gridDims[0] || y >= this.gridDims[1] || x < 0 || y < 0) {
            return null;
        }

        int idx = (x + (y * this.gridDims[0]));
        float value = this.displayValues.get(idx);

        if (Float.isNaN(value) || value == IDecoderConstantsN.GRID_MISSING
                || value == Math.abs(IDecoderConstantsN.GRID_MISSING)) {
            return null;
        }

        return String.valueOf((int) value);
    }

}
