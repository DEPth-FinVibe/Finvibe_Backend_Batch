package depth.finvibe.modules.market.infra.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import depth.finvibe.modules.market.application.port.out.BatchPriceEventProducer;
import depth.finvibe.common.investment.dto.BatchPriceUpdatedEvent;

@Component
@RequiredArgsConstructor
public class MarketKafkaProducer implements BatchPriceEventProducer {
    private static final String BATCH_PRICE_UPDATED_TOPIC = "market.batch-price-updated.v1";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishBatchPriceUpdated(BatchPriceUpdatedEvent event) {
        kafkaTemplate.send(BATCH_PRICE_UPDATED_TOPIC, event);
    }
}
