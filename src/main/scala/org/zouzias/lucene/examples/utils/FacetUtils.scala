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
package org.zouzias.lucene.examples.utils

import org.apache.lucene.facet.{FacetResult, LabelAndValue}

/**
 * Lucene facet utils
 */
object FacetUtils {

  def facetValuestoString(facetValues: Array[LabelAndValue]): String = {
    facetValues.map(x => s"  ${x.label} (${x.value})").mkString("\n")
  }

  def printFacetResult(facetResult: FacetResult): Unit = {
    // scalastyle:off println
    println("********************************")
    println("Facet:")
    println("********************************")
    println(s"* Facet name: ${facetResult.dim}")
    println(s"* Sum of facets counts : ${facetResult.value}")
    println(s"* # of facets : ${facetResult.childCount}")
    // println(s"* Path: ${facetResult.path.mkString("/")}")
    println(s"* Value (count): \n${facetValuestoString(facetResult.labelValues)}")
    println("********************************")
    // scalastyle:on println
  }


}
