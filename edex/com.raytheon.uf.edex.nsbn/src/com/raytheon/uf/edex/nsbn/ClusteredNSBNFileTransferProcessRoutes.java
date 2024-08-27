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

package com.raytheon.uf.edex.nsbn;

import com.raytheon.uf.edex.routes.EDEXRouteBuilder;

/**
 * Camel routes converted from file "nsbnIngest-spring.xml", context
 * "clusteredNSBNFileTransferProcess"
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 2024-08-26   2037797    aford       Initial creation (from auto-generated)
 *
 * </pre>
 */

//@formatter:off
/* Original XML definition:

    <camelContext id="clusteredNSBNFileTransferProcess"
        xmlns="http://camel.apache.org/schema/spring"
        errorHandlerRef="errorHandler">
        <endpoint id="nsbnFileTransferEndpoint"
            uri="file:${nsbn.drop.box}?noop=true&amp;idempotent=true&amp;idempotentRepository=#nsbnIdempotentRepository&amp;delay=5000&amp;maxMessagesPerPoll=1000&amp;exclusiveReadLockStrategy=#nsbnFileChangedStrategy&amp;recursive=true" />

        <route id="nsbnFileScan">
            <from uri="nsbnFileTransferEndpoint" />
            <bean ref="nsbnFileTransferProcess"/>
        </route>
    </camelContext>
*/
//@formatter:on
public class ClusteredNSBNFileTransferProcessRoutes extends EDEXRouteBuilder {

    private final String nsbnDropBox;

    public ClusteredNSBNFileTransferProcessRoutes(String nsbnDropBox) {
        this.nsbnDropBox = nsbnDropBox;
    }

    @Override
    public void configure() throws Exception {
        // @formatter:off
        from("file:" + this.nsbnDropBox +
                "?noop=true" +
                "&idempotent=true" +
                "&idempotentRepository=#bean:nsbnIdempotentRepository" +
                "&delay=5000" +
                "&maxMessagesPerPoll=1000" +
                "&exclusiveReadLockStrategy=#bean:nsbnFileChangedStrategy" +
                "&recursive=true")
          .bean("nsbnFileTransferProcess")
          .setId("nsbnFileScan");
        // @formatter:on
    }
}