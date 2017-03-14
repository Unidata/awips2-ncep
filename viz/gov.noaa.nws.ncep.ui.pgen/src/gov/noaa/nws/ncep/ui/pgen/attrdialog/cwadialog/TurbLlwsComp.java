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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * This class displays the turbulence/LLWS composite.
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
public class TurbLlwsComp extends AbstractCWAComp {
    /** frequency combo */
    private Combo freqCbo;

    /** intensity combo */
    private Combo intstyCbo;

    /** flight from combo */
    private Combo flightFromCbo;

    /** flight to combo */
    private Combo flightToCbo;

    /** Low Level Wind Shear check button */
    private Button llwsChk;

    /** no condition radio button */
    private Button noCondRdo;

    /** conditions continue beyond radio button */
    private Button contgRdo;

    /** conditions improve by radio button */
    private Button imprRdo;

    /** aircraft check button */
    private Button aircraftChk;

    /** additional information check button */
    private Button addnlInfoChk;

    /** additional information text field */
    private Text addnlInfoTxt;

    /** no update check button */
    private Button noUpdateChk;

    /** frequency items */
    private final static String freqItems[] = { " OCNL", " FQT", " CONS" };

    /** intensity items */
    private final static String intstyItems[] = { "MOD", "MOD ISOL SEV",
            "MOD OCNL SEV", "MOD/SEV", "SEV", "EXTRM" };

    /**
     * constructor
     * 
     * @param parent
     */
    public TurbLlwsComp(Composite parent) {
        super(parent, SWT.NONE);
        setLayout(new GridLayout(1, false));

        createTimeComp();

        // Frequency
        Composite freqComp = new Composite(this, SWT.NONE);
        freqComp.setLayout(new GridLayout(4, false));
        Label freqLbl = new Label(freqComp, SWT.NONE);
        freqLbl.setText("Freq:");

        freqCbo = new Combo(freqComp, SWT.READ_ONLY);
        freqCbo.setItems(freqItems);

        // intsty
        Label intstyLbl = new Label(freqComp, SWT.NONE);
        intstyLbl.setText("Intst:");
        GridData intstyGd = new GridData();
        intstyGd.horizontalIndent = HORIZONTAL_INDENT;
        intstyLbl.setLayoutData(intstyGd);

        intstyCbo = new Combo(freqComp, SWT.READ_ONLY);
        intstyCbo.setItems(intstyItems);
        intstyCbo.select(0);

        // from flight
        Composite flightComp = new Composite(this, SWT.NONE);
        flightComp.setLayout(new GridLayout(4, false));
        Label fromFlightLbl = new Label(flightComp, SWT.NONE);
        fromFlightLbl.setText("Flight Level From:");

        flightFromCbo = new Combo(flightComp, SWT.READ_ONLY);
        flightFromCbo.add("SFC");
        for (int i = 10; i <= 350;) {
            flightFromCbo.add(String.format("%03d", i));
            i += 10;
        }
        flightFromCbo.select(0);
        flightFromCbo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                updateToFlightCbo();
            }
        });

        // to flight
        Label toFlightLbl = new Label(flightComp, SWT.NONE);
        toFlightLbl.setText("To:");

        flightToCbo = new Combo(flightComp, SWT.READ_ONLY);
        for (int i = 180; i <= 450;) {
            flightToCbo.add(String.format("%03d", i));
            i += 10;
        }
        flightToCbo.select(0);

        // Add LLWS
        llwsChk = new Button(this, SWT.CHECK);
        llwsChk.setText("Add LLWS");

        // condition buttons
        Composite condComp = new Composite(this, SWT.NONE);
        condComp.setLayout(new GridLayout(3, false));

        noCondRdo = new Button(condComp, SWT.RADIO);
        noCondRdo.setText("No Conds Remark");
        noCondRdo.setSelection(true);

        contgRdo = new Button(condComp, SWT.RADIO);
        contgRdo.setText("Conds Contg Byd");

        imprRdo = new Button(condComp, SWT.RADIO);
        imprRdo.setText("Conds Impr By");

        // report composite
        Composite reportComp = new Composite(this, SWT.NONE);
        reportComp.setLayout(new GridLayout(4, false));

        aircraftChk = new Button(reportComp, SWT.CHECK);
        aircraftChk.setText("Rpt by Aircraft");

        addnlInfoChk = new Button(reportComp, SWT.CHECK);
        addnlInfoChk.setText("Addnl Info to AIRMET TANGO");
        addnlInfoTxt = new Text(reportComp, SWT.BORDER);
        GridData gd = new GridData();
        gd.widthHint = 25;
        addnlInfoTxt.setLayoutData(gd);

        noUpdateChk = new Button(reportComp, SWT.CHECK);
        noUpdateChk.setText("No Updt Aft");
    }

    /**
     * update 'To: Flight Levels'
     */
    private void updateToFlightCbo() {
        int initLevel = (flightFromCbo.getSelectionIndex() + 1) * 10;
        if (initLevel < 180) {
            initLevel = 180;
        }

        int flightToLevel = Integer.parseInt(flightToCbo.getText());

        flightToCbo.removeAll();
        for (int i = initLevel; i <= 450;) {
            flightToCbo.add(String.format("%03d", i));
            i += 10;
        }
        if (flightToLevel < initLevel) {
            flightToCbo.select(0);
        } else {
            flightToCbo.setText(Integer.toString(flightToLevel));
        }
    }

    /**
     * Create product text for Turbulence/LLWS
     * 
     * @param wmoId
     * @param header
     * @param fromline
     * @param body
     * @param cwsuId
     * @param productId
     * @param isCor
     * @return
     */
    @Override
    public String createText(String wmoId, String header, String fromline,
            String body, String cwsuId, String productId, boolean isCor,
            boolean isOperational) {
        String endDateTime = getEndTime();

        String frmLVL = "BLW ";
        if (flightFromCbo.getSelectionIndex() != 0) {
            int flightFrom = Integer.parseInt(flightFromCbo.getText());
            if (flightFrom < 180) {
                frmLVL = flightFromCbo.getText() + "-";
            } else {
                frmLVL = "FL" + flightFromCbo.getText() + "-";
            }
        }

        // get next series ID
        CWAProduct cwaProduct = new CWAProduct(productId, cwsuId,
                isOperational);
        int seriesId = cwaProduct.getNextSeriesId(isCor);

        StringBuilder output = new StringBuilder();
        output.append(getHeaderLines(wmoId, header, isCor));
        output.append(getValidLine(cwsuId, endDateTime, seriesId));
        output.append(fromline).append(" \n");
        output.append(body);
        output.append(freqCbo.getText()).append(" ").append(intstyCbo.getText())
                .append(" TURB ");
        output.append(frmLVL).append("FL").append(flightToCbo.getText());
        if (llwsChk.getSelection()) {
            output.append(" AND LLWS");
        }
        output.append(". \n");

        if (!noCondRdo.getSelection()) {
            if (contgRdo.getSelection()) {
                output.append("CONDS CONTG BYD ");
            } else {
                output.append("CONDS IMPR BY ");
            }
            output.append(endDateTime).append("Z.");
        }
        output.append("\n");
        if (aircraftChk.getSelection()) {
            output.append("RPT BY AIRCRAFT.");
        }
        if (addnlInfoChk.getSelection()) {
            output.append("THIS IS ADDN INFO TO AIRMET TANGO ");
            output.append(addnlInfoTxt.getText().trim());
        }

        if (noUpdateChk.getSelection()) {
            output.append("NO UPDT AFT ").append(endDateTime).append("Z. ");
        }
        output.append("\n= \n");

        return output.toString();
    }
}
