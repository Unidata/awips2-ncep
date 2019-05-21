package gov.noaa.nws.ncep.viz.ui.seek;

import java.util.Iterator;

import org.eclipse.swt.graphics.RGB;

import com.raytheon.uf.viz.core.DrawableString;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.MapDescriptor;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.EditableCapability;
import com.raytheon.viz.ui.input.EditableManager;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * Implements a drawing layer to draw lines for Seek.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------
 * ???                    M. Li     Initial creation.
 * May 08, 2019  63530    tjensen   Added setEditable()
 *
 * </pre>
 *
 * @author mli
 *
 */
public class SeekDrawingLayer
        extends AbstractVizResource<SeekResourceData, MapDescriptor> {

    private final SeekResourceData seekRscData;

    protected String name = "";

    protected RGB color = new RGB(255, 255, 0);

    /** Default text color */
    private final RGB textColor = new RGB(255, 255, 0);

    protected SeekDrawingLayer(SeekResourceData resourceData,
            LoadProperties loadProperties) {
        super(resourceData, loadProperties);
        seekRscData = resourceData;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void initInternal(IGraphicsTarget target) throws VizException {
        // no operation
    }

    @Override
    public void paintInternal(IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {
        Coordinate point1 = seekRscData.getPoint1();
        Coordinate point2 = seekRscData.getPoint2();
        Coordinate firstPt = seekRscData.getFirstPt();
        Coordinate lastPt = seekRscData.getLastPt();

        // Draw line
        if (point1 != null && point2 != null) {
            double[] p1 = descriptor
                    .worldToPixel(new double[] { point1.x, point1.y });
            double[] p2 = descriptor
                    .worldToPixel(new double[] { point2.x, point2.y });
            target.drawLine(p1[0], p1[1], 0.0, p2[0], p2[1], 0.0, textColor,
                    1.0f);
        }

        // Draw distance and direction string
        if (seekRscData.strings != null && seekRscData.strings.size() > 0) {
            Iterator<Coordinate> coords = this.seekRscData.strings.keySet()
                    .iterator();
            double[] in = new double[3];
            while (coords.hasNext()) {
                Coordinate c = coords.next();
                String text = seekRscData.strings.get(c);
                in[0] = c.x;
                in[1] = c.y;
                in[2] = c.z;
                double[] out = descriptor.worldToPixel(in);
                DrawableString ds = new DrawableString(text, textColor);
                ds.setCoordinates(out[0], out[1]);
                ds.textStyle = IGraphicsTarget.TextStyle.NORMAL;
                ds.horizontalAlignment = HorizontalAlignment.LEFT;
                target.drawStrings(ds);
            }
        }

        float zoomLevel = paintProps.getZoomLevel();
        int d = (int) (200 * zoomLevel + 1.0);

        // Draw end point cross
        if (firstPt != null && lastPt == null) {
            double[] p1 = descriptor
                    .worldToPixel(new double[] { firstPt.x, firstPt.y });
            target.drawLine(p1[0] + d, p1[1], 0.0, p1[0] - d, p1[1], 0.0,
                    textColor, 1.0f);
            target.drawLine(p1[0], p1[1] + d, 0.0, p1[0], p1[1] - d, 0.0,
                    textColor, 1.0f);
        }

        if (firstPt == null && lastPt != null) {
            double[] p2 = descriptor
                    .worldToPixel(new double[] { lastPt.x, lastPt.y });
            target.drawLine(p2[0] + d, p2[1], 0.0, p2[0] - d, p2[1], 0.0,
                    textColor, 1.0f);
            target.drawLine(p2[0], p2[1] + d, 0.0, p2[0], p2[1] - d, 0.0,
                    textColor, 1.0f);
        }

        // Draw click point line
        if (firstPt != null && lastPt != null) {
            double[] p1 = descriptor
                    .worldToPixel(new double[] { firstPt.x, firstPt.y });
            double[] p2 = descriptor
                    .worldToPixel(new double[] { lastPt.x, lastPt.y });
            target.drawLine(p1[0], p1[1], 0.0, p2[0], p2[1], 0.0, textColor,
                    1.0f);

            // Draw cross
            target.drawLine(p1[0] + d, p1[1], 0.0, p1[0] - d, p1[1], 0.0,
                    textColor, 1.0f);
            target.drawLine(p1[0], p1[1] + d, 0.0, p1[0], p1[1] - d, 0.0,
                    textColor, 1.0f);
            target.drawLine(p2[0] + d, p2[1], 0.0, p2[0] - d, p2[1], 0.0,
                    textColor, 1.0f);
            target.drawLine(p2[0], p2[1] + d, 0.0, p2[0], p2[1] - d, 0.0,
                    textColor, 1.0f);
        }

    }

    public void clearLine() {
        seekRscData.setPoint1(null);
        seekRscData.setPoint2(null);
    }

    public RGB getColor() {
        return color;
    }

    public void setColor(RGB color) {
        this.color = color;
    }

    @Override
    protected void disposeInternal() {
        // no operation
    }

    public void setEditable(boolean enable) {
        getCapability(EditableCapability.class).setEditable(enable);
        EditableManager.makeEditable(this,
                getCapability(EditableCapability.class).isEditable());
    }

}
