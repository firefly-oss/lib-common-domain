package com.firefly.common.domain.events.inbound;

import com.firefly.common.domain.events.DomainEventEnvelope;
import com.firefly.common.domain.events.DomainSpringEvent;
import com.firefly.common.domain.events.properties.DomainEventsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Inbound Kinesis subscriber that polls shard iterators and republishes as DomainSpringEvent using AWS SDK v2 KinesisAsyncClient.
 */
public class KinesisDomainEventsSubscriber implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(KinesisDomainEventsSubscriber.class);
    private final ApplicationContext ctx;
    private final DomainEventsProperties props;
    private final ApplicationEventPublisher events;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "kinesis-events-subscriber");
        t.setDaemon(true);
        return t;
    });
    private volatile KinesisAsyncClient kinesisClient;
    private volatile String shardIterator;

    public KinesisDomainEventsSubscriber(ApplicationContext ctx,
                                         DomainEventsProperties props,
                                         ApplicationEventPublisher events) {
        this.ctx = ctx;
        this.props = props;
        this.events = events;
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        
        try {
            // Get Kinesis client from application context
            this.kinesisClient = ctx.getBean(KinesisAsyncClient.class);
            
            // Initialize shard iterator
            initializeShardIterator();
            
            long delay = props.getConsumer().getKinesis().getPollDelayMillis();
            executor.scheduleWithFixedDelay(this::pollRecordsSafe, 0L, delay, TimeUnit.MILLISECONDS);
            
            log.info("Kinesis domain events subscriber started for stream: {}", getStreamName());
            
        } catch (Exception e) {
            log.error("Error starting Kinesis domain events subscriber", e);
            running.set(false);
        }
    }

    private void initializeShardIterator() {
        try {
            String streamName = getStreamName();
            
            // List shards to get the first shard ID
            ListShardsRequest listShardsRequest = ListShardsRequest.builder()
                    .streamName(streamName)
                    .build();
            
            ListShardsResponse listShardsResponse = kinesisClient.listShards(listShardsRequest).get();
            
            if (listShardsResponse.shards().isEmpty()) {
                log.warn("No shards found for Kinesis stream: {}", streamName);
                return;
            }
            
            // Use the first shard for simplicity (in production, you'd handle multiple shards)
            String shardId = listShardsResponse.shards().get(0).shardId();
            
            GetShardIteratorRequest getShardIteratorRequest = GetShardIteratorRequest.builder()
                    .streamName(streamName)
                    .shardId(shardId)
                    .shardIteratorType(ShardIteratorType.LATEST) // Start from latest records
                    .build();
            
            GetShardIteratorResponse response = kinesisClient.getShardIterator(getShardIteratorRequest).get();
            this.shardIterator = response.shardIterator();
            
            log.debug("Initialized shard iterator for stream: {}, shard: {}", streamName, shardId);
            
        } catch (Exception e) {
            log.error("Failed to initialize shard iterator for stream: {}", getStreamName(), e);
            throw new RuntimeException("Failed to initialize Kinesis shard iterator", e);
        }
    }

    private void pollRecordsSafe() {
        try {
            pollRecords();
        } catch (Exception e) {
            if (running.get()) {
                log.error("Error polling Kinesis records", e);
            }
        }
    }

    private void pollRecords() throws Exception {
        if (shardIterator == null) {
            log.debug("No shard iterator available, skipping poll");
            return;
        }
        
        GetRecordsRequest getRecordsRequest = GetRecordsRequest.builder()
                .shardIterator(shardIterator)
                .limit(100) // Maximum number of records to retrieve
                .build();
        
        GetRecordsResponse response = kinesisClient.getRecords(getRecordsRequest).get();
        
        // Update shard iterator for next poll
        this.shardIterator = response.nextShardIterator();
        
        List<software.amazon.awssdk.services.kinesis.model.Record> records = response.records();
        if (records == null || records.isEmpty()) {
            log.debug("No records retrieved from Kinesis stream: {}", getStreamName());
            return;
        }
        
        log.debug("Retrieved {} records from Kinesis stream: {}", records.size(), getStreamName());
        
        for (software.amazon.awssdk.services.kinesis.model.Record record : records) {
            processRecord(record);
        }
    }

    private void processRecord(software.amazon.awssdk.services.kinesis.model.Record record) {
        try {
            String data = record.data().asUtf8String();
            
            // Try to parse the data as JSON to extract event metadata
            DomainEventEnvelope envelope = parseEventData(data, record);
            
            events.publishEvent(new DomainSpringEvent(envelope));
            
            log.debug("Processed Kinesis record: sequenceNumber={}, partitionKey={}, type={}", 
                     record.sequenceNumber(), record.partitionKey(), envelope.getType());
                     
        } catch (Exception e) {
            log.error("Error processing Kinesis record: sequenceNumber={}, partitionKey={}", 
                     record.sequenceNumber(), record.partitionKey(), e);
        }
    }

    private DomainEventEnvelope parseEventData(String data, software.amazon.awssdk.services.kinesis.model.Record record) {
        try {
            Object mapperObj = tryGetObjectMapper();
            if (mapperObj instanceof ObjectMapper mapper) {
                // Try to parse as the structured event format we use for publishing
                @SuppressWarnings("unchecked")
                Map<String, Object> eventData = mapper.readValue(data, Map.class);
                
                String topic = (String) eventData.get("topic");
                String type = (String) eventData.get("type");
                String key = (String) eventData.get("key");
                Object payload = eventData.get("payload");
                @SuppressWarnings("unchecked")
                Map<String, Object> headers = (Map<String, Object>) eventData.getOrDefault("headers", new HashMap<>());
                
                return DomainEventEnvelope.builder()
                        .topic(topic != null ? topic : getStreamName())
                        .type(type)
                        .key(key != null ? key : record.partitionKey())
                        .payload(payload)
                        .headers(headers)
                        .build();
            }
        } catch (Exception e) {
            log.debug("Failed to parse structured event data, treating as raw payload: {}", e.getMessage());
        }
        
        // Fallback: treat the entire data as payload
        Map<String, Object> headers = new HashMap<>();
        headers.put("kinesis-sequence-number", record.sequenceNumber());
        headers.put("kinesis-partition-key", record.partitionKey());
        
        return DomainEventEnvelope.builder()
                .topic(getStreamName())
                .type("kinesis.record")
                .key(record.partitionKey())
                .payload(data)
                .headers(headers)
                .build();
    }

    private Object tryGetObjectMapper() {
        try {
            return ctx.getBean(ObjectMapper.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String getStreamName() {
        String streamName = props.getConsumer().getKinesis().getStreamName();
        if (streamName == null || streamName.isEmpty()) {
            streamName = props.getKinesis().getStreamName();
        }
        if (streamName == null || streamName.isEmpty()) {
            throw new IllegalStateException("Kinesis stream name must be configured");
        }
        return streamName;
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("Stopping Kinesis domain events subscriber");
            executor.shutdown();
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }
}