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
package com.raytheon.uf.viz.gempak.cave;

import java.io.File;
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
import com.raytheon.uf.viz.gempak.cave.config.GempakProcessingConfig;
import com.raytheon.uf.viz.gempak.cave.strategy.GempakSameProcessStrategy;
import com.raytheon.uf.viz.gempak.cave.strategy.GempakSubprocessStrategy;
import com.raytheon.uf.viz.gempak.cave.strategy.IGempakProcessingStrategy;
import com.raytheon.uf.viz.gempak.common.data.GempakDataInput;
import com.raytheon.uf.viz.gempak.common.data.GempakDataRecord;
import com.raytheon.uf.viz.gempak.common.exception.GempakException;
import com.raytheon.uf.viz.gempak.common.util.GempakProcessingUtil;

/**
 * Manages processing data through GEMPAK.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 05, 2018 54480      mapeters    Initial creation
 * Sep 26, 2018 54483      mapeters    Propagate exception in
 *                                     {@link #getDataRecord(GempakDataInput)}
 * Oct 08, 2018 54483      mapeters    Moved from gov.noaa.nws.ncep.viz.rsc.ncgrid.gempak,
 *                                     only use subprocesses if subprocess RPM installed
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
         * Use subprocess strategy if config exists and specifies it and the
         * subprocess RPM is installed, otherwise default to same process
         * strategy
         */
        boolean useSubprocessStrategy = false;
        if (config != null && config.isRunInSubprocess()) {
            boolean gempakRpmInstalled = new File(
                    GempakProcessingUtil.SUBPROCESS_SCRIPT_PATH).isFile();
            if (gempakRpmInstalled) {
                useSubprocessStrategy = true;
            } else {
                statusHandler
                        .warn("GEMPAK processing is configured to be performed in "
                                + "subprocesses, but the GEMPAK RPM is not installed");
            }
        }

        if (useSubprocessStrategy) {
            int subprocessLimit = GempakProcessingUtil
                    .validateSubprocessLimit(config.getSubprocessLimit());
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
         * TODO listen for localization file changes?
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
     * @throws GempakException
     *             if an error occurs processing the data
     */
    public GempakDataRecord getDataRecord(GempakDataInput dataInput)
            throws GempakException {
        return processingStrategy.getDataRecord(dataInput);
    }

    private GempakProcessingConfig readConfig() {
        ILocalizationFile configFile = PathManagerFactory.getPathManager()
                .getStaticLocalizationFile(GempakProcessingUtil.CONFIG_PATH);
        GempakProcessingConfig config = null;
        try (InputStream is = configFile.openInputStream()) {
            SingleTypeJAXBManager<GempakProcessingConfig> jaxb = new SingleTypeJAXBManager<>(
                    GempakProcessingConfig.class);
            config = jaxb.unmarshalFromInputStream(is);
        } catch (JAXBException | IOException | LocalizationException
                | SerializationException e) {
            statusHandler.error("Error reading GEMPAK config file: "
                    + GempakProcessingUtil.CONFIG_PATH, e);
        }

        return config;
    }
}
