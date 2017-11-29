/**
 * This software was developed and / or modified by NOAA/NWS/OCP/ASDT
 **/
package gov.noaa.nws.ncep.ui.pgen.attrdialog.cwadialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * This class displays the Blowing dust/blowing sand composite.
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
public class BlduBlsaComp extends AbstractCWAComp {
    /** Coverage combo */
    private Combo coverageCbo;

    /** type combo */
    private Combo typeCbo;

    /** wind direction combo */
    private Combo dirCbo;

    /** wind gust combo */
    private Combo gustCbo;

    /** visibility from combo */
    private Combo vsbyFromCbo;

    /** visibility to combo */
    private Combo vsbyToCbo;

    /** condition from combo */
    private Combo condFromCbo;

    /** condition to combo */
    private Combo condToCbo;

    /** coverage items */
    private final static String coverageItems[] = { "", "OCNL", "SCT",
            "WDSPRD" };

    /** type items */
    private final static String typeItems[] = { "BLDU", "BLSA" };

    /** gust items */
    private final static String gustItems[] = { "20-30 KTS", "30-40 KTS",
            "40-50 KTS", "50-60 KTS" };

    /** vsby from items */
    private final static String vsbyFromItems[] = { "AOB", "LOCLY AOB", "1/4",
            "1/2", "3/4" };

    /** vsby to items */
    private final static String vsbyToItems[] = { "1/2", "3/4", "1", "1 1/2",
            "3", "5" };

    /** cond from items */
    private final static String condFromItems[] = { "", "SPRDG", "???" };

    /** cond to items */
    private final static String condToItems[] = { "NWD", "NEWD", "EWD", "SEWD",
            "SWD", "SWWD", "WWD", "NWWD" };

    /**
     * Constructor
     * 
     * @param parent
     */
    public BlduBlsaComp(Composite parent) {
        super(parent, SWT.NONE);
        setLayout(new GridLayout(1, false));

        createTimeComp();

        Composite coverageComp = new Composite(this, SWT.NONE);
        coverageComp.setLayout(new GridLayout(5, false));
        Label coverageLbl = new Label(coverageComp, SWT.NONE);
        coverageLbl.setText("Coverage:");

        coverageCbo = new Combo(coverageComp, SWT.READ_ONLY);
        coverageCbo.setItems(coverageItems);
        coverageCbo.select(0);

        // type
        Label typeLbl = new Label(coverageComp, SWT.NONE);
        typeLbl.setText("Type:");
        GridData typeGd = new GridData();
        typeGd.horizontalIndent = HORIZONTAL_INDENT;
        typeLbl.setLayoutData(typeGd);

        typeCbo = new Combo(coverageComp, SWT.READ_ONLY);
        typeCbo.setItems(typeItems);
        typeCbo.select(0);

        Composite windComp = new Composite(this, SWT.NONE);
        windComp.setLayout(new GridLayout(4, false));
        Label windLbl = new Label(windComp, SWT.NONE);
        windLbl.setText("Wind Dir:");

        dirCbo = new Combo(windComp, SWT.READ_ONLY);
        for (int i = 10; i <= 360;) {
            dirCbo.add(String.format("%03d", i));
            i += 10;
        }
        dirCbo.select(0);

        Label gustLbl = new Label(windComp, SWT.NONE);
        gustLbl.setText("Gusts:");
        GridData gustGd = new GridData();
        gustGd.horizontalIndent = HORIZONTAL_INDENT;
        gustLbl.setLayoutData(gustGd);

        gustCbo = new Combo(windComp, SWT.READ_ONLY);
        gustCbo.setItems(gustItems);
        gustCbo.select(0);

        // VSBY
        Composite vsbyComp = new Composite(this, SWT.NONE);
        vsbyComp.setLayout(new GridLayout(7, false));
        Label vsbyLbl = new Label(vsbyComp, SWT.NONE);
        vsbyLbl.setText("VSBY:");

        vsbyFromCbo = new Combo(vsbyComp, SWT.READ_ONLY);
        vsbyFromCbo.setItems(vsbyFromItems);
        vsbyFromCbo.select(0);

        Label slash2Lbl = new Label(vsbyComp, SWT.NONE);
        slash2Lbl.setText("/");

        vsbyToCbo = new Combo(vsbyComp, SWT.READ_ONLY);
        vsbyToCbo.setItems(vsbyToItems);
        vsbyToCbo.select(0);

        Label condLbl = new Label(vsbyComp, SWT.NONE);
        condLbl.setText("Conditions:");
        GridData condGd = new GridData();
        condGd.horizontalIndent = HORIZONTAL_INDENT;
        condLbl.setLayoutData(condGd);

        condFromCbo = new Combo(vsbyComp, SWT.READ_ONLY);
        condFromCbo.setItems(condFromItems);
        condFromCbo.select(0);

        condToCbo = new Combo(vsbyComp, SWT.READ_ONLY);
        condToCbo.setItems(condToItems);
        condToCbo.select(0);
    }

    /**
     * Create product text for BLDU/BLSA
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
    public String createText(String wmoId, String header,
            String fromline, String body, String cwsuId, String productId,
            boolean isCor, boolean isOperational) {

        String endDateTime = getEndTime();

        // get next series ID
        CWAProduct cwaProduct = new CWAProduct(productId, cwsuId, isOperational);
        int seriesId = cwaProduct.getNextSeriesId(isCor);

        // Check for isolated cell over VOR. Length would be 3 if it
        // is true.
        if (fromline.length() == 3) {
            fromline = "OVR " + fromline;
        }

        String lvsby = vsbyFromCbo.getText();
        if (!lvsby.equals("AOB ") && !lvsby.equals("LOCLY AOB ")) {
            lvsby += "-";
        }

        StringBuilder output = new StringBuilder();
        output.append(getHeaderLines(wmoId, header, isCor));
        output.append(getValidLine(cwsuId, endDateTime, seriesId));
        output.append(fromline).append("\n");
        if (body.startsWith("A") || body.startsWith("I")) {

            if (body.startsWith("A")) {
                output.append(body).append(" ");
            }

            output.append(typeCbo.getText());
            output.append(" WITH SFC WNDS MOV FROM ");
            output.append(dirCbo.getText());

            output.append(" GUSTS ").append(gustCbo.getText())
                    .append("\n");
            output.append("VIS ").append(lvsby).append(vsbyToCbo.getText())
                    .append("SM\n");
            output.append("CONDS ").append(condFromCbo.getText()).append(" ")
                    .append(condToCbo.getText()).append(".");

            output.append("\n=\n\n");
        }
        return output.toString();
    }
}
