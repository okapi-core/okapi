package org.okapi.metrics.kafka;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecords;

public interface OkapiMetricsKafkaConsumer extends ConsumerRebalanceListener {
    void consume(ConsumerRecords<byte[], byte[]> consumerRecord);
}
