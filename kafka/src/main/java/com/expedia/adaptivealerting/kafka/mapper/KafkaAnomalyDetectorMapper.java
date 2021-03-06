/*
 * Copyright 2018 Expedia Group, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.expedia.adaptivealerting.kafka.mapper;

import com.expedia.adaptivealerting.anomdetect.AnomalyDetectorMapper;
import com.expedia.adaptivealerting.anomdetect.util.HttpClientWrapper;
import com.expedia.adaptivealerting.anomdetect.util.ModelServiceConnector;
import com.expedia.adaptivealerting.core.data.MappedMetricData;
import com.expedia.adaptivealerting.kafka.AbstractKafkaApp;
import com.expedia.adaptivealerting.kafka.serde.JsonPojoSerde;
import com.expedia.adaptivealerting.kafka.util.AppUtil;
import com.expedia.metrics.MetricData;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;

import java.util.Set;
import java.util.stream.Collectors;

import static com.expedia.adaptivealerting.core.util.AssertUtil.notNull;
import static com.expedia.adaptivealerting.kafka.KafkaConfigProps.*;

/**
 * Kafka wrapper around {@link AnomalyDetectorMapper}.
 *
 * @author David Sutherland
 * @author Willie Wheeler
 */
@Slf4j
public final class KafkaAnomalyDetectorMapper extends AbstractKafkaApp {
    private AnomalyDetectorMapper mapper;
    
    public static void main(String[] args) {
        final Config config = AppUtil.getAppConfig(ANOMALY_DETECTOR_MAPPER);
        new KafkaAnomalyDetectorMapper(config, buildMapper(config)).start();
    }
    
    public KafkaAnomalyDetectorMapper(Config config, AnomalyDetectorMapper mapper) {
        super(config);
        notNull(mapper, "mapper can't be null");
        this.mapper = mapper;
    }
    
    @Override
    protected StreamsBuilder streamsBuilder() {
        final String inboundTopic = getAppConfig().getString(INBOUND_TOPIC);
        final String outboundTopic = getAppConfig().getString(OUTBOUND_TOPIC);
        
        log.info("Initializing: inboundTopic={}, outboundTopic={}", inboundTopic, outboundTopic);
        
        final StreamsBuilder builder = new StreamsBuilder();
        final KStream<String, MetricData> stream = builder.stream(inboundTopic);
        stream
                .flatMap((key, metricData) -> {
                            log.info("Mapping key={}, metricData={}", key, metricData);
                            final Set<MappedMetricData> mappedMetricDataSet = mapper.map(metricData);
                            return mappedMetricDataSet.stream()
                                    .map(mappedMetricData -> {
                                        final String newKey = mappedMetricData.getDetectorUuid().toString();
                                        return KeyValue.pair(newKey, mappedMetricData);
                                    })
                                    .collect(Collectors.toSet());
                        }
                )
                // TODO Make outbound serde configurable. [WLW]
                .to(outboundTopic, Produced.with(new Serdes.StringSerde(), new JsonPojoSerde<>()));
        
        return builder;
    }
    
    private static AnomalyDetectorMapper buildMapper(Config appConfig) {
        final HttpClientWrapper httpClient = new HttpClientWrapper();
        final String uriTemplate = appConfig.getString(MODEL_SERVICE_URI_TEMPLATE);
        final ModelServiceConnector connector = new ModelServiceConnector(httpClient, uriTemplate);
        return new AnomalyDetectorMapper(connector);
    }
}
