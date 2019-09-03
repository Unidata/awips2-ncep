package gov.noaa.nws.ncep.viz.overlays.resources;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.graphics.RGB;

import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.IMapDescriptor;
import com.raytheon.uf.viz.core.map.MapDescriptor;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.IResourceDataChanged.ChangeType;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.ColorableCapability;

import gov.noaa.nws.ncep.ui.pgen.controls.ActivityCollection;
import gov.noaa.nws.ncep.ui.pgen.controls.ActivityElement;
import gov.noaa.nws.ncep.ui.pgen.display.AbstractElementContainer;
import gov.noaa.nws.ncep.ui.pgen.display.DisplayProperties;
import gov.noaa.nws.ncep.ui.pgen.display.ElementContainerFactory;
import gov.noaa.nws.ncep.ui.pgen.elements.DrawableElement;
import gov.noaa.nws.ncep.ui.pgen.elements.Layer;
import gov.noaa.nws.ncep.ui.pgen.elements.Product;
import gov.noaa.nws.ncep.ui.pgen.file.FileTools;
import gov.noaa.nws.ncep.ui.pgen.file.ProductConverter;
import gov.noaa.nws.ncep.ui.pgen.file.Products;
import gov.noaa.nws.ncep.ui.pgen.store.PgenStorageException;
import gov.noaa.nws.ncep.ui.pgen.store.StorageUtils;
import gov.noaa.nws.ncep.viz.localization.NcPathManager;
import gov.noaa.nws.ncep.viz.localization.NcPathManager.NcPathConstants;
import gov.noaa.nws.ncep.viz.resources.IStaticDataNatlCntrsResource;

/**
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Dec 20, 2011           ghull     Initial creation
 * Dec 23, 2011  579      jzeng     Implementation
 * Dec 18, 2012  861      ghull     allow display of original element colors
 *                                  from pgen product.
 * Feb 17, 2016  13554    dgilling  Implement IStaticDataNatlCntrsResource.
 * Feb 28, 2019  7752     tjensen   Add capability to load product from
 *                                  database.
 * Jul 03, 2019  65673    tjensen   Fix ColorableCapability
 * Aug 29, 2019  65679    ksunil    added resourceDataChanged
 *
 * </pre>
 *
 * @author
 */
public class PgenStaticOverlayResource extends
        AbstractVizResource<PgenStaticOverlayResourceData, IMapDescriptor>
        implements IStaticDataNatlCntrsResource {
    private final PgenStaticOverlayResourceData pgenOverlayRscData;

    /** Whether the resource is ready to be drawn */

    /** The list of points */
    private List<Product> prds = null;

    private final DisplayProperties dprops = new DisplayProperties();

    /**
     * Create a PGEN XML Overlay resource.
     *
     * @throws VizException
     */
    protected PgenStaticOverlayResource(
            PgenStaticOverlayResourceData resourceData,
            LoadProperties loadProperties) throws VizException {
        super(resourceData, loadProperties);
        pgenOverlayRscData = resourceData;
    }

    @Override
    public void initInternal(IGraphicsTarget target) throws VizException {

        if (pgenOverlayRscData.getPgenStaticProductLocation() != null) {
            String lFileName = pgenOverlayRscData.getPgenStaticProductLocation()
                    + File.separator
                    + pgenOverlayRscData.getPgenStaticProductName();
            File productFile = null;

            if (lFileName.startsWith(NcPathConstants.NCEP_ROOT)) {
                productFile = NcPathManager.getInstance()
                        .getStaticFile(lFileName);
            } else {
                /*
                 * TODO : this should be considered temporary since soon the
                 * PGEN Files will be stored on the server and this option will
                 * not work.
                 */
                productFile = new File(lFileName);
            }

            if (productFile == null || !productFile.exists()) {
                throw new VizException("Error. PGEN product: " + lFileName
                        + ", doesn't exist");
            }

            /*
             * get the PGEN product data, and convert into format ready for
             * display during paint
             */
            try {
                Products products = FileTools
                        .read(productFile.getCanonicalPath());
                prds = ProductConverter.convert(products);

            } catch (IOException e) {
                throw new VizException("Error reading PGEN Product from file: "
                        + productFile.getAbsolutePath(), e);
            }
        } else {
            /*
             * If a dataURI is set in the resource data, retrieve the product
             * from the database. Else try to get products from the xml
             * specified in the resource data.
             */
            ActivityCollection ac = new ActivityCollection();
            ActivityElement myElem = null;
            for (ActivityElement elem : ac.getCurrentActivityList()) {
                if (elem.getActivityLabel()
                        .equals(pgenOverlayRscData.getPgenStaticProductName())
                        && (myElem == null || elem.getRefTime()
                                .after(myElem.getRefTime()))) {
                    myElem = elem;
                }
            }
            if (myElem != null) {
                String dataURI = myElem.getDataURI();
                if (dataURI != null && !(dataURI.isEmpty())) {
                    try {
                        prds = StorageUtils.retrieveProduct(dataURI);
                    } catch (PgenStorageException e) {
                        statusHandler.handle(Priority.PROBLEM,
                                e.getLocalizedMessage(), e);
                    }
                }
            } else {
                throw new VizException(
                        "Error retrieving PGEN product from database:"
                                + pgenOverlayRscData
                                        .getPgenStaticProductName());
            }
        }
        resourceAttrsModified();
    }

    @Override
    public void paintInternal(IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {

        if (paintProps != null && prds != null) {
            if (pgenOverlayRscData.monoColorEnable) {
                RGB rgb = getCapability(ColorableCapability.class).getColor();
                dprops.setLayerColor(new Color(rgb.red, rgb.green, rgb.blue));
            }

            /*
             * Loop through all products in the PGEN drawing layer, drawing the
             * display elements
             */
            for (Product prod : prds) {
                if (prod.isOnOff()) {
                    for (Layer layer : prod.getLayers()) {
                        if (layer.isOnOff()) {
                            Iterator<DrawableElement> iterator = layer
                                    .createDEIterator();
                            AbstractElementContainer container;
                            while (iterator.hasNext()) {
                                DrawableElement el = iterator.next();
                                container = ElementContainerFactory
                                        .createContainer(el,
                                                (MapDescriptor) descriptor,
                                                target);
                                container.draw(target, paintProps, dprops);
                                container.dispose();
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void disposeInternal() {
    }

    @Override
    public void resourceAttrsModified() {
        dprops.setLayerMonoColor(pgenOverlayRscData.monoColorEnable);
    }

    @Override
    public void resourceDataChanged(ChangeType type, Object object) {
        if (object instanceof ColorableCapability) {
            pgenOverlayRscData.setColor(
                    getCapability(ColorableCapability.class).getColor());
        }
    }
}
