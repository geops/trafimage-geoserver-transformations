Geoserver rendering transformation for feature aggregation. See #14080071

# Installation

## Building the extension

Run

    make build
    
The generated JARs will be in the target/ directory.


## Installing into Geoserver

This extension depends on the WPS extension which needs to be installed first. See 
http://docs.geoserver.org/stable/en/user/extensions/wps/install.html on this topic.



To install this extension, drop all JARs to geoservers WEB-INF/lib/ directory and 
restart geoserver.

To see if the installation was successful you may check if the rendering transformations 
as WPS services. Just go to "Demos" -> "WPS request builder". The rendering transformations
should be available in the "Choose Process" select box. The names are 

* "gs:AggregateSimilarFeatures"
* "gs:AggregateSimilarLinesAsPolygon"
* "gs:AggregateAsLineStacks"
* "gs:MakeOffsettedLines"

There is also a special "About Trafimage Rendering Transformations" page in "About & Status" 
menu which lists the version of the extension installed.

# Styling examples

For the SLD code for all examples see the examples directory. Documentation on the 
parameters of the rendering transformations is available in the "WPS request builder"
and in the source files.

Make sure you have an index on the geometries as this will speed up the aggregation significantly.

## AggregateSimilarFeatures

... see examples directory.

## AggregateSimilarLinesAsPolygon

... see examples directory.

## AggregateAsLineStacks

... see examples directory.

## MakeOffsettedLines

... see examples directory.

# Development

## Developing and Debugging using eclipse

### Loading geoserver including the WPS extension into eclipse

* Enter geoservers `src` directory and run `mvn eclipse:eclipse`
* Enter `src/extension/wps` and also run `mvn eclipse:eclipse`
* Load geoserver into eclipse using the menu "File" -> "Import". There choose "General" -> "Existing projects into workspace". Enable recursive/nested searching for projects to make sure the WPS extension is found by eclipse.

After eclipse has build its workspace Right-click the "web-app" project and open "Properties" there you need to

* make sure all required projects including the WPS projects have been added to the "Java Build Path". Click "Add" to see a list of missing projects of the current workspace.
* make sure the WPS projects are also part of the "Project References".

Geoserver can now be launched by right-clicking org.geoserver.web.Start in the "web-app"-project (`src/test/java` directory) and selecting "Run As" -> "Java application".

In case geoserver raises lots of errors it is worth to give it the data directory of a working geoserver install. The official docs recommend setting a `GEOSERVER_DATA_DIRECTORY` in the "Run/Debug settings", but this does not really seem to work. A more reliable way would be symlinking the data directory to replace `src/web/app/src/main/webapp/data`

### Loading the rendering transformations into geoserver

* Enter the directory of the source of the RT and run `mvn eclipse:eclipse`.
* Load the extension into eclipse using the menu "File" -> "Import". There choose "General" -> "Existing projects into workspace".

After eclipse has rebuild its workspace Right-click the "web-app" project again and open "Properties" there you need to

* make sure the rendering transformations project has been added to the "Java Build Path".
* make sure the project is also part of the "Project References".

You may now start geoserver again. The menu of the rendering transformations should now show up in the "About & Status" section of the GUI.

Changes to the projects `pom.xml` file require a new run of `mvn eclipse:eclipse` and refreshing the project inside eclipse.

## Version information

Tha Jars build usiing `make` contain the git commit hash of the source. This can be viewed by opening the file `trafimage-geoserver-transformations.gitversion` bundled in the JAR.
