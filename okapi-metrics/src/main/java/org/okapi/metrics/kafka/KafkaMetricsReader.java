package org.okapi.metrics.kafka;

import java.util.Collection;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;

public class KafkaMetricsReader implements OkapiMetricsKafkaConsumer{
    String topic;

    @Override
    public void consume(ConsumerRecords<byte[], byte[]> records) {
        var body = records.records(topic);
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {

    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {

    }
}
