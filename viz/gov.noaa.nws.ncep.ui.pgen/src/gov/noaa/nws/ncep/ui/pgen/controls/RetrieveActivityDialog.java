/*
 * gov.noaa.nws.ncep.ui.pgen.controls.RetrieveActivityDialog
 *
 * 29 March 2013
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */
package gov.noaa.nws.ncep.ui.pgen.controls;

import java.util.Iterator;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.viz.ui.dialogs.CaveJFACEDialog;

import gov.noaa.nws.ncep.ui.pgen.PgenSession;
import gov.noaa.nws.ncep.ui.pgen.PgenUtil;
import gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent;
import gov.noaa.nws.ncep.ui.pgen.elements.Jet;
import gov.noaa.nws.ncep.ui.pgen.elements.Layer;
import gov.noaa.nws.ncep.ui.pgen.elements.Product;
import gov.noaa.nws.ncep.ui.pgen.rsc.PgenResource;
import gov.noaa.nws.ncep.ui.pgen.sigmet.VaaInfo;
import gov.noaa.nws.ncep.ui.pgen.store.PgenStorageException;
import gov.noaa.nws.ncep.ui.pgen.store.StorageUtils;
import gov.noaa.nws.ncep.ui.pgen.tools.PgenSnapJet;

/**
 * A dialog to Retrieve PGEN activities from EDEX.
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- -----------------------------------
 * 03/13        #977        S.gilbert   Modified from PgenFileManageDialog1
 * 01/2014      #1105       jwu         Use "subtype" for query as well.
 * 08/2014      TTR867      jwu         Add "time stamp" for activities with same label.
 * 08/2014      ?           jwu         Preserve "outputFile" name when opening an activity.
 * 06/24/2015   R7806       A. Su       Added two pull-down menus (site & desk), and a list (subtype).
 *                                      Rearranged three sets of two-choice radio buttons.
 *                                      Implemented new logic for selecting activity labels.
 * 04/14/2016   R13245      B. Yin      Changed the format of date/time string.
 * Feb 28, 2019 7752        tjensen     Moved ActivityElement to its own class
 * 8/27/2019    67216       ksunil      Code re-factored into PgenRetrieveCommonDialogArea
 *
 * </pre>
 *
 * @author
 */
public class RetrieveActivityDialog extends CaveJFACEDialog {

    /*
     * Used to compare Activity's reference times.
     */
    class ActivityTimeComparator extends ViewerComparator {

        @Override
        public int compare(Viewer viewer, Object e1, Object e2) {
            // Comparing e2 to e1 to get reverse ordering
            return ((ActivityElement) e2).compareTo(e1);
        }

    }

    private final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(this.getClass());

    private PgenRetrieveCommonDialogArea commonDialogArea = null;

    private static final int ADD_ID = IDialogConstants.CLIENT_ID + 7587;

    private static final String ADD_LABEL = "Add";

    private static final int REPLACE_ID = IDialogConstants.CLIENT_ID + 7586;

    private static final String REPLACE_LABEL = "Replace";

    private static final int ADVANCE_ID = IDialogConstants.CLIENT_ID + 7588;

    private static final String ADVANCE_LABEL = "Advanced";

    private static final int CLOSE_ID = IDialogConstants.CLIENT_ID + 7590;

    private static final String CLOSE_LABEL = "Close";

    private static String fullName = null;

    /*
     * Constructor
     */
    public RetrieveActivityDialog(Shell parShell) {
        super(parShell);
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Retrieve a PGEN Activity");
    }

    /**
     * R7806: This method uses SWT GridLayouts (replacing FormLayout) to glue
     * together the major components, SWT widgets, of the PGEN Open dialog
     * window, which include two pull-down menus for Site and Desk, two SWT
     * lists for Type and Subtype, three sets of two-choice radio buttons, and a
     * SWT list for Activity labels.
     *
     * It relies on the class ActivityCollection to provide the contents of the
     * SWT widgets.
     */
    @Override
    public Control createDialogArea(Composite parent) {

        Composite dialogArea = (Composite) super.createDialogArea(parent);
        dialogArea.setLayout(new GridLayout(1, false));

        commonDialogArea = new PgenRetrieveCommonDialogArea();
        return commonDialogArea.createComponents(dialogArea);

    }

    @Override
    protected void buttonPressed(int buttonId) {
        switch (buttonId) {
        case ADD_ID:
            openProducts(false);
            break;
        case REPLACE_ID:
            openProducts(true);
            break;
        case ADVANCE_ID:
            appendProducts();
            break;
        case CLOSE_ID:
            close();
            break;
        default:
            statusHandler.error("Unknown button id: " + buttonId);
        }
    }

