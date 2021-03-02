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
import java.nio.file.FileAlreadyExistsException;
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
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationUtil;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.SingleTypeJAXBManager;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.SimulatedTime;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.file.IOPermissionsHelper;
import com.raytheon.uf.edex.core.EDEXUtil;

/**
 * A bean that moves incoming NSBN files to their designated directories and
 * then informs the configured ingest queues for processing.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer     Description
 * ------------- -------- ------------ -----------------------------------------
 * Oct 12, 2018  56039    mrichardson  Initial creation
 * Dec 10, 2018  56039    tjensen      Use Canonical paths, refactor threading,
 *                                     handle retransmissions
 * Jun 07, 2019  64732    tjensen      Improve exception handling, logging
 * Jan 28, 2020  73722    smanoj       Fix to properly handle multiple
 *                                     localization levels
 * Mar  3, 2021  8326     tgurney      Camel 3 fixes
 *
 * </pre>
 *
 * @author mrichardson
 */

public class NSBNFileTransfer implements Processor {
    private static final String NSBN_REJECTED_EXECUTOR = "NSBNRejectedExecutor";

    private static final String NSBN_SHARED_EXECUTOR = "NSBNSharedExecutor";

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(getClass());

    private static final String BASE_DIR_PROPERTY = "nsbn.drop.box";

    private static final String DATE_SUBDIR_PROPERTY = "add.date.to.nsbn.dest.sub.dirs";

    private static final long FILE_PROCESS_WARNING_THRESHOLD = Long
            .getLong("nsbn.file.process.warning");

    private static final int NSBN_DEFAULT_THREADS = Integer
            .getInteger("nsbn.default.pool.threads");

    private static final int NSBN_REJECT_THREADS = Integer
            .getInteger("nsbn.reject.pool.threads");

    private static final String NSBN_STORE = "nsbn_store";

    private static final Path BASE_PATH = Paths
            .get(System.getProperty(BASE_DIR_PROPERTY));

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

    private Map<String, List<NSBNTransferDirectory>> nsbnTransferDirectoryMap;

    private final ThreadLocal<SimpleDateFormat> sdfs = TimeUtil
            .buildThreadLocalSimpleDateFormat(
                    "yyyy" + File.separatorChar + "MM" + File.separatorChar
                            + "dd" + File.separatorChar + "HH",
                    TimeUtil.GMT_TIME_ZONE);

    private static class NamedThreadFactory implements ThreadFactory {

        private final ThreadGroup group;

        private final AtomicInteger threadNumber = new AtomicInteger(1);

        private final String namePrefix;

        public NamedThreadFactory(String namePrefix) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup()
                    : Thread.currentThread().getThreadGroup();
            this.namePrefix = namePrefix + "-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

    public NSBNFileTransfer() {
        initNSBNTransferMap();
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        File file = exchange.getIn().getBody(File.class);

        if (file != null) {
            moveFileToDestinationDir(exchange);
        } else {
            // No file received
            exchange.setRouteStop(true);
        }
    }

    private synchronized void initNSBNTransferMap() {
        statusHandler.info("Initializing NSBN transfer mapping...");
        IPathManager pathMgr = PathManagerFactory.getPathManager();
        LocalizationLevel[] levels = new LocalizationLevel[] {
                LocalizationLevel.SITE, LocalizationLevel.BASE };

        Map<LocalizationLevel, ? extends ILocalizationFile> files = pathMgr
                .getTieredLocalizationFile(LocalizationType.COMMON_STATIC,
                        LocalizationUtil.join("nsbn",
                                "nsbnTransferDirectories.xml"));

        NSBNTransferDirectorySet nsbnTransferDirectorySet;
        nsbnTransferDirectoryMap = new HashMap<>();
        threadPoolExecutorMap = new HashMap<>();

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                NSBN_DEFAULT_THREADS, NSBN_DEFAULT_THREADS, Long.MAX_VALUE,
                TimeUnit.NANOSECONDS, new LinkedBlockingQueue<Runnable>(),
                new NamedThreadFactory("NSBNDefaultPool"));
        threadPoolExecutorMap.put(NSBN_SHARED_EXECUTOR, executor);

        executor = new ThreadPoolExecutor(NSBN_REJECT_THREADS,
                NSBN_REJECT_THREADS, Long.MAX_VALUE, TimeUnit.NANOSECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new NamedThreadFactory("NSBNRejectPool"));
        threadPoolExecutorMap.put(NSBN_REJECTED_EXECUTOR, executor);

