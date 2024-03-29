## GlucoStatusFX

![banner](https://github.com/HanSolo/glucostatusfx/raw/main/src/main/resources/eu/hansolo/fx/glucostatus/icon128x128.png)

<br>
A glucose status monitor for Nightscout implemented in JavaFX.

<br>
<br>

![GitHub Workflow Status](https://img.shields.io/github/workflow/status/HanSolo/glucostatusfx/ci)
![latest tag](https://badgen.net/github/tag/HanSolo/glucostatusfx)
![stars](https://badgen.net/github/stars/HanSolo/glucostatusfx)
![GitHub all releases](https://img.shields.io/github/downloads/HanSolo/glucostatusfx/total)
![license](https://badgen.net/github/license/HanSolo/discocli)

[![JFXCentral](https://img.shields.io/badge/Find_me_on-JFXCentral-blue?logo=googlechrome&logoColor=white)](https://www.jfx-central.com/downloads/glucostatusfx)

<br>
<br>
<b>Running on Linux</b>

When running as jar file you might need to start with on Linux

```java -Djdk.gtk.version=2 -jar GlucoStatusFX-17.0.61.jar```

<br>


## Intro
GlucoStatusFX is a JavaFX application that will sit in your menu bar and is visualizing
data coming from a nightscout server.
To use this app you need to have a nightscout server which url you have to put in the
text field in the settings dialog.

To set up the nightscout url in the settings dialog, just put in the url of your server e.g.

```https://my-glucose.herokuapp.com```

The api secret field is optional (but it is recommended to set it up as described [here](https://nightscout.github.io/nightscout/setup_variables/))

The token field is also optional (find more info [here](https://nightscout.github.io/nightscout/security/)) 

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
![Main Screen](https://i.ibb.co/1QgxS30/Gluco-Status-FX-Main.png)

![Main Screen Poincare](https://i.ibb.co/prXFCqt/Gluco-Status-FX-Poincare.png)

![Settings](https://i.ibb.co/cvPNcs5/Gluco-Status-FX-Settings.jpg)

![Pattern](https://i.ibb.co/p1vWMmp/Gluco-Status-FX-Pattern.png)

![Time in range](https://i.ibb.co/DQmsQXg/Gluco-Status-FX-Time-In-Range.png)

![Last 30 days](https://i.ibb.co/SJ3yG6s/Gluco-Status-FX-30-Days.png)

![Overlay](https://i.ibb.co/bRDBC54/Gluco-Status-FX-Overlay-Days.png)
