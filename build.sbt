/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

name := "lucene-examples"
version := "0.0.1-SNAPSHOT"
organization := "org.zouzias"
scalaVersion := "2.11.7"
licenses += "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")

val luceneV = "5.5.0"

val specs2_core               = "org.specs2"                     %% "specs2-core"              % "2.3.11" % "test"
val scala_check               = "org.scalacheck"                 %% "scalacheck"               % "1.12.2" % "test"
val scalatest                 = "org.scalatest"                  %% "scalatest"                % "2.2.6"  % "test"

val lucene_facet              = "org.apache.lucene"              % "lucene-facet"              % luceneV
val lucene_analyzers          = "org.apache.lucene"              % "lucene-analyzers-common"   % luceneV
val lucene_expressions        = "org.apache.lucene"              % "lucene-expressions"        % luceneV
val lucene_spatial            = "org.apache.lucene"              % "lucene-spatial"            % luceneV

libraryDependencies ++= Seq(
  lucene_facet,
  lucene_analyzers,
  lucene_expressions,
  lucene_spatial,
  specs2_core,
  scalatest
)

lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")
compileScalastyle := org.scalastyle.sbt.ScalastylePlugin.scalastyle.in(Compile).toTask("").value
(compile in Compile) <<= (compile in Compile) dependsOn compileScalastyle


parallelExecution in Test := false
