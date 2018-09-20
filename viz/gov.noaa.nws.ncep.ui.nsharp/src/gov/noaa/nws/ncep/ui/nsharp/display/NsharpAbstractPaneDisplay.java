/**
 * 
 * gov.noaa.nws.ncep.ui.nsharp.display.NsharpAbstractPaneDisplay
 * 
 * This java class performs the NSHARP NsharpAbstractPaneDisplay functions.
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    	Engineer    Description
 * -------		------- 	-------- 	-----------
 * 04/30/2012	229			Chin Chen	Initial coding for multiple display panes implementation
 * 03/11/2013   972         Greg Hull   rm paneNum and editorNum
 * 01/15/2018   6746        bsteffen    Override getGlobalsMap()
 * 07/31/2018   6800        bsteffen    Fix NPE in getGlobalsMap()
 *
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 */
package gov.noaa.nws.ncep.ui.nsharp.display;

import java.util.ArrayList;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.PixelExtent;
import com.raytheon.uf.viz.core.VizConstants;
import com.raytheon.uf.viz.core.drawables.AbstractRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractResourceData;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource.ResourceStatus;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.ColorableCapability;

import gov.noaa.nws.ncep.ui.nsharp.NsharpConstants;
import gov.noaa.nws.ncep.ui.nsharp.display.rsc.NsharpAbstractPaneResourceData;
import gov.noaa.nws.ncep.ui.nsharp.display.rsc.NsharpResourceHandler;

@XmlAccessorType(XmlAccessType.NONE)
public class NsharpAbstractPaneDisplay extends AbstractRenderableDisplay {

    public NsharpAbstractPaneDisplay() {
        super();
    }

    public NsharpAbstractPaneDisplay(PixelExtent pixelExtent, int paneNumber) {
        this(pixelExtent, paneNumber, "AbstractPane",
                new NsharpAbstractPaneDescriptor(pixelExtent, paneNumber));
    }

    public NsharpAbstractPaneDisplay(PixelExtent pixelExtent, int paneNumber,
            String name, NsharpAbstractPaneDescriptor desc) {
        super(pixelExtent, desc);
    }

    @Override
    public NsharpAbstractPaneDescriptor getDescriptor() {
        return (NsharpAbstractPaneDescriptor) super.getDescriptor();
    }

    @Override
    public void paint(IGraphicsTarget target, PaintProperties paintProps)
            throws VizException {
        super.paint(target, paintProps);

        drawTheData(target, paintProps);
    }

    /**
     * Draws the data on the screen.
     * 
     * @throws VizException
     */
    protected void drawTheData(IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {
        ArrayList<ResourcePair> resourceList = new ArrayList<ResourcePair>(
                descriptor.getResourceList());

        PaintProperties myProps = new PaintProperties(paintProps);
        for (ResourcePair pair : resourceList) {
            if (pair.getProperties().isVisible()) {
                AbstractVizResource<?, ?> rsc = pair.getResource();
                if ((rsc != null)
                        && (rsc.getStatus() != ResourceStatus.DISPOSED)) {
                    myProps = calcPaintDataTime(myProps, rsc);
                    rsc.paint(target, myProps);
                }
            }
        }
    }

    @Override
    protected void customizeResourceList(ResourceList resourceList) {
        AbstractResourceData resourceData = new NsharpAbstractPaneResourceData();
        // get a load properties
        LoadProperties loadProperties = new LoadProperties();
        ColorableCapability colorableCapability = new ColorableCapability();
        colorableCapability.setColor(NsharpConstants.backgroundColor);
        loadProperties.getCapabilities().addCapability(colorableCapability);
        // get some resource properties
        ResourceProperties resourceProperties = new ResourceProperties();
        resourceProperties.setVisible(true);
        resourceProperties.setMapLayer(true);
        resourceProperties.setSystemResource(true);
        // Make a resource pair
        ResourcePair resourcePair = new ResourcePair();
        resourcePair.setResourceData(resourceData);
        resourcePair.setLoadProperties(loadProperties);
        resourcePair.setProperties(resourceProperties);
        // add it to the resource list.
        resourceList.add(resourcePair);
    }

    @Override
    public Map<String, Object> getGlobalsMap() {
        Map<String, Object> globals = super.getGlobalsMap();
        NsharpResourceHandler rscHandler = getDescriptor().getRscHandler();
        if (rscHandler != null) {
            globals.put(VizConstants.FRAME_COUNT_ID,
                    rscHandler.getFrameCount());
        }
        globals.put(VizConstants.FRAMES_ID, descriptor.getNumberOfFrames());
        return globals;
    }

}
