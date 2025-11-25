# Sistema de Embeddings Distribuído com Kafka

## Arquitetura

Este sistema utiliza **Apache Kafka** para processamento distribuído e escalável de embeddings:

```
┌─────────────────┐         ┌──────────────┐         ┌──────────────────┐
│   Aplicação     │ ─────>  │    Kafka     │  ─────> │  Consumer Worker │
│  (Producer)     │ Publica │   Broker     │ Consume │  (Processa       │
│                 │         │              │         │   Embedding)     │
└─────────────────┘         └──────────────┘         └──────────────────┘
                                   │                          │
                                   │                          ▼
                                   │                  ┌────────────────┐
                                   │                  │   MongoDB      │
                                   ▼                  │  (Armazena)    │
                            ┌──────────────┐         └────────────────┘
                            │  Monitoring  │
                            │  & Metrics   │
                            └──────────────┘
```

## Tópicos Kafka

- **`embedding-requests`**: Requisições de criação de embeddings
- **`embedding-results`**: Resultados do processamento (sucesso/falha)
- **`embedding-dlq`**: Dead Letter Queue para mensagens falhadas após retries

## Iniciar Infraestrutura

```bash
# Subir Kafka, Zookeeper, MongoDB e Ollama
docker-compose up -d

# Verificar status
docker-compose ps

# Logs do Kafka
docker logs kafka -f
```

## Configurações

### application.properties
```properties
# Kafka
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=embedding-service-group
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.enable-auto-commit=false
```

### Escalabilidade

**Consumers paralelos por instância**: Configurado em `KafkaConfig.java`
```java
private final int concurrency = 3; // 3 threads por instância
```

**Múltiplas instâncias**: Basta rodar mais instâncias da aplicação:
```bash
# Instância 1
mvn spring-boot:run

# Instância 2 (em outro terminal)
SERVER_PORT=8081 mvn spring-boot:run

# Instância 3
SERVER_PORT=8082 mvn spring-boot:run
```

Kafka automaticamente distribui as partições entre os consumers!

## Uso

### Enfileirar Embedding (Assíncrono)

```java
@Autowired
private EmbeddingService embeddingService;

public void indexarDocumento() {
    Map<String, Object> metadata = Map.of(
        "source", "api",
        "category", "technical"
    );
    
    CompletableFuture<String> eventId = embeddingService.createDocumentAsync(
        "Conteúdo do documento",
        metadata,
        "username",
        "document"
    );
    
    eventId.thenAccept(id -> 
        log.info("Enfileirado: {}", id)
    );
}
```

### Monitoramento

Os logs mostram o fluxo completo:

```
[KAFKA PRODUCER] Enviando evento abc-123 para tópico 'embedding-requests'
[KAFKA PRODUCER] Evento abc-123 enviado - partition: 0, offset: 42
[KAFKA CONSUMER] Processando evento abc-123 da partition 0 offset 42
[KAFKA CONSUMER] Embedding processado - evento: abc-123, docId: doc-456, tempo: 1250ms
[KAFKA MONITOR] ✓ Embedding concluído - evento: abc-123, docId: doc-456, tempo: 1250ms
```

## Retry e Error Handling

1. **Retry automático**: 3 tentativas com backoff de 1s
2. **Dead Letter Queue**: Após 3 falhas, evento vai para DLQ
3. **Manual acknowledge**: Só confirma após processamento bem-sucedido

## Vantagens do Kafka vs Fila Interna

| Recurso | Fila Interna | Kafka |
|---------|-------------|-------|
| **Escalabilidade** | ❌ Limitado | ✅ Horizontal |
| **Persistência** | ❌ Memória | ✅ Disco |
| **Distribuído** | ❌ Single node | ✅ Multi-node |
| **Replay** | ❌ Não | ✅ Sim |
| **Monitoramento** | ⚠️ Básico | ✅ Robusto |
| **Backpressure** | ⚠️ Manual | ✅ Automático |

## Métricas e Observabilidade

- **Lag do consumer**: `kafka-consumer-groups --describe`
- **Taxa de processamento**: Logs `[KAFKA MONITOR]`
- **Erros**: Tópico `embedding-dlq`

## Troubleshooting

### Kafka não conecta
```bash
docker logs kafka
# Verificar se porta 9092 está disponível
```

### Consumer não processa
```bash
# Ver consumer group
docker exec -it kafka kafka-consumer-groups --bootstrap-server localhost:9092 --list

# Ver lag
docker exec -it kafka kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group embedding-service-group --describe
```

### Reprocessar DLQ
```bash
# Consumir mensagens da DLQ
docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic embedding-dlq --from-beginning
```

## Performance

- **Throughput**: ~100-200 embeddings/min por instância (depende do modelo)
- **Latência média**: 1-2 segundos por embedding
- **Escalável**: Linear com número de instâncias

## Próximos Passos

- [ ] Dashboard de monitoramento (Kafka UI, Grafana)
- [ ] Rate limiting por usuário
- [ ] Priorização de mensagens
- [ ] Schema Registry para versionamento de eventos
