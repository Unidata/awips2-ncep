#!/awips2/python/bin/python
# filename: convertNcepGridTitleStrings.py
#
# This python script converts the TITLE string in a GRID attribute file (*.attr)
# from the legacy convention that the time information was put at the beginning of the string
# to the AWIPS2 convention that the time information is right justified at the end of string.
#
# For example, the TITLE string
#       TITLE = 1/-1/?~^ (~995 MB) MOISTURE CONVERGENCE, WIND (KTS), THTAE!0
# will be converted to
#       TITLE = 1/-1/(\~995 MB) MOISTURE CONVERGENCE, WIND (KTS), THTAE ?^!0
# where the '\' character (ESCAPE) is used to "escape" a special character from 
# its interpretation and treated literally.
#
# For more information, see the TITLE help file (TITLE.help) and NCEP Redmine ticket R20009.

import sys
import re

charForValidTime = '~'
charForForecastTime = '^'
charForDayOfWeek = '?'
charForComment = '!'
charForSeparation = '|'

def rearrangeLegendString( inputString ):

	myStr = ''
	comment1 = ''
	comment2 = ''
	shortTitleString = ''
	hasComment1 = False

	inputString = inputString.strip()

	hasTwoStrings = re.match( r'([^|]*)\|(.*)', inputString )
	if not hasTwoStrings:
		hasComment1 = re.match( r'([^!]*)!(.*)', inputString )
		if not hasComment1:
			myStr = inputString
		else:
			myStr = hasComment1.group(1)
			comment1 = hasComment1.group(2)
	else:
		shortTitleString = hasTwoStrings.group(2)

		hasComment1 = re.match( r'([^!]*)!(.*)', hasTwoStrings.group(1) )
		if not hasComment1:
			myStr = hasTwoStrings.group(1)
		else:
			myStr = hasComment1.group(1)
			comment1 = hasComment1.group(2)

	
	positionValid = myStr.find( charForValidTime )
	positionForecast = myStr.find( charForForecastTime )
	isCharForDayOfWeekFound = charForDayOfWeek in myStr
	
	if positionValid >= 0 or positionForecast >= 0:
		myStr = myStr.replace( charForValidTime, '', 1 )
		myStr = myStr.replace( charForValidTime, '\\' + charForValidTime )

		myStr = myStr.replace( charForForecastTime, '', 1 )
		myStr = myStr.replace( charForForecastTime, '\\' + charForForecastTime )

		myStr = myStr.replace( charForDayOfWeek, '', 1)
		myStr = myStr.replace( charForDayOfWeek, '\\' + charForDayOfWeek)
		myStr = myStr.strip()

	if positionValid == 0 or positionValid > positionForecast:
		if isCharForDayOfWeekFound:
			myStr += ' ?~'
		else:
			myStr += ' ~'
	elif positionForecast >= 0:
		if isCharForDayOfWeekFound:
			myStr += ' ?^'
		else:
			myStr += ' ^'

	if hasTwoStrings:
		myStr += charForSeparation + shortTitleString
	elif hasComment1:
		myStr += charForComment + comment1

	return myStr


for line in sys.stdin:
	line = line.strip()
	titleString = re.match( r'^TITLE([^/]*)/([^/]*)/(.*)', line)
	if titleString:
		# print '*' + line 
		oldLegendString = titleString.group(3)
		newLegendString = rearrangeLegendString( oldLegendString )
		newTitleString = 'TITLE' + titleString.group(1) + '/' + titleString.group(2) + '/' + newLegendString 
		print newTitleString
	else:
		print line

