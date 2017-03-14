/**
 * This software was developed and / or modified by NOAA/NWS/OCP/ASDT
 **/
package gov.noaa.nws.ncep.ui.pgen.attrdialog.cwadialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.raytheon.viz.ui.dialogs.CaveSWTDialog;

/**
 * This class displays the CSIG dialog.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date        Ticket#  Engineer    Description
 * ----------- -------- ----------- --------------------------
 * 12/02/2016  17469    wkwock      Initial creation
 * 
 * </pre>
 * 
 * @author wkwock
 */
public class CSIGDlg extends CaveSWTDialog {
    /** product text */
    private Text productTxt;

    /** product ID */
    private String productId;

    /** is operational */
    private boolean isOperational;

    /**
     * Constructor
     * 
     * @param display
     * @param productId
     */
    public CSIGDlg(Shell shell, String productId, boolean isOperational) {
        super(shell, SWT.DIALOG_TRIM);
        setText(productId);
        this.productId = productId;
        this.isOperational = isOperational;
    }

    @Override
    protected void initializeComponents(Shell shell) {
        GridLayout mainLayout = new GridLayout(1, false);
        mainLayout.marginHeight = 3;
        mainLayout.marginWidth = 3;
        mainLayout.verticalSpacing = 5;
        shell.setLayout(mainLayout);

        productTxt = new Text(shell,
                SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        GridData gd = new GridData(SWT.FILL, SWT.DEFAULT, false, false);
        gd.heightHint = 260;
        gd.widthHint = 680;
        productTxt.setLayoutData(gd);
        productTxt.setEditable(false);

        Button exitBtn = new Button(shell, SWT.PUSH | SWT.CENTER);
        exitBtn.setText("Exit");
        GridData csigGd = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1,
                1);
        exitBtn.setLayoutData(csigGd);

        exitBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                close();
            }
        });

        displayProduct();
    }

    /**
     * display product
     */
    private void displayProduct() {
        CWAProduct cwaProduct = new CWAProduct(productId, "", isOperational);
        productTxt.setText(cwaProduct.getProductTxt());
    }
}