        SingleTypeJAXBManager<NSBNTransferDirectorySet> mgr = SingleTypeJAXBManager
                .createWithoutException(NSBNTransferDirectorySet.class);
        for (LocalizationLevel level : levels) {
            ILocalizationFile nsbnTransferFile = files.get(level);
            if (nsbnTransferFile == null) {
                continue;
            }

            try (InputStream is = nsbnTransferFile.openInputStream()) {
                nsbnTransferDirectorySet = mgr.unmarshalFromInputStream(is);
                statusHandler.info(String.format("Using nsbnTransferFile [%s]",
                        nsbnTransferFile));

                for (NSBNTransferDirectory nsbnTransferDirectory : nsbnTransferDirectorySet
                        .getDirectories()) {
                    String idx = nsbnTransferDirectory.getId();
                    File destPath = new File(
                            nsbnTransferDirectory.getDestinationDir());
                    List<NSBNTransferDirectory> dir = nsbnTransferDirectoryMap
                            .get(idx);

                    if (!destPath.isDirectory()) {
                        Files.createDirectories(destPath.toPath(),
                                POSIX_DIRECTORY_ATTRIBUTES);
                    }

                    if (dir == null) {
                        dir = new ArrayList<>(1);
                        nsbnTransferDirectoryMap.put(idx, dir);
                    }
                    nsbnTransferDirectory.compileRegexes();
                    dir.add(nsbnTransferDirectory);

                    if (nsbnTransferDirectory.getThreadpoolCount() > 0) {
                        executor = new ThreadPoolExecutor(
                                nsbnTransferDirectory.getThreadpoolCount(),
                                nsbnTransferDirectory.getThreadpoolCount(),
                                Long.MAX_VALUE, TimeUnit.NANOSECONDS,
                                new LinkedBlockingQueue<Runnable>(),
                                new NamedThreadFactory(
                                        "NSBN" + nsbnTransferDirectory.getId()
                                                + "Pool"));
                        threadPoolExecutorMap.put(idx, executor);
                    }
                }
            } catch (LocalizationException | IOException
                    | SerializationException e) {
                statusHandler.error(
                        "Error unmarshalling post processed data type list: "
                                + nsbnTransferFile,
                        e);
            }
        }
    }

