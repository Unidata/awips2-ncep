/**
 * This software was developed and / or modified by NOAA/NWS/OCP/ASDT
 **/
package gov.noaa.nws.ncep.ui.pgen.attrdialog.cwadialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;

/**
 * This class displays the thunderstorm composite.
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
public class ThunderstormComp extends AbstractCWAComp {
    /** developing check button */
    private Button dvlpgChk;

    /** embedded check button */
    private Button embdChk;

    /** coverage combo */
    private Combo coverageCbo;

    /** type combo */
    private Combo typeCbo;

    /** intensity combo */
    private Combo intstCbo;

    /** wind direction combo */
    private Combo dirCbo;

    /** wind speed combo */
    private Combo spdCbo;

    /** Tops from combo */
    private Combo topsFromCbo;

    /** Tops to Combo */
    private Combo topsToCbo;

    /** estimated check button */
    private Button estChk;

    /** tornado check button */
    private Button tornadoChk;

    /** hail check button */
    private Button hailChk;

    /** gust check button */
    private Button gustChk;

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

    /** coverage items */
    private final static String CoverageItems[] = { "ISOL", "WDLY SCT", "SCT",
            "NMRS" };

    /** type items */
    private final static String typeItems[] = { "SHRA/TSRA", "TSRA", "TS" };

    /** intensity items */
    private final static String intstItems[] = { "---", "MOD", "MOD TO HVY",
            "HVY", "HVY TO EXTRM", "EXTRM" };

    /**
     * constructor
     * 
     * @param parent
     */
    public ThunderstormComp(Composite parent) {
        super(parent, SWT.NONE);
        setLayout(new GridLayout(1, false));

        createTimeComp();

        Composite line2Comp = new Composite(this, SWT.NONE);
        line2Comp.setLayout(new GridLayout(5, false));
        // DVLPG
        dvlpgChk = new Button(line2Comp, SWT.CHECK);
        dvlpgChk.setText("DVLPG");
        // EMBD
        embdChk = new Button(line2Comp, SWT.CHECK);
        embdChk.setText("EMBD");

        // coverage
        Composite coverageComp = new Composite(this, SWT.NONE);
        coverageComp.setLayout(new GridLayout(6, false));
        Label coverageLbl = new Label(coverageComp, SWT.NONE);
        coverageLbl.setText("Coverage:");

        coverageCbo = new Combo(coverageComp, SWT.READ_ONLY);
        coverageCbo.setItems(CoverageItems);
        coverageCbo.select(0);

        // Type
        Label typeLbl = new Label(coverageComp, SWT.NONE);
        typeLbl.setText("  Type:");

        typeCbo = new Combo(coverageComp, SWT.READ_ONLY);
        typeCbo.setItems(typeItems);
        typeCbo.select(1);

        // intst
        Label intstLbl = new Label(coverageComp, SWT.NONE);
        intstLbl.setText("  Intst:");

        intstCbo = new Combo(coverageComp, SWT.READ_ONLY);
        intstCbo.setItems(intstItems);
        intstCbo.select(0);

        // wind direction and speed
        Composite windComp = new Composite(this, SWT.NONE);
        windComp.setLayout(new GridLayout(9, false));
        Label windLbl = new Label(windComp, SWT.NONE);
        windLbl.setText("Dir/Spd:");

        dirCbo = new Combo(windComp, SWT.READ_ONLY);
        dirCbo.add("---");
        for (int i = 10; i <= 360;) {
            dirCbo.add(String.format("%03d", i));
            i += 10;
        }
        dirCbo.select(0);

        Label slashLbl = new Label(windComp, SWT.NONE);
        slashLbl.setText("/");

        spdCbo = new Combo(windComp, SWT.READ_ONLY);
        spdCbo.add("MOV LTL");
        for (int i = 5; i <= 60;) {
            spdCbo.add(String.format("%03d", i));
            i += 5;
        }
        spdCbo.select(0);

        // Tops
        Label topsLbl = new Label(windComp, SWT.NONE);
        topsLbl.setText("  Tops:");

        topsFromCbo = new Combo(windComp, SWT.READ_ONLY);
        topsFromCbo.add("---");
        for (int i = 180; i <= 550;) {
            topsFromCbo.add(String.format("%03d", i));
            i += 10;
        }
        topsFromCbo.select(0);

        Label slash2Lbl = new Label(windComp, SWT.NONE);
        slash2Lbl.setText("/");

        topsToCbo = new Combo(windComp, SWT.READ_ONLY);
        for (int i = 250; i <= 600;) {
            topsToCbo.add(String.format("%03d", i));
            i += 10;
        }
        topsToCbo.select(0);

        // est
        estChk = new Button(windComp, SWT.CHECK);
        estChk.setText("Est");

        // severe
        Group severeGrp = new Group(this, SWT.NONE);
        severeGrp.setLayout(new GridLayout(3, false));
        severeGrp.setText("Severe");

        tornadoChk = new Button(severeGrp, SWT.CHECK);
        tornadoChk.setText("Tornado");

        hailChk = new Button(severeGrp, SWT.CHECK);
        hailChk.setText("Large Hail");

        gustChk = new Button(severeGrp, SWT.CHECK);
        gustChk.setText("> 50 Kt Sfc Gusts");

        // condition composite
        Composite conditionComp = new Composite(this, SWT.NONE);
        conditionComp.setLayout(new GridLayout(3, false));

        noCondRdo = new Button(conditionComp, SWT.RADIO);
        noCondRdo.setText("No Conds Remark");
        noCondRdo.setSelection(true);

        contgRdo = new Button(conditionComp, SWT.RADIO);
        contgRdo.setText("Conds Contg Byd");

        imprRdo = new Button(conditionComp, SWT.RADIO);
        imprRdo.setText("Conds Impr By");

        // report composite
        Composite reportComp = new Composite(this, SWT.NONE);
        reportComp.setLayout(new GridLayout(4, false));

        aircraftChk = new Button(reportComp, SWT.CHECK);
        aircraftChk.setText("Rpt by Aircraft");

        addnlInfoChk = new Button(reportComp, SWT.CHECK);
        addnlInfoChk.setText("Addnl Info for SIGMET");
        addnlInfoTxt = new Text(reportComp, SWT.BORDER);
        GridData gd = new GridData();
        gd.widthHint = 25;
        addnlInfoTxt.setLayoutData(gd);

        noUpdateChk = new Button(reportComp, SWT.CHECK);
        noUpdateChk.setText("No Updt Aft");
    }

    /**
     * create thunderstorm product text
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

        int topsTo = Integer.parseInt(topsToCbo.getText());
        int topsFrom = 0;
        if (topsFromCbo.getSelectionIndex() > 0) {
            topsFrom = Integer.parseInt(topsFromCbo.getText());
        }

        if (topsTo <= topsFrom) {
            MessageBox messageBox = new MessageBox(getShell(), SWT.OK);
            messageBox.setText("TOPS Selection");
            messageBox.setMessage("The Tops values " + topsToCbo.getText() + "/"
                    + topsFromCbo.getText() + " are invalid."
                    + "\nPlease reselect the Tops values.");
            messageBox.open();
            return "";
        }

        String severe = "";
        if (tornadoChk.getSelection() || hailChk.getSelection()
                || gustChk.getSelection()) {
            severe = " SVR";
        }

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

        StringBuilder output = new StringBuilder();
        output.append(getHeaderLines(wmoId, header, isCor));
        output.append(getValidLine(cwsuId, endDateTime, seriesId));
        output.append(fromline).append("\n");
        if (dvlpgChk.getSelection()) {
            output.append("DVLPG ");
        }
        if (body.startsWith("A") || body.startsWith("I")) {
            String body2 = body.substring(4);

            if (body.startsWith("A")) {
                output.append(body).append(" ");
            }

            output.append(coverageCbo.getText());
            if (embdChk.getSelection()) {
                output.append(" EMBD");
            }

            output.append(severe).append(" ").append(typeCbo.getText());

            if (body.startsWith("I")) {
                output.append(body2);
            }

            if (intstCbo.getSelectionIndex() == 0) {
                output.append(". ");
            } else {
                output.append(" WITH ").append(intstCbo.getText())
                        .append(" PCPN. ");
            }

            if (dirCbo.getText().equals("---")) {
                output.append("MOV LTL.\n");
            } else {
                output.append("MOV FROM ");
                output.append(dirCbo.getText()).append(spdCbo.getText());
                output.append("KT.\n");
            }

            output.append("TOPS");
            if (estChk.getSelection()) {
                output.append(" EST");
            }
            if (topsFromCbo.getSelectionIndex() == 0) {
                output.append(" TO FL");
                output.append(topsToCbo.getText());
            } else {
                output.append(" FL");
                output.append(topsFromCbo.getText()).append("-")
                        .append(topsToCbo.getText());
            }
            output.append(". ");

            if (tornadoChk.getSelection()) {
                output.append("TORNADO POSS. ");
            }
            if (hailChk.getSelection()) {
                output.append("LARGE HAIL POSS. ");
            }
            if (gustChk.getSelection()) {
                output.append("OVR 50KT WIND GUST POSS. ");
            }

            if (contgRdo.getSelection()) {
                output.append("CONDS CONTG BYD ");
                output.append(endDateTime).append("Z.");
            } else if (imprRdo.getSelection()) {
                output.append("CONDS IMPR BY ");
                output.append(endDateTime).append("Z.");
            }

            if (aircraftChk.getSelection()) {
                output.append("RPT BY AIRCRAFT. ");
            }
            output.append(" ");

            if (addnlInfoChk.getSelection()) {
                output.append("\n")
                        .append("THIS IS ADDN INFO TO CONVECTIVE SIGMET ")
                        .append(addnlInfoTxt.getText());
            }

            if (noUpdateChk.getSelection()) {
                output.append("NO UPDT AFT ").append(endDateTime).append("Z. ");
            }
            output.append("\n=\n\n");
        }
        return output.toString();
    }
}
