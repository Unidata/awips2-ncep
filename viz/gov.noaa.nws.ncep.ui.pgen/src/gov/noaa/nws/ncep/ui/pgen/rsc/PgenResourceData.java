package gov.noaa.nws.ncep.ui.pgen.rsc;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.PlatformUI;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractResourceData;
import com.raytheon.uf.viz.core.rsc.IResourceDataChanged.ChangeType;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.viz.ui.editor.AbstractEditor;
import com.raytheon.viz.ui.perspectives.AbstractVizPerspectiveManager;
import com.raytheon.viz.ui.perspectives.VizPerspectiveListener;
import com.raytheon.viz.ui.tools.AbstractModalTool;
import org.locationtech.jts.geom.Coordinate;

import gov.noaa.nws.ncep.ui.pgen.Activator;
import gov.noaa.nws.ncep.ui.pgen.PgenConstant;
import gov.noaa.nws.ncep.ui.pgen.PgenPreferences;
import gov.noaa.nws.ncep.ui.pgen.PgenSession;
import gov.noaa.nws.ncep.ui.pgen.PgenUtil;
import gov.noaa.nws.ncep.ui.pgen.PgenUtil.PgenMode;
import gov.noaa.nws.ncep.ui.pgen.controls.AddElementCommand;
import gov.noaa.nws.ncep.ui.pgen.controls.AddElementsCommand;
import gov.noaa.nws.ncep.ui.pgen.controls.CommandStackListener;
import gov.noaa.nws.ncep.ui.pgen.controls.DeleteAllCommand;
import gov.noaa.nws.ncep.ui.pgen.controls.DeleteElementCommand;
import gov.noaa.nws.ncep.ui.pgen.controls.DeletePartCommand;
import gov.noaa.nws.ncep.ui.pgen.controls.DeleteSelectedElementsCommand;
import gov.noaa.nws.ncep.ui.pgen.controls.PgenCommand;
import gov.noaa.nws.ncep.ui.pgen.controls.PgenCommandManager;
import gov.noaa.nws.ncep.ui.pgen.controls.PgenRemindDialog;
import gov.noaa.nws.ncep.ui.pgen.controls.ReplaceElementCommand;
import gov.noaa.nws.ncep.ui.pgen.controls.ReplaceElementsCommand;
import gov.noaa.nws.ncep.ui.pgen.display.SymbolImageUtil;
import gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent;
import gov.noaa.nws.ncep.ui.pgen.elements.DECollection;
import gov.noaa.nws.ncep.ui.pgen.elements.Layer;
import gov.noaa.nws.ncep.ui.pgen.elements.Line;
import gov.noaa.nws.ncep.ui.pgen.elements.Product;
import gov.noaa.nws.ncep.ui.pgen.elements.ProductInfo;
import gov.noaa.nws.ncep.ui.pgen.elements.ProductTime;
import gov.noaa.nws.ncep.ui.pgen.file.FileTools;
import gov.noaa.nws.ncep.ui.pgen.file.ProductConverter;
import gov.noaa.nws.ncep.ui.pgen.file.Products;
import gov.noaa.nws.ncep.ui.pgen.layering.PgenLayeringControlDialog;
import gov.noaa.nws.ncep.ui.pgen.productmanage.ProductManageDialog;
import gov.noaa.nws.ncep.ui.pgen.store.PgenStorageException;
import gov.noaa.nws.ncep.ui.pgen.store.StorageUtils;
import gov.noaa.nws.ncep.ui.pgen.tools.AbstractPgenDrawingTool;
import gov.noaa.nws.ncep.viz.common.ISaveableResourceData;

