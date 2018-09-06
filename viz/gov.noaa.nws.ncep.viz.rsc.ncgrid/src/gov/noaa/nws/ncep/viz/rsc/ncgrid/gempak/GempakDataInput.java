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

import java.util.ArrayList;

import com.raytheon.uf.common.geospatial.ISpatialObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.time.DataTime;

import gov.noaa.nws.ncep.viz.rsc.ncgrid.dgdriv.NcgridDataCache;

/**
 * Input to GEMPAK that specifies the data to process and any other necessary
 * parameters.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 05, 2018  7417      mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
@DynamicSerialize
public class GempakDataInput {

    @DynamicSerializeElement
    private String ensembleMember;

    @DynamicSerializeElement
    private ArrayList<DataTime> cycleForecastTimes;

    @DynamicSerializeElement
    private ISpatialObject spatialObject;

    @DynamicSerializeElement
    private String gdattim = null;

    @DynamicSerializeElement
    private String garea;

    @DynamicSerializeElement
    private String gdfile;

    @DynamicSerializeElement
    private String gdpfun;

    @DynamicSerializeElement
    private String glevel;

    @DynamicSerializeElement
    private String gvcord;

    @DynamicSerializeElement
    private String scale;

    @DynamicSerializeElement
    private String dataSource;

    @DynamicSerializeElement
    private String preferences;

    @DynamicSerializeElement
    private boolean scalar;

    @DynamicSerializeElement
    private boolean arrowVector;

    // Not serialized
    private NcgridDataCache cacheData;

    public GempakDataInput() {
    }

    /**
     * @return the ensembleMember
     */
    public String getEnsembleMember() {
        return ensembleMember;
    }

    /**
     * @param ensembleMember
     *            the ensembleMember to set
     */
    public void setEnsembleMember(String ensembleMember) {
        this.ensembleMember = ensembleMember;
    }

    /**
     * @return the cycleForecastTimes
     */
    public ArrayList<DataTime> getCycleForecastTimes() {
        return cycleForecastTimes;
    }

    /**
     * @param cycleForecastTimes
     *            the cycleForecastTimes to set
     */
    public void setCycleForecastTimes(ArrayList<DataTime> cycleForecastTimes) {
        this.cycleForecastTimes = cycleForecastTimes;
    }

    /**
     * @return the spatialObject
     */
    public ISpatialObject getSpatialObject() {
        return spatialObject;
    }

    /**
     * @param spatialObject
     *            the spatialObject to set
     */
    public void setSpatialObject(ISpatialObject spatialObject) {
        this.spatialObject = spatialObject;
    }

    /**
     * @return the gdattim
     */
    public String getGdattim() {
        return gdattim;
    }

    /**
     * @param gdattim
     *            the gdattim to set
     */
    public void setGdattim(String gdattim) {
        this.gdattim = gdattim;
    }

    /**
     * @return the garea
     */
    public String getGarea() {
        return garea;
    }

    /**
     * @param garea
     *            the garea to set
     */
    public void setGarea(String garea) {
        this.garea = garea;
    }

    /**
     * @return the gdfile
     */
    public String getGdfile() {
        return gdfile;
    }

    /**
     * @param gdfile
     *            the gdfile to set
     */
    public void setGdfile(String gdfile) {
        this.gdfile = gdfile;
    }

    /**
     * @return the gdpfun
     */
    public String getGdpfun() {
        return gdpfun;
    }

    /**
     * @param gdpfun
     *            the gdpfun to set
     */
    public void setGdpfun(String gdpfun) {
        this.gdpfun = gdpfun;
    }

    /**
     * @return the glevel
     */
    public String getGlevel() {
        return glevel;
    }

    /**
     * @param glevel
     *            the glevel to set
     */
    public void setGlevel(String glevel) {
        this.glevel = glevel;
    }

    /**
     * @return the gvcord
     */
    public String getGvcord() {
        return gvcord;
    }

    /**
     * @param gvcord
     *            the gvcord to set
     */
    public void setGvcord(String gvcord) {
        this.gvcord = gvcord;
    }

    /**
     * @return the scale
     */
    public String getScale() {
        return scale;
    }

    /**
     * @param scale
     *            the scale to set
     */
    public void setScale(String scale) {
        this.scale = scale;
    }

    /**
     * @return the dataSource
     */
    public String getDataSource() {
        return dataSource;
    }

    /**
     * @param dataSource
     *            the dataSource to set
     */
    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * @return the preferences
     */
    public String getPreferences() {
        return preferences;
    }

    /**
     * @param preferences
     *            the preferences to set
     */
    public void setPreferences(String preferences) {
        this.preferences = preferences;
    }

    /**
     * @return the scalar
     */
    public boolean isScalar() {
        return scalar;
    }

    /**
     * @param scalar
     *            the scalar to set
     */
    public void setScalar(boolean scalar) {
        this.scalar = scalar;
    }

    /**
     * @return the arrowVector
     */
    public boolean isArrowVector() {
        return arrowVector;
    }

    /**
     * @param arrowVector
     *            the arrowVector to set
     */
    public void setArrowVector(boolean arrowVector) {
        this.arrowVector = arrowVector;
    }

    /**
     * @return the cacheData
     */
    public NcgridDataCache getCacheData() {
        return cacheData;
    }

    /**
     * @param cacheData
     *            the cacheData to set
     */
    public void setCacheData(NcgridDataCache cacheData) {
        this.cacheData = cacheData;
    }
}
