package gov.noaa.nws.ncep.viz.rsc.ncgrid.rsc;

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

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBException;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.SingleTypeJAXBManager;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 * Manages the configuration file for NcgridResource.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 20, 2018 54493      E. Debebe   Initial creation
 *
 * </pre>
 *
 * @author edebebe
 */
public class NcgridResourceManager {

    private static final Object INSTANCE_LOCK = new Object();

    private static NcgridResourceManager instance;

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(getClass());
    
    private NcgridResourceConfig config = null;
    

    /**
     * The localization path of the NcgridResource config file.
     */
    public static final String CONFIG_PATH = "ncep" + IPathManager.SEPARATOR
            + "ncgrid" + IPathManager.SEPARATOR + "ncgridResourceConfig.xml";

    /**
     * Private constructor for singleton.
     */
    private NcgridResourceManager() {
        
        this.config = readConfig();

    }

    /**
     * Gets the singleton manager instance, creating it if necessary.
     *
     * @return the {@link NcgridResourceManager} singleton
     */
    public static NcgridResourceManager getInstance() {
        synchronized (INSTANCE_LOCK) {
            if (instance == null) {
                instance = new NcgridResourceManager();
            }
            return instance;
        }
    }

    
    /**
     * Read the values from the config file
     * 
     * @return
     */
    private NcgridResourceConfig readConfig() {
        ILocalizationFile configFile = PathManagerFactory.getPathManager()
                .getStaticLocalizationFile(NcgridResourceManager.CONFIG_PATH);
        NcgridResourceConfig config = null;
        try (InputStream is = configFile.openInputStream()) {
            SingleTypeJAXBManager<NcgridResourceConfig> jaxb = new SingleTypeJAXBManager<>(
                    NcgridResourceConfig.class);
            config = jaxb.unmarshalFromInputStream(is);
        } catch (JAXBException | IOException | LocalizationException
                | SerializationException e) {
            statusHandler.error("Error reading NcgridResource config file: "
                    + NcgridResourceManager.CONFIG_PATH, e);
        }

        return config;
    }
    
    /**
     * Returns the config object
     * 
     * @return
     */
    public NcgridResourceConfig getConfig() {
        
        return config;
    } 
}
