### Codelab Megalith

Megalith is the module container within the Codelab ecosystem.
It directly manages all infra dependencies, discovers CodelabModules registered via Spring factories, 
and includes their autoconfiguration within its application context.


### Module Registry

`CodelabModuleRegistrar` is responsible for discovering modules and triggering infra-specific bean registration for 
all infra components for all modules.

### Persistence

Megalith exposes JPA semantics as-is at a per-module level. It manages connectivity configuration to a common data
source, and allows modules to connect to separate logical units within the database. Each module is mapped 

Megalith disables JPA autoconfiguration. Instead, it provides module-specific DataSource, EntityManagerFactory, 
and TransactionManager for a given persistence unit, supplied via module config. It also instantiates all JpaRepository
extensions found within the module.

### Messaging

Megalith uses Apache Pulsar for messaging, exposing thin interfaces that piggyback on Spring Boot's Pulsar starter
implementation. Each module is mapped to a dedicated Pulsar tenant. Topics and subscriptions are internally namespaced
to the module.

Additionally, producers must be defined per type, as opposed to per queue, mimicking JPA's repository pattern.
For example, to declare a publisher for `CreateOrderEvent`:
```java
@CodelabTopic(name = "orders", value = "orders")
public interface CreateOrderEventPublisher extends CodelabEventPublisher<CreateOrderEvent> {
}
```
Then for actually pushing events:
```java
@Slf4j
@Service
@AllArgsConstructor
public class OrderProducer {

  private final CreateOrderEventPublisher createOrderEventPublisher;

  public void send(CreateOrderRequest request) {
    createOrderEventPublisher.publish(CreateOrderEvent.from(request));
    log.info("Message published at {}", new Date());
  }

}
```
The `CreateOrderEventPublisher` dependency is injected dynamically.

Message consumption works similarly, supporting multiple consumers:
```java
@CodelabSubscription(topic = "orders", subscriptionName = "codelab-orders", concurrency = 1)
public class OrderConsumer implements CodelabEventConsumer<CreateOrderEvent> {

  @Override
  public void consume(CreateOrderEvent event) {
    // business logic
  }
}
```
