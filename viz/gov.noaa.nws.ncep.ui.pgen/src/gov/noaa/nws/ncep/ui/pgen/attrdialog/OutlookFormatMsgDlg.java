/*
 * OutlookFormatMsgDlg
 * 
 * Date created: 5 April 2010
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.ui.pgen.attrdialog;

import gov.noaa.nws.ncep.ui.pgen.PgenUtil;
import gov.noaa.nws.ncep.ui.pgen.elements.Layer;
import gov.noaa.nws.ncep.ui.pgen.elements.Outlook;
import gov.noaa.nws.ncep.ui.pgen.elements.Product;
import gov.noaa.nws.ncep.ui.pgen.elements.ProductTime;
import gov.noaa.nws.ncep.ui.pgen.productmanage.ProductConfigureDialog;
import gov.noaa.nws.ncep.ui.pgen.producttypes.ProductType;
import gov.noaa.nws.ncep.ui.pgen.store.PgenStorageException;
import gov.noaa.nws.ncep.ui.pgen.store.StorageUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

import org.dom4j.Node;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Implementation of a dialog to display information of an outlook product.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date       	Ticket#		Engineer	Description
 * ------------	----------	-----------	--------------------------
 * 02/10			?		B. Yin   	Initial Creation. 
 * 03/12		#703		B. Yin		Generate product text from style sheet
 * 06/12		#786		B. Yin		Close all dialogs after text is saved.
 * 04/29/13     #977        S. Gilbert  PGEN Database support
 * 08/13		TTR 770		B. Yin		Handle no outlook, fixed the file name. 
 * 
 * </pre>
 * 
 * @author B. Yin
 */

public class OutlookFormatMsgDlg extends Dialog {

    // top level container for all widgets
    private Composite top;

    // Message text
    private Text messageBox;

    private String message;

    // instance of outlook format dialog
    private OutlookFormatDlg ofd;

    private Outlook otlk;

    private Layer layer;

    // dialog size
    private final int NUM_LINES = 25;

    private final int NUM_COLUMNS = 68;

    /*
     * constructor
     */
    protected OutlookFormatMsgDlg(Shell parentShell, OutlookFormatDlg ofd,
            Outlook otlk, Layer layer) {
        super(parentShell);
        this.setShellStyle(SWT.TITLE | SWT.MODELESS | SWT.CLOSE);
        this.ofd = ofd;
        this.otlk = otlk;
        this.layer = layer;
        message = "";
    }

    /**
     * Creates the dialog area
     */
    @Override
    public Control createDialogArea(Composite parent) {

        // Set title
        getShell().setText("Outlook Message");

        top = (Composite) super.createDialogArea(parent);

        /*
         * Create the main layout for the dialog area.
         */
        GridLayout mainLayout = new GridLayout(1, true);
        mainLayout.marginHeight = 3;
        mainLayout.marginWidth = 3;
        top.setLayout(mainLayout);

        /*
         * Create a text box for the message
         */
        messageBox = new Text(top, SWT.MULTI | SWT.BORDER | SWT.READ_ONLY
                | SWT.V_SCROLL);
        messageBox.setFont(new Font(messageBox.getDisplay(), "Courier", 12,
                SWT.NORMAL));
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);

        // Calculate approximate size of text box to display 25 lines at 80
        // characters each
        gd.heightHint = NUM_LINES * messageBox.getLineHeight();
        GC gc = new GC(messageBox);
        FontMetrics fm = gc.getFontMetrics();
        gd.widthHint = NUM_COLUMNS * fm.getAverageCharWidth();

        messageBox.setLayoutData(gd);
        messageBox.setText(message);

        // Make sure to dispose of font
        messageBox.addDisposeListener(new DisposeListener() {

            @Override
            public void widgetDisposed(DisposeEvent e) {
                Text w = (Text) e.widget;
                w.getFont().dispose();
            }

        });

