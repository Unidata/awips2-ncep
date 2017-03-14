/**
 * This software was developed and / or modified by NOAA/NWS/OCP/ASDT
 **/
package gov.noaa.nws.ncep.ui.pgen.attrdialog.cwadialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * This class displays the cancel/manual composite.
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
public class CancelManualComp extends AbstractCWAComp {
    /** cancel check button */
    private Button cancelChk;

    /** see check button */
    private Button seeChk;

    /** additional information text field */
    private Text addnlInfoTxt;

    /** no update check button */
    private Button noUpdateChk;

    /**
     * constructor
     * 
     * @param parent
     */
    public CancelManualComp(Composite parent) {
        super(parent, SWT.NONE);
        setLayout(new GridLayout(1, false));

        Label guideLbl = new Label(this, SWT.NONE);
        guideLbl.setText(
                "Click 'Create New Text' to pre-format a manual product.");

        createTimeComp();

        // report composite
        Composite reportComp = new Composite(this, SWT.NONE);
        reportComp.setLayout(new GridLayout(4, false));

        cancelChk = new Button(reportComp, SWT.CHECK);
        cancelChk.setText("Cancel ZHU CWA");

        seeChk = new Button(reportComp, SWT.CHECK);
        seeChk.setText("SEE CNVTV SIGMET #");
        addnlInfoTxt = new Text(reportComp, SWT.BORDER);
        GridData gd = new GridData();
        gd.widthHint = 25;
        addnlInfoTxt.setLayoutData(gd);

        noUpdateChk = new Button(reportComp, SWT.CHECK);
        noUpdateChk.setText("No Updt Aft");
    }

    /**
     * Create product text for cancel/manual
     * 
     * @param wmoId
     * @param header
     * @param fromline
     * @param body
     * @param cwsuId
     * @param productId
     * @param isCor
     * @return product text
     */
    @Override
    public String createText(String wmoId, String header, String fromline,
            String body, String cwsuId, String productId, boolean isCor,
            boolean isOperational) {
        String endDateTime = getEndTime();

        // get next series ID
        CWAProduct cwaProduct = new CWAProduct(productId, cwsuId,
                isOperational);
        int seriesId = cwaProduct.getNextSeriesId(isCor);

        // Check for isolated cell over VOR. Length would be 3 if it
        // is true.
        if (fromline.length() == 3) {
            fromline = "OVR " + fromline;
        }

        String prevalid = "";
        if (cancelChk.getSelection()) {
            prevalid = String.valueOf(seriesId - 1);
            fromline = "";
        }

        StringBuilder output = new StringBuilder();
        output.append(getHeaderLines(wmoId, header, isCor));
        output.append(getValidLine(cwsuId, endDateTime, seriesId));
        output.append(fromline);
        if (this.cancelChk.getSelection()) {
            output.append("CANCEL ").append(cwsuId).append(" CWA");
        }
        output.append(" ").append(prevalid).append(". ");
        if (this.seeChk.getSelection()) {
            output.append("SEE CNVTV SIGMET ").append(addnlInfoTxt.getText());
        }
        if (noUpdateChk.getSelection()) {
            output.append("NO UPDT AFT ").append(endDateTime).append("Z. ");
        }

        output.append("\n\n= \n");
        return output.toString();
    }

    /**
     * update CWSU ID
     * 
     * @param cwsuId
     */
    public void updateCwsuId(String cwsuId) {
        cancelChk.setText("Cancel " + cwsuId + " CWA");
    }
}
