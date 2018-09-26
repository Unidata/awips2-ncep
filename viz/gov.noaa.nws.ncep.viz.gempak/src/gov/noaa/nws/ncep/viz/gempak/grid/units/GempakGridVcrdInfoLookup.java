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
package gov.noaa.nws.ncep.viz.gempak.grid.units;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.serialization.SingleTypeJAXBManager;

import gov.noaa.nws.ncep.common.log.logger.NcepLogger;
import gov.noaa.nws.ncep.common.log.logger.NcepLoggerManager;
import gov.noaa.nws.ncep.viz.localization.NcPathManager;

/**
 * Handles looking up GEMPAK VCORD information.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 *                                     Initial creation
 * Sep 13, 2018 54483      mapeters    Sync {@link #getInstance()}, cleanup
 *
 * </pre>
 *
 * @author mapeters
 */
public class GempakGridVcrdInfoLookup {

    public static final String GRID_GEMPAK_VCORD_FILE = "ncep"
            + IPathManager.SEPARATOR + "GempakGridUnits"
            + IPathManager.SEPARATOR + "gempakGridVcrdUnits.xml";

    private static final Object INSTANCE_LOCK = new Object();

    /** The logger */
    private static NcepLogger logger = NcepLoggerManager
            .getNcepLogger(GempakGridVcrdInfoLookup.class);

    /** The singleton instance of GridLookupFileName **/
    private static GempakGridVcrdInfoLookup instance;

    private final Map<String, GempakGridVcrdInfo> vcrdInfo;

    public static GempakGridVcrdInfoLookup getInstance() {
        synchronized (INSTANCE_LOCK) {
            if (instance == null) {
                instance = new GempakGridVcrdInfoLookup();
            }
            return instance;
        }
    }

    private GempakGridVcrdInfoLookup() {
        vcrdInfo = new HashMap<>();
        try {
            initVcordInfo();
        } catch (IOException e) {
            logger.error("Unable to initialize gempak vcord information list!",
                    e);
        }
    }

    private void initVcordInfo() throws IOException {
        ILocalizationFile gempakVcrdInfo = NcPathManager.getInstance()
                .getStaticLocalizationFile(GRID_GEMPAK_VCORD_FILE);
        if (!gempakVcrdInfo.exists()) {
            return;
        }
        try (InputStream is = gempakVcrdInfo.openInputStream()) {
            SingleTypeJAXBManager<GempakGridVcrdInfoSet> jaxb = new SingleTypeJAXBManager<>(
                    GempakGridVcrdInfoSet.class);
            GempakGridVcrdInfoSet vcrdInfoList = jaxb
                    .unmarshalFromInputStream(is);
            for (GempakGridVcrdInfo vcrd : vcrdInfoList.getVcordinfo()) {
                vcrdInfo.put(vcrd.getGnam(), vcrd);
            }
        } catch (Exception e) {
            throw new IOException(
                    "Unable to unmarshal ncep gempak vcrd info file");
        }
    }

    public String getVcrdUnit(String parm) {
        String units = null;
        GempakGridVcrdInfo pInfo = vcrdInfo.get(parm);
        if (pInfo != null) {
            units = pInfo.getUnits();
        }
        return units;
    }

    public int getParmScale(String parm) {
        int scale = 0;
        GempakGridVcrdInfo pInfo = vcrdInfo.get(parm);
        if (pInfo != null) {
            scale = pInfo.getScale();
        }
        return scale;
    }

    public void display() {
        int cnt = 1;
        logger.info("Size of vcrd table:" + vcrdInfo.size());
        for (GempakGridVcrdInfo parm : vcrdInfo.values()) {
            logger.info("No." + cnt + " name:" + parm.getName() + " gname:"
                    + parm.getGnam() + " units:" + parm.getUnits() + " scale:"
                    + parm.getScale());
            cnt++;
        }
    }
}
