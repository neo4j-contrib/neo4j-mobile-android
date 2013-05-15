## About Neo4j

### The Project

The port is based on [1.5 M02](https://github.com/neo4j/community/tarball/1.5.M02).

## How to use Neo4j Embedded

There are two ways to use the embedded database in your app.

* Reference the neo4j-android project as an Android library project
* ... or just copy the neo4j-android.jar and use it like any other external Java library

Note that if you are using the Neo4j embedded database directly in your app you are linking against GPL software.

## Neo4j Service Wrapper

Android has a notion of [Services](http://developer.android.com/guide/topics/fundamentals/services.html), which can be used to components that are shared between apps (using IPC to cross process boundaries).

## Building the software

The Neo4A APK's have been successfully built with Java 1.6, Ant 1.8.1, Android SDK r21 and tested on Android 4.0.3.

* Prepare your development machine according to the [android developers site](http://developer.android.com/sdk/index.html).
* Checkout and prepare the source by creating local.properties pointing to the location of the Android SDK in each project: 


```
git clone https://github.com/UrsBoehm/Neo4A.git
cd Neo4A
echo "sdk.dir=[sdk-location]" > ./neo4j-android/local.properties
echo "sdk.dir=[sdk-location]" > ./neo4j-android-common/local.properties
echo "sdk.dir=[sdk-location]" > ./neo4j-android-client/local.properties
echo "sdk.dir=[sdk-location]" > ./neo4j-android-service/local.properties
echo "sdk.dir=[sdk-location]" > ./neo4j-android-dbinspector/local.properties
```

* Build and install the service:

```
cd neo4j-android-service
ant clean debug
adb uninstall org.neo4j.android.service
adb install bin/neo4j-android-service-debug.apk
```

* Build and install the dbinspector:

```
cd neo4j-android-dbinspector
ant clean debug
adb uninstall com.noser.neo4j.android.dbinspector
adb install bin/neo4j-android-dbinspector-debug.apk
```

* Run he dbinspector on the target device. The service will be started automatically.

## TODO

* Areas that need testing:
  * Node and relationship indexing
  * Transaction management when multiple clients are connected
* Investigate richer error handling (not just SOME error). Neo4j uses almost exclusively RuntimeExceptions

## Main Concepts

The following sections describe the core ideas behind the service abstraction.

### Project Layout

* neo4j-android (Android Library Project): The Android port of the embeddable Neo4j graph database
* neo4j-android-service (Android Project, APK): The actual service, IPC and transaction handling 
* neo4j-android-common (Android Library Project): The AIDL interface and Parcelables, which are used on both client and server-side
* neo4j-android-client (Android Library Project): The client support package, mainly helpers for error handling
* neo4j-android-dbinspector (Android Project, APK): Sample neo4j client implementing a graph database browser and manipulator.

To develop an app that uses the service, the neo4j-android-client library has to be added as a dependency. By transitivity, a dependency on neo4j-android-common is also incurred.

### AIDL Interface Design

The AIDL interface is inspired by the functionality provided by the [REST](http://docs.neo4j.org/chunked/stable/rest-api.html) interface.

#### Callback Objects

A not so well-known feature of AIDL is that you can pass as arguments or return other remote objects (IBinder instances). The interface uses this extensively, to model iterators (returning a remote object whose implementation lives on the server side) or evaluators (a remote object whose implementation lives on the client side).

#### Parcelables

To provide a rich and descriptive interface, we use parcelables extensively to model the Neo4j domain objects such as nodes and relationships, parameter objects (e.g. traversal description) and errors.

Parcelable classes need to be declared in AIDL files (one file per class), and need to be available on both the client and the server side.

#### Out Parameters

We use out parameters for error reporting. Every operation that can potentially fail has a ParcelableError out parameter that will hold non-zero error code in case of failure. Checking for errors on the client-side can be done either manually (not recommended) or by using the exception-enabled service wrapper in the client project.

### Transactions

Neo4j uses a JTA abstraction for its transaction manager. JTA proposes that the transaction context is associated with the thread that begins the transaction, i.e. the transaction is a thread-local object. This poses a problem, as Binder calls on the service side are served by a pool of worker threads and there is no immediate control over this mechanism. However, JTA has a notation of suspending and resuming transactions, where the thread who owns the context may suspend the transaction, and another thread can resume it (passing a handle that was returned when the transaction was suspended). We use this mechanism to resume transactions on entry into an IPC call, and suspend it on exit.

However, we still need to associate a context with something. That something is the binder instance through which the call was routed. The assumption here is that the client will execute all calls within the transaction through the same binder object.

Zombie transactions caused by the death of a client are cleaned up by a reaper thread.

We do *not* support nested transactions. Two subsequent calls to beginTx() will lead to an IllegalStateException. This is something we should probably support, but it's non-trivial at first glance.

### Access Control

We apply the standard Android permission-based security model to Neo4j by distinguishing between read and write access to the DB.
* org.neo4j.android.permission.READ: Read permission. Needed to bind to the service, and read data from the database.
* org.neo4j.android.permission.WRITE: Write permission. Needed to write data to the database.
* org.neo4j.android.permission.ADMIN: Required for administrative purposes.

### Error Handling

Exceptions across processes are not supported in Android. Actually, if an Android service throws an unchecked exception in a service call, the exception will bubble all the way up to the VM and the service will crash. Neo4j exceptions are almost exclusively runtime exceptions, which means every IPC call has a catch-all directive which will transform the exception to a ParcelableError and return it gracefully to the client.

We can't enforce error checking on the client, it is recommended to use the exception-enabled service wrapper in the client project.

### Indexing

Indexing is exposed through the same AIDL interface as other database operations. This is not terribly elegant, but needed as modifying index operations participate in transactions, and use the binder instance to associate a transactional context with our calls. Hence both have to be siphoned through the same binder instance.

## Handling Neo4j Updates

As long as there is no official Neo4j port, we have to merge our changes manually.

So far, only adaptions to the neo4j-kernel module have been made, and they are manageable.


### Licensing

From the Neo4j Download page:

> Community is licensed under GPL, and contains all the awesome graphiness you want
> Advanced is AGPL licensed, and adds monitoring capabilities to your database
> Enterprise is also AGPL licensed, and adds monitoring, live backups and high availability

We use the community edition.

## Porting Neo4j to Android

### General Considerations

The Neo4j project has a pretty modular structure. The modules we decided to port are the following:

* neo4j-kernel: Core graph DB concepts, embeddable database, I/O, transactions, ...
* neo4j-graph-algo: Standard graph algorithms
* neo4j-graph-matching: Graph matching algorithms
* neo4j-lucene-indexing: Indexing support via Apache Lucene

We have omitted the other modules because they are geared toward server usage, REST and command-line invocations, all of which do not apply to our use case.

### Missing Java Core APIs

Neo4j references some core Java namespaces which are not available in Android. The next sections describe which ones, and how we worked around it.

#### JTA - Java Transaction API

The JTA API is unavailable on Android, and used to expose a transactional interface in Neo4j.

Simply including the JTA API jar from the Apache Geronimo project is not enough. Dex does not allow defining classes in core namespaces (java.*,* javax*.*). The exact error message is shown below:

>Ill-advised or mistaken usage of a core class (java.* or javax.*)
>when not building a core library.
>
>This is often due to inadvertently including a core library file
>in your application's project, when using an IDE (such as
>Eclipse). If you are sure you're not intentionally defining a
>core class, then this is the most likely explanation of what's
>going on.
>
>However, you might actually be trying to define a class in a core
>namespace, the source of which you may have taken, for example,
>from a non-Android virtual machine project. This will most
>assuredly not work. At a minimum, it jeopardizes the
>compatibility of your app with future versions of the platform.
>It is also often of questionable legality.
>
>If you really intend to build a core library -- which is only
>appropriate as part of creating a full virtual machine
>distribution, as opposed to compiling an application -- then use
>the "--core-library" option to suppress this error message.
>
>If you go ahead and use "--core-library" but are in fact
>building an application, then be forewarned that your application
>will still fail to build or run, at some point. Please be
>prepared for angry customers who find, for example, that your
>application ceases to function once they upgrade their operating
>system. You will be to blame for this problem.
>
>If you are legitimately using some code that happens to be in a
>core package, then the easiest safe alternative you have is to
>repackage that code. That is, move the classes in question into
>your own package namespace. This means that they will never be in
>conflict with core system classes. JarJar is a tool that may help
>you in this endeavor. If you find that you cannot do this, then
>that is an indication that the path you are on will ultimately
>lead to pain, suffering, grief, and lamentation.
>
>[2011-11-08 13:48:13 - neo4j-android-example] Dx 1 error; aborting

We solve this by using the jarjar repackager to move all types in the javax.transaction namespace to an application-specific org.neo4j.javax.transaction namespace. The process is described [here](http://code.google.com/p/dalvik/wiki/JavaxPackages).

#### RMI - Remote Method Invocation

Basically the same issue as with JTA. Solved this by extracting the classes from the JDK's rt.jar, and repackaging them into org.neo4j.java.rmi namespace.

#### JMX - Java Management Extensions

JMX is used to determine properties of the VM runtime environment, such as amount of memory available.

Affected classes:
* org.neo4j.kernel.AutoConfigurator
* org.neo4j.kernel.ConfigurationLogging

JMX is not available on Android, and we can not apply the repackaging approach here (the Dalvik VM is very different beast than the JVM). So we need to find another solution to determine runtime machine properties.

Android devices have a notion of "memory class", which is summarized in the [Android developer reference](http://developer.android.com/reference/android/app/ActivityManager.html#getMemoryClass%28%29):

>public int getMemoryClass () 
>
>Since: [API Level > 5](http://developer.android.com/guide/appendix/api-levels.html#level5)
>
>Return the approximate per-application memory class of the current  device.  This gives you an idea of how 
>hard a memory limit you should  impose on your application to let the overall system work best.  The  >returned value is in megabytes; the baseline Android memory class is  16 (which happens to be the Java 
>heap limit of those devices); some  device with more memory may return 24 or even higher numbers.`

We use this value (on the TP build this currently returns 16).

The amount of heap space available can be adjusted by setting the following propert in init.rc:
```
setprop dalvik.vm.heapsize 32m
```

### Unit Tests

It would be desirable to run the Neo4j unit test suite (which seems to be rather extensive) on the Android device, to verify that it is unaffected by differences between the Java Standard Library and the Android implementation. However, it is not easily possible, since the Neo4j unit tests are written for JUnit 4, and Android supports only JUnit 3. The problem is nicely summed up in this [issue](http://code.google.com/p/android/issues/detail?id=10655).

I also investigated completely bypassing Android application and test package instrumentation, and just upload a JAR containing JUnit 4 and the Neo4j port + test cases to the device, and running it directly with dalvikvm. However, it's not as simple as that, since the test classes themselves do not have just Neo4j and JUnit 4 as dependency, but also reference JDK classes.

The bottom-line is we'd have to invest a bit more to port the unit tests to Android/JUnit 3.

### Miscellaneous

When using Neo4j, you get a lot of these warnings in Logcat:

>W/OSFileSystem(  842): fdatasync(2) unimplemented on Android - doing fsync(2)

Android does not have an fdatasync implementation, it uses fsync instead. After reading the [manpage](http://linux.die.net/man/2/fsync) on fsync and fdatasync I do not think this is a problem.

