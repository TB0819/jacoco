package org.jacoco.report;

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.internal.diff.CodeDiff;
import org.jacoco.core.internal.diff.GitAdapter;
import org.jacoco.core.tools.ExecFileLoader;
import org.jacoco.report.html.HTMLFormatter;
import org.jacoco.report.internal.html.HTMLGroupVisitor;

import java.io.File;
import java.io.IOException;

public class ReportGeneratorTest {
    private final String title;

    private final File executionDataFile;
    private final File classesDirectory;
    private final File sourceDirectory;
    private final File sourceDirectory1;
    private final File reportDirectory;

    private ExecFileLoader execFileLoader;

    /**
     * Create a new generator based for the given project.
     *
     */
    public ReportGeneratorTest(String rootPath, String projectName) {
        this.title = projectName;
        this.executionDataFile = new File(rootPath + "/jacoco-all-client.exec");
        this.classesDirectory = new File(rootPath +  "/bin");
        this.sourceDirectory = new File("C:\\workspace\\companywork\\demo/");
        this.sourceDirectory1 = new File("C:\\workspace\\companywork\\demo/src/main/java");
        this.reportDirectory = new File(rootPath + "/coveragereport");
        GitAdapter.setCredentialsProvider("cangzhu", "test1234");
    }

    /**
     * Create the report.
     *
     * @throws IOException
     */
    public void create() throws IOException {

        // Read the jacoco.exec file. Multiple data files could be merged
        // at this point
        loadExecutionData();

        // Run the structure analyzer on a single class folder to build up
        // the coverage model. The process would be similar if your classes
        // were in a jar file. Typically you would create a bundle for each
        // class folder and each jar you want in your report. If you have
        // more than one bundle you will need to add a grouping node to your
        // report
        final IBundleCoverage bundleCoverage = analyzeStructure();

        createReport(bundleCoverage);

    }

    private void createReport(final IBundleCoverage bundleCoverage)
            throws IOException {

        // Create a concrete report visitor based on some supplied
        // configuration. In this case we use the defaults
        final HTMLFormatter htmlFormatter = new HTMLFormatter();
        final IReportVisitor visitor = htmlFormatter
                    .createVisitor(new FileMultiReportOutput(reportDirectory));

        // Initialize the report with all of the execution and session
        // information. At this point the report doesn't know about the
        // structure of the report being created
        visitor.visitInfo(execFileLoader.getSessionInfoStore().getInfos(),
                execFileLoader.getExecutionDataStore().getContents());

        // Populate the report structure with the bundle coverage information.
        // Call visitGroup if you need groups in your report.
        //多源码路径
        MultiSourceFileLocator sourceLocator = new MultiSourceFileLocator(4);
        sourceLocator.add( new DirectorySourceFileLocator(
                sourceDirectory, "utf-8", 4));
        sourceLocator.add( new DirectorySourceFileLocator(
                sourceDirectory1, "utf-8", 4));

        visitor.visitBundle(bundleCoverage,sourceLocator);
//        visitor.visitBundle(bundleCoverage, new DirectorySourceFileLocator(
//                sourceDirectory, "utf-8", 4));

        // Signal end of structure information to allow report to write all
        // information out
        visitor.visitEnd();

    }

    private void loadExecutionData() throws IOException {
        execFileLoader = new ExecFileLoader();
        execFileLoader.load(executionDataFile);
    }

    private IBundleCoverage analyzeStructure() throws IOException {
        final CoverageBuilder coverageBuilder = new CoverageBuilder("C:\\workspace\\companywork\\demo","jacoco-cov");
//        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(
                execFileLoader.getExecutionDataStore(), coverageBuilder);

        analyzer.analyzeAll(classesDirectory);

        return coverageBuilder.getBundle(title);
    }

    /**
     * Starts the report generation process
     *
     * @param args
     *            Arguments to the application. This will be the location of the
     *            eclipse projects that will be used to generate reports for
     * @throws IOException
     */
    public static void main(final String[] args) throws IOException {
        String rootPath = System.getProperty("user.dir")+"/org.jacoco.report.test";
        final ReportGeneratorTest generator = new ReportGeneratorTest(rootPath,"jacocoTest");
        generator.create();
    }
}
