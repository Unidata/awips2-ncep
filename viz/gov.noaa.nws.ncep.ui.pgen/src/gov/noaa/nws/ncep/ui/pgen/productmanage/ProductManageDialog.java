package gov.noaa.nws.ncep.ui.pgen.productmanage;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.PlatformUI;

import com.raytheon.uf.viz.core.exception.VizException;

import gov.noaa.nws.ncep.ui.pgen.PgenSession;
import gov.noaa.nws.ncep.ui.pgen.PgenUtil;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.AttrSettings;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.GfaAttrDlg;
import gov.noaa.nws.ncep.ui.pgen.attrdialog.OutlookAttrDlg;
import gov.noaa.nws.ncep.ui.pgen.controls.StoreActivityDialog;
import gov.noaa.nws.ncep.ui.pgen.elements.Layer;
import gov.noaa.nws.ncep.ui.pgen.elements.Product;
import gov.noaa.nws.ncep.ui.pgen.elements.ProductInfo;
import gov.noaa.nws.ncep.ui.pgen.elements.ProductTime;
import gov.noaa.nws.ncep.ui.pgen.producttypes.PgenLayer;
import gov.noaa.nws.ncep.ui.pgen.producttypes.PgenSave;
import gov.noaa.nws.ncep.ui.pgen.producttypes.ProductType;
import gov.noaa.nws.ncep.ui.pgen.rsc.PgenResourceData;

/**
 * gov.noaa.nws.ncep.ui.pgen.productManage.ProductManageDialog his code has been
 * developed by the NCEP/SIB for use in the AWIPS2 system.
 *
 * This class provides a dialog to manage PGEN products in National Centers
 * perspective.
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- -----------  --------------------------
 * 09/09        #151        J. Wu       Initial creation.
 * 09/10        #151        J. Wu       Updated with the layer configuration.
 * 01/11        #151        J. Wu       Simplified output for post-processing
 * 09/11        #335        J. Wu       Added file auto storage/acccess
 * 09/11        #335        J. Wu       made cascading menu for activity type/subtype.
 * 06/12        TTR253      J. Wu       made layer check boxes to stay de-selected
 *                                      unless the user selects them.
 * 06/12        TTR559      B. Yin      Link the layer name to Outlook type
 * 12/12        #937        J. Wu       Update G_Airmet layers/hazard - "C&V"
 * 09/13        ?           J. Wu       Use new "StoreActivityDialog" at exit.
 * 11/13        #1049       B. Yin      Handle outlook type defined in layer.
 * 08/14        TTR962      J. Wu       Format output file with DD, MM, YYYY, HH.
 * 06/15        R8189       J. Wu       Set Pgen palette per layer.
 * 12/21/2015   R12964      J. Lopez    Layers remember the last selected class
 * 05/02/2016   R16076      J. Wu       pull getPrdOutputFile() to PgenUtil.
 * 05/10/2016   R13560      S. Russell  Updated exitProductManage() and renamed
 *                                      it to exitPGenActivityManagement()
 * 06/29/2016   R18611      S. Russell  Updated exitPgenActivityManagement() to
 *                                      add code to set the needsSaving flag
 *                                      to false after all products are removed
 * 09/06/2019   #64146      K. sunil    Display all activities and not just the one in use..
 * 09/20/2019   #69091      K. sunil    When new activity is created through "New" button, keep
 *                                       existing activities checked.
 * </pre>
 *
 * @author jwu
 * @version 1.0
 *
 */

public class ProductManageDialog extends ProductDialog {

    /**
     * List of products and buttons.
     */
    private ArrayList<Product> prdList = null;

    private ArrayList<Text> prdTypeTexts = null;

    private ArrayList<Button> prdNameBtns = null;

    private ArrayList<Button> prdDispOnOffBtns = null;

    protected ProductNameDialog prdNameDlg = null;

    protected ProductFileNameDialog prdFileInOutDlg = null;

    private Button prodAllOnOffBtn = null;

    private boolean prodAllOnOff = false;

    private int prdInUse = -1;

    boolean openPrdNameDialog = false;

    protected LinkedHashMap<String, ProductType> prdTypesMap = null;

    /**
     * Default colors for the default and active product of layer name button.
     */
    private final Color defaultButtonColor = Color.lightGray;

    private final Color activeButtonColor = Color.green;

    /**
     * Layer name edit dialog.
     */
    protected LayeringNameDialog layerNameDlg = null;

    protected LayeringDisplayDialog displayDlg = null;

    protected LayeringLpfFileDialog layerLpfFileDlg = null;

    /**
     * List of layers and buttons.
     */
    private ArrayList<Layer> layerList = null;

    private Composite layersComp = null;

    private ArrayList<Button> layerNameBtns = null;

    private ArrayList<Button> displayOnOffBtns = null;

    private ArrayList<Button> colorModeBtns = null;

    private Button allOnOffBtn = null;

    /**
     * The layer & color mode button in use.
     */
    private int layerInUse = -1;

    private int colorModeBtnInUse = -1;

    private boolean allOnOff = false;

    /**
     * Open dialog in compact mode or full mode.
     */
    private Button arrowBtn = null;

    boolean compact = true;

    boolean openLayerNameDialog = false;

    /**
     * Constructor.
     */
    public ProductManageDialog(Shell parentShell) {
        super(parentShell);
    }

    /**
     * Sets the title of the dialog.
     */
    public void setTitle() {
        shell.setText("Activity Center");
    }

    /**
     * Set the default location.
     * 
     * @param parent
     */
    public void setDefaultLocation(Shell parent) {

        if (shellLocation == null) {
            Point pt = parent.getLocation();
            shell.setLocation(pt.x + 255, pt.y + 146);
        } else {
            shell.setLocation(shellLocation);
        }

    }

    /**
     * Pops up a second dialog - the window to edit the layer name.
     */
    protected void popupSecondDialog() {
        if (openPrdNameDialog) {
            editProductAttr();
        } else if (openLayerNameDialog) {
            editLayerName();
        }

    }

    /**
     * Initialize the dialog components.
     */
    public void initializeComponents() {

        // Initialize the product in PgenResource.
        initialize();

        // Load the product types.
        prdTypesMap = ProductConfigureDialog.getProductTypes();

        // Create product control part.
        createProductPart();
        addSeparator();
        addSeparator();

        // Create layering control part.
        createLayeringPart();
        addSeparator();
        addSeparator();

        // Create "Exit" and expansion buttons.
        createExitPart();

        // Set PGEN palette based on the curent product's type.
        resetPalette(currentProduct, currentLayer);

        // load settings
        AttrSettings.getInstance().loadProdSettings(currentProduct.getType());

    }

