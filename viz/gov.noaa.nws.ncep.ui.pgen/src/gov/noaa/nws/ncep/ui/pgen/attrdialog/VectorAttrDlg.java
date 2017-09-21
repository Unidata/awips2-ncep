/*
 * gov.noaa.nws.ncep.ui.pgen.attrDialog.VectorAttrDlg
 * 
 * 14 May 2009
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.ui.pgen.attrdialog;

import java.awt.Color;
import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.exception.VizException;
import com.vividsolutions.jts.geom.Coordinate;

import gov.noaa.nws.ncep.ui.pgen.PgenSession;
import gov.noaa.nws.ncep.ui.pgen.display.ArrowHead;
import gov.noaa.nws.ncep.ui.pgen.display.IAttribute;
import gov.noaa.nws.ncep.ui.pgen.display.IVector;
import gov.noaa.nws.ncep.ui.pgen.elements.Vector;
import gov.noaa.nws.ncep.viz.common.ui.color.ColorButtonSelector;

/**
 * Singleton attribute dialog for text.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ---------   ----------- --------------------------
 * 05/09        #111        J. Wu       Initial Creation.
 * 09/09        #149        B. Yin      Added check boxes for multi-selection
 * 03/10        #231        Archana     Altered the dialog for the vector attribute
 *                                      to show only a button showing the 
 *                                      selected color instead of displaying 
 *                                      the complete color matrix.
 * 04/11        #?          B. Yin      Re-factor IAttribute
 * 03/13        #928        B. Yin      Added a separator above the button bar.
 * 08/15        R8188       J. Lopez    Changed rotation of Hash Mark to match legacy
 * 09/29/2015   R1283       J. Wu       Fix direction-change when moving hash marks.
 * 03/03/2016   #13557      J. Beck     Set keyboard focus to wind speed text field for Wind Barb and Wind Arrow
 * 11/07/2016   R23252      S. Russell  Added method getArrowHeadType()
 * 
 * </pre>
 * 
 * @author J. Wu
 */

public class VectorAttrDlg extends AttrDlg implements IVector {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(VectorAttrDlg.class);

    private static final String HASH = "Hash";

    private static final String BARB = "Barb";

    private static final String DIRECTIONAL = "Directional";

    private static final String ARROW = "Arrow";

    protected static enum Attributes {
        COLOR, CLEAR, DIRECTION, SPEED, SIZE, WIDTH, HEADSIZE
    };

    private static VectorAttrDlg INSTANCE = null;

    private Composite top = null;

    // Vector drawing tool attributes

    // Color attribute is a single color block that activates a colorSelector
    // when clicked
    private Label colorLabel;

    private ColorButtonSelector colorSelector = null;

    // A single radio button is used for the clear attribute.
    private Label clearLabel;

    private Button clearButton1 = null;

    private Button clearButton2 = null;

    // The direction attribute has 3 components.
    protected Label directionLabel;

    protected Slider directionSlider = null;

    protected Text directionText = null;

    // Speed attribute.
    protected Label speedLabel = null;

    protected Slider speedSlider = null;

    protected Text speedText = null;

    // Size attribute.
    protected Label sizeLabel;

    protected Slider sizeSlider = null;

    protected Text sizeText = null;

    // Width attribute.
    protected Label widthLabel;

    protected Slider widthSlider = null;

    protected Text widthText = null;

    // The arrowhead attribute.
    protected Label arrowheadSizeLabel = null;

    protected Slider arrowheadSizeSlider = null;

    protected Text arrowheadSizeText = null;

    protected ArrowHead.ArrowHeadType arrowHeadType = ArrowHead.ArrowHeadType.FILLED;

    // Check boxes are for instances when multi-selection is used.
    // These check boxes appear on the leftmost column of the dialog, one check
    // box for each attribute.
    // Not all dialogs and tools use the check boxes, but some do.
    private Button checkBox[];

