package gov.noaa.nws.ncep.viz.rsc.ncgrid.rsc;

import static gov.noaa.nws.ncep.viz.rsc.ncgrid.rsc.NcEnsembleResourceData.CyclePlaceholder.MAX_MODEL_CYCLES;
import gov.noaa.nws.ncep.viz.common.ui.NmapCommon;
import gov.noaa.nws.ncep.viz.common.util.CommonDateFormatUtil;
import gov.noaa.nws.ncep.viz.resources.attributes.ResourceAttrSet;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.exception.VizException;

/**
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 05/2010      277         M. Li         Initial creation 
 * 12/2011      578         G. Hull       Change to Composite on the Edit Attrs Dlg
 * 12/2011      578         G. Hull       create from seld cycle time.
 * 01/10/12                 X. Guo        Updated Attrs Dlg editor
 * 09/05/13     #1031       G. Hull       replace EnsembleComponentInventoryMngr with 
 *                                        query using NcGridInventory.
 * 03/22/2016   R10366      bkowal        Cleanup. No longer refresh the GD File name
 *                                        after every GUI action.
 * 08/18/2016   R17569      K Bugenhagen  Modified calls to NcEnsembleResourceData methods 
 *                                        since they are no longer static.
 * 
 * </pre>
 * 
 * @author mli
 * @version 1.0
 * 
 */
public class EnsembleSelectComposite extends Composite {

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(getClass());

    private NcEnsembleResourceData rscData;

    private Label weightStatus = null;

    private final int WINDOW_WIDTH = 1250;

    private final int HEIGHT_PER_LINE = 41;

    private final String NA_CYCLE = "  N/A  ";

    private final int SLIDER_INTERVAL = 5;

    private DataTime seldEnsCycleTime;

    private Text selectedModelText;

    private ScrolledComposite scrolledComposite;

    private Map<Button, String> modelExpandButtons;

    private Map<String, Composite> modelMemberListComp;

    private Map<String, GridData> modelExpandMemberGrid;

    private Map<Button, String> modelButtons;

    private Map<Button, String> primaryModelButtons;

    private Map<Slider, String> cycleSliders;

    private Map<Text, String> cyclePercentTexts;

    private Map<Button, String> cycleSelectedButtons;

    private Map<String, Date[]> modelCycleDates;

    private int totalModelCount = 0;

    private boolean gdFileUpdated = false;

    public EnsembleSelectComposite(Composite parent) {
        super(parent, SWT.SHADOW_NONE);
    }

