/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package gov.noaa.nws.ncep.ui.nsharp.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.viz.core.IGraphicsTarget.LineStyle;
import com.raytheon.uf.viz.core.exception.VizException;

import gov.noaa.nws.ncep.ui.nsharp.NsharpConstants;
import gov.noaa.nws.ncep.ui.nsharp.NsharpLineProperty;
import gov.noaa.nws.ncep.ui.nsharp.display.NsharpEditor;
import gov.noaa.nws.ncep.ui.nsharp.display.rsc.NsharpResourceHandler;
import gov.noaa.nws.ncep.viz.common.ui.color.ColorMatrixSelector;

/**
 * gov.noaa.nws.ncep.ui.nsharp.palette.NsharpDataDisplayConfigDialog
 *
 *
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 03/21/2012    229       Chin Chen    Initial coding
 * Aug 17, 2018  #7081     dgilling     Refactor based on CaveJFACEDialog.
 *
 * </pre>
 *
 * @author Chin Chen
 */

public class NsharpDataDisplayConfigDialog extends AbstractNsharpConfigDlg {

    private List<String> availLine = new ArrayList<>();

    private NsharpLineProperty curLineProperty;

    private LineStyle curLineStyle;

    private int curLineWidth;

    private RGB curLineColor;

    private String selectedLineName = StringUtils.EMPTY;

    private Combo selLineCombo;

    private Canvas currentLinePreviewCanvas;

    private Canvas newLinePreviewCanvas;

    private ColorMatrixSelector colorSelector;

    private Color newLineCanvasColor;

    private Color curLineCanvasColor;

