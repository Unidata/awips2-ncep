package gov.noaa.nws.ncep.ui.nsharp.view;

/**
 * 
 * gov.noaa.nws.ncep.ui.nsharp.palette.NsharpPrintHandle
 * 
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    	Engineer    Description
 * -------		------- 	-------- 	-----------
 * 03/23/2010	229			Chin Chen	Initial coding
 * 07/05/2016   RM#15923    Chin Chen   NSHARP - Native Code replacement
 * 
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 */
import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibBasics;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.Helicity;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.Parcel;
import gov.noaa.nws.ncep.edex.common.nsharpLib.struct.WindComponent;
import gov.noaa.nws.ncep.ui.nsharp.NsharpConstants;
import gov.noaa.nws.ncep.ui.nsharp.NsharpWGraphics;
import gov.noaa.nws.ncep.ui.nsharp.display.NsharpEditor;
import gov.noaa.nws.ncep.ui.nsharp.display.rsc.NsharpResourceHandler;
import gov.noaa.nws.ncep.ui.nsharp.display.rsc.NsharpWeatherDataStore;
import gov.noaa.nws.ncep.ui.nsharp.natives.NsharpNativeConstants;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

import com.raytheon.uf.viz.core.exception.VizException;

public class NsharpPrintHandle {
    private Printer printer;

    private StringBuffer wordBuffer;

    private int lineHeight = 0;

    private int tabWidth = 0;

    private int leftMargin, rightMargin, topMargin, bottomMargin;

    private GC gc;

    private static int SKEWT_X_ORIG = 0;

    private static int SKEWT_HEIGHT = 375;

    private static int SKEWT_WIDTH = 420;

    private static int HODO_X_ORIG = SKEWT_X_ORIG + SKEWT_WIDTH - 130;

    private static int HODO_HEIGHT = 130;

    private static int HODO_WIDTH = HODO_HEIGHT;

    private Font printerFont;

    private Color printerForegroundColor, printerBackgroundColor;

    private Transform transform;

    private static NsharpPrintHandle printHandle = null;

    public static NsharpPrintHandle getPrintHandle() {
        if (printHandle == null)
            printHandle = new NsharpPrintHandle();
        return printHandle;
    }

    public void handlePrint(String intext) {
        Job uijob = new UIJob("clear source selection") {
            public IStatus runInUIThread(IProgressMonitor monitor) {
                Shell shell = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getShell();
                PrintDialog dialog = new PrintDialog(shell, SWT.NULL);
                PrinterData data = dialog.open();

                if (data == null)
                    return Status.CANCEL_STATUS;
                if (data.printToFile) {
                    String fileName = data.fileName;
                    int i = fileName.indexOf("///");
                    if (i != -1)
                        data.fileName = fileName.substring(0, i)
                                + fileName.substring(i + 2);

                }
                try {
                    // Do the printing in a background thread so that spooling
                    // does not freeze the UI.
                    createPrinter(data);
                    if (startJob()) {
                        printPage();
                        endJob();
                    }
                    disposePrinter();
                } catch (Exception e) {

                    e.printStackTrace();
                }
                return Status.OK_STATUS;
            }

        };
        uijob.setSystem(true);
        uijob.schedule();

    }

    public void createPrinter(PrinterData data) {
        this.printer = new Printer(data);
    }

    public boolean startJob() {
        String tabs;
        if (printer.startJob("NSHARP")) {
            Rectangle clientArea = printer.getClientArea();
            Rectangle trim = printer.computeTrim(0, 0, 0, 0);
            Point dpi = printer.getDPI();

            float dpiScaleX = dpi.x / 72f;
            float dpiScaleY = dpi.y / 72f;

            transform = new Transform(printer);
            transform.scale(dpiScaleX, dpiScaleY);

            leftMargin = 72 + trim.x; // one inch from left side of paper
            rightMargin = clientArea.width - 72 + trim.x + trim.width;
            topMargin = 72 + trim.y; // one inch from top edge of paper
            bottomMargin = clientArea.height - 72 + trim.y + trim.height;
            /* Create a buffer for computing tab width. */
            int tabSize = 4;
            StringBuffer tabBuffer = new StringBuffer(tabSize);
            for (int i = 0; i < tabSize; i++)
                tabBuffer.append(' ');
            tabs = tabBuffer.toString();

            /*
             * Create printer GC, and create and set the printer font &
             * foreground color.
             */
            gc = new GC(printer);
            int fontSize = (int) Math.round(5 / dpiScaleY);
            fontSize = Math.max(1, fontSize);
            printerFont = new Font(printer, "Courier", fontSize, SWT.NORMAL);
            gc.setFont(printerFont);
            tabWidth = gc.stringExtent(tabs).x;
            lineHeight = gc.getFontMetrics().getHeight();

            RGB rgb = new RGB(0, 0, 0);// Black
            printerForegroundColor = new Color(printer, rgb);
            gc.setForeground(printerForegroundColor);
            rgb = new RGB(255, 255, 255);// white
            printerBackgroundColor = new Color(printer, rgb);
            gc.setBackground(printerBackgroundColor);
            gc.setTransform(transform);
            return true;
        }
        return false;
    }

