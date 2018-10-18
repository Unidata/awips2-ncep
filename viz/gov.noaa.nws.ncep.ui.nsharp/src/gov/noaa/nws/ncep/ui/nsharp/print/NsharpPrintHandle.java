package gov.noaa.nws.ncep.ui.nsharp.print;

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
 * Date         Ticket#     Engineer    Description
 * -------      -------     --------    -----------
 * 03/23/2010   229         Chin Chen   Initial coding
 * 07/05/2016   RM#15923    Chin Chen   NSHARP - Native Code replacement
 * 10/16/2018   6845        bsteffen    Extract logic for printing graphs and
 *                                      text boxes into new classes.
 * 
 * </pre>
 * 
 * @author Chin Chen
 */

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.Shell;

import gov.noaa.nws.ncep.ui.nsharp.display.NsharpEditor;
import gov.noaa.nws.ncep.ui.nsharp.display.rsc.NsharpResourceHandler;

public class NsharpPrintHandle {

    private NsharpResourceHandler handler;

    private Printer printer;

    private GC gc;

    private Font font;

    public void handlePrint(Shell shell) {
        if (getHandler() == null) {
            return;
        }

        PrintDialog dialog = new PrintDialog(shell, SWT.NULL);
        PrinterData data = dialog.open();

        if (data == null)
            return;
        if (data.printToFile) {
            String fileName = data.fileName;
            int i = fileName.indexOf("///");
            if (i != -1)
                data.fileName = fileName.substring(0, i)
                        + fileName.substring(i + 2);

        }
        createPrinter(data);
        if (startJob()) {
            printPage();
            endJob();
        }
        disposePrinter();
    }

    private NsharpResourceHandler getHandler() {
        if (handler == null) {
            NsharpEditor editor = NsharpEditor.getActiveNsharpEditor();
            if (editor != null) {
                handler = editor.getRscHandler();
            }
        }
        return handler;
    }

    public void createPrinter(PrinterData data) {
        this.printer = new Printer(data);
    }

    public void disposePrinter() {
        printer.dispose();
        printer = null;
    }

    public boolean startJob() {
        if (printer.startJob("NSHARP")) {

            /*
             * Create printer GC, and create and set the printer font &
             * foreground color.
             */
            gc = new GC(printer);
            font = new Font(printer, "Courier", 5, SWT.NORMAL);
            gc.setFont(font);

            gc.setForeground(printer.getSystemColor(SWT.COLOR_BLACK));
            gc.setBackground(printer.getSystemColor(SWT.COLOR_WHITE));
            return true;
        }
        return false;
    }

    public void printPage() {
        if(getHandler() == null){
            return;
        }
        printer.startPage();

        Rectangle pageBounds = getPageBounds();

        int lineHeight = gc.getFontMetrics().getHeight();

        int x = pageBounds.x;
        int y = pageBounds.y;

        String stationInfo = handler.getPickedStnInfoStr();
        if (stationInfo != null) {
            gc.drawString(stationInfo, x, y);
            y += lineHeight;
        }

        int width = pageBounds.width;
        int height = pageBounds.height * 5 / 8;

        Rectangle graphBounds = new Rectangle(x, y, width, height);
        NsharpGraphPrinter graphPrinter = new NsharpGraphPrinter(gc,
                graphBounds, handler);
        graphPrinter.print();

        /* extra lineHeight is whitespace padding */
        y += graphBounds.height + 2 * lineHeight;
        height = pageBounds.height - (y - pageBounds.y) - lineHeight * 2;
        Rectangle textBounds = new Rectangle(x, y, width, height);
        NSharpTextPrinter textPrinter = new NSharpTextPrinter(gc, textBounds,
                handler);
        textPrinter.print();

        y = pageBounds.y + pageBounds.height - lineHeight * 2;
        gc.drawString("Output produced by: NCO-SIB AWIPS2 NSHARP", x, y);
        y += lineHeight;
        gc.drawString("National SkewT-Hodograph Analysis and Research Program",
                x, y);
        printer.endPage();
    }

    private Rectangle getPageBounds() {
        Rectangle trim = printer.computeTrim(0, 0, 0, 0);
        Rectangle bounds = printer.getBounds();
        Point dpi = printer.getDPI();

        /* 1 inch margins. */
        int left = bounds.x + dpi.x + trim.x;
        int width = bounds.width - dpi.x * 2;
        int top = bounds.y + dpi.y + trim.y;
        int height = bounds.height - dpi.y * 2;
        return new Rectangle(left, top, width, height);
    }

    public void endJob() {
        printer.endJob();
        font.dispose();
        gc.dispose();
    }

}
