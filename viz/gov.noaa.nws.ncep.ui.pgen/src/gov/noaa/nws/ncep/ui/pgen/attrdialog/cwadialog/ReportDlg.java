/**
 * This software was developed and / or modified by NOAA/NWS/OCP/ASDT
 **/
package gov.noaa.nws.ncep.ui.pgen.attrdialog.cwadialog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.raytheon.uf.common.dataplugin.text.db.StdTextProduct;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.viz.ui.dialogs.CaveSWTDialog;

/**
 * This class displays the report dialog.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date        Ticket#  Engineer    Description
 * ----------- -------- ----------- --------------------------
 * 032/02/2017  17469    wkwock      Initial creation
 * 
 * </pre>
 * 
 * @author wkwock
 */
public class ReportDlg extends CaveSWTDialog {
    /** logger */
    private static final IUFStatusHandler logger = UFStatus
            .getHandler(CWAProduct.class);

    /** product text */
    private Text productTxt;

    /** AWIPS node */
    private String awipsNode;

    /** CWSU ID */
    private String cwsuId;

    /** is operational mode */
    private boolean isOperational;

    /** year combo */
    private Combo yearCbo;

    /** month combo */
    private Combo monthCbo;

    /** type combo */
    private Combo typeCbo;

    /** weather element types */
    private String[] types = { "Thunderstorm", "IFR/LIFR", "Turb/LLWS",
            "Icing/FRZA", "BLDU/BLSA", "Volcano", "Can/Man", "MIS" };

    /** type in the file name */
    private String[] filenameTypeClauses = { "thunder", "ifr", "turb", "icing",
            "bldu", "volcano", "canman", "MIS" };

    /**
     * Constructor
     * 
     * @param display
     * @param productId
     */
    public ReportDlg(Shell shell, String awipsNode, String cwsuId,
            Boolean isOperational) {
        super(shell, SWT.DIALOG_TRIM);
        setText("Report Generation");
        this.awipsNode = awipsNode;
        this.cwsuId = cwsuId;
        this.isOperational = isOperational;
    }

