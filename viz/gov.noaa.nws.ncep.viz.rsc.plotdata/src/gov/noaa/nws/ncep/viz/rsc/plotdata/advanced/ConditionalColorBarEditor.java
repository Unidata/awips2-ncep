package gov.noaa.nws.ncep.viz.rsc.plotdata.advanced;

import java.util.ArrayList;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Pattern;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Spinner;

import gov.noaa.nws.ncep.viz.common.ui.color.ColorButtonSelector;

/**
 * This widget is used to edit Conditional ColorBars for Plot Resources.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer   Description
 * ------------- -------- ---------- -------------------------------------------
 * Jul 24, 2012  431      S. Gurung  Initial Creation.
 * Nov 16, 2016  21762    P. Moyer   Removed -Inf/Inf control buttons.
 *                                   Implemented Alpha transparency.
 * Mar 24, 2020  8049     randerso   Remove READ_ONLY from Spinner controls.
 *                                   Code cleanup.
 *
 * </pre>
 *
 * @author sgurung
 */
/*
 * TODO: This class seems to be largely a copy of ColorBarEditor. Consider
 * refactoring into a single class or class hierarchy
 */
public class ConditionalColorBarEditor extends Composite {
    private static final int CBAR_XOFF = 15;

    private ArrayList<Rectangle> colorBarRects = null;

    private int seldIntrvl = 0;

    private int dragXvalue = 0;

    private final Canvas canvas;

    private Font font;

    private Color canvasColor;

    private Color seldColor;

    private Color labelColor;

    private ConditionalColorBar colorBar;

    private Point canvasSize = null;

    private final Spinner numDecimalsSpnr;

    private final Button showLabelsBtn;

    private final Spinner intrvlMinSpnr;

    private final Spinner intrvlMaxSpnr;

    private Composite colorComp;

    private Composite labelColorComp;

    private final ColorButtonSelector intrvlColorSelector;

    // also used for the border color
    private final ColorButtonSelector labelColorSelector;

    private Cursor pointerCursor;

    private Cursor dragIntrvlCursor;

    private Display colorDevice;

    private double scaleMult = 1.0;

    // TODO: replace with proper MouseListener
    private final Listener mouseLstnr = new Listener() {

        @Override
        public void handleEvent(Event e) {
            canvas.redraw();
            switch (e.type) {
            case SWT.MouseDoubleClick:
                // doesn't work?
                System.out.println("Double Click");
                break;
            case SWT.MouseDown:
                if (e.button != 1) {
                    return;
                }

                for (Rectangle rect : colorBarRects) {
                    int intrvlIndx = colorBarRects.indexOf(rect);

                    /*
                     * if non-image and if drawing to scale then we first check
                     * to see if clicking on the edge of an interval (give them
                     * a little room for error) (can't drag the last interval)
                     */ if (colorBar.getDrawToScale()) {

                        if (intrvlIndx != colorBarRects.size() - 1
                                && Math.abs(e.x - rect.x - rect.width) <= 1) {

                            dragXvalue = rect.x + rect.width;
                            seldIntrvl = colorBarRects.indexOf(rect);
                            break;
                        }
                    }

                    if (rect.contains(e.x, e.y)) {
                        seldIntrvl = colorBarRects.indexOf(rect);
                        updateSelectedInterval();
                    }
                }

                canvas.redraw();

                break;

            case SWT.MouseMove:
                /*
                 * if not dragging, check if we on an interval and change the
                 * cursor
                 */
                if (dragXvalue == 0) {
                    boolean crossingInterval = false;

                    /*
                     * it only makes sense for the user to be able to drag an
                     * interval if the colorbar is drawn to scale.
                     */
                    if (colorBar.getDrawToScale()) {
                        /*
                         * check to see if crossing the edge of an interval (but
                         * not the last interval)
                         */
                        for (int i = 0; i < colorBarRects.size() - 1; i++) {
                            Rectangle rect = colorBarRects.get(i);
                            if (e.y >= rect.y - 2
                                    && e.y < rect.y + rect.height + 2) {
                                if (Math.abs(e.x - rect.x - rect.width) <= 1) {
                                    crossingInterval = true;
                                    break;
                                }
                            }
                        }
                    }

                    canvas.setCursor((crossingInterval ? dragIntrvlCursor
                            : pointerCursor));
                } else {
                    dragXvalue = e.x;

                    /*
                     * if the user drags the max value past the min value or
                     * past an max value for the next interval then we will
                     * remove it and stop dragging.
                     *
                     */
                    if (e.x <= colorBarRects.get(seldIntrvl).x) {
                        colorBar.removeInterval(seldIntrvl);
                        dragXvalue = 0;
                    } else {
                        // note that we can't drag the last interval
                        Rectangle nextRect = colorBarRects.get(seldIntrvl + 1);

                        if (e.x >= nextRect.x + nextRect.width) {
                            Float newMax = colorBar
                                    .getIntervalMax(seldIntrvl + 1);
                            colorBar.removeInterval(seldIntrvl + 1);
                            colorBar.setIntervalMax(seldIntrvl, newMax);
                            dragXvalue = 0;
                        }
                    }

                    computeColorBarRectangles();
                    updateSelectedInterval();
                }

                break;

            case SWT.MouseUp:
                if (e.button != 1) {
                    return;
                }
                /*
                 * if dragging, then interpolate the new max and update the
                 * interval
                 */
                if (dragXvalue != 0) {
                    Float newMax = interpolateNewIntervalMax();
                    if (newMax != Float.NaN) {
                        colorBar.setIntervalMax(seldIntrvl, newMax);
                    }

                    computeColorBarRectangles();
                    updateSelectedInterval();

                    dragXvalue = 0;
                }

                canvas.redraw();
                break;
            }
        }
    };

