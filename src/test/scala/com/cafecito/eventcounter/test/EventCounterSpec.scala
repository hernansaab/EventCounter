package com.cafecito.eventcounter.test

import com.cafecito.eventcounter.{CurentTimeMs, EventCounter}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.apache.spark.util.SizeEstimator

/**
 * Test spec for EventCounter
 * There is one true unit test case (because System.currentTimeMillis() is mocked) at the end of this spec.
 * There is also a memory footprint test to ensure there are no memory leaks
 *
 */
class EventCounterSpec extends AnyFlatSpec  with MockFactory {

  def signalNTimes(eventCounter:EventCounter, count:Int): Unit = {
    (1 to count).foreach(i => eventCounter.signal())
  }
  it should "Should generate 0 if no signals added and timeBackInSeconds set greater than timeWindowInSeconds" in {
    val eventCounter = new EventCounter(10)
    assert(eventCounter.countEvents(300) == 0)
  }

  it should "produce Error if constructor argument is less or equal to zero" in {
    assertThrows[Error] {
      val eventCounter = new EventCounter(0)
    }
  }

  it should "work with small increments of 100 ms" in {
    val eventCounter = new EventCounter(3)
    signalNTimes(eventCounter, 5)
    Thread.sleep(100)
    signalNTimes(eventCounter, 4)
    Thread.sleep(100)
    signalNTimes(eventCounter, 2)
    Thread.sleep(100)
    assert(eventCounter.countEvents(3) == 11)
    assert(eventCounter.countEvents(4) == 11)
    assert(eventCounter.countEvents(0) == 0)
    Thread.sleep(900)
    signalNTimes(eventCounter, 4)

    assert(eventCounter.countEvents(1) == 4)
    assert(eventCounter.countEvents(2) == 15)
    Thread.sleep(950)
    assert(eventCounter.countEvents(2) == 15)
    Thread.sleep(100)
    assert(eventCounter.countEvents(2) == 4)
    assert(eventCounter.countEvents(3) == 15)
    Thread.sleep(100)
    assert(eventCounter.countEvents(2) == 4)

  }
  it should "work with small time window of 3 seconds" in {
    val eventCounter = new EventCounter(3)
    eventCounter.signal()
    assert(eventCounter.countEvents(1) == 1)
    assert(eventCounter.countEvents(3) == 1)
    Thread.sleep(1000)
    eventCounter.signal()
    eventCounter.signal()
    eventCounter.signal()
    Thread.sleep(1000)
    assert(eventCounter.countEvents(2) == 3)
    assert(eventCounter.countEvents(3) == 4)
    Thread.sleep(1000)
    assert(eventCounter.countEvents(1) == 0)
    assert(eventCounter.countEvents(3) == 3)
    Thread.sleep(1000)
    assert(eventCounter.countEvents(1) == 0)
    assert(eventCounter.countEvents(3) == 0)
    Thread.sleep(4000)
    eventCounter.signal()
    assert(eventCounter.countEvents(3) == 1)
    Thread.sleep(3100)
    assert(eventCounter.countEvents(3) == 0)
    assert(eventCounter.countEvents(2) == 0)
    assert(eventCounter.countEvents(1) == 0)
    assert(eventCounter.countEvents(0) == 0)
  }
  it should "work with very small time window of 1 seconds" in {
    val eventCounter = new EventCounter(1)
    eventCounter.signal()
    assert(eventCounter.countEvents(1) == 1)
    assert(eventCounter.countEvents(0) == 0)
    Thread.sleep(900)
    assert(eventCounter.countEvents(1) == 1)
    Thread.sleep(200)
    assert(eventCounter.countEvents(1) == 0)
    eventCounter.signal()
    assert(eventCounter.countEvents(1) == 1)
    Thread.sleep(200)
    assert(eventCounter.countEvents(1) == 1)
    Thread.sleep(1000)
    assert(eventCounter.countEvents(1) == 0)
    signalNTimes(eventCounter, 4)
    assert(eventCounter.countEvents(1) == 4)
    Thread.sleep(200)
    assert(eventCounter.countEvents(1) == 4)
    Thread.sleep(1100)
    assert(eventCounter.countEvents(1) == 0)
    Thread.sleep(200)
    eventCounter.signal()
    assert(eventCounter.countEvents(1) == 1)
  }

  it should "have stable memory footprint" in {
    val eventCounter = new EventCounter(10)
    val size = SizeEstimator.estimate(eventCounter)
    eventCounter.signal()
    eventCounter.signal()
    eventCounter.countEvents(5)
    assert(SizeEstimator.estimate(eventCounter) == size)
  }

  it should "Work even if signal occurs before current time due to high concurrency" in {

    val timeMsMock: CurentTimeMs = stub[CurentTimeMs]
    val eventCounter = new EventCounter(3)(timeMsMock)
    (timeMsMock.currentTimeMillis _).when().returns(12000L).twice()
    signalNTimes(eventCounter, 2)
    (timeMsMock.currentTimeMillis _).when().returns(11000L).once()
    signalNTimes(eventCounter, 1)
    (timeMsMock.currentTimeMillis _).when().returns(12100L).once()
    assert(eventCounter.countEvents(1) == 2)
    (timeMsMock.currentTimeMillis _).when().returns(12100L).once()
    assert(eventCounter.countEvents(3) == 3)
    (timeMsMock.currentTimeMillis _).when().returns(22100L).once()
    assert(eventCounter.countEvents() == 0)
  }
}
