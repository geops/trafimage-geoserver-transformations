Geoserver rendering transformation for feature aggregation. See #14080071

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
