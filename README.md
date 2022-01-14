## GlucoStatusFX


[![GitHub stars](https://badgen.net/github/stars/HanSolo/glucostatusfx)](https://GitHub.com/HanSolo/glucostatusfx/stargazers/)

[![GitHub license](https://badgen.net/github/license/HanSolo/glucostatusfx)](https://github.com/HanSolo/glucostatusfx/blob/main/LICENSE)

<br>
A glucose status monitor for Nightscout implemented in JavaFX.

Donations are welcome at [Paypal](https://paypal.me/hans0l0)

## Intro
GlucoStatusFX is a JavaFX application that will sit in your menu bar and is visualizing
data coming from a nightscout server.
To use this app you need to have a nightscout server which url you have to put in the
text field in the settings dialog.
In the settings dialog you can will find the following parameters:

<b>"Unit mg/dl":</b><br>
Whatever unit you have defined on your nightscout server you can change the display to either mg/dl or mmol/l. In case you would like to see the values in mg/dl, please enable the switch "Unit mg/dl". If you disable this switch the values will be shown in mmol/l.

<b>"Low value notification":</b><br>
Enable the switch if you would like to receive notifications for low values (in GlucoStatusFX acceptable low means values between min acceptable and 55 mg/dl which is min critical).

<b>"Acceptable low value notification":</b><br>
Enable the switch if you would like to receive notifications for acceptable low values (in GlucoStatusFX acceptable low means values between min normal and min acceptable values).

<b>"Acceptable high value notification":</b><br>
Enable the switch if you would like to receive notifications for acceptable high values (in GlucoStatusFX acceptable high means values between max normal and max acceptable values).

<b>"High value notification":</b><br>
Enable the switch if you would like to receive notifications for high values (in GlucoStatusFX high means values between max acceptable and 350 mg/dl which is max critical).

<br>
<b>"Too low notification interval":</b><br>
You can define the interval for notifications of too low values in a range of 5-10 minutes. In case of too low values you will receive notifications in the given interval.

<b>"Too high notification interval":</b><br>
You can define the interval for notifications of too high values in a range of 5-30 minutes. In case of too high values you will receive notifications in the given interval.

<br>
<b>"Min acceptable":</b><br>
The min acceptable value can be defined in the range of 60-70 mg/dl.

<b>"Min normal":</b><br>
The min normal value can be defined in the range of 70-80 mg/dl.

<b>"Max normal":</b><br>
The max normal value can be defined in the range of 120-160 mg/dl.

<b>"Max acceptable":</b><br>
The max acceptable value can be defined in the range of 120-250 mg/dl.

## Overview
![Main Screen](https://i.ibb.co/gP5w82q/Monosnap-2022-01-14-16-24-52.png)

![Settings](https://i.ibb.co/BT53NY4/Monosnap-2022-01-14-16-25-06.png)

![Pattern](https://i.ibb.co/6Rz4kZ6/Monosnap-2022-01-14-16-25-19.png)

![Time in range](https://i.ibb.co/1Gmh6tx/Monosnap-2022-01-14-16-25-32.png)