    public void printPage() {
        NsharpEditor editor = NsharpEditor.getActiveNsharpEditor();
        if (editor != null) {
            NsharpResourceHandler rscHandler = editor.getRscHandler();
            if (rscHandler != null) {
                printer.startPage();
                // Print SkewT square
                gc.drawRectangle(leftMargin + SKEWT_X_ORIG, topMargin,
                        SKEWT_WIDTH, SKEWT_HEIGHT);
                // set view dimension
                NsharpWGraphics world = new NsharpWGraphics(leftMargin
                        + SKEWT_X_ORIG, topMargin, leftMargin + SKEWT_X_ORIG
                        + SKEWT_WIDTH, topMargin + SKEWT_HEIGHT);
                // set SKEWT virtual world coordinate.
                world.setWorldCoordinates(NsharpConstants.left,
                        NsharpConstants.top, NsharpConstants.right,
                        NsharpConstants.bottom);
                gc.setLineWidth(1);

                try {

                    gc.setClipping(leftMargin - 30, topMargin - 30,
                            rightMargin + 30, bottomMargin);
                    gc.drawString(rscHandler.getPickedStnInfoStr(), leftMargin
                            + SKEWT_X_ORIG, topMargin - 20);
                    rscHandler.printNsharpPressureLinesNumber(world, gc);
                    rscHandler.printNsharpTempNumber(world, gc);
                    rscHandler.printHeightMark(world, gc);
                    rscHandler.printNsharpWind(world, gc);
                    // set clipping
                    gc.setClipping(leftMargin + SKEWT_X_ORIG, topMargin,
                            SKEWT_WIDTH, SKEWT_HEIGHT);
                    // print skewt background
                    rscHandler.getSkewtPaneRsc().getSkewTBackground()
                            .paintForPrint(world, gc);
                    gc.setLineWidth(2);
                    gc.setLineStyle(SWT.LINE_SOLID);
                    rscHandler.printNsharpPressureTempCurve(world,
                            rscHandler.TEMP_TYPE, gc,
                            rscHandler.getSoundingLys());
                    rscHandler.printNsharpPressureTempCurve(world,
                            rscHandler.DEWPOINT_TYPE, gc,
                            rscHandler.getSoundingLys());

                    gc.setLineStyle(SWT.LINE_DASH);
                    rscHandler.printNsharpWetbulbTraceCurve(world, gc);
                    gc.setLineStyle(SWT.LINE_DASHDOTDOT);
                    rscHandler.printNsharpParcelTraceCurve(world, gc);
                    gc.setLineStyle(SWT.LINE_SOLID);
                    // fill/cover this skewt area to be used by Hodo
                    gc.fillRectangle(leftMargin + HODO_X_ORIG, topMargin,
                            HODO_WIDTH, HODO_HEIGHT);

                } catch (VizException e) {
                    e.printStackTrace();
                }
                gc.setLineWidth(2);
                // Print Hodo square
                gc.drawRectangle(leftMargin + HODO_X_ORIG, topMargin,
                        HODO_WIDTH, HODO_HEIGHT);

                // set HODO view world
                world = new NsharpWGraphics(leftMargin + HODO_X_ORIG,
                        topMargin, leftMargin + HODO_X_ORIG + HODO_WIDTH,
                        topMargin + HODO_HEIGHT);
                // set HODO real world coordinate.
                world.setWorldCoordinates(-50, 90, 90, -50);

                gc.setLineWidth(1);
                // print hodo background
                rscHandler.getHodoPaneRsc().getHodoBackground()
                        .paintForPrint(world, gc);
                try {
                    // print hodo
                    gc.setLineStyle(SWT.LINE_SOLID);

                    rscHandler.printNsharpHodoWind(world, gc,
                            rscHandler.getSoundingLys());
                } catch (VizException e) {
                    e.printStackTrace();
                }

                // reset clipping
                gc.setClipping(leftMargin - 15, topMargin + SKEWT_HEIGHT,
                        rightMargin - leftMargin + 30, bottomMargin - topMargin
                                + 30);

                gc.setLineWidth(2);
                // print thermodynamic data title and its box
                gc.drawString("THERMODYNAMIC PARAMETERS", leftMargin + 50,
                        topMargin + SKEWT_HEIGHT + 20);

                gc.drawLine(leftMargin - 15, topMargin + SKEWT_HEIGHT + 30,
                        leftMargin + 205, topMargin + SKEWT_HEIGHT + 30);

                String textStr = printThermodynamicParametersBox1(rscHandler);

                int curY = printText(textStr, leftMargin - 12, topMargin
                        + SKEWT_HEIGHT + 35, leftMargin + 205, topMargin
                        + SKEWT_HEIGHT + 295);
                gc.drawLine(leftMargin - 15, curY, leftMargin + 205, curY);
                gc.drawLine(leftMargin - 15, curY, leftMargin - 15, topMargin
                        + SKEWT_HEIGHT + 30);
                gc.drawLine(leftMargin + 205, curY, leftMargin + 205, topMargin
                        + SKEWT_HEIGHT + 30);
                String str1 = "", str2 = "", str3 = "";
                textStr = printThermodynamicParametersBox2(rscHandler);
                int gapIndex1 = textStr.indexOf("BOXLINE");
                str1 = textStr.substring(0, gapIndex1);
                int gapIndex2 = textStr.indexOf("BOXLINE", gapIndex1 + 1);
                str2 = textStr.substring(gapIndex1 + ("BOXLINE".length()),
                        gapIndex2);
                str3 = textStr.substring(gapIndex2 + ("BOXLINE".length()));
                int preY = curY;
                curY = printText(str1, leftMargin - 12, curY + 1,
                        leftMargin + 205, topMargin + SKEWT_HEIGHT + 295);
                gc.drawLine(leftMargin - 15, curY, leftMargin + 205, curY);
                gc.drawLine(leftMargin - 15, curY, leftMargin - 15, preY);
                gc.drawLine(leftMargin + 205, curY, leftMargin + 205, preY);
                preY = curY;
                curY = printText(str2, leftMargin - 12, curY + 1,
                        leftMargin + 205, topMargin + SKEWT_HEIGHT + 295);
                gc.drawLine(leftMargin - 15, curY, leftMargin + 205, curY);
                gc.drawLine(leftMargin - 15, curY, leftMargin - 15, preY);
                gc.drawLine(leftMargin + 205, curY, leftMargin + 205, preY);
                preY = curY;
                curY = printText(str3, leftMargin - 12, curY + 1,
                        leftMargin + 205, topMargin + SKEWT_HEIGHT + 295);
                gc.drawLine(leftMargin - 15, curY, leftMargin + 205, curY);
                gc.drawLine(leftMargin - 15, curY, leftMargin - 15, preY);
                gc.drawLine(leftMargin + 205, curY, leftMargin + 205, preY);

                textStr = "Output produced by: NCO-SIB AWIPS2 NSHARP\nNational SkewT-Hodograph Analysis and Research Program\n";
                printText(textStr, leftMargin - 12, curY + 100,
                        leftMargin + 300, curY + 120);

                // print kinematic data title and its box
                gc.drawString("KINEMATIC PARAMETERS", leftMargin + 280,
                        topMargin + SKEWT_HEIGHT + 20);
                gc.drawLine(leftMargin + 225, topMargin + SKEWT_HEIGHT + 30,
                        leftMargin + 445, topMargin + SKEWT_HEIGHT + 30);
                textStr = printKinematicParametersBox(rscHandler);
                curY = printText(textStr, leftMargin + 228, topMargin
                        + SKEWT_HEIGHT + 35, leftMargin + 444, topMargin
                        + SKEWT_HEIGHT + 295);

                gc.drawLine(leftMargin + 225, curY, leftMargin + 445, curY);
                gc.drawLine(leftMargin + 225, curY, leftMargin + 225, topMargin
                        + SKEWT_HEIGHT + 30);
                gc.drawLine(leftMargin + 445, curY, leftMargin + 445, topMargin
                        + SKEWT_HEIGHT + 30);

                // print STORM STRUCTURE PARAMETERS
                gc.drawString("STORM STRUCTURE PARAMETERS", leftMargin + 280,
                        curY + 20);
                gc.drawLine(leftMargin + 225, curY + 30, leftMargin + 445,
                        curY + 30);
                textStr = printStormStructureParametersBox(rscHandler);
                preY = curY + 30;
                curY = printText(textStr, leftMargin + 228, curY + 35,
                        leftMargin + 444, topMargin + SKEWT_HEIGHT + 295);
                gc.drawLine(leftMargin + 225, curY, leftMargin + 445, curY);
                gc.drawLine(leftMargin + 225, curY, leftMargin + 225, preY);
                gc.drawLine(leftMargin + 445, curY, leftMargin + 445, preY);

                printer.endPage();
            }
        }
    }