/**
 * Contains all the PGEN Products, layers, and Elements behind the PgenResource.
 * Also holds the command manager to undo/redo changes to the data in the
 * product list.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#      Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 01/15        #5413       B. Yin      Close PGEN palette in cleanup
 * 05/02/2016   R16076      J. Wu       update startProductManage()   
 * 05/10/2016   R13560      S. Russell  Updated promptToSave to no longer save
 *                                      data, that is done in PGenPaletteWindow
 *                                      ISaveablePart2 code.  Also modified it
 *                                      to return the return code of the dialog
 * 06/15/2016   R13559      bkowal      File cleanup. Removed commented code.
 * 07/28/2016   R17954      B. Yin      Close PGEN palette only in D2D.
 *                                      Implemented ISaveableResourceData
 * 08/15/2016   R21066      J. Wu       Add switchLayer() for hot key handler.
 * 11/30/2016   R17954      Bugenhagen  Keep track of total number of pgen resources
 *                                      in session.  Modified cleanup method.
 *                                      Always return resource data as dirty.
 * 
 * </pre>
 * 
 * @author sgilbert
 */

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class PgenResourceData extends AbstractResourceData
        implements CommandStackListener, ISaveableResourceData {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(PgenResourceData.class);

    private List<Product> productList;

    private PgenCommandManager commandMgr;

    /**
     * Current active product in the PGEN drawing layer.
     */
    private Product activeProduct = null;

    private ProductManageDialog productManageDlg = null;

    /**
     * Current active layer in the PGEN drawing layer' active product.
     */
    private Layer activeLayer = null;

    private PgenLayeringControlDialog layeringControlDlg = null;

    /*
     * This group of fields used for the Autosave and recovery feature
     */
    private String recoveryFilename;

    private String autoSaveFilename = null;

    private boolean autosave = false;

    private long autosaveInterval; // in milliseconds

    private long lastSaveTime = 0;

    private boolean multiSave = true;

    private boolean needsSaving = false;

    private boolean needsDisplay = false;

    /**
     * Number of pgen resources for this resource data object. Not to be
     * confused with the total number of pgen resources in a pgen session
     * {@link PgenSession#getNumberOfSessionResources}.
     */
    private int numberOfResources = 0;

    private ArrayList<PgenResource> rscList = new ArrayList<>();

    public PgenResourceData() {
        super();
        productList = new ArrayList<>();
        commandMgr = new PgenCommandManager();
        commandMgr.addStackListener(this);
        recoveryFilename = PgenUtil.RECOVERY_PREFIX + System.currentTimeMillis()
                + "." + this.hashCode() + PgenUtil.RECOVERY_POSTFIX;
        initializeProducts();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.rsc.AbstractResourceData#construct(com.raytheon
     * .uf.viz.core.comm.LoadProperties,
     * com.raytheon.uf.viz.core.drawables.IDescriptor)
     */
    @Override
    public PgenResource construct(LoadProperties loadProperties,
            IDescriptor descriptor) throws VizException {
        numberOfResources++;
        PgenSession.getInstance().bumpNumberOfSessionResources();
        PgenResource rsc = new PgenResource(this, loadProperties);
        rscList.add(rsc);
        return rsc;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.rsc.AbstractResourceData#update(java.lang.Object
     * )
     */
    @Override
    public void update(Object updateData) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.rsc.AbstractResourceData#equals(java.lang.Object
     * )
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }

    /**
     * @return the productList
     */
    public List<Product> getProductList() {
        return productList;
    }

    /**
     * @return the commandMgr
     */
    public PgenCommandManager getCommandMgr() {
        return commandMgr;
    }

    /**
     * @return the activeProduct
     */
    public Product getActiveProduct() {
        return activeProduct;
    }

    /**
     * @param activeProduct
     *            the activeProduct to set
     */
    public void setActiveProduct(Product activeProduct) {
        this.activeProduct = activeProduct;
    }

    /**
     * @return the productManageDlg
     */
    public ProductManageDialog getProductManageDlg() {
        return productManageDlg;
    }

    /**
     * @param productManageDlg
     *            the productManageDlg to set
     */
    public void setProductManageDlg(ProductManageDialog productManageDlg) {
        this.productManageDlg = productManageDlg;
    }

    /**
     * @return the activeLayer
     */
    public Layer getActiveLayer() {
        return activeLayer;
    }

    /**
     * @param activeLayer
     *            the activeLayer to set
     */
    public void setActiveLayer(Layer activeLayer) {
        this.activeLayer = activeLayer;
    }

    /**
     * @return the layeringControlDlg
     */
    public PgenLayeringControlDialog getLayeringControlDlg() {
        return layeringControlDlg;
    }

    /**
     * @param layeringControlDlg
     *            the layeringControlDlg to set
     */
    public void setLayeringControlDlg(
            PgenLayeringControlDialog layeringControlDlg) {
        this.layeringControlDlg = layeringControlDlg;
    }

    /**
     * Start product management or layering if necessary.
     */
    public void startProductManage() {

        if (productManageDlg != null) {
            productManageDlg.close();
        }

        if (!activeProduct.getName()
                .equalsIgnoreCase(PgenConstant.GENERAL_DEFAULT)
                || !activeProduct.getType()
                        .equalsIgnoreCase(PgenConstant.GENERAL_DEFAULT)
                || productList.size() > 1) {

            activeProduct.setOnOff(true);

            activateProductManage();
        } else if (productList.size() == 1) {
            startLayering();
        } else {
            PgenUtil.setSelectingMode();
        }

    }

    /**
     * Activate product management.
     */
    public void activateProductManage() {

        if (layeringControlDlg != null && layeringControlDlg.isOpen()) {
            layeringControlDlg.close();
        }

        initializeProducts();

        AbstractEditor mapEditor = PgenUtil.getActiveEditor();

        if (productManageDlg == null) {

            Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                    .getShell();
            productManageDlg = new ProductManageDialog(shell);

        }

        PgenUtil.setSelectingMode();

        if (!(productManageDlg.isOpen())) {
            productManageDlg.open();
        }

        mapEditor.refresh();

    }

    /**
     * closes any
     */
    public void closeDialogs() {

        if (layeringControlDlg != null && layeringControlDlg.isOpen()) {
            layeringControlDlg.close();
        }

        if (productManageDlg != null && productManageDlg.isOpen()) {
            productManageDlg.close();
        }

    }

    /**
     * Initialize product list for the resource.
     */
    public void initializeProducts() {

        /*
         * Create an active product with an active layer and add to the product
         * List .
         */
        if (productList.size() == 0) {

            activeProduct = new Product("Default", "Default", "Default",
                    new ProductInfo(), new ProductTime(),
                    new ArrayList<Layer>());

            activeLayer = new Layer();
            activeProduct.addLayer(activeLayer);

            productList.add(activeProduct);

        }

    }

    /**
     * Uses a PgenCommand to replace one drawable element in the product list
     * with another drawable element.
     * 
     * @param old
     *            Element to replace
     * @param Element
     *            new drawable element
     */
    public void replaceElement(AbstractDrawableComponent old,
            AbstractDrawableComponent newde) {

        /*
         * create a new ReplaceElementCommand and send it to the Command Manager
         */
        PgenCommand cmd = new ReplaceElementCommand(productList, old, newde);
        commandMgr.addCommand(cmd);

    }

    /**
     * Uses a PgenCommand to replace a set of drawable element in the active
     * layer with another set of drawable elements. If the parent is not null,
     * old elements will be removed from the parent and new elements will be add
     * in the parent. If the parent is null, the old element list should have
     * same number of elements as the new list has. Loop through each of the
     * elements in the old list, find the parent, remove the old element and add
     * the new element.
     * 
     * @param parent
     *            parent collection of the old elements
     * @param old
     *            Elements to replace
     * @param newde
     *            New drawable elements
     */
    public void replaceElements(DECollection parent,
            List<AbstractDrawableComponent> old,
            List<AbstractDrawableComponent> newde) {
        /*
         * create a new ReplaceElementsCommand and send it to the Command
         * Manager
         */
        if (old == null || old.isEmpty()) {
            parent = activeLayer;
        }

        PgenCommand cmd = new ReplaceElementsCommand(parent, old, newde);
        commandMgr.addCommand(cmd);

    }

    /**
     * Replace the active product with a new product.
     */
    public void replaceProduct(List<Product> prds) {

        int index = 0;
        if (productList.size() > 0) {
            index = productList.indexOf(activeProduct);
            productList.set(index, prds.get(0));
        } else {
            productList.addAll(prds);
        }

        /*
         * Set active product and layer to start product management.
         */
        activeProduct = productList.get(index);
        activeLayer = productList.get(index).getLayer(0);

        startProductManage();

    }

    /**
     * Add products to the existing products. Rule: (1) If there is only a
     * Default product with a Default layer in the "productList" and no DEs in
     * it. This empty product is removed before appending any products to
     * "productList". (2) If an incoming product has the same name as an
     * existing product. it is appended but an warning message is provided to
     * ask the user to change it to a different names.
     */
    public void addProduct(List<Product> prds) {

        // remove the empty "Default" product
        if (removeEmptyDefaultProduct()) {
            if (prds != null && prds.size() > 0) {
                productList.clear();
            }
        }

        // Find the active Product.
        int index = -1;
        if (productList.size() > 0) {
            index = productList.indexOf(activeProduct);
        }

        // Append all products
        productList.addAll(prds);

        /*
         * Set active product and layer to start layering control.
         */
        if (index < 0) {
            activeProduct = productList.get(0);
            activeLayer = productList.get(0).getLayer(0);
        }

        startProductManage();

    }

    /**
     * Append the incoming activity to the active activity.
     * 
     * Rule: (1) If there is only ONE layer in the incoming activity, its
     * contents is combined into the active layer. (2) If there are more than
     * one layer in the incoming activity, we will first try to match the active
     * layer with the incoming layers by this order: a. a layer with the same
     * name as the active layers' b. a "Default" layer
     * 
     * All other layers in the incoming activity will be matched against the
     * layers in the active activity by the layer's name. If no match found, the
     * incoming layer is attached as a separate layer.
     * 
     */
    public void appendProduct(List<Product> prds) {

        // remove the empty "Default" product
        if (productList.size() == 1
                && productList.get(0).getName().equals("Default")
                && productList.get(0).getLayers().size() == 1
                && productList.get(0).getLayers().get(0).getName()
                        .equals("Default")
                && productList.get(0).getLayers().get(0).getDrawables()
                        .size() == 0) {

            if (prds != null && prds.size() > 0) {
                productList.clear();
            }

        }

        /*
         * Incoming activity always has only one activity
         */
        if (productList.isEmpty()) {
            productList.add(prds.get(0));
            activeProduct = productList.get(0);
            activeLayer = productList.get(0).getLayer(0);
        } else {
            int nlayers = prds.get(0).getLayers().size();

            // Only one layer, combine them anyway
            if (nlayers == 1) {
                activeLayer.add(prds.get(0).getLayers().get(0).getDrawables());
            } else {

                ArrayList<Boolean> layerUsed = new ArrayList<>();
                for (int ii = 0; ii < nlayers; ii++) {
                    layerUsed.add(false);
                }

                // Find match for active layer
                Layer matchLayer = prds.get(0).getLayer(activeLayer.getName());
                if (matchLayer == null) {
                    matchLayer = prds.get(0).getLayer("Default");
                }

                if (matchLayer != null) {
                    activeLayer.add(matchLayer.getDrawables());
                }

                // match other layers by name
                int ii = 0;
                for (Layer lyr : prds.get(0).getLayers()) {
                    if (lyr == matchLayer) {
                        layerUsed.set(ii, true);
                    } else {
                        Layer mlyr = activeProduct.getLayer(lyr.getName());
                        if (mlyr != null) {
                            mlyr.add(lyr.getDrawables());
                            layerUsed.set(ii, true);
                        }
                    }

                    ii++;
                }

                // append unused layers
                int jj = 0;
                for (Layer lyr : prds.get(0).getLayers()) {
                    if (!layerUsed.get(jj)) {
                        activeProduct.addLayer(lyr);
                    }

                    jj++;
                }
            }
        }

        startProductManage();

    }

    /**
     * Uses a PgenCommand to remove an element from the product list
     * 
     * @param de
     *            Element to be removed
     */
    public void removeElement(AbstractDrawableComponent adc) {

        /*
         * create a new DeleteElementCommand and send it to the Command Manager
         */
        PgenCommand cmd = new DeleteElementCommand(productList, adc);
        commandMgr.addCommand(cmd);

    }

    /**
     * Deletes all selected elements.
     */
    public void removeElements(List<AbstractDrawableComponent> adc) {

        PgenCommand cmd = new DeleteSelectedElementsCommand(productList, adc);
        commandMgr.addCommand(cmd);

    }

    /**
     * Uses a PgenCommand to remove all elements from the active layer
     * 
     * @param de
     *            Element to be removed
     */
    public void removeAllActiveDEs() {
        /*
         * create a new DeleteAllSelectedElementsCommand with a list of all
         * elements on the active layer and send it to the Command Manager
         */
        PgenCommand cmd = new DeleteSelectedElementsCommand(productList,
                (List<AbstractDrawableComponent>) activeLayer.getDrawables());

        commandMgr.addCommand(cmd);

    }

    /**
     * Uses a PgenCommand to remove all elements from the product list
     * 
     * @param de
     *            Element to be removed
     */
    public void removeAllProducts() {

        /*
         * create a new DeleteAllCommand and send it to the Command Manager
         */
        PgenCommand cmd = new DeleteAllCommand(productList);
        commandMgr.addCommand(cmd);

    }

    /**
     * Uses a PgenCommand to add a DrawableElement to the productList.
     * 
     * @param de
     *            The DrawableElement being added.
     */
    public void addElement(AbstractDrawableComponent de) {

        PgenCommand cmd = new AddElementCommand(productList, activeProduct,
                activeLayer, de);
        commandMgr.addCommand(cmd);

    }

    /**
     * Uses a PgenCommand to add a List of DrawableElements to the productList.
     * 
     * @param elems
     *            List of DrawableElement being added.
     */
    public void addElements(List<AbstractDrawableComponent> elems) {

        PgenCommand cmd = new AddElementsCommand(productList, activeProduct,
                activeLayer, elems);
        commandMgr.addCommand(cmd);

    }

    /**
     * Delete the part between point 1 and point 2 from an muliti-point element
     * 
     * @param mpe
     *            - multi-point element
     * @param pt1
     *            - the first point of the deleting part
     * @param pt2
     *            - the second point of the deleting part
     */
    public void deleteElementPart(Line mpe, Coordinate pt1, Coordinate pt2) {

        PgenCommand cmd = new DeletePartCommand(productList, mpe, pt1, pt2);
        commandMgr.addCommand(cmd);

    }

    /**
     * Start layering if necessary.
     */
    public void startLayering() {

        if (layeringControlDlg != null) {
            layeringControlDlg.close();
        }

        if (!activeLayer.getName().equalsIgnoreCase("Default")
                || activeProduct.getLayers().size() > 1) {

            activateLayering();
        } else {
            PgenUtil.setSelectingMode();
        }

    }

    /**
     * Activate layering control.
     */
    public void activateLayering() {

        if (productManageDlg != null && productManageDlg.isOpen()) {
            productManageDlg.close();
        }

        AbstractEditor mapEditor = PgenUtil.getActiveEditor();

        if (layeringControlDlg == null) {

            Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                    .getShell();
            layeringControlDlg = new PgenLayeringControlDialog(shell);

        }

        PgenUtil.setSelectingMode();

        if (!(layeringControlDlg.isOpen())) {
            layeringControlDlg.open();
        }

        mapEditor.refresh();

    }

    /**
     * @param autoSaveFilename
     *            the autoSaveFilename to set
     */
    public void setAutoSaveFilename(String autoSaveFilename) {
        this.autoSaveFilename = autoSaveFilename;
    }

    /**
     * @param autosave
     *            the autosave to set
     */
    public void setAutosave(boolean autosave) {
        this.autosave = autosave;
    }

    /**
     * If there are no more dataChangedListeners registered with this Data
     * object, clean up command manager stacks and listeners, and determines if
     * data needs to be saved.
     * 
     * @param paneImage
     */
    public synchronized void cleanup() {

        closeDialogs();

        numberOfResources--;
        if (numberOfResources != 0) {
            return; // not ready yet
        }

        int numResourcesinSession = PgenSession.getInstance()
                .getNumberOfSessionResources();

        // if not multi-mode or only one resource remaining in session,
        // close palette
        if (PgenUtil.getPgenMode() != PgenMode.MULTIPLE
                || numResourcesinSession == 1) {
            PgenSession.getInstance().closePalette();
        }

        // decrement number of session resources
        PgenSession.getInstance()
                .setNumberOfSessionResources(--numResourcesinSession);

        commandMgr.flushStacks();
        commandMgr.removeStackListener(this);

        // remove temp recovery file
        removeTempFile();

        if (autosave) {
            storeAllProducts();
        }

        if (PgenUtil.getPgenMode() == PgenMode.SINGLE) {
            PgenUtil.resetResourceData();
        }

        deactivatePgenTools();

    }

    /**
     * De-activates all PGEN tools (called when PGEN resource is removed)
     */
    private void deactivatePgenTools() {
        AbstractVizPerspectiveManager mgr = VizPerspectiveListener
                .getCurrentPerspectiveManager();
        if (mgr != null) {
            for (AbstractModalTool mt : mgr.getToolManager()
                    .getSelectedModalTools()) {
                if (mt instanceof AbstractPgenDrawingTool) {
                    ((AbstractPgenDrawingTool) mt).deactivateTool();
                }
            }
        }
    }

    /*
     * deletes temp recovery file for this pgen resource data
     */
    private void removeTempFile() {

        String filepath = PgenUtil.getTempWorkDir() + File.separator
                + recoveryFilename;

        File tmpFile = new File(filepath);
        if (tmpFile.exists())
            tmpFile.delete();

    }

    /*
     * save a temporary recovery file
     */
    private void recoverySave() {

        String filepath = PgenUtil.getTempWorkDir() + File.separator
                + recoveryFilename;
        PgenResource rsc = PgenSession.getInstance().getPgenResource();
        ArrayList<Product> prds = (ArrayList<Product>) rsc.getProducts();

        Products filePrds = ProductConverter.convert(prds);
        FileTools.write(filepath, filePrds);

    }

    /**
     * Saves all elements in the productList to a given file.
     * 
     * If "multiSave" is true or a product's "saveLayers" flag is configured to
     * be true - each product is saved into a separate file in directory
     * ".../[filename]_post/[product_name].xml" and each layer is save at the
     * same directory as "[layer_name].xml" If a product has a specified output
     * file, it is saved to that given file as well and layers are saved under
     * the directory specified in the output file, if the "saveLayers" is true
     * for the product.
     * 
     * @param filename
     * @param postSave
     *            flag to save individual product and layer for
     *            post-processing.S
     */
    public void saveProducts(String filename, boolean postSave) {

        this.multiSave = postSave;

        ArrayList<Product> prds = (ArrayList<Product>) productList;

        if (filename != null && !prds.isEmpty()) {

            Products filePrds = ProductConverter.convert(prds);

            /*
             * Write all products into one file.
             */
            FileTools.write(filename, filePrds);

            /*
             * Write individual product and its layers into files.
             */
            int lastind = filename.lastIndexOf("/");
            if (lastind < 0) {
                filename = new String(
                        PgenUtil.getWorkingDirectory() + filename);
            }

            String dlftPrdSaveDir;
            if (filename.endsWith(".xml")) {
                dlftPrdSaveDir = new String(
                        filename.substring(0, filename.length() - 4) + "_post");
            } else {
                dlftPrdSaveDir = new String(filename + "_post");
            }

            ArrayList<Product> onePrd = new ArrayList<>();
            for (Product prd : prds) {

                onePrd.clear();

                Product backupPrd = prd.copy();
                onePrd.add(backupPrd);

                // User-configured save
                String givenFile = prd.getOutputFile();
                String givenPrdFile = null;
                if (givenFile != null) {
                    givenPrdFile = givenFile.trim();
                }

                String givenPrdSaveDir = null;
                String givenPrdSaveFile = null;

                if (givenPrdFile != null && givenPrdFile.length() > 0) {

                    int lind = givenPrdFile.lastIndexOf("/");

                    if (givenPrdFile.endsWith(".xml")) {
                        if (lind >= 0) {
                            givenPrdSaveDir = givenPrdFile.substring(0, lind);
                            givenPrdSaveFile = givenPrdFile.substring(lind + 1,
                                    givenPrdFile.length());
                        } else {
                            givenPrdSaveFile = new String(givenPrdFile);
                        }

                        if (givenPrdSaveFile.length() == 4) {
                            givenPrdSaveFile = null;
                        }

                    } else {
                        if (lind >= 0) {
                            givenPrdSaveDir = new String(givenPrdFile);
                        } else {
                            givenPrdSaveDir = new String(
                                    PgenUtil.getWorkingDirectory() + "/"
                                            + givenPrdFile);
                        }
                    }
                }

                // configured save
                Products singlePrd = ProductConverter.convert(onePrd);
                if (givenPrdSaveDir != null) {
                    String outpf = new String(
                            givenPrdSaveDir + "/" + prd.getName() + ".xml");
                    if (givenPrdSaveFile != null) {
                        outpf = new String(
                                givenPrdSaveDir + "/" + givenPrdSaveFile);
                    }

                    FileTools.write(outpf, singlePrd);
                } else {
                    if (givenPrdSaveFile != null) {
                        String outpf = new String(PgenUtil.getWorkingDirectory()
                                + "/" + givenPrdSaveFile);
                        FileTools.write(outpf, singlePrd);
                    }
                }

                // Forced save
                String defaultPrdFile = new String(
                        dlftPrdSaveDir + "/" + prd.getName() + ".xml");

                if (postSave || prd.isSaveLayers()) {

                    // Save single product
                    FileTools.write(defaultPrdFile, singlePrd);

                    // Save each layer
                    for (Layer lyr : prd.getLayers()) {

                        backupPrd.clear();
                        backupPrd.addLayer(lyr);

                        onePrd.clear();
                        onePrd.add(backupPrd);

                        String outlyrfile = new String(
                                dlftPrdSaveDir + "/" + prd.getName() + "_post/"
                                        + lyr.getName() + ".xml");

                        Products oneLayerPrd = ProductConverter.convert(onePrd);
                        FileTools.write(outlyrfile, oneLayerPrd);

                        // save to configured directory if required.
                        if (givenPrdSaveDir != null) {

                            String outpf;
                            if (givenPrdSaveFile != null) {
                                String pname = givenPrdSaveFile.substring(0,
                                        givenPrdSaveFile.length() - 4);
                                outpf = new String(givenPrdSaveDir + "/" + pname
                                        + "_post/" + lyr.getName() + ".xml");
                            } else {
                                outpf = new String(givenPrdSaveDir + "/"
                                        + prd.getName() + "_post/"
                                        + lyr.getName() + ".xml");
                            }

                            FileTools.write(outpf, oneLayerPrd);
                        }

                    }
                }
            }

            needsSaving = false;
        }

    }

    /**
     * Saves all elements in the productList to a given file Also, save each
     * product as a separate file and each layer in each product as a separate
     * file as well.
     * 
     * @param filename
     */
    public void saveProducts_prev(String filename, boolean multiSave) {

        this.multiSave = multiSave;

        ArrayList<Product> prds = (ArrayList<Product>) productList;

        if (filename != null && !prds.isEmpty()) {

            /*
             * Update input/output file names for each product/layer If output
             * file name is not specified, the default file name format for a
             * product is: user-specified file name + "_" + product name +
             * ".xml". the default file name format for a layer is:
             * user-specified file name + "_" + product name + + "_" + layer
             * name + ".xml".
             */
            String infile = null;
            String outfile = null;
            String oneFile = filename.substring(0,
                    filename.lastIndexOf(".xml"));

            for (Product prd : prds) {
                prd.setUseFile(true);
                outfile = prd.getOutputFile();
                if (outfile == null || outfile.trim().length() == 0) {
                    prd.setOutputFile(oneFile + "_" + prd.getName() + ".xml");
                }

                infile = prd.getInputFile();
                if (infile == null || infile.trim().length() == 0) {
                    prd.setInputFile(prd.getOutputFile());
                }

                for (Layer lyr : prd.getLayers()) {
                    outfile = lyr.getOutputFile();
                    if (outfile == null || outfile.trim().length() == 0) {
                        lyr.setOutputFile(oneFile + "_" + prd.getName() + "_"
                                + lyr.getName() + ".xml");
                    }

                    infile = lyr.getInputFile();
                    if (infile == null || infile.trim().length() == 0) {
                        lyr.setInputFile(lyr.getOutputFile());
                    }
                }
            }

            Products filePrds = ProductConverter.convert(prds);

            /*
             * Write products into multiple files. Layers could be written into
             * the their own files if the "saveLayers" flag is true for a
             * product.
             */
            if (multiSave) {
                ArrayList<Product> onePrd = new ArrayList<>();
                for (Product prd : prds) {

                    onePrd.clear();
                    prd.setUseFile(true);

                    Product backupPrd = prd.copy();
                    onePrd.add(backupPrd);

                    FileTools.write(backupPrd.getOutputFile(),
                            ProductConverter.convert(onePrd));

                    // Write out each layer
                    if (prd.isSaveLayers()) {
                        for (Layer lyr : prd.getLayers()) {

                            backupPrd.clear();
                            backupPrd.addLayer(lyr);
                            backupPrd.setInputFile(lyr.getInputFile());
                            backupPrd.setOutputFile(lyr.getOutputFile());

                            onePrd.clear();
                            onePrd.add(backupPrd);

                            FileTools.write(lyr.getOutputFile(),
                                    ProductConverter.convert(onePrd));

                        }

                    }

                }

            }

            /*
             * Write all products into one file.
             */
            FileTools.write(filename, filePrds);
            needsSaving = false;
        }
    }

    /*
     * (non-Javadoc) Invoked by CommandManager when change has been made to the
     * ProductList
     * 
     * @see
     * gov.noaa.nws.ncep.ui.pgen.controls.CommandStackListener#stacksUpdated
     * (int, int)
     */
    @Override
    public void stacksUpdated(int undoSize, int redoSize) {

        if (undoSize + redoSize == 0)
            return;

        needsSaving = true;
        needsDisplay = true;

        // Save current image of pane for possible future reminder
        IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();

        // convert minutes to milliseconds
        autosaveInterval = prefs.getLong(PgenPreferences.P_AUTO_FREQ) * 60
                * 1000;

        // Write out a temporary recovery file
        recoverySave();

        /*
         * If autosave feature is on, and enough time has elapsed, dump the
         * current ProeductList to the autosave file
         */
        if (autosave) {
            long current = System.currentTimeMillis();
            if ((current - lastSaveTime) > autosaveInterval) {
                storeAllProducts();
                lastSaveTime = current;
                needsSaving = false;
            }
        }

        fireChangeListeners(ChangeType.DATA_UPDATE, null);

    }

    /**
     * PGen Exit Dialog, ask the user if they would like to save their PGen work
     * 
     * @param paneImage
     *            ( a screen shot of CAVE at the time of the prompt
     * @return int - code of the button pushed on the dialog
     */

    public int promptToSave(BufferedImage paneImage) {

        int returnCode = 0;

        Image im = null;
        if (paneImage != null) {
            // Create SWT image of the editor pane
            ImageData tmpdata = SymbolImageUtil.convertToSWT(paneImage);
            ImageData idata = tmpdata.scaledTo(tmpdata.width / 2,
                    tmpdata.height / 2);
            im = new Image(PlatformUI.getWorkbench().getDisplay(), idata);
        }

        // Display confirmation dialog
        PgenRemindDialog confirmDlg = new PgenRemindDialog(
                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                im);

        confirmDlg.open();
        returnCode = confirmDlg.getReturnCode();
        return returnCode;
    }

    /**
     * Saves all elements in a product to a configured file.
     * 
     * Layers might be saved into individual files as well if "saveLayers" is
     * true for this product type.
     * 
     */
    public boolean saveOneProduct(Product prd, String outfile) {

        if (prd == null) {
            return false;
        }

        ArrayList<Product> prds = new ArrayList<>();
        prds.add(prd);

        String filename = buildFileName(prd);
        if (outfile != null) {
            filename = new String(outfile);
        }

        prd.setOutputFile(filename);

        // Write all products into one file.
        Products filePrds = ProductConverter.convert(prds);
        FileTools.write(filename, filePrds);

        // Write each layer into a file if requested
        if (prd.isSaveLayers()) {

            Product backupPrd = prd.copy();
            // Save each layer
            for (Layer lyr : prd.getLayers()) {
                backupPrd.clear();
                backupPrd.addLayer(lyr);

                prds.clear();
                prds.add(backupPrd);

                String outlyrfile = new String(
                        filename.substring(0, filename.length() - 4) + "."
                                + lyr.getName() + ".xml");

                Products oneLayerPrd = ProductConverter.convert(prds);
                FileTools.write(outlyrfile, oneLayerPrd);
            }
        }

        needsSaving = false;

        return true;

    }

    /**
     * Saves all elements in the active product to a configured file.
     * 
     * Layers might be saved into individual files as well if "saveLayers" is
     * true for this product type.
     */
    public boolean saveCurrentProduct(String fileName) {

        return saveOneProduct(getActiveProduct(), fileName);
    }

    /**
     * Saves all products to their configured files.
     * 
     * Layers might be saved into individual files as well if "saveLayers" is
     * true for this product type.
     */
    public boolean saveAllProducts() {

        for (Product pp : productList) {
            String ofile = pp.getOutputFile();
            pp.setInputFile(ofile);
            saveOneProduct(pp, ofile);
        }

        return true;
    }

    /**
     * Stores all elements in the active product to EDEX with the given label.
     */
    public boolean storeCurrentProduct(String label) {
        return storeProduct(getActiveProduct(), label);
    }

    /**
     * Stores all products to EDEX using their configured filenames as their
     * labels.
     */
    public boolean storeAllProducts() {

        for (Product pp : productList) {
            String ofile = pp.getOutputFile();
            pp.setInputFile(ofile);
            storeProduct(pp, ofile);
        }

        return true;
    }

    public boolean storeProduct(Product prd, String label) {

        if (prd == null) {
            return false;
        }

        String activityLabel = null;
        if (label != null) {
            activityLabel = new String(label);
        } else {
            activityLabel = buildActivityLabel(prd);
        }

        prd.setOutputFile(activityLabel);

        try {
            StorageUtils.storeProduct(prd);
        } catch (PgenStorageException e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
            return false;
        }

        needsSaving = false;
        return true;
    }

    /**
     * Build a full path file name for a product's configured path/output file
     * name
     * 
     * @param prd
     * @return
     */
    public String buildFileName(Product prd) {

        String sfile = null;
        if (productManageDlg != null) {
            sfile = productManageDlg.getPrdOutputFile(prd);
        } else {

            StringBuilder sdir = new StringBuilder();

            sdir.append(PgenUtil.getPgenOprDirectory() + File.separator
                    + "Default.DDMMYYYY.HH.xml");

            sfile = new String(sdir.toString());
            sfile = PgenUtil.replaceWithDate(sfile, Calendar.getInstance());
        }

        return sfile;
    }

    /**
     * Build an activity name for a product's configured path/output name.
     * 
     * @param prd
     * @return
     */
    public String buildActivityLabel(Product prd) {

        String sfile = null;
        if (productManageDlg != null) {
            String temp = productManageDlg.getPrdOutputFile(prd);
            int idx = temp.lastIndexOf(File.separator);
            sfile = temp.substring(idx + 1);
        } else {
            sfile = PgenUtil.replaceWithDate("Default.DDMMYYYY.HH.xml",
                    Calendar.getInstance());
        }

        return sfile;
    }

    /**
     * Return the "needSaving" flag.
     * 
     * @return
     */
    public boolean isNeedsSaving() {
        return needsSaving;
    }

    /**
     * @param save
     */
    public void setNeedsSaving(boolean save) {
        needsSaving = save;
    }

    /**
     * 
     * @return boolean
     */
    public boolean isNeedsDisplay() {
        return needsDisplay;
    }

    /**
     * 
     * @param needsDisplay
     */
    public void setNeedsDisplay(boolean needsDisplay) {
        this.needsDisplay = needsDisplay;
    }

    /**
     * Check if need to remove the empty "Default" product
     * 
     * @return
     */
    public boolean removeEmptyDefaultProduct() {

        boolean remove = false;
        if (productList.size() == 1
                && productList.get(0).getName().equals("Default")
                && productList.get(0).getType().equals("Default")
                && productList.get(0).getLayers().size() == 1
                && productList.get(0).getLayers().get(0).getName()
                        .equals("Default")
                && productList.get(0).getLayers().get(0).getDrawables()
                        .size() == 0) {
            remove = true;
        }

        return remove;

    }

    /**
     * Switch the active layer to a given layer & update GUI.
     * 
     * @param layerName
     *            layer to switch on
     * @return
     */
    public void switchLayer(String layerName) {
        if (productManageDlg != null && productManageDlg.isOpen()) {
            productManageDlg.switchLayer(layerName);
        } else if (layeringControlDlg != null && layeringControlDlg.isOpen()) {
            layeringControlDlg.switchLayer(layerName);
        }
    }

    @Override
    public boolean isResourceDataDirty() {
        return true;
    }

    @Override
    public int promptToSaveOnCloseResourceData() {
        if ((PgenUtil.getPgenMode() == PgenMode.MULTIPLE)
                && (PgenSession.getInstance().getPgenPalette() != null)
                || ((PgenUtil.getPgenMode() == PgenMode.SINGLE && PgenSession
                        .getInstance().getPgenResourceData() == this))) {
            return PgenSession.getInstance().getPgenPalette()
                    .promptToSaveOnClose();
        } else {
            return ISaveablePart2.DEFAULT;
        }

    }

    @Override
    public void doSaveResourceData(IProgressMonitor monitor) {
        if (PgenSession.getInstance().getPgenPalette() != null) {
            PgenSession.getInstance().getPgenPalette().doSave(monitor);
        }
    }

}
