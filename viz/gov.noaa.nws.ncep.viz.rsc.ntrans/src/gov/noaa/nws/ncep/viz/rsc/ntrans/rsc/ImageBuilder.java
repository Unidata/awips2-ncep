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
package gov.noaa.nws.ncep.viz.rsc.ntrans.rsc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.eclipse.swt.graphics.RGB;

import com.raytheon.uf.viz.core.DrawableCircle;
import com.raytheon.uf.viz.core.DrawableString;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.VerticalAlignment;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.IFont.Style;
import com.raytheon.uf.viz.core.drawables.IShadedShape;
import com.raytheon.uf.viz.core.exception.VizException;
import org.locationtech.jts.geom.LineString;

import gov.noaa.nws.ncep.viz.rsc.ntrans.jcgm.Command;
import gov.noaa.nws.ncep.viz.rsc.ntrans.ncgm.INcCommand;
import gov.noaa.nws.ncep.viz.rsc.ntrans.ncgm.NcCGM;
import gov.noaa.nws.ncep.viz.rsc.ntrans.wireframe.SharedWireframeGenerator;
import gov.noaa.nws.ncep.viz.rsc.ntrans.wireframe.SharedWireframeGenerator.SharedWireframe;
import gov.noaa.nws.ncep.viz.rsc.ntrans.wireframe.WireframeKey;
import gov.noaa.nws.ncep.viz.rsc.ntrans.wireframe.WireframeShapeBuilder;

/**
 * ImageBuilder - Class which holds the state of a single image while it's under
 * construction by sequential execution of CGM commands. For efficiency (and
 * other) reasons, it is useful not to draw some elements immediately to the
 * screen as each drawable CGM command is processed, but rather to accumulate
 * simple objects (e.g., polylines) into larger aggregate constructs
 * (wireframes) for drawing later by AWIPS II IGraphicsTarget commands. Also,
 * some CGM commands are dependent on modes or states set by previous CGM
 * commands (e.g., LineWidth); this structure also provides a place to hold
 * these states.
 * 
 * ImageBuilder is used to construct a PaintableImage, but is kept distinct so
 * that the former can be discarded (along with any temporary "building
 * materials" it contains) once the latter has been fully constructed.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 30, 2014            bhebbard     Initial creation
 * Oct 24, 2016  R22550    bsteffen     Handle more of the details of rendering
 *                                      using methods.
 * 
 * </pre>
 * 
 * @author bhebbard
 * @version 1.0
 */

// ------------------------------------------------------------

public class ImageBuilder {

    // This class holds the state of the image while it's under
    // construction by sequential execution of the CGM commands.

    // Collection of all wireframes under construction, keyed by unique output
    // draw states
    protected LinkedHashMap<WireframeKey, WireframeShapeBuilder> wireframes = new LinkedHashMap<>();

    // Line color set by the most recent CGM LineColour command. Default to
    // WHITE.
    protected RGB currentLineColor = new RGB(255, 255, 255);

    // Line width set by the most recent CGM LineWidth command. Default to 1
    // pixel.
    protected double currentLineWidth = 1.0;

    // Accumulator for AWIPS II DrawableString text objects
    protected List<DrawableString> strings = new ArrayList<>();

    protected RGB currentTextColor = new RGB(255, 255, 255);

    protected List<IFont> fonts = new ArrayList<>();

    protected HorizontalAlignment horizontalAlignment = HorizontalAlignment.CENTER;

    protected VerticalAlignment verticalAlignment = VerticalAlignment.TOP;

    protected List<DrawableCircle> circles = new ArrayList<>();

    protected final IShadedShape shadedShape;

    protected RGB currentFillColor = new RGB(0, 255, 0);

    protected final double scale;

    public ImageBuilder(IDescriptor descriptor, IGraphicsTarget target,
            double scale) {
        IFont currentFont = target.initializeFont("Monospace", 10,
                new IFont.Style[] { Style.BOLD });
        this.fonts.add(currentFont);
        this.scale = scale;
        shadedShape = target.createShadedShape(false,
                descriptor.getGridGeometry());
    }

    public PaintableImage build(NcCGM cgmImage,
            SharedWireframeGenerator wireframeGen) throws VizException {
        for (Command c : cgmImage.getCommands()) {
            if (c instanceof INcCommand) {
                ((INcCommand) c).contributeToPaintableImage(this);
            }
        }

        shadedShape.compile();

        LinkedHashMap<WireframeKey, SharedWireframe> compiledWireframes = new LinkedHashMap<>();

        for (WireframeKey key : this.wireframes.keySet()) {
            WireframeShapeBuilder wireframeForThisKey = this.wireframes
                    .get(key);
            SharedWireframe shared = wireframeGen
                    .getWireframeShape(wireframeForThisKey);
            compiledWireframes.put(key, shared);
        }

        return new PaintableImage(compiledWireframes, shadedShape, strings,
                circles, fonts);
    }

    public double[] scalePoint(double[] oldpoint) {
        return scalePoint(oldpoint[0], oldpoint[1]);
    }

    public double[] scalePoint(double x, double y) {
        double[] newpoint = new double[2];
        newpoint[0] = x * scale;
        newpoint[1] = 1000.000 - y * scale; // TODO: Avoid hardcoding 1000
        return newpoint;
    }


    public IFont getCurrentFont() {
        return fonts.get(0);
    }

    public void setCurrentFont(IFont currentFont) {
        fonts.add(0, currentFont);
    }

    public RGB getCurrentLineColor() {
        return currentLineColor;
    }

    public void setCurrentLineColor(RGB currentLineColor) {
        this.currentLineColor = currentLineColor;
    }

    public double getCurrentLineWidth() {
        return currentLineWidth;
    }

    public void setCurrentLineWidth(double currentLineWidth) {
        this.currentLineWidth = currentLineWidth;
    }

    public RGB getCurrentFillColor() {
        return currentFillColor;
    }

    public void setCurrentFillColor(RGB currentFillColor) {
        this.currentFillColor = currentFillColor;
    }

    public RGB getCurrentTextColor() {
        return currentTextColor;
    }

    public void setCurrentTextColor(RGB currentTextColor) {
        this.currentTextColor = currentTextColor;
    }

    public void setTextAlignment(HorizontalAlignment horizontalAlignment,
            VerticalAlignment verticalAlignment) {
        this.horizontalAlignment = horizontalAlignment;
        this.verticalAlignment = verticalAlignment;
    }

    public HorizontalAlignment getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public VerticalAlignment getVerticalAlignment() {
        return verticalAlignment;
    }

    public void addCircle(DrawableCircle circle) {
        circles.add(circle);
    }

    public void addString(DrawableString string) {
        strings.add(string);
    }

    public void addPolygon(LineString[] lineString) {
        shadedShape.addPolygonPixelSpace(lineString, currentFillColor);
    }

    public void addLineSegment(double[][] segment) {
        WireframeKey key = new WireframeKey(currentLineColor, currentLineWidth);
        /* Remove to reset insertion order. */
        WireframeShapeBuilder builder = wireframes.remove(key);
        if (builder == null) {
            builder = new WireframeShapeBuilder();
        }
        builder.addLineSegment(segment);
        wireframes.put(key, builder);
    }

}