    public ConditionalColorBarEditor(Composite parent,
            ConditionalColorBar cbar) {
        super(parent, SWT.NONE);

        colorBar = cbar;
        colorDevice = parent.getDisplay();

        initialize();

        Composite topForm = this;
        FormData fd = new FormData(600, 260);
        fd.top = new FormAttachment(0, 0);
        fd.left = new FormAttachment(0, 0);
        fd.right = new FormAttachment(100, 0);
        fd.bottom = new FormAttachment(100, 0);
        topForm.setLayoutData(fd);

        topForm.setLayout(new FormLayout());

        numDecimalsSpnr = new Spinner(topForm, SWT.BORDER);
        numDecimalsSpnr.setToolTipText("Number of decimals");
        fd = new FormData();
        fd.left = new FormAttachment(10, 100);
        fd.top = new FormAttachment(0, 15);
        numDecimalsSpnr.setLayoutData(fd);

        Label numDecimalsLbl = new Label(topForm, SWT.NONE);
        numDecimalsLbl.setText("Number of Decimals");
        fd = new FormData();
        fd.right = new FormAttachment(numDecimalsSpnr, -10, SWT.LEFT);
        fd.top = new FormAttachment(numDecimalsSpnr, 3, SWT.TOP);
        numDecimalsLbl.setLayoutData(fd);

        Label lblColLbl = new Label(topForm, SWT.NONE);
        lblColLbl.setText("Label Color");
        fd = new FormData();
        fd.top = new FormAttachment(numDecimalsSpnr, 4, SWT.TOP);
        fd.left = new FormAttachment(numDecimalsSpnr, 35, SWT.RIGHT);
        lblColLbl.setLayoutData(fd);

        labelColorComp = new Composite(topForm, SWT.None);
        fd = new FormData();
        fd.top = new FormAttachment(lblColLbl, -2, SWT.TOP);
        fd.left = new FormAttachment(lblColLbl, 20, SWT.RIGHT);
        labelColorComp.setLayoutData(fd);

        GridLayout gl = new GridLayout();
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        labelColorComp.setLayout(gl);

        labelColorSelector = new ColorButtonSelector(labelColorComp, 50, 25);

        showLabelsBtn = new Button(topForm, SWT.CHECK);
        showLabelsBtn.setText("Show Labels");
        fd = new FormData();
        fd.left = new FormAttachment(labelColorComp, 35, SWT.RIGHT);
        fd.top = new FormAttachment(labelColorComp, 0, SWT.TOP);
        showLabelsBtn.setLayoutData(fd);

        canvas = new Canvas(topForm, SWT.BORDER);

        fd = new FormData();
        fd.height = 100;
        fd.top = new FormAttachment(numDecimalsSpnr, 25, SWT.BOTTOM);
        fd.left = new FormAttachment(0, 20);
        fd.right = new FormAttachment(100, -20);

        canvas.setLayoutData(fd);

        colorComp = new Composite(topForm, SWT.None);
        fd = new FormData();
        fd.top = new FormAttachment(canvas, 25, SWT.BOTTOM);
        fd.left = new FormAttachment(50, -30);
        fd.bottom = new FormAttachment(100, -20);
        colorComp.setLayoutData(fd);
        colorComp.setLayout(gl);

        intrvlColorSelector = new ColorButtonSelector(colorComp, 50, 25, true);

        intrvlMinSpnr = new Spinner(topForm, SWT.BORDER);
        intrvlMaxSpnr = new Spinner(topForm, SWT.BORDER);

        createNonImageCbarWidgets();

        initWidgets();

        computeColorBarRectangles();

        updateSelectedInterval();
    }

