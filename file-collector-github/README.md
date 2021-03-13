# file-collector-github

```
gradlew shadowJar
java -jar build/libs/file-collector-github-all.jar -q "extension:pdf filename:test5" -o output
```

The arguments are as follows.

```
usage: java -jar file-collector-github-all.jar -q <query> -o <directory>
 -q,--query <query>        Search query
 -o,--output <directory>   Output directory
```
