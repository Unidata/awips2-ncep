/**
 * This software was developed and / or modified by NOAA/NWS/OCP/ASDT
 **/
package gov.noaa.nws.ncep.ui.pgen.attrdialog.cwadialog;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import gov.noaa.nws.ncep.edex.common.stationTables.Station;

/**
 * This class displays the volcano composite.
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
public class VolcanoComp extends AbstractCWAComp {
    /** volcano stations combo */
    private Combo volcanoCbo;

    /** erupted radio button */
    private Button eruptedRdo;

    /** erupting radio button */
    private Button eruptingRdo;

    /** eruption stopped radio button */
    private Button stoppedRdo;

    /** possible eruption radio button */
    private Button possibleRdo;

    /** ash combo */
    private Combo ashCbo;

    /** tops combo */
    private Combo topsCbo;

    /** estimate check button */
    private Button estChk;

    /** plume direction combo */
    private Combo plumeDirCbo;

    /** plume speed combo */
    private Combo plumeSpdCbo;

    /** locations that ash could reach combo */
    private Combo reachCbo;

    /** within combo */
    private Combo withinCbo;

    /** volcano list */
    private List<Station> volcanoList;

    /** ash items */
    private final static String ashItems[] = { "Satellite", "Radar",
            "Satellite & Radar", "PIREP", "Webcam", "Volcano Observatory" };

    /** plume items */
    private final static String plumeItems[] = { "N", "NNE", "NE", "ENE", "E",
            "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW",
            "NWN" };

    /**
     * constructor
     * 
     * @param parent
     */
    public VolcanoComp(Composite parent) {
        super(parent, SWT.NONE);
        setLayout(new GridLayout(1, false));

        createTimeComp();

        // volcano
        Composite volcanoComp = new Composite(this, SWT.NONE);
        volcanoComp.setLayout(new GridLayout(2, false));
        Label volcanoLbl = new Label(volcanoComp, SWT.NONE);
        volcanoLbl.setText(" Volcano:");

        volcanoCbo = new Combo(volcanoComp, SWT.READ_ONLY);
        GridData gd = new GridData();
        gd.widthHint = 150;
        volcanoCbo.setLayoutData(gd);

        // status
        Composite statusComp = new Composite(this, SWT.NONE);
        statusComp.setLayout(new GridLayout(4, false));

        eruptedRdo = new Button(statusComp, SWT.RADIO);
        eruptedRdo.setText("Has Erupted");
        eruptedRdo.setSelection(true);

        eruptingRdo = new Button(statusComp, SWT.RADIO);
        eruptingRdo.setText("Continues to Erupt");

        stoppedRdo = new Button(statusComp, SWT.RADIO);
        stoppedRdo.setText("Has Stopped Erupting");

        possibleRdo = new Button(statusComp, SWT.RADIO);
        possibleRdo.setText("Possible Eruption");

        // Duration
        Composite ashComp = new Composite(this, SWT.NONE);
        ashComp.setLayout(new GridLayout(5, false));
        Label ashLbl = new Label(ashComp, SWT.NONE);
        ashLbl.setText("Ash Plume:");

        ashCbo = new Combo(ashComp, SWT.READ_ONLY);
        ashCbo.setItems(ashItems);
        ashCbo.select(0);

        Label topsLbl = new Label(ashComp, SWT.NONE);
        topsLbl.setText(" Tops:");

        topsCbo = new Combo(ashComp, SWT.READ_ONLY);
        for (int i = 10; i <= 400;) {
            topsCbo.add(String.format("%03d", i));
            i += 10;
        }
        for (int i = 450; i <= 600;) {
            topsCbo.add(Integer.toString(i));
            i += 50;
        }
        for (int i = 700; i <= 6000;) {
            topsCbo.add(Integer.toString(i));
            i += 100;
        }
        topsCbo.select(0);

        estChk = new Button(ashComp, SWT.CHECK);
        estChk.setText("Est");

        // plume dir
        Composite plumeComp = new Composite(this, SWT.NONE);
        plumeComp.setLayout(new GridLayout(4, false));
        Label plumeLbl = new Label(plumeComp, SWT.NONE);
        plumeLbl.setText("Plume Dir && Spd:");

        plumeDirCbo = new Combo(plumeComp, SWT.READ_ONLY);
        plumeDirCbo.setItems(plumeItems);
        plumeDirCbo.select(0);

        Label atLbl = new Label(plumeComp, SWT.NONE);
        atLbl.setText(" at");

        plumeSpdCbo = new Combo(plumeComp, SWT.READ_ONLY);
        plumeSpdCbo.add("MOV LTL");
        for (int i = 5; i <= 100;) {
            plumeSpdCbo.add(String.format("%02d KT", i));
            i += 5;
        }
        plumeSpdCbo.select(0);

        // ash reach
        Composite reachComp = new Composite(this, SWT.NONE);
        reachComp.setLayout(new GridLayout(4, false));
        Label reachLbl = new Label(reachComp, SWT.NONE);
        reachLbl.setText("Ash will reach:");

        reachCbo = new Combo(reachComp, SWT.READ_ONLY);
        gd = new GridData();
        gd.widthHint = 150;
        reachCbo.setLayoutData(gd);

        Label withinLbl = new Label(reachComp, SWT.NONE);
        withinLbl.setText(" within");

        withinCbo = new Combo(reachComp, SWT.READ_ONLY);
        withinCbo.add("");// 1st one blank
        for (int i = 1; i <= 6;) {
            withinCbo.add(String.format("%d HOURS", i));
            i++;
        }
        withinCbo.select(0);
    }

    /**
     * Create product text for volcano
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

        CWAProduct cwaProduct = new CWAProduct(productId, cwsuId,
                isOperational);
        int seriesId = cwaProduct.getNextSeriesId(isCor);

        // Check for isolated cell over VOR. Length would be 3 if it
        // is true.
        if (fromline.length() == 3) {
            fromline = "OVR " + fromline;
        }

        Station station = volcanoList.get(volcanoCbo.getSelectionIndex());

        StringBuilder output = new StringBuilder();
        output.append(getHeaderLines(wmoId, header, isCor));
        output.append(getValidLine(cwsuId, endDateTime, seriesId));

        output.append(fromline).append("\n");
        output.append(station.getStnname().toUpperCase())
                .append(" VOLCANO AT " + station.getLatitude() + " LAT "
                        + station.getLongitude() + " LON ");
        if (eruptedRdo.getSelection()) {
            output.append("HAS ERUPTED.\n");
        } else if (eruptingRdo.getSelection()) {
            output.append("CONTINUES TI ERUPT.\n");
        } else if (stoppedRdo.getSelection()) {
            output.append("HAS STOPPED ERUPTING.\n");
        } else {
            output.append("HAS POSSIBLE ERUPTION.\n");
        }

        output.append("ASH IS APPARENT ON ");
        if (ashCbo.getSelectionIndex() == 0) {
            output.append("SATELLITE.\n");
        } else if (ashCbo.getSelectionIndex() == 1) {
            output.append("NEXRAD RADAR.\n");
        } else if (ashCbo.getSelectionIndex() == 2) {
            output.append("SATELLITE AND NEXRAD RADAR.\n");
        } else {
            output.append(ashCbo.getText().toUpperCase()).append(".\n");
        }

        output.append("ASH TOPS ARE ");
        if (estChk.getSelection()) {
            output.append("ESTIMATED TO ");
        }
        int tops = Integer.parseInt(topsCbo.getText());
        if (tops >= 180) {
            output.append("FL");
        }
        output.append(topsCbo.getText()).append(".\n");

        output.append("PLUME IS MOVING ").append(plumeDirCbo.getText())
                .append(" AT ").append(plumeSpdCbo.getText()).append(".\n");

        // Skip "PUFF MODELS ..." if the 1st reach (blank) selected
        if (reachCbo.getSelectionIndex() != 0) {
            output.append("PUFF MODELS INDICATE ASH WILL REACH ")
                    .append(reachCbo.getText()).append(" WITHIN ")
                    .append(withinCbo.getText()).append(".\n");
        }
        output.append("THIS PRODUCT IS VALID UNTIL ").append(endDateTime);
        output.append(
                "\nOR UNTIL A VOLCANIC ASH SIGMET AND/OR VAA IS ISSUED.");
        return output.toString();
    }

    /**
     * update volcano list and reach cities
     * 
     * @param cwaConfigs
     */
    public void updateVolcano(CWAGeneratorConfigXML cwaConfigs) {
        this.volcanoList = cwaConfigs.getStations();
        for (Station station : cwaConfigs.getStations()) {
            volcanoCbo.add(station.getStnname());
        }
        volcanoCbo.select(0);

        reachCbo.add(""); // 1st one should always be a blank
        for (String reach : cwaConfigs.getReach()) {
            reachCbo.add(reach);
        }
        reachCbo.select(0);
    }
}
