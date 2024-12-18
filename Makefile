
build-docker:
	docker build --build-arg JAR_FILE=target/\*.jar -t apollographql/apollo-connector-gen .

