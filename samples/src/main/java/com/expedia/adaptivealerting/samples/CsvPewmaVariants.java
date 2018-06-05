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
package com.expedia.adaptivealerting.samples;

import com.expedia.adaptivealerting.anomdetect.PewmaAnomalyDetector;
import com.expedia.adaptivealerting.tools.pipeline.filter.OutlierDetectorStreamFilter;
import com.expedia.adaptivealerting.tools.pipeline.sink.ConsoleLogStreamSink;
import com.expedia.adaptivealerting.tools.pipeline.sink.OutlierChartStreamSink;
import com.expedia.adaptivealerting.tools.pipeline.source.CsvMetricSource;
import com.expedia.adaptivealerting.tools.visualization.ChartSeries;

import java.io.InputStream;

import static com.expedia.adaptivealerting.tools.visualization.ChartUtil.*;

/**
 * @author Willie Wheeler
 */
public class CsvPewmaVariants {
    
    public static void main(String[] args) {
        final InputStream is = ClassLoader.getSystemResourceAsStream("samples/sample001.csv");
        final CsvMetricSource source = new CsvMetricSource(is, "data", 1000L);
        
        final OutlierDetectorStreamFilter pewma1Filter =
                new OutlierDetectorStreamFilter(new PewmaAnomalyDetector(0.15, 1.0, 2.0, 3.0, 0.0));
        final OutlierDetectorStreamFilter pewma2Filter =
                new OutlierDetectorStreamFilter(new PewmaAnomalyDetector(0.25, 1.0, 2.0, 3.0, 0.0));
        final OutlierDetectorStreamFilter pewma3Filter =
                new OutlierDetectorStreamFilter(new PewmaAnomalyDetector(0.35, 1.0, 2.0, 3.0, 0.0));
        
        final ChartSeries pewma1Series = new ChartSeries();
        final ChartSeries pewma2Series = new ChartSeries();
        final ChartSeries pewma3Series = new ChartSeries();
        
        source.addMetricPointSubscriber(pewma1Filter);
        source.addMetricPointSubscriber(pewma2Filter);
        source.addMetricPointSubscriber(pewma3Filter);
        
        pewma1Filter.addAnomalyResultSubscriber(new ConsoleLogStreamSink());
        pewma1Filter.addAnomalyResultSubscriber(new OutlierChartStreamSink(pewma1Series));
        pewma2Filter.addAnomalyResultSubscriber(new ConsoleLogStreamSink());
        pewma2Filter.addAnomalyResultSubscriber(new OutlierChartStreamSink(pewma2Series));
        pewma3Filter.addAnomalyResultSubscriber(new ConsoleLogStreamSink());
        pewma3Filter.addAnomalyResultSubscriber(new OutlierChartStreamSink(pewma3Series));
        
        showChartFrame(createChartFrame(
                "Cal Inflow",
                createChart("PEWMA: alpha=0.15", pewma1Series),
                createChart("PEWMA: alpha=0.25", pewma2Series),
                createChart("PEWMA: alpha=0.35", pewma3Series)));
        
        source.start();
    }
}