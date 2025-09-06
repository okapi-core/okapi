package org.okapi.metrics.kafka;

import com.google.gson.Gson;
import java.io.IOException;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssigner;
import org.okapi.metrics.io.StreamReadingException;
import org.okapi.rest.metrics.SubmitMetricsRequestInternal;

@Slf4j
public class KafkaMetricsWriter implements OkapiMetricsKafkaWriter {
    @Setter
    ShardsAndSeriesAssigner shardsAndSeriesAssigner;
    String topic;
    KafkaProducer<byte[], byte[]> producer;
    Gson gson;
    
    @Override
    public void onRequestArrive(SubmitMetricsRequestInternal request) {
        var serializd = gson.toJson(request);
        var producerRecord = new ProducerRecord<byte[], byte[]>(topic, serializd.getBytes());
        producer.send(producerRecord);
    }


    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void init() throws IOException, StreamReadingException {
        log.info("This is a no-op.");
    }
}
