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

package gov.noaa.nws.ncep.edex.plugin.ncgrib;

import com.raytheon.uf.edex.esb.camel.EDEXRouteBuilder;

/**
 * Camel routes converted from file "ncgrib-file-endpoint.xml", context
 * "ncgrib-qpid-endpoint"
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 2024-07-11   2037701    aford       Initial creation (from auto-generated)
 *
 * </pre>
 */

public class NcgribQpidEndpointRoutes extends EDEXRouteBuilder {

    private final String gribSplitThreads;

    public NcgribQpidEndpointRoutes(String gribSplitThreads) {
        this.gribSplitThreads = gribSplitThreads;
    }

    @Override
    public void configure() throws Exception {
        // @formatter:off
        from("jms-durable:queue:Ingest.ncGrib?concurrentConsumers=" + this.gribSplitThreads)
                .autoStartup(false)
                .doTry()
                        .pipeline()
                                .bean("ncgribFilenameProcessor")
                                .setHeader("pluginName", constant("grid"))
                                .bean("stringToFile")
                                .split(method("gribSplitter", "split")).streaming()
                                .to("jms-durable:queue:Ingest.GribDecode")
                        .end()
                .endDoTry()
                .doCatch(Throwable.class)
                        .to("direct:logFailedData")
                .endDoTry()
                .end()
                .setId("ncgribFileNameConsumerRoute");
        // @formatter:on
    }
}
