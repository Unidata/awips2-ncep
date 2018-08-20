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
package gov.noaa.nws.ncep.viz.common.ui.color;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Pattern;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

/**
 * The <code>ColorMatrixSelector</code> can be used in place of JFace
 * <code>ColorSelector</code> to provide a familiar GEMPAK color matrix palette,
 * in addition to a path to the standard OS specific SWT ColorDialog.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 21 Apr 2009   74/90     B. Hebbard   Initial implementation.
 * 11 May 2009   74B       B. Hebbard   Suppress selectedColorCanvas if requested size zero
 * 11/25/2016    R21762    P. Moyer     Implemented alpha transparency/"No Color" button option
 * Aug 22, 2018  #7081     dgilling     Refactor based on Composite.
 *
 * </pre>
 *
 * @author bhebbard
 */

public class ColorMatrixSelector extends Composite {

    // Property name to use in color change event
    private static final String PROP_COLORCHANGE = "colorValue";

    // Property name to use in Alpha change event
    private static final String PROP_ALPHACHANGE = "alphaValue";

    private final Point previewCanvasSize;

    private final Point colorButtonSize;

    private final int colorButtonSpacing;

    private final boolean isHorizontalLayout;

    private final boolean isColorGridHorizontalLayout;

    private final boolean isUsingTransparentColor;

    private final ListenerList<IPropertyChangeListener> listeners;

    // A (sparse) mapping of RGB triplets back to the GEMPAK palette colors
    private final Map<RGB, GempakColor> RGBGempakColorMap;

    // Given a GEMPAK color, take us back to its own button
    private final Map<GempakColor, Button> colorButtonMap;

    private Color currentColor;

    private RGB currentRGB;

    private int currentAlpha;

    private RGB oldRGB;

    private int oldAlpha;

    private Canvas previewCanvas;

    private Composite buttonGridComposite;

    private Button noColorButton;

    /**
     * Constructor: Create a new instance of the receiver in the supplied parent
     * <code>Composite</code>. Defaults to no "No Color" button added.
     *
     * @param parent
     *            A parent Composite to contain the new color matrix selector.
     *            Note that the receiver will assign a layout to the parent.
     * @param horizontalLayout
     *            If true, the preview area, selection matrix, and custom color
     *            button will be laid out horizontally; if false, vertically.
     * @param horizontalMatrix
     *            If true, the color matrix itself will be laid out
     *            horizontally; if false, vertically.
     * @param selectedColorCanvasHeight
     *            Height (pixels) of preview area showing the selected color.
     * @param selectedColorCanvasWidth
     *            Width (pixels) of preview area showing the selected color.
     * @param colorButtonHeight
     *            Height (pixels) of each individual button in the color matrix.
     * @param colorButtonWidth
     *            Width (pixels) of each individual button in the color matrix.
     * @param firstSpace
     *            Space (pixels) 'before' color preview area (above if vertical
     *            layout; left if horizontal layout).
     * @param interSpace
     *            Space (pixels) between button matrix and both preview area and
     *            custom color button.
     * @param colorButtonSpace
     *            Space (pixels) between individual buttons in matrix, both
     *            horizontally and vertically.
     */
    public ColorMatrixSelector(final Composite parent,
            boolean horizontalLayout, boolean horizontalMatrix,
            int selectedColorCanvasHeight, int selectedColorCanvasWidth,
            int colorButtonHeight, int colorButtonWidth, int firstSpace,
            int interSpace, int colorButtonSpace) {
        this(parent, horizontalLayout, horizontalMatrix,
                selectedColorCanvasHeight, selectedColorCanvasWidth,
                colorButtonHeight, colorButtonWidth, firstSpace, interSpace,
                colorButtonSpace, false);

    }

