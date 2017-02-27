/**
 * 
 */
package gov.noaa.nws.ncep.viz.common.ui.color;

import org.eclipse.core.commands.common.EventManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Pattern;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * The <code>ColorButtonSelector</code> can be used in place of JFace
 * <code>ColorSelector</code> to provide a familiar GEMPAK color matrix palette,
 * in addition to a path to the standard OS specific SWT ColorMatrixDialog.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 07 May 2009  74B         B. Hebbard  Initial implementation.
 * 11/25/2016   R21762      P. Moyer    Implemented alpha transparency/"No Color" option
 * </pre>
 * 
 * @author bhebbard
 * @version 1
 */
public class ColorButtonSelector extends EventManager {

    // The currently selected color, as SWT RGB object
    private RGB currentRGB = null;

    // The previously selected color (if any)
    private RGB oldRGB = null;

    // The currently selected Alpha, as Integer
    private Integer currentAlpha = null;

    // The previously selected Alpha (if any)
    private Integer oldAlpha = null;

    // The currently selected color as SWT Color object (requires disposal care)
    private Color currentColor = null;

    // Property name to use in color change event
    public static final String PROP_COLORCHANGE = "colorValue"; //$NON-NLS-1$

    // Property name to use in color change event
    public static final String PROP_ALPHACHANGE = "alphaValue"; //$NON-NLS-1$

    // Composite containing selector
    private final Composite composite;

    // The color button itself
    private Button colorButton = null;

    // flag to enable/disable No Color (Alpha = 0) option
    private boolean alphaActive = false;

    // image background for Alpha
    private final Image alphaImage;

    // pattern background for Alpha
    private final Pattern alphaPattern;

    /**
     * Constructor: Create a new instance of the receiver in the supplied parent
     * <code>Composite</code>, using a default size and passed-in alpha flag.
     * 
     * @param parent
     *            A parent Composite in which to put the new color button
     *            selector.
     * @param alpha
     *            Enables or disables "No Color" option.
     */
    public ColorButtonSelector(final Composite parent, final boolean alpha) {
        this(parent, 23, 18, alpha); // use a default size
    }

    /**
     * Constructor: Create a new instance of the receiver in the supplied parent
     * <code>Composite</code>, using a default size and no "No Color" option.
     * 
     * @param parent
     *            A parent Composite in which to put the new color button
     *            selector.
     */
    public ColorButtonSelector(final Composite parent) {
        this(parent, 23, 18, false); // use a default size
    }

    /**
     * Constructor: Create a new instance of the receiver in the supplied parent
     * <code>Composite</code> with no "No Color" option.
     * 
     * @param parent
     *            A parent Composite in which to put the new color button
     *            selector.
     * @param colorButtonWidth
     *            Width (pixels) of the button.
     * @param colorButtonHeight
     *            Height (pixels) of the button.
     */
    public ColorButtonSelector(final Composite parent,
            final int colorButtonWidth, final int colorButtonHeight) {
        this(parent, colorButtonWidth, colorButtonHeight, false);
    }

