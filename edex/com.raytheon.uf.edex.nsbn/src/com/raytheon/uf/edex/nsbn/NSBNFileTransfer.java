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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationUtil;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.SingleTypeJAXBManager;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.SimulatedTime;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.file.IOPermissionsHelper;
import com.raytheon.uf.edex.core.EDEXUtil;
import com.raytheon.uf.edex.core.EdexException;

/**
 * A bean that moves incoming NSBN files to their designated directories
 * and then informs the configured ingest queues for processing.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 12, 2018 56039      mrichardson Initial creation
 *  
 * </pre>
 * 
 * @author mrichardson
 */

public class NSBNFileTransfer implements Processor  {
    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(getClass());
    
    private static final String BASE_DIR_PROPERTY = "nsbn.drop.box";

    private static final String DATE_SUBDIR_PROPERTY = "add.date.to.nsbn.dest.sub.dirs";
    
    private static final long LONG_FILE_PROCESS_WARNING_THRESHOLD =
            Long.getLong("long.file.process.warning");
    
    private static final String NSBN_STORE = "nsbn_store";
    
    private static final Path BASE_PATH = Paths.get(System.getProperty(BASE_DIR_PROPERTY));
    
    private static final PosixFilePermission[] POSIX_FILE_PERMISSIONS = new PosixFilePermission[] {
            PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE };

    private static final PosixFilePermission[] POSIX_DIRECTORY_PERMISSIONS = new PosixFilePermission[] {
            PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ,
            PosixFilePermission.GROUP_WRITE,
            PosixFilePermission.GROUP_EXECUTE };

    private static final Set<PosixFilePermission> POSIX_FILE_SET = IOPermissionsHelper
            .getPermissionsAsSet(POSIX_FILE_PERMISSIONS);

    private static final FileAttribute<Set<PosixFilePermission>> POSIX_DIRECTORY_ATTRIBUTES = IOPermissionsHelper
            .getPermissionsAsAttributes(POSIX_DIRECTORY_PERMISSIONS);

    private static Map<String, ThreadPoolExecutor> threadPoolExecutorMap;
    
