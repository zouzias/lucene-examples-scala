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

package org.zouzias.lucene.examples.rangefacets

import java.io.Closeable

import org.apache.lucene.analysis.core.WhitespaceAnalyzer
import org.apache.lucene.document.{Document, Field, LongField, NumericDocValuesField}
import org.apache.lucene.facet.{FacetResult, FacetsCollector}
import org.apache.lucene.facet.range.{LongRange, LongRangeFacetCounts}
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.search.{IndexSearcher, MatchAllDocsQuery}
import org.apache.lucene.store.RAMDirectory

class RangeFacetsExample extends Closeable {

  private val indexDir = new RAMDirectory()
  private val wsAnalyzer = new WhitespaceAnalyzer()
  private val indexWriterConfig = new IndexWriterConfig(wsAnalyzer)
    .setOpenMode(OpenMode.CREATE_OR_APPEND)
  private val indexWriter = new IndexWriter(indexDir, indexWriterConfig)

  private val nowSec = System.currentTimeMillis()

  // Add documents with a fake timestamp, 1000 sec before
  // "now", 2000 sec before "now", ...:
  (1 to 1000).foreach{ case i =>
    val  doc = new Document()
    val meta = nowSec - i * 100

    // Add as doc values field, so we can compute range facets:
    doc.add(new NumericDocValuesField("timestamp", meta))

    // Add as numeric field so we can drill-down:
    doc.add(new LongField("timestamp", meta, Field.Store.NO))
    indexWriter.addDocument(doc)
  }

  // Open near-real-time searcher
  val searcher = new IndexSearcher(DirectoryReader.open(indexWriter))
  indexWriter.close()

  /**
   * User runs a query and counts facets
   * @return
   */
  def search(): FacetResult = {
    val PAST_HOUR = new LongRange("Past hour", nowSec - 3600, true, nowSec, true)
    val PAST_SIX_HOURS = new LongRange("Past six hours", nowSec - 6 * 3600, true, nowSec, true)
    val PAST_DAY = new LongRange("Past day", nowSec - 24 * 3600, true, nowSec, true)

    // Aggregates the facet counts
    val fc = new FacetsCollector()

    // MatchAllDocsQuery is for "browsing" (counts facets
    // for all non-deleted docs in the index); normally
    // you'd use a "normal" query:
    FacetsCollector.search(searcher, new MatchAllDocsQuery(), 10, fc)

    val facets = new LongRangeFacetCounts("timestamp", fc, PAST_HOUR, PAST_SIX_HOURS, PAST_DAY)
    facets.getTopChildren(10, "timestamp")
}

  @Override
  def close(): Unit = {
    searcher.getIndexReader().close()
    indexDir.close()
  }
}

object RangeFacetsExample extends App {

  val example = new RangeFacetsExample()

  // scalastyle:off println
  println("Facet counting example:")
  println("-----------------------")
  println(example.search())
  // scalastyle:on println

  example.close()
}
