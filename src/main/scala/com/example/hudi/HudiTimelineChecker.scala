package com.example.hudi

import com.typesafe.scalalogging.LazyLogging
import org.apache.hudi.common.table.timeline.HoodieActiveTimeline

import java.time.LocalDateTime
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{Executors, TimeUnit}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

object HudiTimelineChecker extends App with LazyLogging {

  private[this] val numOfArrayIndexOutOfBoundsExceptions: AtomicLong = new AtomicLong(0)
  private[this] val numOfFutureCommits: AtomicLong = new AtomicLong(0)

  private[this] val numOfInstantAttempts: AtomicLong = new AtomicLong(0)

  private[hudi] lazy val activeThreads: Int = System.getProperty("active.threads").toInt
  private[hudi] lazy val activeDurationMillis: Long = System.getProperty("active.duration.millis").toLong

  private[hudi] val executorService = Executors.newCachedThreadPool()
  private[hudi] implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(executorService)

  import java.time.format.DateTimeFormatter

  private[hudi] val instantFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

  private[hudi] val instantFormatter0_10_0 = new DateTimeFormatterBuilder().appendPattern("yyyyMMddHHmmss")
    .appendValue(ChronoField.MILLI_OF_SECOND, 3).toFormatter

  val futures = schedule()

  val futureToWaitFor = Future.sequence(futures)

  try {
    Await.result(futureToWaitFor, Duration(activeDurationMillis, TimeUnit.MILLISECONDS))
  } catch {
    case ex: Exception =>
      logger.warn(s"Expected exception while running test ${ex}")
  }

  logger.warn(
    s"""
       |##################################################################################################
       |Number of generated(attempted) instants            : ${numOfInstantAttempts.get()}
       |Number of commits in the future                    : ${numOfFutureCommits.get()}
       |Number of ArrayIndexOutOfBoundsException exceptions: ${numOfArrayIndexOutOfBoundsExceptions.get()}
       |##################################################################################################
       |""".stripMargin)

  private[hudi] def schedule(): List[Future[Unit]] = (1 to activeThreads).map(_ => worker()).toList

  private[hudi] def worker(): Future[Unit] = Future {

    while(true) {
      try {
        numOfInstantAttempts.getAndIncrement()
        val instant = timed(s"Generating instant in thread ${Thread.currentThread()}") {
          HoodieActiveTimeline.createNewInstantTime()
        }
        if(checkFutureInstant(instant)) {
          numOfFutureCommits.incrementAndGet()
        }
      } catch {
        case ex: ArrayIndexOutOfBoundsException =>
          numOfArrayIndexOutOfBoundsExceptions.incrementAndGet()
          logger.warn(s"${Thread.currentThread()}: ${ex}")
      }
    }
  }

  private[hudi] def checkFutureInstant(instant: String): Boolean = {
    val now = LocalDateTime.now().format(instantFormatter)
    //val now = LocalDateTime.now().format(instantFormatter0_10_0)
    if(instant > now) {
      logger.warn(s"Instant in the future, Thread: ${Thread.currentThread()}, instant: ${instant}, now: ${now}")
      true
    } else {
      logger.info(s"Thread: ${Thread.currentThread()}, instant: ${instant}, now: ${now}")
      false
    }
  }

  private[hudi] def timed[A](blockDesc: String)(block: => A): A = timedWithMs(blockDesc = blockDesc)(block) match {
    case (result, _) => result
  }

  private[hudi] def timedWithMs[A](blockDesc: String)(block: => A): (A, Long) = {
    logger.info(s"Starting timed block -> $blockDesc")
    val startTime = System.currentTimeMillis
    val result = block
    val finishTime = System.currentTimeMillis() - startTime

    logger.info(s"Time spent in block -> $blockDesc is $finishTime ms")

    (result, finishTime)
  }
}