    private static ThreadPoolExecutor rejectedExecutor = 
            new ThreadPoolExecutor(
                1, 5, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
    
    private Map<String, List<NSBNTransferDirectory>> nsbnTransferDirectoryMap;

    private final ThreadLocal<SimpleDateFormat> sdfs = TimeUtil
            .buildThreadLocalSimpleDateFormat(
                    "yyyy" + File.separatorChar 
                    + "MM" + File.separatorChar 
                    + "dd" + File.separatorChar + "HH",
                    TimeUtil.GMT_TIME_ZONE);

    public NSBNFileTransfer () {
        initNSBNTransferMap();
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        File file = exchange.getIn().getBody(File.class);
        
        if (file != null) {
            moveFileToDestinationDir(exchange);
        } else {
            // No file received
            exchange.getOut().setFault(true);
        }
    }

    private synchronized void initNSBNTransferMap() {
        statusHandler.info("Initializing NSBN transfer mapping...");
        IPathManager pathMgr = PathManagerFactory.getPathManager();
        LocalizationLevel[] levels = new LocalizationLevel[] { LocalizationLevel.BASE, LocalizationLevel.SITE };

        Map<LocalizationLevel, ? extends ILocalizationFile> files = pathMgr
                .getTieredLocalizationFile(LocalizationType.COMMON_STATIC,
                        LocalizationUtil.join("nsbn",
                                "nsbnTransferDirectories.xml"));

        NSBNTransferDirectorySet nsbnTransferDirectorySet;
        nsbnTransferDirectoryMap = new HashMap<>();
        threadPoolExecutorMap = new HashMap<>();
        SingleTypeJAXBManager<NSBNTransferDirectorySet> mgr = 
                SingleTypeJAXBManager.createWithoutException(NSBNTransferDirectorySet.class);
        for (LocalizationLevel level : levels) {
            ILocalizationFile nsbnTransferFile = files.get(level);
            if (nsbnTransferFile == null) {
                continue;
            }

            try (InputStream is = nsbnTransferFile.openInputStream()) {
                nsbnTransferDirectorySet = mgr
                        .unmarshalFromInputStream(is);
                statusHandler.info(String.format(
                        "Using nsbnTransferFile [%s]", nsbnTransferFile));
                
                ThreadPoolExecutor executor;
                
                for (NSBNTransferDirectory nsbnTransferDirectory : nsbnTransferDirectorySet.getDirectories()) {
                    String idx = nsbnTransferDirectory.getId();
                    File destPath = new File(nsbnTransferDirectory.getDestinationDir());
                    List <NSBNTransferDirectory> dir = nsbnTransferDirectoryMap.get(idx);
                    
                    if (!destPath.isDirectory()) {
                        Files.createDirectories(destPath.toPath(), POSIX_DIRECTORY_ATTRIBUTES);
                    }
                    
                    if (dir == null) {
                        dir = new ArrayList<>(1);
                        nsbnTransferDirectoryMap.put(idx, dir);
                    }
                    nsbnTransferDirectory.compileRegexes();
                    dir.add(nsbnTransferDirectory);
                    
                    executor = new ThreadPoolExecutor(
                            1, nsbnTransferDirectory.getThreadpoolCount(),
                            60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
                    threadPoolExecutorMap.put(idx, executor);
                }
            } catch (LocalizationException | IOException
                    | SerializationException e) {
                statusHandler.error(
                        "Error unmarshalling post processed data type list: "
                                + nsbnTransferFile, e);
            }
        }
    }

    /**
     * Moves the file defined in the exchange param to an archive directory
     *  and sits the file in a thread pool of a corresponding queue to ingest.
     * 
     * @param exchange
     * @throws IOException
     */
    public void moveFileToDestinationDir(Exchange exchange) throws IOException {
        File inFile = exchange.getIn().getBody(File.class);
        final long START_METHOD_TIME = System.currentTimeMillis();
        final long FILE_MODIFIED_TIME = inFile.lastModified();
        String fileName = inFile.getPath();
        statusHandler.info("File found for processing: " + fileName);
        boolean fileAccepted = false;
        StringBuilder path = new StringBuilder(inFile.getPath().length());
        String destDir = "";
        Date timeAdded = SimulatedTime.getSystemTime().getTime();
        NSBNTransferDirectory currDirectory;
        Path sourcePath = inFile.toPath();
        int baseDepth = BASE_PATH.getNameCount();
        Path sourceParent = sourcePath.getName(baseDepth);
        File srcParentAsFile = sourceParent.toFile();
        String fileParent = srcParentAsFile.getName();
        
        for (String directoryKey : nsbnTransferDirectoryMap.keySet()) {
            currDirectory = nsbnTransferDirectoryMap.get(directoryKey).get(0);
            if (fileParent.equals(currDirectory.getScanDir())) {
                fileAccepted = currDirectory.accept(inFile);
                
                // Only continue moving the file over if the file
                //   matches the configured include pattern(s) and does
                //   not fall under any exclude pattern(s) (if provided)
                if (fileAccepted) {
                    destDir = currDirectory.getDestinationDir() + File.separatorChar;
                    final String destQueue = currDirectory.getDestinationQueue();
                    path.append(destDir);
                    
                    // If configured to do so, append sub-directories
                    //  to the path based on the date
                    if (Boolean.getBoolean(DATE_SUBDIR_PROPERTY)) {
                        path.append(sdfs.get().format(timeAdded)).append(File.separatorChar);
                    }
                    
                    exchange.getIn().setHeader("header", path + inFile.getName());
                    exchange.getIn().setHeader("enqueueTime", System.currentTimeMillis());
                    
                    ThreadPoolExecutor executor = threadPoolExecutorMap.get(currDirectory.getId());
                    
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            File newFile;
                            try {
                                newFile = createAndMoveNewFile(path, inFile);
                                // Notify ingest of the new file
                                EDEXUtil.getMessageProducer().sendAsyncUri(
                                        "jms-durable:queue:" + destQueue, newFile.getPath());
                                
                                long stopMethodTime = System.currentTimeMillis();
                                long processTime = stopMethodTime - START_METHOD_TIME;
                                long latencyTime = stopMethodTime - FILE_MODIFIED_TIME;
                                if (latencyTime/TimeUtil.MILLIS_PER_SECOND > LONG_FILE_PROCESS_WARNING_THRESHOLD ||
                                        processTime/TimeUtil.MILLIS_PER_SECOND > LONG_FILE_PROCESS_WARNING_THRESHOLD) {
                                    statusHandler.warn("File " + newFile.getAbsolutePath() + " took longer than "
                                            + LONG_FILE_PROCESS_WARNING_THRESHOLD + "s to process. "
                                            + " File process time: " + TimeUtil.prettyDuration(processTime)
                                            + ", File latency time: " + TimeUtil.prettyDuration(latencyTime));
                                } else {
                                    statusHandler.info("File " + newFile.getAbsolutePath() + " processed."
                                            + " File process time: " + TimeUtil.prettyDuration(processTime)
                                            + ", File latency time: " + TimeUtil.prettyDuration(latencyTime));
                                }
                            } catch (IOException e) {
                                statusHandler.handle(Priority.ERROR, "Unable to create and move file ["
                                        + path.toString() + fileName + "]", e);
                            } catch (EdexException e) {
                                statusHandler.handle(Priority.ERROR, "Failed to insert file [" 
                                        + path.toString() + fileName + "] into NSBN ingest stream.", e);
                            }
                        }
                    });
                    break;
                }
            }
        }
            
        if (!fileAccepted) {
            // If the file is not recognized or is excluded,
            //   then move the file to the rejected directory
            rejectedExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    StringBuilder rejectPath = new StringBuilder(inFile.getPath().length());
                    rejectPath.append(File.separator).append(NSBN_STORE)
                        .append(File.separator).append("rejected_files").append(File.separator)
                        .append(fileParent).append(File.separatorChar)
                        .append(sdfs.get().format(timeAdded)).append(File.separatorChar);

                    try {
                        createAndMoveNewFile(rejectPath, inFile);
                    } catch (IOException e) {
                        statusHandler.handle(Priority.ERROR, 
                                "Unable to create and move rejected file ["
                                + path.toString() + fileName + "]", e);
                    }
                    statusHandler.warn("The file " + inFile.getAbsolutePath()
                        + " was rejected for transfer.");
                }
            });
            
        }
        
    }

    private File createAndMoveNewFile(StringBuilder path, File inFile) throws IOException {
        File dir = new File(path.toString());
        File newFile = new File(dir, inFile.getName());
        Path oldPath = inFile.toPath();
        Path newPath = newFile.toPath();

        if (!dir.exists()) {
            Files.createDirectories(dir.toPath(), POSIX_DIRECTORY_ATTRIBUTES);
        }

        Files.move(oldPath, newPath);

        try {
            IOPermissionsHelper.applyFilePermissions(newPath,
                    POSIX_FILE_SET);
        } catch (Exception e1) {
            statusHandler.handle(Priority.WARN, e1.getMessage(), e1);
        }

        statusHandler.handle(Priority.INFO,
                "TransferFile: " + inFile.getAbsolutePath());

        return newFile;
    }
}