    /**
     * Initialize the product in the PgenResource.
     */
    private void initialize() {

        prdList = (ArrayList<Product>) drawingLayer.getProducts();

        if (currentProduct == null) {

            currentProduct = new Product("Default", "Default", "Default",
                    new ProductInfo(), new ProductTime(),
                    new ArrayList<Layer>());

            currentProduct.setOnOff(false);

            drawingLayer.addProduct(currentProduct);
            drawingLayer.setActiveProduct(currentProduct);
        } else {
            if (prdList.contains(currentProduct))
                prdInUse = prdList.indexOf(currentProduct);
            else
                prdInUse = -1;
        }

        layerList = (ArrayList<Layer>) currentProduct.getLayers();

        if (currentLayer == null) {

            currentLayer = new Layer();

            currentProduct.addLayer(currentLayer);

            currentLayer.setOnOff(false);

            drawingLayer.setActiveLayer(currentLayer);
        } else {
            if (layerList.contains(currentLayer))
                layerInUse = layerList.indexOf(currentLayer);
            else
                layerInUse = -1;
        }

        currentLayer.setInUse(true);

        prdNameBtns = new ArrayList<>();
        prdDispOnOffBtns = new ArrayList<>();
        prdTypeTexts = new ArrayList<>();
        layerNameBtns = new ArrayList<>();
        displayOnOffBtns = new ArrayList<>();
        colorModeBtns = new ArrayList<>();
    }

