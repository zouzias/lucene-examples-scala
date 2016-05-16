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

package org.zouzias.lucene.examples.spatial

import com.spatial4j.core.context.SpatialContext
import com.spatial4j.core.distance.DistanceUtils
import com.spatial4j.core.shape.{Point, Shape}
import org.apache.lucene.document.{Document, NumericDocValuesField, StoredField}
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.search._
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree
import org.apache.lucene.spatial.query.{SpatialArgs, SpatialArgsParser, SpatialOperation}
import org.apache.lucene.store.{Directory, RAMDirectory}

/**
 * Scala rewrite of https://github.com/apache/lucene-solr/blob/branch_5_5/lucene/spatial/src/test/org/apache/lucene/spatial/SpatialExample.java
 */
class SpatialExample {

  /**
   * The Spatial4j {@link SpatialContext} is a sort of global-ish singleton
   * needed by Lucene spatial.  It's a facade to the rest of Spatial4j, acting
   * as a factory for {@link Shape}s and provides access to reading and writing
   * them from Strings.
   */
  private val ctx: SpatialContext = SpatialContext.GEO
  // Typical geospatial context
  // These can also be constructed from SpatialContextFactory


  // results in sub-meter precision for geohash
  private val maxLevels = 11

    // This can also be constructed from SpatialPrefixTreeFactory
    val grid = new GeohashPrefixTree(ctx, maxLevels)

  /**
   * The Lucene spatial {@link SpatialStrategy} encapsulates an approach to
   * indexing and searching shapes, and providing distance values for them.
   * It's a simple API to unify different approaches. You might use more than
   * one strategy for a shape as each strategy has its strengths and weaknesses.
   * <p />
   * Note that these are initialized with a field name.
   */
  private val strategy = new RecursivePrefixTreeStrategy(grid, "myGeoField")

  private val directory: Directory = new RAMDirectory()

  /**
   * Index 3 spatial documents with ids 2, 4, 20
   */
  def indexPoints(): Unit = {

    val iwConfig = new IndexWriterConfig(null)
    val indexWriter = new IndexWriter(directory, iwConfig)

    // Spatial4j is x-y order for arguments
    indexWriter.addDocument(indexSpatialDocument(2, Seq(ctx.makePoint(-80.93, 33.77))))

    // Spatial4j has a WKT parser which is also "x y" order
    indexWriter.addDocument(indexSpatialDocument(4,
      Seq(ctx.readShapeFromWkt("POINT(60.9289094 -50.7693246)"))))

    indexWriter.addDocument(indexSpatialDocument(20,
      Seq(ctx.makePoint(0.1, 0.1), ctx.makePoint(0, 0))))

    indexWriter.close()
  }

  /**
   * Index a lucene document with a given Id and sequence of shapes
   * @param id
   * @param shapes
   * @return
   */
  private def indexSpatialDocument(id: Int, shapes: Iterable[Shape]): Document = {
    val doc = new Document()
    doc.add(new StoredField("id", id))
    doc.add(new NumericDocValuesField("id", id))

    // Potentially more than one shape in this field is supported by some
    // strategies; see the Javadoc of the SpatialStrategy impl to see.
    shapes.foreach{ case shape =>
      strategy.createIndexableFields(shape).foreach{ case field =>
        doc.add(field)
      }

      // store it too; the format is up to you
      // (assume point in this example)
      val pt: Point = shape.asInstanceOf[Point]
      doc.add(new StoredField(strategy.getFieldName(), s"${pt.getX()} ${pt.getY()}"))
    }

    doc
  }


  /**
   * Search documents that lie within a circle
   */
  def searchWithinCircle(): Unit = {
    val indexReader = DirectoryReader.open(directory)
    val indexSearcher = new IndexSearcher(indexReader)
    val idSort = new Sort(new SortField("id", SortField.Type.INT))

    // Filter by circle (<= distance from a point)

    // Search with circle
    // note: SpatialArgs can be parsed from a string
    val args: SpatialArgs = new SpatialArgs(SpatialOperation.Intersects,
      ctx.makeCircle(-80.0, 33.0,
        DistanceUtils.dist2Degrees(200, DistanceUtils.EARTH_MEAN_RADIUS_KM)))

    val query = strategy.makeQuery(args)
    val docs: TopDocs = indexSearcher.search(query, 10, idSort)
    assertDocMatchedIds(indexSearcher, docs, Array(2))

    // Now, lets get the distance for the 1st doc via computing from stored point value:
    // (this computation is usually not redundant)
    val firstDoc = indexSearcher.doc(docs.scoreDocs.head.doc)
    val firstDocStr = firstDoc.getField(strategy.getFieldName()).stringValue()

    // assumes firstDocStr is "x y" as written in indexSpatialDocument()
    val spaceIdx = firstDocStr.split(' ').map(_.toDouble)
    val x = spaceIdx(0)
    val y = spaceIdx(1)
    val doc1DistDEG = ctx.calcDistance(args.getShape().getCenter(), x, y)

    assert( Math.abs(121.6D
      - DistanceUtils.degrees2Dist(doc1DistDEG, DistanceUtils.EARTH_MEAN_RADIUS_KM)) <= 0.1,
    "Distance is less than 0.1 kilometers")

    // or more simply:
    assert( Math.abs(121.6d - doc1DistDEG * DistanceUtils.DEG_TO_KM) <= 0.1,
      "Distance is less than 0.1 kilometers")

    indexReader.close()
  }

  def searchByPoint(): Unit = {
    val indexReader = DirectoryReader.open(directory)
    val indexSearcher = new IndexSearcher(indexReader)

    // Match all, order by distance ascending

    val pt = ctx.makePoint(60, -50)

    // the distance (in km)
    val valueSource = strategy.makeDistanceValueSource(pt, DistanceUtils.DEG_TO_KM)


    // false = ascending dist
    val distSort = new Sort(valueSource.getSortField(false)).rewrite(indexSearcher)

    val docs = indexSearcher.search(new MatchAllDocsQuery(), 10, distSort)

    assertDocMatchedIds(indexSearcher, docs, Array(4, 20, 2))

    // To get the distance, we could compute from stored values like earlier.
    // However in this example we sorted on it, and the distance will get
    // computed redundantly.  If the distance is only needed for the top-X
    // search results then that's not a big deal. Alternatively, try wrapping
    // the ValueSource with CachingDoubleValueSource then retrieve the value
    // from the ValueSource now. See LUCENE-4541 for an example.

    indexReader.close()
  }

  def assertDocMatchedIds(indexSearcher: IndexSearcher,
                          docs: TopDocs,
                          ids: Iterable[Int]): Unit = {
    val gotIds = docs.scoreDocs.map(x =>
      indexSearcher.doc(x.doc)
      .getField("id").numericValue().intValue()
    )

    assert(ids.zip(gotIds).forall{ case (id1, id2) => id1.equals(id2)},
      "Document ids returned in correct order")
  }
}

object SpatialExample extends App {

  private val ctx: SpatialContext = SpatialContext.GEO

  def testSpatialArgsPassing(): Unit = {

    // demo arg parsing
    val args1 = new SpatialArgs(SpatialOperation.Intersects, ctx.makeCircle(-80.0, 33.0, 1))
    val args2 = new SpatialArgsParser().parse("Intersects(BUFFER(POINT(-80 33),1))", ctx)
    assert(args1.toString() == args2.toString())
  }

  testSpatialArgsPassing()

  val example = new SpatialExample()
  example.indexPoints()
  example.searchByPoint()
}
