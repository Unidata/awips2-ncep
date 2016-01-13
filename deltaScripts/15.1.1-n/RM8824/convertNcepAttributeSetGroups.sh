#!/bin/bash
# convertNcepAttributeSetGroups.sh
# 
# This bash script converts xml files, specified in a variable "xmlFiles",
# from the <attrSetNames> format to the <attrSetLabels> format, 
# by using a python script, convertNcepAttributeSetGroups.py,
# which is located in the same directory as the bash script.
#
# Specifically,
#
#	<attrSetNames>asn1,asn2,...,asnN</attrSetNames>
#
# will be coverted to
#
#	<attrSetLabels>
#		<attrSetLabel>
#			<name>asn1</name>
#		</attrSetLabel>
#		...
#		<attrSetLabel>
#			<name>asnN</name>
#		</attrSetLabel>
#	</attrSetLabels>
#
# For more information, see Redmine ticket R8824.

xmlFiles="/awips2/edex/data/utility/cave_static/*/*/ncep/AttributeSetGroups/*/*.xml"
convertingScript=./convertNcepAttributeSetGroups.py

LS=`ls $xmlFiles`

if [ $? -ne 0 ]; then
    echo "exiting: No xml files found"
exit 1
fi

for file in $LS
do
    echo "converting $file ..."
    $convertingScript < $file > /tmp/myTemp.xml; exitCode="$?"
    if [ $exitCode = 0 ]
    then
         mv -f /tmp/myTemp.xml $file
    elif [ $exitCode = 1 ]
    then
        echo "WARNING: Not well-formed xml file! Giving up..."
    elif [ $exitCode = 2 ]
    then
        echo "WARNING: The tag AttributeSetGroup not found!  Giving up..."
    elif [ $exitCode = 3 ]
    then
        echo "WARNING: The tag attrSetNames not found!  Giving up..."
    else
        echo "WARNING: unknown exit code " + $exitCode
    fi
done

echo "INFO: The conversion has completed!"
exit 0

