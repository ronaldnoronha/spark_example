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

// scalastyle:off println
package com.example.kmeans

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.{DoubleType, StructField, StructType, TimestampType}
import org.apache.spark.ml.clustering.{KMeans, KMeansModel}
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.mllib.linalg.Vectors
// $example off$

object KMeansExample {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder
      .master("local[*]")
      .appName("lambda")
      .getOrCreate()
    // $example on$
    // Load and parse the data
    val schema = StructType(Array(
      StructField("x", DoubleType, nullable = false),
      StructField("y", DoubleType, nullable = false),
      StructField("z", DoubleType, nullable = false)
    ))

    val data = spark.read.format("csv")
      .option("header", value = true)
      .option("delimiter", ",")
      .schema(schema)
      .load("/home/ronald/KMeans.csv")
      .cache()

    data.printSchema()

    data.show(10)

    val cols = Array("x", "y", "z")
    val assembler = new VectorAssembler().setInputCols(cols).setOutputCol("Features")
    val featureDf = assembler.transform(data)

    featureDf.printSchema()

    featureDf.show(10)

    val splitDataSet: Array[org.apache.spark.sql.Dataset[org.apache.spark.sql.Row]] =
      featureDf.randomSplit(Array(0.7,0.3),98765L)

    val trainingData = splitDataSet(0)
    val testData = splitDataSet(1)

    val kmeans = new KMeans()
      .setK(8)
      .setFeaturesCol("Features")
      .setPredictionCol("Prediction")

    val kmeansModel = kmeans.fit(trainingData)
    kmeansModel.clusterCenters.foreach(println)

    val predictDf = kmeansModel.transform(testData)
    predictDf.show(10)

    predictDf.groupBy("Prediction").count().show()

    val predictTrainDf = kmeansModel.transform(trainingData)
    predictTrainDf.show(10)

    predictTrainDf.groupBy("Prediction").count().show()
  }
}
// scalastyle:on println
