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
package com.expedia.adaptivealerting.kafka.serde;

import com.expedia.adaptivealerting.core.data.MappedMetricData;
import com.expedia.metrics.MetricData;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;

/**
 * Timestamp extractor for {@link MetricData}, similar to
 * {@link org.apache.kafka.streams.processor.LogAndSkipOnInvalidTimestamp}.
 * Adding this as com.expedia.www.haystack.commons.kstreams.MetricPointTimestampExtractor doesn't handle null values.
 *
 * @author Shubham Sethi
 * @author Willie Wheeler
 */
public final class MappedMetricDataTimestampExtractor implements TimestampExtractor {
    
    @Override
    public long extract(ConsumerRecord<Object, Object> record, long previousTimestamp) {
        final MappedMetricData mappedMetricData = (MappedMetricData) record.value();
        if (mappedMetricData == null || mappedMetricData.getMetricData() == null) {
            
            // We don't want to log this because sometimes it fills up the logs.
            // TODO Figure out what to do instead. Maybe a counter.
//            log.warn("Skipping null MappedMetricData");
            
            // -1 skips the record.
            return -1L;
        }
        return mappedMetricData.getMetricData().getTimestamp() * 1000L;
    }
}