    /**
     * Protected constructor
     * 
     * @param parShell
     * @throws VizException
     */
    protected VectorAttrDlg(Shell parShell) throws VizException {
        super(parShell);
    }

    /**
     * Gets an attribute dialog instance.
     * 
     * @param parShell
     * @return a single global point of access to VectorAttrDlg
     */
    public static VectorAttrDlg getInstance(Shell parShell) {
        if (INSTANCE == null) {
            try {

                INSTANCE = new VectorAttrDlg(parShell);

            } catch (VizException e) {
                e.printStackTrace();
            }
        }
        return INSTANCE;
    }

    /*
     * (non-Javadoc) Creates the dialog area.
     * 
     * @see
     * org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets
     * .Composite)
     */
    @Override
    public Control createDialogArea(Composite parent) {

        top = (Composite) super.createDialogArea(parent);

        // Create the main layout for the shell.
        GridLayout mainLayout = new GridLayout(3, false);
        mainLayout.marginHeight = 3;
        mainLayout.marginWidth = 3;
        top.setLayout(mainLayout);
        // Initialize all of the menus, controls, and layouts
        initializeComponents();

        return top;

    }

    /**
     * Creates buttons, menus, and other controls in the dialog area.
     */
    private void initializeComponents() {

        this.getShell().setText("Vector Attributes");
        checkBox = new Button[7];

        createColorAttr();
        createClearAttr();
        createDirectionAttr();
        createSpeedAttr();
        createSizeAttr();
        createWidthAttr();
        createHeadSizeAttr();

        addSeparator(top.getParent());

    }

    /*
     * (non-Javadoc) Return color from the color picker dialog.
     * 
     * @see gov.noaa.nws.ncep.ui.pgen.attrdialog.AttrDlg#getColors()
     */

    public Color[] getColors() {
        if (checkBox[Attributes.COLOR.ordinal()].getSelection()) {
            // IAttribute requires to return an array of colors
            // Only the first color is used at this time.
            Color[] colors = new Color[1];

            colors[0] = new java.awt.Color(colorSelector.getColorValue().red,
                    colorSelector.getColorValue().green,
                    colorSelector.getColorValue().blue);

            return colors;
        } else {
            return null;
        }
    }

    /**
     * Sets the color of the color picker of the dialog.
     * 
     * @param clr
     */
    private void setColor(Color clr) {

        colorSelector.setColorValue(
                new RGB(clr.getRed(), clr.getGreen(), clr.getBlue()));
    }

    /**
     * Gets the vector type.
     * 
     * @return type of vector
     */
    public VectorType getVectorType() {
        return null;
    }

    /**
     * Gets the wind direction. North is considered 0 degrees and direction
     * increases clockwise.
     * 
     * @return the direction from which the wind is blowing
     */
    public double getDirection() {
        if (checkBox[Attributes.DIRECTION.ordinal()].getSelection()) {
            return (double) directionSlider.getSelection();
        } else {
            return java.lang.Double.NaN;
        }
    }

    /**
     * Gets the wind speed.
     * 
     * @return wind speed
     */
    public double getSpeed() {
        if (checkBox[Attributes.SPEED.ordinal()].getSelection()) {

            Double speed = Double.valueOf(0);

            /*
             * if speedText field is empty or not well formed, an exception will
             * be thrown. We will catch it and set the speed of zero KPH
             */
            try {
                speed = Double.valueOf(speedText.getText());
            } catch (NumberFormatException e) {

                // speed is already set to 0 KPH, set the Text field when input
                // cannot be parsed, we force the dialog to display a zero
                speedText.setText("0");
            }

            return speed;

        } else {
            return java.lang.Double.NaN;
        }
    }

    /**
     * Gets the line width of the vector object.
     * 
     * @return line width
     */
    public float getLineWidth() {
        if (checkBox[Attributes.WIDTH.ordinal()].getSelection()) {
            return (float) widthSlider.getSelection();
        } else {
            return java.lang.Float.NaN;
        }
    }

