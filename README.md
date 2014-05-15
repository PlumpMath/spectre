Overview
========

Spectre is a java library that provides simple read/write capabilities to aspects. 
Aspects are single dimension data extracted from replays and organized in to groups
described by a "path". An example of such a path is `/basic/position/hero` which
will contain the X/Y position data for all of the heroes each tick.

Purpose
=======

The primary goal of spectre is to make it as simple as possible to read/write and
most importantly extend the set of aspects available for analysis.

Usage
=====

Spectre will be available as a single .jar file that can be included in analysis
code in order to have access to the aspects library. Basic usage of the library
can be found by examining the example class at:

     /aspectSerializer/src/main/java/skadistats/WritePosition.java 

Extending
========

Adding a new aspect is done through the following steps:

### 1. Create protobuf definition
Create a new protobuf definition file under

    /aspectSerializer/src/main/proto/aspect/<category>

where `category` is either `basic` or `derived` depending on the type of aspect you are
creating. Make sure you include

    option java_package = "skadistats.spectre.proto.<category"

as part of your protobuf file so the generated classes are placed in the correct location.
Each protobuf message name must be globally unique due to the way they are used within the
API, however you can use the same protobuf messages for multiple different aspect path definitions.

### 2. Add your new aspect to msgtypes.properties
Edit `/aspectSerializer/src/main/proto/msgtypes.properties` and add your aspect definition.
The definition format is:
    <protobuf class> : <msg id> : <aspect path>
    
###### protobuf class
The protobuf class is the Java class name including the container class which is named
after your .proto file. So if you create a `HeroPosition` message definition inside of
`position.proto` then the protobuf class will be `Position.HeroPosition`.

###### msg id
Number used to identify message types as part of serialization. Each message id must
be unique. Please use monotonically increasing values in this file.

###### aspect path
This is the aspect path which is used to identify the given aspect when reading/writing it.

### 3. Recreate spectre .jar
Running `ant clean && ant` in the spectre root should be enough. The resulting jar is located
at `/aspectSerializer/target/spectre-1.0-jar-with-dependencies.jar`.

To-Do
=====

- Tests (duh)
- Fix resulting jar name to just be spectre-1.0.jar
- Reduce size of jar by removing some of the stuff that's packed in it