/**
 * 
 */
package gov.noaa.nws.ncep.viz.common.ui.color;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.common.EventManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Pattern;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;

/**
 * The <code>ColorMatrixSelector</code> can be used in place of JFace
 * <code>ColorSelector</code> to provide a familiar GEMPAK color matrix palette,
 * in addition to a path to the standard OS specific SWT ColorDialog.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 21 Apr 2009  74/90       B. Hebbard  Initial implementation.
 * 11 May 2009  74B         B. Hebbard  Suppress selectedColorCanvas if requested size zero
 * 11/25/2016   R21762      P. Moyer    Implemented alpha transparency/"No Color" button option
 * </pre>
 * 
 * @author bhebbard
 * @version 1
 */
public class ColorMatrixSelector extends EventManager {

    // The currently selected color, as SWT RGB object
    private RGB currentRGB = null;

    // The previously selected color (if any)
    private RGB oldRGB = null;

    // The currently selected alpha; defaults to 255 for safety
    private Integer currentAlpha = new Integer(255);

    // The previously selected alpha
    private Integer oldAlpha = null;

    private boolean activeAlpha = false;

    // The currently selected color as SWT Color object (requires disposal care)
    private Color currentColor = null;

    // Property name to use in color change event
    public static final String PROP_COLORCHANGE = "colorValue"; //$NON-NLS-1$

    // Property name to use in Alpha change event
    public static final String PROP_ALPHACHANGE = "alphaValue"; //$NON-NLS-1$

    // A (sparse) mapping of RGB triplets back to the GEMPAK palette colors
    private final Map<RGB, GempakColor> RGBGempakColorMap = new HashMap<>(
            2 * 32);

    // Given a GEMPAK color, take us back to its own button
    private final Map<GempakColor, Button> colorButtonMap = new EnumMap<>(
            GempakColor.class);

    // Composite given to us by caller in which to place selector
    private final Composite parentComposite;

    // The 'demo' area where we show the user the current selected color
    private final Canvas selectedColorCanvas;

