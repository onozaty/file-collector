# file-collector-download

```
gradlew shadowJar
java -jar build/libs/file-collector-download-all.jar -u urls.txt -o output
```

The arguments are as follows.

```
usage: java -jar file-collector-download-all.jar -u <file> -o <directory>
 -u,--urls <file>          URL list file
 -o,--output <directory>   Output directory
```
