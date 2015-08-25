package gov.noaa.nws.ncep.viz.common.ui;

import gov.noaa.nws.ncep.viz.common.Activator;
import gov.noaa.nws.ncep.viz.common.preferences.NcepGeneralPreferencesPage;
import gov.noaa.nws.ncep.viz.common.preferences.NcepPreferences;
import gov.noaa.nws.ncep.viz.localization.NcPathManager.NcPathConstants;

import org.eclipse.jface.preference.IPreferenceStore;

import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.viz.core.ProgramArguments;

/**
 * This class is the placeholder for the NC perspective to perform
 * initialization during the Spring Framework initialization.
 * 
 * The first version includes the logic related to the Desk level,
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 08/06/2015     R8015     A. Su       Initial creation.
 * 
 * </pre>
 * 
 * @author asu
 * @version 1
 * 
 */

public class StartupInitialization {

    public static final String DESK_ARG = "-desk";

    private static IPreferenceStore ncPrefStore = Activator.getDefault()
            .getPreferenceStore();

    // Added this to make it more clear that this plugin's store is used for
    // all Ncep preferences
    public static IPreferenceStore getNcepPreferenceStore() {
        return ncPrefStore;
    }

    static {

        ncPrefStore.setDefault(NcepGeneralPreferencesPage.PromptOnDisplayClose,
                false);
        ncPrefStore.setDefault(
                NcepGeneralPreferencesPage.ShowLatestResourceTimes, true);
        ncPrefStore.setDefault(
                NcepGeneralPreferencesPage.OnlyShowResourcesWithData, true);

        String desk = ProgramArguments.getInstance().getString(DESK_ARG);

        if (desk != null && !desk.trim().isEmpty()) {
            desk = desk.trim().toUpperCase();
            System.out.println("Setting Desk to " + desk
                    + " from Program Arguement.");
        } else {
            desk = "NONE";
        }

        ncPrefStore.setDefault(NcepPreferences.DeskNamePref, desk);

        // SITE < DESK < USER
        // NOTE : order of 650 is between SITE(order=500) and USER(order=1000).
        LocalizationLevel DESK = LocalizationLevel.createLevel(
                NcPathConstants.DESK_LEVEL, 650);

        // sanity check to make sure the order is correct
        //
        if (LocalizationLevel.SITE.compareTo(DESK) >= 0) {
            System.out
                    .println("WARNING: the SITE level order >= the DESK???? ");
        }
        if (LocalizationLevel.USER.compareTo(DESK) <= 0) {
            System.out
                    .println("WARNING: the USER level order <= the DESK???? ");
        }
    }
}
