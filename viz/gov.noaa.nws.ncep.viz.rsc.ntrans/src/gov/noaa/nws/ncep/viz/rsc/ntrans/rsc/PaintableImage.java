package gov.noaa.nws.ncep.viz.rsc.ntrans.rsc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import com.raytheon.uf.viz.core.DrawableCircle;
import com.raytheon.uf.viz.core.DrawableString;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.IShadedShape;
import com.raytheon.uf.viz.core.drawables.IWireframeShape;
import com.raytheon.uf.viz.core.exception.VizException;

import gov.noaa.nws.ncep.viz.rsc.ntrans.wireframe.SharedWireframeGenerator.SharedWireframe;
import gov.noaa.nws.ncep.viz.rsc.ntrans.wireframe.WireframeKey;

/**
 * 
 * This is just a container holding the ready-to-paint information for // a
 * single NTRANS image.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ------------------------
 * Oct 24, 2016  R22550   bsteffen  Share wireframe shapes.
 * 
 * </pre>
 *
 */
public class PaintableImage {

    protected IShadedShape shadedShape;

    protected LinkedHashMap<WireframeKey, SharedWireframe> wireframes;

    protected List<DrawableString> strings;

    protected List<DrawableCircle> circles;

    /*
     * Need reference to all fonts in the strings so they can be disposed. Since
     * some fonts are reused across multiple strings the easiest way to ensure
     * each font is disposed once is to keep a separate list.
     */
    protected List<IFont> fonts;

    public PaintableImage(
            LinkedHashMap<WireframeKey, SharedWireframe> wireframes,
            IShadedShape shadedShape, List<DrawableString> strings,
            List<DrawableCircle> circles, List<IFont> fonts) {
        this.wireframes = wireframes;
        this.shadedShape = shadedShape;
        this.strings = strings;
        this.circles = circles;
        this.fonts = fonts;
    }

    public void paint(IGraphicsTarget target) throws VizException {
        if (shadedShape != null) {
            target.drawShadedShape(shadedShape, 1.0f, 1.0f);
        }

        for (Entry<WireframeKey, SharedWireframe> entry : wireframes
                .entrySet()) {
            IWireframeShape wireframeForThisKey = entry.getValue()
                    .getWireframe();
            target.drawWireframeShape(wireframeForThisKey, entry.getKey().color,
                    (float) entry.getKey().width);
        }

        target.drawStrings(strings);
        target.drawCircle(circles.toArray(new DrawableCircle[circles.size()]));
    }

    public void dispose() {
        if (shadedShape != null) {
            shadedShape.dispose();
            shadedShape = null;
        }
        if (wireframes != null) {
            for (SharedWireframe wf : wireframes.values()) {
                wf.dispose();
            }
            wireframes.clear();
        }
        strings.clear();
        circles.clear();
        for (IFont font : fonts) {
            font.dispose();
        }
        fonts.clear();
    }
}