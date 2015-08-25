/**
 * This code has unlimited rights, and is provided "as is" by the National Centers 
 * for Environmental Prediction, without warranty of any kind, either expressed or implied, 
 * including but not limited to the implied warranties of merchantability and/or fitness 
 * for a particular purpose.
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 **/
package gov.noaa.nws.ncep.common.dataplugin.geomag.util;

import gov.noaa.nws.ncep.common.dataplugin.geomag.GeoMagAvg;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * 
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 22, 2014 RM5412      jtravis     Initial creation
 * 
 * </pre>
 * 
 * @author jtravis
 * @version 1.0
 */

public class ReportGenerator {

    private BufferedWriter buffOut = null;

    private static final String FILE_SEPARATOR = System
            .getProperty("file.separator");

    private static final String LINE_SEPARATOR = System
            .getProperty("line.separator");

    private static final String reportDirString = System
            .getProperty("user.home")
            + FILE_SEPARATOR
            + "AWIPS2-K-Index-Test-Results";

    private static String testDirString = null;

    private static File reportDir = null;

    private TestCondition testCondition = null;

    public static enum ReportType {
        CURRTIMEINDEX, DB, DA1, DA2, DDATA, DDDATA, DDATAEX, DDEV, DDEV_FINAL, DHRAVGS, DQDC, DQDC0, DATALIST, FITLENGTH, HA1, HA2, HDATA, HHDATA, HDATAEX, HDEV, HDEV_FINAL, HHRAVGS, HQDC, HQDC0, KINDEX, KINDEX_FINAL, QDAQDC, QDAVG_SHIFTED, QDAYS, QHAQDC, QHAVG_SHIFTED, QHRAVGD, QHRAVGH
    }

    /**
     * 
     */
    public ReportGenerator(TestCondition tc) throws Exception {

        if (tc.isInitialized()) {

            this.testCondition = tc;

            // create the reports dir if it does not yet exist
            reportDir = new File(ReportGenerator.reportDirString);

            if (!reportDir.exists()) {
                reportDir.mkdir();
            }

            if (!reportDir.canRead() || !reportDir.canWrite()) {
                throw new Exception(
                        "FAILURE - The "
                                + reportDir.getAbsolutePath()
                                + " cannot be written and/or read - check permissions - no reports will be created");
            }

            // create directory for the specific test
            testDirString = reportDir.getAbsolutePath() + FILE_SEPARATOR
                    + tc.getTestName();
            reportDir = new File(testDirString);

            if (!reportDir.exists()) {
                reportDir.mkdir();
            }

            if (!reportDir.canRead() || !reportDir.canWrite()) {
                throw new Exception(
                        "FAILURE - The "
                                + reportDir.getAbsolutePath()
                                + " cannot be written and/or read - check permissions - no reports will be created");
            }

        } else {
            throw new Exception(
                    "FAILURE - The Test Condition was not initialized so cannot create report(s)");
        }

    }