    /**
     * Gets the size scale factor for the object.
     * 
     * @return size scale factor
     */
    public double getSizeScale() {
        if (checkBox[Attributes.SIZE.ordinal()].getSelection()) {
            return (double) (sizeSlider.getSelection() / 10.0);
        } else {
            return java.lang.Double.NaN;
        }
    }

    /**
     * Gets the size scale for the arrow head.
     * 
     * @return arrow head size
     */
    public double getArrowHeadSize() {
        if (checkBox[Attributes.HEADSIZE.ordinal()].getSelection()) {
            return (double) (arrowheadSizeSlider.getSelection() / 10.0);
        } else {
            return java.lang.Double.NaN;
        }
    }

    /**
     * Gets boolean flag indicating whether an arrow has speed and direction, or
     * direction only.
     * 
     * @return direction only flag
     */
    public boolean hasDirectionOnly() {
        return false;
    }

    /**
     * Checks whether the background of the object should be cleared.
     * 
     * @return true if background should be cleared, null otherwise
     */
    public Boolean isClear() {
        if (checkBox[Attributes.CLEAR.ordinal()].getSelection()) {
            return clearButton1.getSelection();
        } else {
            return null;
        }
    }

    /**
     * Checks whether the background of the object should be cleared.
     * 
     * @return true, if background should be cleared
     */
    public Boolean hasBackgroundMask() {
        return isClear();
    }

    /**
     * Sets the wind speed.
     */
    public void setSpeed(double spd) {
        speedSlider.setSelection((int) spd);
        speedText.setText("" + (int) spd);
    }

    /**
     * Sets the wind direction.
     */
    public void setDirection(double dir) {
        directionSlider.setSelection((int) dir);
        directionText.setText("" + (int) dir);
    }

    /**
     * Sets the line width of the vector object.
     * 
     */
    public void setLineWidth(float width) {
        widthSlider.setSelection((int) width);
        widthText.setText("" + width);
    }

    /**
     * Sets the size scale factor for the object.
     * 
     */
    public void setSizeScale(double size) {
        sizeSlider.setSelection((int) (size * 10));
        sizeText.setText("" + size);

    }

    /**
     * Sets the size scale for the arrow head.
     */
    public void setArrowHeadSize(double ahs) {
        arrowheadSizeSlider.setSelection((int) (ahs * 10));
        arrowheadSizeText.setText("" + ahs);
    }

    /**
     * Sets whether the background of the object should be cleared.
     */
    public void setClear(boolean clr) {
        clearButton1.setSelection(clr);
    }

    /**
     * Gets a Map of all attributes of the dialog.
     */
    public HashMap<String, Object> getAttrFromDlg() {

        HashMap<String, Object> attr = new HashMap<String, Object>();

        attr.put("speed", this.getSpeed());
        attr.put("direction", this.getDirection());
        attr.put("arrowHeadSize", this.getArrowHeadSize());
        attr.put("sizeScale", this.getSizeScale());
        attr.put("lineWidth", this.getLineWidth());
        attr.put("clear", this.isClear());
        attr.put("color", this.getColor());

        return attr;
    }

    /**
     * Sets values of all attributes of the dialog.
     */
    public void setAttrForDlg(IAttribute iattr) {
        if (iattr instanceof IVector) {
            IVector attr = (IVector) iattr;
            adjustAttrForDlg(((Vector) attr).getPgenType());

            Color clr = attr.getColors()[0];
            if (clr != null)
                this.setColor(clr);

            this.setSpeed(attr.getSpeed());
            this.setClear(attr.isClear());
            this.setDirection(attr.getDirection());
            this.setSizeScale(attr.getSizeScale());
            this.setLineWidth(attr.getLineWidth());
            this.setArrowHeadSize(attr.getArrowHeadSize());
        }
    }

