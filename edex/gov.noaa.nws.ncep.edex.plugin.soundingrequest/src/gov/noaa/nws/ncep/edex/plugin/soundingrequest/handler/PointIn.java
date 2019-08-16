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

package gov.noaa.nws.ncep.edex.plugin.soundingrequest.handler;

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.dataplugin.HDF5Util;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.common.dataplugin.persist.IPersistable;
import com.raytheon.uf.common.datastorage.DataStoreFactory;
import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.datastorage.records.FloatDataRecord;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.edex.database.plugin.PluginDao;
import com.raytheon.uf.edex.database.plugin.PluginFactory;

/**
 *
 * PointIn task derived from original uEngine PointIn task. Reads a file in from
 * the data store.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer   Description
 * ------------- -------- ---------- -------------------------------------------
 * Mar 29, 2007           njensen    Initial Creation
 * Mar 28, 2012           Chin Chen  Add new APIs to support query multiple
 *                                   Points at one shoot and using
 *                                   dataStore.retrieveGroups()
 * Oct 15, 2012  2473     bsteffen   Remove unused imports
 * May 26, 2015           chin chen  remove debug message
 * May 26, 2015  8306     Chin Chen  eliminate NSHARP dependence on uEngine.
 *                                   Copy whole file PointIn.java from uEngine
 *                                   project to this serverRequestService
 *                                   project. "refactor" and clean up unused
 *                                   code for this ticket.
 * Jul 12, 2019           tjensen    Refactor getHDFGroupDataPoints to support
 *                                   retrieval from multiple HDF5 files
 *
 * </pre>
 *
 * @author njensen
 */

public class PointIn {

    private static final Logger logger = LoggerFactory.getLogger(PointIn.class);

    private final PluginDataObject dataRecord;

    private PluginDao dao;

    private final int indX;

    private final int indY;

    /**
     * Constructor
     *
     * @param aPlugin
     *            the plugin
     * @param aDataRecord
     *            the data record to read in
     */
    public PointIn(String aPlugin, PluginDataObject aDataRecord, int xInd,
            int yInd) {
        dataRecord = aDataRecord;
        indX = xInd;
        indY = yInd;
        try {
            dao = PluginFactory.getInstance().getPluginDao(aPlugin);
        } catch (PluginException e) {
            logger.error("Unable to get " + dataRecord.getPluginName() + " dao",
                    e);
        }
    }

    public PointIn(String aPlugin, PluginDataObject aDataRecord) {
        dataRecord = aDataRecord;
        indX = 0;
        indY = 0;
        try {
            dao = PluginFactory.getInstance().getPluginDao(aPlugin);
        } catch (PluginException e) {
            logger.error("Unable to get " + dataRecord.getPluginName() + " dao",
                    e);
        }
    }

    public float getPointData() throws PluginException {
        return ((FloatDataRecord) getHDF5DataPoint(dataRecord, indX, indY))
                .getFloatData()[0];
    }

    public IDataRecord getHDF5DataPoint(PluginDataObject object, int xInd,
            int yInd) throws PluginException {

        Request pointRequest = Request.buildPointRequest(new Point(xInd, yInd));
        IDataRecord[] dr = null;

        if (object instanceof IPersistable) {
            IDataStore dataStore = dao.getDataStore((IPersistable) object);
            try {
                String[] groups = new String[1];
                groups[0] = object.getDataURI();
                dr = dataStore.retrieveGroups(groups, pointRequest);
            } catch (Exception e) {
                throw new PluginException("Error getting HDF5 data", e);
            }
        }
        if (dr == null) {
            throw new PluginException("Error getting HDF5 data");
        }
        return dr[0];
    }

    /**
     * Gets the file path of an hdf5 file
     *
     * @param record
     *            the record
     * @param type
     *            the type of record
     * @return the path to the file
     */
    private static String getFilename(PluginDataObject record, String type) {
        String filename = null;
        if (record != null) {
            File file = HDF5Util.findHDF5Location(record);
            if (file != null) {
                filename = file.getPath();
            }
        }
        return filename;
    }

    /**
     * This API is to query grid data for multiple Points and multiple
     * parameters. Parameters can be same parameter but at different pressure
     * level. They will be treated as different parameters.
     *
     * @param objects
     *            :parameters to be query
     * @param points
     *            : query locations, they are index in a 2 dimensional grid (can
     *            not use lat/lon directly). Use PointUtil.determineIndex to
     *            convert from lat/lon to Point.
     * @param newOrderedObjects
     *            : parameters that were queried in the order used by the return
     *            object
     *
     * @return List of float arrays, with each array containing the values for
     *         the parameters for each point requested. List is in order the
     *         points were passed in; array is in order the of the
     *         newObjectOrder that is returned.
     */
    public static List<float[]> getHDF5GroupDataPoints(Object[] objects,
            List<Point> points, List<PluginDataObject> newOrderedObjects)
            throws PluginException {
        Request pointRequest = (Request
                .buildPointRequest(points.toArray(new Point[points.size()])));
        Map<String, List<PluginDataObject>> fileMap = new HashMap<>();
        Map<Point, List<Float>> rdata = new HashMap<>();

        // Determine which files we need to retrieve data from
        for (Object o : objects) {
            PluginDataObject pdo = (PluginDataObject) o;
            String file = getFilename(pdo, pdo.getPluginName());
            if (file != null) {
                List<PluginDataObject> objs = fileMap.get(file);
                if (objs == null) {
                    objs = new ArrayList<>();
                    fileMap.put(file, objs);
                }
                objs.add(pdo);
            }
        }

        // Loop over each file and retrieve the data for the specified points
        for (Entry<String, List<PluginDataObject>> entry : fileMap.entrySet()) {
            String file = entry.getKey();
            List<PluginDataObject> objs = entry.getValue();

            String[] groups = new String[objs.size()];
            int i = 0;
            for (PluginDataObject obj : objs) {
                groups[i] = obj.getDataURI();
                newOrderedObjects.add(obj);
                i++;
            }
            IDataRecord[] dr = null;

            try {
                IDataStore ds = DataStoreFactory.getDataStore(new File(file));
                dr = ds.retrieveGroups(groups, pointRequest);

                for (IDataRecord element : dr) {
                    float[] data = (float[]) element.getDataObject();

                    for (i = 0; i < data.length; i++) {
                        List<Float> pData = rdata.get(points.get(i));
                        if (pData == null) {
                            pData = new ArrayList<>();
                            rdata.put(points.get(i), pData);
                        }
                        pData.add(data[i]);
                    }
                }

            } catch (Exception e) {
                throw new PluginException("Error getting HDF5 data", e);
            }
        }

        // Organize the data in the order the callers expect to retrieve it
        List<float[]> rval = new ArrayList<>();
        for (Point pt : points) {
            List<Float> data = rdata.get(pt);
            float[] fdata = new float[data.size()];
            for (int f = 0; f < data.size(); f++) {
                fdata[f] = data.get(f).floatValue();
            }
            rval.add(fdata);
        }

        return rval;
    }

}