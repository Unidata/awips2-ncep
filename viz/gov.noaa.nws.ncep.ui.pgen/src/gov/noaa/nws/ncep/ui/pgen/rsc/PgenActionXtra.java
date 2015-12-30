package gov.noaa.nws.ncep.ui.pgen.rsc;

import gov.noaa.nws.ncep.ui.pgen.PgenConstant;
import gov.noaa.nws.ncep.ui.pgen.PgenSession;
import gov.noaa.nws.ncep.ui.pgen.contours.ContourMinmax;
import gov.noaa.nws.ncep.ui.pgen.elements.AbstractDrawableComponent;
import gov.noaa.nws.ncep.ui.pgen.elements.DECollection;
import gov.noaa.nws.ncep.ui.pgen.elements.Text;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;

/**
 * The class PgenActionXtra provides methods for processing PGen context menu
 * choices that only get included in the context menu conditionally. Lists of
 * conditionally included PGen context menu items can be found in the plugin.xml
 * for ncep.ui.pgen in the "actionsxtra" attribute.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 13, 2015 R8198      srussell     Initial creation
 * Sep 01, 2015 R11365     srussell     added a new form of getActionsXtra()
 *                                      that returns a List<String> for a 
 *                                      specified PgenType and does not apply
 *                                      rules for inclusion to a context menu.
 * 
 * </pre>
 * 
 * @author srussell
 * @version 1.0
 */

public class PgenActionXtra {

    /**
     * Entry point, most other methods in this class are an accessory to this
     * one
     * 
     * @param AbstractDrawableComponent
     *            adc, currently selected drawable element
     * 
     * @return A list of conditional context menu choices concatenated in a a
     *         String. These context menu choices have passed testing for
     *         inclusion on the context menu
     */

    public static String getActionsXtra(AbstractDrawableComponent adc) {
        String actionsxtra = null;
        String pgenType = adc.getPgenCategory();

        HashMap<String, IConfigurationElement> itemMap = PgenSession
                .getInstance().getPgenPalette().getItemMap();

        IConfigurationElement ice = itemMap.get(pgenType);

        if (ice != null) {
            actionsxtra = ice.getAttribute(PgenConstant.ACTIONSXTRA);
            actionsxtra = (actionsxtra != null) ? actionsxtra.trim()
                    : actionsxtra;
        }

        // Remove any conditional context menu items that do not apply given the
        // current state of the application
        if (actionsxtra != null) {
            actionsxtra = applyRulesToActionsExtra(adc, actionsxtra);
        }

        return actionsxtra;

    }

    /**
     * Get a List of Pgen context menu choices (actionsxtra ) associated with a
     * PgenType ( aka the "name" attribute in the plugin.xml ) represented as an
     * IConfigurationElement input argument
     * 
     * @param IConfigurationElement
     *            ice,
     * 
     * @return List<String> context menu choices, actionsxtra, Pgen actions that
     *         never appear as buttons for PgenType represented by the input
     *         argument
     */

    public static List<String> getActionsXtra(IConfigurationElement ice) {
        String actionsxtra = null;
        List<String> actionsxtraList = null;

        if (ice != null) {
            actionsxtra = ice.getAttribute(PgenConstant.ACTIONSXTRA);
            actionsxtra = (actionsxtra != null) ? actionsxtra.trim()
                    : actionsxtra;
        }

        if (actionsxtra != null && !actionsxtra.isEmpty()) {
            actionsxtraList = Arrays.asList(actionsxtra
                    .split(PgenConstant.PLUGINXML_ATTRIBUTE_DELIMETER));
        }

        return actionsxtraList;

    }

    /**
     * Determines which rules (method) should be applied to the retrieved list
     * of conditional context menu choices.
     * 
     * @param AbstractDrawableComponent
     *            adc - the currently selected Pgen obj
     * @param String
     *            actionsxtra - the retrieved conditional context menu choices
     * 
     * @return String the list of conditional context menu choices with the
     *         choices that do not apply removed.
     * 
     */

    private static String applyRulesToActionsExtra(
            AbstractDrawableComponent adc, String actionsxtra) {

        String xtraMenuChoices = null;

        // If the currently selected element is a non-met,non-contour symbol
        if (adc.getPgenCategory().equalsIgnoreCase(PgenConstant.SYMBOL)) {
            if (!(adc.getParent() instanceof ContourMinmax)) {
                // Apply this particular rule method for this situation
                xtraMenuChoices = applyRulesLabeledSymbols(adc, actionsxtra);
            }
        }
        return xtraMenuChoices;
    }

    /**
     * Decides which conditional context menu items to include on the context
     * menu for a selected "labeledSymbol". A "labeledSymbol" is a non-met,
     * non-contour symbol that is housed in a DECollection object. Inside the
     * DECollection object is a DrawableElement obj for the symbol and a Text
     * object ( with String[] ) for the label. Unlabled symbols are simply lone
     * DrawableElement objects.
     * 
     * @param AbstractDrawableComponent
     *            adc - currently selected "labeledSymbol"
     * @param String
     *            actionsxtra - list of conditional context menu choices
     * @return String list of conditional context menu choices that have passed
     *         the test for "labeledSymbols"
     */

    private static String applyRulesLabeledSymbols(
            AbstractDrawableComponent adc, String actionsxtra) {

        boolean hasLabel = false;
        StringBuffer sb = new StringBuffer();

        List<String> menuoptions = Arrays.asList(actionsxtra
                .split(PgenConstant.PLUGINXML_ATTRIBUTE_DELIMETER));

        Text text = getDELabel((DECollection) adc.getParent());

        String[] label = (text != null) ? text.getText() : new String[0];

        if (label.length > 0) {
            hasLabel = true;
        }

        // For each conditional context menu item in the retrieved list
        for (int i = 0; i < menuoptions.size(); i++) {

            String menuoption = menuoptions.get(i);

            if (menuoption.equalsIgnoreCase(PgenConstant.ADD_LABEL)) {
                // No existing label for the symbol
                if (!hasLabel) {
                    // Add "Add Label" to the context menu for the symbol
                    sb.append(menuoption);
                    if (i < (menuoptions.size() - 1)) {
                        sb.append(",");
                    }
                }
            } else if (menuoption.equalsIgnoreCase(PgenConstant.DELETE_LABEL)) {
                // Symbol has a label to remove
                if (hasLabel) {
                    // Add "Delete Label" to the context menu for the symbol
                    sb.append(menuoption);
                    if (i < (menuoptions.size() - 1)) {
                        sb.append(",");
                    }
                }
            }

        }

        return sb.toString();

    }

    /**
     * Retrieve the first Text obj from a DECollection obj.
     * 
     * Non-met, non-contour symbols that are labeled or that can be labeled are
     * instantiated as DECollection objects that hold one symbol as a
     * DrawableElement obj and that hold one Text obj to hold a label ( a
     * String[] ). Note: this method is public as it is used in other Pgen
     * classes.
     * 
     * @param DECollection
     *            labeledSymbol
     * 
     * @return Text - the Text object holding a label, or null if there is none.
     */
    public static Text getDELabel(DECollection dec) {

        Text label = null;
        Iterator<AbstractDrawableComponent> it = dec.getComponentIterator();

        while (it.hasNext()) {
            AbstractDrawableComponent item = it.next();
            if (item instanceof Text) {
                label = (Text) item;
                return label;
            }
        }

        return label;

    }

}