    /**
     * Sets values for all attributes of the dialog, based on the type of
     * vector.
     */
    public void adjustAttrForDlg(String pgenType) {

        if (pgenType.equalsIgnoreCase(BARB)) {

            this.getShell().setText("Wind Barb Attributes");

            setAttributeComponentsEnabled(arrowheadSizeLabel, arrowheadSizeText,
                    arrowheadSizeSlider, false);
            setAttributeComponentsEnabled(speedLabel, speedText, speedSlider,
                    true);

            speedSlider.setValues(100, 0, 405, 5, 5, 5);
            speedText.setText("100");

        } else if (pgenType.equalsIgnoreCase(HASH)) {

            this.getShell().setText("Hash Attributes");

            setAttributeComponentsEnabled(arrowheadSizeLabel, arrowheadSizeText,
                    arrowheadSizeSlider, false);
            setAttributeComponentsEnabled(speedLabel, speedText, speedSlider,
                    false);

        } else if (pgenType.equalsIgnoreCase(DIRECTIONAL)) {

            this.getShell().setText("Directional Arrow Attributes");

            setAttributeComponentsEnabled(speedLabel, speedText, speedSlider,
                    false);

        } else if (pgenType.equalsIgnoreCase(ARROW)) {

            // it must be a wind arrow at this point
            this.getShell().setText("Wind Arrow Attributes");

            setAttributeComponentsEnabled(arrowheadSizeLabel, arrowheadSizeText,
                    arrowheadSizeSlider, true);
            setAttributeComponentsEnabled(speedLabel, speedText, speedSlider,
                    true);

            speedSlider.setValues(10, 0, 401, 1, 1, 1);

        } else {

            statusHandler.info("Unhandled Vector Type...");
        }
    }

    /**
     * Checks whether or not the wind speed Text field is enabled. If enabled,
     * we are drawing a vector that has a speed attribute (wind barb or wind
     * arrow).
     * 
     * @return true if enabled, false otherwise
     */
    public boolean isSpeedTextEnabled() {
        return speedText.isEnabled();
    }

    /**
     * Sets focus on the wind speed Text field.
     * 
     * Call this to keep keyboard focus on the speed text field. The user can
     * type a speed value without mouse movement or clicking inside the Text
     * field. The focus stays at the end of the text field so the user may
     * easily edit the value, including using backspace key.
     * 
     * @param dialog
     *            the dialog box containing the speed text field
     */
    public void setSpeedTextFocus(AttrDlg dialog) {

        if (dialog != null && dialog instanceof VectorAttrDlg
                && !speedText.isDisposed()) {

            speedText.setFocus();
            speedText.setSelection(speedText.getText().length());
        }
    }

    /*
     * Create UI widgets and handlers for each of the attributes in the the
     * dialog box: Size, Line width, Arrowhead size, Color, Clear, Direction,
     * and Speed.
     */

    /**
     * Create widgets for the Size attribute.
     */
    private void createSizeAttr() {
        checkBox[Attributes.SIZE.ordinal()] = new Button(top, SWT.CHECK);
        checkBox[Attributes.SIZE.ordinal()]
                .setLayoutData(new GridData(CHK_WIDTH, CHK_HEIGHT));

        checkBox[Attributes.SIZE.ordinal()]
                .addSelectionListener(new SelectionListener() {

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {
                        // must implement all methods in super class
                    }

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        Button btn = (Button) e.widget;
                        if (btn.getSelection()) {
                            setAttributeComponentsEnabled(sizeLabel, sizeText,
                                    sizeSlider, true);
                        } else {
                            setAttributeComponentsEnabled(sizeLabel, sizeText,
                                    sizeSlider, false);
                        }
                    }

                });

        sizeLabel = new Label(top, SWT.LEFT);
        sizeLabel.setText("Size:");
        GridLayout gl = new GridLayout(2, false);

