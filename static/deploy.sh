#!/bin/bash

javac StaticTest.java
jar cvfm StaticTest.jar Manifest *.class
