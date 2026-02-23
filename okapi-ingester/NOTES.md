# To run the okapi-ingester in test mode

Please use profiles; `chtest` and `chengine`.
This can be applied with `--spring.profiles.active=chtest,chengine`.


# Running Kafka JMX scraper
To scrape Kakfa JMX metrics use `opentelemetry-jmx-scraper.jar` with config file:
`sample-otel-config/otel-jmx-scraper/config.properties`.