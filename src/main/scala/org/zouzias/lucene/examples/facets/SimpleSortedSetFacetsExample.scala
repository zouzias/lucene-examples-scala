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
package org.zouzias.lucene.examples.facets

import org.apache.lucene.analysis.core.WhitespaceAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.facet._
import org.apache.lucene.facet.sortedset.{DefaultSortedSetDocValuesReaderState, SortedSetDocValuesFacetCounts, SortedSetDocValuesFacetField}
import org.apache.lucene.store.Directory
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.search.{IndexSearcher, MatchAllDocsQuery}
import org.apache.lucene.store.RAMDirectory
import org.zouzias.lucene.examples.utils.FacetUtils

import scala.collection.mutable.ArrayBuffer

class SimpleSortedSetFacetsExample(indexDir: Directory,
                                   facetsConfig: FacetsConfig = new FacetsConfig()) {


  /**
   * Index documents
   * @param docs
   */
  def index(docs: Seq[Document]): Unit = {
    val wsAnalyzer = new WhitespaceAnalyzer()
    val indexWriterConfig = new IndexWriterConfig(wsAnalyzer).setOpenMode(OpenMode.CREATE_OR_APPEND)
    val indexWriter = new IndexWriter(indexDir, indexWriterConfig)

    docs.foreach( doc => indexWriter.addDocument(facetsConfig.build(doc)))

    indexWriter.close()
  }

  def index(): Unit = {

    val docs = new ArrayBuffer[Document]()

    val doc = new Document()
    doc.add(new SortedSetDocValuesFacetField("Author", "Bob"))
    doc.add(new SortedSetDocValuesFacetField("Publish Date", "2010"))
    docs += doc

    val doc2 = new Document()
    doc2.add(new SortedSetDocValuesFacetField("Author", "Lisa"))
    doc2.add(new SortedSetDocValuesFacetField("Publish Date", "2010"))
    docs += doc2

    val doc3 = new Document()
    doc3.add(new SortedSetDocValuesFacetField("Author", "Lisa"))
    doc3.add(new SortedSetDocValuesFacetField("Publish Date", "2012"))
    docs += doc3

    val doc4 = new Document()
    doc4.add(new SortedSetDocValuesFacetField("Author", "Susan"))
    doc4.add(new SortedSetDocValuesFacetField("Publish Date", "2012"))
    docs += doc4

    val doc5 = new Document()
    doc5.add(new SortedSetDocValuesFacetField("Author", "Frank"))
    doc5.add(new SortedSetDocValuesFacetField("Publish Date", "1999"))
    docs += doc5

    val doc6 = new Document()
    doc6.add(new SortedSetDocValuesFacetField("Author", "Anastasios"))
    doc6.add(new SortedSetDocValuesFacetField("Publish Date", "1984"))
    docs += doc6

    index(docs)
  }


  def facetsOnly(indexDir: Directory): List[FacetResult] = {
    val indexReader = DirectoryReader.open(indexDir)
    val searcher = new IndexSearcher(indexReader)

    val state = new DefaultSortedSetDocValuesReaderState(indexReader)

    // Aggregates the facet counts
    val fc = new FacetsCollector()

    FacetsCollector.search(searcher, new MatchAllDocsQuery(), 10, fc)

    // Retrieve results
    val results = new ArrayBuffer[FacetResult]

  // Count both "Publish Date" and "Author" dimensions
    // Retrieve results
    val facets: Facets = new SortedSetDocValuesFacetCounts(state, fc)

    results += facets.getTopChildren(10, "Author")
    results += facets.getTopChildren(10, "Publish Date")

    indexReader.close()

    results.toList
  }
}

object SimpleSortedSetFacetsExample {

  private val indexDir = new RAMDirectory()

  def main(args: Array[String]): Unit = {
    val example = new SimpleSortedSetFacetsExample(indexDir)

    // Index sample docs
    example.index()

    // Printout the facets
    val facets = example.facetsOnly(indexDir)
    facets.foreach(FacetUtils.printFacetResult(_))
  }
}