    public void endJob() {
        printer.endJob();

        /* Cleanup graphics resources used in printing */
        printerFont.dispose();
        printerForegroundColor.dispose();
        printerBackgroundColor.dispose();
        transform.dispose();
        gc.dispose();
    }

    public void disposePrinter() {
        printer.dispose();
    }

    private int x, y;

    private int index, end;

    private int printText(String textToPrint, int xmin, int ymin, int xmax,
            int ymax) {

        wordBuffer = new StringBuffer();
        x = xmin;
        y = ymin;
        index = 0;
        end = textToPrint.length();
        while (index < end) {
            char c = textToPrint.charAt(index);
            index++;
            if (c != 0) {
                if (c == 0x0a || c == 0x0d) {
                    if (c == 0x0d && index < end
                            && textToPrint.charAt(index) == 0x0a) {
                        index++;
                    }
                    printWordBuffer(xmin, ymin, xmax, ymax);
                    newline(xmin, ymin, xmax, ymax);
                } else {
                    if (c != '\t') {
                        wordBuffer.append(c);
                    }
                    if (Character.isWhitespace(c)) {
                        printWordBuffer(xmin, ymin, xmax, ymax);
                        if (c == '\t') {
                            x += tabWidth;
                        }
                    }
                }
            }
        }
        return y;
    }

