/*
 * gov.noaa.nws.ncep.ui.pgen.attrDialog.TextAttrDlg
 * 
 * 15 April 2009
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.ui.pgen.attrdialog;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;

import com.raytheon.uf.viz.core.exception.VizException;
import org.locationtech.jts.geom.Coordinate;

import gov.noaa.nws.ncep.ui.pgen.PgenConstant;
import gov.noaa.nws.ncep.ui.pgen.PgenSession;
import gov.noaa.nws.ncep.ui.pgen.display.IAttribute;
import gov.noaa.nws.ncep.ui.pgen.display.IText;
import gov.noaa.nws.ncep.viz.common.ui.color.ColorButtonSelector;

/**
 * Singleton attribute dialog for text.
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer        Description
 * ----------------------------------------------------------
 * 04/09                    J. Wu           Initial Creation.
 * 09/09        #149        B. Yin          Added check boxes for multi-selection
 * 03/10        #231        Archana         Altered the dialog for text
 *                                          to display only a button showing the
 *                                          selected color instead of displaying
 *                                          the complete color matrix.
 * 04/11        #?          B. Yin          Re-factor IAttribute
 * 03/13        #928        B. Yin          Added a separator above the button bar.
 * 07/15        R8903       J. Lopez        Creates a pop up if the text field is left blank
 * 08/15        R8553       B. Yin          Remember last text per box type
 * 12/22/2015   R13545      K. Bugenhagen   Remember all attributes per box type
 * 06/30/2016   R17980      B. Yin          Fixed error dialog issue in multi-select mode.
 * 07/27/2016   R17378      J. Wu           Set cursor at end of the text string.
 * 08/20/2019   67218       ksunil          prevent the text box from starting the text on line 2
 * 02/26/2020   75024       smanoj          Fix to have correct default Color Attributes for 
 *                                          tropical TROF front label.
 * 08/12/2020   81342       mroos           Remove trailing newline at end of text & cursor placement                                         
 *
 * </pre>
 *
 * @author J. Wu
 */

public class TextAttrDlg extends AttrDlg implements IText {

    public static enum FontSizeName {
        TINY, SMALL, MEDIUM, LARGE, HUGE, GIANT
    };

    public static int[] FontSizeValue = { 10, 12, 14, 18, 24, 34 };

    public static String[] FontName = new String[] { "Courier", "Nimbus Sans L",
            "Liberation Serif" };

    private static final String bgmask = " w/ bg mask";

    private static enum ChkBox {
        TEXT, BOX, SIZE, FONT, STYLE, JUSTIFICATION, COLOR, ROTATION
    };

    private static final int MIN_ROTATION = 0;

    private static final int MAX_ROTATION = 360;

    private static final int START_ROTATION = 0;

    private static final int INC_ROTATION = 1;

    private static TextAttrDlg INSTANCE = null;

    private final int TEXT_WIDTH = 160;

    private final int TEXT_HEIGHT = 40;

    private final float INITIAL_FONT_SIZE = 0.0f;

    private final String DEFAULT_FONT_NAME = FontName[0];

    private final FontStyle DEFAULT_FONT_STYLE = FontStyle.REGULAR;

    private final float DEFAULT_FONT_SIZE = 14.0f;

    private final TextJustification DEFAULT_JUSTIFICATION = TextJustification.LEFT_JUSTIFY;

    private final float DEFAULT_ROTATION = 0.0f;

    private final TextRotation DEFAULT_RELATIVE_ROTATION = TextRotation.SCREEN_RELATIVE;

    private Composite top = null;

    private Label colorLbl;

    private ColorButtonSelector cs = null;

    protected Text text = null;

    protected Label textLabel;

    protected Label boxLbl;

    private Combo boxCombo = null;

    private Label sizeLbl;

    protected Combo sizeCombo = null;

    private Label fontLbl;

    private Combo fontCombo = null;

    private Label styleLbl;

    private Combo styleCombo = null;

    private Label justLbl;

    private Combo justCombo = null;

    protected Label rotLbl;

    private Group rotGroup;

    private Group relGroup;

    protected Slider rotSlider = null;

    protected Text rotText = null;

    protected Button screenBtn = null;

    protected Button northBtn = null;

    // Check boxes for multi-selection
    private Button chkBox[];

    // Hash maps for remembering the last attributes specified for a box type

    private Map<String, String> lastTextPerBoxType = new HashMap<>();

    private Map<String, String> lastFontPerBoxType = new HashMap<>();

    private Map<String, String> lastStylePerBoxType = new HashMap<>();

