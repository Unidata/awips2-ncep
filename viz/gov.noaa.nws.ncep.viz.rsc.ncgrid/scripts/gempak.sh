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
# Aug 30, 2018  7417    mapeters    Initial creation
#


user=$(/usr/bin/whoami)
if [ ${user} == 'root' ];then
   echo "WARNING: CAVE cannot be run as user '${user}'!"
   echo "         change to another user and run again."
   exit 1
fi

source /awips2/cave/caveUtil.sh
RC=$?
if [ ${RC} -ne 0 ]; then
   echo "ERROR: unable to find and/or access /awips2/cave/caveUtil.sh."
   exit 1
fi

COMPONENT="gempak"
OPTIONAL_ARGS=(-noredirect)
export PROGRAM_NAME=${COMPONENT}

export IGNORE_NUM_CAVES=1

trap killIt TERM

function killIt() {
    kill ${PID}
}

exec /awips2/cave/cave.sh -nosplash "${OPTIONAL_ARGS[@]}" -component ${COMPONENT} "$@" &
PID=$!
wait ${PID}
exitCode=$?

exit $exitCode