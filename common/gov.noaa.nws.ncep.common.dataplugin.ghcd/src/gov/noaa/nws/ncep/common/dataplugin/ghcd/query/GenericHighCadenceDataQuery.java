/**
 * This code has unlimited rights, and is provided "as is" by the National Centers 
 * for Environmental Prediction, without warranty of any kind, either expressed or implied, 
 * including but not limited to the implied warranties of merchantability and/or fitness 
 * for a particular purpose.
 * 
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 */
package gov.noaa.nws.ncep.common.dataplugin.ghcd.query;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.raytheon.edex.uengine.tasks.query.TableQuery;
import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.common.datastorage.records.FloatDataRecord;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.datastorage.records.IntegerDataRecord;
import com.raytheon.uf.common.datastorage.records.LongDataRecord;
import com.raytheon.uf.common.datastorage.records.StringDataRecord;
import com.raytheon.uf.common.pointdata.PointDataContainer;
import com.raytheon.uf.common.pointdata.PointDataView;
import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.pointdata.DbParameterDescription;
import com.raytheon.uf.edex.pointdata.PointDataDbDescription;
import com.raytheon.uf.edex.pointdata.PointDataQuery;

/**
 * A query task for accessing generic high cadence data.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer    Description
 * -------      -------     --------    -----------
 * 01/22/2014   1100        sgurung     Initial creation
 * 
 * </pre>
 * 
 * @author sgurung
 * @version 1.0
 * 
 */
public class GenericHighCadenceDataQuery extends PointDataQuery {

    protected TableQuery tq;

    public GenericHighCadenceDataQuery(String plugin)
            throws DataAccessLayerException, PluginException {
        super(plugin);
    }

    private List<Map<String, Object>> performDbQuery(final List<String> fields,
            final int limit) throws Exception {

        for (String field : fields) {
            query.addReturnedField(field);
        }
        query.setMaxResults(limit);
        List<?> queryResults = dao.queryByCriteria(query);

        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();

        for (Object o : queryResults) {
            Map<String, Object> workingMap = new HashMap<String, Object>();
            if (o instanceof Object[]) {
                Object[] oArr = (Object[]) o;
                for (int i = 0; i < fields.size(); i++) {
                    workingMap.put(fields.get(i), oArr[i]);
                }
            } else if (fields.size() == 1) {
                workingMap.put(fields.get(0), o);
            }
            results.add(workingMap);
        }

        return results;

    }