    private Map<String, String> lastJustificationPerBoxType = new HashMap<>();

    private Map<String, Color> lastColorPerBoxType = new HashMap<>();

    private Map<String, Float> lastFontSizePerBoxType = new HashMap<>();

    private Map<String, String> lastRotationPerBoxType = new HashMap<>();

    private Map<String, String> lastRotationRelativityPerBoxType = new HashMap<>();

    /*
     * Attribute values for the last box type that was created. These values are
     * used to populate a newly-created box type rather than use defaults.
     */

    private String lastFontName;

    private String lastStyle;

    private float lastFontSize;

    private String lastJustification;

    private float lastRotation;

    private String lastRotationRelativity;

    private Color lastColor;

    private String lastText;

    /**
     * Private constructor
     * 
     * @param parShell
     * @throws VizException
     */
    protected TextAttrDlg(Shell parShell) throws VizException {

        super(parShell);

    }

    /**
     * Creates a text attribute dialog if the dialog does not exist and returns
     * the instance. If the dialog exists, return the instance.
     * 
     * @param parShell
     * @return
     */
    public static TextAttrDlg getInstance(Shell parShell) {

        if (INSTANCE == null) {

            try {

                INSTANCE = new TextAttrDlg(parShell);

            } catch (VizException e) {

                e.printStackTrace();

            }
        }

        return INSTANCE;

    }

    /**
     * Creates the dialog area
     */
    @Override
    public Control createDialogArea(Composite parent) {

        top = (Composite) super.createDialogArea(parent);

        // Create the main layout for the shell.
        GridLayout mainLayout = new GridLayout(3, false);
        mainLayout.marginHeight = 3;
        mainLayout.marginWidth = 3;
        mainLayout.horizontalSpacing = 3;
        top.setLayout(mainLayout);

        // Initialize all of the menus, controls, and layouts
        initializeComponents();

        return top;

    }

    /**
     * Creates buttons, menus, and other controls in the dialog area
     * 
     * @param listener
     */
    private void initializeComponents() {

        this.getShell().setText("Text Attributes");

        chkBox = new Button[8];

        createTextAttr();
        createBoxAttr();
        createSizeAttr();
        createFontAttr();
        createStyleAttr();
        createJustAttr();
        createColorAttr();
        createRotationAttr();
        addSeparator(top.getParent());

    }

    /**
     * Gets the text
     */
    public String[] getString() {

        if (chkBox[ChkBox.TEXT.ordinal()].getSelection()) {
            return text.getText().split("\n");
        } else {
            return null;
        }

    }

    /**
     * Return font size from the font size combo
     */
    public float getFontSize() {

        if (chkBox[ChkBox.SIZE.ordinal()].getSelection()) {
            return (FontSizeValue[sizeCombo.getSelectionIndex()]);
        } else {
            return java.lang.Float.NaN;
        }
    }

    /**
     * Return font name from the font combo
     */
    public String getFontName() {
        if (chkBox[ChkBox.FONT.ordinal()].getSelection()) {
            return fontCombo.getText();
        } else {
            return null;
        }
    }

    /**
     * Return font style from the style combo
     */
    public FontStyle getStyle() {
        if (chkBox[ChkBox.STYLE.ordinal()].getSelection()) {
            return FontStyle.values()[styleCombo.getSelectionIndex()];
        } else {
            return null;
        }
    }

    /**
     * Return TextJustification from the justification combo
     */
    public TextJustification getJustification() {
        if (chkBox[ChkBox.JUSTIFICATION.ordinal()].getSelection()) {
            return TextJustification.values()[justCombo.getSelectionIndex()];
        } else {
            return null;
        }
    }

    /**
     * Return text rotation from the rotation slider
     */
    public double getRotation() {
        if (chkBox[ChkBox.ROTATION.ordinal()].getSelection()) {
            return rotSlider.getSelection();
        } else {
            return java.lang.Double.NaN;
        }

    }

    /**
     * Return text rotation relativity from the rotation radio boxes
     */
    public TextRotation getRotationRelativity() {

        if (chkBox[ChkBox.ROTATION.ordinal()].getSelection()) {

            if (screenBtn.getSelection()) {
                return TextRotation.SCREEN_RELATIVE;
            } else {
                return TextRotation.NORTH_RELATIVE;
            }
        } else {
            return null;
        }
    }

    /**
     * Return text mask/outline from the box combo
     */
    @Override
    public DisplayType getDisplayType() {
        if (chkBox[ChkBox.BOX.ordinal()].getSelection()) {

            for (DisplayType type : DisplayType.values()) {
                if (boxCombo.getText().startsWith(type.name())) {
                    return type;
                }
            }
            return null;
        } else {
            return null;
        }
    }