    /**
     * Constructor: Create a new instance of the receiver in the supplied parent
     * <code>Composite</code>.
     *
     * @param parent
     *            A parent Composite to contain the new color matrix selector.
     *            Note that the receiver will assign a layout to the parent.
     * @param horizontalLayout
     *            If true, the preview area, selection matrix, and custom color
     *            button will be laid out horizontally; if false, vertically.
     * @param horizontalMatrix
     *            If true, the color matrix itself will be laid out
     *            horizontally; if false, vertically.
     * @param selectedColorCanvasHeight
     *            Height (pixels) of preview area showing the selected color.
     * @param selectedColorCanvasWidth
     *            Width (pixels) of preview area showing the selected color.
     * @param colorButtonHeight
     *            Height (pixels) of each individual button in the color matrix.
     * @param colorButtonWidth
     *            Width (pixels) of each individual button in the color matrix.
     * @param firstSpace
     *            Space (pixels) 'before' color preview area (above if vertical
     *            layout; left if horizontal layout).
     * @param interSpace
     *            Space (pixels) between button matrix and both preview area and
     *            custom color button.
     * @param colorButtonSpace
     *            Space (pixels) between individual buttons in matrix, both
     *            horizontally and vertically.
     * @param alphaActive
     *            Enables or disables "No Color" button option.
     */
    public ColorMatrixSelector(final Composite parent,
            boolean horizontalLayout, boolean horizontalMatrix,
            int selectedColorCanvasHeight, int selectedColorCanvasWidth,
            int colorButtonHeight, int colorButtonWidth, int firstSpace,
            int interSpace, int colorButtonSpace, boolean alphaActive) {
        super(parent, SWT.NONE);

        this.listeners = new ListenerList<>(ListenerList.IDENTITY);

        this.isHorizontalLayout = horizontalLayout;
        this.isColorGridHorizontalLayout = horizontalMatrix;
        this.previewCanvasSize = new Point(selectedColorCanvasWidth,
                selectedColorCanvasHeight);
        this.colorButtonSize = new Point(colorButtonWidth, colorButtonHeight);
        this.colorButtonSpacing = colorButtonSpace;
        this.isUsingTransparentColor = alphaActive;

        this.currentRGB = null;
        this.currentAlpha = 255;

        this.RGBGempakColorMap = new HashMap<>();
        this.colorButtonMap = new EnumMap<>(GempakColor.class);

        int numColumns = (this.isHorizontalLayout) ? 2 : 1;
        GridLayout layout = new GridLayout(numColumns, false);
        if (this.isHorizontalLayout) {
            layout.marginLeft = firstSpace;
            layout.horizontalSpacing = interSpace;
        } else {
            layout.marginTop = firstSpace;
            layout.verticalSpacing = interSpace;
        }
        setLayout(layout);

        initializeComponents();
    }

    private void initializeComponents() {
        createColorPreviewCanvas();

        createColorButtonGrid();

        createButtons();
    }

    private void createColorPreviewCanvas() {
        previewCanvas = new Canvas(this, SWT.BORDER);

        previewCanvas.addPaintListener((e) -> {
            if (currentColor != null) {
                Canvas canvas = (Canvas) e.widget;

                if (currentColor.getAlpha() != 0) {
                    canvas.setBackground(currentColor);
                }
                if (currentColor.getAlpha() == 0) {
                    final Pattern pattern = assembleAlphaPattern(canvas);

                    Rectangle r = canvas.getClientArea();
                    GC gc = e.gc;
                    gc.setBackgroundPattern(pattern);
                    gc.setForegroundPattern(pattern);
                    gc.fillRectangle(r);
                }
            }
        });

        previewCanvas.addDisposeListener((e) -> {
            if (currentColor != null) {
                currentColor.dispose();
            }
        });

        GridData layoutData = (isHorizontalLayout)
                ? new GridData(SWT.LEFT, SWT.CENTER, false, false)
                : new GridData(SWT.CENTER, SWT.TOP, false, false);
        layoutData.widthHint = previewCanvasSize.x;
        layoutData.heightHint = previewCanvasSize.y;
        previewCanvas.setLayoutData(layoutData);

        if (previewCanvasSize.x == 0 && previewCanvasSize.y == 0) {
            previewCanvas.setVisible(false);
        }
    }

