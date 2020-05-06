# **Event Counter Library**
This library allows you to track events and count the number of events that occurred in a time window.  
This library allows for EventCounter public methods to be invoked concurrently in different threads.  


### Installing Event Counter

#### Add file github.sbt to path $HOME/.sbt/1.0 with the following content (required by github):
```
credentials +=
   Credentials(
     "GitHub Package Registry",
     "maven.pkg.github.com",
     "hernansaab",
     "4da13dbaf713e30c067aa59734cf11d9c17a3ed4")
```
     
#### Add the following to your build.sbt file
```sbt
externalResolvers += "ExampleLibrary packages" at "https://maven.pkg.github.com/cafecito/sbt-github-packages"

libraryDependencies += "com.cafecito" %% "eventcounter" % "0.2.0-SNAPSHOT"
```

### Usage
#### 
```scala
import com.hernansaab.EventCounter

// Constructs event counter with time window of 20 seconds
val eventCounter = new EventCounter(20)

// Signal an event
eventCounter.signal()
eventCounter.signal()
eventCounter.signal()

// Count the number of events that were added within 2 second ago
val countedEvents = eventCounter.countEvents(2)

// Should display "3 events occurred 2 seconds ago"  

println(f"$countedEvents events occurred 2 seconds ago")

```
