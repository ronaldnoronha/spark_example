package uber

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.ml.clustering.{KMeans, KMeansModel}
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.sql.types.{DoubleType, StringType, StructField, StructType, TimestampType}
import org.apache.spark.sql.functions.to_timestamp

object KMeansUber {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder
      .master("local[*]")
      .appName("lambda")
      .getOrCreate()

    // SparkSession has implicits
    import spark.implicits._

    // schema
    val schema = StructType(Array(
      StructField("Time", TimestampType, nullable = true),
      StructField("Lat", DoubleType, nullable = true),
      StructField("Lon", DoubleType, nullable = true),
      StructField("Base", StringType, nullable = true)
    ))

//    val schema = StructType(Array(
//      StructField("x", DoubleType, nullable = false),
//      StructField("y", DoubleType, nullable = false),
//      StructField("z", DoubleType, nullable = false),
//      StructField("target", StringType, nullable = false)
//    ))

//    val uberDf = spark.read.format("csv")
//      .option("header","true")
//      .option("dateFormat", "MM/dd/yyyy HH:mm:ss")
//      .schema(schema)
//      .load("/home/ronald/uber.csv")
//      .cache()

    val uberDf = spark.read.format("csv")
      .option("header", value = true)
      .option("delimiter", ",")
      .option("mode", "DROPMALFORMED")
      .option("timestampFormat", "yyyy-MM-dd HH:mm:ss")
      .schema(schema)
      .load("/home/ronald/train.csv")
      .cache()


    val df = spark.read.format("csv").option("header",value=true).option("delimiter",",").option("mode","DROPMALFORMED").schema(schema).load("/home/ronald/train.csv").cache()

    uberDf.printSchema()
    /*
     * output
    root
     |-- time: timestamp (nullable = true)
     |-- lat: double (nullable = true)
     |-- lon: double (nullable = true)
     |-- base: string (nullable = true)
    */

    uberDf.show(10)


    // transform userDf with VectorAssembler to add feature column
    val cols = Array("Lat", "Lon")
    val assembler = new VectorAssembler().setInputCols(cols).setOutputCol("Features")
    val featureDf = assembler.transform(uberDf)

    featureDf.printSchema()
    /*
     * output
    root
     |-- time: timestamp (nullable = true)
     |-- lat: double (nullable = true)
     |-- lon: double (nullable = true)
     |-- base: string (nullable = true)
     |-- features: vector (nullable = true)
    */

    featureDf.show(10)
    /*
     * output
    +-------------------+-------+--------+------+------------------+
    |               time|    lat|     lon|  base|          features|
    +-------------------+-------+--------+------+------------------+
    |0010-07-07 00:02:00|40.7521|-73.9914|B02512|[40.7521,-73.9914]|
    |0010-07-07 00:06:00|40.6965|-73.9715|B02512|[40.6965,-73.9715]|
    |0010-07-07 00:15:00|40.7464|-73.9838|B02512|[40.7464,-73.9838]|
    |0010-07-07 00:17:00|40.7463|-74.0011|B02512|[40.7463,-74.0011]|
    |0010-07-07 00:17:00|40.7594|-73.9734|B02512|[40.7594,-73.9734]|
    |0010-07-07 00:20:00|40.7685|-73.8625|B02512|[40.7685,-73.8625]|
    |0010-07-07 00:21:00|40.7637|-73.9962|B02512|[40.7637,-73.9962]|
    |0010-07-07 00:21:00|40.7252|-74.0023|B02512|[40.7252,-74.0023]|
    |0010-07-07 00:25:00|40.7607|-73.9625|B02512|[40.7607,-73.9625]|
    |0010-07-07 00:25:00|40.7212|-73.9879|B02512|[40.7212,-73.9879]|
    +-------------------+-------+--------+------+------------------+
    */

    // kmeans model with 8 clusters
    val kmeans = new KMeans()
      .setK(8)
      .setFeaturesCol("Features")
      .setPredictionCol("Prediction")

    val splitDataSet: Array[org.apache.spark.sql.Dataset[org.apache.spark.sql.Row]] =
      featureDf.randomSplit(Array(0.7,0.3),98765L)

    val trainingData = splitDataSet(0)
    val testData = splitDataSet(1)
    val kmeansModel = kmeans.fit(trainingData)
    kmeansModel.clusterCenters.foreach(println)
    /*
     * output
    [40.73140629015964,-73.9982639026069]
    [40.65596370575975,-73.78189605983525]
    [40.704405990360335,-74.19064831305947]
    [40.787147812448445,-73.88033836332613]
    [40.9109989247312,-73.59091852770885]
    [40.76590827702134,-73.9728956110863]
    [40.47641392405063,-74.51029113924052]
    [40.687648075223024,-73.96489148316304]
    */

    // test the model with test data set
    val predictDf = kmeansModel.transform(testData)
    predictDf.show(10)
    /*
     * output
    +-------------------+-------+--------+------+------------------+----------+
    |               time|    lat|     lon|  base|          features|prediction|
    +-------------------+-------+--------+------+------------------+----------+
    |0010-07-07 00:00:00|40.6449|-73.7822|B02598|[40.6449,-73.7822]|         1|
    |0010-07-07 00:01:00| 40.694|-73.9872|B02598| [40.694,-73.9872]|         7|
    |0010-07-07 00:01:00|40.7422|-74.0042|B02598|[40.7422,-74.0042]|         0|
    |0010-07-07 00:02:00|40.7206| -73.989|B02598| [40.7206,-73.989]|         0|
    |0010-07-07 00:07:00|40.7476|-74.0081|B02598|[40.7476,-74.0081]|         0|
    |0010-07-07 00:13:00|40.7024|-73.9873|B02598|[40.7024,-73.9873]|         7|
    |0010-07-07 00:17:00|40.7463|-74.0011|B02512|[40.7463,-74.0011]|         0|
    |0010-07-07 00:17:00|40.8135|-73.9612|B02598|[40.8135,-73.9612]|         5|
    |0010-07-07 00:17:00|40.8143|-73.9473|B02598|[40.8143,-73.9473]|         5|
    |0010-07-07 00:18:00|40.6946|-73.9613|B02598|[40.6946,-73.9613]|         7|
    +-------------------+-------+--------+------+------------------+----------+
    */

    // no of categories
    predictDf.groupBy("Prediction").count().show()
    /*
     * output
    +----------+-----+
    |prediction|count|
    +----------+-----+
    |         1| 5591|
    |         6|   72|
    |         3| 7903|
    |         5|75023|
    |         4|  480|
    |         7|21225|
    |         2| 1887|
    |         0|83697|
    +----------+-----+
    */

    // save model
    kmeansModel.write.overwrite()
      .save("/home/ronald/models/uber-model")

    // load model
    val kmeansModelLoded = KMeansModel
      .load("/home/ronald/models/uber-model")
  }

}