    @Override
    public PointDataContainer execute() throws Exception {
        List<String> hdf5attribList = new ArrayList<String>();
        HashSet<String> dbAttribSet = new HashSet<String>();
        List<DbParameterDescription> dbParamDesc = new ArrayList<DbParameterDescription>();

        PointDataDbDescription dbDesc = dao.getPointDataDbDescription();
        if (dbDesc == null) {
            hdf5attribList.addAll(Arrays.asList(attribs));
        } else {
            for (String attrib : attribs) {
                DbParameterDescription desc = dbDesc.getDescription(attrib);
                if (desc != null) {
                    dbAttribSet.add(desc.getQueryName());
                    dbParamDesc.add(desc);
                } else {
                    hdf5attribList.add(attrib);
                }
            }
        }

        dbAttribSet.add("id");
        if (!hdf5attribList.isEmpty()) {
            dbAttribSet.add("pointDataView.curIdx");
            dbAttribSet.addAll(Arrays.asList(dao.getKeysRequiredForFileName()));
        }

        List<Map<String, Object>> dbResults = performDbQuery(
                new ArrayList<String>(dbAttribSet), 999999);

        if ((dbResults == null) || dbResults.isEmpty()) {
            return null;
        }

        Map<Integer, Map<String, Object>> dbResultMap = new HashMap<Integer, Map<String, Object>>();
        PointDataContainer masterPDC = null;

        if (hdf5attribList.isEmpty()) {

            int[] idArr = new int[dbResults.size()];
            for (int j = 0; j < dbResults.size(); j++) {
                Map<String, Object> workingMap = dbResults.get(j);
                idArr[j] = (Integer) workingMap.get("id");
                dbResultMap.put(idArr[j], workingMap);
            }
            masterPDC = PointDataContainer
                    .build(new IDataRecord[] { new IntegerDataRecord("id", "",
                            idArr) });
            masterPDC.setCurrentSz(masterPDC.getAllocatedSz());
        } else {
            List<String> files = new ArrayList<String>();
            List<List<Integer>> ids = new ArrayList<List<Integer>>();
            List<List<Integer>> indexes = new ArrayList<List<Integer>>();

            for (Map<String, Object> workingMap : dbResults) {
                int id = (Integer) workingMap.get("id");
                int idx = (Integer) workingMap.get("pointDataView.curIdx");
                dbResultMap.put(id, workingMap);
                String fileName = dao.getPointDataFileName(workingMap);

                int listIndex = files.indexOf(fileName);
                if (listIndex == -1) {
                    listIndex = files.size();
                    files.add(fileName);
                    ids.add(new ArrayList<Integer>());
                    indexes.add(new ArrayList<Integer>());
                }
                ids.get(listIndex).add(id);
                indexes.get(listIndex).add(idx);
            }
            // long t0 = System.currentTimeMillis();
            for (int i = 0; i < files.size(); i++) {
                File file = new File(files.get(i));
                // for (String att : hdf5attribList) {
                // System.out.println("hdf5 attribute=" + att);
                // }
                List<String> attribSet = new ArrayList<String>(hdf5attribList);
                int[] idxArr = new int[indexes.get(i).size()];
                int[] idArr = new int[ids.get(i).size()];
                for (int j = 0; j < idArr.length; j++) {
                    idxArr[j] = indexes.get(i).get(j);
                    idArr[j] = ids.get(i).get(j);
                    // System.out.println("hdf5 idx=" + idxArr[j] + " id="
                    // + idArr[j]);
                }
                PointDataContainer pdc = dao.getPointData(file, idxArr, idArr,
                        attribSet.toArray(new String[0]), this.requestStyle);
                if (masterPDC == null) {
                    masterPDC = pdc;
                    masterPDC.setCurrentSz(masterPDC.getAllocatedSz());
                } else {
                    masterPDC.combine(pdc);
                    masterPDC.setCurrentSz(masterPDC.getAllocatedSz());
                }
            }
            // long t1 = System.currentTimeMillis();
            // System.out
            // .println("Total time (ms) spent on pointdata hdf5 retrieval (all files): "
            // + (t1 - t0));
        }

        if (!dbParamDesc.isEmpty()) {
            for (DbParameterDescription desc : dbParamDesc) {
                switch (desc.getType()) {
                case FLOAT:
                    float[] fdata = new float[masterPDC.getCurrentSz()];
                    FloatDataRecord frec = new FloatDataRecord(
                            desc.getParameterName(), "", fdata);
                    if (desc.getFillValue() != null) {
                        frec.setFillValue(Float.parseFloat(desc.getFillValue()));
                    }
                    masterPDC.add(frec, desc.getUnit());
                    break;
                case INT:
                    int[] idata = new int[masterPDC.getCurrentSz()];
                    masterPDC
                            .add(new IntegerDataRecord(desc.getParameterName(),
                                    "", idata), desc.getUnit());
                    break;
                case LONG:
                    long[] ldata = new long[masterPDC.getCurrentSz()];
                    masterPDC.add(new LongDataRecord(desc.getParameterName(),
                            "", ldata), desc.getUnit());
                    break;
                case STRING:
                    String[] sdata = new String[masterPDC.getCurrentSz()];
                    masterPDC.add(new StringDataRecord(desc.getParameterName(),
                            "", sdata), desc.getUnit());
                    break;
                default:
                    break;
                }

            }
            for (int i = 0; i < masterPDC.getAllocatedSz(); i++) {
                PointDataView pdv = masterPDC.readRandom(i);
                Map<String, Object> dbMap = dbResultMap.get(pdv.getInt("id"));
                for (DbParameterDescription desc : dbParamDesc) {
                    Object obj = dbMap.get(desc.getQueryName());
                    if (obj == null) {
                        obj = pdv.getContainer()
                                .getParameterRecord(desc.getParameterName())
                                .getFillValue();
                        if (obj == null) {
                            continue;
                        }
                    }
                    switch (desc.getType()) {
                    case FLOAT:
                        pdv.setFloat(desc.getParameterName(),
                                ((Number) obj).floatValue());
                        break;
                    case INT:

                        pdv.setInt(desc.getParameterName(),
                                ((Number) obj).intValue());
                        break;
                    case LONG:
                        pdv.setLong(desc.getParameterName(),
                                ((Number) obj).longValue());
                        break;
                    case STRING:
                        pdv.setString(desc.getParameterName(), obj.toString());
                        break;
                    default:
                        break;
                    }
                }
            }
        }

        return masterPDC;
    }
}
