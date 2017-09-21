package gov.noaa.nws.ncep.viz.overlays.dialogs;

import gov.noaa.nws.ncep.viz.common.ui.color.ColorMatrixSelector;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResourceData;
import gov.noaa.nws.ncep.viz.resources.attributes.AbstractEditResourceAttrsDialog;
import gov.noaa.nws.ncep.viz.resources.attributes.ResourceAttrSet.RscAttrValue;

import java.util.EnumMap;
import java.util.Map;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.viz.core.IGraphicsTarget.LineStyle;
import com.raytheon.uf.viz.core.rsc.capabilities.Capabilities;

/**
 * Provides an interface to modify the line overlay parameters
 * 
 * This dialog must serve two purposes: Allow 'live' editing of an already
 * loaded resource, and selection of preferences for an RBD for future use. For
 * this reason, the dialog (1) allows a caller to set preselected values (say,
 * from current resource settings, or from tables of defaults for a particular
 * resource type), (2) stores selected values in private member variables, and
 * (3) allows the caller to retrieve selected values after the dialog has been
 * closed.
 * 
 * (Design note: Item (2) above is required because we can't interrogate the
 * settings of widgets after the dialog has been closed, because they may have
 * been destroyed by then.)
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 06 Feb 2009  53          bhebbard    Initial Creation.
 * 15 Apr 2009  53B         bhebbard    Implement preview; show line images on
 *                                      width/style buttons; remove LineScale;
 *                                      strengthen error checking in setAttributesFromMap;
 *                                      various layout and other cleanups.
 * 21 Apr 2009  90          bhebbard    Factor out color selection; use ColorMatrixSelector.
 * 17 Jun 2009  115         Greg Hull   Integrated with AbstractEditResourceAttrsDialog
 * 27 Apr 2010   #245       Greg Hull   Added Apply Button
 * 04/05/2016   R15715      dgilling    Refactored for new AbstractEditResourceAttrsDialog constructor.
 * 
 * </pre>
 * 
 * @author bhebbard
 * @version 1
 */

public class ChangeLineAttributesDialog extends AbstractEditResourceAttrsDialog {

    // Current attribute values.

    // (Just initialize each to a 'neutral' default state on dialog
    // creation; typically the caller will later use setter methods
    // to apply particular defaults before opening the dialog.)

    private RscAttrValue lineStyle = null;

    private RscAttrValue lineColor = null;

    private RscAttrValue lineWidth = null;

    public ChangeLineAttributesDialog(Shell parentShell,
            INatlCntrsResourceData rd, Capabilities capabilities, Boolean apply) {
        super(parentShell, rd, capabilities, apply);
    }