    /**
     * Constructor: Create a new instance of the receiver in the supplied parent
     * <code>Composite</code>.
     * 
     * @param parent
     *            A parent Composite in which to put the new color button
     *            selector.
     * @param colorButtonWidth
     *            Width (pixels) of the button.
     * @param colorButtonHeight
     *            Height (pixels) of the button.
     * @param alpha
     *            Enable or disable "No Color" option.
     */
    public ColorButtonSelector(final Composite parent,
            final int colorButtonWidth, final int colorButtonHeight,
            final boolean alpha) {
        alphaActive = alpha;

        // create the Alpha pattern

        // initial default alpha value
        currentAlpha = new Integer(255);

        composite = new Composite(parent, SWT.NONE);

        FormLayout parentLayout = new FormLayout();
        composite.setLayout(parentLayout);

        colorButton = new Button(composite, SWT.NONE);

        Display display = colorButton.getDisplay();
        // define a pattern on an image
        alphaImage = new Image(display, 20, 20);
        Color grey = display.getSystemColor(SWT.COLOR_GRAY);
        Color white = display.getSystemColor(SWT.COLOR_WHITE);
        GC gcp = new GC(alphaImage);
        gcp.setBackground(white);
        gcp.fillRectangle(0, 0, 20, 20);
        gcp.setBackground(grey);
        gcp.fillRectangle(0, 0, 10, 10);
        gcp.fillRectangle(10, 10, 10, 10);
        gcp.dispose();
        alphaPattern = new Pattern(display, alphaImage);

        FormData formData = new FormData(colorButtonWidth, colorButtonHeight);
        formData.height = colorButtonHeight;
        formData.width = colorButtonWidth;
        colorButton.setLayoutData(formData);

        colorButton.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent event) {
                // modify so that colorMatrixDialog is passed alphaActive
                ColorMatrixDialog colorDialog = new ColorMatrixDialog(
                        composite.getShell(), "Color Palette", alphaActive);
                colorDialog.setColor(currentRGB);
                colorDialog.setAlpha(currentAlpha);
                colorButton.setEnabled(false);
                if (colorDialog.open() != Window.CANCEL) // no explicit OK;
                                                         // assume unless cancel
                {
                    setColorValue(colorDialog.getColor());
                    if (alphaActive) {
                        setAlphaValue(colorDialog.getAlpha());
                    } else {
                        setAlphaValue(255);
                    }
                    fireColorChangeEvent();
                    fireAlphaChangeEvent();
                }
                colorButton.setEnabled(true);
            }

            public void widgetDefaultSelected(SelectionEvent event) {
            }
        });

        // changes the color of the button when the color is changed via event
        // (either from outside or from the internal ColorMatrixDialog)
        colorButton.addPaintListener(new PaintListener() {
            public void paintControl(PaintEvent event) {
                Button button = (Button) event.widget;

                if (currentColor.getAlpha() != 0) {
                    button.setBackground(currentColor);
                }
                if (currentColor.getAlpha() == 0) {
                    button.setBackground(alphaImage.getBackground());
                    event.gc.setBackgroundPattern(alphaPattern);
                    button.setBackgroundImage(alphaImage);

                }

                // prevent button from turning white on mouseover
                event.gc.fillRectangle(2, 2, colorButtonWidth - 4,
                        colorButtonHeight - 4);
            }
        });

    }

    /**
     * Get the button control being wrappered by the selector.
     *
     * @return <code>Button</code>
     */
    public Button getButton() {
        return colorButton;
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

        // Update the Color from new RGB; tell canvas to update

        if (currentColor != null) {
            currentColor.dispose();
        }
        currentColor = new Color(composite.getDisplay(), currentRGB,
                currentAlpha);
        colorButton.redraw();
        colorButton.update();
    }

    public RGB getColorValue() {
        return currentRGB;
    }

    /**
     * Set the current Alpha value and update the control.
     * 
     * @param newRGB
     *            The new color.
     */
    public void setAlphaValue(Integer newAlpha) {
        oldAlpha = currentAlpha;
        currentAlpha = newAlpha;

        // Update the Color from new Alpha; tell canvas to update

        if (currentColor != null) {
            currentColor.dispose();
        }
        currentColor = new Color(composite.getDisplay(), currentRGB,
                currentAlpha);
        colorButton.redraw();
        colorButton.update();
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
        if (isListenerAttached() && (!currentRGB.equals(oldRGB))) {
            PropertyChangeEvent colorChangeEvent = new PropertyChangeEvent(this,
                    PROP_COLORCHANGE, oldRGB, currentRGB);
            for (Object ls : getListeners()) {
                IPropertyChangeListener listener = (IPropertyChangeListener) ls;
                listener.propertyChange(colorChangeEvent);
            }
        }
    }

    protected void fireAlphaChangeEvent()
    // Notify all registered listeners that a color change has occurred.
    {
        if (isListenerAttached() && (!currentAlpha.equals(oldAlpha))) {
            PropertyChangeEvent alphaChangeEvent = new PropertyChangeEvent(this,
                    PROP_ALPHACHANGE, oldAlpha, currentAlpha);
            for (Object ls : getListeners()) {
                IPropertyChangeListener listener = (IPropertyChangeListener) ls;
                listener.propertyChange(alphaChangeEvent);
            }
        }
    }

    public void dispose() {
        if (composite != null) {
            composite.dispose();
        }
    }

    public boolean isDisposed() {
        return composite.isDisposed();
    }
}
