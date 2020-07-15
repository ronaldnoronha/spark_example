package example.stream

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer

import org.apache.spark.SparkConf
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka010._
import java.time.LocalDateTime

/**
 * Consumes messages from one or more topics in Kafka and does wordcount.
 * Usage: DirectKafkaWordCount <brokers> <topics>
 *   <brokers> is a list of one or more Kafka brokers
 *   <groupId> is a consumer group name to consume from topics
 *   <topics> is a list of one or more kafka topics to consume from
 *
 * Example:
 *    $ bin/run-example streaming.DirectKafkaWordCount broker1-host:port,broker2-host:port \
 *    consumer-group topic1,topic2
 */
object DirectKafkaWordCount {
  def main(args: Array[String]): Unit = {
//    if (args.length < 3) {
//      System.err.println(s"""
//                            |Usage: DirectKafkaWordCount <brokers> <groupId> <topics>
//                            |  <brokers> is a list of one or more Kafka brokers
//                            |  <groupId> is a consumer group name to consume from topics
//                            |  <topics> is a list of one or more kafka topics to consume from
//                            |
//        """.stripMargin)
//      System.exit(1)
//    }

//    StreamingExamples.setStreamingLogLevels()
    val brokers = args(0)
    val groupId = args(1)
    val topics = args(2)
//    val Array(brokers, groupId, topics) = args

    // Create context with 2 second batch interval
    val sparkConf = new SparkConf().setAppName("DirectKafkaWordCount")
    val ssc = new StreamingContext(sparkConf, Seconds(2))
    // Create direct kafka stream with brokers and topics
    val topicsSet = topics.split(",").toSet
    val kafkaParams = Map[String, Object](
      ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG -> brokers,
      ConsumerConfig.GROUP_ID_CONFIG -> groupId,
      ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG -> classOf[StringDeserializer],
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG -> classOf[StringDeserializer])
    val messages = KafkaUtils.createDirectStream[String, String](
      ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, String](topicsSet, kafkaParams))

    // Get the lines, split them into words, count the words and print
    val lines = messages.map(_.value).map(_.split(","))
    val timestamp = lines.map(_(0)).map(_+" "+LocalDateTime.now().toString())
    val coords = lines.map(_(1).split(" ").map(_.toDouble))
    val target = lines.map(_(2).toInt)
//    val wordCounts = lines.map(x => (x, 1L)).reduceByKey(_ + _)
//    wordCounts.print()
    coords.map(_(0)).print()
    timestamp.print()
//    coords.print()

//    println(LocalDateTime.now().toString())
    // Start the computation
    ssc.start()
    ssc.awaitTermination()
  }
}
// scalastyle:on println