    private void createNonImageCbarWidgets() {
        Composite topForm = this;
        FormData fd;

        fd = new FormData();
        fd.width = 40;
        fd.top = new FormAttachment(colorComp, 0, SWT.TOP);
        fd.right = new FormAttachment(colorComp, -25, SWT.LEFT);
        intrvlMinSpnr.setLayoutData(fd);

        fd = new FormData();
        fd.top = new FormAttachment(intrvlMinSpnr, 0, SWT.TOP);
        fd.left = new FormAttachment(intrvlMinSpnr, 0, SWT.LEFT);
        fd.right = new FormAttachment(intrvlMinSpnr, 0, SWT.RIGHT);
        fd.bottom = new FormAttachment(intrvlMinSpnr, 0, SWT.BOTTOM);

        Label intrvlMinLbl = new Label(topForm, SWT.NONE);
        intrvlMinLbl.setText("Minimum");
        fd = new FormData();
        fd.left = new FormAttachment(intrvlMinSpnr, 0, SWT.LEFT);
        fd.bottom = new FormAttachment(intrvlMinSpnr, -2, SWT.TOP);
        intrvlMinLbl.setLayoutData(fd);

        fd = new FormData();
        fd.width = 40;
        fd.top = new FormAttachment(colorComp, 0, SWT.TOP);
        fd.left = new FormAttachment(colorComp, 20, SWT.RIGHT);
        intrvlMaxSpnr.setLayoutData(fd);

        fd = new FormData();
        fd.top = new FormAttachment(intrvlMaxSpnr, 0, SWT.TOP);
        fd.left = new FormAttachment(intrvlMaxSpnr, 0, SWT.LEFT);
        fd.right = new FormAttachment(intrvlMaxSpnr, 0, SWT.RIGHT);
        fd.bottom = new FormAttachment(intrvlMaxSpnr, 0, SWT.BOTTOM);

        Label intrvlMaxLbl = new Label(topForm, SWT.NONE);
        intrvlMaxLbl.setText("Maximum");
        fd = new FormData();
        fd.left = new FormAttachment(intrvlMaxSpnr, 0, SWT.LEFT);
        fd.bottom = new FormAttachment(intrvlMaxSpnr, -2, SWT.TOP);
        intrvlMaxLbl.setLayoutData(fd);

        Button addIntrvlBtn = new Button(topForm, SWT.PUSH);
        fd = new FormData();
        addIntrvlBtn.setText("  Insert  ");
        fd.top = new FormAttachment(colorComp, 0, SWT.TOP);
        fd.right = new FormAttachment(intrvlMinSpnr, -20, SWT.LEFT);
        addIntrvlBtn.setLayoutData(fd);

        Button removeIntrvlBtn = new Button(topForm, SWT.PUSH);
        fd = new FormData();
        removeIntrvlBtn.setText(" Remove ");
        fd.top = new FormAttachment(colorComp, 0, SWT.TOP);
        fd.left = new FormAttachment(intrvlMaxSpnr, 20, SWT.RIGHT);
        removeIntrvlBtn.setLayoutData(fd);

        removeIntrvlBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (colorBar.getNumIntervals() == 1) {
                    // popup a msg dialog
                    System.out.println("Can't remove last interval");
                    return;
                }

                colorBar.removeInterval(seldIntrvl);

                if (seldIntrvl >= colorBar.getNumIntervals()) {
                    seldIntrvl = colorBar.getNumIntervals() - 1;
                }

                computeColorBarRectangles();
                updateSelectedInterval();
            }
        });

        addIntrvlBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                colorBar.createNewInterval(seldIntrvl);
                computeColorBarRectangles();
                updateSelectedInterval();
            }
        });

        intrvlMinSpnr.addModifyListener(minSpnrListener);

        intrvlMaxSpnr.addModifyListener(maxSpnrListener);

        removeModifyListenersForSpinners();
        intrvlMinSpnr.setDigits(colorBar.getNumDecimals());
        intrvlMinSpnr.setIncrement(1);

        intrvlMaxSpnr.setDigits(colorBar.getNumDecimals());
        intrvlMaxSpnr.setIncrement(1);

        addModifyListenersForSpinners();

    }

    private void initialize() {

        int numDecimals = colorBar.getNumDecimals();
        scaleMult = Math.pow(10, numDecimals);

        colorBar.setColorDevice(colorDevice);

        pointerCursor = new Cursor(colorDevice, SWT.CURSOR_ARROW);
        dragIntrvlCursor = new Cursor(colorDevice, SWT.CURSOR_SIZEWE);

        labelColor = new Color(colorDevice, colorBar.getLabelColor());
        canvasColor = new Color(colorDevice, 0, 0, 0);
        // white
        seldColor = new Color(colorDevice, 255, 255, 255);

        font = new Font(colorDevice, "Times", 10, SWT.BOLD);
    }

    /**
     * create listeners and init values and selections
     */
    private void initWidgets() {
        seldIntrvl = 0;

        canvasSize = canvas.getSize();

        numDecimalsSpnr.setSelection(colorBar.getNumDecimals());

        numDecimalsSpnr.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                colorBar.setNumDecimals(numDecimalsSpnr.getSelection());

                int numDecimals = colorBar.getNumDecimals();
                scaleMult = Math.pow(10, numDecimals);

                removeModifyListenersForSpinners();

                intrvlMinSpnr.setDigits(colorBar.getNumDecimals());
                intrvlMinSpnr.setIncrement(1);

                intrvlMaxSpnr.setDigits(colorBar.getNumDecimals());
                intrvlMaxSpnr.setIncrement(1);

                addModifyListenersForSpinners();

                computeColorBarRectangles();
                updateSelectedInterval();
            }
        });

        numDecimalsSpnr.setMinimum(0);
        numDecimalsSpnr.setMaximum(2);
        numDecimalsSpnr.setIncrement(1);

        showLabelsBtn.setSelection(colorBar.getShowLabels());

        showLabelsBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                colorBar.setShowLabels(showLabelsBtn.getSelection());
                computeColorBarRectangles();
            }
        });

        canvas.setFont(font);
        canvas.setBackground(canvasColor);

        canvas.addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent e) {
                drawColorBar((Canvas) e.getSource(), e.gc);
            }
        });

        canvas.addControlListener(new ControlListener() {
            @Override
            public void controlMoved(ControlEvent e) {
                canvasSize = canvas.getSize();
                computeColorBarRectangles();
            }

            @Override
            public void controlResized(ControlEvent e) {
                canvasSize = canvas.getSize();
                computeColorBarRectangles();
            }

        });

        // TODO: user proper addMouse*Listener() calls and appropriate listeners
        canvas.addListener(SWT.MouseDown, mouseLstnr);
        canvas.addListener(SWT.MouseMove, mouseLstnr);
        canvas.addListener(SWT.MouseUp, mouseLstnr);

        labelColorSelector.setColorValue(colorBar.getLabelColor());

        intrvlColorSelector.addListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                colorBar.setRGB(seldIntrvl,
                        intrvlColorSelector.getColorValue());
                colorBar.setAlpha(seldIntrvl,
                        intrvlColorSelector.getAlphaValue());
                canvas.redraw();
            }
        });

        labelColorSelector.addListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                colorBar.setLabelColor(labelColorSelector.getColorValue());
                if (labelColor != null) {
                    labelColor.dispose();
                }
                labelColor = new Color(colorDevice,
                        labelColorSelector.getColorValue());

                canvas.redraw();
            }
        });

    }

    /**
     * update the values used to draw the colorBar
     */
    private void computeColorBarRectangles() {

        /*
         * if an interval has been removed or added then just start over with a
         * new array
         */
        if (colorBarRects == null
                || colorBarRects.size() != colorBar.getNumIntervals()) {
            colorBarRects = new ArrayList<>();

            for (int c = 0; c < colorBar.getNumIntervals(); c++) {
                colorBarRects.add(new Rectangle(0, 0, 1, 1));
            }
        }

        int barIntX = CBAR_XOFF;
        int cbarPixWidth = canvasSize.x - 2 * CBAR_XOFF;

        // center in the middle of the canvas
        // 4 for the text height approx.
        int barIntY = canvasSize.y / 2 - colorBar.getWidthInPixels() / 2 + 4;

        // this will be constant if not drawing to scale
        int barIntWidth = 1;

        if (!colorBarRects.isEmpty()) {
            barIntWidth = (canvasSize.x - 2 * CBAR_XOFF) / colorBarRects.size();
        }

        if (colorBar.getDrawToScale()) {
            int intCnt = colorBar.getNumIntervals();

            // the size in pixels of an interval to +- infinity
            int infIntSize = cbarPixWidth / intCnt;

            /*
             * a scale value by dividing the width in pixels by the range of
             * actual values (not including infinite intervals)
             */
            Float rangeMin = (colorBar
                    .getIntervalMin(0) == Float.NEGATIVE_INFINITY
                            ? colorBar.getIntervalMax(0)
                            : colorBar.getIntervalMin(0));
            Float rangeMax = (colorBar
                    .getIntervalMax(intCnt - 1) == Float.POSITIVE_INFINITY
                            ? colorBar.getIntervalMin(intCnt - 1)
                            : colorBar.getIntervalMax(intCnt - 1));

            if (colorBar.getIntervalMin(0) == Float.NEGATIVE_INFINITY) {
                cbarPixWidth -= infIntSize;
            }
            if (colorBar
                    .getIntervalMax(intCnt - 1) == Float.POSITIVE_INFINITY) {
                cbarPixWidth -= infIntSize;
            }

            float pixPerUnit = cbarPixWidth / (rangeMax - rangeMin);

            for (int b = 0; b < intCnt; b++) {
                Rectangle cbarRect = colorBarRects.get(b);

                cbarRect.x = barIntX;
                cbarRect.y = barIntY;
                cbarRect.height = colorBar.getWidthInPixels();

                /*
                 * determine the width of the interval in pixels. if inf then
                 * use the ave size of an interval
                 */
                if ((b == 0 && colorBar
                        .getIntervalMin(0) == Float.NEGATIVE_INFINITY)
                        || (b == intCnt - 1 && colorBar.getIntervalMax(
                                b) == Float.POSITIVE_INFINITY)) {
                    barIntWidth = infIntSize;
                } else {
                    barIntWidth = (int) (pixPerUnit
                            * (colorBar.getIntervalMax(b)
                                    - colorBar.getIntervalMin(b)));
                }
                barIntX += barIntWidth;
                cbarRect.width = barIntWidth;
            }
        } else {
            for (Rectangle cbarRect : colorBarRects) {
                cbarRect.x = barIntX;
                cbarRect.y = barIntY;
                cbarRect.height = colorBar.getWidthInPixels();

                barIntX += barIntWidth;
                cbarRect.width = barIntWidth;
            }
        }
        canvas.redraw();
    }

    private void drawColorBar(Canvas canvas, GC gc) {

        int textHeight = gc.getFontMetrics().getHeight();
        int charWidth = gc.getFontMetrics().getAverageCharWidth();

        gc.setLineWidth(1);

        // draw the rectangles in the given color
        for (int b = 0; b < colorBar.getNumIntervals(); b++) {

            if (colorBar.getColor(b).getAlpha() != 0) {
                gc.setBackground(colorBar.getColor(b));
                gc.setForeground(colorBar.getColor(b));
            }
            if (colorBar.getColor(b).getAlpha() == 0) {
                final Pattern pattern = assembleAlphaPattern(canvas);

                gc.setBackgroundPattern(pattern);
                gc.setForegroundPattern(pattern);
            }

            gc.drawRectangle(colorBarRects.get(b));
            gc.fillRectangle(colorBarRects.get(b));

        }

        Rectangle seldRect = colorBarRects.get(seldIntrvl);

        gc.setLineWidth(1);
        gc.setForeground(seldColor);

        /*
         * if dragging, draw the modified interval selection. otherwise
         * highlight the selected interval/pixel
         */
        if (dragXvalue != 0) {
            gc.drawLine(seldRect.x + seldRect.width, seldRect.y,
                    seldRect.x + seldRect.width, seldRect.y + seldRect.height);
            gc.drawLine(dragXvalue, seldRect.y - textHeight, dragXvalue,
                    seldRect.y + seldRect.height);
            gc.drawRectangle(seldRect.x, seldRect.y - 1,
                    dragXvalue - seldRect.x, seldRect.height + 2);

            String dragMaxLabel = getLabelString(interpolateNewIntervalMax());

            gc.drawText(dragMaxLabel,
                    dragXvalue - charWidth * dragMaxLabel.length() / 2,
                    seldRect.y - textHeight * 2, true);
        } else {
            gc.drawRectangle(seldRect.x, seldRect.y - 1, seldRect.width,
                    seldRect.height + 2);
        }

        /*
         * if showing the labels, draw them based on whether this for an image
         * or not
         */
        if (colorBar.getShowLabels()) {

            gc.setForeground(labelColor);

            int numIntrvs = colorBar.getNumIntervals();
            int textWidth = 0;
            int prevLabelExtent = 0;
            boolean labelRaised = false;

            for (int b = 0; b < numIntrvs; b++) {
                String labelStr = colorBar.getLabelString(b);
                textWidth = labelStr.length() * charWidth;
                int textX = colorBarRects.get(b).x - textWidth / 2;
                int textY = colorBarRects.get(b).y - textHeight;

                /*
                 * if this label will overwrite the previous one and it was not
                 * raised then raise it up
                 */
                labelRaised = (!labelRaised && textX < prevLabelExtent);

                if (labelRaised) {
                    textY -= textHeight / 2;
                    textY = colorBarRects.get(b).y
                            + colorBarRects.get(b).height;
                }

                gc.drawText(labelStr, textX, textY, true);

                prevLabelExtent = textX + textWidth;
            }

            String labelStr = colorBar.getLabelString(numIntrvs);
            gc.drawText(labelStr, colorBarRects.get(numIntrvs - 1).x
                    + colorBarRects.get(numIntrvs - 1).width - textWidth / 2,
                    colorBarRects.get(numIntrvs - 1).y - textHeight, true);
        }
    }

    /**
     * assembles the pattern used for representing No Color
     */
    private Pattern assembleAlphaPattern(Canvas canvas) {
        Display display = canvas.getDisplay();
        // define a pattern on an image
        final Image image = new Image(display, 20, 20);
        Color grey = display.getSystemColor(SWT.COLOR_GRAY);
        Color white = display.getSystemColor(SWT.COLOR_WHITE);
        GC gcp = new GC(image);
        gcp.setBackground(white);
        gcp.fillRectangle(0, 0, 20, 20);
        gcp.setBackground(grey);
        gcp.fillRectangle(0, 0, 10, 10);
        gcp.fillRectangle(10, 10, 10, 10);
        gcp.dispose();
        final Pattern pattern;
        pattern = new Pattern(display, image);
        return pattern;
    }

    // TODO : Add support for control over num of decimal places
    private String getLabelString(Float val) {
        if (val == Float.NEGATIVE_INFINITY) {
            return "-Inf";
        } else if (val == Float.POSITIVE_INFINITY) {
            return "Inf";
        } else {
            return Float.toString(val);
        }
    }

    /**
     * this is called when the user selects an interval and when the usr
     * modifies either by setting the min/max or by inserting or removing an
     * interval
     */
    private void updateSelectedInterval() {
        removeModifyListenersForSpinners();

        RGB seldRGB = colorBar.getRGB(seldIntrvl);
        Integer seldAlpha = colorBar.getAlpha(seldIntrvl);
        if (seldRGB != null) {
            intrvlColorSelector.setColorValue(seldRGB);
            intrvlColorSelector.setAlphaValue(seldAlpha);
        }

        int lastIntrvl = colorBar.getNumIntervals() - 1;

        /*
         * wait to set these values in the Spinners since we first have to
         * change the min/max ranges or the Spinner won's accept the new
         * selections.
         */
        Float minVal = colorBar.getIntervalMin(seldIntrvl) * (float) scaleMult;
        Float maxVal = colorBar.getIntervalMax(seldIntrvl) * (float) scaleMult;

        /*
         * if the min/max is +-Inf then this will not be visible since it will
         * be covered up by the Inf Text
         *
         * set the new min an max allowed values for the Spinners based on the
         * min and max interval values of the adjacent intervals. This will also
         * prevent the user from editing the text to be a value out of range for
         * the interval.
         */
        int minMinVal, maxMinVal;
        int minMaxVal, maxMaxVal;

        // Set the min for the min to the min of the prev interval
        if (seldIntrvl == 0 || (seldIntrvl == 1
                && colorBar.getIntervalMin(0) == Float.NEGATIVE_INFINITY)) {
            minMinVal = Integer.MIN_VALUE;
        } else {
            Float tVal = colorBar.getIntervalMin(seldIntrvl - 1).floatValue()
                    * (float) scaleMult;
            minMinVal = Math.round(tVal) + 1;
        }

        // set the max for the min to the max of this interval
        maxMinVal = Math.round(maxVal) - 1;

        // set the min for the max to the min for this interval
        minMaxVal = Math.round(minVal) + 1;

        // set the max for the max to the max of the next interval
        if (seldIntrvl == lastIntrvl
                || (seldIntrvl == lastIntrvl - 1 && colorBar.getIntervalMax(
                        lastIntrvl) == Float.POSITIVE_INFINITY)) {
            maxMaxVal = Integer.MAX_VALUE;

        } else {
            Float tVal = colorBar.getIntervalMax(seldIntrvl + 1).floatValue()
                    * (float) scaleMult;
            maxMaxVal = Math.round(tVal) - 1;
        }

        intrvlMinSpnr.setMinimum(minMinVal);
        intrvlMinSpnr.setMaximum(maxMinVal);

        intrvlMaxSpnr.setMinimum(minMaxVal);
        intrvlMaxSpnr.setMaximum(maxMaxVal);

        setIntervalMin(minVal);
        setIntervalMax(maxVal);

        addModifyListenersForSpinners();
    }

    private Float interpolateNewIntervalMax() {
        Rectangle seldRect = colorBarRects.get(seldIntrvl);
        Float newMax = colorBar.getIntervalMin(seldIntrvl)
                + (float) (dragXvalue - seldRect.x) / (float) seldRect.width
                        * (colorBar.getIntervalMax(seldIntrvl)
                                - colorBar.getIntervalMin(seldIntrvl));

        newMax = (float) (Math.round(newMax.floatValue() * scaleMult)
                / scaleMult);
        return newMax;
    }

    private void setIntervalMin(Float newMin) {

        boolean isInf = (newMin == Float.NEGATIVE_INFINITY ? true : false);

        intrvlMinSpnr.setVisible(!isInf);

        intrvlMinSpnr
                .setSelection((isInf ? Integer.MIN_VALUE : Math.round(newMin)));
    }

    private void setIntervalMax(Float newMax) {

        boolean isInf = (newMax == Float.POSITIVE_INFINITY ? true : false);

        intrvlMaxSpnr.setVisible(!isInf);

        intrvlMaxSpnr
                .setSelection((isInf ? Integer.MAX_VALUE : Math.round(newMax)));
    }

    @Override
    public void dispose() {
        super.dispose();

        if (font != null) {
            font.dispose();
        }
        if (canvas != null) {
            canvas.dispose();
        }
        if (seldColor != null) {
            seldColor.dispose();
        }
        if (labelColor != null) {
            labelColor.dispose();
        }
        if (pointerCursor != null) {
            pointerCursor.dispose();
        }
        if (dragIntrvlCursor != null) {
            dragIntrvlCursor.dispose();
        }
    }

    public ModifyListener minSpnrListener = new ModifyListener() {
        @Override
        public void modifyText(ModifyEvent e) {

            float intrvlValue = intrvlMinSpnr.getSelection();

            if (intrvlValue == Integer.MIN_VALUE) {
                return;
            }

            Float minVal = intrvlValue / (float) scaleMult;

            colorBar.setIntervalMin(seldIntrvl, minVal);
            computeColorBarRectangles();
        }
    };

    public ModifyListener maxSpnrListener = new ModifyListener() {
        @Override
        public void modifyText(ModifyEvent e) {

            float intrvlValue = intrvlMaxSpnr.getSelection();

            if (intrvlValue == Integer.MAX_VALUE) {
                return;
            }

            Float maxVal = intrvlMaxSpnr.getSelection() / (float) scaleMult;

            colorBar.setIntervalMax(seldIntrvl, maxVal);
            computeColorBarRectangles();
        }
    };

    private void addModifyListenersForSpinners() {
        intrvlMinSpnr.addModifyListener(minSpnrListener);
        intrvlMaxSpnr.addModifyListener(maxSpnrListener);
    }

    private void removeModifyListenersForSpinners() {
        intrvlMinSpnr.removeModifyListener(minSpnrListener);
        intrvlMaxSpnr.removeModifyListener(maxSpnrListener);
    }
}