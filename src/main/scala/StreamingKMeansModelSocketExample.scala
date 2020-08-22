package example.stream

import java.time.LocalDateTime

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer

import scala.io.Source
import org.apache.spark.SparkConf
import org.apache.spark.mllib.clustering.StreamingKMeansModel
import org.apache.spark.mllib.linalg
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.{Seconds, StreamingContext}

object StreamingKMeansModelSocketExample {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("StreamingKMeansModelExample")
    val ssc = new StreamingContext(conf, Seconds(1))

    val filename = "/home/ronald/random_centers.csv"
    val lines = Source.fromFile(filename).getLines.toArray.map(_.split(","))

    val centers:Array[linalg.Vector] = new Array[linalg.Vector](lines.length-1)
    for (i <- 1 to lines.length-1) {
      centers(i-1) = Vectors.dense(lines(i).map(_.toDouble))
    }
    val weights:Array[Double] = new Array[Double](centers.length)
    for (i<-0 to weights.length-1) {
      weights(i) = 1.0
    }

    val model = new StreamingKMeansModel(centers,weights)

    val messages = ssc.socketTextStream(args(0), args(1).toInt, StorageLevel.MEMORY_AND_DISK_SER)

    val inputLines = messages.flatMap(_.split(",")))

    inputLines.foreachRDD( rdd => {
      println(rdd)
    })

//    val inputLines = messages.map(_.split(","))

//    inputLines.foreachRDD ( rdd => {
//      for (i <- rdd) {
//        println(i.toString())
//      }
//    })

    val timestamp = inputLines.map(_(0)).map(_+" "+LocalDateTime.now().toString())
    val coords = inputLines.map(_(1).split(" ").map(_.toDouble)).map(x => Vectors.dense(x))

    coords.foreachRDD(rdd=>{
      for (i <- rdd){
        println(i.toString())
      }
    })
//    val target = inputLines.map(_(2).toInt)
//    val timestampCreated, point, target = inputLines.flatMap(_.split(","))
//    coords.foreachRDD(rdd => {
//      model.update(rdd, 1.0, "batches")
//      println("Centers:")
//      println("Count: "+coords.count())
//      for (i <- model.clusterCenters) {
//        println(i.toString())
//      }
//    })

    ssc.start()
    ssc.awaitTerminationOrTimeout(10)
  }

}