    @Override
    public Composite createDialog(Composite composite) {
        final Display display = composite.getDisplay();

        FormLayout layout0 = new FormLayout();
        composite.setLayout(layout0);

        lineStyle = editedRscAttrSet.getRscAttr("lineStyle");
        lineColor = editedRscAttrSet.getRscAttr("color");
        lineWidth = editedRscAttrSet.getRscAttr("lineWidth");

        // confirm the classes of the attributes..
        if (lineStyle.getAttrClass() != LineStyle.class) {
            statusHandler.error("lineStyle is not of expected class? "
                    + lineStyle.getAttrClass().toString());
        } else if (lineColor.getAttrClass() != RGB.class) {
            statusHandler.error("lineColor is not of expected class? "
                    + lineColor.getAttrClass().toString());
        } else if (lineWidth.getAttrClass() != Integer.class) {
            statusHandler.error("lineWidth is not of expected class? "
                    + lineWidth.getAttrClass().toString());
        }

        // Lay out the various groups within the dialog

        Group linePreviewAreaGroup = new Group(composite, SWT.SHADOW_NONE);
        linePreviewAreaGroup.setLayout(new FillLayout());

        FormData formData0 = new FormData();
        formData0.top = new FormAttachment(5, 0);
        formData0.left = new FormAttachment(2, 0);
        formData0.width = 196;
        formData0.height = 30;
        linePreviewAreaGroup.setLayoutData(formData0);

        Group selectLineWidthGroup = new Group(composite, SWT.SHADOW_NONE);
        selectLineWidthGroup.setText("Width");
        GridLayout lineWidthGridLayout = new GridLayout();
        lineWidthGridLayout.numColumns = 2;
        lineWidthGridLayout.marginHeight = 18;
        lineWidthGridLayout.marginWidth = 18;
        lineWidthGridLayout.horizontalSpacing = 8;
        lineWidthGridLayout.verticalSpacing = 8;
        selectLineWidthGroup.setLayout(lineWidthGridLayout);

        FormData formData1 = new FormData();
        formData1.top = new FormAttachment(linePreviewAreaGroup, 16);
        formData1.left = new FormAttachment(2, 0);
        selectLineWidthGroup.setLayoutData(formData1);

        Group selectLineStyleGroup = new Group(composite, SWT.SHADOW_NONE);
        selectLineStyleGroup.setText("Style");
        GridLayout lineStyleGridLayout = new GridLayout();
        lineStyleGridLayout.numColumns = 2;
        lineStyleGridLayout.marginHeight = 18;
        lineStyleGridLayout.marginWidth = 18;
        lineStyleGridLayout.horizontalSpacing = 8;
        lineStyleGridLayout.verticalSpacing = 8;
        selectLineStyleGroup.setLayout(lineStyleGridLayout);

        FormData formData2 = new FormData();
        formData2.top = new FormAttachment(selectLineWidthGroup, 16);
        formData2.left = new FormAttachment(2, 0);
        selectLineStyleGroup.setLayoutData(formData2);

        Group selectLineColorGroup = new Group(composite, SWT.SHADOW_NONE);
        selectLineColorGroup.setText("Color");

        FormData formData5 = new FormData();
        formData5.top = new FormAttachment(5, 0);
        formData5.left = new FormAttachment(selectLineWidthGroup, 10);
        formData5.right = new FormAttachment(98, 0);
        formData5.height = 300;
        selectLineColorGroup.setLayoutData(formData5);

        final Color black = composite.getDisplay().getSystemColor(
                SWT.COLOR_BLACK);
        final Color white = composite.getDisplay().getSystemColor(
                SWT.COLOR_WHITE);

        // Line Preview Area

        final Canvas linePreviewAreaCanvas = new Canvas(linePreviewAreaGroup,
                SWT.NONE);

        final int previewLineXmin = 16;
        final int previewLineXmax = 180;
        final int previewLineYctr = 16;

        linePreviewAreaCanvas.addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent event) {
                GC gc = event.gc;
                gc.setLineWidth(((Integer) lineWidth.getAttrValue()).intValue());
                gc.setForeground(new Color(display, (RGB) lineColor
                        .getAttrValue()));
                linePreviewAreaCanvas.setBackground(((RGB) lineColor
                        .getAttrValue()).getHSB()[2] > 0.2 ? black : white);
                int x1 = previewLineXmin;
                int x2 = previewLineXmin;
                int[] segLengths = STYLE_MAP.get(lineStyle.getAttrValue());
                while (x2 < previewLineXmax) {
                    boolean draw = true;
                    for (int i : segLengths) {
                        x2 = Math.min(x1 + i, previewLineXmax);
                        if (draw) {
                            gc.drawLine(x1, previewLineYctr, x2,
                                    previewLineYctr);
                        }
                        if (x2 >= previewLineXmax) {
                            break;
                        }
                        draw = !draw;
                        x1 = x2;
                    }
                }
            }
        });

        // Parameters to give a uniform look to all line width/style buttons

        final int lineButtonHeight = 75;
        final int lineButtonWidth = 15;
        final int buttonLineXmin = 8;
        final int buttonLineXmax = 68;
        final int buttonLineYctr = 7;

        // Line Width

        final Button[] selectLineWidthButtons = new Button[4];
        final int[] lineWidthButtonSequence = { 0, 2, // ...for 2-column grid
                                                      // layout
                1, 3 };
        for (int i : lineWidthButtonSequence) {
            selectLineWidthButtons[i] = new Button(selectLineWidthGroup,
                    SWT.TOGGLE);
            GridData gridData = new GridData();
            gridData.heightHint = lineButtonWidth;
            gridData.widthHint = lineButtonHeight;
            selectLineWidthButtons[i].setLayoutData(gridData);
            selectLineWidthButtons[i].setData(i + 1);
            selectLineWidthButtons[i].setToolTipText("Width " + (i + 1));
            selectLineWidthButtons[i].addPaintListener(new PaintListener() {
                @Override
                public void paintControl(PaintEvent event) {
                    GC gc = event.gc;
                    int width = (Integer) event.widget.getData();
                    gc.setLineWidth(width);
                    gc.setForeground(black);
                    gc.drawLine(buttonLineXmin, buttonLineYctr, buttonLineXmax,
                            buttonLineYctr);
                }
            });
            selectLineWidthButtons[i]
                    .addSelectionListener(new SelectionListener() {
                        @Override
                        public void widgetSelected(SelectionEvent event) {
                            selectLineWidthButtons[(Integer) lineWidth
                                    .getAttrValue() - 1].setSelection(false);
                            lineWidth.setAttrValue(event.widget.getData());
                            linePreviewAreaCanvas.redraw();
                            linePreviewAreaCanvas.update();
                        }

                        @Override
                        public void widgetDefaultSelected(SelectionEvent event) {
                        }
                    });
        }
        selectLineWidthButtons[(Integer) lineWidth.getAttrValue() - 1]
                .setSelection(true); // set initial state

        // Line Style

        final Map<LineStyle, Button> lineStyleButtonMap = new EnumMap<LineStyle, Button>(
                LineStyle.class);

        final LineStyle[] lineStyleButtonSequence = { // ...for 2-column grid
                // layout
                LineStyle.DOTS, LineStyle.LONG_DASHED, LineStyle.SOLID,
                LineStyle.LONG_DASH_THREE_SHORT_DASHES, LineStyle.SHORT_DASHED,
                LineStyle.LONG_DASH_DOT, LineStyle.MEDIUM_DASHED,
                LineStyle.LONG_DASH_THREE_DOTS, LineStyle.LONG_DASH_SHORT_DASH,
                LineStyle.MEDIUM_DASH_DOT, };

        for (LineStyle ls : lineStyleButtonSequence) {
            Button lineStyleButton = new Button(selectLineStyleGroup,
                    SWT.TOGGLE);
            lineStyleButtonMap.put(ls, lineStyleButton);
            GridData gridData = new GridData();
            gridData.heightHint = lineButtonWidth;
            gridData.widthHint = lineButtonHeight;
            lineStyleButton.setLayoutData(gridData);
            lineStyleButton.setData(ls);
            lineStyleButton.setToolTipText(ls.name());
            lineStyleButton.addPaintListener(new PaintListener() {
                @Override
                public void paintControl(PaintEvent event) {
                    GC gc = event.gc;
                    gc.setLineWidth(1);
                    gc.setForeground(black);
                    LineStyle ls = (LineStyle) event.widget.getData();
                    int[] segLengths = STYLE_MAP.get(ls);
                    int x1 = buttonLineXmin;
                    int x2 = buttonLineXmin;
                    while (x2 < buttonLineXmax) {
                        boolean draw = true;
                        for (int i : segLengths) {
                            x2 = Math.min(x1 + i, buttonLineXmax);
                            if (draw) {
                                gc.drawLine(x1, buttonLineYctr, x2,
                                        buttonLineYctr);
                            }
                            if (x2 >= buttonLineXmax) {
                                break;
                            }
                            draw = !draw;
                            x1 = x2;
                        }
                    }
                }
            });
            lineStyleButton.addSelectionListener(new SelectionListener() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    lineStyleButtonMap.get(lineStyle.getAttrValue())
                            .setSelection(false);
                    lineStyle.setAttrValue(event.widget.getData());
                    linePreviewAreaCanvas.redraw();
                    linePreviewAreaCanvas.update();
                }

                @Override
                public void widgetDefaultSelected(SelectionEvent event) {
                }
            });
        }
        lineStyleButtonMap.get(lineStyle.getAttrValue()).setSelection(true); // set
                                                                             // initial
                                                                             // state

        // Line Color

        final ColorMatrixSelector cms = new ColorMatrixSelector(
                selectLineColorGroup, false, false, 28, 92, 18, 22, 28, 80, 4,
                8, 5);
        cms.setColorValue((RGB) lineColor.getAttrValue());
        cms.addListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                lineColor.setAttrValue(cms.getColorValue());
                linePreviewAreaCanvas.redraw();
                linePreviewAreaCanvas.update();
            }
        });

        // from JFaceDialog
        // applyDialogFont(composite);
        return composite;
    }

    @Override
    public void initWidgets() {
        // TODO Auto-generated method stub

    }
}
