/**
 * This code has unlimited rights, and is provided "as is" by the National Centers 
 * for Environmental Prediction, without warranty of any kind, either expressed or implied, 
 * including but not limited to the implied warranties of merchantability and/or fitness 
 * for a particular purpose.
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 **/
package gov.noaa.nws.ncep.common.dataplugin.geomag.util;

import java.util.Calendar;

/**
 * 
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 22, 2014            jtravis     Initial creation
 * 
 * </pre>
 * 
 * @author jtravis
 * @version 1.0
 */

public class TestCondition {

    private String testNumber = null;

    private Integer hourOfInterest = null;

    private Integer minuteOfInterest = null;

    private String stationCd = null;

    private Calendar cal = null;

    /**
     * 
     */
    public TestCondition() {
        // TODO Auto-generated constructor stub
    }

    public boolean isInitialized() {

        if (testNumber == null || hourOfInterest == null
                || minuteOfInterest == null || stationCd == null || cal == null) {

            return false;

        } else {

            return true;

        }

    }

    public String getTestName() {

        String name = this.stationCd + "." + cal.get(Calendar.YEAR) + "."
                + cal.get(Calendar.MONTH) + "."
                + cal.get(Calendar.DAY_OF_MONTH) + "." + this.hourOfInterest
                + "." + this.minuteOfInterest + "." + "TEST" + "."
                + this.testNumber;

        return name;

    }

    /**
     * @return the testNumber
     */
    public String getTestNumber() {
        return testNumber;
    }

    /**
     * @param testNumber
     *            the testNumber to set
     */
    public void setTestNumber(String testNumber) {
        this.testNumber = testNumber;
    }

    /**
     * @return the hourOfInterest
     */
    public int getHourOfInterest() {
        return hourOfInterest;
    }

    /**
     * @param hourOfInterest
     *            the hourOfInterest to set
     */
    public void setHourOfInterest(int hourOfInterest) {
        this.hourOfInterest = hourOfInterest;
    }

    /**
     * @return the minuteOfInterest
     */
    public int getMinuteOfInterest() {
        return minuteOfInterest;
    }

    /**
     * @param minuteOfInterest
     *            the minuteOfInterest to set
     */
    public void setMinuteOfInterest(int minuteOfInterest) {
        this.minuteOfInterest = minuteOfInterest;
    }

    /**
     * @return the stationCd
     */
    public String getStationCd() {
        return stationCd;
    }

    /**
     * @param stationCd
     *            the stationCd to set
     */
    public void setStationCd(String stationCd) {
        this.stationCd = stationCd;
    }

    /**
     * @return the cal
     */
    public Calendar getCal() {
        return cal;
    }

    /**
     * @param cal
     *            the cal to set
     */
    public void setCal(long timeInMillis) {

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timeInMillis);

        this.cal = c;
    }

}
