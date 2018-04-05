package gov.noaa.nws.ncep.viz.common.staticPointDataSource;

import gov.noaa.nws.ncep.edex.common.stationTables.Station;
import gov.noaa.nws.ncep.edex.common.stationTables.StationTable;
import gov.noaa.nws.ncep.viz.localization.NcPathManager;
import gov.noaa.nws.ncep.viz.localization.NcPathManager.NcPathConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.raytheon.uf.viz.core.exception.VizException;

/**
<pre>
SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * ????          ???            ???     Initial creation.
 * 06/20/2017    R34594     J. Huber    Update path to station files.
*/

public class StationFilePointDataSource extends AbstractPointDataSource {

    private StationTable stationTable;

    private String filename;

    private String labelField = "";

    private List<LabeledPoint> stnPoints = new ArrayList<>();

    public StationFilePointDataSource(String srcName, String lblFld) {
        filename = srcName;
        labelField = lblFld;
    }

    @Override
    public StaticPointDataSourceType getSourceType() {
        return StaticPointDataSourceType.NCEP_STATIONS_TBL_FILES;
    }

    @Override
    public void loadData() throws VizException {

        File file = new File(filename);
        if (!file.isAbsolute()) {
            file = NcPathManager.getInstance()
                    .getStaticFile(NcPathConstants.PGEN_TABLES_DIR
                            + File.separator + filename);

            if (file == null){
                file = NcPathManager.getInstance()
                        .getStaticFile(NcPathConstants.AWW_STATIONS_DIR
                                + File.separator + filename);
            }
            if (file == null){
                StringBuffer sb = new StringBuffer();
                sb.append("Could not find specified file ");
                sb.append(filename);
                sb.append(" in ");
                sb.append(NcPathConstants.AWW_STATIONS_DIR);
                sb.append(" or ");
                sb.append(NcPathConstants.PGEN_TABLES_DIR);
                statusHandler.error(sb.toString());
                return;
            }
        }

        stationTable = new StationTable(file.getAbsolutePath());

        for (Station stn : stationTable.getStationList()) {

            LabeledPoint lp = new LabeledPoint(stn.getStnname(),
                    stn.getLatitude(), stn.getLongitude());

            // use the names of the xml elements as the label names.
            lp.addLabel("stid", stn.getStid());
            lp.addLabel("stnnum", stn.getStnnum());
            lp.addLabel("stnname", stn.getStnname());
            lp.addLabel("country", stn.getCountry());
            lp.addLabel("state", stn.getState());
            lp.addLabel("elevation", stn.getElevation().toString());
            lp.addLabel("wfo", stn.getWfo());

            if (labelField.equalsIgnoreCase("stid")) {
                lp.setName(stn.getStid());
            } else if (labelField.equalsIgnoreCase("stnnum")) {
                lp.setName(stn.getStnnum());
            } else if (labelField.equalsIgnoreCase("stnname")) {
                lp.setName(stn.getStnname());
            } else if (labelField.equalsIgnoreCase("country")) {
                lp.setName(stn.getCountry());
            } else if (labelField.equalsIgnoreCase("state")) {
                lp.setName(stn.getState());
            } else if (labelField.equalsIgnoreCase("elevation")) {
                lp.setName(stn.getElevation().toString());
            } else if (labelField.equalsIgnoreCase("wfo")) {
                lp.setName(stn.getWfo());
            }

            stnPoints.add(lp);

            insertPoint(lp);
        }
    }

    @Override
    public List<LabeledPoint> getPointData() {
        return stnPoints;
    }

    // not implemented
    @Override
    public Map<String, LabeledPoint> getPointDataByLabel() {
        return null;
    }
}
