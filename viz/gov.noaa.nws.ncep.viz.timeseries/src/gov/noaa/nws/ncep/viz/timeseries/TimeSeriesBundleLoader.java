package gov.noaa.nws.ncep.viz.timeseries;

import java.io.File;
import java.util.Map;

import com.raytheon.uf.viz.core.DescriptorMap;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.procedures.Bundle;
import com.raytheon.viz.ui.BundleLoader;
import com.raytheon.viz.ui.UiUtil;
import com.raytheon.viz.ui.editor.AbstractEditor;

public class TimeSeriesBundleLoader {
    /**
     * Load a bundle from a file into a container
     * 
     * @param editor
     *            the container to load to
     * @param f
     *            the file containing the bundle
     * @param descriptor
     *            Optional: A descriptor that should be used for time matching
     * @throws VizException
     */
    public static void loadTo(File f, Map<String, String> variables)
            throws VizException {
        Bundle b = Bundle.unmarshalBundle(f, variables);

        IRenderableDisplay renderableDisplay = b.getDisplays()[0];
        IDescriptor bundleDescriptor = renderableDisplay.getDescriptor();
        String bundleEditorId = DescriptorMap.getEditorId(bundleDescriptor
                .getClass().getName());
        // AbstractEditor editor = UiUtil.createOrOpenEditor(bundleEditorId,
        AbstractEditor editor = UiUtil.createEditor(bundleEditorId,
                b.getDisplays());

        BundleLoader.loadTo(editor, b);
    }

}
