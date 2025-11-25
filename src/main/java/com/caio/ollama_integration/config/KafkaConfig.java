package com.caio.ollama_integration.config;

import com.caio.ollama_integration.model.kafka.EmbeddingRequestEvent;
import com.caio.ollama_integration.model.kafka.EmbeddingResultEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuração do Kafka para processamento distribuído de embeddings
 */
@Slf4j
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:embedding-service-group}")
    private String groupId;

    private final int concurrency = 3; // Número de consumers paralelos por instância

    // ==================== PRODUCER CONFIG ====================

    @Bean
    public ProducerFactory<String, EmbeddingRequestEvent> embeddingRequestProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "1");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, EmbeddingRequestEvent> embeddingRequestKafkaTemplate() {
        return new KafkaTemplate<>(embeddingRequestProducerFactory());
    }

    @Bean
    public ProducerFactory<String, EmbeddingResultEvent> embeddingResultProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "1");

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, EmbeddingResultEvent> embeddingResultKafkaTemplate() {
        return new KafkaTemplate<>(embeddingResultProducerFactory());
    }

    // ==================== CONSUMER CONFIG ====================

    @Bean
    public ConsumerFactory<String, EmbeddingRequestEvent> embeddingRequestConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        configProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, EmbeddingRequestEvent.class.getName());
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.caio.ollama_integration.model.kafka");
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000); // 5 minutos

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EmbeddingRequestEvent> embeddingRequestKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, EmbeddingRequestEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(embeddingRequestConsumerFactory());
        factory.setConcurrency(concurrency); // Número de consumers paralelos
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        // Retry e error handling
        factory.setCommonErrorHandler(new org.springframework.kafka.listener.DefaultErrorHandler(
                (record, exception) -> {
                    log.error("[KAFKA] Erro ao processar mensagem após retries: {}", record, exception);
                },
                new org.springframework.util.backoff.FixedBackOff(1000L, 3L) // 3 tentativas com 1s de intervalo
        ));

        return factory;
    }

    @Bean
    public ConsumerFactory<String, EmbeddingResultEvent> embeddingResultConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId + "-result");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        configProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, EmbeddingResultEvent.class.getName());
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.caio.ollama_integration.model.kafka");
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EmbeddingResultEvent> embeddingResultKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, EmbeddingResultEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(embeddingResultConsumerFactory());
        factory.setConcurrency(2);

        return factory;
    }
}
