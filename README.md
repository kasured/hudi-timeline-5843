# hudi-timeline-5843
The purpose is to reproduce the issue described in https://github.com/apache/hudi/issues/5843

### Reproduce Locally
To initiate the test run the following command
```bash
sbt -J-Xmx1G -Dactive.threads=20 -Dactive.duration.millis=30000 run
```
```text
##################################################################################################
Number of generated(attempted) instants            : 28722
Number of commits in the future                    : 0
Number of ArrayIndexOutOfBoundsException exceptions: 0
##################################################################################################
```
