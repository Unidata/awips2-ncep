package gov.noaa.nws.ncep.viz.rsc.ncgrid.dgdriv;

/**
 * 
 */

import gov.noaa.nws.ncep.viz.rsc.ncgrid.FloatGridData;

import com.raytheon.uf.viz.core.rsc.DisplayType;

public class TestSO {

    private static final int NUM_TIMES = 50;

    public static void main(String args[]) {

        for (int n = 0; n < NUM_TIMES; n++) {
            float rmin = Float.MAX_VALUE, rmax = Float.MIN_VALUE;
            FloatGridData rec = getMeAGrid();
            rec = getMeAnotherGrid();
            rec = getMeAThirdGrid();
            float[] grid = rec.getXdataAsArray();
            for (int j = 0; j < grid.length; j++) {
                rmin = Math.min(rmin, grid[j]);
                rmax = Math.max(rmax, grid[j]);
            }
            System.out.println("GRID " + grid.length + "   rmin = " + rmin
                    + "   rmax = " + rmax);
        }

    }

    private static FloatGridData getMeAGrid() {

        FloatGridData rec = null;

        TestDgdriv aDgdriv = new TestDgdriv();
        // aDgdriv.setCycleForecastTimes(dataTimesForDgdriv);
        // aDgdriv.setSpatialObject(cov);
        aDgdriv.setGdattim("2011-10-03 12:00:00.0 (3)");
        aDgdriv.setGarea("dset");
        aDgdriv.setGdfile("NAM104");
        aDgdriv.setGdpfun("knts((mag(wnd)))");
        aDgdriv.setGlevel("200");
        aDgdriv.setGvcord("pres");
        aDgdriv.setScale("0                             ");
        aDgdriv.setDataSource("ncgrib");

        DisplayType displayType = DisplayType.CONTOUR;
        if (displayType == DisplayType.ARROW || displayType == DisplayType.BARB
                || displayType == DisplayType.STREAMLINE) {
            /*
             * Specify vector data retrieval from GEMPAK GD
             */
            aDgdriv.setScalar(false);
        } else {
            /*
             * Specify scalar data retrieval from GEMPAK GD
             */
            aDgdriv.setScalar(true);
        }

        try {
            rec = aDgdriv.execute();
        } catch (DgdrivException e) {
            System.out.println("GEMPAK GD error stack:\n" + e.getMessage());
        }

        return rec;
    }

    private static FloatGridData getMeAnotherGrid() {

        FloatGridData rec = null;

        TestDgdriv aDgdriv = new TestDgdriv();
        // aDgdriv.setCycleForecastTimes(dataTimesForDgdriv);
        // aDgdriv.setSpatialObject(cov);
        aDgdriv.setGdattim("2011-10-03 12:00:00.0 (3)");
        aDgdriv.setGarea("dset");
        aDgdriv.setGdfile("NAM104");
        aDgdriv.setGdpfun("sm5s(hght)");
        aDgdriv.setGlevel("200");
        aDgdriv.setGvcord("pres");
        aDgdriv.setScale("0                             ");
        aDgdriv.setDataSource("ncgrib");

        DisplayType displayType = DisplayType.CONTOUR;
        if (displayType == DisplayType.ARROW || displayType == DisplayType.BARB
                || displayType == DisplayType.STREAMLINE) {
            /*
             * Specify vector data retrieval from GEMPAK GD
             */
            aDgdriv.setScalar(false);
        } else {
            /*
             * Specify scalar data retrieval from GEMPAK GD
             */
            aDgdriv.setScalar(true);
        }

        try {
            rec = aDgdriv.execute();
        } catch (DgdrivException e) {
            System.out.println("GEMPAK GD error stack:\n" + e.getMessage());
        }

        return rec;
    }

    private static FloatGridData getMeAThirdGrid() {

        FloatGridData rec = null;

        TestDgdriv aDgdriv = new TestDgdriv();
        // aDgdriv.setCycleForecastTimes(dataTimesForDgdriv);
        // aDgdriv.setSpatialObject(cov);
        aDgdriv.setGdattim("2011-10-03 12:00:00.0 (3)");
        aDgdriv.setGarea("dset");
        aDgdriv.setGdfile("NAM104");
        aDgdriv.setGdpfun("kntv(wnd)");
        aDgdriv.setGlevel("200");
        aDgdriv.setGvcord("pres");
        aDgdriv.setScale("0                             ");
        aDgdriv.setDataSource("ncgrib");

        DisplayType displayType = DisplayType.BARB;
        if (displayType == DisplayType.ARROW || displayType == DisplayType.BARB
                || displayType == DisplayType.STREAMLINE) {
            /*
             * Specify vector data retrieval from GEMPAK GD
             */
            aDgdriv.setScalar(false);
        } else {
            /*
             * Specify scalar data retrieval from GEMPAK GD
             */
            aDgdriv.setScalar(true);
        }

        try {
            rec = aDgdriv.execute();
        } catch (DgdrivException e) {
            System.out.println("GEMPAK GD error stack:\n" + e.getMessage());
        }

        return rec;
    }

}
