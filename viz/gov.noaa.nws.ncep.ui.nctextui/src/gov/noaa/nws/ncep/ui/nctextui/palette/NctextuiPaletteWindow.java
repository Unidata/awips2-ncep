/**
 * 
 * gov.noaa.nws.ncep.ui.nctextui.palette.NctextPaletteWindow
 * 
 * This java class performs the NCTEXT GUI construction.
 * This code has been developed by the SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    	Engineer    Description
 * -------		------- 	-------- 	-----------
 * 12/24/2009		TBD		Chin Chen	Initial coding
 * 06/28/2011       T402       X. Guo   Re-format NCTEXT view panel, check
 *                                      the click action on nctext legend
 * 02/15/2012     #972      G. Hull     NatlCntrsEditor 
 *
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 */
package gov.noaa.nws.ncep.ui.nctextui.palette;

import gov.noaa.nws.ncep.ui.nctextui.dbutil.EReportTimeRange;
import gov.noaa.nws.ncep.ui.nctextui.dbutil.NctextDbQuery;
import gov.noaa.nws.ncep.ui.nctextui.dbutil.NctextStationInfo;
import gov.noaa.nws.ncep.ui.nctextui.rsc.NctextuiResource;
import gov.noaa.nws.ncep.viz.ui.display.NatlCntrsEditor;
import gov.noaa.nws.ncep.viz.ui.display.NcDisplayMngr;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.viz.ui.UiUtil;

