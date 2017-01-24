/**
 * 
 * gov.noaa.nws.ncep.ui.nsharp.Activator
 * 
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    	Engineer    Description
 * -------		------- 	-------- 	-----------
 * 03/23/2010	229			Chin Chen	Initial coding
 * 05/08/2014   3108        bkowal      Disabled loading of native libraries that
 *                                      are not available to Windows when the Windows
 *                                      OS is detected.
 *
 * 07/05/2016   RM#15923    Chin Chen   NSHARP - Native Code replacement 
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 */
package gov.noaa.nws.ncep.ui.nsharp;

import java.io.IOException;
import java.util.PropertyResourceBundle;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "gov.noaa.nws.ncep.ui.nsharp";

    // The shared instance
    private static Activator plugin;

    /**
     * The constructor
     */
    public Activator() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
     * )
     */
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
     * )
     */
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     * 
     * @return the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }

    // for plugin to retrieve runtime properties from build.properties
    protected final static String MY_PROPERTIES = "build.properties";

    protected PropertyResourceBundle myProperties;

    public PropertyResourceBundle getMyProperties() {

        if (myProperties == null) {

            try {

                myProperties = new PropertyResourceBundle(

                FileLocator.openStream(this.getBundle(),

                new Path(MY_PROPERTIES), false));

            } catch (IOException e) {

                System.out.println("Error open NSHARP build.properties file");

            }
        }
        return myProperties;
    }

}