    public Boolean maskText() {
        if (chkBox[ChkBox.BOX.ordinal()].getSelection()) {

            if (boxCombo.getText().contains(bgmask)) {
                return true;
            } else {
                return false;
            }
        } else {
            return null;
        }
    }

    /**
     * Return text offset
     */
    public int getXOffset() {
        return 0;
    }

    public int getYOffset() {
        return 0;
    }

    /**
     * Return color from the color picker of the dialog
     */
    public Color[] getColors() {
        if (chkBox[ChkBox.COLOR.ordinal()].getSelection()) {

            // IAttribute requires to return an array of colors
            // Only the first color is used at this time.
            Color[] colors = new Color[1];

            colors[0] = new java.awt.Color(cs.getColorValue().red,
                    cs.getColorValue().green, cs.getColorValue().blue);

            return colors;
        } else {
            return null;
        }

    }

    /**
     * Sets the text
     * 
     * @param txt
     */
    public void setText(String[] txt) {
        StringBuilder result = new StringBuilder("");
        for (String st : txt) {
            result.append(st + "\n");
        }

        int length = result.length();
        if (length > 0) {
            result.delete(length - 1, length);
        }

        // Set text and then the cursor at the end of the string.
        String str = result.toString();
        text.setText(str);
        if (str.length() > 0) {
            text.setSelection(str.length());
        }
    }

    /**
     * Set font size
     */
    public void setFontSize(float size) {
        int index = 0;
        for (int ii = 0; ii < FontSizeValue.length; ii++) {
            if ((int) size == FontSizeValue[ii]) {
                index = ii;
                break;
            }
        }
        sizeCombo.select(index);
    }

    /**
     * Set font name
     */
    public void setFontName(String name) {
        for (int i = 0; i < FontName.length; i++) {
            if (FontName[i].equalsIgnoreCase(name)) {
                fontCombo.setText(FontName[i]);
                fontCombo.select(i);
                break;
            }
        }
    }

    /**
     * set font style
     */
    public void setStyle(FontStyle style) {
        int index = 0;
        for (FontStyle fs : FontStyle.values()) {
            if (fs == style) {
                styleCombo.setText(fs.name());
                break;
            }
            index++;
        }
        styleCombo.select(index);
    }

    /**
     * Return TextJustification from the justification combo
     */
    public void setJustification(TextJustification just) {
        int index = 0;
        for (TextJustification js : TextJustification.values()) {
            if (js == just) {
                justCombo.setText(js.name());
                break;
            }
            index++;
        }
        justCombo.select(index);
    }

    /**
     * set text rotation
     */
    public void setRotation(double rot) {
        rotSlider.setSelection((int) rot);
        rotText.setText("" + (int) rot);
    }

    /**
     * set text rotation relativity
     */
    public void setRotationRelativity(TextRotation trt) {
        if (trt == TextRotation.SCREEN_RELATIVE) {
            screenBtn.setSelection(true);
            northBtn.setSelection(false);
        } else {
            northBtn.setSelection(true);
            screenBtn.setSelection(false);
        }
    }

    /**
     * set text box from the mask/outline flag
     */
    public void setBoxText(boolean mask, DisplayType outline) {

        StringBuilder sb = new StringBuilder(outline.name());

        if (mask) {
            sb.append(bgmask);
        }

        boxCombo.setText(sb.toString());

    }

    /**
     * set text offset
     */
    public void setXOffset(int xoff) {
    }

    public void setYOffset(int yoff) {
    }

    /**
     * Sets the color of the color picker of the dialog.
     * 
     * @param clr
     */
    public void setColor(Color clr) {

        cs.setColorValue(new RGB(clr.getRed(), clr.getGreen(), clr.getBlue()));

    }

    /**
     * Sets values of all attributes of the dialog.
     */
    public void setAttrForDlg(IAttribute iattr) {

        if (iattr instanceof IText) {
            IText attr = (IText) iattr;
            this.setBoxText(attr.maskText(), attr.getDisplayType());
            this.setText(attr.getString());
            this.setFontName(attr.getFontName());
            this.setFontSize(attr.getFontSize());
            this.setJustification(attr.getJustification());
            this.setRotation(attr.getRotation());
            this.setRotationRelativity(attr.getRotationRelativity());
            this.setStyle(attr.getStyle());
            this.setXOffset(attr.getXOffset());
            this.setYOffset(attr.getYOffset());
            this.setBoxText(attr.maskText(), attr.getDisplayType());
            Color clr = attr.getColors()[0];
            if (clr != null)
                this.setColor(clr);
        }
    }

