#!/bin/bash

javac DynamicTest.java
jar cvfm DynamicTest.jar Manifest *.class