    public void generateReport(ReportType type, int count, double[] data) {

        File report = null;

        switch (type) {
        case DB:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".db.csv");
            break;
        case DA1:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".da1.csv");
            break;
        case DA2:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".da2.csv");
            break;
        case DDATA:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".ddata.csv");
            break;
        case DDDATA:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".dddata.csv");
            break;
        case DDATAEX:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".ddataex.csv");
            break;
        case DDEV:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".ddev.csv");
            break;
        // case DDEV2:
        // report = new File(ReportGenerator.reportDir
        // + ReportGenerator.FILE_SEPARATOR
        // + this.testCondition.getTestName() + "." + count
        // + ".ddev2.csv");
        // break;
        case DDEV_FINAL:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".ddev-final.csv");
            break;
        case DHRAVGS:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".dhravgs.csv");
            break;
        case DQDC0:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".dqdc0.csv");
            break;
        case DQDC:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".dqdc.csv");
            break;
        // case DQDC3:
        // report = new File(ReportGenerator.reportDir
        // + ReportGenerator.FILE_SEPARATOR
        // + this.testCondition.getTestName() + "." + count
        // + ".dqdc3.csv");
        // break;
        // case DQDC4:
        // report = new File(ReportGenerator.reportDir
        // + ReportGenerator.FILE_SEPARATOR
        // + this.testCondition.getTestName() + "." + count
        // + ".dqdc4.csv");
        // break;
        case FITLENGTH:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".fitlength.csv");
            break;
        case HA1:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".ha1.csv");
            break;
        case HA2:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".ha2.csv");
            break;
        case HDATA:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".hdata.csv");
            break;
        case HHDATA:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".hhdata.csv");
            break;
        case HDATAEX:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".hdataex.csv");
            break;
        case HDEV:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".hdev.csv");
            break;
        // case HDEV2:
        // report = new File(ReportGenerator.reportDir
        // + ReportGenerator.FILE_SEPARATOR
        // + this.testCondition.getTestName() + "." + count
        // + ".hdev2.csv");
        // break;
        case HDEV_FINAL:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".hdev-final.csv");
            break;
        case HHRAVGS:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".hhravgs.csv");
            break;
        case HQDC0:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".hqdc0.csv");
            break;
        case HQDC:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".hqdc.csv");
            break;
        // case HQDC3:
        // report = new File(ReportGenerator.reportDir
        // + ReportGenerator.FILE_SEPARATOR
        // + this.testCondition.getTestName() + "." + count
        // + ".hqdc3.csv");
        // break;
        // case HQDC4:
        // report = new File(ReportGenerator.reportDir
        // + ReportGenerator.FILE_SEPARATOR
        // + this.testCondition.getTestName() + "." + count
        // + ".hqdc4.csv");
        // break;
        case KINDEX:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".kindex.csv");
            break;
        case KINDEX_FINAL:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".kindex-final.csv");
            break;
        case QDAQDC:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".qdaqdc.csv");
            break;
        case QDAVG_SHIFTED:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".qdavg_shifted.csv");
            break;
        case QDAYS:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".qdays.csv");
            break;
        case QHAQDC:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".qhaqdc.csv");
            break;
        case QHAVG_SHIFTED:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".qhavg_shifted.csv");
            break;
        case QHRAVGD:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".qhravgd.csv");
            break;
        case QHRAVGH:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".qhravgh.csv");
            break;
        }

        try {

            // if file doesn't exists, then create it
            if (!report.exists()) {
                report.createNewFile();
            }

            // init the buffer
            buffOut = new BufferedWriter(new FileWriter(report));

            for (int i = 0; i < data.length; i++) {
                buffOut.write(data[i] + ReportGenerator.LINE_SEPARATOR);
            }

            buffOut.close();

        } catch (IOException e) {
            System.err.println("FAILURE - Failed To Write Report for: " + type);
            e.printStackTrace();
        }

    }

    public void generateReport(ReportType type, int count, int data) {

        File report = null;

        switch (type) {
        case CURRTIMEINDEX:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".currTimeIndex.csv");
            break;
        }

        try {

            // if file doesn't exists, then create it
            if (!report.exists()) {
                report.createNewFile();
            }

            // init the buffer
            buffOut = new BufferedWriter(new FileWriter(report));
            buffOut.write(data + ReportGenerator.LINE_SEPARATOR);

            buffOut.close();

        } catch (IOException e) {
            System.err.println("FAILURE - Failed To Write Report for: " + type);
            e.printStackTrace();
        }

    }

    public void generateReport(ReportType type, int count, List<double[]> data) {

        File report = null;

        switch (type) {
        case DB:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".db.csv");
            break;
        case DA1:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".da1.csv");
            break;
        case DA2:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".da2.csv");
            break;
        case DDATA:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".ddata.csv");
            break;
        case DDDATA:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".dddata.csv");
            break;
        case DDATAEX:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".ddataex.csv");
            break;
        case DDEV:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".ddev.csv");
            break;
        // case DDEV2:
        // report = new File(ReportGenerator.reportDir
        // + ReportGenerator.FILE_SEPARATOR
        // + this.testCondition.getTestName() + "." + count
        // + ".ddev2.csv");
        // break;
        case DDEV_FINAL:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".ddev-final.csv");
            break;
        case DQDC0:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".dqdc0.csv");
            break;
        case DQDC:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".dqdc.csv");
            break;
        // case DQDC3:
        // report = new File(ReportGenerator.reportDir
        // + ReportGenerator.FILE_SEPARATOR
        // + this.testCondition.getTestName() + "." + count
        // + ".dqdc3.csv");
        // break;
        // case DQDC4:
        // report = new File(ReportGenerator.reportDir
        // + ReportGenerator.FILE_SEPARATOR
        // + this.testCondition.getTestName() + "." + count
        // + ".dqdc4.csv");
        // break;
        case FITLENGTH:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".fitlength.csv");
            break;
        case HA1:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".ha1.csv");
            break;
        case HA2:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".ha2.csv");
            break;
        case HDATA:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".hdata.csv");
            break;
        case HHDATA:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".hhdata.csv");
            break;
        case HDATAEX:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".hdataex.csv");
            break;
        case HDEV:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".hdev.csv");
            break;
        // case HDEV2:
        // report = new File(ReportGenerator.reportDir
        // + ReportGenerator.FILE_SEPARATOR
        // + this.testCondition.getTestName() + "." + count
        // + ".hdev2.csv");
        // break;
        case HDEV_FINAL:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".hdev-final.csv");
            break;
        case HQDC0:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".hqdc0.csv");
            break;
        case HQDC:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".hqdc.csv");
            break;
        // case HQDC3:
        // report = new File(ReportGenerator.reportDir
        // + ReportGenerator.FILE_SEPARATOR
        // + this.testCondition.getTestName() + "." + count
        // + ".hqdc3.csv");
        // break;
        // case HQDC4:
        // report = new File(ReportGenerator.reportDir
        // + ReportGenerator.FILE_SEPARATOR
        // + this.testCondition.getTestName() + "." + count
        // + ".hqdc4.csv");
        // break;
        case KINDEX:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".kindex.csv");
            break;
        case KINDEX_FINAL:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".kindex-final.csv");
            break;
        case QDAQDC:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".qdaqdc.csv");
            break;
        case QDAVG_SHIFTED:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".qdavg_shifted.csv");
            break;
        case QDAYS:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".qdays.csv");
            break;
        case QHAQDC:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".qhaqdc.csv");
            break;
        case QHAVG_SHIFTED:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".qhavg_shifted.csv");
            break;
        case QHRAVGD:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".qhravgd.csv");
            break;
        case QHRAVGH:
            report = new File(ReportGenerator.reportDir
                    + ReportGenerator.FILE_SEPARATOR
                    + this.testCondition.getTestName() + "." + count
                    + ".qhravgh.csv");
            break;
        }

        try {

            // if file doesn't exists, then create it
            if (!report.exists()) {
                report.createNewFile();
            }

            // init the buffer
            buffOut = new BufferedWriter(new FileWriter(report));

            for (int i = 0; i < data.get(0).length; i++) {
                buffOut.write(data.get(0)[i] + "," + data.get(1)[i] + ","
                        + data.get(2)[i] + "," + data.get(3)[i] + ","
                        + data.get(4)[i] + "," + data.get(5)[i]
                        + ReportGenerator.LINE_SEPARATOR);
            }

            buffOut.close();

        } catch (IOException e) {
            System.err.println("FAILURE - Failed To Write Report for: " + type);
            e.printStackTrace();
        }

    }

    public void generateDataListReport(int count, List<GeoMagAvg> dataList) {

        File report = new File(ReportGenerator.reportDir
                + ReportGenerator.FILE_SEPARATOR
                + this.testCondition.getTestName() + "." + count
                + ".datalist.csv");

        try {

            // if file doesn't exists, then create it
            if (!report.exists()) {
                report.createNewFile();
            }

            // init the buffer
            buffOut = new BufferedWriter(new FileWriter(report));

            for (int i = 0; i < dataList.size(); i++) {
                buffOut.write(dataList.get(i).getAvgTime()
                        + ReportGenerator.LINE_SEPARATOR);
            }

            buffOut.close();

        } catch (IOException e) {
            System.err
                    .println("FAILURE - Failed To Write Report for DATALIST ");
            e.printStackTrace();
        }

    }
}