    /**
     * Set the check boxes visible/invisible
     * 
     * @param flag
     */
    private void enableChkBoxes(boolean flag) {

        if (!flag) {
            setAllChkBoxes();
        }
        for (ChkBox chk : ChkBox.values()) {
            chkBox[chk.ordinal()].setVisible(flag);
        }

    }

    /**
     * Enable/disable all widgets in the attribute dialog
     * 
     * @param flag
     */
    private void enableAllWidgets(boolean flag) {
        textLabel.setEnabled(flag);
        text.setEnabled(flag);

        boxLbl.setEnabled(flag);
        boxCombo.setEnabled(flag);

        sizeLbl.setEnabled(flag);
        sizeCombo.setEnabled(flag);

        fontLbl.setEnabled(flag);
        fontCombo.setEnabled(flag);

        styleLbl.setEnabled(flag);
        styleCombo.setEnabled(flag);

        justLbl.setEnabled(flag);
        justCombo.setEnabled(flag);

        colorLbl.setEnabled(flag);

        rotLbl.setEnabled(flag);
        rotGroup.setEnabled(flag);
        rotSlider.setEnabled(flag);
        rotText.setEnabled(flag);

        relGroup.setEnabled(flag);
        screenBtn.setEnabled(flag);
        northBtn.setEnabled(flag);
    }

    /**
     * Set all check boxes to true
     */
    private void setAllChkBoxes() {

        for (ChkBox chk : ChkBox.values()) {
            chkBox[chk.ordinal()].setSelection(true);
        }
    }

    /**
     * Get Color value corresponding to RGB value
     * 
     * @param rgbValue
     * 
     * @return color value
     */
    private Color getColorFromColorValue(RGB rgbValue) {
        return new Color(rgbValue.red, rgbValue.green, rgbValue.blue);
    }

    @Override
    public int open() {

        this.create();

        if (PgenSession.getInstance().getPgenPalette().getCurrentAction()
                .equalsIgnoreCase(PgenConstant.ACTION_MULTISELECT)) {
            enableChkBoxes(true);
            enableAllWidgets(false);
        } else {
            enableChkBoxes(false);
        }

        return super.open();
    }

