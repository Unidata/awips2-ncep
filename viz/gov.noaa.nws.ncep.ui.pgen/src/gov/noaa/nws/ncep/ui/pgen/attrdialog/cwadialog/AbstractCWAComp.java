/**
 * This software was developed and / or modified by NOAA/NWS/OCP/ASDT
 **/
package gov.noaa.nws.ncep.ui.pgen.attrdialog.cwadialog;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;

import com.raytheon.uf.common.time.util.TimeUtil;

/**
 * Abstract class for CWA composites
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date        Ticket#  Engineer    Description
 * ----------- -------- ----------- --------------------------
 * 02/02/2016  17469    wkwock      Initial creation
 * 
 * </pre>
 * 
 * @author wkwock
 */
public abstract class AbstractCWAComp extends Composite {
    /** start time check button */
    private Button startTimeChk;

    /** day Spinner */
    private Spinner daySpinner;

    /** start time */
    private DateTime startTime;

    /** duration combo */
    private Combo durationCbo;

    /** duration items */
    private final static String durationItems[] = { "30", "60", "90", "120" };

    /** horizontal indent */
    protected final int HORIZONTAL_INDENT = 20;

    /**
     * Constructor
     * 
     * @param parent
     * @param style
     */
    public AbstractCWAComp(Composite parent, int style) {
        super(parent, style);
    }

    /**
     * create the start time composite
     */
    protected void createTimeComp() {
        Composite timeComp = new Composite(this, SWT.NONE);
        timeComp.setLayout(new GridLayout(5, false));
        // Start time
        startTimeChk = new Button(timeComp, SWT.CHECK);
        startTimeChk.setText("Start Time:");
        daySpinner = new Spinner(timeComp, SWT.BORDER);
        Calendar calendar = TimeUtil.newCalendar();
        daySpinner.setMinimum(1);
        daySpinner.setMaximum(calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        daySpinner.setSelection(calendar.get(Calendar.DAY_OF_MONTH));
        GridData dayGd = new GridData();
        dayGd.widthHint = 15;
        daySpinner.setLayoutData(dayGd);
        startTime = new DateTime(timeComp, SWT.TIME | SWT.BORDER);

        // Duration
        Label durationLbl = new Label(timeComp, SWT.NONE);
        durationLbl.setText("Duration:");
        GridData durationGd = new GridData();
        durationGd.horizontalIndent = HORIZONTAL_INDENT;
        durationLbl.setLayoutData(durationGd);

        durationCbo = new Combo(timeComp, SWT.READ_ONLY);
        durationCbo.setItems(durationItems);
        durationCbo.select(0);
    }

    /**
     * get start time
     * 
     * @return start time
     */
    protected String getStartTime() {
        Calendar calendar = TimeUtil.newCalendar();
        if (startTimeChk.getSelection()) {
            calendar.set(Calendar.DAY_OF_YEAR, daySpinner.getSelection());
            calendar.set(Calendar.HOUR_OF_DAY, startTime.getHours());
            calendar.set(Calendar.MINUTE, startTime.getMinutes());
        }
        SimpleDateFormat sdfDate = new SimpleDateFormat("ddHHmm");
        return sdfDate.format(calendar.getTime());
    }

    /**
     * get end time
     * 
     * @return end time
     */
    protected String getEndTime() {
        Calendar calendar = TimeUtil.newCalendar();
        if (startTimeChk.getSelection()) {
            calendar.set(Calendar.DAY_OF_YEAR, daySpinner.getSelection());
            calendar.set(Calendar.HOUR_OF_DAY, startTime.getHours());
            calendar.set(Calendar.MINUTE, startTime.getMinutes());
        }
        int duration = Integer.parseInt(durationCbo.getText());
        calendar.add(Calendar.SECOND, duration * 60);
        SimpleDateFormat sdfDate = new SimpleDateFormat("ddHHmm");
        return sdfDate.format(calendar.getTime());
    }

    /**
     * get duration
     * 
     * @return duration
     */
    protected int getDuration() {
        int duration = Integer.parseInt(durationCbo.getText());
        return duration;
    }

    /**
     * get header lines
     * 
     * @param wmoId
     * @return header lines
     */
    protected String getHeaderLines(String wmoId, String header,
            boolean isCor) {
        String startDateTime = getStartTime();
        StringBuilder headerLines = new StringBuilder();
        headerLines.append(wmoId).append(" ").append(startDateTime)
                .append(" \n");
        headerLines.append(header).append(" ").append(startDateTime);
        if (isCor) {
            headerLines.append(" COR");
        }
        headerLines.append(" \n");

        return headerLines.toString();
    }

    /**
     * get the valid line
     * 
     * @param cwsuId
     * @param endDateTime
     * @param seriesId
     * @return the valid line
     */
    protected String getValidLine(String cwsuId, String endDateTime,
            int seriesId) {
        StringBuilder validLine = new StringBuilder();
        validLine.append(cwsuId).append(" CWA ").append(seriesId)
                .append(" VALID UNTIL ").append(endDateTime).append("Z \n");

        return validLine.toString();
    }

    /**
     * Create text product
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
    protected abstract String createText(String wmoId, String header,
            String fromline, String body, String cwsuId, String productId,
            boolean isCor, boolean isOperational);

}
