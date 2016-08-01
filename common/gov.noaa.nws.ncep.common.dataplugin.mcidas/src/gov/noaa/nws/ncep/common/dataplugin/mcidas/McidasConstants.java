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
package gov.noaa.nws.ncep.common.dataplugin.mcidas;

/**
 * Constants useful to McidasRecord and the mcidas plugins.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 10, 2015  10436     njensen     Initial creation
 * 12/04/2015    R12953    R Reynolds  Added projection and resolution
 * 01/25/2016    R14142    R Reynolds  added CUSTOM_NAME
 * 06/06/2016    R15945    R Reynolds  Added constants
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public class McidasConstants {
	
	
    public static final String SATELLLITE = "satellite";

    public static final String AREA = "area";

    public static final String RESOLUTION = "resolution";

    public static final String CHANNEL = "channel";

    public static final String RESOURCE_DEFINITION = "RD";

    public static final String ATTRIBUTE_SET_ALIAS = "AS";
	
	

    public static final String PLUGIN_NAME = "mcidas";

    public static final String SATELLITE_ID = "satelliteId";

    public static final String AREA_ID = "areaId";


    public static final String PROJECTION = "projection";

    public static final String MCIDAS_SATELLITE = "McidasSatellite";

    public static final String IMAGE_TYPE_ID = "imageTypeId";

    public static final String CUSTOM_NAME = "CustomName";

}