    public void init(NcEnsembleResourceData resrcData,
            ResourceAttrSet editedRscAttrSet) throws VizException {
        rscData = resrcData;
        modelExpandButtons = new HashMap<Button, String>();
        modelExpandMemberGrid = new HashMap<String, GridData>();
        modelMemberListComp = new HashMap<String, Composite>();
        modelButtons = new HashMap<Button, String>();
        primaryModelButtons = new HashMap<Button, String>();
        cycleSliders = new HashMap<Slider, String>();
        cyclePercentTexts = new HashMap<Text, String>();
        cycleSelectedButtons = new HashMap<Button, String>();
        modelCycleDates = new HashMap<String, Date[]>();
        seldEnsCycleTime = rscData.getResourceName().getCycleTime();

        String ensCompsModels[] = rscData.getAvailableModels().split(";");

        statusHandler.debug("getAvailableModels: "
                + rscData.getAvailableModels());

        Composite comp = new Composite(this, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        comp.setLayout(gl);

        Label cycTimeLbl = new Label(comp, SWT.NONE);
        cycTimeLbl.setText("Selected Cycle Time : "
                + NmapCommon.getTimeStringFromDataTime(seldEnsCycleTime, "/")
                + "\n");

        weightStatus = new Label(comp, SWT.NONE);
        weightStatus.setText("Total Weight:        ");

        Label label = new Label(comp, SWT.NONE);
        label.setText("\n"
                + "Model                      "
                + "Primary            "
                + "Cycle1                                                 "
                + "Cycle2                                                      "
                + "Cycle3                                                    "
                + "Cycle4");

        label.setLayoutData(new GridData(WINDOW_WIDTH, SWT.DEFAULT));

        scrolledComposite = new ScrolledComposite(this, SWT.V_SCROLL
                | SWT.BORDER);
        scrolledComposite.setLayout(new GridLayout(1, true));
        GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd2.heightHint = 8 * HEIGHT_PER_LINE;
        scrolledComposite.setLayoutData(gd2);
        Composite modelListComp = new Composite(scrolledComposite, SWT.NONE);
        modelListComp.setLayout(gl);

        for (String ensCompModel : ensCompsModels) {
            ensCompModel = ensCompModel.trim();
            statusHandler.debug("trim availModels: " + ensCompModel); // GOS

            Composite modelComp = new Composite(modelListComp, SWT.NONE);
            modelComp.setLayout(new GridLayout(19, false));
            modelComp.setLayoutData(new GridData());

            addEnsModelWidgets(modelComp, ensCompModel,
                    ensCompModel.contains(":"));
            modelListComp.pack();
        }
        modelListComp.setSize(WINDOW_WIDTH, totalModelCount * HEIGHT_PER_LINE);
        scrolledComposite.setContent(modelListComp);

        createModelSelectionControls();
        String gdfile = rscData.getGdfile();
        if (gdfile == null || gdfile.length() < 3) {
            updateModelString();
        } else {
            populateGdfile(gdfile);
        }
        updateTotalWeight();
    }

    private void addEnsModelWidgets(Composite rowComp, String ensCompModel,
            boolean hasMembers) {

        if (hasMembers) {
            String model = ensCompModel.substring(0, ensCompModel.indexOf(":"));
            String[] members = ensCompModel.substring(
                    ensCompModel.indexOf(":") + 1).split(",");
            Button modelExpand = new Button(rowComp, SWT.ARROW | SWT.DOWN);
            modelExpand.setEnabled(true);
            modelExpand.addSelectionListener(new ModelExpandListener());
            modelExpandButtons.put(modelExpand, model);
            addEnsModelWidgets(rowComp, model, false);
            rowComp.pack();
            totalModelCount++;

            Composite memberListComp = new Composite(rowComp.getParent(),
                    SWT.NONE);
            memberListComp.setLayout(new GridLayout(1, false));
            GridData memberGrid = new GridData(WINDOW_WIDTH, members.length
                    * HEIGHT_PER_LINE);
            memberGrid.verticalIndent = 0;
            memberGrid.grabExcessVerticalSpace = true;
            memberGrid.verticalAlignment = SWT.TOP;
            memberListComp.setLayoutData(memberGrid);
            for (String member : members) {
                Composite memberComp = new Composite(memberListComp, SWT.NONE);
                memberComp.setLayout(new GridLayout(19, false));
                memberComp.setLayoutData(new GridData());

                addEnsModelWidgets(memberComp, model + ":" + member, false);
                memberComp.pack();
                memberListComp.pack();
            }
            modelMemberListComp.put(model, memberListComp);
            modelExpandMemberGrid.put(model, memberGrid);
        } else {
            if (rowComp.getChildren().length == 0) {
                Label label = new Label(rowComp, SWT.NONE);
                label.setText("       ");
            }
            String modelKey = ensCompModel + "|";
            Date[] cycles = rscData.queryLatestAvailCycleTimes(ensCompModel,
                    rscData.getResourceName().getCycleTime());
            modelCycleDates.put(modelKey, cycles);

            boolean modelHasData = false;
            Button modelSelect = new Button(rowComp, SWT.CHECK);
            modelSelect.setText(ensCompModel.contains(":") ? ensCompModel
                    .substring(ensCompModel.indexOf(":")) : ensCompModel);
            modelSelect.setLayoutData(new GridData(100, SWT.DEFAULT));
            modelSelect.setSelection(false);
            modelSelect.addSelectionListener(new ModelSelectListener());
            modelButtons.put(modelSelect, modelKey);

            Button primarySelect = new Button(rowComp, SWT.CHECK);
            primarySelect.setText("      ");
            primarySelect.setEnabled(false);
            primarySelect.setSelection(false);
            primarySelect
                    .addSelectionListener(new PrimaryModelSelectListener());
            primaryModelButtons.put(primarySelect, modelKey);

            for (int i = 0; i < MAX_MODEL_CYCLES; i++) {
                String cycleTimeStr, cycleKey;

                Slider slider = new Slider(rowComp, SWT.HORIZONTAL);
                slider.setEnabled(false);
                slider.setSelection(0);
                slider.setToolTipText("Percentages of all cycles must sum to 100%");
                slider.setThumb(2);
                slider.setMinimum(0);
                slider.setMaximum(100 + slider.getThumb());
                slider.setIncrement(5);
                slider.setSize(50, 100);
                slider.addSelectionListener(new CycleSliderSelectListener());
                slider.pack();

                Text percent = new Text(rowComp, SWT.SINGLE | SWT.BORDER
                        | SWT.RIGHT);
                percent.setEnabled(false);
                percent.setTextLimit(3);
                percent.setText("");
                percent.setLayoutData(new GridData(23, SWT.DEFAULT));
                percent.addSelectionListener(new CycleTextSelectListener());
                percent.addFocusListener(new CycleTextFocusListener());

                Label percentLabel = new Label(rowComp, SWT.LEFT);
                percentLabel.setEnabled(false);
                percentLabel.setText("%");

                Button cycleSelect = new Button(rowComp, SWT.CHECK);
                cycleSelect.setEnabled(false);
                cycleSelect.setSelection(false);
                cycleSelect.addSelectionListener(new CycleSelectListener());

                if (i < cycles.length) {
                    modelHasData = true;
                    cycleTimeStr = CommonDateFormatUtil
                            .getCycleTimeString(cycles[i]);
                    cycleKey = cycleTimeStr;
                } else {
                    cycleTimeStr = NA_CYCLE;
                    cycleKey = cycleTimeStr.trim() + i;
                }
                cycleKey = modelKey + cycleKey;
                cycleSliders.put(slider, cycleKey);
                cyclePercentTexts.put(percent, cycleKey);

                cycleSelect.setText(cycleTimeStr);
                cycleSelectedButtons.put(cycleSelect, cycleKey);
            }
            modelSelect.setEnabled(modelHasData);
            totalModelCount++;
        }
    }

    private void setSelectedButtonWithKey(String searchKey, boolean selected,
            Map<Button, String> buttonMap) {
        for (Entry<Button, String> buttonEntry : buttonMap.entrySet()) {
            if (buttonEntry.getValue().equals(searchKey)) {
                buttonEntry.getKey().setSelection(selected);
                break;
            }
        }
    }

    private void setAllSelectedButtonWithKey(String searchKey,
            boolean selected, Map<Button, String> buttonMap) {
        for (Entry<Button, String> buttonEntry : buttonMap.entrySet()) {
            if (buttonEntry.getValue().startsWith(searchKey)) {
                buttonEntry.getKey().setSelection(selected);
            }
        }
    }

    private Slider findCycleSlider(String cycleKey) {
        List<Slider> resultList = findAllCycleSlider(cycleKey);
        return (resultList.isEmpty() ? null : resultList.get(0));
    }

    private Text findCycleText(String cycleKey) {
        List<Text> resultList = findAllCycleText(cycleKey);
        return (resultList.isEmpty() ? null : resultList.get(0));
    }

    private List<Slider> findAllCycleSlider(String cycleKey) {
        List<Slider> returnList = new ArrayList<Slider>();
        for (Entry<Slider, String> slider : cycleSliders.entrySet()) {
            if (!slider.getValue().contains(NA_CYCLE.trim())
                    && slider.getValue().startsWith(cycleKey)) {
                returnList.add(slider.getKey());
            }
        }
        return returnList;
    }

    private List<Text> findAllCycleText(String cycleKey) {
        List<Text> returnList = new ArrayList<Text>();
        for (Entry<Text, String> percentTxt : cyclePercentTexts.entrySet()) {
            if (!percentTxt.getValue().contains(NA_CYCLE.trim())
                    && percentTxt.getValue().startsWith(cycleKey)) {
                returnList.add(percentTxt.getKey());
            }
        }
        return returnList;
    }

    /**
     * Create the locatorSelection controls.
     */
    private void createModelSelectionControls() {
        Composite modelSelectionComp = new Composite(this, SWT.NONE);
        GridLayout gl = new GridLayout(3, false);
        modelSelectionComp.setLayout(gl);

        GridData gd = new GridData(120, SWT.DEFAULT);
        Label label = new Label(modelSelectionComp, SWT.NONE);
        label.setText("Selected Models:");
        label.setLayoutData(gd);

        selectedModelText = new Text(modelSelectionComp, SWT.SINGLE
                | SWT.BORDER);
        selectedModelText.setLayoutData(new GridData(450, SWT.DEFAULT));

        Button clearAllBtn = new Button(modelSelectionComp, SWT.NONE);
        clearAllBtn.setText("Clear");
        clearAllBtn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                clearAllPercentageValues("", false);
                setModelWidgetsEnabled("", false);
                for (Button modelSelect : modelButtons.keySet()) {
                    modelSelect.setSelection(false);
                }
                updateModelString();
            }
        });
    }

    public void updateTotalWeight() {
        int newTotalWelght = 0;
        for (Slider cycleSlider : cycleSliders.keySet()) {
            newTotalWelght += cycleSlider.getSelection();
        }
        if (newTotalWelght > 100) {
            weightStatus.setForeground(getDisplay().getSystemColor(
                    SWT.COLOR_RED));
        } else if (newTotalWelght < 100) {
            weightStatus.setForeground(getDisplay().getSystemColor(
                    SWT.COLOR_DARK_YELLOW));
        } else {
            weightStatus.setForeground(getDisplay().getSystemColor(
                    SWT.COLOR_DARK_GREEN));
        }
        weightStatus.setText("Total Weight: " + newTotalWelght + "%    ");
    }

    public void updateModelString() {
        StringBuilder selectedModels = new StringBuilder();
        String primaryModelKey = "";
        for (Entry<Button, String> primaryEntry : primaryModelButtons
                .entrySet()) {
            if (primaryEntry.getKey().getSelection()) {
                primaryModelKey = primaryEntry.getValue();
                break;
            }
        }
        for (Entry<Button, String> modelEntry : modelButtons.entrySet()) {
            if (modelEntry.getKey().getSelection()) {
                StringBuilder modelBlockBuilder = new StringBuilder();
                String model = modelEntry.getValue();
                boolean cycleSelected = false;

                for (Entry<Button, String> cycleSelectEntry : cycleSelectedButtons
                        .entrySet()) {
                    String cycleKey = cycleSelectEntry.getValue();
                    if (cycleSelectEntry.getKey().getSelection()
                            && cycleKey.startsWith(model)) {
                        cycleSelected = true;
                        int percent = findCycleSlider(cycleKey).getSelection();
                        modelBlockBuilder
                                .append(percent > 0 ? percent + "%" : "")
                                .append(cycleKey).append(",");
                    } else {
                        continue;
                    }
                }
                if (!cycleSelected) {
                    modelBlockBuilder.append(model.replace("|", ""))
                            .append(",");
                }
                if (model.equals(primaryModelKey)) {
                    selectedModels.insert(0, modelBlockBuilder.toString());
                } else {
                    selectedModels.append(modelBlockBuilder.toString());
                }
            } else {
                continue;
            }
        }
        if (selectedModels.length() > 0) {
            selectedModels.deleteCharAt(selectedModels.lastIndexOf(","));
        }
        selectedModels.insert(0, "{").append("}");
        selectedModelText.setText(selectedModels.toString());
        this.gdFileUpdated = true;
    }

    public void updateGDFile() {
        if (this.gdFileUpdated) {
            /*
             * TODO: It is true that it may still take some amount of time (<=
             * 2s) before the dialog will close. However, that is what DR 10435
             * has been created to address.
             */
            final String gdFile = rscData.convertGdfileToWildcardString(
                    selectedModelText.getText(), rscData.getResourceName()
                            .getCycleTime());
            this.rscData.setGdfile(gdFile);
        }
    }

    private void populateGdfile(String gdfile) {
        gdfile = rscData.convertGdfileToCycleTimeString(gdfile, rscData
                .getResourceName().getCycleTime());
        selectedModelText.setText(gdfile);
        gdfile = gdfile.substring(1, gdfile.length() - 1);
        String[] modelBlocks = gdfile.split(",");

        for (int i = 0; i < modelBlocks.length; i++) {
            int percent = -1;
            String modelKey;
            String cycleKey;
            if (modelBlocks[i].contains("%")) {
                int percentIndex = modelBlocks[i].indexOf("%");
                percent = Integer.parseInt(modelBlocks[i].substring(0,
                        percentIndex));
                modelBlocks[i] = modelBlocks[i].substring(percentIndex + 1);
            }
            if (modelBlocks[i].contains("|")) {
                cycleKey = modelBlocks[i];
                modelKey = modelBlocks[i].substring(0,
                        modelBlocks[i].indexOf("|") + 1);
            } else {
                modelKey = modelBlocks[i] + "|";
                cycleKey = modelKey + modelCycleDates.get(modelKey)[0];
            }
            setModelWidgetsEnabled(modelKey, true);
            setSelectedButtonWithKey(modelKey, true, modelButtons);
            clearAllPercentageValues(cycleKey, true);
            setSelectedButtonWithKey(cycleKey, true, cycleSelectedButtons);
            if (percent > 0) {
                updateModelCyclePercentage(findCycleSlider(cycleKey),
                        findCycleText(cycleKey), percent);
            }
            if (i == 0) {
                setSelectedButtonWithKey(modelKey, true, primaryModelButtons);
            }
        }
    }

    private void setModelWidgetsEnabled(String model, boolean enabled) {
        for (Entry<Button, String> primary : primaryModelButtons.entrySet()) {
            if (primary.getValue().startsWith(model)) {
                primary.getKey().setEnabled(enabled);
            }
        }
        for (Entry<Button, String> selected : cycleSelectedButtons.entrySet()) {
            if (!selected.getValue().contains(NA_CYCLE.trim())
                    && selected.getValue().startsWith(model)) {
                selected.getKey().setEnabled(enabled);
            }
        }
    }

    private void updateModelCyclePercentage(Text text) {
        Slider cycleSlider = findCycleSlider(cyclePercentTexts.get(text));
        int newValue = 0;
        try {
            newValue = Integer.parseInt(text.getText().trim());
        } catch (NumberFormatException nfe) {
            newValue = 0;
        }
        updateModelCyclePercentage(cycleSlider, text, newValue);
    }

    private void updateModelCyclePercentage(Slider slider, Text text,
            int newValue) {
        slider.getParent().setFocus();
        int remainder = newValue % SLIDER_INTERVAL;
        if (newValue < 0 || newValue > 100) {
            // Value out of range!
            newValue = 0;
        }
        if (remainder != 0) {
            newValue = newValue - remainder;
            if ((float) remainder / SLIDER_INTERVAL >= .5) {
                newValue = newValue + SLIDER_INTERVAL;
            }
        }
        slider.setSelection(newValue);
        text.setText(String.valueOf(newValue));
    }

    private void clearAllPercentageValues(String key, boolean enabled) {
        for (Slider cycleSlider : findAllCycleSlider(key)) {
            cycleSlider.setEnabled(enabled);
            cycleSlider.setSelection(0);
        }
        for (Text cycleTxt : findAllCycleText(key)) {
            cycleTxt.setEnabled(enabled);
            cycleTxt.setText("");
        }
    }

    private class ModelExpandListener implements SelectionListener {

        @Override
        public void widgetSelected(SelectionEvent e) {
            Button modelExpand = (Button) e.getSource();
            String modelNameKey = modelExpandButtons.get(modelExpand);
            GridData memberListGD = modelExpandMemberGrid.get(modelNameKey);
            Composite memberListComp = modelMemberListComp.get(modelNameKey);

            if (modelExpand.getAlignment() == SWT.DOWN) {
                modelExpand.setAlignment(SWT.RIGHT);
                memberListGD.exclude = true;
                memberListComp.setVisible(false);
            } else {
                modelExpand.setAlignment(SWT.DOWN);
                memberListGD.exclude = false;
                memberListComp.setVisible(true);
            }
            memberListComp.getParent().pack();
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent e) {
        }
    }

    private class ModelSelectListener implements SelectionListener {

        @Override
        public void widgetSelected(SelectionEvent e) {
            Button modelSelected = (Button) e.getSource();
            String modelNameKey = modelButtons.get(modelSelected);

            setSelectedButtonWithKey(modelNameKey, false, primaryModelButtons);
            setAllSelectedButtonWithKey(modelNameKey, false,
                    cycleSelectedButtons);
            clearAllPercentageValues(modelNameKey, false);
            setModelWidgetsEnabled(modelNameKey, modelSelected.getSelection());

            updateModelString();
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent e) {
        }
    }

    private class PrimaryModelSelectListener implements SelectionListener {

        @Override
        public void widgetSelected(SelectionEvent e) {
            Button modelSelected = (Button) e.getSource();
            for (Button primaryButton : primaryModelButtons.keySet()) {
                primaryButton.setSelection(false);
            }
            modelSelected.setSelection(true);
            updateModelString();
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent e) {
        }
    }

    private class CycleSelectListener implements SelectionListener {

        @Override
        public void widgetSelected(SelectionEvent e) {
            Button cycleSelected = (Button) e.getSource();
            String cycleKey = cycleSelectedButtons.get(cycleSelected);

            clearAllPercentageValues(cycleKey, cycleSelected.getSelection());

            updateModelString();
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent e) {
        }
    }

    private class CycleSliderSelectListener implements SelectionListener {

        @Override
        public void widgetSelected(SelectionEvent e) {
            Slider cycleSlider = (Slider) e.getSource();
            Text cycleText = findCycleText(cycleSliders.get(cycleSlider));

            updateModelCyclePercentage(cycleSlider, cycleText,
                    cycleSlider.getSelection());

            updateTotalWeight();
            updateModelString();
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent e) {
        }
    }

    private class CycleTextSelectListener implements SelectionListener {

        @Override
        public void widgetSelected(SelectionEvent e) {
            Text cycleText = (Text) e.getSource();

            updateModelCyclePercentage(cycleText);

            updateTotalWeight();
            updateModelString();
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent e) {
            widgetSelected(e);
        }
    }

    private class CycleTextFocusListener implements FocusListener {

        @Override
        public void focusGained(FocusEvent e) {
            // Do nothing
        }

        @Override
        public void focusLost(FocusEvent e) {
            Text cycleText = (Text) e.getSource();

            updateModelCyclePercentage(cycleText);

            updateTotalWeight();
            updateModelString();
        }

    }
}
