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
package gov.noaa.nws.ncep.viz.rsc.ncgrid.gempak;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBException;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.SingleTypeJAXBManager;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 * Manages processing data through GEMPAK.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 05, 2018 7417       mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public class GempakProcessingManager {

    private static final Object INSTANCE_LOCK = new Object();

    private static GempakProcessingManager instance;

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(getClass());

    private final IGempakProcessingStrategy processingStrategy;

    /**
     * Private constructor for singleton.
     */
    private GempakProcessingManager() {
        GempakProcessingConfig config = readConfig();

        /*
         * Use subprocess strategy if config exists and specifies it, otherwise
         * default to same process strategy
         */
        if (config != null && config.isRunAsSubprocess()) {
            int subprocessLimit = config.getSubprocessLimit();
            statusHandler.info("GEMPAK processing will be performed in "
                    + "subprocesses, with a limit of " + subprocessLimit
                    + " concurrent subprocesses");
            processingStrategy = new GempakSubprocessStrategy(subprocessLimit);
        } else {
            statusHandler.info(
                    "GEMPAK processing will be performed in the CAVE process");
            processingStrategy = new GempakSameProcessStrategy();
        }
        /*
         * TODO Force to run in CAVE process if appropriate scripts aren't
         * installed (e.g. cave.sh) so that this still works on dev boxes. Also,
         * listen for localization file changes?
         */
    }

    /**
     * Gets the singleton manager instance, creating it if necessary.
     *
     * @return the {@link GempakProcessingManager} singleton
     */
    public static GempakProcessingManager getInstance() {
        synchronized (INSTANCE_LOCK) {
            if (instance == null) {
                instance = new GempakProcessingManager();
            }
            return instance;
        }
    }

    /**
     * Process the data input through GEMPAK and return the data record result.
     *
     * @param dataInput
     *            the data to process
     * @return the processed data record
     */
    public GempakDataRecord getDataRecord(GempakDataInput dataInput) {
        return processingStrategy.getDataRecord(dataInput);
    }

    private GempakProcessingConfig readConfig() {
        ILocalizationFile configFile = PathManagerFactory.getPathManager()
                .getStaticLocalizationFile(
                        GempakProcessingConstants.CONFIG_PATH);
        GempakProcessingConfig config = null;
        try (InputStream is = configFile.openInputStream()) {
            SingleTypeJAXBManager<GempakProcessingConfig> jaxb = new SingleTypeJAXBManager<>(
                    GempakProcessingConfig.class);
            config = jaxb.unmarshalFromInputStream(is);
        } catch (JAXBException | IOException | LocalizationException
                | SerializationException e) {
            statusHandler.error("Error reading GEMPAK config file: "
                    + GempakProcessingConstants.CONFIG_PATH, e);
        }
        return config;
    }
}