        return top;
    }

    /*
     * 
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
     */
    @Override
    protected void okPressed() {
        savePressed();
        super.okPressed();
    }

    /**
     * Save the formatted outlook element to a file
     */
    private void savePressed() {

        String pdName = ofd.getOtlkDlg().drawingLayer.getActiveProduct()
                .getType();
        ProductType pt = ProductConfigureDialog.getProductTypes().get(pdName);
        if (pt != null)
            pdName = pt.getType();

        String pd1 = pdName.replaceAll(" ", "_");

        String dirPath = PgenUtil.getPgenOprDirectory() + File.separator + pd1
                + File.separator + "prod" + File.separator + "text"
                + File.separator;

        String fileName = getFileName(otlk) + ".dat";

        InputDialog dlg = new InputDialog(this.getShell(), "Save Outlook",
                "Save To File:", fileName, null);
        dlg.open();
        if (dlg.getReturnCode() == Dialog.OK) {
            fileName = dlg.getValue();
            if (!fileName.isEmpty()) {
                ofd.issueOutlook(otlk);

                String dataURI = storeProduct(getFileName(otlk) + ".xml");
                if (dataURI == null)
                    return;

                try {
                    StorageUtils.storeDerivedProduct(dataURI, fileName, "TEXT",
                            message, true);
                } catch (PgenStorageException e) {
                    StorageUtils.showError(e);
                    return;
                }
                // FileTools.writeFile(fileName, message);
                // otlk.saveToFile( dirPath + getFileName(otlk)+".xml");

                // clean up
                this.close();
            }
        }

    }

    /**
     * Get the default file name for the formatted outlook
     * 
     * @param otlk
     * @return
     */
    private String getFileName(Outlook ol) {
    	String type = "";
        if ( ol != null ) {
        	type = ol.getOutlookType();
        }
        else {
        	//Should be the outlook type for the layer, but there is no outlook type in layer currently. 
        	type = layer.getName();
        	//type = ofd.getOtlkDlg().getOutlookType();
        }
        
        String name = "";
        if (type != null) {
            type = type.toUpperCase();
            String xpath = OutlookAttrDlg.OTLK_XPATH + "[@name='" + type + "']";
            String prefix = "";
            Node nd = OutlookAttrDlg.readOutlookTbl()
                    .selectSingleNode(xpath);
            
            if ( nd != null ) {
            	prefix = nd.valueOf("@prefix");
            }
            
            if (prefix.isEmpty()) {
                name += "outlook";
            } else {
                name += prefix + "outlook";
            }
        }

      //  name += "_"
      //          + ofd.getDays().toUpperCase().replace("-", "").replace(" ", "")
      //                  .replace("FIRE", "");

        String days = ofd.getDays();
        
        if ( days.contains("Enh")){
        	days = "Day1";
        }
        
        name += "_"
                 + days.toUpperCase().replace("-", "").replace(" ", "");
        name += "_" + String.format("%1$td%1$tH%1$tM", ofd.getInitTime()) + "Z";
        return name;

    }

    /**
     * Re-order the outlook lines and re-generate the message
     */
    private void updatePressed() {
    	if ( otlk != null ){
        // ofd.reorderOtlkLines();
        otlk.reorderLines();
        messageBox.setText(ofd.formatOtlk(otlk, layer));
    }
    }

    /*
     * 
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
     */
    @Override
    protected void cancelPressed() {

        // close the format dialog
        ofd.close();
        super.cancelPressed();

    }

    /**
     * Set outlook text
     * 
     * @param str
     *            The issued watch text
     */
    public void setMessage(String str) {
        message = str;
    }

    @Override
    /**
     * Set the location of the dialog
     * Set the OK button to Save
     */
    public int open() {

        if (this.getShell() == null) {
            this.create();
        }

        this.getShell().setLocation(this.getShell().getParent().getLocation());
        this.getButton(IDialogConstants.OK_ID).setText("Save");
        this.getButtonBar().pack();

        return super.open();

    }

    /**
     * Create buttons on the button bar
     */
    @Override
    public void createButtonsForButtonBar(Composite parent) {

        GridLayout barGl = new GridLayout(3, false);
        parent.setLayout(barGl);

        Button updtBtn = new Button(parent, SWT.PUSH);

        super.createButtonsForButtonBar(parent);

        // add update button
        updtBtn.setText("Update");
        updtBtn.setLayoutData(getButton(IDialogConstants.CANCEL_ID)
                .getLayoutData());
        updtBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                updatePressed();
            }

        });

        getButton(IDialogConstants.OK_ID).setText("Save");
    }

    public void setOtlk(Outlook otlk) {
        this.otlk = otlk;
    }

    public Outlook getOtlk() {
        return otlk;
    }

    public void setLayer(Layer layer) {
        this.layer = layer;
    }

    public Layer getLayer() {
        return layer;
    }
    
    /**
     * Save this element to EDEX
     * 
     * @param filename
     */
    private String storeProduct(String label) {

        String dataURI;

        Layer defaultLayer = new Layer();
        if ( otlk != null ) defaultLayer.addElement(otlk);
        ArrayList<Layer> layerList = new ArrayList<Layer>();
        layerList.add(defaultLayer);
        
        ProductTime refTime = null; 
        String forecaster = "";
        if ( otlk != null ){
        	refTime = new ProductTime(otlk.getIssueTime());
        	forecaster = otlk.getForecaster();
        }
        else {
        	refTime = new ProductTime(Calendar.getInstance( TimeZone.getTimeZone("GMT") ));
        	forecaster = ofd.getForecaster();
        }

        Product defaultProduct = new Product("", "OUTLOOK", forecaster, null,
                refTime, layerList);

        // Product defaultProduct = new Product();
        // defaultProduct.addLayer(defaultLayer);

        defaultProduct.setOutputFile(label);
        defaultProduct.setCenter(PgenUtil.getCurrentOffice());

        try {
            dataURI = StorageUtils.storeProduct(defaultProduct);
        } catch (PgenStorageException e) {
            StorageUtils.showError(e);
            return null;
        }

        return dataURI;
    }

}
