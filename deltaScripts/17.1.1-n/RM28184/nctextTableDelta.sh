#!/bin/bash
#
# nctextTableDelta.sh
###############################################################
#   
#   SOFTWARE HISTORY
#   
#   Date            Ticket#       Engineer       Description
#   ------------    ----------    -----------    ------------
#   02/23/2017      RM28184       Chin Chen      Initial Creation
#  
#  Purpose:  used to run a SQL script to add nctext_tafstn table 
#            to database
#  Database: metadata 
#  Schema:   awips
#  SQL file: createNctextTafstnTbl.sql
#  
##############################################################
SQL_SCRIPT="createNctextTafstnTbl.sql"

# ensure that the sql script is present
if [ ! -f ${SQL_SCRIPT} ]; then
   echo "ERROR: the required sql script - ${SQL_SCRIPT} was not found."
   echo "FATAL: the update has failed!"
   exit 1
fi

echo "INFO: adding nctext_tafstn table to the metadata database"

# run the sql script
/awips2/psql/bin/psql -U awipsadmin -d metadata -f ${SQL_SCRIPT}

exit 0


