/**
 * 
 * Util
 * 
 * This java class contains edex generic utility methods for use.  It can
 * be seen by any other NCEP package, so is a good place to put generic
 * utility methods
 *  
 * T. Lee	11/2008	Creation
 * T. Lee	 3/2009	Fixed roll-over cases; added String functions
 * T. Lee	 4/2009	Added date to the base time
 * T. Lee
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    	Engineer    Description
 * ------------ ---------- 	----------- --------------------------
 * 11/2008		14			T. Lee		Creation
 *  3/2009		14			T. Lee		Fixed roll-over cases; added String functions
 *  4/2009		14			T. Lee		Added date to the base time
 *  5/2009		128			T. Lee		Used UTC in findDataTime
 * 12/12/2016   R27046      S. Russell  Added findFraction() and 
 *                                      findFractionForANumberGreaterThanOne()
 * Sep 06, 2024 2037799     njensen     Split UtilN into two classes and move the edex
 *                                      dependent code to the other one
 * </pre>
 * 
 * @author T.Lee
 */

package gov.noaa.nws.ncep.edex.common.tools.decoder;

public class UtilN {

    /**
     * Constructor
     */
    public UtilN() {

    }

    /**
     * Remove the leading spaces and tabs in a string.
     */
    public static String removeLeadingWhiteSpaces(String str) {
        int i;
        for (i = 0; i < str.length(); i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                break;
            }
        }
        return str.substring(i);
    }

    /**
     * Remove multiple white spaces in a string.
     */
    public static String removeExtraWhiteSpaces(String str) {
        StringBuffer sb = new StringBuffer();
        int i;
        char first = str.charAt(0);
        char second;
        for (i = 1; i < str.length(); i++) {
            second = str.charAt(i);
            if (!Character.isWhitespace(first)
                    || !Character.isWhitespace(second)) {
                sb.append(first);
                first = second;
            }
            if (i == (str.length() - 1)) {
                sb.append(second);
            }
        }
        return sb.toString();
    }

    /**
     * Convert a decimal value less than 1, to a String representing a fraction
     * 
     * @param p
     * @return
     */
    public static String findFraction(double p) {

        if (p == 0.0) {
            return "";
        }

        Double decimal = p;
        String fraction = decimal.toString();
        double denominator = 0;

        // Only produce fractions for numbers less than one.
        if (decimal < 1) {
            while (decimal.toString().length() != 1) {

                // multiplying a decimal by a whole number will yield the result
                // of the numerator. If the numerator is a whole number so to is
                // the denominator and thus the proper fraction.
                denominator += 1;
                Double numerator = decimal * denominator;
                Integer checkInt = new Integer(numerator.intValue());

                if (numerator / checkInt == 1) {
                    fraction = numerator.intValue() + "/" + (int) denominator;
                    break;
                }
            }
        }
        return fraction;
    }

    /**
     * Convert a decimal greater than 1 into a String with a whole and (if
     * relevant ) to a fraction.
     * 
     * @param d
     * @return
     */
    public static String findFractionForANumberGreaterThanOne(Double d) {
        StringBuffer sb;

        if (d == 0.0) {
            sb = new StringBuffer("0.0");
            return sb.toString();
        } else {
            sb = new StringBuffer();
        }

        String sWholeNumber = splitTheDouble(d, 0);
        String sDecimal = splitTheDouble(d, 1);
        double dDecimal = 0.0d;
        String fraction = null;

        // If the decimal number is NOT zero
        if (!sDecimal.equals("0")) {

            // Format the decimal number as a fraction
            dDecimal = Double.parseDouble("0." + sDecimal);
            fraction = findFraction(dDecimal);

            // If the whole number is NOT zero
            if (!sWholeNumber.equals("0")) {
                // Add it and a white space
                sb.append(sWholeNumber);
                sb.append(" ");
            }

            // Add the fraction
            sb.append(fraction);

        } else { // the decimal number IS zero
            sb.append(sWholeNumber);
            sb.append(".0");

        }

        return sb.toString();
    }

    /**
     * Split a double into whole number and decimal number. Return the chosen
     * String token. Zero returns the whole number. One returns the decimal
     * number.
     * 
     * @param dParentValue
     * @param iToken
     * @return
     */

    public static String splitTheDouble(double dParentValue, int iToken) {

        String sPartReturned = null;
        String sParentValue = Double.toString(dParentValue);
        String[] tokens = sParentValue.split("\\.");

        // Set the whole number String part to be returned
        if (iToken == 0) {
            sPartReturned = tokens[iToken];
        } else if (iToken == 1) {
            // Set the decimal number String part to be returned
            sPartReturned = tokens[iToken];
        }

        return sPartReturned;

    }

}