public class NctextuiPaletteWindow extends ViewPart implements
        SelectionListener, DisposeListener, IPartListener {

    private String selectedGp = null;

    private String selectedType = null;

    private IWorkbenchPage page;

    private org.eclipse.swt.widgets.List gpWdgList = null;

    private org.eclipse.swt.widgets.List typeWdgList = null;

    private NctextDbQuery query;

    private EReportTimeRange timeCovered = EReportTimeRange.TWELVE_HOURS;

    private boolean isState = false;

    private boolean replaceText = true;

    private final static String GP_LABEL = "Select Data Type Group ";

    private final static String PRODUCT_LABEL = "Select Data Type Product";

    private static final char NEW_LINE = '\n';

    private Group dataTypeGp;

    private Group prodListGp;

    private Group timeGp;

    private Group staStnGp;

    private Group textGp;

    private String currentProductName = null;

    private int colWidth = 200;

    private int listHeight = 150;

    private int btnGapY = 5;

    private int btnGapX = 5;

    private int timeBtnWidth = 40;

    private int staBtnWidth = 75;

    private int btnHeight = 20;

    private int pushbtnHeight = 25;

    private int labelGap = 20;

    private static int ASCII_CR_VAL = 13;

    private Text text;

    private Button nextBtn, prevBtn;

    private java.util.List<NctextStationInfo> points = new ArrayList<NctextStationInfo>();

    private HandlePrinting printingHandle;

    private boolean isEditorVisible = true;

    private int dataTypeGpItem = 0;

    private int dataTypePdItem = 0;

    private java.util.List<Object[]> currentTextReports = null;

    private int currentTextIndex;

    public int getCurrentTextIndex() {
        return currentTextIndex;
    }

    public void setCurrentTextIndex(int currentTextIndex) {
        this.currentTextIndex = currentTextIndex;
    }

    public java.util.List<Object[]> getCurrentTextReports() {
        return currentTextReports;
    }

    public void setCurrentTextReports(
            java.util.List<Object[]> currentTextReports) {
        this.currentTextReports = currentTextReports;
    }

    public java.util.List<NctextStationInfo> getPoints() {
        return points;
    }

    public void setPoints(java.util.List<NctextStationInfo> points) {
        this.points = points;
    }

    public boolean isReplaceText() {
        return replaceText;
    }

    public void setReplaceText(boolean replaceText) {
        this.replaceText = replaceText;
    }

    public EReportTimeRange getTimeCovered() {
        return timeCovered;
    }

    public void setTimeCovered(EReportTimeRange timeCovered) {
        this.timeCovered = timeCovered;
    }

    public boolean isState() {
        return isState;
    }

    public void setState(boolean isState) {
        this.isState = isState;
    }

    public String getCurrentProductName() {
        return currentProductName;
    }

    public void setCurrentProductName(String currentProductName) {
        this.currentProductName = currentProductName;
    }

    public void enableNextBtn(boolean enable) {
        nextBtn.setEnabled(enable);
    }

    public void enablePrevBtn(boolean enable) {
        prevBtn.setEnabled(enable);
    }

    public void setEditorVisible(boolean isVisible) {
        this.isEditorVisible = isVisible;
    }

    public boolean getEditorVisible() {
        return this.isEditorVisible;
    }

    public void setDataTypeGroupItem(int item) {
        this.dataTypeGpItem = item;
    }

    public int getDataTypeGroupItem() {
        return this.dataTypeGpItem;
    }

    public void setDataTypeProductItem(int item) {
        this.dataTypePdItem = item;
    }

    public int getDataTypeProductItem() {
        return this.dataTypePdItem;
    }

    public NctextuiPaletteWindow() {

        super();
        if (nctextuiPaletteWindow == null)
            nctextuiPaletteWindow = this;
        else {
            NctextuiPaletteWindow tmp = nctextuiPaletteWindow;
            nctextuiPaletteWindow = this;
            nctextuiPaletteWindow.setCurrentProductName(tmp
                    .getCurrentProductName());
            nctextuiPaletteWindow.setDataTypeGroupItem(tmp
                    .getDataTypeGroupItem());
            nctextuiPaletteWindow.setDataTypeProductItem(tmp
                    .getDataTypeProductItem());
            ;
            nctextuiPaletteWindow.selectedGp = tmp.selectedGp;
            nctextuiPaletteWindow.selectedType = tmp.selectedType;
            nctextuiPaletteWindow.setTimeCovered(tmp.getTimeCovered());
            nctextuiPaletteWindow.setState(tmp.isState());
        }
    }

    // create this singleton object
    private static NctextuiPaletteWindow nctextuiPaletteWindow = null;

    public static NctextuiPaletteWindow getAccess() {
        return nctextuiPaletteWindow;
    }

    /**
     * Invoked by the workbench to initialize this View.
     */
    public void init(IViewSite site) {
        try {

            super.init(site);

        } catch (PartInitException pie) {

            pie.printStackTrace();

        }
        NctextuiResource.registerMouseHandler();
        page = site.getPage();
        page.addPartListener(this);

    }

    /**
     * Disposes resource. invoked by the workbench
     */
    public void dispose() {
        if (!isEditorVisible) {
            NctextuiResource.unregisterMouseHandler();
            return;
        } else {
            super.dispose();

            NatlCntrsEditor editor = NctextuiResource.getMapEditor();

            if (editor != null) {
                for (IRenderableDisplay display : UiUtil
                        .getDisplaysFromContainer(editor)) {
                    for (ResourcePair rp : display.getDescriptor()
                            .getResourceList()) {
                        if (rp.getResource() instanceof NctextuiResource) {
                            NctextuiResource rsc = (NctextuiResource) rp
                                    .getResource();
                            rsc.unload();
                        }
                    }
                }
            }
            nctextuiPaletteWindow = null;

            // remove the workbench part listener
            page.removePartListener(this);
        }

    }

    private void close() {
        IWorkbenchPage wpage = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage();

        IViewPart vpart = wpage.findView("gov.noaa.nws.ncep.ui.NCTEXTUI");
        wpage.hideView(vpart);

        NcDisplayMngr.setPanningMode();
    }

    /**
     * Invoked by the workbench, this method sets up the SWT controls for the
     * nctext palette
     */
    @Override
    public void createPartControl(Composite parent) {

        parent.setLayout(new GridLayout(1, false));
        // create textGp group. It contains text and textMode group
        textGp = new Group(parent, SWT.SHADOW_OUT);
        textGp.setLayout(new GridLayout());
        textGp.setText("Text Report");
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        textGp.setLayoutData(data);

        createTextArea(textGp);
        createTextModeGp(textGp);

        // Create ConfigGp group. It contains dataTypegp, dataProductGp, time
        // cover group and state/stn group
        Group configGp = new Group(parent, SWT.SHADOW_ETCHED_OUT);
        configGp.setLayout(new GridLayout(3, false));

        createGpList(configGp);

        createProductList(configGp);

        createTimeCoverBtns(configGp);
        if (currentProductName != null) {
            handleStnMarkingRequestByBtn();
        }
    }

    public void createGpList(Composite parent) {
        dataTypeGp = new Group(parent, SWT.SHADOW_ETCHED_IN);
        dataTypeGp.setText(GP_LABEL);

        // create GP widget list
        gpWdgList = new org.eclipse.swt.widgets.List(dataTypeGp, SWT.SINGLE
                | SWT.V_SCROLL);
        gpWdgList.setBounds(dataTypeGp.getBounds().x + btnGapX,
                dataTypeGp.getBounds().y + labelGap, colWidth, listHeight);
        // query gp list form nctextdbrsc
        query = NctextDbQuery.getAccess();
        // query = NctextDbQueryX.getAccess();
        java.util.List<String> groupList = query.getDataTypeGpList();

        if ((groupList != null) && (groupList.size() > 0)
                && (dataTypeGpItem < groupList.size())) {
            // set default select gp to first gp in the list
            selectedGp = groupList.get(dataTypeGpItem);
        } else {
            return;
        }
        // add gp item to gp widget list
        for (String str : groupList) {
            gpWdgList.add(str);
        }

        gpWdgList.setSelection(dataTypeGpItem);

        // create a selection listener to handle user's selection on gp list
        gpWdgList.addListener(SWT.Selection, new Listener() {
            java.util.List<String> prodTypeList;

            public void handleEvent(Event e) {
                if (gpWdgList.getSelectionCount() > 0) {
                    selectedGp = gpWdgList.getSelection()[0];
                    // query selected gp's product type list form nctextdbrsc
                    prodTypeList = query.getGpProductList(selectedGp);
                    typeWdgList.removeAll();

                    for (String str : prodTypeList) {
                        /*
                         * add product type list to widget list, note that
                         * Widgget list was created earlier. This part of code
                         * is listener event handler and is invoked when user
                         * picks gp
                         */
                        typeWdgList.add(str);
                    }
                    typeWdgList.setSelection(0);
                    setDataTypeGroupItem(gpWdgList.getSelectionIndex());
                }
            }
        });

    }

    public void createProductList(Composite parent) {
        prodListGp = new Group(parent, SWT.SHADOW_ETCHED_IN);
        prodListGp.setText(PRODUCT_LABEL);
        // create product type widget list
        typeWdgList = new org.eclipse.swt.widgets.List(prodListGp, SWT.SINGLE
                | SWT.V_SCROLL | SWT.H_SCROLL);

        typeWdgList.setBounds(prodListGp.getBounds().x + btnGapX,
                prodListGp.getBounds().y + labelGap, colWidth, listHeight);

        /* add default product type list */
        java.util.List<String> tempprodTypeList;
        tempprodTypeList = query.getGpProductList(selectedGp);
        typeWdgList.removeAll();

        if (tempprodTypeList == null || tempprodTypeList.size() == 0)
            return;
        for (String str : tempprodTypeList) {
            // add default product type list to widget list, note that widget
            // list was created earlier.
            typeWdgList.add(str);
        }

        typeWdgList.setSelection(dataTypePdItem);
        typeWdgList.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                if (typeWdgList.getSelectionCount() > 0) {
                    selectedType = typeWdgList.getSelection()[0];
                    currentProductName = selectedType;
                    // handle station marking here, when user picked product
                    // type
                    handleProductTypeStnMarking();
                    setDataTypeProductItem(typeWdgList.getSelectionIndex());
                }
            }
        });
    }

    private void handleProductTypeStnMarking() {
        points = query.getProductStaList(selectedType, timeCovered);

        if (points != null && points.size() == 1) {
            NctextuiResource.getNctextuiResource().setPoints(points);
            NctextuiResource.getNctextuiResource().setPickedStnPt(points);
            displayProduct(points.get(0));
        } else {

            if (points != null && points.size() > 1) {
                text.setText(" ");
            } else {
                text.setText("No Station Reports This Product At Selected Time Range");
            }
            nextBtn.setEnabled(false);
            prevBtn.setEnabled(false);
            NatlCntrsEditor mapEditor = NctextuiResource.getMapEditor();
            if (mapEditor != null) {
                mapEditor.refresh();
                PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                        .getActivePage().bringToTop(mapEditor);
            }
            NctextuiResource.getNctextuiResource().setPoints(points);
            NctextuiResource.getNctextuiResource().setPickedStnPt(null);
        }
    }

    private void handleStnMarkingRequestByBtn() {
        points = query.getProductStaList(selectedType, timeCovered);
        if (points != null && points.size() == 1) {
            NctextuiResource.getNctextuiResource().setPoints(points);
            NctextuiResource.getNctextuiResource().setPickedStnPt(points);
            displayProduct(points.get(0));
        } else {
            if ((points != null) && (points.size() != 0)) {

                if (replaceText)
                    text.setText(" ");
            } else {
                if (replaceText)
                    text.setText("No Report Available");
            }

            nextBtn.setEnabled(false);
            prevBtn.setEnabled(false);
            NatlCntrsEditor mapEditor = NctextuiResource.getMapEditor();
            if (mapEditor != null) {
                mapEditor.refresh();
                PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                        .getActivePage().bringToTop(mapEditor);
            }

            NctextuiResource.getNctextuiResource().setPoints(points);
            if (replaceText) {
                NctextuiResource.getNctextuiResource().setPickedStnPt(null);
            }
        }
    }

    public void createTimeCoverBtns(Composite parent) {

        Group composite = new Group(parent, SWT.NORMAL);
        composite.setLayout(new GridLayout(1, false));

        timeGp = new Group(composite, SWT.SHADOW_ETCHED_IN);
        timeGp.setText("Hour Covered");

        Listener btnListeener = new Listener() {
            public void handleEvent(Event event) {
                String btnText = ((Button) (event.widget)).getText();
                timeCovered = EReportTimeRange.NONE;
                if (!btnText.equals("all")) {
                    EReportTimeRange timeRange = EReportTimeRange.NONE;
                    timeCovered = timeRange.getTimeRangeFromInt(Integer
                            .valueOf(btnText));
                }
                handleStnMarkingRequestByBtn();
            }

        };

        Button oneHrBtn = new Button(timeGp, SWT.RADIO | SWT.BORDER);
        oneHrBtn.setText("1");
        oneHrBtn.setEnabled(true);
        oneHrBtn.setBounds(timeGp.getBounds().x + btnGapX, timeGp.getBounds().y
                + labelGap, timeBtnWidth, btnHeight);
        oneHrBtn.addListener(SWT.MouseUp, btnListeener);

        Button threeHrBtn = new Button(timeGp, SWT.RADIO);
        threeHrBtn.setText("3");
        threeHrBtn.setEnabled(true);
        threeHrBtn.setBounds(
                btnGapX + oneHrBtn.getBounds().x + oneHrBtn.getBounds().width,
                timeGp.getBounds().y + labelGap, timeBtnWidth, btnHeight);

        threeHrBtn.addListener(SWT.MouseUp, btnListeener);

        Button sixHrBtn = new Button(timeGp, SWT.RADIO);
        sixHrBtn.setText("6");
        sixHrBtn.setEnabled(true);
        sixHrBtn.setBounds(threeHrBtn.getBounds().x
                + threeHrBtn.getBounds().width + btnGapX, timeGp.getBounds().y
                + labelGap, timeBtnWidth, btnHeight);

        sixHrBtn.addListener(SWT.MouseUp, btnListeener);

        Button twelveHrBtn = new Button(timeGp, SWT.RADIO);
        twelveHrBtn.setText("12");
        twelveHrBtn.setEnabled(true);
        twelveHrBtn.setBounds(timeGp.getBounds().x + btnGapX,
                oneHrBtn.getBounds().y + oneHrBtn.getBounds().height + btnGapY,
                timeBtnWidth, btnHeight);

        twelveHrBtn.addListener(SWT.MouseUp, btnListeener);

        Button twentyfourHrBtn = new Button(timeGp, SWT.RADIO);
        twentyfourHrBtn.setText("24");
        twentyfourHrBtn.setEnabled(true);
        twentyfourHrBtn.setBounds(btnGapX + twelveHrBtn.getBounds().x
                + twelveHrBtn.getBounds().width, threeHrBtn.getBounds().y
                + threeHrBtn.getBounds().height + btnGapY, timeBtnWidth,
                btnHeight);

        twentyfourHrBtn.addListener(SWT.MouseUp, btnListeener);

        Button fourtyeightHrBtn = new Button(timeGp, SWT.RADIO);
        fourtyeightHrBtn.setText("48");
        fourtyeightHrBtn.setEnabled(true);
        fourtyeightHrBtn.setBounds(btnGapX + twentyfourHrBtn.getBounds().x
                + twentyfourHrBtn.getBounds().width, sixHrBtn.getBounds().y
                + sixHrBtn.getBounds().height + btnGapY, timeBtnWidth,
                btnHeight);

        fourtyeightHrBtn.addListener(SWT.MouseUp, btnListeener);

        Button allHrBtn = new Button(timeGp, SWT.RADIO);
        allHrBtn.setText("all");
        allHrBtn.setEnabled(true);
        allHrBtn.setBounds(btnGapX + fourtyeightHrBtn.getBounds().x
                + fourtyeightHrBtn.getBounds().width, sixHrBtn.getBounds().y
                + sixHrBtn.getBounds().height + btnGapY, timeBtnWidth,
                btnHeight);

        if (timeCovered == EReportTimeRange.ONE_HOUR)
            oneHrBtn.setSelection(true);
        else if (timeCovered == EReportTimeRange.THREE_HOURS)
            threeHrBtn.setSelection(true);
        else if (timeCovered == EReportTimeRange.SIX_HOURS)
            sixHrBtn.setSelection(true);
        else if (timeCovered == EReportTimeRange.TWELVE_HOURS)
            twelveHrBtn.setSelection(true);
        else if (timeCovered == EReportTimeRange.TWENTYFOUR_HOURS)
            twentyfourHrBtn.setSelection(true);
        else if (timeCovered == EReportTimeRange.FORTYEIGHT_HOURS)
            fourtyeightHrBtn.setSelection(true);
        else
            allHrBtn.setSelection(true);

        allHrBtn.addListener(SWT.MouseUp, btnListeener);
        createStaStnBtns(composite);
    }

    public void createStaStnBtns(Composite parent) {
        staStnGp = new Group(parent, SWT.LEFT);
        staStnGp.setText("Select By ");

        Button stationBtn = new Button(staStnGp, SWT.RADIO);
        stationBtn.setText("station");
        stationBtn.setEnabled(true);
        stationBtn.setBounds(staStnGp.getBounds().x + btnGapX,
                staStnGp.getBounds().y + labelGap, staBtnWidth, btnHeight);
        if (!isState)
            stationBtn.setSelection(true);

        stationBtn.addListener(SWT.MouseUp, new Listener() {
            public void handleEvent(Event event) {
                isState = false;
                handleStnMarkingRequestByBtn();
            }
        });
        Button stateBtn = new Button(staStnGp, SWT.RADIO);
        stateBtn.setText("state");
        stateBtn.setEnabled(true);
        if (isState)
            stateBtn.setSelection(true);
        stateBtn.setBounds(stationBtn.getBounds().x
                + stationBtn.getBounds().width + btnGapX,
                staStnGp.getBounds().y + labelGap, staBtnWidth, btnHeight);

        stateBtn.addListener(SWT.MouseUp, new Listener() {
            public void handleEvent(Event event) {
                isState = true;
                handleStnMarkingRequestByBtn();
            }
        });
    }

    public void createTextArea(Composite parent) {

        // Text display area
        text = new Text(parent, SWT.V_SCROLL | SWT.H_SCROLL);

        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        text.setLayoutData(data);
        Font font = text.getFont();
        FontData[] fontData = font.getFontData();
        for (int i = 0; i < fontData.length; i++) {
            fontData[i].setName("courier");
        }
        Font newFont = new Font(font.getDevice(), fontData);
        text.setFont(newFont);
    }

    public Text getText() {
        return text;
    }

    public void setText(Text text) {
        this.text = text;
    }

    private String removeCR(String curStr) {
        int i = ASCII_CR_VAL;
        char asciiCr = (char) i;
        String newStr = curStr.replace(asciiCr, ' ');
        return newStr;
    }

    public void createTextModeGp(Composite parent) {
        printingHandle = HandlePrinting.getPrintHandle();
        Group textModeGp = new Group(parent, SWT.SHADOW_OUT);
        textModeGp.setText("Text Mode:");

        Button replaceBtn = new Button(textModeGp, SWT.RADIO);
        replaceBtn.setText("Replace");
        replaceBtn.setEnabled(true);
        replaceBtn.setBounds(textModeGp.getBounds().x + btnGapX,
                textModeGp.getBounds().y + labelGap, staBtnWidth, btnHeight);
        replaceBtn.setSelection(true);

        replaceBtn.addListener(SWT.MouseUp, new Listener() {
            public void handleEvent(Event event) {
                replaceText = true;
                text.setText("");
                handleStnMarkingRequestByBtn();
            }
        });
        Button appendBtn = new Button(textModeGp, SWT.RADIO);
        appendBtn.setText("Append");
        appendBtn.setEnabled(true);
        appendBtn.setBounds(replaceBtn.getBounds().x
                + replaceBtn.getBounds().width + btnGapX,
                textModeGp.getBounds().y + labelGap, staBtnWidth, btnHeight);

        appendBtn.addListener(SWT.MouseUp, new Listener() {
            public void handleEvent(Event event) {
                replaceText = false;
                nextBtn.setEnabled(false);
                prevBtn.setEnabled(false);
            }
        });

        // Push buttons for Previous text info
        prevBtn = new Button(textModeGp, SWT.PUSH);
        prevBtn.setText("Previous");
        prevBtn.setEnabled(false);
        prevBtn.setBounds(appendBtn.getBounds().x + appendBtn.getBounds().width
                + btnGapX, textModeGp.getBounds().y + labelGap, staBtnWidth,
                pushbtnHeight);

        prevBtn.addListener(SWT.MouseUp, new Listener() {
            public void handleEvent(Event event) {
                // Action to display previous text report
                if ((currentTextReports != null)
                        && (currentTextReports.size() > currentTextIndex + 1)) {
                    String dispStr = removeCR((String) currentTextReports
                            .get(currentTextIndex + 1)[0]);
                    String curText = text.getText();
                    int endIndex = curText.indexOf("----");
                    if (endIndex != -1) {
                        curText = curText.substring(0, endIndex + 4);
                        text.setText(curText + NEW_LINE + dispStr);
                    } else
                        text.setText(dispStr);

                    nextBtn.setEnabled(true);
                    currentTextIndex++;
                    if (currentTextReports.size() <= currentTextIndex + 1) {
                        prevBtn.setEnabled(false);
                    }
                }
            }
        });

        // Push buttons for Next text info
        nextBtn = new Button(textModeGp, SWT.PUSH);
        nextBtn.setText("Next");
        nextBtn.setEnabled(false);
        nextBtn.setBounds(prevBtn.getBounds().x + prevBtn.getBounds().width
                + btnGapX, textModeGp.getBounds().y + labelGap, staBtnWidth,
                pushbtnHeight);

        nextBtn.addListener(SWT.MouseUp, new Listener() {
            public void handleEvent(Event event) {
                // Action to display next text report
                if ((currentTextReports != null)
                        && (currentTextReports.size() > currentTextIndex)
                        && (currentTextIndex >= 1)) {
                    String dispStr = removeCR((String) currentTextReports
                            .get(currentTextIndex - 1)[0]);
                    String curText = text.getText();
                    int endIndex = curText.indexOf("----");
                    if (endIndex != -1) {
                        curText = curText.substring(0, endIndex + 4);
                        text.setText(curText + NEW_LINE + dispStr);
                    } else
                        text.setText(dispStr);
                    prevBtn.setEnabled(true);
                    currentTextIndex--;
                    if (currentTextIndex == 0) {
                        nextBtn.setEnabled(false);
                    }
                }

            }
        });

        // Push buttons for print text info
        Button printBtn = new Button(textModeGp, SWT.PUSH);
        printBtn.setText("Print");
        printBtn.setEnabled(true);
        printBtn.setBounds(nextBtn.getBounds().x + nextBtn.getBounds().width
                + btnGapX, textModeGp.getBounds().y + labelGap, staBtnWidth,
                pushbtnHeight);

        printBtn.addListener(SWT.MouseUp, new Listener() {
            public void handleEvent(Event event) {
                // Action to print text report
                printingHandle.handlePrint(text.getText());
            }
        });

    }

    public void widgetDefaultSelected(SelectionEvent se) {

    }

    public void widgetDisposed(DisposeEvent event) {

    }

    @Override
    public void partActivated(IWorkbenchPart part) {
        if (part instanceof NctextuiPaletteWindow) {
            NctextuiResource rsc = NctextuiResource.getNctextuiResource();
            if (rsc != null)
                rsc.setEditable(true);
        }
    }

    @Override
    public void partBroughtToTop(IWorkbenchPart part) {
        partActivated(part);
    }

    @Override
    public void partClosed(IWorkbenchPart part) {
    }

    @Override
    public void partDeactivated(IWorkbenchPart part) {
    }

    @Override
    public void partOpened(IWorkbenchPart part) {
    }

    /**
     * 
     * @return the currently selected category on the palette
     */
    public String getCurrentCategory() {
        return null;
    }

    @Override
    public void setFocus() {
        // TODO Auto-generated method stub

    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        // TODO Auto-generated method stub

    }

    public void displayProduct(NctextStationInfo StnPt) {
        NatlCntrsEditor mapEditor = NctextuiResource.getMapEditor();

        if (StnPt != null && (mapEditor != null)) {
            // add RED "X" marker(s) on picked stn
            List<NctextStationInfo> rtnStateStnLst = new ArrayList<NctextStationInfo>();
            if (nctextuiPaletteWindow.isState() == true) {

                List<NctextStationInfo> stateStnLst = query
                        .getStateStationInfoList(nctextuiPaletteWindow
                                .getCurrentProductName() + StnPt.getState());
                // need to filter out those stns does not have reports in DB
                // now, use points list for reference
                for (NctextStationInfo stnInState : stateStnLst) {
                    for (NctextStationInfo stnHasRpt : points) {
                        if (stnInState.getStnid().equals(stnHasRpt.getStnid()) == true) {
                            rtnStateStnLst.add(stnInState);
                            break;
                        }
                    }
                }
            } else {
                rtnStateStnLst.add(StnPt);
            }

            // Make text bold for readability
            Text text = nctextuiPaletteWindow.getText();
            bold(text);

            if (nctextuiPaletteWindow.isReplaceText() == false) {
                // APPEND mode
                List<NctextStationInfo> prevPickedStnLst = NctextuiResource
                        .getNctextuiResource().getPickedStnPt();
                if (prevPickedStnLst.size() > 0) {
                    if (rtnStateStnLst.addAll(prevPickedStnLst) == false) {
                        return;
                    }
                }
            } else {
                // REPLACE mode
                text.setText("");
            }
            NctextuiResource.getNctextuiResource().setPickedStnPt(
                    rtnStateStnLst);
            mapEditor.refresh();
            // QUERY DB now....Object[0] = Rawrecord text data, Object[1] =
            // issuesite
            List<List<Object[]>> rptLstList = query.getProductDataListList(
                    nctextuiPaletteWindow.selectedGp,
                    nctextuiPaletteWindow.getCurrentProductName(), StnPt,
                    nctextuiPaletteWindow.getTimeCovered(),
                    nctextuiPaletteWindow.isState(), null);
            if (rptLstList.isEmpty()) {
                if (nctextuiPaletteWindow.isState())
                    text.append("--State " + StnPt.getState() + "--"
                            + nctextuiPaletteWindow.getCurrentProductName()
                            + " Report (Station picked " + StnPt.getStnid()
                            + ")\n");
                else
                    text.append("--Text-- " + ": "
                            + nctextuiPaletteWindow.getCurrentProductName()
                            + ": Reporting Station: (" + StnPt.getStnid()
                            + ") " + StnPt.getStnname() + NEW_LINE);
                if (nctextuiPaletteWindow.getTimeCovered().getTimeRange() == 0)
                    text.append("Report unavailable in database.\n");
                else
                    text.append("Report unavailable within "
                            + nctextuiPaletteWindow.getTimeCovered()
                                    .getTimeRange() + " hour(s) range.\n");

                nctextuiPaletteWindow.enablePrevBtn(false);
            } else {
                String textToDisp;
                String textRawStr;
                StringBuilder textStr;
                if (nctextuiPaletteWindow.isState()) {
                    // SelectBy State mode
                    textStr = new StringBuilder("--State " + StnPt.getState()
                            + "--"
                            + nctextuiPaletteWindow.getCurrentProductName()
                            + " Report\n");

                    for (List<Object[]> lstObj : rptLstList) {
                        textStr.append("--Station "
                                + (String) (lstObj.get(0))[1] + "-- : "
                                + nctextuiPaletteWindow.getCurrentProductName()
                                + NEW_LINE);
                        textRawStr = (String) (lstObj.get(0))[0];
                        // remove CR before displaying
                        textToDisp = removeCR(textRawStr);
                        textStr.append(textToDisp + NEW_LINE);
                    }

                    // When put text string to Text display, use "setText" but
                    // not "append" method, so, the text will show from top
                    if (nctextuiPaletteWindow.isReplaceText() == false) {
                        // get current text string from Text
                        StringBuilder textStr1 = new StringBuilder(
                                text.getText());
                        textStr1.append(textStr.toString());
                        text.setText(textStr1.toString());
                    } else
                        text.setText(textStr.toString());
                } else {
                    // SelectBy Station mode
                    // "----" used as text header delimiter
                    String textHeader = "--Text-- " + ": "
                            + nctextuiPaletteWindow.getCurrentProductName()
                            + ": Reporting Station: (" + StnPt.getStnid()
                            + ") " + StnPt.getStnname() + "----" + NEW_LINE;
                    nctextuiPaletteWindow.setCurrentTextReports(rptLstList
                            .get(0));

                    int currentTextIndex = 0;
                    nctextuiPaletteWindow.setCurrentTextIndex(currentTextIndex);
                    textRawStr = (String) (rptLstList.get(0)
                            .get(currentTextIndex))[0];
                    // remove CR before displaying
                    textToDisp = removeCR(textRawStr);
                    // When put text string to Text display, use "setText" but
                    // not "append" method, so, the text will show from top
                    if (nctextuiPaletteWindow.isReplaceText() == false) {
                        // Append mode: get current text string from Text
                        StringBuilder textStr1 = new StringBuilder(
                                text.getText());
                        textStr1.append(textHeader + textToDisp);
                        text.setText(textStr1.toString());
                    } else
                        // Replace mode
                        text.setText(textHeader + textToDisp);
                    if ((rptLstList.get(0).size() > 1)
                            && (nctextuiPaletteWindow.isReplaceText() == true)) {
                        // System.out.println("debug 1 list size "+
                        // rptLstList.get(0).size());
                        nctextuiPaletteWindow.enablePrevBtn(true);
                    } else {
                        nctextuiPaletteWindow.enablePrevBtn(false);
                    }
                }
            }
        }
    }

    /**
     * Bolds text
     * 
     * @param text
     *            text to be emboldened
     */
    private void bold(Text text) {
        FontData[] fontData = text.getFont().getFontData();
        for (int i = 0; i < fontData.length; i++) {
            fontData[i].setStyle(SWT.BOLD);
        }
        Font newFont = new Font(text.getDisplay(), fontData);
        text.setFont(newFont);
    }

}
