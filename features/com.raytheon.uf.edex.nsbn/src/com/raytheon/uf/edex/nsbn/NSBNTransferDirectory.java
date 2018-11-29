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

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 * A container class to hold which directory configurations
 * apply to a transfer data type.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Oct 18, 2018  56039    mrichardson Initial Creation
 * 
 * 
 * </pre>
 * 
 * @author mrichardson
 */

@XmlRootElement(name = "nsbnTransferDirectory")
@XmlAccessorType(XmlAccessType.NONE)
public class NSBNTransferDirectory implements FileFilter {

    @XmlAttribute
    private String id;

    @XmlElement
    private int threadpoolCount;

    @XmlElement
    private String scanDir;

    @XmlElement
    private String destinationQueue;

    @XmlElement
    private String destinationDir;

    @XmlElements({ @XmlElement(name="include") })
    private List<String> include;
    
    private List<Pattern> includePatterns = new ArrayList<Pattern>();

    @XmlElements({ @XmlElement(name="exclude") })
    private List<String> exclude;
    
    private List<Pattern> excludePatterns = new ArrayList<Pattern>();

    public NSBNTransferDirectory() {

    }
    
    @Override
    public boolean accept(File f) {
        boolean accept = true;
        
        // Exclude any file that matches against an exclude regex
        if (!excludePatterns.isEmpty()) {
            for (Pattern exc : excludePatterns) {
                accept = !exc.matcher(f.getName()).find();
                if (!accept) {
                    break;
                }
            }
        }
        
        if (accept) {
            // Include any file that matches against an include regex
            //  or if an include regex has been declared, but is empty
            if (!includePatterns.isEmpty()) {
                for (Pattern inc : includePatterns) {
                    accept = inc.matcher(f.getName()).find();
                    if (accept) {
                        break;
                    }
                }
            }
        }
        
        return accept;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getThreadpoolCount() {
        return threadpoolCount;
    }

    public void setThreadpoolCount(int threadpoolCount) {
        this.threadpoolCount = threadpoolCount;
    }

    public String getScanDir() {
        return scanDir;
    }
    
    public void setScanDir(String scanDir) {
        this.scanDir = scanDir;
    }
    
    public String getDestinationQueue() {
        return destinationQueue;
    }

    public void setDestinationQueue(String destinationQueue) {
        this.destinationQueue = destinationQueue;
    }
    
    public String getDestinationDir() {
        return destinationDir;
    }

    public void setDestinationDir(String destinationDir) {
        this.destinationDir = destinationDir;
    }

    public List<String> getInclude() {
        return include;
    }

    public void setInclude(List<String> include) {
        this.include = include;
    }

    public List<String> getExclude() {
        return exclude;
    }

    public void setExclude(List<String> exclude) {
        this.exclude = exclude;
    }
    
    public void compileRegexes() {
        for (String inc : include) {
            if (inc != null && !inc.isEmpty()) {
                includePatterns.add(Pattern.compile(inc));
            }
        }
        for (String exc : exclude) {
            if (exc != null && !exc.isEmpty()) {
                excludePatterns.add(Pattern.compile(exc));
            }
        }
    }
}