    @Override
    protected void initializeComponents(Shell shell) {
        final  int HORIZONTAL_INDENT = 20;

        GridLayout mainLayout = new GridLayout(1, false);
        mainLayout.marginHeight = 3;
        mainLayout.marginWidth = 3;
        mainLayout.verticalSpacing = 5;
        shell.setLayout(mainLayout);

        SelectionAdapter generateReportListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                generateReport();
            }
        };

        Composite timeComp = new Composite(shell, SWT.NONE);
        GridLayout gl = new GridLayout(7, false);
        timeComp.setLayout(gl);

        Label yearLbl = new Label(timeComp, SWT.NONE);
        yearLbl.setText("Year:");
        yearCbo = new Combo(timeComp, SWT.READ_ONLY);
        int year = TimeUtil.newCalendar().get(Calendar.YEAR);
        for (int i = year - 5; i <= year; i++) {
            yearCbo.add(Integer.toString(i));
        }
        yearCbo.select(5);

        Label monthLbl = new Label(timeComp, SWT.NONE);
        monthLbl.setText("Month:");
        GridData gd = new GridData();
        gd.horizontalIndent = HORIZONTAL_INDENT;
        monthLbl.setLayoutData(gd);

        monthCbo = new Combo(timeComp, SWT.READ_ONLY);
        DateFormatSymbols dfs = new DateFormatSymbols();
        String[] months = dfs.getShortMonths();
        for (int i = 0; i < 12; i++) {
            monthCbo.add(months[i]);
        }
        monthCbo.select(0);

        Label typeLbl = new Label(timeComp, SWT.NONE);
        typeLbl.setText("Report Type:");
        gd = new GridData();
        gd.horizontalIndent = HORIZONTAL_INDENT;
        typeLbl.setLayoutData(gd);
        typeCbo = new Combo(timeComp, SWT.READ_ONLY);
        typeCbo.setItems(types);
        typeCbo.select(0);

        Button generateBtn = new Button(timeComp, SWT.PUSH | SWT.CENTER);
        generateBtn.setText("Generate Report");
        generateBtn.addSelectionListener(generateReportListener);
        gd = new GridData();
        gd.horizontalIndent = HORIZONTAL_INDENT;
        generateBtn.setLayoutData(gd);

        productTxt = new Text(shell, SWT.MULTI | SWT.BORDER);
        gd = new GridData(SWT.FILL, SWT.DEFAULT, false, false);
        gd.heightHint = 260;
        gd.widthHint = 680;
        productTxt.setLayoutData(gd);
        productTxt.setEditable(false);

        Composite bottomComp = new Composite(shell, SWT.NONE);
        gl = new GridLayout(3, false);
        bottomComp.setLayout(gl);
        gd = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1);
        bottomComp.setLayoutData(gd);

        Button saveBtn = new Button(bottomComp, SWT.PUSH);
        saveBtn.setText("Save");
        saveBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                saveReport();
            }
        });

        Button exitBtn = new Button(bottomComp, SWT.PUSH | SWT.CENTER);
        exitBtn.setText("Exit");
        exitBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                close();
            }
        });
    }

    /**
     * save report to a file
     */
    private void saveReport() {
        if (productTxt.getText().isEmpty()) {
            return;
        }

        String fileName = monthCbo.getText() + yearCbo.getText() + "-"
                + filenameTypeClauses[typeCbo.getSelectionIndex()] + "-CWA.txt";
        DirectoryDialog dlg = new DirectoryDialog(shell);
        dlg.setText("Select a Directory");
        dlg.setMessage("Select a directory to save file " + fileName);
        String dir = dlg.open();
        if (dir != null) {
            try (PrintWriter out = new PrintWriter(
                    dir + File.separator + fileName)) {
                out.println(productTxt.getText());
            } catch (FileNotFoundException e) {
                logger.error("Failed to save report to file " + fileName, e);
            }
        }
    }

    /**
     * generate report
     */
    private void generateReport() {
        // get reports from textdb
        String nnnId;
        if (isOperational) {
            if (typeCbo.getSelectionIndex() == 7) {// MIS/CWS product
                nnnId = "CWS";
            } else {
                nnnId = "CWA";
            }
        } else {
            nnnId = "WRK";
        }

        String productId;
        List<StdTextProduct> productList;
        if (typeCbo.getSelectionIndex() == 7) {
            productId = "ALL:" + awipsNode + nnnId + cwsuId;
            productList = CWAProduct.executeAfosCmd(productId, true);
        } else {
            productList = new ArrayList<>();
            for (int i = 1; i < 7; i++) {
                productId = "ALL:" + awipsNode + nnnId + cwsuId.substring(1)
                        + i;
                productList.addAll(CWAProduct.executeAfosCmd(productId, true));
            }
        }

        if (productList == null || productList.isEmpty()) {
            productTxt.setText("");
            return;
        }

        // filter reports by time and type
        Iterator<StdTextProduct> iter = productList.iterator();
        while (iter.hasNext()) {
            StdTextProduct product = iter.next();
            Calendar refTime = TimeUtil.newCalendar(product.getRefTime());
            if (!Integer.toString(refTime.get(Calendar.YEAR))
                    .equals(yearCbo.getText())
                    || monthCbo.getSelectionIndex() != refTime
                            .get(Calendar.MONTH)) {
                iter.remove();
                continue;
            }

            String[] lines = product.getProduct().split("\n");
            if (lines.length < 5) {
                continue;
            }

            // filter by type
            // skip MIS filter because it has it's own unique nnnId
            int index = typeCbo.getSelectionIndex();
            if (index == 6) {// can/man
                if (lines[4].contains("TSRA") || lines[4].contains(" TS")) {
                    iter.remove();// remove thunderstorm reports
                } else if (lines[4].contains("IFR")) {
                    iter.remove(); // remove IFR reports
                } else if (lines[4].contains("TURB")) {
                    iter.remove(); // remove turbulence reports
                } else if (lines[4].contains("ICE") || lines[4].contains("FZDZ")
                        || lines[4].contains("FZRA")
                        || lines[4].contains("SLT")) {
                    iter.remove(); // remove icing/frza reports
                } else if (lines[4].contains("BLDU")
                        || lines[4].contains("BLSA")) {
                    iter.remove(); // remove BLDU/BLSA reports
                } else if (lines[4].contains("VOLCANO")) {
                    iter.remove(); // remove volcano reports
                }
            } else if (index != 7) {
                switch (index) {
                case 0:// determine thunderstorm reports
                    if (!(lines[4].contains("TSRA")
                            || lines[4].contains(" TS"))) {
                        iter.remove();
                    }
                    break;
                case 1:// determine IFR/LIFR reports
                    if (!lines[4].contains("IFR")) {
                        iter.remove();
                    }
                    break;
                case 2:// determine Turbulence//LLWS reports
                    if (!lines[4].contains("TURB")) {
                        iter.remove();
                    }
                    break;
                case 3:// determine Icing/FRZA reports
                    if (!(lines[4].contains("ICE") || lines[4].contains("FZDZ")
                            || lines[4].contains("FZRA")
                            || lines[4].contains("SLT"))) {
                        iter.remove();
                    }
                    break;
                case 4:// determine BLDU/BLSA reports
                    if (!(lines[4].contains("BLDU")
                            || lines[4].contains("BLSA"))) {
                        iter.remove();
                    }
                    break;
                case 5:// determine volcano reports
                    if (!lines[4].contains("VOLCANO")) {
                        iter.remove();
                    }
                    break;
                }
            }

        }

        // display report on the text field
        StringBuilder report = new StringBuilder();
        for (StdTextProduct product : productList) {
            if (report.length() != 0) {
                report.append("\n");
            }
            report.append(product.getProduct());
        }

        productTxt.setText(report.toString());
    }
}
