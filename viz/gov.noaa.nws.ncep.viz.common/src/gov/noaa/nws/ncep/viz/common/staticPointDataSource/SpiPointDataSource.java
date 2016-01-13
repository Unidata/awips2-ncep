package gov.noaa.nws.ncep.viz.common.staticPointDataSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.util.FileUtil;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.viz.pointdata.StaticPlotInfoPV;
import com.raytheon.viz.pointdata.StaticPlotInfoPV.SPIEntry;

/**
 * 
 * <pre>
 * (non-Javadoc)
 * 
 *    SOFTWARE HISTORY
 * 
 *    Date          Ticket#       Engineer           Description
 * -----------    ----------    -----------    --------------------------
 * 12/21/2015       R9407        jhuber          Changed logic to read from CAVE --> basemaps instead 
 *                                               of ncep --> basemaps to merge usage of spi files.
 * 
 * 
 * </pre>
 */

public class SpiPointDataSource extends AbstractPointDataSource {

    private String spiFilename;

    private String labelField;

    private List<LabeledPoint> spiPoints = new ArrayList<LabeledPoint>();

    public SpiPointDataSource(String fname, String lblFld) {
        spiFilename = fname;
        labelField = lblFld;
    }

    @Override
    public StaticPointDataSourceType getSourceType() {
        return StaticPointDataSourceType.SPI_FILE;
    }

    @Override
    public void loadData() throws VizException {
        File file = new File(spiFilename);
        if (!file.isAbsolute()) {
            spiFilename = FileUtil.join(VizApp.getMapsDir(), spiFilename);
            file = PathManagerFactory.getPathManager().getStaticFile(
                    spiFilename);
        }
        if ((file == null) || (file.exists() == false)) {
            throw new VizException("Could not find lpi file",
                    new FileNotFoundException(spiFilename));
        }
        HashMap<String, SPIEntry> spiEntries = StaticPlotInfoPV
                .readStaticPlotInfoPV(file.getAbsolutePath(), true)
                .getSpiList();

        for (String icao : spiEntries.keySet()) {
            SPIEntry spi = spiEntries.get(icao);
            LabeledPoint lp = new LabeledPoint(icao, spi.latlon.y, spi.latlon.x);

            spiPoints.add(lp);

            insertPoint(lp);
        }
    }

    @Override
    public List<LabeledPoint> getPointData() {
        return spiPoints;
    }

    // not implemented
    @Override
    public Map<String, LabeledPoint> getPointDataByLabel() {
        return null;
    }
}
