Geoserver rendering transformation for feature aggregation. 
See internal issue #14080071

# General

In the context of the Trafimage geoportal (http://maps.trafimage.ch/)of the swiss railway company SBB geOps was given the task to visualize events and activities along the train tracks. These include planned, ongoing and finished construction work, maintenance tasks and information on equipment along the tracks.

The data to be visualized has a very high information density which makes it challenging to create a visualization without sacrificing much information. Luckily GeoServer supports using WPS processes to be used during the rendering of a map in a WMS request under the name of Rendering Transformations (http://docs.geoserver.org/stable/en/user/styling/sld-extensions/rendering-transform.html). This feature allows a more complex styling than what is possible using SLD alone by allowing the implementer to modify the features in the rendering pipeline or even draw them on the image of the map directly. We decided to use the approach of the vector-to-vector transformations to be able to modify the incoming geodata and still benefit of GeoServer's rich styling abilites for vector data. 

![Example rendering](examples/big_example.png?raw=true")

The visualization is based on the whole track network being split into segments based according to the data which will be visualized. There are similar line segments for each event/activity-type at a track. These line segments are then grouped by their geometry and then drawn at an offset parallel to the train tracks. The offset, color as well as the width of these resulting ribbons show the type as well as importance of the visualized event.

There are custom transformations for just stacking line segments along a track as well as for aggregating segments based on their attributes to acheive a weighting by drawing the width of the line in proportion to how many features are part of the aggregation. All of these WPS processes have a high configurability to allow reuse in other contexts.

# Installation

## Building the extension

Run

    make build
    
The generated JARs will be in the `target/` directory.


## Installing into Geoserver

This extension depends on the WPS extension which needs to be installed first. See 
http://docs.geoserver.org/stable/en/user/extensions/wps/install.html on this topic.

To install this extension, drop all JARs of the `target` directory to geoservers WEB-INF/lib/ directory and 
restart geoserver. The `target` directory will also contain the dependencies which are not already bundled with
geoserver.

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

## Scripting with Javascript

Not all processes support javascript. For detailed info on a process see the "Processes" section.

### Environment

In the javascript environment the following objects are defined:

The `console` object has the following methods:

`log`: This method takes a String as argument and logs its value to the geoserver log on the "INFO" loglevel. Please do not leave too verbose logging calls in production code as these will slow down rendering significantly.

`error`: This method takes a String as argument and logs its value to the geoserver log on the "ERROR" loglevel. Please do not leave too verbose logging calls in production code as these will slow down rendering significantly.


## Processes

### AggregateSimilarFeatures

#### Javascript

not supported

#### Examples

... see examples directory.


### AggregateSimilarLinesAsPolygon

#### Javascript

not supported

#### Examples

... see examples directory.


### AggregateAsLineStacks

#### Javascript

It is possible to pass a script to the rendering transformation using the `renderScript` parameter of the transformation.

The script must define a `getFeatureWith` function. This function is called to get the width of a stack in pixels. The function recieves two parameters: the length of the line in map units (float value) and the number of features in the stack (integer value). The function must return a number.

It is possible to pass two values from the SLD to the javascript using the `scriptCustomVariable1` and `scriptCustomVariable2` parameters of the rendering transformation. This will create the corresponding global javascript variables `customVariable1` and `customVariable2`. These variable will be created AFTER the initial evaluation of the script, so the should only be accessed from inside the functions called by the rendering transformation.

Using these custom variables allows for example to pass the scale denominator to the script:

            <ogc:Function name="parameter">
              <ogc:Literal>scriptCustomVariable1</ogc:Literal>
              <ogc:Function name="env">
                <ogc:Literal>wms_scale_denominator</ogc:Literal>
              </ogc:Function>
            </ogc:Function>
            
            <ogc:Function name="parameter">
              <ogc:Literal>scriptCustomVariable2</ogc:Literal>
              <ogc:Literal>jsdhfsfhj</ogc:Literal>
            </ogc:Function>
            
            <ogc:Function name="parameter">
              <ogc:Literal>renderScript</ogc:Literal>
              <ogc:Literal>
               
              var maxLineWidth = 20;
              var minLineWidth = 8;
                
              function getFeatureWith(featureLength, aggCount) {
                console.log("featureLength="+featureLength
                  +" aggCount="+aggCount
                  +" wms_scale_denominator (customVariable1)="+customVariable1
                  +" useless stuff (customVariable2)="+customVariable2
                );
                
                var width = Math.min(Math.max(minLineWidth, aggCount), maxLineWidth);
                return width;
               }
              </ogc:Literal>
            </ogc:Function>


#### Examples

... see examples directory.


### MakeOffsettedLines

#### Javascript

not supported

#### Examples

... see examples directory.


### LineStacks

#### Javascript

It is possible to pass a script to the rendering transformation using the `renderScript` parameter of the transformation.

The script must define a `getFeatureWith` function. This function is called to get the width of a stack in pixels. The function recieves two parameters: the length of the line in map units (float value). The function must return a number.

It is possible to pass two values from the SLD to the javascript using the `scriptCustomVariable1` and `scriptCustomVariable2` parameters of the rendering transformation. This will create the corresponding global javascript variables `customVariable1` and `customVariable2`. These variable will be created AFTER the initial evaluation of the script, so the should only be accessed from inside the functions called by the rendering transformation.

Using these custom variables allows for example to pass the scale denominator to the script:

            <ogc:Function name="parameter">
              <ogc:Literal>scriptCustomVariable1</ogc:Literal>
              <ogc:Function name="env">
                <ogc:Literal>wms_scale_denominator</ogc:Literal>
              </ogc:Function>
            </ogc:Function>
            
            <ogc:Function name="parameter">
              <ogc:Literal>scriptCustomVariable2</ogc:Literal>
              <ogc:Literal>jsdhfsfhj</ogc:Literal>
            </ogc:Function>
            
            <ogc:Function name="parameter">
              <ogc:Literal>renderScript</ogc:Literal>
              <ogc:Literal>
               
              var maxLineWidth = 20;
              var minLineWidth = 8;
                
              function getFeatureWith(featureLength) {
                console.log("featureLength="+featureLength
                  +" wms_scale_denominator (customVariable1)="+customVariable1
                  +" useless stuff (customVariable2)="+customVariable2
                );
                
                var width = Math.floor(Math.random()*6);
                return width;
               }
              </ogc:Literal>
            </ogc:Function>


#### Examples

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

The Jars build using `make` contain the git commit hash of the source. This can be viewed by opening the file `trafimage-geoserver-transformations.gitversion` bundled in the JAR.


# License

Copyright (c) 2014 geOps - www.geops.de. All rights reserved.

This code is licensed under the GPL 2.0 license, available at the root
application directory.