    /**
     * Moves the file defined in the exchange param to an archive directory and
     * sits the file in a thread pool of a corresponding queue to ingest.
     *
     * @param exchange
     * @throws IOException
     */
    public void moveFileToDestinationDir(Exchange exchange) throws IOException {
        File inFile = exchange.getIn().getBody(File.class).getCanonicalFile();
        final long START_METHOD_TIME = System.currentTimeMillis();
        final long FILE_MODIFIED_TIME = inFile.lastModified();
        String fileName = inFile.getAbsolutePath();
        statusHandler.info("File found for processing: " + fileName);
        boolean fileAccepted = false;
        StringBuilder path = new StringBuilder(fileName.length());
        String destDir = "";
        Date timeAdded = SimulatedTime.getSystemTime().getTime();
        NSBNTransferDirectory currDirectory;
        Path sourcePath = inFile.toPath();
        int baseDepth = BASE_PATH.getNameCount();
        Path sourceParent = sourcePath.getName(baseDepth);
        File srcParentAsFile = sourceParent.toFile();
        String fileParent = srcParentAsFile.getName();
        ThreadPoolExecutor executor;

        for (Entry<String, List<NSBNTransferDirectory>> directory : nsbnTransferDirectoryMap
                .entrySet()) {
            currDirectory = nsbnTransferDirectoryMap.get(directory.getKey())
                    .get(0);
            if (fileParent.equals(currDirectory.getScanDir())) {
                fileAccepted = currDirectory.accept(inFile);

                /*
                 * Only continue moving the file over if the file matches the
                 * configured include pattern(s) and does not fall under any
                 * exclude pattern(s) (if provided)
                 */
                if (fileAccepted) {
                    destDir = currDirectory.getDestinationDir()
                            + File.separatorChar;
                    final String destQueue = currDirectory
                            .getDestinationQueue();
                    path.append(destDir);

                    /*
                     * If configured to do so, append sub-directories to the
                     * path based on the date
                     */
                    if (Boolean.getBoolean(DATE_SUBDIR_PROPERTY)) {
                        path.append(sdfs.get().format(timeAdded))
                                .append(File.separatorChar);
                    }

                    executor = threadPoolExecutorMap.get(currDirectory.getId());

                    if (executor == null) {
                        executor = threadPoolExecutorMap
                                .get(NSBN_SHARED_EXECUTOR);
                    }

                    executor.execute(() -> {
                        File newFile;
                        try {
                            newFile = createAndMoveNewFile(path, inFile);
                            Map<String, Object> headers = new HashMap<>();
                            headers.put("enqueueTime",
                                    System.currentTimeMillis());

                            // Notify ingest of the new file
                            EDEXUtil.getMessageProducer().sendAsyncUri(
                                    "jms-durable:queue:" + destQueue,
                                    newFile.getPath(), headers);

                            long stopMethodTime = System.currentTimeMillis();
                            long processTime = stopMethodTime
                                    - START_METHOD_TIME;
                            long latencyTime = stopMethodTime
                                    - FILE_MODIFIED_TIME;
                            if (latencyTime
                                    / TimeUtil.MILLIS_PER_SECOND > FILE_PROCESS_WARNING_THRESHOLD
                                    || processTime
                                            / TimeUtil.MILLIS_PER_SECOND > FILE_PROCESS_WARNING_THRESHOLD) {
                                statusHandler.warn("File "
                                        + newFile.getAbsolutePath()
                                        + " took longer than "
                                        + FILE_PROCESS_WARNING_THRESHOLD
                                        + "s to process. "
                                        + " File process time: "
                                        + TimeUtil.prettyDuration(processTime)
                                        + ", File latency time: "
                                        + TimeUtil.prettyDuration(latencyTime));
                            } else {
                                statusHandler.info("File "
                                        + newFile.getAbsolutePath()
                                        + " sent to queue '" + destQueue
                                        + "'. File process time: "
                                        + TimeUtil.prettyDuration(processTime)
                                        + ", File latency time: "
                                        + TimeUtil.prettyDuration(latencyTime));
                            }
                        } catch (IOException e1) {
                            statusHandler
                                    .error("Unable to create and move file ["
                                            + fileName + "]", e1);
                        } catch (Exception e2) {
                            statusHandler.error("Failed to insert file ["
                                    + fileName + "] into NSBN ingest stream.",
                                    e2);
                        }
                    });
                    break;
                }
            }
        }

        if (!fileAccepted) {
            /*
             * If the file is not recognized or is excluded, then move the file
             * to the rejected directory
             */
            executor = threadPoolExecutorMap.get(NSBN_REJECTED_EXECUTOR);
            executor.execute(() -> {
                StringBuilder rejectPath = new StringBuilder(
                        inFile.getPath().length());
                rejectPath.append(File.separator).append(NSBN_STORE)
                        .append(File.separator).append("rejected_files")
                        .append(File.separator).append(fileParent)
                        .append(File.separatorChar)
                        .append(sdfs.get().format(timeAdded))
                        .append(File.separatorChar);

                try {
                    createAndMoveNewFile(rejectPath, inFile);
                } catch (IOException e) {
                    statusHandler
                            .error("Unable to create and move rejected file ["
                                    + path.toString() + fileName + "]", e);
                }
                statusHandler.warn("The file " + inFile.getAbsolutePath()
                        + " was rejected for transfer.");
            });

        }

    }

    private File createAndMoveNewFile(StringBuilder path, File inFile)
            throws IOException {
        File dir = new File(path.toString());
        String fileName = inFile.getName();
        File newFile = new File(dir, fileName);
        Path oldPath = inFile.toPath();
        Path newPath = newFile.toPath();

        if (!dir.exists()) {
            Files.createDirectories(dir.toPath(), POSIX_DIRECTORY_ATTRIBUTES);
        }
        boolean moved = false;
        int attempt = 1;
        while (!moved && attempt < 50) {

            try {
                Files.move(oldPath, newPath);
                moved = true;
            } catch (FileAlreadyExistsException e) {
                /**
                 * If file already exists, assume this is an update and move the
                 * file with a numbered suffix.
                 */
                newFile = new File(dir, fileName + "." + attempt);
                newPath = newFile.toPath();
                attempt++;
            }
        }

        try {
            IOPermissionsHelper.applyFilePermissions(newPath, POSIX_FILE_SET);
        } catch (Exception e1) {
            statusHandler.warn(e1.getMessage(), e1);
        }

        statusHandler.info("TransferFile: " + inFile.getAbsolutePath());

        return newFile;
    }
}
