package com.cafecito.eventcounter

/**
 * CurrentTimeMsImplementation and CurentTimeMs are built for testing
 * This class wraps System.currentTimeMillis() to allow for overriding times during testing
 */

protected trait CurentTimeMs {
  def currentTimeMillis(): Long
}

private class CurrentTimeMsImplementation extends CurentTimeMs {
  override def currentTimeMillis(): Long = System.currentTimeMillis()
}

/**
 * EventCounter
 * There are two public methods signal() and countEvents()
 * Both methods can be invoked independently in different threads, provided object is already constructed
 * Size of EventCounter
 *  This object contains two arrays, timeWindow[Int] contains the count for each second timeslot and
 *    the other, timeWindowLastUpdated[Long] keeps track of last updated time for each second timeslot
 *    Size in bytes: timeWindowInSeconds * 4 + timeWindowInSeconds * 8
 * Total memory usage (bytes) = 100 + timeWindowInSeconds * 4 + timeWindowInSeconds * 8
 * Example if we construct object from new EventCounter(300) it would occupy the following amount of memory
 *    Size in bytes = 100 + 300*4 + 300*8 = 3700 bytes
 *
 * Performance of EventCounter
 *  signal() takes constant time to run. It is always fast and there are no iterators used.
 *  countEvents(n) takes time proportional to n to run
 *
 * @param timeWindowInSeconds
 * @param currentTimeMs
 */
class EventCounter(timeWindowInSeconds: Int)(implicit currentTimeMs: CurentTimeMs = new CurrentTimeMsImplementation) {
  if (timeWindowInSeconds <= 0) {
    throw new Error(f"Values less or equal to zero not accepted in constructor. timeWindowInSeconds set to $timeWindowInSeconds")
  }

  /**
   * Convenience converter used mainly do to the Long type of
   * System.currentTimeMillis() matching the index type of array timeWindow
   *
   * @param long long number
   * @return int number
   */
  implicit def longToInt(long: Long): Int = long.intValue()

  // Here is where we count all the events for each second timeslot
  // THis array is implemented as circular
  val timeWindow: Array[Int] = new Array[Int](timeWindowInSeconds + 1)
  val timeWindowLastUpdated: Array[Long] = new Array[Long](timeWindowInSeconds + 1)


  // Start position of circular array timeWindow
  @volatile var tail: Int = 0
  @volatile var head: Int = timeWindowInSeconds

  /**
   * Signal an event of any type occurring at time of invocation
   */
  def signal(): Unit = {
    val timestamp = currentTimeMs.currentTimeMillis()
    this.synchronized {
      val lastCurrentTimeStamp = timeWindowLastUpdated(tail)
      val diff: Int = (timestamp - lastCurrentTimeStamp) / 1000
      if (diff >= 0) {
        appendEventToTail(diff, timestamp)
      } else if (diff < 0) { //Uncommon, may only occur during high concurrency
        prependEventFromHead(diff, timestamp)
      }
    }
  }


  /**
   * Count the total number of signal() events relative to time of invocation, timeWindowInSeconds back in time
   *
   * @param timeBackInSeconds query parameter
   * @return number of events relative to time of invocation all the way back to timeBackInSeconds
   */
  def countEvents(timeBackInSeconds: Int = timeWindowInSeconds): Int = {
    val timestamp = currentTimeMs.currentTimeMillis()
    val timeSlotIsNotExpired: Int => Boolean = (position: Int) => {
      (timestamp - timeWindowLastUpdated(position)) / 1000 <= timeWindowInSeconds
    }
    this.synchronized {
      val diff = timeBackInSeconds - (timestamp - timeWindowLastUpdated(tail)) / 1000
      val numberOfElementsInTimeWindow = getNumberOfElements()
      val limit = if (diff >= numberOfElementsInTimeWindow) numberOfElementsInTimeWindow else diff
      (0 until limit).map(i => {
        calculatePositionFromTail(-i)
      }).filter(position => {
        timeSlotIsNotExpired(position)
      }).map(timeWindow).sum
    }
  }

  /**
   * Add event that happened before lastCurrentTimeStamp.
   * This function should be called rarely. Only in high on concurrent loads
   *
   * @param diff
   * @param timestamp
   */
  private def prependEventFromHead(diff: Int, timestamp: Int): Unit = {
    //you can't add events past beyond timeWindowInSeconds
    if (Math.abs(diff) <= timeWindowInSeconds) {
      head = calculatePositionFromTail(diff - 1)
      val position = calculatePositionFromTail(diff)
      incrementEventCountInTimeWindowPosition(position, timestamp)
    }
  }

  /**
   * Add an event forward in time relative to lastCurrentTimeStamp.
   *
   * @param diff
   * @param timestamp
   */
  private def appendEventToTail(diff: Int, timestamp: Long): Unit = {
    if (diff >= timeWindowInSeconds) {
      head = calculatePositionFromTail(-1) //sets buffer size to 1 since we moved forward beyond timeWindowInSeconds
    } else {
      tail = calculatePositionFromTail(diff) //move tail forward in time
    }
    incrementEventCountInTimeWindowPosition(tail, timestamp)
  }

  def incrementEventCountInTimeWindowPosition(position: Int, timestamp: Long): Unit = {
    val elapsedSeconds = (timestamp - timeWindowLastUpdated(position)) / 1000
    // if timeslot is expired, we initialize to 1, otherwise, increment
    if (elapsedSeconds >= timeWindowInSeconds) {
      timeWindow(position) = 1
    } else {
      timeWindow(position) = timeWindow(position) + 1
    }
    timeWindowLastUpdated(position) = timestamp
  }


  private def getNumberOfElements(): Int = {
    if (tail >= head) {
      tail - head
    } else {
      timeWindow.length - (head - tail)
    }
  }

  private def calculatePositionFromTail(i: Int): Int = {
    val positionSigned = (tail + i) % timeWindow.length
    if (positionSigned < 0) {
      timeWindow.length + positionSigned
    } else {
      positionSigned
    }
  }
}
