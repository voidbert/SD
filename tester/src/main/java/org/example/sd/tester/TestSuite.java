/*
 * Copyright 2024 Carolina Pereira, Diogo Costa, Humberto Gomes, Sara Lopes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.example.sd.tester;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;

import org.example.sd.common.KeyValueDB;
import org.example.sd.libserver.MultiConditionHashMapBackend;
import org.example.sd.libserver.ShardedHashMapBackend;
import org.example.sd.libserver.SimpleHashMapBackend;

import org.apache.commons.math3.distribution.AbstractIntegerDistribution;
import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import org.apache.xmlgraphics.java2d.GraphicContext;
import org.apache.xmlgraphics.java2d.ps.EPSDocumentGraphics2D;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

public class TestSuite {
    private final static int[] threadCounts = new int[] { 1, 2, 4, 8, 16 };

    private final static Map<String, OperationDistribution> operationDistributions = Map.ofEntries(
        Map.entry("Maioritariamente leituras",
                  new OperationDistribution(0.05, 0.70, 0.0, 0.25, 0.0)),
        Map.entry("Equilibrado", new OperationDistribution(0.25, 0.25, 0.25, 0.25, 0.0)),
        Map.entry("Equilibrado com getWhen",
                  new OperationDistribution(0.25, 0.20, 0.25, 0.25, 0.05)));

    private final static KeyValueDB[] backends =
        new KeyValueDB[] { new SimpleHashMapBackend(),
                           new MultiConditionHashMapBackend(),
                           new ShardedHashMapBackend(64) };

    private final String outputDirectory;

    public TestSuite(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public TestSuite(TestSuite suite) {
        this(suite.getOutputDirectory());
    }

    public String getOutputDirectory() {
        return this.outputDirectory;
    }

    public void run() throws IOException {
        (new File(this.outputDirectory)).mkdir();

        Map<String, DefaultCategoryDataset> times = new HashMap<String, DefaultCategoryDataset>();

        for (int nThreads : TestSuite.threadCounts) {
            for (Map.Entry<String, OperationDistribution> dist :
                 TestSuite.operationDistributions.entrySet()) {

                String                distName  = dist.getKey();
                OperationDistribution distValue = dist.getValue();

                times.putIfAbsent(distName, new DefaultCategoryDataset());

                DefaultCategoryDataset dataset = new DefaultCategoryDataset();
                for (KeyValueDB backend : TestSuite.backends) {
                    if (backend instanceof ShardedHashMapBackend && distValue.getGetWhen() > 0)
                        continue;

                    TestResults results = this.runTest(nThreads, backend, distValue);
                    this.exportCSV(results, nThreads, distName, backend);
                    this.addTestResultsToComparisonDataset(dataset, results, backend);

                    times.get(distName).addValue(results.getTestTime() * 1.0e-9,
                                                 nThreads + " threads",
                                                 backend.getClass().getSimpleName());
                }

                this.exportComparisonChart(dataset, nThreads, dist.getKey());
            }
        }

        for (Map.Entry<String, DefaultCategoryDataset> time : times.entrySet())
            if (TestSuite.operationDistributions.get(time.getKey()).getGetWhen() == 0.0)
                this.exportThreadsChart(time.getValue(), time.getKey());
    }

    private TestResults
        runTest(int nThreads, KeyValueDB backend, OperationDistribution operationDistribution) {
        final int nOperations        = operationDistribution.getGetWhen() > 0 ? 1 << 20 : 1 << 23;
        final int operationBlockSize = 4096;
        final int nKeys              = 128;
        final int nValues            = 128;
        final int keyLength          = 8;
        final int valueLength        = 8;

        final AbstractIntegerDistribution keyDistribution =
            new UniformIntegerDistribution(0, nKeys - 1);
        final AbstractIntegerDistribution valueDistribution =
            new UniformIntegerDistribution(0, nValues - 1);
        final AbstractIntegerDistribution multiCountDistribution =
            new UniformIntegerDistribution(2, 4);

        final DatabasePopulator populator =
            new DatabasePopulator(backend, nKeys, nValues, keyLength, valueLength);

        Test test = new Test(populator,
                             operationDistribution,
                             keyDistribution,
                             valueDistribution,
                             multiCountDistribution,
                             nThreads,
                             nOperations,
                             operationBlockSize);
        return test.run();
    }

    private void addTestResultsToComparisonDataset(DefaultCategoryDataset dataset,
                                                   TestResults            results,
                                                   KeyValueDB             backend) {

        for (Operation operation : Operation.values()) {
            if (operation != Operation.GET_WHEN) {
                OptionalDouble operationResult = results.getAverage(operation);
                if (!operationResult.isPresent())
                    continue;

                dataset.addValue(operationResult.getAsDouble(),
                                 backend.getClass().getSimpleName() + "   ",
                                 operation.toString());
            }
        }
    }

    private void
        exportCSV(TestResults results, int nThreads, String distributionName, KeyValueDB backend)
            throws IOException {

        String threadString = nThreads > 1 ? "threads" : "thread";
        String filename     = String.format("%s/%s_%s_%d_%s.csv",
                                        this.outputDirectory,
                                        backend.getClass().getSimpleName(),
                                        distributionName.replace(' ', '_'),
                                        nThreads,
                                        threadString);

        StringBuilder fileContents = new StringBuilder("OPERATION,AVG,STDEV\n");
        for (Operation operation : Operation.values()) {
            if (operation != Operation.GET_WHEN) {
                String         operationAvgStr = "";
                OptionalDouble operationAvg    = results.getAverage(operation);
                if (operationAvg.isPresent())
                    operationAvgStr = Double.toString(operationAvg.getAsDouble());

                String         operationStdevStr = "";
                OptionalDouble operationStdev    = results.getStdev(operation);
                if (operationStdev.isPresent())
                    operationStdevStr = Double.toString(operationStdev.getAsDouble());

                fileContents.append(operation);
                fileContents.append(",");
                fileContents.append(operationAvgStr);
                fileContents.append(",");
                fileContents.append(operationStdevStr);
                fileContents.append("\n");
            }
        }

        PrintWriter out = new PrintWriter(filename);
        out.print(fileContents.toString());
        out.close();
        System.out.printf("Exported %s\n", filename);
    }

    private void exportComparisonChart(CategoryDataset dataset,
                                       int             nThreads,
                                       String          distributionName) throws IOException {

        String threadString = nThreads > 1 ? "threads" : "thread";
        String title        = String.format("%s (%d %s)", distributionName, nThreads, threadString);
        String filename     = String.format("%s/%s_%d_%s.eps",
                                        this.outputDirectory,
                                        distributionName.replace(' ', '_'),
                                        nThreads,
                                        threadString);

        JFreeChart chart = ChartFactory.createBarChart(title, null, "Tempo (ns)", dataset);
        this.exportChart(chart, filename);
    }

    private void exportThreadsChart(CategoryDataset dataset, String distributionName)
        throws IOException {

        String title = String.format("Duração de um teste - %s", distributionName);
        String filename =
            String.format("%s/%s.eps", this.outputDirectory, distributionName.replace(' ', '_'));

        JFreeChart chart = ChartFactory.createBarChart(title, null, "Tempo (s)", dataset);
        this.exportChart(chart, filename);
    }

    private void exportChart(JFreeChart chart, String filename) throws IOException {
        (new OurChartTheme()).apply(chart);
        chart.getCategoryPlot().getDomainAxis().setTickMarksVisible(false);
        chart.getCategoryPlot().getRangeAxis().setTickMarksVisible(false);
        chart.getCategoryPlot().getRangeAxis().setAxisLineVisible(false);

        FileOutputStream      stream = new FileOutputStream(new File(filename));
        EPSDocumentGraphics2D g      = new EPSDocumentGraphics2D(false);
        g.setGraphicContext(new GraphicContext());
        g.setupDocument(stream, 800, 400);
        chart.draw(g, new Rectangle2D.Double(0, 0, 800, 400));
        stream.close();

        System.out.printf("Exported %s\n", filename);
    }

    public Object clone() {
        return new TestSuite(this);
    }

    public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass())
            return false;

        TestSuite suite = (TestSuite) o;
        return this.outputDirectory == suite.getOutputDirectory();
    }

    public String toString() {
        return String.format("TestSuite(outputDirectory = %s)", this.outputDirectory);
    }
}
