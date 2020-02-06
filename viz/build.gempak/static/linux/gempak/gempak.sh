#!/bin/bash
# GEMPAK startup script
# Note: GEMPAK will not run as 'root'

# This software was developed and / or modified by Raytheon Company,
# pursuant to Contract DG133W-05-CQ-1067 with the US Government.
#
# U.S. EXPORT CONTROLLED TECHNICAL DATA
# This software product contains export-restricted data whose
# export/transfer/disclosure is restricted by U.S. law. Dissemination
# to non-U.S. persons whether in the United States or abroad requires
# an export license or other authorization.
#
# Contractor Name:        Raytheon Company
# Contractor Address:     6825 Pine Street, Suite 340
#                         Mail Stop B8
#                         Omaha, NE 68106
#                         402.291.0100
#
# See the AWIPS II Master Rights File ("Master Rights File.pdf") for
# further licensing information.
#
#
# SOFTWARE HISTORY
# Date          Ticket# Engineer    Description
# ------------- ------- ----------- --------------------------
# Aug 30, 2018  54480   mapeters    Initial creation
# Oct 08, 2018  54483   mapeters    Refactored for separate GEMPAK RPM (instead
#                                   of wrapping /awips2/cave/cave)
# Jan 09, 2020  7606      randerso    Remove jre directory level from JAVA_HOME
#


user=$(/usr/bin/whoami)
if [ ${user} == 'root' ];then
   echo "WARNING: GEMPAK cannot be run as user '${user}'!"
   echo "         change to another user and run again."
   exit 1
fi

# We will no longer be using hard-coded paths that need to be replaced.
# Use rpm to find the paths that we need.
JAVA_INSTALL="/awips2/java"
RC="$?"
if [ ! "${RC}" = "0" ]; then
   echo "ERROR: awips2-java Must Be Installed."
   echo "Unable To Continue ... Terminating."
   exit 1
fi
PYTHON_INSTALL="/awips2/python"
RC="$?"
if [ ! "${RC}" = "0" ]; then
   echo "ERROR: awips2-python Must Be Installed."
   echo "Unable To Continue ... Terminating."
   exit 1
fi
GEMPAK_INSTALL="/awips2/gempak"

path_to_script=$(readlink -f "$0")
dir=$(dirname "$path_to_script")

export AWIPS_INSTALL_DIR=${GEMPAK_INSTALL}

export LD_LIBRARY_PATH=${JAVA_INSTALL}/lib:$LD_LIBRARY_PATH
export PATH=${JAVA_INSTALL}/bin:$PATH
export JAVA_HOME="${JAVA_INSTALL}"

exitVal=1

#check for the logs directory, which may not be present at first start
hostName=$(hostname -s)
# Logback configuration files will append user.home to LOGDIR.
export LOGDIR=caveData/logs/consoleLogs/"$hostName"/
FULL_LOGDIR="$HOME/$LOGDIR"

if [ ! -d "$FULL_LOGDIR" ]; then
 mkdir -p "$FULL_LOGDIR"
fi

SWITCHES=()

# Delete old Eclipse configuration directories that are no longer in use
function deleteOldEclipseConfigurationDirs()
{
    local tmp_dir=$1
    local tmp_dir_pat=$(echo "$tmp_dir" | sed -e 's/|/\\|/g')
    save_IFS=$IFS
    IFS=$'\n'
    # Find directories that are owned by the user and  older than one hour
    local old_dirs=( $(find "$tmp_dir" -mindepth 1 -maxdepth 1 -type d -user "$USER" -mmin +60) )
    IFS=$save_IFS
    if (( ${#old_dirs[@]} < 1 )); then
        return
    fi
    # Determine which of those directories are in use.
    local lsof_args=()
    for d in "${old_dirs[@]}"; do
        lsof_args+=('+D')
        lsof_args+=("$d")
    done
    IFS=$'\n'
    # Run lsof, producing machine readable output, filter the out process IDs,
    # the leading 'n' of any path, and any subpath under a configuration
    # directory.  Then filter for uniq values.
    in_use_dirs=$(lsof -w -n -l -P -S 10 -F pn "${lsof_args[@]}" | grep -v ^p | \
        sed -r -e 's|^n('"$tmp_dir_pat"'/[^/]*).*$|\1|' | uniq)
    IFS=$save_IFS
    for p in "${old_dirs[@]}"; do
        if ! echo "$in_use_dirs" | grep -qxF "$p"; then
            rm -rf "$p"
        fi
    done
}

function deleteEclipseConfigurationDir()
{
    if [[ -n $eclipseConfigurationDir ]]; then
        sleep 2
        rm -rf "$eclipseConfigurationDir"
    fi
}

function createEclipseConfigurationDir()
{
    local d dir id=$(hostname)-$(whoami)
    for d in "/local/cave-eclipse/" "$HOME/.cave-eclipse/"; do
        if [[ $d == $HOME/* ]]; then
            mkdir -p "$d" || continue
        fi
        deleteOldEclipseConfigurationDirs "$d"
        if dir=$(mktemp -d --tmpdir="$d" "${id}-XXXX"); then
            eclipseConfigurationDir=$dir
            trap deleteEclipseConfigurationDir EXIT
            SWITCHES+=(-configuration "$eclipseConfigurationDir")
            return 0
        fi
    done
    echo "Unable to create a unique Eclipse configuration directory.  Will proceed with default." >&2
    return 1
}

createEclipseConfigurationDir
TMP_VMARGS="--launcher.appendVmargs -vmargs -Djava.io.tmpdir=${eclipseConfigurationDir}"

# takes in a process id
# kills spawned subprocesses of pid
# and then kills the process itself and exits
function cleanExit()
{
    pid=$1
    if [[ -n $pid ]]
    then
        pkill -P $pid
        kill $pid
    fi
    exit
}
trap 'cleanExit $pid' SIGHUP SIGINT SIGQUIT SIGTERM

VERSION_ARGS=()
if [ -f "${dir}"/awipsVersion.txt ]; then
   prevIFS=${IFS}
   IFS=$'\n'
   for line in $(cat "${dir}"/awipsVersion.txt); do
      VERSION_ARGS+=("${line}")
   done
   IFS=${prevIFS}
fi

# VERSION_ARGS includes jvm arguments so it must always be at the end of the argument
# sequence passed to GEMPAK.
if [ -w $FULL_LOGDIR ] ; then
    ${dir}/gempak "${SWITCHES[@]}" $* ${TMP_VMARGS} "${VERSION_ARGS[@]}" >/dev/null 2>&1 &
else
    ${dir}/gempak "${SWITCHES[@]}" $* ${TMP_VMARGS} "${VERSION_ARGS[@]}" &
fi
pid=$!
wait $pid
exitVal=$?

exit $exitVal