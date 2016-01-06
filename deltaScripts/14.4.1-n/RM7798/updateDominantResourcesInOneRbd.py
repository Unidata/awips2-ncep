#!/usr/bin/python
# This script will repair (if needed) SPF/RBD XML files saved by users during a 
# period when a bug in the system caused certain RBDs to be written incorrectly. 

# Specifically, the bug would cause the "dominantResourceName" attribute
# of timeMatcher elements to be written with a fixed cycle time for GRID
# and NTRANS resources, even if "Save Source Timestamp As:" option was set
# to LATEST during Save RBD.  This script will update these fixed cycle times
# (such as "(150511_06)") to "(LATEST)" in the timeMatcher dominantResourceName
# IF the resource name with the fixed cycle time does not match any displayed
# resource in the RBD, but does match such a resource with "(LATEST)".
#
# (See NCEP Redmine Issues #4983 and #7798 for more information.)
#
# This update only needs to be run individually if there are saved RBDs 
# being stored outside of localization.  For procedures saved in localization,
# updateRbds.sh will automatically call this on each one.

'''
Created on Jun 19, 2015

@author: bhebbard
'''

import sys
import xml.etree.ElementTree as ET

def updateRbdDominantResources(RbdFile):
#   print "Parsing RBD XML file at " + RbdFile
    try:
        tree = ET.parse(RbdFile)
    except:
        print "No changes made to file " + RbdFile + ", which does not contain a valid XML document"
        print ""
        return
    root = tree.getroot()
    fileUpdated = False

# Get a list of the resourceName of all displayed resources

    displayedResources = root.findall(".//resource/resourceData")
    displayedResourceNames = []
    for resource in displayedResources:
#        print resource
        displayedResourceNames.append(resource.get("resourceName"))
#    print displayedResourceNames

# For each timeMatcher element, fix the dominantResourceName attribute if necessary.
# Specifically,if its cycle time is NOT "LATEST" and it fails to match the resourceName
# of any displayed resource in the RBD, then see if switching it to LATEST will cause
# it to match one.

    for timeMatcher in root.findall(".//timeMatcher"):
        dominantResourceName = timeMatcher.get("dominantResourceName")
#       print "dominantResourceName is...", dominantResourceName
        if dominantResourceName == None or dominantResourceName == "" or dominantResourceName.find("(LATEST)") != -1:
#           dominantResourceName is null, empty, or already contains (LATEST), so no fix needed/possible
            continue
        if dominantResourceName in displayedResourceNames:
#           dominantResourceName, though it doesn't contain (LATEST), DOES match a displayed resource, so no fix needed
            continue
#       dominantResourceName doesn't contain (LATEST) and doesn't match a displayed resource, so let's try to fix it...
        if dominantResourceName.find("(") == -1:
#           dominantResourceName dosn't contain a cycle time (or any '(') to try fixing, so no fix is possible"
            continue
        dominantResourceNameSplit = dominantResourceName.split("(")
        dominantResourceNameSplit[-1] = "LATEST)"
        dominantResourceNameWithLatest = "(".join(dominantResourceNameSplit)
        if dominantResourceNameWithLatest in displayedResourceNames:
#           dominantResourceName with (LATEST) substituted DOES match a displayed resource, so let's change it to that
            print 'Changing dominantResourceName from "' + dominantResourceName + '"'
            print '                                to "' + dominantResourceNameWithLatest + '"'
            timeMatcher.set("dominantResourceName", dominantResourceNameWithLatest)
            fileUpdated = True
            continue
#        if here dominantResourceName with (LATEST) substituted STILL DOESN'T MATCH any displayed resource...
#        ...  This shouldn't happen, but let's just leave it alone, and continue loop to next timeMatcher"
    
    if fileUpdated:
        print "Writing changes to file " + RbdFile
        tree.write(RbdFile)
    else:
        print "No changes made to file " + RbdFile
    print ""
    
    
if __name__ == '__main__':
    for arg in sys.argv[1:]:
        updateRbdDominantResources(arg)