    // the No Color button
    private Button noColorButton = null;

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
     * @param customButtonHeight
     *            Height (pixels) of button to bring up the custom color dialog.
     * @param customButtonWidth
     *            Width (pixels) of button to bring up the custom color dialog.
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
            final boolean horizontalLayout, final boolean horizontalMatrix,
            final int selectedColorCanvasHeight,
            final int selectedColorCanvasWidth, final int colorButtonHeight,
            final int colorButtonWidth, final int customButtonHeight,
            final int customButtonWidth, final int firstSpace,
            final int interSpace, final int colorButtonSpace) {
        this(parent, horizontalLayout, horizontalMatrix,
                selectedColorCanvasHeight, selectedColorCanvasWidth,
                colorButtonHeight, colorButtonWidth, customButtonHeight,
                customButtonWidth, firstSpace, interSpace, colorButtonSpace,
                false);

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
     * @param customButtonHeight
     *            Height (pixels) of button to bring up the custom color dialog.
     * @param customButtonWidth
     *            Width (pixels) of button to bring up the custom color dialog.
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
            final boolean horizontalLayout, final boolean horizontalMatrix,
            final int selectedColorCanvasHeight,
            final int selectedColorCanvasWidth, final int colorButtonHeight,
            final int colorButtonWidth, final int customButtonHeight,
            final int customButtonWidth, final int firstSpace,
            final int interSpace, final int colorButtonSpace,
            final boolean alphaActive) {
        // TODO: Think about changing booleans to enums
        parentComposite = parent;

        activeAlpha = alphaActive;

        FormLayout parentLayout = new FormLayout();
        parentComposite.setLayout(parentLayout);

        // (1) The 'demo' area where we show the user the current selected color

        selectedColorCanvas = new Canvas(parentComposite, SWT.BORDER);
        createSelectedColorCanvas(horizontalLayout, selectedColorCanvasHeight,
                selectedColorCanvasWidth, firstSpace);

        // (2) The matrix of color buttons for familiar GEMPAK palette

        final Group buttonMatrixGroup = createGempakColorMatrix(
                horizontalLayout, horizontalMatrix, colorButtonHeight,
                colorButtonWidth, firstSpace, interSpace, colorButtonSpace);

        // (3) A button to take us to a dialog where the user can get any
        // desired color

        final Button customColorButton = createCustomColorButton(
                horizontalLayout, customButtonHeight, customButtonWidth,
                firstSpace, interSpace, buttonMatrixGroup);

        // (4) The optional "No Color" button for transparency activation

        createNoColorButton(horizontalLayout, customButtonHeight,
                customButtonWidth, interSpace, buttonMatrixGroup,
                customColorButton);

        if (RGBGempakColorMap.get(currentRGB) != null) // set initial state
        {
            colorButtonMap.get(RGBGempakColorMap.get(currentRGB))
                    .setSelection(true);
        }

    }

    /**
     * Creates the NoColor button if the activeAlpha is true.
     * 
     * @param horizontalLayout
     * @param customButtonHeight
     * @param customButtonWidth
     * @param interSpace
     * @param buttonMatrixGroup
     * @param customColorButton
     */
    private void createNoColorButton(final boolean horizontalLayout,
            final int customButtonHeight, final int customButtonWidth,
            final int interSpace, final Group buttonMatrixGroup,
            final Button customColorButton) {
        if (activeAlpha) {
            noColorButton = new Button(parentComposite, SWT.TOGGLE);
            FormData formData4 = new FormData(customButtonWidth,
                    customButtonHeight);
            formData4.height = customButtonHeight;
            formData4.width = customButtonWidth;
            if (horizontalLayout) {
                formData4.top = new FormAttachment(80, 0);
                formData4.left = new FormAttachment(customColorButton,
                        interSpace);
            } else {
                formData4.top = new FormAttachment(customColorButton,
                        interSpace);
                formData4.left = new FormAttachment(buttonMatrixGroup, 0,
                        SWT.CENTER);
            }
            noColorButton.setLayoutData(formData4);

            noColorButton.setText("No Color");
            noColorButton.addSelectionListener(new SelectionListener() {
                public void widgetSelected(SelectionEvent event) {
                    setColorValue(new RGB(1, 1, 1)); // really dark grey
                    setAlphaValue(new Integer(0));
                    fireColorChangeEvent();
                    fireAlphaChangeEvent();
                }

                public void widgetDefaultSelected(SelectionEvent event) {
                }
            });
        }
    }

    /**
     * Creates the CustomColorButton that can invoke the system color picker.
     * 
     * @param horizontalLayout
     * @param customButtonHeight
     * @param customButtonWidth
     * @param firstSpace
     * @param interSpace
     * @param buttonMatrixGroup
     * @return
     */
    private Button createCustomColorButton(final boolean horizontalLayout,
            final int customButtonHeight, final int customButtonWidth,
            final int firstSpace, final int interSpace,
            final Group buttonMatrixGroup) {
        final Button customColorButton = new Button(parentComposite, SWT.NONE);
        FormData formData3 = new FormData(customButtonWidth,
                customButtonHeight);
        formData3.height = customButtonHeight;
        formData3.width = customButtonWidth;
        if (horizontalLayout) {
            /*
             * formData3.top = new FormAttachment(buttonMatrixGroup, 0,
             * SWT.CENTER); //TTR 44 formData3.left = new
             * FormAttachment(buttonMatrixGroup, interSpace);
             */
            formData3.top = new FormAttachment(80, 0);
            formData3.left = new FormAttachment(firstSpace, 0);
        } else {
            formData3.top = new FormAttachment(buttonMatrixGroup, interSpace);
            formData3.left = new FormAttachment(buttonMatrixGroup, 0,
                    SWT.CENTER);
        }
        customColorButton.setLayoutData(formData3);

        // customColorButton.setText(horizontalLayout ? "..." : "Custom...");
        // TTR 44
        customColorButton
                .setText(horizontalLayout ? "More Colors" : "Custom...");
        customColorButton.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent event) {
                ColorDialog colorDialog = new ColorDialog(
                        parentComposite.getShell());
                colorDialog.setRGB(currentRGB);
                buttonMatrixGroup.setEnabled(false);
                customColorButton.setEnabled(false);
                RGB returnColor = colorDialog.open();
                buttonMatrixGroup.setEnabled(true);
                customColorButton.setEnabled(true);
                if (returnColor != null) {
                    setColorValue(returnColor);
                    fireColorChangeEvent();
                }
            }

            public void widgetDefaultSelected(SelectionEvent event) {
            }
        });
        return customColorButton;
    }

    /**
     * Creates the Gempak Color Matrix of buttons for color selection.
     * 
     * @param horizontalLayout
     * @param horizontalMatrix
     * @param colorButtonHeight
     * @param colorButtonWidth
     * @param firstSpace
     * @param interSpace
     * @param colorButtonSpace
     * @return
     */
    private Group createGempakColorMatrix(final boolean horizontalLayout,
            final boolean horizontalMatrix, final int colorButtonHeight,
            final int colorButtonWidth, final int firstSpace,
            final int interSpace, final int colorButtonSpace) {
        final Group buttonMatrixGroup = new Group(parentComposite, SWT.NONE);

        FormData formData2 = new FormData();
        if (horizontalLayout) {
            formData2.top = new FormAttachment(5, 0);
            formData2.left = new FormAttachment(firstSpace, 0);
        } else {
            formData2.top = new FormAttachment(selectedColorCanvas, interSpace);
            formData2.left = new FormAttachment(selectedColorCanvas, 0,
                    SWT.CENTER);
        }
        buttonMatrixGroup.setLayoutData(formData2);

        GridLayout matrixLayout = new GridLayout();
        matrixLayout.numColumns = horizontalMatrix ? 8 : 4;
        matrixLayout.horizontalSpacing = colorButtonSpace;
        matrixLayout.verticalSpacing = colorButtonSpace;
        buttonMatrixGroup.setLayout(matrixLayout);

        for (GempakColor gColor : GempakColor.values()) {
            Button colorButton = new Button(buttonMatrixGroup, SWT.TOGGLE);
            RGBGempakColorMap.put(gColor.getRGB(), gColor);
            colorButtonMap.put(gColor, colorButton);
            GridData gridData = new GridData();
            gridData.heightHint = colorButtonHeight;
            gridData.widthHint = colorButtonWidth;
            colorButton.setLayoutData(gridData);
            colorButton.setBackground(
                    new Color(parentComposite.getDisplay(), gColor.getRGB()));
            colorButton.setToolTipText(gColor.toString().toLowerCase());
            colorButton.addSelectionListener(new SelectionListener() {
                public void widgetSelected(SelectionEvent event) {
                    Button colorButton = (Button) event.widget;
                    setColorValue(colorButton.getBackground().getRGB());
                    setAlphaValue(new Integer(255));
                    fireColorChangeEvent();
                }

                public void widgetDefaultSelected(SelectionEvent event) {
                }
            });
            colorButton.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent event) {
                    Button colorButton = (Button) event.widget;
                    Color color = colorButton.getBackground();
                    color.dispose();
                }
            });
        }
        return buttonMatrixGroup;
    }

    /**
     * Creates the SelectedColor canvas that can be displayed if Height and
     * Width are greater than 0.
     * 
     * @param horizontalLayout
     * @param selectedColorCanvasHeight
     * @param selectedColorCanvasWidth
     * @param firstSpace
     */
    private void createSelectedColorCanvas(final boolean horizontalLayout,
            final int selectedColorCanvasHeight,
            final int selectedColorCanvasWidth, final int firstSpace) {
        FormData formData1 = new FormData(selectedColorCanvasWidth,
                selectedColorCanvasHeight);
        if (horizontalLayout) {
            formData1.top = new FormAttachment(50,
                    -selectedColorCanvasHeight / 2 - 1); // center
            formData1.left = new FormAttachment(firstSpace, 0);
        } else {
            formData1.top = new FormAttachment(firstSpace, 0);
            formData1.left = new FormAttachment(50,
                    -selectedColorCanvasWidth / 2 - 2); // center
        }
        selectedColorCanvas.setLayoutData(formData1);
        // changes the color of the demo area when a color change occurs.
        selectedColorCanvas.addPaintListener(new PaintListener() {
            public void paintControl(PaintEvent event) {
                Canvas canvas = (Canvas) event.widget;

                if (currentColor.getAlpha() != 0) {
                    canvas.setBackground(currentColor);
                }
                if (currentColor.getAlpha() == 0) {
                    final Pattern pattern = assembleAlphaPattern(canvas);

                    Rectangle r = canvas.getClientArea();
                    GC gc = event.gc;
                    gc.setBackgroundPattern(pattern);
                    gc.setForegroundPattern(pattern);
                    gc.fillRectangle(r);

                }
            }
        });
        if (selectedColorCanvasWidth == 0 && selectedColorCanvasHeight == 0) {
            selectedColorCanvas.setVisible(false);
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

        // If previous color was a GEMPAK color, make sure its
        // matrix button is popped out (deselected)...

        if (!currentRGB.equals(oldRGB)) {
            GempakColor oldGempakColor = RGBGempakColorMap.get(oldRGB);
            if (oldGempakColor != null) {
                colorButtonMap.get(oldGempakColor).setSelection(false);
            }
        }

        // ...and if the new color is a GEMPAK color, make sure its
        // button is pushed. (Note we need to do this even if the
        // selection came from a button push; if same as previous color,
        // SWT.TOGGLE button will pop out if it was already in.)

        GempakColor newGempakColor = RGBGempakColorMap.get(currentRGB);
        if (newGempakColor != null) {
            colorButtonMap.get(newGempakColor).setSelection(true);
        }

        // Update the Color from new RGB; tell canvas to update

        if (currentColor != null) {
            currentColor.dispose();
        }
        currentColor = new Color(parentComposite.getDisplay(), currentRGB,
                currentAlpha);
        selectedColorCanvas.redraw();
        selectedColorCanvas.update();
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
    public void setAlphaValue(Integer newAlpha) {
        oldAlpha = currentAlpha;
        currentAlpha = newAlpha;

        if (activeAlpha) {
            // determine which value is prominent
            if (currentAlpha == 0) {
                // if incoming value is 0, then make the "No Color" button
                // appear selected
                noColorButton.setSelection(true);
            } else if (currentAlpha == 255) {
                // if the incoming value is 255, then make the "No Color" button
                // appear unselected
                // the color buttons will be handled by setColorValue
                noColorButton.setSelection(false);
            } else {
                noColorButton.setSelection(false);
            }
        }

        if (currentColor != null) {
            currentColor.dispose();
        }
        currentColor = new Color(parentComposite.getDisplay(), currentRGB,
                currentAlpha);
        selectedColorCanvas.redraw();
        selectedColorCanvas.update();
    }

    public Integer getAlphaValue() {
        return currentAlpha;
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
        addListenerObject(listener);
    }

    /**
     * Removes the given listener from this <code>ColorMatrixSelector</code>.
     * Has no effect if the listener is not registered.
     * 
     * @param listener
     *            A property change listener
     */
    public void removeListener(IPropertyChangeListener listener) {
        removeListenerObject(listener);
    }

    protected void fireColorChangeEvent()
    // Notify all registered listeners that a color change has occurred.
    {
        if (isListenerAttached() && !currentRGB.equals(oldRGB)) {
            PropertyChangeEvent colorChangeEvent = new PropertyChangeEvent(this,
                    PROP_COLORCHANGE, oldRGB, currentRGB);
            for (Object ls : getListeners()) {
                IPropertyChangeListener listener = (IPropertyChangeListener) ls;
                listener.propertyChange(colorChangeEvent);
            }
        }
    }

    protected void fireAlphaChangeEvent()
    // Notify all registered listeners that an alpha change has occurred.
    {
        if (isListenerAttached() && !currentAlpha.equals(oldAlpha)) {
            PropertyChangeEvent colorChangeEvent = new PropertyChangeEvent(this,
                    PROP_ALPHACHANGE, oldAlpha, currentAlpha);
            for (Object ls : getListeners()) {
                IPropertyChangeListener listener = (IPropertyChangeListener) ls;
                listener.propertyChange(colorChangeEvent);
            }
        }
    }

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
}
