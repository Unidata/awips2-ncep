#!/awips2/python/bin/python
# convertNcepAttributeSetGroups.py
# 
# This script converts an xml file from standard input with the <attrSetNames> format 
# to standard output with the <attrSetLabels> format.
#
# Specifically,
#       <attrSetNames>asn1,asn2,...,asnN</attrSetNames>
# will be coverted to
#       <attrSetLabels>
#               <attrSetLabel>
#                       <name>asn1</name>
#               </attrSetLabel>
#               ...
#               <attrSetLabel>
#                       <name>asnN</name>
#               </attrSetLabel>
#       </attrSetLabels>
#
# For more information, see NCEP Redmine ticket R8824.

import sys
import re
import xml.etree.ElementTree as ET

def indent(elem, level=0):
    i = "\n" + level*"  "
    if len(elem):
        if not elem.text or not elem.text.strip():
            elem.text = i + "  "
        if not elem.tail or not elem.tail.strip():
            elem.tail = i
        for elem in elem:
            indent(elem, level+1)
        if not elem.tail or not elem.tail.strip():
            elem.tail = i
    else:
        if level and (not elem.tail or not elem.tail.strip()):
            elem.tail = i

try:
    tree = ET.parse(sys.stdin)
except:
    print 'Not well-formed xml file! Exiting...'
    sys.exit(2)

root = tree.getroot()
if root.tag != 'AttributeSetGroup':
    print 'The tag AttributeSetGroup not found!  Exiting...'
    sys.exit(2)

oldElement = root.find('attrSetNames')

if oldElement is None:
    print 'The tag attrSetNames not found!  Exiting...'
    sys.exit(2)

oldContents  = oldElement.text
root.remove(oldElement)
newElement = ET.SubElement(root, 'attrSetLabels')

if oldContents:
    for name in re.split(',', oldContents):
       labelElement = ET.SubElement(newElement, 'attrSetLabel')
       nameElement = ET.SubElement(labelElement, 'name')
       nameElement.text = name

indent(root)
xmlString = ET.tostring(root, encoding="UTF-8", method="xml")
print xmlString
sys.exit(0)