    /**
     * Create the product control GUI
     */
    private void createProductPart() {

        // Create a title
        Composite titleComp = new Composite(shell, SWT.NONE);

        GridLayout gl0;
        if (compact) {
            gl0 = new GridLayout(1, true);
        } else {
            gl0 = new GridLayout(2, true);
        }

        gl0.makeColumnsEqualWidth = false;

        titleComp.setLayout(gl0);

        Label prds = new Label(titleComp, SWT.NONE);
        prds.setText("Activities:");

        addSeparator();

        // Create add/All On/delete buttons
        Composite addProdComp = new Composite(shell, SWT.NONE);

        int numActionBtns;
        if (compact) {
            numActionBtns = 2;
        } else {
            numActionBtns = 3;
        }

        GridLayout gl = new GridLayout(numActionBtns, true);

        gl.makeColumnsEqualWidth = false;
        gl.marginHeight = 1;
        gl.marginWidth = 1;
        gl.verticalSpacing = 1;
        gl.horizontalSpacing = 1;
        addProdComp.setLayout(gl);

        Button addPrdBtn = new Button(addProdComp, SWT.NONE);
        addPrdBtn.setText("New");
        addPrdBtn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                addProduct();
            }
        });

        prodAllOnOffBtn = new Button(addProdComp, SWT.NONE);
        prodAllOnOffBtn.setText("All On");
        prodAllOnOffBtn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                updateProdDispChecks();
            }
        });

        // Create "Delete Layer" button.
        if (!compact) {

            Button delPrdBtn = new Button(addProdComp, SWT.NONE);
            delPrdBtn.setText("Delete");
            delPrdBtn.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent event) {
                    deleteProduct();
                }
            });
        }

        addSeparator();

        // Create buttons for product name, display on/off, and type.
        createProducts();

    }

    /**
     * Add a separator.
     */
    private void addSeparator() {
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        Label sepLbl = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
        sepLbl.setLayoutData(gd);
    }

    /*
     * Create name, on/off, and type buttons for one product
     */
    private void createProducts() {

        // Save the selected PGEN class to the current layer
        savePGENClass();

        Composite prdsComp = new Composite(shell, SWT.NONE);

        int numActionBtns;
        if (compact) {
            numActionBtns = 2;
        } else {
            numActionBtns = 3;
        }

        GridLayout gl = new GridLayout(numActionBtns, true);
        ;
        gl.makeColumnsEqualWidth = false;
        gl.marginHeight = 1;
        gl.marginWidth = 1;
        gl.verticalSpacing = 1;
        gl.horizontalSpacing = 1;

        prdsComp.setLayout(gl);

        if (prdInUse < 0 || prdInUse >= prdList.size()) {
            prdInUse = 0;
            layerInUse = 0;
            currentProduct = prdList.get(prdInUse);
            currentProduct.setInUse(true);
            drawingLayer.setActiveProduct(currentProduct);

            layerList = (ArrayList<Layer>) currentProduct.getLayers();
            currentLayer = currentProduct.getLayer(layerInUse);
            currentLayer.setInUse(true);
            drawingLayer.setActiveLayer(currentLayer);

        }

        int ii = 0;
        for (Product prd : prdList) {

            // Populate the "saveLayers" flag in the product type to the product
            // level.
            ProductType ptyp = prdTypesMap.get(prd.getType());
            if (ptyp != null && ptyp.getPgenSave() != null) {
                prd.setSaveLayers(ptyp.getPgenSave().isSaveLayers());
            }

            Button nameBtn = new Button(prdsComp, SWT.PUSH);
            nameBtn.setText(prd.getName());

            if (ii == prdInUse) {
                setButtonColor(nameBtn, activeButtonColor);
            } else {
                setButtonColor(nameBtn, defaultButtonColor);
            }

            nameBtn.setData(ii);
            nameBtn.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent event) {

                    int iprd = Integer
                            .parseInt(event.widget.getData().toString());

                    if (prdInUse == iprd) {

                        openPrdNameDialog = true;
                        openLayerNameDialog = false;

                        if (layerNameDlg != null)
                            layerNameDlg.close();

                        editProductAttr();

                    } else {
                        switchProduct(Integer
                                .parseInt(event.widget.getData().toString()));
                    }

                }
            });

            prdNameBtns.add(nameBtn);

            Button dispBtn = new Button(prdsComp, SWT.CHECK);
            dispBtn.setSelection(prd.isOnOff());
            dispBtn.setData(ii);
            dispBtn.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent event) {
                    turnOnProduct(Integer
                            .parseInt(event.widget.getData().toString()));

                }
            });

            prdDispOnOffBtns.add(dispBtn);

            if (!compact) {

                Composite typeComp = new Composite(prdsComp, SWT.RIGHT);
                typeComp.setLayout(new GridLayout(2, false));

                final Text typeText = new Text(typeComp, SWT.LEFT | SWT.BORDER);
                typeText.setSize(200, 20);
                typeText.setText(prdList.get(ii).getType());
                typeText.setData(ii);
                typeText.setEditable(false);
                prdTypeTexts.add(typeText);

                final ToolBar tb = new ToolBar(typeComp, SWT.HORIZONTAL);
                final ToolItem ti = new ToolItem(tb, SWT.DROP_DOWN);

                ti.setEnabled(true);

                final Menu mu = new Menu(shell.getShell(), SWT.POP_UP);

                MenuItem mi1 = new MenuItem(mu, SWT.PUSH, 0);
                mi1.setText("Default");
                mi1.setData("Default");
                mi1.addSelectionListener(new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent e) {
                        typeText.setText(
                                ((MenuItem) e.widget).getData().toString());
                        typeText.pack();
                        shell.pack();
                        switchProductType(typeText);
                    }
                });

                int ntyp = 1;
                ArrayList<String> typeUsed = new ArrayList<>();
                for (String ptypName : prdTypesMap.keySet()) {

                    ProductType prdType = prdTypesMap.get(ptypName);
                    LinkedHashMap<String, String> subtypesNalias = getSubtypes(
                            prdType.getType(), true);

                    if ((ptypName.equals(prdType.getName())
                            && !prdType.getType().equals(prdType.getName()))
                            || !hasSubtypes(subtypesNalias.values())) {

                        MenuItem typeItem = new MenuItem(mu, SWT.PUSH, ntyp);

                        typeItem.setText(ptypName);
                        typeItem.setData(ptypName);
                        typeItem.addSelectionListener(new SelectionAdapter() {
                            public void widgetSelected(SelectionEvent e) {
                                String typeName = ((MenuItem) e.widget)
                                        .getData().toString();
                                typeText.setText(typeName);
                                typeText.pack();
                                shell.pack();
                                switchProductType(typeText);
                            }
                        });

                    } else {

                        if (typeUsed.contains(prdType.getType())) {
                            continue;
                        } else {
                            typeUsed.add(prdType.getType());

                        }

                        MenuItem typeItem = new MenuItem(mu, SWT.CASCADE, ntyp);

                        typeItem.setText(prdType.getType());
                        Menu submenu = new Menu(typeItem);
                        typeItem.setMenu(submenu);

                        for (String styp : subtypesNalias.keySet()) {
                            MenuItem subtypeItem = new MenuItem(submenu,
                                    SWT.PUSH);
                            subtypeItem.setText(subtypesNalias.get(styp));

                            subtypeItem.setData(styp);

                            subtypeItem.addSelectionListener(
                                    new SelectionAdapter() {
                                        public void widgetSelected(
                                                SelectionEvent e) {
                                            String typeName = ((MenuItem) e.widget)
                                                    .getData().toString();
                                            typeText.setText(typeName);
                                            typeText.pack();
                                            shell.pack();
                                            switchProductType(typeText);
                                        }
                                    });
                        }
                    }

                    ntyp++;
                }

                ti.addListener(SWT.Selection, new Listener() {
                    public void handleEvent(Event event) {
                        Rectangle bounds = ti.getBounds();
                        Point point = tb.toDisplay(bounds.x,
                                bounds.y + bounds.height);
                        mu.setLocation(point);
                        mu.setVisible(true);
                    }
                });

            }

            ii++;

        }

    }

    /**
     * Create the layering control GUI
     */
    private void createLayeringPart() {

        Composite titleComp = new Composite(shell, SWT.NONE);

        Label lyrs = new Label(titleComp, SWT.NONE);
        lyrs.setText("Layers:");

        GridLayout gl0 = new GridLayout(1, true);
        titleComp.setLayout(gl0);

        addSeparator();

        Composite addLayerComp = new Composite(shell, SWT.NONE);

        int numActionBtns;
        if (compact) {
            numActionBtns = 2;
        } else {
            numActionBtns = 3;
        }

        GridLayout gl = new GridLayout(numActionBtns, true);
        gl.makeColumnsEqualWidth = false;
        gl.marginHeight = 1;
        gl.marginWidth = 1;
        gl.verticalSpacing = 1;
        gl.horizontalSpacing = 1;
        addLayerComp.setLayout(gl);

        Button addLayerBtn = new Button(addLayerComp, SWT.NONE);
        addLayerBtn.setText("New");
        addLayerBtn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                addLayer();
            }
        });

        // Create "All On" button
        allOnOffBtn = new Button(addLayerComp, SWT.NONE);
        allOnOffBtn.setText("All On");
        allOnOffBtn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                updateLayeringDisplayChecks();
            }
        });

        // Create "Delete Layer" button.
        if (!compact) {

            Button delLayerBtn = new Button(addLayerComp, SWT.NONE);
            delLayerBtn.setText("Delete");
            delLayerBtn.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent event) {
                    deleteLayer();
                }
            });
        }

        addSeparator();

        // Create buttons for layer name, display on/off, and color/fill mode.
        createLayers();

    }

    /*
     * Create name, on/off, and color mode buttons for one layer
     */
    private void createLayers() {

        // Save the selected PGEN class to the current layer
        savePGENClass();

        if (layerList == null || layerList.size() == 0) {
            return;
        }

        layersComp = new Composite(shell, SWT.NONE);
        GridLayout gl = new GridLayout(3, false);
        if (compact) {
            gl = new GridLayout(2, false);
        }

        gl.marginHeight = 1;
        gl.marginWidth = 1;
        gl.verticalSpacing = 1;
        gl.horizontalSpacing = 0;

        layersComp.setLayout(gl);

        int ii = 0;
        for (Layer lyr : layerList) {

            Button nameBtn = new Button(layersComp, SWT.PUSH);
            nameBtn.setText(lyr.getName().replace("&", "&&"));
            setButtonColor(nameBtn, defaultButtonColor);
            nameBtn.setData(ii);
            nameBtn.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent event) {
                    int ilayer = Integer
                            .parseInt(event.widget.getData().toString());
                    if (layerInUse == ilayer) {

                        openPrdNameDialog = false;
                        openLayerNameDialog = true;

                        if (prdNameDlg != null)
                            prdNameDlg.close();
                        editLayerName();

                    } else {
                        switchLayer(ilayer);
                    }
                }
            });

            layerNameBtns.add(nameBtn);

            Button dispBtn = new Button(layersComp, SWT.CHECK);

            dispBtn.setSelection(lyr.isOnOff());
            dispBtn.setData(ii);
            dispBtn.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent event) {
                    turnOnLayer(Integer
                            .parseInt(event.widget.getData().toString()));
                }
            });

            displayOnOffBtns.add(dispBtn);

            if (!compact) {
                Button clrBtn = new Button(layersComp, SWT.PUSH);
                clrBtn.setText(
                        getDisplayString(lyr.isMonoColor(), lyr.isFilled()));
                setButtonColor(clrBtn, lyr.getColor());
                clrBtn.setData(ii);

                clrBtn.addSelectionListener(new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent event) {
                        colorModeBtnInUse = Integer
                                .parseInt(event.widget.getData().toString());
                        editDisplayAttr();
                    }

                });

                colorModeBtns.add(clrBtn);

            }

            ii++;

        }

        if (layerInUse < 0 || layerInUse >= layerList.size()) {
            layerInUse = layerList.size() - 1;
        }

        setButtonColor(layerNameBtns.get(layerInUse), activeButtonColor);

        currentLayer = layerList.get(layerInUse);

        // Selects PGEN class from the current layer
        selectPGENClass();

        drawingLayer.setActiveLayer(currentLayer);

    }

    /**
     * Saves the selected PGEN class in the current layer
     */
    private void savePGENClass() {
        currentLayer.setCategory(PgenSession.getInstance().getPgenPalette()
                .getCurrentCategory());

    }

    /**
     * Selects PGEN class from the current layer
     */
    private void selectPGENClass() {
        PgenSession.getInstance().getPgenPalette()
                .setCurrentCategory(currentLayer.getCategory(), true);

    }

    /*
     * Update the active layer with a new name;
     */
    protected void updateActiveLayerName(String name) {

        boolean update = false;

        /*
         * Update only if the new layer name is not empty and not the same as
         * the layer names on the current product. Also, the name should not be
         * any variations of "Default".
         * 
         * Note: we assume the layer names should be unique within a product.
         */
        if (name != null && name.length() > 0) {
            update = true;
            for (Layer lyr : currentProduct.getLayers()) {
                if (lyr.getName().equals(name)) {
                    update = false;
                    break;
                }
            }
        }

        /*
         * Rebuilds and opens the layering control dialog since the size of the
         * button will change with the length of the new name.
         */
        openLayerNameDialog = false;
        if (update) {

            layerNameBtns.get(layerInUse).setText(name);

            if (layerInUse >= 0) {
                layerList.get(layerInUse).setName(name);
                currentLayer.setName(name);
            }

            drawingLayer.setActiveLayer(currentLayer);
            startPGenActivityManagement();
        }

    }

    /**
     * Update active layer's colorMode, color and fill mode;
     */
    protected void updateDisplayAttr(boolean mono, Color clr, boolean fill) {

        if (colorModeBtnInUse >= 0) {
            colorModeBtns.get(colorModeBtnInUse)
                    .setText(getDisplayString(mono, fill));

            layerList.get(colorModeBtnInUse).setMonoColor(mono);
            layerList.get(colorModeBtnInUse).setColor(clr);

            setButtonColor(colorModeBtns.get(colorModeBtnInUse), clr);

            layerList.get(colorModeBtnInUse).setFilled(fill);

        }

    }

    /**
     * Retrieve active layer;
     */
    protected Layer getActiveLayer() {

        return currentLayer;

    }

    /**
     * Retrieve the layer associated with the color mode button;
     */
    protected Layer getLayerForColorMode() {

        return layerList.get(colorModeBtnInUse);

    }

    /**
     * Edit a selected layer's name
     */
    private void editLayerName() {

        /*
         * Pop up layer name editing window
         */
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getShell();
        if (layerNameDlg == null)
            layerNameDlg = new LayeringNameDialog(shell, this);

        cleanupDialogs();

        layerNameDlg.open();
    }

    /**
     * Edit a selected layer's display attributes ( color and fill mode)
     */
    private void editDisplayAttr() {

        /*
         * Pop up layer name editing window
         */
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getShell();
        if (displayDlg == null)
            displayDlg = new LayeringDisplayDialog(shell, this);

        cleanupDialogs();

        displayDlg.open();

    }

    /*
     * Add a new layer.
     */
    private void addLayer() {

        // Save the selected PGEN class to the current layer
        savePGENClass();

        // Set the flag to pop up the layer name editing dialog.
        openLayerNameDialog = true;
        openPrdNameDialog = false;

        cleanupDialogs();

        // Construct a unique layer name.
        int size1 = layerList.size() + 1;
        String name = new String("Layer_" + size1);

        for (int ii = 0; ii < layerList.size(); ii++) {
            if (name.equals(layerList.get(ii).getName())) {
                name = new String("Layer_" + (size1++));
                ii = 0;
            }
        }

        /*
         * Reset the previous active layer's display status based on its display
         * option button.
         */
        layerList = (ArrayList<Layer>) currentProduct.getLayers();
        boolean ponoff = displayOnOffBtns.get(layerInUse).getSelection();
        layerList.get(layerInUse).setOnOff(ponoff);

        // Create a new layer and set as the new active layer.
        currentLayer = new Layer();
        currentLayer.setName(name);

        drawingLayer.setActiveLayer(currentLayer);
        currentProduct.addLayer(currentLayer);

        layerInUse = layerList.size() - 1;

        // Re-open the layering control dialog.
        startPGenActivityManagement();

    }

    /**
     * Switch to a given layer (used for switching from GFA hazard type).
     */
    public void switchLayer(String newLayer) {
        String clayer = layerList.get(layerInUse).getName();
        int which = -1;

        if (!newLayer.equals(clayer)) {
            for (int ii = 0; ii < layerNameBtns.size(); ii++) {
                if (layerNameBtns.get(ii).getText().equals(newLayer)) {
                    which = ii;
                    break;
                }
            }

            if (which >= 0) {
                switchLayer(which);
            }
        }

    }

    /**
     * Switch between layer.
     */
    private void switchLayer(int which) {

        // Save the selected PGEN class to the current layer
        savePGENClass();

        /*
         * Reset the previous active layer's display status based on its display
         * option button.
         */
        boolean ponoff = displayOnOffBtns.get(layerInUse).getSelection();
        layerList.get(layerInUse).setOnOff(ponoff);

        // Switch the color for the active layers
        setButtonColor(layerNameBtns.get(layerInUse), defaultButtonColor);
        layerInUse = which;

        setButtonColor(layerNameBtns.get(layerInUse), activeButtonColor);

        openPrdNameDialog = false;
        openLayerNameDialog = false;

        cleanupDialogs();

        currentLayer = layerList.get(layerInUse);

        drawingLayer.setActiveLayer(currentLayer);

        drawingLayer.removeGhostLine();

        if (GfaAttrDlg.getInstance(this.getParent()).isGfaOpen()) {
            if (drawingLayer.getSelectedDE() != null) {
                GfaAttrDlg.getInstance(this.getParent()).close();
            } else {
                GfaAttrDlg.getInstance(this.getParent())
                        .switchHazard(currentLayer.getName());
            }
        } else if (OutlookAttrDlg.getInstance(this.getParent())
                .getShell() != null) {
            if (drawingLayer.getSelectedDE() != null) {
                OutlookAttrDlg.getInstance(this.getParent()).close();
            } else {
                OutlookAttrDlg.getInstance(this.getParent())
                        .setOtlkType(currentLayer.getMetaInfoFromKey(
                                OutlookAttrDlg.OTLK_TYPE_IN_LAYER_META));
            }
        } else {
            PgenUtil.setSelectingMode();
        }

        drawingLayer.removeSelected();

        // Reset PGEN palette
        resetPalette(currentProduct, currentLayer);

        // Reset undo/redo and refresh
        PgenSession.getInstance().disableUndoRedo();

        PgenUtil.refresh();

    }

    /**
     * Turn on/off the display check button for a layer.
     */
    private void turnOnLayer(int which) {

        if (which != layerInUse) {
            layerList.get(which)
                    .setOnOff(displayOnOffBtns.get(which).getSelection());
        }

        PgenUtil.refresh();
    }

    /**
     * Toggle the display on/off for all check buttons.
     */
    private void updateLayeringDisplayChecks() {

        if (allOnOff) {
            allOnOff = false;
            allOnOffBtn.setText("All On");
        } else {
            allOnOff = true;
            allOnOffBtn.setText("All Off");
        }

        for (int ii = 0; ii < layerList.size(); ii++) {
            displayOnOffBtns.get(ii).setSelection(allOnOff);
            layerList.get(ii).setOnOff(allOnOff);
        }

        PgenUtil.refresh();

    }

    /**
     * Return the name of the layer on which the color mode button is clicked.
     */
    protected String getColorModeLayerName() {

        if (layerList != null && colorModeBtnInUse >= 0) {
            return layerList.get(colorModeBtnInUse).getName();
        }

        return null;
    }

    /**
     * Exit layering.
     */
    private void exitLayering() {

        /*
         * Create a new Default layer & move all DEs onto this layer.
         */
        currentLayer = new Layer();

        for (int ii = 0; ii < currentProduct.getLayers().size(); ii++) {
            currentLayer.add(currentProduct.getLayer(ii).getDrawables());
        }

        /*
         * Clear out the active product & add the new layer into it.
         */
        currentProduct.clear();

        currentProduct.addLayer(currentLayer);

        /*
         * Set the new layer & product as the active in the PgenResource.
         */
        drawingLayer.setActiveLayer(currentLayer);
        drawingLayer.setActiveProduct(currentProduct);

        layerList = (ArrayList<Layer>) currentProduct.getLayers();

        /*
         * Refresh display and reset undo/redo as well.
         */
        PgenUtil.refresh();
        // PgenUtil.resetUndoRedo();
        PgenSession.getInstance().disableUndoRedo();

        /*
         * Dispose all layering dialogs.
         */
        if (layerNameDlg != null)
            layerNameDlg.close();
        if (displayDlg != null)
            displayDlg.close();
        if (layerLpfFileDlg != null)
            layerLpfFileDlg.close();

        close();

    }

    /**
     * Delete the current layer.
     */
    private void deleteLayer() {

        // Set the flag - do not pop up the layer name editing dialog.
        openLayerNameDialog = false;

        // Remove the current layer & set the next layer as the active
        if (currentProduct.getLayers().size() > 1) {
            currentProduct.removeLayer(layerInUse);
        }

        if (layerInUse >= currentProduct.getLayers().size()) {
            layerInUse--;
        }

        currentLayer = currentProduct.getLayer(layerInUse);

        // currentLayer.setOnOff( true );

        drawingLayer.setActiveLayer(currentLayer);

        layerList = (ArrayList<Layer>) currentProduct.getLayers();

        // Selects PGEN class from the current layer
        selectPGENClass();

        // Re-open the layering dialog.
        startPGenActivityManagement();

        PgenUtil.refresh();

    }

    /**
     * Open the dialog.
     */
    private void startPGenActivityManagement() {

        // Close dialogs.
        cleanupDialogs();

        // Close the dialog first.
        if (isOpen()) {
            close();
        }

        // Build & open the product control dialog.
        open();

    }

    /**
     * Turn on/off the display check button for a layer.
     */
    private void turnOnProduct(int which) {

        if (which != prdInUse) {
            prdList.get(which)
                    .setOnOff(prdDispOnOffBtns.get(which).getSelection());
        }

        PgenUtil.refresh();

    }

    /**
     * Exit the PGen Activity Management Center. Call a dialog asking the user
     * if they would like to save their changes to the Activity Management
     * Center dialogs.
     */
    private void exitPgenActivityManagement() {

        int returnCode = 0;

        // If there are unsaved Activity Management Center changes
        if (needSaving()) {

            // Launch a dialog box asking the user if they want to save their
            // work
            MessageDialog confirmDlg = new MessageDialog(
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                            .getShell(),
                    "Confirm Exit from Activity Management", null,
                    "Do you want to save the changes?", MessageDialog.QUESTION,
                    new String[] { "Yes", "No", "Cancel" }, 0);

            returnCode = confirmDlg.open();

            // If the "Yes" button is pushed, bring up the diaglog to save
            // the work
            if (returnCode == MessageDialog.OK) {

                StoreActivityDialog storeDlg = null;
                if (storeDlg == null) {
                    try {
                        storeDlg = new StoreActivityDialog(shell, "Save As");
                    } catch (VizException e) {
                        e.printStackTrace();
                    }
                }

                if (storeDlg != null)
                    storeDlg.open();

            }

        }

        /*
         * If the Cancel button is pushed, don't save anything, and stop the
         * process of shutting down the Activity Managment Center.
         * 
         * Note: JFace MessageDialogs assign int values to buttons from left to
         * right.
         * 
         * Yes,No,Cancel becomes 0,1,2 respecitively. If Cancel was first it
         * would be 0, not 2. *
         * 
         * Could not find any Eclipse enums or contstant where "Cancel" is 2, so
         * a raw int is used in this conditional
         */

        // If the Cancel button was pushed
        if (returnCode == 2) {
            // Abort the shutdown process of the Activity Management Center
            return;
        }

        /*
         * Reset to "Default" product with a "Default" Layer.
         */

        // If Only one "Default" product
        if (drawingLayer.getProducts().size() == 1
                && currentProduct.getName().equalsIgnoreCase("Default")
                && currentProduct.getType().equalsIgnoreCase("Default")) {

            exitLayering();

            if (prdNameDlg != null)
                prdNameDlg.close();

        } else { // Multiple products

            drawingLayer.removeAllProducts();

            currentProduct = new Product("Default", "Default", "Default",
                    new ProductInfo(), new ProductTime(),
                    new ArrayList<Layer>());

            currentProduct.setType("Default");

            currentLayer = new Layer();
            currentProduct.addLayer(currentLayer);

            drawingLayer.addProduct(currentProduct);
            drawingLayer.setActiveProduct(currentProduct);
            drawingLayer.setActiveLayer(currentLayer);

            // Refresh display and reset undo/redo as well.
            PgenUtil.refresh();

            PgenSession.getInstance().disableUndoRedo();

            // All Products ( including drawables ) were just removed and
            // new empty layers were created. Nothing ( objects draw on the
            // map ) to save. Reset the needsSaving flag to false
            PgenResourceData prd = drawingLayer.getResourceData();
            if (prd != null) {
                prd.setNeedsSaving(false);
            }

            // Dispose all layering dialogs.
            cleanupDialogs();
            close();

        }

        // reset the output file to null
        currentProduct.setOutputFile(null);

        /*
         * Reset PGEN palette and dialog flags.
         */
        refreshPgenPalette(null);
        compact = true;
        openLayerNameDialog = false;
        openPrdNameDialog = false;

    }

    /**
     * Exit product management.
     */
    private void createExitPart() {

        Composite exitComp = new Composite(shell, SWT.NONE);

        GridLayout gl = new GridLayout(2, false);
        gl.marginHeight = 1;
        gl.marginWidth = 1;
        gl.verticalSpacing = 1;
        gl.horizontalSpacing = 2;

        exitComp.setLayout(gl);

        Button exitBtn = new Button(exitComp, SWT.NONE);
        exitBtn.setText("Exit");
        exitBtn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                exitPgenActivityManagement();
            }
        });

        arrowBtn = new Button(exitComp, SWT.NONE);

        if (compact) {
            arrowBtn.setText(">>");
        } else {
            arrowBtn.setText("<<");
        }

        arrowBtn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {

                openLayerNameDialog = false;

                if (compact) {
                    arrowBtn.setText("<<");
                } else {
                    arrowBtn.setText(">>");
                }

                compact = !compact;

                openPrdNameDialog = false;
                openLayerNameDialog = false;

                cleanupDialogs();

                startPGenActivityManagement();

            }
        });

    }

    /**
     * Add a new product.
     */
    private void addProduct() {

        // Save the selected PGEN class to the current layer
        savePGENClass();
        // Set the flag to pop up the product editing dialog.
        openPrdNameDialog = true;
        openLayerNameDialog = false;

        cleanupDialogs();

        // Construct a unique product name.
        String name = new String("Default");

        // Create a new product and set as the new active product.
        if (currentProduct != null) {
            currentProduct.setInUse(false);
        }

        currentProduct = new Product();
        currentProduct.setName(name);

        drawingLayer.addProduct(currentProduct);
        drawingLayer.setActiveProduct(currentProduct);

        prdList = (ArrayList<Product>) drawingLayer.getProducts();

        prdInUse = prdList.size() - 1;

        // Create a new layer and set as the new active layer.
        currentLayer = new Layer();

        drawingLayer.setActiveLayer(currentLayer);
        currentProduct.addLayer(currentLayer);

        layerList = (ArrayList<Layer>) currentProduct.getLayers();

        layerInUse = layerList.size() - 1;

        // Re-open the product manage dialog.
        startPGenActivityManagement();

    }

    /**
     * Switch between products.
     */
    private void switchProduct(int which) {

        // Save the current PGEN class
        savePGENClass();

        // Switch the color for the active layers
        setButtonColor(prdNameBtns.get(prdInUse), defaultButtonColor);

        prdList.get(prdInUse).setInUse(false);

        /*
         * As per ticket 64146, while switching products, we need to display all
         * activities from the (previously) selected products
         */
        for (int i = 0; i < prdList.size(); i++) {
            if (i == which || i == prdInUse) {
                prdDispOnOffBtns.get(i).setSelection(true);
                prdList.get(i).setOnOff(true);
                continue;
            }
            prdList.get(i).setOnOff(prdDispOnOffBtns.get(i).getSelection());
        }

        prdInUse = which;

        setButtonColor(prdNameBtns.get(prdInUse), activeButtonColor);

        // Turn the display for the product
        openPrdNameDialog = false;
        openLayerNameDialog = false;

        cleanupDialogs();

        currentProduct = prdList.get(prdInUse);

        currentProduct.setOnOff(true);
        currentProduct.setInUse(true);

        drawingLayer.setActiveProduct(currentProduct);

        // Turn the display on for the product
        layerList = (ArrayList<Layer>) currentProduct.getLayers();

        layerInUse = 0;

        currentLayer = layerList.get(layerInUse);

        drawingLayer.setActiveLayer(currentLayer);
        drawingLayer.removeGhostLine();
        drawingLayer.removeSelected();

        PgenUtil.setSelectingMode();

        // Reset PGEN palette
        resetPalette(currentProduct, currentLayer);

        // Reset undo/redo and refresh
        PgenSession.getInstance().disableUndoRedo();

        PgenUtil.refresh();

        // Re-open the product manage dialog.
        startPGenActivityManagement();

        // load settings
        AttrSettings.getInstance().loadProdSettings(currentProduct.getType());
    }

    /**
     * Delete the current product.
     */
    private void deleteProduct() {

        if (prdList.size() <= 1)
            return;

        // Set the flag - do not pop up the layer name editing dialog.
        openPrdNameDialog = false;
        openLayerNameDialog = false;

        // Remove the current product & set the next product as the active
        if (currentProduct != null) {
            drawingLayer.removeProduct(currentProduct);
        }

        prdList = (ArrayList<Product>) drawingLayer.getProducts();

        prdInUse--;

        if (prdInUse < 0) {
            prdInUse = 0;
        }

        currentProduct = prdList.get(prdInUse);
        currentProduct.setInUse(true);

        drawingLayer.setActiveProduct(currentProduct);

        //
        layerList = (ArrayList<Layer>) currentProduct.getLayers();
        layerInUse = layerList.size() - 1;
        currentLayer = layerList.get(layerInUse);

        drawingLayer.setActiveLayer(currentLayer);

        // Selects PGEN class from the current layer
        selectPGENClass();

        PgenUtil.refresh();

        // Re-open the layering dialog.
        startPGenActivityManagement();

    }

    /**
     * Toggle the display on/off for all check buttons.
     */
    private void updateProdDispChecks() {

        if (prodAllOnOff) {
            prodAllOnOff = false;
            prodAllOnOffBtn.setText("All On");
        } else {
            prodAllOnOff = true;
            prodAllOnOffBtn.setText("All Off");
        }

        for (int ii = 0; ii < prdList.size(); ii++) {
            prdDispOnOffBtns.get(ii).setSelection(prodAllOnOff);
            prdList.get(ii).setOnOff(prodAllOnOff);
        }

        PgenUtil.refresh();

    }

    /**
     * Retrieve active product;
     */
    protected Product getActiveProduct() {

        return currentProduct;

    }

    /**
     * Update the attributes for the current product;
     */
    protected void updateProductAttr(HashMap<String, String> attr) {

        boolean update = false;
        String value;

        value = attr.get("name");
        if (value != null && value.length() > 0
                && !value.equals(currentProduct.getName())) {
            currentProduct.setName(value);
            update = true;
        }

        value = attr.get("type");
        if (value != null && value.length() > 0
                && !value.equals(currentProduct.getType())) {
            updateLayerInfoFromNewPrdType(currentProduct, value);
            update = true;
        }

        value = attr.get("forecaster");
        if (!value.equals(currentProduct.getForecaster())) {
            if (value != null) {
                value = value.trim();
            }
            currentProduct.setForecaster(value);
        }

        value = attr.get("center");
        if (!value.equals(currentProduct.getCenter())) {
            if (value != null) {
                value = value.trim();
            }

            currentProduct.setCenter(value);
        }

        value = attr.get("saveLayers");
        if (value != null) {
            if (value.equals("true")) {
                currentProduct.setSaveLayers(true);
            } else {
                currentProduct.setSaveLayers(false);
            }
        }

        value = attr.get("outputfile");
        if (!value.equals(currentProduct.getOutputFile())) {
            if (value != null) {
                value = value.trim();
            }

        }

        /*
         * Rebuilds and opens the product control dialog since the size of the
         * button will change with the length of the new attributes.
         */
        openPrdNameDialog = false;
        if (update) {

            resetPalette(currentProduct, currentLayer);

            startPGenActivityManagement();
        }

    }

    /**
     * Update the layers defined in a new product type to a given product if the
     * new type is different from the given product's type.
     * 
     * Rules:
     * 
     * (1) Remove empty layers in the selected product, if there is at least one
     * layer in the new product type.
     * 
     * (2) If an existing layer has the same name as one defined in the new
     * product type, update its attributes to those defined in the product type.
     * 
     * (3) If a defined layer in the new product type does not exist in the
     * selected product, add the defined layer.
     * 
     * (4) If an existing layer is not empty but is not defined in the new
     * product type, keep it as is and the user can decide later if to keep it
     * or not.
     * 
     */
    private void updateLayerInfoFromNewPrdType(Product prd, String prdtype) {

        /*
         * Mark the existing layers with "P" and then add layers defined in the
         * new product type.
         */
        if (!prd.getType().equals(prdtype)) {

            prd.setSaveLayers(false);
            prd.setOutputFile(null);

            prd.setType(prdtype);

            ProductType newType = prdTypesMap.get(prdtype);

            // remove empty layers.
            ArrayList<Layer> layers = new ArrayList<>();
            for (Layer lyr : prd.getLayers()) {
                if (lyr.getDrawables().size() > 0) {
                    layers.add(lyr);
                }
            }

            prd.clear();

            if (layers.size() > 0) {
                for (Layer lyr : layers) {
                    prd.addLayer(lyr);
                }
            }

            // update layer info
            if (newType != null) {

                PgenSave psave = newType.getPgenSave();

                if (psave != null) {
                    prd.setSaveLayers(psave.isSaveLayers());

                }

                ArrayList<Layer> lyrs = (ArrayList<Layer>) (prd.getLayers());
                for (PgenLayer plyr : newType.getPgenLayer()) {

                    // update existing layers
                    boolean updated = false;
                    for (Layer olyr : lyrs) {
                        if (olyr.getName().equals(plyr.getName())) {

                            olyr.setOnOff(plyr.isOnOff());
                            olyr.setMonoColor(plyr.isMonoColor());
                            olyr.setFilled(plyr.isFilled());
                            olyr.setInputFile(null);
                            olyr.setOutputFile(null);

                            Color clr = new Color(plyr.getColor().getRed(),
                                    plyr.getColor().getGreen(),
                                    plyr.getColor().getBlue(),
                                    plyr.getColor().getAlpha());
                            olyr.setColor(clr);
                            updated = true;
                            break;
                        }
                    }

                    // Add layers defined in the new product type
                    if (!updated) {

                        Layer lyr = new Layer();
                        lyr.setName(plyr.getName());
                        lyr.setOnOff(plyr.isOnOff());
                        lyr.setMonoColor(plyr.isMonoColor());
                        lyr.setFilled(plyr.isFilled());
                        lyr.setInputFile(null);
                        lyr.setOutputFile(null);

                        Color clr = new Color(plyr.getColor().getRed(),
                                plyr.getColor().getGreen(),
                                plyr.getColor().getBlue(),
                                plyr.getColor().getAlpha());
                        lyr.setColor(clr);

                        prd.addLayer(lyr);
                    }
                }

            }

            // reset the layer list and the current layer
            if (prd.getLayers().size() <= 0) {
                prd.addLayer(new Layer());
            }

            if (prd.equals(currentProduct)) {
                layerList = (ArrayList<Layer>) currentProduct.getLayers();
                layerInUse = -1;
                if (layerList.size() > 0) {
                    layerInUse = 0;
                }
            }

        }

    }

    /**
     * Edit a selected product's attributes
     */
    private void editProductAttr() {

        /*
         * Pop up product attribute editing window
         */
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getShell();
        if (prdNameDlg == null)
            prdNameDlg = new ProductNameDialog(shell, this);

        cleanupDialogs();
        prdNameDlg.open();

    }

    /**
     * Select and switch to a product type with confirmation.
     */
    private void switchProductType(Text txt) {

        if (prdNameDlg != null && (prdNameDlg.isOpen())) {
            prdNameDlg.close();
        }

        int prdIndex = Integer.parseInt(txt.getData().toString());
        String prevType = prdList.get(prdIndex).getType();
        boolean typeExist = false;

        for (String ptypName : prdTypesMap.keySet()) {
            if (prevType.equals(ptypName)) {
                typeExist = true;
                break;
            }
        }

        if (!typeExist) {
            prevType = new String("Default");
        }

        if (!(txt.getText().equals(prevType))) {

            MessageDialog confirmDlg = new MessageDialog(
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                            .getShell(),
                    "Confirm changing product types", null,
                    "Are you sure you want to change from type " + prevType
                            + " to type " + txt.getText() + "?",
                    MessageDialog.QUESTION, new String[] { "OK", "Cancel" }, 0);

            confirmDlg.open();

            if (confirmDlg.getReturnCode() == MessageDialog.OK) {
                Product prd = prdList.get(prdIndex);

                if (currentProduct.equals(prd)) {
                    resetPalette(currentProduct, currentLayer);
                }

                /*
                 * update layers info defined in the new product type.
                 */
                String newTypeName = txt.getText();

                updateLayerInfoFromNewPrdType(prd, newTypeName);

                prdList.get(prdIndex).setType(newTypeName);
                prdList.get(prdIndex).setName(newTypeName);
                prdNameBtns.get(prdIndex).setText(newTypeName);

                if (prd.equals(currentProduct)) {
                    switchProduct(prdInUse);
                }
            }

        }
    }

    /**
     * Reset PGEN palette based on a product's type and layer.
     */
    private void resetPalette(Product prd, Layer clayer) {

        ProductType ptyp = prdTypesMap.get(prd.getType());

        if (clayer != null) {
            this.refreshPgenPalette(ptyp, clayer.getName());
        } else {
            this.refreshPgenPalette(ptyp, null);
        }

        PgenSession.getInstance().getPgenPalette().setActiveIcon("Select");

        // Selects PGEN class from the current layer
        selectPGENClass();

        PgenUtil.setSelectingMode();

    }

    /**
     * Clean up the child dialogs associated with the product manage dialog.
     */
    protected void cleanupDialogs() {
        if (prdNameDlg != null)
            prdNameDlg.close();
        if (layerNameDlg != null)
            layerNameDlg.close();
        if (displayDlg != null)
            displayDlg.close();
        if (layerLpfFileDlg != null)
            layerLpfFileDlg.close();
        if (prdFileInOutDlg != null)
            prdFileInOutDlg.close();
    }

    /**
     * Set the flag to open/close the product name dialog;
     */
    protected void setOpenPrdNameDlg(boolean value) {
        openPrdNameDialog = value;
    }

    /**
     * Set the flag to open/close the product name dialog;
     */
    protected void setopenLayerNameDlg(boolean value) {
        openLayerNameDialog = value;
    }

    /**
     * Return the full-path specified output file name for the product.
     */
    public String getPrdOutputFile(Product prd) {

        String sfile = PgenUtil.buildPrdFileName(prd, prdTypesMap);

        String filename = new String(
                buildFilePath(prd) + File.separator + sfile);

        return filename;
    }

    /*
     * Build a full path for a product's configured base path/type/subtype.
     */
    private String buildFilePath(Product prd) {

        StringBuilder sdir = new StringBuilder();

        sdir.append(PgenUtil.getPgenOprDirectory());

        String typeName = prd.getType();
        ProductType actTyp = prdTypesMap.get(typeName);
        if (actTyp != null) {
            sdir.append(File.separator + actTyp.getType());
            sdir.append(File.separator + "xml");
        }

        String path = sdir.toString().replace(' ', '_');

        return path;
    }

    /**
     * Retrieve the all subtypes defined for an activity type
     * 
     * Subtypes with alias could be excluded.
     * 
     */
    protected LinkedHashMap<String, String> getSubtypes(String ptype,
            boolean noAlias) {

        LinkedHashMap<String, String> stypes = new LinkedHashMap<>();

        for (String typeID : prdTypesMap.keySet()) {
            ProductType prdType = prdTypesMap.get(typeID);
            if (prdType.getType().equals(ptype)) {
                if (noAlias) {
                    if (prdType.getName() == null
                            || prdType.getName().trim().length() == 0
                            || prdType.getName().equals(prdType.getType())) {
                        stypes.put(typeID, prdType.getSubtype());
                    }
                } else {
                    stypes.put(typeID, prdType.getSubtype());
                }
            }
        }

        return stypes;

    }

    /**
     * Check if an activity type has no subtypes (except "None")
     * 
     */
    protected boolean hasSubtypes(Collection<String> subtypes) {

        boolean hasSubtypes = true;
        if (subtypes == null || subtypes.size() == 0) {
            hasSubtypes = false;
        } else if (subtypes.size() == 1) {
            for (String st : subtypes) {
                if (st.equalsIgnoreCase("None")) {
                    hasSubtypes = false;
                    break;
                }
            }
        }

        return hasSubtypes;

    }

    /*
     * Close the dialog
     */
    public void close() {
        PgenUtil.setSelectingMode();
        super.close();
    }

    /*
     * Clean up before close the shell - default is to do nothing.
     */
    protected void exit() {
        exitPgenActivityManagement();
    }

    /*
     * Build a string based on given Mono color and fill flags..
     */
    private String getDisplayString(boolean monoClr, boolean fill) {
        String dispStr = "";
        if (monoClr) {
            if (fill)
                dispStr += "M/F ";
            else
                dispStr += "M/N";

        } else {
            if (fill)
                dispStr += "A/F ";
            else
                dispStr += "A/N ";
        }

        return dispStr;
    }

}