    private void createColorButtonGrid() {
        buttonGridComposite = new Composite(this, SWT.BORDER);

        GridLayout layout = new GridLayout();
        layout.numColumns = isColorGridHorizontalLayout ? 8 : 4;
        layout.makeColumnsEqualWidth = true;
        layout.horizontalSpacing = colorButtonSpacing;
        layout.verticalSpacing = colorButtonSpacing;
        buttonGridComposite.setLayout(layout);

        GridData layoutData = (isHorizontalLayout)
                ? new GridData(SWT.DEFAULT, SWT.FILL, false, true)
                : new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        buttonGridComposite.setLayoutData(layoutData);

        for (GempakColor gColor : GempakColor.values()) {
            Button colorButton = new Button(buttonGridComposite, SWT.TOGGLE);
            colorButton.setBackground(new Color(getDisplay(), gColor.getRGB()));
            colorButton.setToolTipText(gColor.toString().toLowerCase());

            colorButton.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    Control control = (Control) e.widget;
                    setColorValue(control.getBackground().getRGB());
                    setAlphaValue(new Integer(255));
                    fireColorChangeEvent();
                }
            });
            colorButton.addDisposeListener((e) -> {
                Control widget = (Control) e.widget;
                Color color = widget.getBackground();
                color.dispose();
            });

            layoutData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
            layoutData.heightHint = colorButtonSize.y;
            layoutData.widthHint = colorButtonSize.x;
            colorButton.setLayoutData(layoutData);

            RGBGempakColorMap.put(gColor.getRGB(), gColor);
            colorButtonMap.put(gColor, colorButton);
        }
    }

    private void createButtons() {
        Composite buttonComposite = new Composite(this, SWT.NONE);
        GridLayout layout = (isHorizontalLayout) ? new GridLayout(2, true)
                : new GridLayout();
        buttonComposite.setLayout(layout);
        GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
        if (isHorizontalLayout) {
            layoutData.horizontalSpan = 2;
        }
        buttonComposite
                .setLayoutData(layoutData);

        Button customColorButton = new Button(buttonComposite, SWT.PUSH);
        customColorButton
                .setText(isHorizontalLayout ? "More Colors" : "Custom...");
        customColorButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                ColorDialog colorDialog = new ColorDialog(getShell());
                colorDialog.setRGB(currentRGB);
                buttonGridComposite.setEnabled(false);
                customColorButton.setEnabled(false);
                RGB returnColor = colorDialog.open();
                buttonGridComposite.setEnabled(true);
                customColorButton.setEnabled(true);
                if (returnColor != null) {
                    setColorValue(returnColor);
                    fireColorChangeEvent();
                }
            }
        });
        customColorButton.setLayoutData(
                new GridData(SWT.CENTER, SWT.TOP, true, false));

        if (isUsingTransparentColor) {
            noColorButton = new Button(buttonComposite, SWT.TOGGLE);
            noColorButton.setText("No Color");
            noColorButton.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    setColorValue(new RGB(1, 1, 1)); // really dark grey
                    setAlphaValue(0);
                    fireColorChangeEvent();
                    fireAlphaChangeEvent();
                }
            });
            noColorButton.setLayoutData(
                    new GridData(SWT.CENTER, SWT.TOP, true, false));
        }
    }

    /**
     * Set the current color value and update the control.
     *
     * @param newRGB
     *            The new color.
     */
    public void setColorValue(RGB newRGB) {
        oldRGB = currentRGB;
        currentRGB = newRGB;

        /*
         * If previous color was a GEMPAK color, make sure its matrix button is
         * popped out (deselected)...
         */
        if (!currentRGB.equals(oldRGB)) {
            GempakColor oldGempakColor = RGBGempakColorMap.get(oldRGB);
            if (oldGempakColor != null) {
                colorButtonMap.get(oldGempakColor).setSelection(false);
            }
        }

        /*
         * ...and if the new color is a GEMPAK color, make sure its button is
         * pushed. (Note we need to do this even if the selection came from a
         * button push; if same as previous color, SWT.TOGGLE button will pop
         * out if it was already in.)
         */
        GempakColor newGempakColor = RGBGempakColorMap.get(currentRGB);
        if (newGempakColor != null) {
            colorButtonMap.get(newGempakColor).setSelection(true);
        }

        colorChanged();
    }

    public RGB getColorValue() {
        return currentRGB;
    }

    /**
     * Set the current Alpha value and update the control.
     *
     * @param newAlpha
     *            The new color.
     */
    public void setAlphaValue(int newAlpha) {
        oldAlpha = currentAlpha;
        currentAlpha = newAlpha;

        if (isUsingTransparentColor) {
            if (currentAlpha == 0) {
                /*
                 * if incoming value is 0, then make the "No Color" button
                 * appear selected
                 */
                noColorButton.setSelection(true);
            } else if (currentAlpha == 255) {
                /*
                 * if the incoming value is 255, then make the "No Color" button
                 * appear unselected the color buttons will be handled by
                 * setColorValue
                 */
                noColorButton.setSelection(false);
            } else {
                noColorButton.setSelection(false);
            }

            colorChanged();
        }
    }

    public int getAlphaValue() {
        return currentAlpha;
    }

    private void colorChanged() {
        if (currentColor != null) {
            currentColor.dispose();
        }
        currentColor = new Color(getDisplay(), currentRGB, currentAlpha);
        previewCanvas.redraw();
    }

    /**
     * Adds a property change listener to this <code>ColorMatrixSelector</code>.
     * Events are fired when the color in the control changes via the user
     * clicking and selecting a new one in the color matrix or the custom color
     * dialog. No event is fired in the case where
     * <code>setColorValue(RGB)</code> is invoked, or if the new color is
     * identical to the old one.
     *
     * @param listener
     *            A property change listener
     *
     */
    public void addListener(IPropertyChangeListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Removes the given listener from this <code>ColorMatrixSelector</code>.
     * Has no effect if the listener is not registered.
     *
     * @param listener
     *            A property change listener
     */
    public void removeListener(IPropertyChangeListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    protected void fireColorChangeEvent() {
        // Notify all registered listeners that a color change has occurred.
        if (!listeners.isEmpty() && !currentRGB.equals(oldRGB)) {
            PropertyChangeEvent colorChangeEvent = new PropertyChangeEvent(this,
                    PROP_COLORCHANGE, oldRGB, currentRGB);
            for (Object listener : listeners.getListeners()) {
                IPropertyChangeListener casted = (IPropertyChangeListener) listener;
                casted.propertyChange(colorChangeEvent);
            }
        }
    }

    protected void fireAlphaChangeEvent() {
        // Notify all registered listeners that an alpha change has occurred.
        if (!listeners.isEmpty() && currentAlpha != oldAlpha) {
            PropertyChangeEvent colorChangeEvent = new PropertyChangeEvent(this,
                    PROP_ALPHACHANGE, oldAlpha, currentAlpha);
            for (Object listener : listeners.getListeners()) {
                IPropertyChangeListener casted = (IPropertyChangeListener) listener;
                casted.propertyChange(colorChangeEvent);
            }
        }
    }

    private Pattern assembleAlphaPattern(final Canvas canvas) {
        Display display = canvas.getDisplay();
        // define a pattern on an image
        final Image image = new Image(display, 20, 20);
        Color grey = display.getSystemColor(SWT.COLOR_GRAY);
        Color white = display.getSystemColor(SWT.COLOR_WHITE);
        GC gc = new GC(image);
        gc.setBackground(white);
        gc.fillRectangle(0, 0, 20, 20);
        gc.setBackground(grey);
        gc.fillRectangle(0, 0, 10, 10);
        gc.fillRectangle(10, 10, 10, 10);
        gc.dispose();
        final Pattern pattern;
        pattern = new Pattern(display, image);
        return pattern;
    }
}