    /**
     * Create Replace/Append/Cancel button for "Open" a product file or
     * Save/Cancel button for "Save" a product file.
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {

        createButton(parent, ADD_ID, ADD_LABEL, true);
        createButton(parent, REPLACE_ID, REPLACE_LABEL, true);
        createButton(parent, ADVANCE_ID, ADVANCE_LABEL, true);
        createButton(parent, CLOSE_ID, CLOSE_LABEL, true);

    }

    /**
     * Retrieve an activity to replace or append to the current product list in
     * the current PGEN session.
     */
    private void openProducts(boolean replace) {

        ActivityElement elem = getActivitySelection();
        if (elem == null) {
            return;
        }
        fullName = elem.getActivityLabel();

        java.util.List<Product> pgenProds = null;
        try {
            pgenProds = StorageUtils.retrieveProduct(elem.getDataURI());
        } catch (PgenStorageException e) {
            StorageUtils.showError(e);
        }

        /*
         * some Volcano Products are pure texts: TEST/RESUME and cannot be
         * drawn.
         */
        if (VaaInfo.isNoneDrawableTxt(pgenProds)) {
            VaaInfo.openMsgDlg(VaaInfo.NONE_DRAWABLE_MSG);
            return;
        }

        /*
         * Confirm the action
         */
        PgenResource pgen = PgenSession.getInstance().getPgenResource();

        // Force all product/layer display onOff flag to be false at the start.
        if (replace) {
            boolean confirmed = MessageDialog.openConfirm(getShell(),
                    "Confirm File Replace",
                    "Replace Activity <" + pgen.getActiveProduct().getType()
                            + "> with New Activity <"
                            + pgenProds.get(0).getType() + "> ?");

            if (!confirmed) {
                return;
            }

        }

        pgen.setAutosave(
                commonDialogArea.getAutoSaveRadioButtons()[1].getSelection());
        if (fullName.endsWith(".lpf")) {
            pgen.setAutoSaveFilename(fullName.replace(".lpf", "xml"));
        } else {
            pgen.setAutoSaveFilename(fullName);
        }

        pgenProds.get(0).setInputFile(fullName);

        this.setJetTool(pgenProds);

        close();

        /*
         * Replace the active product or add the product to the end
         */
        if (replace) {
            // Reset the output file name.
            // for (gov.noaa.nws.ncep.ui.pgen.elements.Product pp : pgenProds) {
            // pp.setOutputFile(null);
            // }

            PgenFileNameDisplay.getInstance().setFileName(fullName);
            pgen.replaceProduct(pgenProds);
        } else {

            if (pgen.getActiveProduct() == null
                    || pgen.removeEmptyDefaultProduct()) {
                PgenFileNameDisplay.getInstance().setFileName(fullName);
            }

            pgen.addProduct(pgenProds);
        }

        PgenUtil.refresh();

    }

    /**
     * Append the products in a file with those in the current product list.
     */
    private void appendProducts() {

        ActivityElement elem = getActivitySelection();
        if (elem == null) {
            return;
        }
        fullName = elem.getActivityLabel();

        java.util.List<Product> pgenProds = null;
        try {
            pgenProds = StorageUtils.retrieveProduct(elem.getDataURI());
        } catch (PgenStorageException e) {
            StorageUtils.showError(e);
        }

        /*
         * some Volcano Products are pure texts: TEST/RESUME and cannot be
         * drawn.
         */
        if (VaaInfo.isNoneDrawableTxt(pgenProds)) {
            VaaInfo.openMsgDlg(VaaInfo.NONE_DRAWABLE_MSG);
            return;
        }

        PgenResource pgen = PgenSession.getInstance().getPgenResource();

        PgenLayerMergeDialog layerMergeDlg = null;
        try {
            layerMergeDlg = new PgenLayerMergeDialog(getShell(),
                    pgenProds.get(0), fullName);
        } catch (Exception e) {
            statusHandler.error(e.getLocalizedMessage(), e);
        }

        if (layerMergeDlg != null) {

            layerMergeDlg.open();
            if (layerMergeDlg.getReturnCode() == MessageDialog.OK) {

                pgen.setAutosave(commonDialogArea.getAutoSaveRadioButtons()[1]
                        .getSelection());
                if (fullName.endsWith(".lpf")) {
                    pgen.setAutoSaveFilename(fullName.replace(".lpf", "xml"));
                } else {
                    pgen.setAutoSaveFilename(fullName);
                }

                this.setJetTool(pgenProds);

                close();

                pgen.getResourceData().startProductManage();
            }
        }

    }

    private ActivityElement getActivitySelection() {
        ActivityElement elem = null;

        if (!commonDialogArea.getFileListViewer().getSelection().isEmpty()) {
            IStructuredSelection sel = (IStructuredSelection) commonDialogArea
                    .getFileListViewer().getSelection();
            elem = (ActivityElement) sel.getFirstElement();
        } else {
            MessageDialog.openInformation(getShell(), "Invalid PGEN Selection",
                    "Please select an Activity from the Activity Label list.");
        }

        return elem;
    }

    /**
     * Sets the jet snap tool in order to zoom tghe jet correctly.
     *
     * @param prods
     */
    private void setJetTool(
            java.util.List<gov.noaa.nws.ncep.ui.pgen.elements.Product> prods) {

        PgenSnapJet st = new PgenSnapJet(
                PgenSession.getInstance().getPgenResource().getDescriptor(),
                PgenUtil.getActiveEditor(), null);

        for (gov.noaa.nws.ncep.ui.pgen.elements.Product prod : prods) {
            for (Layer layer : prod.getLayers()) {

                Iterator<AbstractDrawableComponent> iterator = layer
                        .getComponentIterator();
                while (iterator.hasNext()) {
                    AbstractDrawableComponent adc = iterator.next();
                    if (adc instanceof Jet) {
                        ((Jet) adc).setSnapTool(st);
                        // st.snapJet((Jet)adc);
                    }
                }
            }
        }

    }

}