    public NsharpDataDisplayConfigDialog(Shell parentShell) {
        super(parentShell, "Nsharp Data Display");

        availLine = Arrays.asList(NsharpConstants.lineNameArray);

        curLineProperty = configStore.getLinePropertyMap()
                .get(availLine.get(0));
        curLineStyle = curLineProperty.getLineStyle();
        curLineWidth = curLineProperty.getLineWidth();
        curLineColor = curLineProperty.getLineColor();
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.setLayout(new GridLayout(2, false));
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        final Color black = composite.getDisplay()
                .getSystemColor(SWT.COLOR_BLACK);
        final Color white = composite.getDisplay()
                .getSystemColor(SWT.COLOR_WHITE);

        selLineCombo = new Combo(composite, SWT.DROP_DOWN);
        selLineCombo.setItems(availLine.toArray(new String[0]));
        selLineCombo.select(0);
        selLineCombo.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                String seldLine = selLineCombo.getText();
                if (seldLine.equals(selectedLineName)) {
                    return;
                }

                selectedLineName = seldLine;
                curLineProperty = configStore.getLinePropertyMap()
                        .get(selectedLineName);
                if (curLineProperty == null) {
                    // this only happened when configuration xml does not have
                    // this line property
                    curLineProperty = new NsharpLineProperty();
                    configStore.getLinePropertyMap().put(selectedLineName,
                            curLineProperty);
                }
                curLineStyle = curLineProperty.getLineStyle();
                curLineWidth = curLineProperty.getLineWidth();
                curLineColor = curLineProperty.getLineColor();

                newLinePreviewCanvas.redraw();
                currentLinePreviewCanvas.redraw();
                colorSelector.setColorValue(curLineColor);
            }
        });
        selLineCombo.setLayoutData(
                new GridData(SWT.FILL, SWT.DEFAULT, true, false));

        currentLinePreviewCanvas = new Canvas(composite, SWT.BORDER);
        currentLinePreviewCanvas.addPaintListener((e) -> {
            if (curLineCanvasColor != null) {
                curLineCanvasColor.dispose();
            }

            GC gc = e.gc;
            gc.setLineWidth(curLineWidth);
            currentLinePreviewCanvas.setBackground(
                    curLineColor.getHSB()[2] > 0.2 ? black : white);
            curLineCanvasColor = new Color(e.display, curLineColor);
            gc.setForeground(curLineCanvasColor);
            newLinePreviewCanvas.setBackground(
                    curLineColor.getHSB()[2] > 0.2 ? black : white);
            gc.setLineDash(curLineStyle.getSWTLineStyle());

            Rectangle bounds = ((Canvas) e.widget).getClientArea();
            int y = bounds.height / 2;
            gc.drawLine(0, y, bounds.width, y);
        });
        currentLinePreviewCanvas.addDisposeListener((e) -> {
            if (curLineCanvasColor != null) {
                curLineCanvasColor.dispose();
            }
        });
        GridData layoutData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        layoutData.heightHint = 30;
        currentLinePreviewCanvas.setLayoutData(layoutData);

        Composite leftControlsComp = new Composite(composite, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        leftControlsComp.setLayout(layout);
        leftControlsComp
                .setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        newLinePreviewCanvas = new Canvas(leftControlsComp, SWT.BORDER);
        newLinePreviewCanvas.addPaintListener((e) -> {
            if (newLineCanvasColor != null) {
                newLineCanvasColor.dispose();
            }

            GC gc = e.gc;
            gc.setLineWidth(curLineWidth);
            newLineCanvasColor = new Color(e.display, curLineColor);
            gc.setForeground(newLineCanvasColor);
            newLinePreviewCanvas.setBackground(
                    curLineColor.getHSB()[2] > 0.2 ? black : white);
            gc.setLineDash(curLineStyle.getSWTLineStyle());

            Rectangle bounds = ((Canvas) e.widget).getClientArea();
            int y = bounds.height / 2;
            gc.drawLine(0, y, bounds.width, y);
        });
        newLinePreviewCanvas.addDisposeListener((e) -> {
            if (newLineCanvasColor != null) {
                newLineCanvasColor.dispose();
            }
        });
        layoutData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        layoutData.heightHint = 30;
        newLinePreviewCanvas.setLayoutData(layoutData);

        Group selectLineWidthGroup = new Group(leftControlsComp, SWT.DEFAULT);
        selectLineWidthGroup.setText("Width");
        selectLineWidthGroup.setLayout(new GridLayout(2, true));
        selectLineWidthGroup
                .setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Map<Integer, Button> widthButtonMap = new HashMap<>();
        Integer[] lineWidthButtonSequence = { 1, 3, 2, 4 };
        for (Integer width : lineWidthButtonSequence) {
            Button button = new Button(selectLineWidthGroup, SWT.TOGGLE);
            button.setData(width);
            button.setToolTipText("Width " + width);
            Image image = createLineWidthImage(button, width);
            button.setImage(image);

            button.addDisposeListener((e) -> {
                image.dispose();
            });

            button.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    widthButtonMap.get(curLineWidth).setSelection(false);
                    curLineWidth = (Integer) e.widget.getData();
                    newLinePreviewCanvas.redraw();
                }
            });

            button.setLayoutData(
                    new GridData(SWT.CENTER, SWT.DEFAULT, true, false));

            widthButtonMap.put(width, button);
        }
        widthButtonMap.get(curLineWidth).setSelection(true);

        Group selectLineStyleGroup = new Group(leftControlsComp, SWT.DEFAULT);
        selectLineStyleGroup.setText("Style");
        selectLineStyleGroup.setLayout(new GridLayout(2, true));
        selectLineStyleGroup
                .setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        final Map<LineStyle, Button> lineStyleButtonMap = new EnumMap<>(
                LineStyle.class);
        final LineStyle[] lineStyleButtonSequence = { LineStyle.DOTS,
                LineStyle.LONG_DASHED, LineStyle.SOLID,
                LineStyle.LONG_DASH_THREE_SHORT_DASHES, LineStyle.SHORT_DASHED,
                LineStyle.LONG_DASH_DOT, LineStyle.MEDIUM_DASHED,
                LineStyle.LONG_DASH_THREE_DOTS, LineStyle.LONG_DASH_SHORT_DASH,
                LineStyle.MEDIUM_DASH_DOT, };

        for (LineStyle style : lineStyleButtonSequence) {
            Button button = new Button(selectLineStyleGroup, SWT.TOGGLE);
            button.setData(style);
            button.setToolTipText(style.name());
            Image image = createLineStyleImage(button, style);
            button.setImage(image);

            button.addDisposeListener((e) -> {
                image.dispose();
            });

            button.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    lineStyleButtonMap.get(curLineStyle).setSelection(false);
                    curLineStyle = (LineStyle) e.widget.getData();
                    newLinePreviewCanvas.redraw();
                }
            });

            button.setLayoutData(
                    new GridData(SWT.CENTER, SWT.DEFAULT, true, false));

            lineStyleButtonMap.put(style, button);
        }
        lineStyleButtonMap.get(curLineStyle).setSelection(true);

        Group selectLineColorGroup = new Group(composite, SWT.DEFAULT);
        selectLineColorGroup.setText("Color");
        selectLineColorGroup.setLayout(new GridLayout());
        selectLineColorGroup
                .setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        colorSelector = new ColorMatrixSelector(selectLineColorGroup, false,
                false, 28, 92, 18, 22, 4, 8, 5, false);
        colorSelector.setColorValue(curLineColor);
        colorSelector.addListener((e) -> {
            curLineColor = colorSelector.getColorValue();
            newLinePreviewCanvas.redraw();
        });

        colorSelector
                .setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

        return composite;
    }

    @Override
    protected void handleApplyClicked() {
        applyChanges();
    }

    @Override
    protected void handleSaveClicked() {
        applyChanges();
        try {
            mgr.saveConfigStoreToFs(configStore);
        } catch (VizException e) {
            statusHandler.error("Unable to save data display settings.");
        }
    }

    private void applyChanges() {
        curLineProperty.setLineColor(curLineColor);
        curLineProperty.setLineStyle(curLineStyle);
        curLineProperty.setLineWidth(curLineWidth);
        currentLinePreviewCanvas.redraw();
        notifyEditor();
    }

    private void notifyEditor() {
        NsharpEditor editor = NsharpEditor.getActiveNsharpEditor();
        if (editor != null) {
            NsharpResourceHandler rsc = editor.getRscHandler();
            rsc.setLinePropertyMap(configStore.getLinePropertyMap());
            editor.refresh();
        }
    }

    private Image createLineWidthImage(Button button, int lineWidth) {
        Point imageBounds = computeButtonSize(button);

        Image image = new Image(button.getDisplay(), imageBounds.x,
                imageBounds.y);
        GC gc = new GC(image);
        Color bgColor = gc.getBackground();
        gc.fillRectangle(0, 0, imageBounds.x, imageBounds.y);
        int y = imageBounds.y / 2;
        gc.setLineWidth(lineWidth);
        gc.drawLine(0, y, imageBounds.x, y);
        gc.dispose();

        ImageData imageData = image.getImageData();
        int transparentPixel = imageData.palette.getPixel(bgColor.getRGB());
        imageData.transparentPixel = transparentPixel;

        image.dispose();

        return new Image(button.getDisplay(), imageData);
    }

    private Image createLineStyleImage(Button button, LineStyle style) {
        Point imageBounds = computeButtonSize(button);

        Image image = new Image(button.getDisplay(), imageBounds.x,
                imageBounds.y);
        GC gc = new GC(image);
        Color bgColor = gc.getBackground();
        gc.fillRectangle(0, 0, imageBounds.x, imageBounds.y);
        int y = imageBounds.y / 2;
        gc.setLineDash(style.getSWTLineStyle());
        gc.drawLine(0, y, imageBounds.x, y);
        gc.dispose();

        ImageData imageData = image.getImageData();
        int transparentPixel = imageData.palette.getPixel(bgColor.getRGB());
        imageData.transparentPixel = transparentPixel;

        image.dispose();

        return new Image(button.getDisplay(), imageData);
    }

    private Point computeButtonSize(final Control control) {
        GC gc = new GC(control);
        Font f = JFaceResources.getFontRegistry()
                .get(JFaceResources.DIALOG_FONT);
        gc.setFont(f);
        int height = gc.getFontMetrics().getHeight();
        int width = control.getDisplay().getDPI().x;
        gc.dispose();
        return new Point(width, height);
    }
}
