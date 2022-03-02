package edu.umb.testutils;

import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.launcher.TestExecutionListener;
import stdlib.StdOut;

import java.io.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.logging.Logger;



public class TestListener implements TestWatcher, TestExecutionListener,
        BeforeAllCallback, BeforeEachCallback, BeforeTestExecutionCallback,
        AfterAllCallback,  AfterEachCallback,  AfterTestExecutionCallback,
        TestExecutionExceptionHandler {
    private static final int MAX_OUTPUT_LENGTH = 8192; // TODO: if exceed -> trigger something?
    private static final ByteArrayOutputStream capturedData = new ByteArrayOutputStream();
    private static final PrintStream STDOUT = System.out;
    private static final PrintStream TESTOUT  = new PrintStream(capturedData);
    private static final Logger logger = Logger.getLogger(TestListener.class.getName());

    private static boolean started;                 // Has testing begun?
    private static String mainTestClass;            // Used to track start and end time
    private static String msgFlag;                  // The string that encloses info on the Test
    private static HashMap<Integer, Test> tests;    // Stores tests sent from TestLauncher
    private static TestResult currentTestResult;    // used in beforeEach and afterEach
    private static List<TestResult> allTestResults;
    private static PriorityQueue<TestResult> allTestResults2;
    private static HashMap<Integer, TestResult> allTestResults3;
    private static LinkedHashMap<String, Double> pointDistribution;
    private static long startTime;
    private String testName;
    private static int count;

    static {
        count = 0;
        started = false;
        mainTestClass = null;
        logger.info(String.format("TestListener Logger online. level %s. Parent %s, level: %s",
                logger.getLevel(), logger.getParent(), logger.getParent().getLevel()));
    }

    public static void setMessageFlag(String flag, HashMap<Integer, Test> tests) {
        logger.finest(String.format("Setting message flag to %s and linking tests", flag));
        msgFlag = flag;
        TestListener.tests = tests;
    }

    public static void receiveMessage(String message) {
        logger.finest(String.format("Receiving message: %s", message));
        System.out.print(msgFlag+message+msgFlag);
    }

    public static void resetStdOut() {
        logger.finest("Resetting standard out");
        // Reset standard out, clear capturedData
        System.setOut(STDOUT);
        // TODO: Make StdOut a modular import?
        StdOut.resync();
        capturedData.reset();
    }

    /**
     * This method is called before any tests are run. Use this method
     * to set up all test variables and data structures.
     *
     * In reality, this method is called at the beginning of each test class.
     * The static variable 'started' is used to short circuit the method,
     * allowing it to be used as a sort of constructor for a single student's
     * submission.
     *
     * mainTestClass is used to track the first class that triggers this method,
     * which should also be the last class to trigger afterAll
     *
     * @param context The context for this test
     */
    @Override
    public void beforeAll(ExtensionContext context) {
        logger.finest("Setting up TestListener if not started yet");
        if (!started) {
            logger.finest("Tests not started. Starting setup");
            started = true;
            testName = "start";
            if (context.getTestClass().isPresent()) {
                mainTestClass = context.getTestClass().get().getCanonicalName();
            } else {
                throw new RuntimeException("beforeAll context.getTestClass() is not present");
            }
            allTestResults = new ArrayList<TestResult>();
            allTestResults2 = new PriorityQueue<TestResult>();
            allTestResults3 = new HashMap<Integer, TestResult>();
            pointDistribution = new LinkedHashMap<>();
            startTime = System.currentTimeMillis();
        }
    }

    /**
     * This is called before the @BeforeEach annotated methods
     *
     * Sets up a new TestResult object with the current tests's data.
     * @param context the context of the current test
     */
    @Override
    public void beforeEach(ExtensionContext context) {
        logger.finest(String.format("New test started. Context: %s", context.toString()));
        capturedData.reset();
        System.setOut(TESTOUT);
        StdOut.resync();
    }

    /**
     * This method is called immediately before a test method is executed.
     * This is AFTER any @BeforeEach is called, but before the test itself.
     * Currently unused
     *
     * @param context The context for the current test
     */
    @Override
    public void beforeTestExecution(ExtensionContext context) {
//        logger.finest("This method currently does nothing");
    }

    /**
     * This method is called only if an exception was thrown during the test.
     * This should not include exceptions thrown during setup or teardown.
     *
     * @param context the context for the current test
     * @param throwable the exception thrown by the test
     */
    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        logger.finer(String.format("Exception thrown! Handling %s", throwable.toString()));
        String output = setCurrentTest();
        currentTestResult.setScore(0);
        if (throwable.getMessage() != null && throwable.getMessage().contains("timed out after")) {
            String msg = "%s\n" +
                    "***********\n" +
                    "__TIMEOUT__\n" +
                    "***********\n" +
                    "Add print statements to your code to find out what's going wrong.\n\n" +
                    "A TIMEOUT message usually means one of two things:\n" +
                    "1. A loop or iterator isn't ending due to an unreachable ending condition,\n" +
                    "usually meaning an infinite loop. Print something in the iterator methods\n" +
                    "or loops.\n" +
                    "2. A recursive calculation isn't finding an ending condition. Print a\n" +
                    "message in the recursive methods to find out which one is being called\n" +
                    "too much.\n\n" +
                    "In either case, include some data values that should follow the pattern\n" +
                    "your code should be making -- iteration, calculation, traversal, decision,\n" +
                    "etc. Pair that up with your IDE debugger for added visibility into what's\n" +
                    "happening in your code.";
            currentTestResult.addOutput(String.format(msg, output));
        } else {
            currentTestResult.addOutput(String.format("%s\n%s\n", output, throwable.toString()));
        }
        throw throwable;
    }

    /**
     * This method is called immediately after a test method is executed.
     *
     * Currently unused
     *
     * @param context the context for the current test
     */
    @Override
    public void afterTestExecution(ExtensionContext context) {
//        logger.finest("This method currently does nothing");
    }

    /**
     * This method is called after each test method, and after any
     * \@AfterEach annotated method is called.
     *
     * Currently used to assess test success.
     *
     * @param context the context for the current test
     */
    @Override
    public void afterEach(ExtensionContext context) {
        logger.finest(String.format("Test ended. Context: %s", context.toString()));
        String capturedDataString = setCurrentTest();

        if (capturedDataString.length() > 0) {
             currentTestResult.addOutput(" Captured Test Output: \n");
            if (capturedDataString.length() > MAX_OUTPUT_LENGTH) {
                capturedDataString = capturedDataString.substring(0, MAX_OUTPUT_LENGTH) +
                        "... truncated due to excessive output!";
            }
            // This is the standard out that was printed during the test.
            currentTestResult.addOutput(capturedDataString);
        }

        resetStdOut();

        if (!currentTestResult.getName().equals(testName)) {
            testName = currentTestResult.getName();
            logger.info(String.format("Testing %s", testName));
        }

        allTestResults.add(currentTestResult);
        allTestResults2.add(currentTestResult);
        String name = currentTestResult.getName();
        name = name.contains("Checkstyle") ? "Checkstyle" : name;
        double total = pointDistribution.getOrDefault(name, 0.0);
        pointDistribution.put(name, total + currentTestResult.getMaxScore());
    }

    /**
     * This method is called after all test methods from a SINGLE test class
     * are done.
     *
     * TODO: confirm test ordering. Is it really the last test if thisTestClass.equals(mainTestClass)?
     *
     * @param context the context for the current test
     */
    @Override
    public void afterAll(ExtensionContext context) {
        logger.finest(String.format("All Tests Done. Context: %s", context.toString()));
        String thisTestClass;
        if (context.getTestClass().isPresent()) {
            thisTestClass = context.getTestClass().get().getCanonicalName();
        } else {
            throw new RuntimeException("afterAll context.getTestClass() is not present");
        }
        if(thisTestClass.equals(mainTestClass)) {
            long elapsed = System.currentTimeMillis() - startTime;

            double maxScore = 0.0;
            double score = 0.0;
            ArrayList<String> objects = new ArrayList<>();
            for (TestResult tr : allTestResults2) {
                objects.add(tr.toJSON());
                maxScore += tr.getMaxScore();
                score += tr.getScore();
            }
            String testsJSON = String.join(",", objects);

            String output = "{" + String.join(",", new String[] {
                    String.format("\"score\": %.2f", (score/maxScore)*TestUtils.getMaxTestScore()),
                    String.format("\"execution_time\": %d", elapsed),
                    String.format("\"tests\": [%s]", testsJSON),
                    String.format("\"leaderboard\": [%s]", leaderboard())
            }) + "}";
            System.err.printf("*************\nStudent score: %.2f / %.2f = %.2f\n*************\n",
                    score, maxScore, (score/maxScore)*TestUtils.getMaxTestScore());

            try (PrintWriter tempOut = new PrintWriter("/autograder/results/results.json")) {
                tempOut.println(output);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.err.println("Point Distribution:");
        double total = pointDistribution.keySet().stream().mapToDouble(key -> pointDistribution.get(key)).sum();

        for (String key : pointDistribution.keySet()) {
            double val = pointDistribution.get(key);
            System.err.printf("(%5.2f%%) %s: %s\n", (val/total)*100, key, val);
        }
    }

    private String setCurrentTest() {
        logger.finest("Setting currentTestResult and extracting output");
        String capturedDataString = capturedData.toString();

        int start, end;
        while((start = capturedDataString.indexOf(msgFlag)) != -1) {
            start += msgFlag.length();
            end = capturedDataString.indexOf(msgFlag, start);
            String message = capturedDataString.substring(start,end);
            String cutout = msgFlag + message + msgFlag;
            capturedDataString = capturedDataString.replace(cutout,"");

            int testNumber = Integer.parseInt(message);
            currentTestResult = allTestResults3.get(testNumber);
            if (null == currentTestResult) {
                Test test = tests.get(testNumber);
                currentTestResult = new TestResult(testNumber, test);
                allTestResults3.put(testNumber, currentTestResult);
            }
        }
        assert currentTestResult != null;
        return capturedDataString;
    }

    private String leaderboard() {
        logger.finest("Calculating leaderboard stats");
        FileReader fr;
        String leaderboard = "{\"name\": \"Score (%%)\", \"value\": %.2f, \"order\": \"desc\"}";
        JsonObject a;

        // read submission data as JsonObject
        try {
            fr = new FileReader("/autograder/submission_metadata.json");
            a = (JsonObject) Jsoner.deserialize(fr);
        } catch (FileNotFoundException e) {
            return String.format(leaderboard, -1.0);
        } catch (JsonException e) {
            e.printStackTrace();
            return String.format(leaderboard, -1.0);
        }

        // extract the relevant dates and previous submission count
        OffsetDateTime rel = OffsetDateTime.parse(((String)((JsonObject)a.get("assignment")).get("release_date")));
        OffsetDateTime due = OffsetDateTime.parse(((String)((JsonObject)a.get("assignment")).get("due_date")));
        OffsetDateTime sub = OffsetDateTime.parse((String)a.get("created_at"));

        // Calculate submission time differences
        long diff1 = (due.toEpochSecond() - rel.toEpochSecond());
        long diff2 = (sub.toEpochSecond() - rel.toEpochSecond());

        // Find the point slot that this submission belongs to. That is,
        // given the number of days (in seconds) allowed for this project, into
        // which tenth does this submission fall?
        double slotSize = ((double)diff1)/10.0;
        double slotCalc = ((double)diff2) / slotSize;
        int slot = (int)slotCalc;

        // Calculate the max/submission scores, based on the number of tests.
        // TODO: Is this infallible?
        double maxScore = 0.0;
        double submissionScore = 0.0;
        for (TestResult result : allTestResults2) {
            maxScore += result.getMaxScore();
            submissionScore += result.getScore();
        }

        // Calculate the leaderboard score. Breakdown:
        // 50% for autograder score
        // 0% based on previous submission count
        // 50% based on time until due
        double gradedScore = 0.5 * (submissionScore/maxScore) * 100;
        // TODO: Can this go negative if prior to release date?
        double timeScore = 0.5 * Math.max(0, 100 - slot*10);
        System.out.printf("\nLeaderboard Score: %.2f (Correctness: %.2f [%.2f/%.2f], Time: %.2f)\n",
                gradedScore+timeScore, gradedScore, submissionScore, maxScore, timeScore);

        double leaderboardScore = gradedScore + timeScore;

        return String.format(leaderboard, leaderboardScore);
    }
}
