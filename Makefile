.PHONY: test build run docker up demo k8s clean
test:        ## unit + characterization (parity) + API tests
	mvn -B verify
build:
	mvn -B -DskipTests package
run: build   ## zero-dependency local run (in-memory H2)
	java -jar target/option2-trade-slice.jar
docker:
	docker build -t option2-trade-slice:1.0.0 .
up:          ## app + Postgres
	docker compose up --build
demo:        ## scripted strangler walkthrough (service must be running)
	./scripts/demo.sh
k8s:         ## deploy to the current kube context
	kubectl apply -k k8s/
clean:
	mvn -B clean; rm -rf out
