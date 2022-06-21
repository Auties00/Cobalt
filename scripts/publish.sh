cd ./..
mvn release:prepare -Darguments=-DskipTests
mvn release:perform -Darguments=-DskipTests