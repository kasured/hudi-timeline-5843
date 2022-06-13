# hudi-timeline-5843
The purpose is to reproduce the issue described in https://github.com/apache/hudi/issues/5843

### Reproduce Locally
To initiate the test run the following command
```bash
sbt -J-Xmx1G -Dactive.threads=50 -Dactive.duration.millis=60000 run
```
You might see different results depending on the configuration but the number of generated instances will be significantly lower
than in the branch `hoodie/0.10.0`
```text
##################################################################################################
Number of generated(attempted) instants            : 107
Number of commits in the future                    : 9
Number of ArrayIndexOutOfBoundsException exceptions: 0
##################################################################################################
```
Also you will see future commits 
```text
4305 [pool-8-thread-56] WARN  com.example.hudi.HudiTimelineChecker$  - Instant in the future, Thread: Thread[pool-8-thread-56,5,main], instant: 20220613225959, now: 20220613220000
```
Also please pay attention that during that execution you will have very high CPU utilization because 
of the multiple CAS retries

I could not yet reproduce `ArrayIndexOutOfBoundsException` which we also see on our EMR cluster, however I believe that 
is also caused by the fact that `SimpleDateFormat` is used. One can check very similar symptoms here https://stackoverflow.com/questions/4021151/java-dateformat-is-not-threadsafe-what-does-this-leads-to 