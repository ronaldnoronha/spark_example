//package example.stream
//
//import org.apache.spark
//import org.apache.spark.sql.functions._
//import org.apache.spark.sql.SparkSession
//import org.apache.spark.streaming.{Seconds, StreamingContext}
//
//class KafkaStreamingWorkingExample {
//
//  val df = spark
//    .readStream
//    .format("kafka")
//    .option("kafka.bootstrap.servers", "localhost:9092")
//    .option("subscribe","test")
//    .load()
//    .selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)")
//    .as[(String, String)]
//
//  val query = df.writeStream.format("console")
//
//  query.start()
//
//  query.awaitTermination()
//
//}
