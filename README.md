# GlucoStatusFX

This Gluon sample was generated from https://start.gluon.io

## Basic Requirements

A list of the basic requirements can be found online in the [Gluon documentation](https://docs.gluonhq.com/#_requirements).

## Quick instructions

### Run the sample on JVM/HotSpot:

    mvn gluonfx:run

### Run the sample as a native image:

    mvn gluonfx:build gluonfx:nativerun

### Run the sample as a native android image:

    mvn -Pandroid gluonfx:build gluonfx:package gluonfx:install gluonfx:nativerun

### Run the sample as a native iOS image:

    mvn -Pios gluonfx:build gluonfx:package gluonfx:install gluonfx:nativerun


### Install native iOS app on your iPhone (in developer mode)
Build it:
    mvn -Pios gluonfx:build

Package it:
    mvn -Pios gluonfx:package

Deploy it:
    mvn -Pios gluonfx:install


## Selected features

This is a list of all the features that were selected when creating the sample:

### JavaFX 17.0.6 Modules

 - javafx-base
 - javafx-graphics
 - javafx-controls
 - javafx-media
 - javafx-swing

### Gluon Features

 - Glisten: build platform independent user interfaces
 - Attach audio
 - Attach connectivity
 - Attach display
 - Attach lifecycle
 - Attach local notifications
 - Attach settings
 - Attach statusbar
 - Attach storage
