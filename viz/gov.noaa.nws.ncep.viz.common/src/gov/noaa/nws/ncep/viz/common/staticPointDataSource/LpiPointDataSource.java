package gov.noaa.nws.ncep.viz.common.staticPointDataSource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.util.FileUtil;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.exception.VizException;

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
 *                                               of ncep --> basemaps to merge usage of lpi files.
 * 
 * 
 * </pre>
 */

public class LpiPointDataSource extends AbstractPointDataSource {

    private String lpiFilename;

    private String labelField;

    private List<LabeledPoint> lpiPoints = new ArrayList<LabeledPoint>();

    public LpiPointDataSource(String fname, String lblFld) {
        lpiFilename = fname;
        labelField = lblFld;
    }

    @Override
    public StaticPointDataSourceType getSourceType() {
        return StaticPointDataSourceType.LPI_FILE;
    }

    @Override
    public void loadData() throws VizException {
        try {
            File file = new File(lpiFilename);
            if (!file.isAbsolute()) {
                lpiFilename = FileUtil.join(VizApp.getMapsDir(), lpiFilename);
                file = PathManagerFactory.getPathManager().getStaticFile(
                        lpiFilename);
            }
            if ((file == null) || (file.exists() == false)) {
                throw new VizException("Could not find lpi file",
                        new FileNotFoundException(lpiFilename));
            }

            BufferedReader in = new BufferedReader(new FileReader(file));

            String s = in.readLine();
            while (s != null) {
                LabeledPoint lp = readPoint(s);
                if (lp != null) {
                    lpiPoints.add(lp);
                    insertPoint(lp);
                }
                s = in.readLine();
            }
            in.close();

        } catch (FileNotFoundException e) {
            throw new VizException("Can't find file for :" + lpiFilename);
        } catch (IOException e) {
            throw new VizException("I/O error on file :" + lpiFilename);
        }

    }

    public LabeledPoint readPoint(String s) throws IOException {

        int maxLen = 0;
        Scanner in = new Scanner(s);

        if (!in.hasNextDouble()) {
            in.close();
            return null;
        }
        double lat = in.nextDouble();

        if (!in.hasNextDouble()) {
            in.close();
            return null;
        }
        double lon = in.nextDouble();

        if (!in.hasNextDouble()) {
            in.close();
            return null;
        }

        if (!in.hasNext()) {
            in.close();
            return null;
        }
        String lbl = in.findInLine("[^\\|]*").trim();

        if (lbl.length() > maxLen) {
            maxLen = lbl.length();
        }
        in.close();
        return new LabeledPoint(lbl, lat, lon);
    }

    @Override
    public List<LabeledPoint> getPointData() {
        return lpiPoints;
    }

    // not implemented
    @Override
    public Map<String, LabeledPoint> getPointDataByLabel() {
        return null;
    }
}
