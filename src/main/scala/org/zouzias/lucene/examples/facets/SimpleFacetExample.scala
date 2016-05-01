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
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts
import org.apache.lucene.facet._
import org.apache.lucene.facet.taxonomy.directory.{DirectoryTaxonomyReader, DirectoryTaxonomyWriter}
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.search.{IndexSearcher, MatchAllDocsQuery}
import org.apache.lucene.store.{Directory, RAMDirectory}
import org.zouzias.lucene.examples.utils.FacetUtils

import scala.collection.mutable.ArrayBuffer

class SimpleFacetExample(indexDir: Directory,
                         taxonomyDir: Directory,
                         facetsConfig: FacetsConfig = new FacetsConfig()) {


  def index(docs: Seq[Document]): Unit = {
    val indexWriter = new IndexWriter(indexDir, new IndexWriterConfig(
      new WhitespaceAnalyzer()).setOpenMode(OpenMode.CREATE_OR_APPEND))

    // Writes facet ords to a separate directory from the main index
    val taxoWriter = new DirectoryTaxonomyWriter(taxonomyDir)

    docs.foreach( doc =>
    indexWriter.addDocument(facetsConfig.build(taxoWriter, doc))
    )

    indexWriter.close()
    taxoWriter.close()
  }

  def index(): Unit = {

    val docs = new ArrayBuffer[Document]()

    val doc = new Document()
    doc.add(new FacetField("Author", "Bob"))
    doc.add(new FacetField("Publish Date", "2010"))
    docs += doc

    val doc2 = new Document()
    doc2.add(new FacetField("Author", "Lisa"))
    doc2.add(new FacetField("Publish Date", "2010"))
    docs += doc2

    val doc3 = new Document()
    doc3.add(new FacetField("Author", "Lisa"))
    doc3.add(new FacetField("Publish Date", "2012"))
    docs += doc3

    val doc4 = new Document()
    doc4.add(new FacetField("Author", "Susan"))
    doc4.add(new FacetField("Publish Date", "2012"))
    docs += doc4

    val doc5 = new Document()
    doc5.add(new FacetField("Author", "Frank"))
    doc5.add(new FacetField("Publish Date", "1999"))
    docs += doc5

    val doc6 = new Document()
    doc6.add(new FacetField("Author", "Anastasios"))
    doc6.add(new FacetField("Publish Date", "1984"))
    docs += doc6

    index(docs)
  }


  def facetsOnly(indexDir: RAMDirectory, taxoDir: RAMDirectory): List[FacetResult] = {
    val indexReader = DirectoryReader.open(indexDir)
    val searcher = new IndexSearcher(indexReader)
    val taxoReader = new DirectoryTaxonomyReader(taxoDir)

    val fc = new FacetsCollector()

    // MatchAllDocsQuery is for "browsing" (counts facets
    // for all non-deleted docs in the index) normally
    // you'd use a "normal" query:
    searcher.search(new MatchAllDocsQuery(), fc)

    // Retrieve results
    val results = new ArrayBuffer[FacetResult]

    // Count both "Publish Date" and "Author" dimensions
    val facets = new FastTaxonomyFacetCounts(taxoReader, facetsConfig, fc)

    results += facets.getTopChildren(10, "Author")
    results += facets.getTopChildren(10, "Publish Date")

    indexReader.close()
    taxoReader.close()

    results.toList
  }
}

object SimpleFacetExample {

  private val indexDir = new RAMDirectory()
  private val taxoDir = new RAMDirectory()

  def main(args: Array[String]): Unit = {
    val example = new SimpleFacetExample(indexDir, taxoDir)

    // Index sample docs
    example.index()

    example.index()


    // Printout the facets
    val facets = example.facetsOnly(indexDir, taxoDir)
    facets.foreach(FacetUtils.printFacetResult(_))
  }
}