    /**
     * create widgets for the text attribute
     */
    private void createTextAttr() {

        chkBox[ChkBox.TEXT.ordinal()] = new Button(top, SWT.CHECK);
        chkBox[ChkBox.TEXT.ordinal()]
                .setLayoutData(new GridData(CHK_WIDTH, CHK_HEIGHT));
        chkBox[ChkBox.TEXT.ordinal()]
                .addSelectionListener(new SelectionAdapter() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        Button btn = (Button) e.widget;
                        if (btn.getSelection()) {
                            textLabel.setEnabled(true);
                            text.setEnabled(true);
                        } else {
                            textLabel.setEnabled(false);
                            text.setEnabled(false);

                        }
                    }

                });

        textLabel = new Label(top, SWT.LEFT);
        textLabel.setText("Text:");

        int style = SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL;
        text = new Text(top, style);

        text.setLayoutData(new GridData(TEXT_WIDTH, TEXT_HEIGHT));
        text.setEditable(true);

        // Update the text/box-type map
        text.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                String theText = text.getText();
                lastTextPerBoxType.put(boxCombo.getText(), theText);
                lastText = theText;
            }
        });

        text.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if ("\n".equals(text.getText())) {
                    text.setSelection(0);
                }
            }

        });

        text.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                if ("\n".equals(text.getText())) {
                    text.setSelection(0);
                }
            }

            @Override
            public void mouseDoubleClick(MouseEvent e) {
                if ("\n".equals(text.getText())) {
                    text.setSelection(0);
                }
            }
        });

    }

    /**
     * create widgets for the box attribute
     */
    private void createBoxAttr() {

        chkBox[ChkBox.BOX.ordinal()] = new Button(top, SWT.CHECK);
        chkBox[ChkBox.BOX.ordinal()]
                .setLayoutData(new GridData(CHK_WIDTH, CHK_HEIGHT));
        chkBox[ChkBox.BOX.ordinal()]
                .addSelectionListener(new SelectionAdapter() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        Button btn = (Button) e.widget;
                        if (btn.getSelection()) {
                            boxLbl.setEnabled(true);
                            boxCombo.setEnabled(true);
                        } else {
                            boxLbl.setEnabled(false);
                            boxCombo.setEnabled(false);

                        }
                    }
                });

        boxLbl = new Label(top, SWT.LEFT);
        boxLbl.setText("Box:");

        boxCombo = new Combo(top, SWT.DROP_DOWN | SWT.READ_ONLY);
        for (DisplayType type : DisplayType.values()) {
            boxCombo.add(type.name());
            boxCombo.add(type.name() + bgmask);

            // initialize the text/box-type map
            if (!lastTextPerBoxType.containsKey(type.name())) {
                lastTextPerBoxType.put(type.name(), "");
                lastTextPerBoxType.put(type.name() + bgmask, "");
            }
        }

        boxCombo.select(0);

        /*
         * Set all attributes associated with this box type to the last values
         * specified for the box type, or to the values specified for the
         * previous box type (can be a different type) created.
         */
        boxCombo.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {

                String text = "";
                String lastTextForBox = lastTextPerBoxType
                        .get(boxCombo.getText());
                if (lastTextForBox != null && !lastTextForBox.equals("")
                        && !lastTextForBox.equals("\n")) {
                    text = lastTextForBox;
                } else if (lastText != null && !lastText.equals("")) {
                    text = lastText;
                }
                setText(new String[] { text });

                // If this box type has been used before, use the font for
                // that box type.
                if (lastFontPerBoxType.containsKey(boxCombo.getText())) {
                    String fontName = lastFontPerBoxType
                            .get(boxCombo.getText());
                    if (fontName != null && !fontName.equals("")) {
                        setFontName(fontName);
                    } else {
                        if (lastFontName != null && !lastFontName.equals("")) {
                            setFontName(lastFontName);
                        } else {
                            setFontName(DEFAULT_FONT_NAME);
                        }
                    }
                }

                // If this box type has been used before, use the style for
                // that box type.
                if (lastStylePerBoxType.containsKey(boxCombo.getText())) {
                    String style = lastStylePerBoxType.get(boxCombo.getText());
                    if (style != null && !style.equals("")) {
                        setStyle(FontStyle.valueOf(style));
                    } else {
                        if (lastStyle != null && !lastStyle.equals("")) {
                            setStyle(FontStyle.valueOf(lastStyle));
                        } else {
                            setStyle(DEFAULT_FONT_STYLE);
                        }
                    }
                }

                // If this box type has been used before, use the font size for
                // that box type.
                if (lastFontSizePerBoxType.containsKey(boxCombo.getText())) {
                    float fontSize = lastFontSizePerBoxType
                            .get(boxCombo.getText());
                    if (!(Float.isNaN(fontSize))
                            && fontSize != INITIAL_FONT_SIZE) {
                        setFontSize(fontSize);
                    } else {
                        if (!(Float.isNaN(lastFontSize))) {
                            setFontSize(lastFontSize);
                        } else {
                            setFontSize(DEFAULT_FONT_SIZE);
                        }
                    }
                }

                // If this box type has been used before, use the justification
                // for that box type.
                if (lastJustificationPerBoxType
                        .containsKey(boxCombo.getText())) {
                    String justification = lastJustificationPerBoxType
                            .get(boxCombo.getText());
                    if (justification != null && !justification.equals("")) {
                        setJustification(
                                TextJustification.valueOf(justification));
                    } else {
                        if (lastJustification != null
                                && !lastJustification.equals("")) {
                            setJustification(TextJustification
                                    .valueOf(lastJustification));
                        } else {
                            setJustification(DEFAULT_JUSTIFICATION);
                        }
                    }
                }

                // If this box type has been used before, use the rotation for
                // that box type.
                if (lastRotationPerBoxType.containsKey(boxCombo.getText())) {
                    String rotation = lastRotationPerBoxType
                            .get(boxCombo.getText());
                    if (rotation != null && !rotation.equals("")) {
                        setRotation(Double.valueOf(rotation));
                    } else {
                        if (!(Float.isNaN(lastRotation))) {
                            setRotation(Double.valueOf(lastRotation));
                        } else {
                            setRotation(DEFAULT_ROTATION);
                        }
                    }
                }

                // If this box type has been used before, use the rotation
                // relativity for that box type.
                setRotationRelativity(DEFAULT_RELATIVE_ROTATION);
                if (lastRotationRelativityPerBoxType
                        .containsKey(boxCombo.getText())) {
                    String rotationRelativity = lastRotationRelativityPerBoxType
                            .get(boxCombo.getText());
                    if (rotationRelativity != null
                            && !rotationRelativity.equals("")) {
                        setRotationRelativity(
                                TextRotation.valueOf(rotationRelativity));
                    }
                } else if (lastRotationRelativity != null) {
                    setRotationRelativity(
                            TextRotation.valueOf(lastRotationRelativity));
                }

                // If this box type has been used before, use the color
                // relativity for that box type.
                if (lastColorPerBoxType.containsKey(boxCombo.getText())) {
                    Color color = lastColorPerBoxType.get(boxCombo.getText());
                    if (color != null) {
                        setColor(color);
                    }
                } else if (lastColor != null) {
                    setColor(lastColor);
                }

            }
        });
    }

    /**
     * create widgets for the size attribute
     */
    private void createSizeAttr() {

        chkBox[ChkBox.SIZE.ordinal()] = new Button(top, SWT.CHECK);
        chkBox[ChkBox.SIZE.ordinal()]
                .setLayoutData(new GridData(CHK_WIDTH, CHK_HEIGHT));
        chkBox[ChkBox.SIZE.ordinal()]
                .addSelectionListener(new SelectionAdapter() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        Button btn = (Button) e.widget;
                        if (btn.getSelection()) {
                            sizeLbl.setEnabled(true);
                            sizeCombo.setEnabled(true);
                        } else {
                            sizeLbl.setEnabled(false);
                            sizeCombo.setEnabled(false);

                        }
                    }

                });

        sizeLbl = new Label(top, SWT.LEFT);
        sizeLbl.setText("Size:");

        sizeCombo = new Combo(top, SWT.DROP_DOWN | SWT.READ_ONLY);

        for (FontSizeName fs : FontSizeName.values()) {
            sizeCombo.add(fs.name());
        }

        float fontSize = DEFAULT_FONT_SIZE;
        if (lastFontSizePerBoxType.get(boxCombo.getText()) != null) {
            fontSize = lastFontSizePerBoxType.get(boxCombo.getText());
            if (!Float.isNaN(fontSize) && fontSize != INITIAL_FONT_SIZE) {
                setFontSize(fontSize);
            } else if (!Float.isNaN(lastFontSize)) {
                setFontSize(lastFontSize);
            }
        } else {
            setFontSize(fontSize);
        }

        // Update the font/box-type map
        sizeCombo.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                float fontSize = (float) FontSizeValue[sizeCombo
                        .getSelectionIndex()];
                lastFontSizePerBoxType.put(boxCombo.getText(), fontSize);
                lastFontSize = fontSize;
            }
        });
    }

    /**
     * create widgets for the font attribute
     */
    private void createFontAttr() {

        chkBox[ChkBox.FONT.ordinal()] = new Button(top, SWT.CHECK);
        chkBox[ChkBox.FONT.ordinal()]
                .setLayoutData(new GridData(CHK_WIDTH, CHK_HEIGHT));
        chkBox[ChkBox.FONT.ordinal()]
                .addSelectionListener(new SelectionAdapter() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        Button btn = (Button) e.widget;
                        if (btn.getSelection()) {
                            fontLbl.setEnabled(true);
                            fontCombo.setEnabled(true);
                        } else {
                            fontLbl.setEnabled(false);
                            fontCombo.setEnabled(false);

                        }
                    }
                });

        fontLbl = new Label(top, SWT.LEFT);
        fontLbl.setText("Font:");

        fontCombo = new Combo(top, SWT.DROP_DOWN | SWT.READ_ONLY);

        for (String st : FontName) {
            fontCombo.add(st);
        }

        setFontName("");
        String fontName = lastFontPerBoxType.get(boxCombo.getText());
        if (fontName != null && !fontName.equals("")) {
            setFontName(fontName);
        } else if (lastFontName != null && !lastFontName.equals("")) {
            setFontName(lastFontName);
        }

        // Update the font/box-type map
        fontCombo.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                lastFontPerBoxType.put(boxCombo.getText(), fontCombo.getText());
                lastFontName = fontCombo.getText();
            }
        });

    }

    /**
     * create widgets for the style attribute
     */
    private void createStyleAttr() {
        chkBox[ChkBox.STYLE.ordinal()] = new Button(top, SWT.CHECK);
        chkBox[ChkBox.STYLE.ordinal()]
                .setLayoutData(new GridData(CHK_WIDTH, CHK_HEIGHT));
        chkBox[ChkBox.STYLE.ordinal()]
                .addSelectionListener(new SelectionAdapter() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        Button btn = (Button) e.widget;
                        if (btn.getSelection()) {
                            styleLbl.setEnabled(true);
                            styleCombo.setEnabled(true);
                        } else {
                            styleLbl.setEnabled(false);
                            styleCombo.setEnabled(false);

                        }
                    }
                });

        styleLbl = new Label(top, SWT.LEFT);
        styleLbl.setText("Style:");

        styleCombo = new Combo(top, SWT.DROP_DOWN | SWT.READ_ONLY);

        for (FontStyle fs : FontStyle.values()) {
            styleCombo.add(fs.name());
        }

        setStyle(DEFAULT_FONT_STYLE);
        String style = lastStylePerBoxType.get(boxCombo.getText());
        if (style != null && !style.equals("")) {
            setStyle(FontStyle.valueOf(style));
        } else if (lastStyle != null && !lastStyle.equals("")) {
            setStyle(FontStyle.valueOf(lastStyle));
        }

        // Update the font style/box-type map
        styleCombo.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                lastStylePerBoxType.put(boxCombo.getText(),
                        styleCombo.getText());
                lastStyle = styleCombo.getText();
            }
        });
    }

    /**
     * create widgets for the Justification attribute
     */
    private void createJustAttr() {

        chkBox[ChkBox.JUSTIFICATION.ordinal()] = new Button(top, SWT.CHECK);
        chkBox[ChkBox.JUSTIFICATION.ordinal()]
                .setLayoutData(new GridData(CHK_WIDTH, CHK_HEIGHT));
        chkBox[ChkBox.JUSTIFICATION.ordinal()]
                .addSelectionListener(new SelectionAdapter() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        Button btn = (Button) e.widget;
                        if (btn.getSelection()) {
                            justLbl.setEnabled(true);
                            justCombo.setEnabled(true);
                        } else {
                            justLbl.setEnabled(false);
                            justCombo.setEnabled(false);

                        }
                    }

                });

        justLbl = new Label(top, SWT.LEFT);
        justLbl.setText("Just:");

        justCombo = new Combo(top, SWT.DROP_DOWN | SWT.READ_ONLY);

        for (TextJustification js : TextJustification.values()) {
            justCombo.add(js.name());
        }

        setJustification(DEFAULT_JUSTIFICATION);
        String justification = lastJustificationPerBoxType
                .get(boxCombo.getText());
        if (justification != null && !justification.equals("")) {
            setJustification(TextJustification.valueOf(justification));
        } else if (lastJustification != null && !lastJustification.equals("")) {
            setJustification(TextJustification.valueOf(lastJustification));
        }

        // Update the font style/box-type map
        justCombo.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                lastJustificationPerBoxType.put(boxCombo.getText(),
                        justCombo.getText());
                lastJustification = justCombo.getText();
            }
        });
    }

    /**
     * create widgets for the Color attribute
     */
    private void createColorAttr() {
        chkBox[ChkBox.COLOR.ordinal()] = new Button(top, SWT.CHECK);
        chkBox[ChkBox.COLOR.ordinal()]
                .setLayoutData(new GridData(CHK_WIDTH, CHK_HEIGHT));
        chkBox[ChkBox.COLOR.ordinal()]
                .addSelectionListener(new SelectionAdapter() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        Button btn = (Button) e.widget;
                        if (btn.getSelection()) {
                            colorLbl.setEnabled(true);
                        } else {
                            colorLbl.setEnabled(false);

                        }
                    }

                });

        colorLbl = new Label(top, SWT.LEFT);
        colorLbl.setText("Color:");
        cs = new ColorButtonSelector(top);

        Color color = lastColorPerBoxType.get(boxCombo.getText());
        if (color != null) {
            setColor(color);
        } else if (lastColor != null) {
            setColor(lastColor);
        }

        // Update the color/box-type map
        cs.addListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                Color currentColor = getColorFromColorValue(cs.getColorValue());
                lastColorPerBoxType.put(boxCombo.getText(), currentColor);
                lastColor = currentColor;
            }
        });

    }

    /**
     * create widgets for the Rotation attribute
     */
    private void createRotationAttr() {

        chkBox[ChkBox.ROTATION.ordinal()] = new Button(top, SWT.CHECK);
        chkBox[ChkBox.ROTATION.ordinal()]
                .setLayoutData(new GridData(CHK_WIDTH, CHK_HEIGHT));
        chkBox[ChkBox.ROTATION.ordinal()]
                .addSelectionListener(new SelectionAdapter() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        Button btn = (Button) e.widget;
                        if (btn.getSelection()) {
                            rotLbl.setEnabled(true);
                            rotGroup.setEnabled(true);
                            rotSlider.setEnabled(true);
                            rotText.setEnabled(true);

                            relGroup.setEnabled(true);
                            screenBtn.setEnabled(true);
                            northBtn.setEnabled(true);
                        } else {
                            rotLbl.setEnabled(false);
                            rotGroup.setEnabled(false);
                            rotSlider.setEnabled(false);
                            rotText.setEnabled(false);

                            relGroup.setEnabled(false);
                            screenBtn.setEnabled(false);
                            northBtn.setEnabled(false);
                        }
                    }

                });

        rotLbl = new Label(top, SWT.LEFT);
        rotLbl.setText("Rot:");

        rotGroup = new Group(top, SWT.NONE);
        GridLayout gl = new GridLayout(2, false);
        rotGroup.setLayout(gl);

        rotSlider = new Slider(rotGroup, SWT.HORIZONTAL);
        rotSlider.setValues(START_ROTATION, MIN_ROTATION, MAX_ROTATION, 1,
                INC_ROTATION, 5);

        rotSlider.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                rotText.setText("" + rotSlider.getSelection());
                lastRotationPerBoxType.put(boxCombo.getText(),
                        rotText.getText());
            }
        });

        rotText = new Text(rotGroup, SWT.SINGLE | SWT.BORDER);
        rotText.setLayoutData(new GridData(20, 10));
        rotText.setEditable(true);
        rotText.setText("" + START_ROTATION);
        rotText.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                int value = 0;
                try {
                    value = Integer.parseInt(rotText.getText());
                    if (value >= MIN_ROTATION && value < MAX_ROTATION) {
                        rotSlider.setSelection(value);
                        rotText.setToolTipText("");
                    } else {
                        rotText.setToolTipText(
                                "Only integer values between 0 and 360 are accepted.");
                    }
                } catch (NumberFormatException e1) {
                    rotText.setToolTipText(
                            "Only integer values between 0 and 360 are accepted.");
                }
                lastRotation = 0.0f;
                String rotation = rotText.getText();
                lastRotationPerBoxType.put(boxCombo.getText(), rotation);
                if (rotation != null && !rotation.equals("")) {
                    lastRotation = Float.valueOf(rotation);
                }
            }
        });

        Button scnChkBox = new Button(top, SWT.CHECK);
        scnChkBox.setLayoutData(new GridData(CHK_WIDTH, CHK_HEIGHT));
        scnChkBox.setVisible(false);

        Label grpLbl = new Label(top, SWT.LEFT);
        grpLbl.setText("   ");

        relGroup = new Group(top, SWT.NONE);
        relGroup.setLayout(gl);

        screenBtn = new Button(relGroup, SWT.RADIO);
        screenBtn.setText("Screen");

        northBtn = new Button(relGroup, SWT.RADIO);
        northBtn.setText("North");

        screenBtn.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                lastRotationRelativity = TextRotation.SCREEN_RELATIVE
                        .toString();
                lastRotationRelativityPerBoxType.put(boxCombo.getText(),
                        lastRotationRelativity);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                // TODO Auto-generated method stub
            }
        });

        northBtn.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                lastRotationRelativity = TextRotation.NORTH_RELATIVE.toString();
                lastRotationRelativityPerBoxType.put(boxCombo.getText(),
                        lastRotationRelativity);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                // TODO Auto-generated method stub
            }
        });

    }

    @Override
    public void okPressed() {
        /*
         * An error will pop up if the user leaves the text field blank. This
         * prevents errors in DisplayElementsFactory.java when bounds is null
         * Don't open error dialog in multi-select mode when checkbox is
         * unchecked.
         */
        if (!(PgenSession.getInstance().getPgenPalette().getCurrentAction()
                .equalsIgnoreCase(PgenConstant.ACTION_MULTISELECT)
                && !chkBox[ChkBox.TEXT.ordinal()].getSelection())
                && (text.getText().length() == 0
                        || text.getText().matches("^[ \n]*$"))) {

            MessageDialog.openError(null, "Warning!", "No text entered");

        } else {

            // Invokes the superclass AttrDlg.okPressed(), which copies the
            // information from the dialog into a duplicate Text object,
            // stores it, and draws it.
            super.okPressed();
        }
    }

    @Override
    public Coordinate getPosition() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Color getTextColor() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Boolean isClear() {
        return false;
    }

    @Override
    public Coordinate getLocation() {
        return null;
    }

    @Override
    public Boolean getHide() {
        return false;
    }

    @Override
    public Boolean getAuto() {
        return false;
    }

    @Override
    public int getIthw() {
        return 2;
    }

    @Override
    public int getIwidth() {
        return 1;
    }

}