        Group sizeGroup = new Group(top, SWT.NONE);
        sizeGroup.setLayout(gl);

        sizeSlider = new Slider(sizeGroup, SWT.HORIZONTAL);
        sizeSlider.setValues(10, 1, 101, 1, 1, 1);
        sizeSlider.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                sizeText.setText("" + sizeSlider.getSelection() / 10.0);
            }
        });

        sizeText = new Text(sizeGroup, SWT.SINGLE | SWT.BORDER);
        sizeText.setLayoutData(new GridData(25, 10));
        sizeText.setEditable(true);
        sizeText.setText("1.0");
        sizeText.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                double value = 0;
                try {
                    value = Double.parseDouble(sizeText.getText());
                    if (value >= 0.1 && value < 10.0) {
                        sizeSlider.setSelection((int) (value * 10));
                        sizeText.setToolTipText("");
                    } else {
                        sizeText.setToolTipText(
                                "Only values between 0.1 and 10.0 are accepted.");
                    }
                } catch (NumberFormatException e1) {
                    sizeText.setToolTipText(
                            "Only values between 0.1 and 10.0 are accepted.");
                }
            }
        });
    }

    /**
     * Create widgets for the line width attribute.
     */
    private void createWidthAttr() {
        checkBox[Attributes.WIDTH.ordinal()] = new Button(top, SWT.CHECK);
        checkBox[Attributes.WIDTH.ordinal()]
                .setLayoutData(new GridData(CHK_WIDTH, CHK_HEIGHT));

        checkBox[Attributes.WIDTH.ordinal()]
                .addSelectionListener(new SelectionListener() {

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {
                        // must implement all methods in super class
                    }

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        Button btn = (Button) e.widget;
                        if (btn.getSelection()) {
                            setAttributeComponentsEnabled(widthLabel, widthText,
                                    widthSlider, true);
                        } else {
                            setAttributeComponentsEnabled(widthLabel, widthText,
                                    widthSlider, false);
                        }
                    }

                });

        widthLabel = new Label(top, SWT.LEFT);
        widthLabel.setText("Width:");

        GridLayout gl = new GridLayout(2, false);

        Group widthGroup = new Group(top, SWT.NONE);
        widthGroup.setLayout(gl);

        widthSlider = new Slider(widthGroup, SWT.HORIZONTAL);
        widthSlider.setValues(2, 1, 11, 1, 1, 1);
        widthSlider.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                widthText.setText("" + widthSlider.getSelection());
            }
        });

        widthText = new Text(widthGroup, SWT.SINGLE | SWT.BORDER);
        widthText.setLayoutData(new GridData(25, 10));
        widthText.setEditable(true);
        widthText.setText("2");
        widthText.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                int value = 0;
                try {
                    value = Integer.parseInt(widthText.getText());
                    if (value >= 1 && value < 11) {
                        widthSlider.setSelection(value);
                        widthText.setToolTipText("");
                    } else {
                        widthText.setToolTipText(
                                "Only values between 1.0 and 10.0 are accepted.");
                    }
                } catch (NumberFormatException e1) {
                    widthText.setToolTipText(
                            "Only values between 1.0 and 10.0 are accepted.");
                }
            }
        });
    }

    /**
     * Create widgets for the arrowhead size attribute.
     */
    private void createHeadSizeAttr() {
        checkBox[Attributes.HEADSIZE.ordinal()] = new Button(top, SWT.CHECK);
        checkBox[Attributes.HEADSIZE.ordinal()]
                .setLayoutData(new GridData(CHK_WIDTH, CHK_HEIGHT));

        checkBox[Attributes.HEADSIZE.ordinal()]
                .addSelectionListener(new SelectionListener() {

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {
                        // must implement all methods in super class
                    }

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        Button btn = (Button) e.widget;
                        if (btn.getSelection()) {
                            setAttributeComponentsEnabled(arrowheadSizeLabel,
                                    arrowheadSizeText, arrowheadSizeSlider,
                                    true);
                        } else {
                            setAttributeComponentsEnabled(arrowheadSizeLabel,
                                    arrowheadSizeText, arrowheadSizeSlider,
                                    false);
                        }
                    }

                });

        arrowheadSizeLabel = new Label(top, SWT.LEFT);
        arrowheadSizeLabel.setText("Head Size:");
        GridLayout gl = new GridLayout(2, false);

        Group arwHeadSizeGroup = new Group(top, SWT.NONE);
        arwHeadSizeGroup.setLayout(gl);

        arrowheadSizeSlider = new Slider(arwHeadSizeGroup, SWT.HORIZONTAL);
        arrowheadSizeSlider.setValues(10, 1, 101, 1, 1, 1);
        arrowheadSizeSlider.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                arrowheadSizeText.setText(
                        "" + arrowheadSizeSlider.getSelection() / 10.0);
            }
        });

        arrowheadSizeText = new Text(arwHeadSizeGroup, SWT.SINGLE | SWT.BORDER);
        arrowheadSizeText.setLayoutData(new GridData(25, 10));
        arrowheadSizeText.setEditable(true);
        arrowheadSizeText.setText("1.0");
        arrowheadSizeText.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                double value = 0;
                try {
                    value = Double.parseDouble(arrowheadSizeText.getText());
                    if (value >= 0.1 && value < 10.0) {
                        arrowheadSizeSlider.setSelection((int) (value * 10));
                        arrowheadSizeText.setToolTipText("");
                    } else {
                        arrowheadSizeText.setToolTipText(
                                "Only values between 0.1 and 10.0 are accepted.");
                    }
                } catch (NumberFormatException e1) {
                    arrowheadSizeText.setToolTipText(
                            "Only values between 0.1 and 10.0 are accepted.");
                }
            }
        });

    }

    /**
     * Create widgets for the Color attribute.
     */
    private void createColorAttr() {
        checkBox[Attributes.COLOR.ordinal()] = new Button(top, SWT.CHECK);
        checkBox[Attributes.COLOR.ordinal()]
                .setLayoutData(new GridData(CHK_WIDTH, CHK_HEIGHT));
        checkBox[Attributes.COLOR.ordinal()]
                .addSelectionListener(new SelectionListener() {

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {
                        // must implement all methods in super class
                    }

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        Button btn = (Button) e.widget;
                        if (btn.getSelection()) {
                            colorLabel.setEnabled(true);
                        } else {
                            colorLabel.setEnabled(false);
                        }
                    }

                });

        colorLabel = new Label(top, SWT.LEFT);
        colorLabel.setText("Color:");

        colorSelector = new ColorButtonSelector(top);
        colorSelector.setColorValue(new RGB(0, 255, 0));
    }

    /**
     * Create widgets for the Clear attribute.
     */
    private void createClearAttr() {
        checkBox[Attributes.CLEAR.ordinal()] = new Button(top, SWT.CHECK);
        checkBox[Attributes.CLEAR.ordinal()]
                .setLayoutData(new GridData(CHK_WIDTH, CHK_HEIGHT));

        checkBox[Attributes.CLEAR.ordinal()]
                .addSelectionListener(new SelectionListener() {

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {
                    }

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        Button btn = (Button) e.widget;
                        if (btn.getSelection()) {
                            clearLabel.setEnabled(true);
                            clearButton1.setEnabled(true);
                            clearButton2.setEnabled(true);
                        } else {
                            clearLabel.setEnabled(false);
                            clearButton1.setEnabled(false);
                            clearButton2.setEnabled(false);
                        }
                    }

                });

        clearLabel = new Label(top, SWT.LEFT);
        clearLabel.setText("Clear:");

        Group clearGroup = new Group(top, SWT.NONE);
        GridLayout gl = new GridLayout(2, false);
        clearGroup.setLayout(gl);

        clearButton1 = new Button(clearGroup, SWT.RADIO);
        clearButton1.setText("On");
        clearButton1.setSelection(true);

        clearButton2 = new Button(clearGroup, SWT.RADIO);
        clearButton2.setText("Off");
    }

    /**
     * Create widgets for the Direction attribute.
     */
    private void createDirectionAttr() {
        checkBox[Attributes.DIRECTION.ordinal()] = new Button(top, SWT.CHECK);
        checkBox[Attributes.DIRECTION.ordinal()]
                .setLayoutData(new GridData(CHK_WIDTH, CHK_HEIGHT));

        checkBox[Attributes.DIRECTION.ordinal()]
                .addSelectionListener(new SelectionListener() {

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {
                    }

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        Button btn = (Button) e.widget;
                        if (btn.getSelection()) {
                            setAttributeComponentsEnabled(directionLabel,
                                    directionText, directionSlider, true);

                        } else {
                            setAttributeComponentsEnabled(directionLabel,
                                    directionText, directionSlider, false);

                        }
                    }

                });

        directionLabel = new Label(top, SWT.LEFT);
        directionLabel.setText("Direction:");
        GridLayout gl = new GridLayout(2, false);

        Group dirGroup = new Group(top, SWT.NONE);
        dirGroup.setLayout(gl);

        directionSlider = new Slider(dirGroup, SWT.HORIZONTAL);
        directionSlider.setValues(360, 0, 365, 5, 5, 5);
        directionSlider.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                directionText.setText("" + directionSlider.getSelection());
            }
        });

        directionText = new Text(dirGroup, SWT.SINGLE | SWT.BORDER);
        directionText.setLayoutData(new GridData(25, 10));
        directionText.setEditable(true);
        directionText.setText("360");
        directionText.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                int value = 0;
                try {
                    value = Integer.parseInt(directionText.getText());
                    if (value >= 0 && value < 361) {
                        directionSlider.setSelection(value / 5 * 5);
                        directionText.setToolTipText("");
                    } else {
                        directionText.setToolTipText(
                                "Only integer values between 0 and 360 are accepted.");
                    }
                } catch (NumberFormatException e1) {
                    directionText.setToolTipText(
                            "Only integer values between 0 and 360 are accepted.");
                }
            }
        });

    }

    /**
     * Create widgets for the speed attribute.
     */
    private void createSpeedAttr() {
        checkBox[Attributes.SPEED.ordinal()] = new Button(top, SWT.CHECK);
        checkBox[Attributes.SPEED.ordinal()]
                .setLayoutData(new GridData(CHK_WIDTH, CHK_HEIGHT));

        checkBox[Attributes.SPEED.ordinal()]
                .addSelectionListener(new SelectionListener() {

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {
                    }

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        Button btn = (Button) e.widget;
                        if (btn.getSelection()) {
                            setAttributeComponentsEnabled(speedLabel, speedText,
                                    speedSlider, true);

                        } else {
                            setAttributeComponentsEnabled(speedLabel, speedText,
                                    speedSlider, false);
                        }
                    }

                });

        speedLabel = new Label(top, SWT.LEFT);
        speedLabel.setText("Speed:");
        GridLayout gl = new GridLayout(2, false);

        Group spdGroup = new Group(top, SWT.NONE);
        spdGroup.setLayout(gl);

        speedSlider = new Slider(spdGroup, SWT.HORIZONTAL);
        speedSlider.setValues(10, 0, 401, 1, 1, 1);
        speedSlider.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                int thumb = speedSlider.getThumb();
                speedText.setText(
                        "" + (speedSlider.getSelection() / thumb) * thumb);
            }
        });

        speedText = new Text(spdGroup, SWT.SINGLE | SWT.BORDER);
        speedText.setLayoutData(new GridData(25, 10));
        speedText.setEditable(true);
        speedText.setText("10");
        speedText.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                int value = 0;
                try {
                    value = Integer.parseInt(speedText.getText());
                    if (value >= 0 && value < 401) {
                        speedSlider.setSelection(value);
                        speedText.setToolTipText("");
                    } else {
                        speedText.setToolTipText(
                                "Only integer values between 0 and 400 are accepted.");
                    }
                } catch (NumberFormatException e1) {
                    speedText.setToolTipText(
                            "Only integer values between 0 and 400 are accepted.");
                }
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see gov.noaa.nws.ncep.ui.pgen.attrdialog.AttrDlg#open()
     */
    @Override
    public int open() {

        this.create();

        if (PgenSession.getInstance().getPgenPalette().getCurrentAction()
                .equalsIgnoreCase("MultiSelect")) {
            enableChkBoxes(true);
            enableAllWidgets(false);
        } else {
            enableChkBoxes(false);
        }

        int rt = super.open();
        Point shellSizeInPoint = this.getShell().getSize();
        shellSizeInPoint.x += 20;
        this.getShell().setSize(shellSizeInPoint);
        return rt;
    }

    /**
     * Sets checkBox[] selected and visible attributes.
     * 
     * @param flag
     *            - used to set selected and visible attributes
     */
    private void enableChkBoxes(boolean flag) {

        if (!flag) {

            // Set all multi-selection check boxes to true
            for (Attributes chk : Attributes.values()) {
                checkBox[chk.ordinal()].setSelection(true);
            }
        }

        // Set all multi-selection check boxes to visible
        for (Attributes chk : Attributes.values()) {
            checkBox[chk.ordinal()].setVisible(flag);
        }

    }

    /**
     * Enables/disables all widgets in the dialog.
     * 
     * @param flag
     *            - enables widget attributes if true
     */
    private void enableAllWidgets(boolean flag) {

        colorLabel.setEnabled(flag);

        clearLabel.setEnabled(flag);
        clearButton1.setEnabled(flag);
        clearButton2.setEnabled(flag);

        setAttributeComponentsEnabled(widthLabel, widthText, widthSlider, flag);
        setAttributeComponentsEnabled(widthLabel, widthText, widthSlider, flag);
        setAttributeComponentsEnabled(sizeLabel, sizeText, sizeSlider, flag);
        setAttributeComponentsEnabled(directionLabel, directionText,
                directionSlider, flag);
        setAttributeComponentsEnabled(speedLabel, speedText, speedSlider, flag);
        setAttributeComponentsEnabled(arrowheadSizeLabel, arrowheadSizeText,
                arrowheadSizeSlider, flag);

    }

    /**
     * Enable or disable widgets (Label, Text, Slider) that make up an
     * attribute.
     * 
     * @param label
     *            - the attribute Label
     * @param text
     *            - the attribute Text
     * @param slider
     *            - the attribute Slider
     * @param enable
     *            - true to enable, false to disable
     */
    protected void setAttributeComponentsEnabled(Label label, Text text,
            Slider slider, Boolean enable) {

        if (enable == true) {

            label.setEnabled(true);
            text.setEnabled(true);
            slider.setEnabled(true);

        } else {

            label.setEnabled(false);
            text.setEnabled(false);
            slider.setEnabled(false);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see gov.noaa.nws.ncep.ui.pgen.display.ISinglePoint#getLocation()
     */
    @Override
    public Coordinate getLocation() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see gov.noaa.nws.ncep.ui.pgen.display.IVector#getColor()
     */
    @Override
    public Color getColor() {
        return this.getColors()[0];
    }

    /*
     * Gets an enum indicating whether the arrow head should be OPEN or FILLED
     * for Vector arrows.
     * 
     * @return enum indicating the arrow head should be OPEN or FILLED.
     */
    public ArrowHead.ArrowHeadType getArrowHeadType() {
        return this.arrowHeadType;

    }

}