    void printWordBuffer(int xmin, int ymin, int xmax, int ymax) {
        if (wordBuffer.length() > 0) {
            String word = wordBuffer.toString();
            int wordWidth = gc.stringExtent(word).x;
            if (x + wordWidth > xmax) {
                /* word doesn't fit on current line, so wrap */
                System.out
                        .println("word doesn't fit on current line, so wrap ");
                newline(xmin, ymin, xmax, ymax);
            }
            gc.drawString(word, x, y, false);

            x += wordWidth;
            wordBuffer = new StringBuffer();
        }
    }

    void newline(int xmin, int ymin, int xmax, int ymax) {
        x = xmin;
        y += lineHeight;
    }

    protected NsharpWGraphics computeWorld(int x1, int y1, int x2, int y2) {
        NsharpWGraphics world = new NsharpWGraphics(x1, y1, x1, y1);
        world.setWorldCoordinates(-50, 90, 90, -50);
        return world;
    }

    private String printKinematicParametersBox(NsharpResourceHandler rscHandler) {
        /*
         * this function is for printing and coded based on legacy nsharp
         * software show_meanwind() in xwvid3.c
         */
        NsharpWeatherDataStore weatherDataStore = rscHandler
                .getWeatherDataStore();
        String textStr, finalTextStr = "\t";
        finalTextStr = finalTextStr + NsharpNativeConstants.MEAN_WIND_STR;

        // mean wind at 0-6 km
        WindComponent meanWind = weatherDataStore.getStormTypeToMeanWindMap()
                .get("SFC-6km");
        if (NsharpLibBasics.qc(meanWind.getWdir())
                && NsharpLibBasics.qc(meanWind.getWspd())) {
            textStr = NsharpNativeConstants.MEANWIND_SFC6KM_LINE;
            textStr = String.format(textStr, meanWind.getWdir(),
                    meanWind.getWspd(),
                    NsharpLibBasics.kt_to_mps(meanWind.getWspd()));
        } else {
            textStr = NsharpNativeConstants.MEANWIND_SFC6KM_MISSING;
        }
        finalTextStr = finalTextStr + textStr;
        // mean wind at LFC-EL
        int currentParcel = rscHandler.getCurrentParcel();
        NsharpWeatherDataStore.ParcelMiscParams parcelMisc = weatherDataStore
                .getParcelMiscParamsMap().get(currentParcel);
        meanWind = null;
        if (parcelMisc != null) {
            meanWind = parcelMisc.getMeanWindCompLfcToEl();
        }
        if (meanWind != null && NsharpLibBasics.qc(meanWind.getWdir())
                && NsharpLibBasics.qc(meanWind.getWspd())) {
            textStr = NsharpNativeConstants.MEANWIND_LFC_EL_LINE;
            textStr = String.format(textStr, meanWind.getWdir(),
                    meanWind.getWspd(),
                    NsharpLibBasics.kt_to_mps(meanWind.getWspd()));
        } else {
            textStr = NsharpNativeConstants.MEANWIND_LFC_EL_MISSING;
        }
        finalTextStr = finalTextStr + textStr;
        // mean wind at 850-200 mb
        meanWind = weatherDataStore.getMeanWindComp850To200mb();
        if (NsharpLibBasics.qc(meanWind.getWdir())
                && NsharpLibBasics.qc(meanWind.getWspd())) {
            textStr = NsharpNativeConstants.MEANWIND_850_200MB_LINE;
            textStr = String.format(textStr, meanWind.getWdir(),
                    meanWind.getWspd(),
                    NsharpLibBasics.kt_to_mps(meanWind.getWspd()));
        } else {
            textStr = NsharpNativeConstants.MEANWIND_850_200MB_MISSING;
        }
        finalTextStr = finalTextStr + textStr + "\n";

        /*
         * the following function is for pritning and coded based on legacy
         * nsharp software show_shear() in xwvid3.c
         */
        finalTextStr = finalTextStr
                + NsharpNativeConstants.ENVIRONMENTAL_SHEAR_STR;

        finalTextStr = finalTextStr
                + NsharpNativeConstants.SHEAR_LAYER_DELTA_STR;

        // wind shear at Low - 3 km
        float shearWind = weatherDataStore.getStormTypeToWindShearMap().get(
                "SFC-3km");
        if (NsharpLibBasics.qc(shearWind)) {
            textStr = NsharpNativeConstants.SHEAR_LOW_3KM_LINE;
            textStr = String.format(textStr, shearWind,
                    NsharpLibBasics.kt_to_mps(shearWind),
                    NsharpLibBasics.kt_to_mps(shearWind) / .3F);
        } else {
            textStr = NsharpNativeConstants.SHEAR_LOW_3KM_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        // wind shear at Sfc - 2 km
        shearWind = weatherDataStore.getStormTypeToWindShearMap()
                .get("SFC-2km");
        if (NsharpLibBasics.qc(shearWind)) {
            textStr = NsharpNativeConstants.SHEAR_SFC_2KM_LINE;
            textStr = String.format(textStr, shearWind,
                    NsharpLibBasics.kt_to_mps(shearWind),
                    NsharpLibBasics.kt_to_mps(shearWind) / .3F);
        } else {
            textStr = NsharpNativeConstants.SHEAR_SFC_2KM_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        // wind shear at Sfc - 6 km
        shearWind = weatherDataStore.getStormTypeToWindShearMap()
                .get("SFC-6km");
        if (NsharpLibBasics.qc(shearWind)) {
            textStr = NsharpNativeConstants.SHEAR_SFC_6KM_LINE;
            textStr = String.format(textStr, shearWind,
                    NsharpLibBasics.kt_to_mps(shearWind),
                    NsharpLibBasics.kt_to_mps(shearWind) / .3F);
        } else {
            textStr = NsharpNativeConstants.SHEAR_SFC_6KM_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        // wind shear at Sfc - 12 km
        shearWind = weatherDataStore.getShearWindCompSfcTo12km().getWspd();
        if (NsharpLibBasics.qc(shearWind)) {
            textStr = NsharpNativeConstants.SHEAR_SFC_12KM_LINE;
            textStr = String.format(textStr, shearWind,
                    NsharpLibBasics.kt_to_mps(shearWind),
                    NsharpLibBasics.kt_to_mps(shearWind) / .3F);
        } else {
            textStr = NsharpNativeConstants.SHEAR_SFC_12KM_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        // BRN Shear
        Parcel parcel = weatherDataStore.getParcelMap().get(currentParcel);
        if (parcel != null && NsharpLibBasics.qc(parcel.getBrnShear())) {
            textStr = NsharpNativeConstants.STORM_TYPE_BRNSHEAR_LINE;
            textStr = String.format(textStr, parcel.getBrnShear(),
                    NsharpConstants.SQUARE_SYMBOL,
                    NsharpConstants.SQUARE_SYMBOL);
        } else
            textStr = NsharpNativeConstants.STORM_TYPE_BRNSHEAR_MISSING;
        finalTextStr = finalTextStr + textStr;

        return finalTextStr;
    }

    private String printThermodynamicParametersBox1(
            NsharpResourceHandler rscHandler) {
        // this function is coded based on native nsharp
        // show_parcel() in xwvid3.c
        // This function is called to construct text string for printing
        NsharpWeatherDataStore weatherDataStore = rscHandler
                .getWeatherDataStore();

        String finalTextStr = "";

        int currentParcel = rscHandler.getCurrentParcel();
        Parcel pcl = weatherDataStore.getParcelMap().get(currentParcel);
        if (pcl == null) {
            return finalTextStr;
        }
        String hdrStr = NsharpNativeConstants.parcelToHdrStrMap
                .get(currentParcel);

        finalTextStr = finalTextStr + hdrStr;

        String textStr = NsharpNativeConstants.PARCEL_LPL_LINE;
        textStr = String.format(textStr, (int) pcl.getLplpres(),
                (int) pcl.getLpltemp(), (int) pcl.getLpldwpt(),
                (int) NsharpLibBasics.ctof(pcl.getLpltemp()),
                (int) NsharpLibBasics.ctof(pcl.getLpldwpt()));

        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(pcl.getBplus())) {
            textStr = NsharpNativeConstants.PARCEL_CAPE_LINE;
            textStr = String.format(textStr, pcl.getBplus());
        } else {
            textStr = NsharpNativeConstants.PARCEL_CAPE_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(pcl.getLi5())) {
            textStr = NsharpNativeConstants.PARCEL_LI_LINE;
            textStr = String.format(textStr, pcl.getLi5());
        } else {
            textStr = NsharpNativeConstants.PARCEL_LI_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(pcl.getBfzl())) {
            textStr = NsharpNativeConstants.PARCEL_BFZL_LINE;
            textStr = String.format(textStr, pcl.getBfzl());
        } else {
            textStr = NsharpNativeConstants.PARCEL_BFZL_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(pcl.getLimax())) {
            textStr = NsharpNativeConstants.PARCEL_LIMIN_LINE;
            textStr = String
                    .format(textStr, pcl.getLimax(), pcl.getLimaxpres());
        } else {
            textStr = NsharpNativeConstants.PARCEL_LIMIN_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(pcl.getBminus())) {
            textStr = NsharpNativeConstants.PARCEL_CINH_LINE;
            textStr = String.format(textStr, pcl.getBminus());
        } else {
            textStr = NsharpNativeConstants.PARCEL_CINH_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(pcl.getCap())) {
            textStr = NsharpNativeConstants.PARCEL_CAP_LINE;
            textStr = String.format(textStr, pcl.getCap(), pcl.getCappres());
        } else {
            textStr = NsharpNativeConstants.PARCEL_CAP_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        textStr = NsharpNativeConstants.PARCEL_LEVEL_LINE;
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(pcl.getLclpres())
                && NsharpLibBasics.qc(pcl.getLclAgl())) {
            textStr = NsharpNativeConstants.PARCEL_LCL_LINE;
            textStr = String.format(textStr, pcl.getLclpres(), pcl.getLclAgl());
        } else {
            textStr = NsharpNativeConstants.PARCEL_LCL_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(pcl.getLfcpres())
                && NsharpLibBasics.qc(pcl.getLfcAgl())
                && NsharpLibBasics.qc(pcl.getLfcTemp())) {
            textStr = NsharpNativeConstants.PARCEL_LFC_LINE;
            textStr = String.format(textStr, pcl.getLfcpres(), pcl.getLfcAgl(),
                    pcl.getLfcTemp());
        } else {
            textStr = NsharpNativeConstants.PARCEL_LFC_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(pcl.getElpres())
                && NsharpLibBasics.qc(pcl.getElAgl())
                && NsharpLibBasics.qc(pcl.getElTemp())) {
            textStr = NsharpNativeConstants.PARCEL_EL_LINE;
            textStr = String.format(textStr, pcl.getElpres(), pcl.getElAgl(),
                    pcl.getElTemp());
        } else {
            textStr = NsharpNativeConstants.PARCEL_EL_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(pcl.getMplpres())
                && NsharpLibBasics.qc(pcl.getMplAgl())) {
            textStr = NsharpNativeConstants.PARCEL_MPL_LINE;
            textStr = String.format(textStr, pcl.getMplpres(), pcl.getMplAgl());
        } else {
            textStr = NsharpNativeConstants.PARCEL_MPL_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        return finalTextStr;
    }

    public String printThermodynamicParametersBox2(
            NsharpResourceHandler rscHandler) {
        /*
         * this function is coded based on legacy native nsharp software
         * show_thermoparms(), show_moisture(),show_instability() in xwvid3.c
         * This function is called to construct text string for printing
         */
        NsharpWeatherDataStore weatherDataStore = rscHandler
                .getWeatherDataStore();

        String finalTextStr = "";

        String textStr;
        float pw = weatherDataStore.getPw();
        if (pw >= 0) {
            textStr = NsharpNativeConstants.THERMO_PWATER_LINE;
            textStr = String.format(textStr, pw);
        } else {
            textStr = NsharpNativeConstants.THERMO_PWATER_MISSING;
        }
        finalTextStr = finalTextStr + textStr;
        if (NsharpLibBasics.qc(weatherDataStore.getMeanRh())) {
            textStr = NsharpNativeConstants.THERMO_MEANRH_LINE;
            textStr = String.format(textStr, weatherDataStore.getMeanRh(),
                    NsharpConstants.PERCENT_SYMBOL);
        } else {
            textStr = NsharpNativeConstants.THERMO_MEANRH_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(weatherDataStore.getMeanMixRatio())) {
            textStr = NsharpNativeConstants.THERMO_MEANW_LINE;
            textStr = String
                    .format(textStr, weatherDataStore.getMeanMixRatio());
        } else {
            textStr = NsharpNativeConstants.THERMO_MEANW_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(weatherDataStore.getLowRh())) {
            textStr = NsharpNativeConstants.THERMO_MEANLRH_LINE;
            textStr = String.format(textStr, weatherDataStore.getLowRh(),
                    NsharpConstants.PERCENT_SYMBOL);
        } else {
            textStr = NsharpNativeConstants.THERMO_MEANLRH_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(weatherDataStore.getTopMoistLyrPress())) {
            textStr = NsharpNativeConstants.THERMO_TOP_LINE;
            textStr = String.format(textStr,
                    weatherDataStore.getTopMoistLyrPress(),
                    weatherDataStore.getTopMoistLyrHeight());
        } else {
            textStr = NsharpNativeConstants.THERMO_TOP_MISSING;
        }
        finalTextStr = finalTextStr + textStr + "BOXLINE";

        if (NsharpLibBasics.qc(weatherDataStore
                .getSevenHundredTo500mbTempDelta())
                && NsharpLibBasics.qc(weatherDataStore
                        .getSevenHundredTo500mbLapseRate())) {
            textStr = NsharpNativeConstants.THERMO_700500mb_LINE;
            textStr = String.format(textStr,
                    weatherDataStore.getSevenHundredTo500mbTempDelta(),
                    weatherDataStore.getSevenHundredTo500mbLapseRate());
        } else {
            textStr = NsharpNativeConstants.THERMO_700500mb_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(weatherDataStore.getEight50To500mbLapseRate())
                && NsharpLibBasics.qc(weatherDataStore
                        .getEight50To500mbTempDelta())) {
            textStr = NsharpNativeConstants.THERMO_850500mb_LINE;
            textStr = String.format(textStr,
                    weatherDataStore.getEight50To500mbTempDelta(),
                    weatherDataStore.getEight50To500mbLapseRate());
        } else {
            textStr = NsharpNativeConstants.THERMO_850500mb_MISSING;
        }
        finalTextStr = finalTextStr + textStr;
        finalTextStr = finalTextStr + "BOXLINE";

        // misc parameters data--------------//
        if (NsharpLibBasics.qc(weatherDataStore.getTotTots())) {
            textStr = NsharpNativeConstants.THERMO_TOTAL_LINE;
            textStr = String.format(textStr, weatherDataStore.getTotTots());
        } else {
            textStr = NsharpNativeConstants.THERMO_TOTAL_MISSING;
        }
        finalTextStr = finalTextStr + textStr;
        if (NsharpLibBasics.qc(weatherDataStore.getkIndex())) {
            textStr = NsharpNativeConstants.THERMO_KINDEX_LINE;
            textStr = String.format(textStr, weatherDataStore.getkIndex());
        } else {
            textStr = NsharpNativeConstants.THERMO_KINDEX_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(weatherDataStore.getSweatIndex())) {
            textStr = NsharpNativeConstants.THERMO_SWEAT_LINE;
            textStr = String.format(textStr, weatherDataStore.getSweatIndex());
        } else {
            textStr = NsharpNativeConstants.THERMO_SWEAT_MISSING;
        }
        finalTextStr = finalTextStr + textStr;
        if (NsharpLibBasics.qc(weatherDataStore.getMaxTemp())) {
            textStr = NsharpNativeConstants.THERMO_MAXT_LINE;
            textStr = String.format(textStr, weatherDataStore.getMaxTemp());
        } else {
            textStr = NsharpNativeConstants.THERMO_MAXT_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(weatherDataStore.getThetaDiff())) {
            textStr = NsharpNativeConstants.THERMO_THETAE_LINE;
            textStr = String.format(textStr, weatherDataStore.getThetaDiff());
        } else {
            textStr = NsharpNativeConstants.THERMO_THETAE_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(weatherDataStore.getConvT())) {
            textStr = NsharpNativeConstants.THERMO_CONVT_LINE;
            textStr = String.format(textStr, weatherDataStore.getConvT());
        } else {
            textStr = NsharpNativeConstants.THERMO_CONVT_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(weatherDataStore.getWbzft())) {
            textStr = NsharpNativeConstants.THERMO_WBZ_LINE;
            textStr = String.format(textStr, weatherDataStore.getWbzft());
        } else {
            textStr = NsharpNativeConstants.THERMO_WBZ_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        if (NsharpLibBasics.qc(weatherDataStore.getFgzft())) {
            textStr = NsharpNativeConstants.THERMO_FGZ_LINE;
            textStr = String.format(textStr, weatherDataStore.getFgzft());
        } else {
            textStr = NsharpNativeConstants.THERMO_FGZ_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        return finalTextStr;

    }

    public String printStormStructureParametersBox(
            NsharpResourceHandler rscHandler) {
        NsharpWeatherDataStore weatherDataStore = rscHandler
                .getWeatherDataStore();
        String textStr, finalTextStr = "";
        // helicity for sfc-3 km
        Helicity helicity = weatherDataStore.getStormTypeToHelicityMap().get(
                "SFC-3km");
        if (NsharpLibBasics.qc(helicity.getPosHelicity())
                && NsharpLibBasics.qc(helicity.getNegHelicity())) {
            textStr = "Sfc - 3km SREH =\t\t%.0f m%c/s%c\r\n";
            textStr = String.format(textStr, helicity.getTotalHelicity(),
                    NsharpConstants.SQUARE_SYMBOL,
                    NsharpConstants.SQUARE_SYMBOL);
        } else {
            textStr = "Sfc - 3km SREH =\t\tM";
        }
        finalTextStr = finalTextStr + textStr;
        // EFF. SREH
        helicity = weatherDataStore.getStormTypeToHelicityMap().get(
                "Eff Inflow");

        if (NsharpLibBasics.qc(helicity.getPosHelicity())
                && NsharpLibBasics.qc(helicity.getNegHelicity())) {
            textStr = NsharpNativeConstants.STORM_TYPE_EFF_LINE;
            textStr = String.format(textStr, helicity.getTotalHelicity(),
                    NsharpConstants.SQUARE_SYMBOL,
                    NsharpConstants.SQUARE_SYMBOL);
        } else {
            textStr = NsharpNativeConstants.STORM_TYPE_EFF_MISSING;
        }

        finalTextStr = finalTextStr + textStr;
        // EHI
        int currentParcel = rscHandler.getCurrentParcel();
        Parcel pcl = weatherDataStore.getParcelMap().get(currentParcel);
        if (pcl != null && NsharpLibBasics.qc(pcl.getBplus())) {
            float ehi = pcl.getBplus() * helicity.getTotalHelicity() / 160000;
            if (NsharpLibBasics.qc(ehi)) {
                textStr = NsharpNativeConstants.STORM_TYPE_EHI_LINE;
                textStr = String.format(textStr, ehi);
            } else {
                textStr = NsharpNativeConstants.STORM_TYPE_EHI_MISSING;
            }
        } else {
            textStr = NsharpNativeConstants.STORM_TYPE_EHI_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        // BRN
        if (NsharpLibBasics.qc(pcl.getBrn())) {
            textStr = NsharpNativeConstants.STORM_TYPE_BRN_LINE;
            textStr = String.format(textStr, pcl.getBrn());
        } else {
            textStr = NsharpNativeConstants.STORM_TYPE_BRN_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        // Strom wind
        finalTextStr = finalTextStr + NsharpNativeConstants.STORM_WIND_STR;
        finalTextStr = finalTextStr
                + NsharpNativeConstants.STORM_LAYER_VECTOR_STR;
        // pressure-weighted SR mean wind at sfc-2 km
        WindComponent srWind = weatherDataStore.getStormTypeToSrMeanWindMap()
                .get("SFC-2km");
        if (NsharpLibBasics.qc(srWind.getWdir())) {
            textStr = NsharpNativeConstants.STORM_SFC2KM_VECT_LINE;
            textStr = String.format(textStr, srWind.getWdir(),
                    srWind.getWspd(),
                    NsharpLibBasics.kt_to_mps(srWind.getWspd()));
        } else {
            textStr = NsharpNativeConstants.STORM_SFC2KM_VECT_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        // pressure-weighted SR mean wind at 4-6 km
        srWind = weatherDataStore.getSrMeanWindComp4To6km();
        if (NsharpLibBasics.qc(srWind.getWdir())) {
            textStr = NsharpNativeConstants.STORM_4_6KM_VECT_LINE;
            textStr = String.format(textStr, srWind.getWdir(),
                    srWind.getWspd(),
                    NsharpLibBasics.kt_to_mps(srWind.getWspd()));
        } else {
            textStr = NsharpNativeConstants.STORM_4_6KM_VECT_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        // pressure-weighted SR mean wind at 9-11 km
        srWind = weatherDataStore.getSrMeanWindComp9To11km();
        if (NsharpLibBasics.qc(srWind.getWdir())) {
            textStr = NsharpNativeConstants.STORM_9_11KM_VECT_LINE;
            textStr = String.format(textStr, srWind.getWdir(),
                    srWind.getWspd(),
                    NsharpLibBasics.kt_to_mps(srWind.getWspd()));
        } else {
            textStr = NsharpNativeConstants.STORM_9_11KM_VECT_MISSING;
        }
        finalTextStr = finalTextStr + textStr;

        return finalTextStr;
    }
}
