package edu.umb.testutils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.logging.Logger;

import edu.umb.testutils.BuilderFaces.*;
import stdlib.StdRandom;

public class TestBuilder implements PreparedTest, ExceptionMessage,
        TwoStepTestStart, TwoStepTestFinish {
    private static final Logger logger = Logger.getLogger(TestBuilder.class.getName());

    static {
        logger.info(String.format("TestBuilder Logger online. level %s. Parent %s, level: %s",
                logger.getLevel(), logger.getParent(), logger.getParent().getLevel()));
    }
    // Private constructor to prevent direct instantiation
    private TestBuilder() {
        descriptionSB   = new StringBuilder();
        setupSB         = new StringBuilder();
        initialized     = false;
        seedCounter     = 0;
        seedIsSet       = false;
    }
    private TestBuilder(TestBuilder started) {
        if (started.constructorArgs == null) {
            String msg = "\nMissing constructor arguments.\nYou must use either the "
                    + "twoStepTestStart().constructorArgs() or the props().constructorArgs()\n"
                    + "method chain to set the arguments before calling twoStepTestFinish().";
            throw new UnpreparedTestException(msg);
        }
        descriptionSB = new StringBuilder(started.descriptionSB);
        if (started.setupSB.length() > 0) descriptionSB.append(started.setupSB.toString());
        setupSB = new StringBuilder();

        actualClassInstance = started.actualClassInstance;
        expectClassInstance = started.expectClassInstance;
        exceptionMessage    = started.exceptionMessage;
        constructorArgs     = started.constructorArgs;      // defensive copy?
        tempMethodName      = started.tempMethodName;
        exceptionType       = started.exceptionType;
        failedMessage       = started.failedMessage;
        actualClass         = started.actualClass;
        description         = started.description;
        expectClass         = started.expectClass;
        initialized         = started.initialized;
        seedCounter         = started.seedCounter;
        methodArgs          = started.methodArgs;           // defensive copy?
        methodName          = started.methodName;
        className           = started.className;
        fieldName           = started.fieldName;
        seedIsSet           = started.seedIsSet;
        maxScore            = started.maxScore;
        testType            = started.testType;
        hint                = started.hint;
        name                = started.name;
        note                = started.note;
        seed                = started.seed;
        stdInput            = started.stdInput;
    }

    // The factory to be used for all tests.
    static TestFactory factory;
    static void initFactory(TestFactory theFactory) { factory = theFactory; }

    // startedTest is used when initializing a test object via the factory.startTestCreation()
    // method. The same test object is used by any subsequent calls to factory.finishTestCreation(),
    // until a new call to factory.startTestCreation() is made.
    private static TestBuilder startedTest;

    /* ************************************************************************************** */
    /* Static fields and preparedTest section                                                 */
    /* ************************************************************************************** */

    // Static fields are re-used for tests created using the factory.preparedTest() method.
    // These fields are reset using the factory.resetStaticFields() method
    private static String   sName;
    private static String   sClassName;
    private static Object[] sConstructorArgs;
    private static String   sMethodName;
    private static Object[] sMethodArgs;
    private static double sMaxScore;
    private static String   sHint;
    private static TestType sTestType;
    private static String   sDescription;
    private static String   sNote;
    private static String   sExceptionMessage;
    private static long     sSeed;
    private static boolean  sSeedIsSet;
    private static String   sStdInput;
    private static boolean  sIterableOrderMatters;
    private static Class<? extends Throwable> sExceptionType;

    static void resetStaticFields() {
        sName = null;
        sClassName = null;
        sConstructorArgs = null;
        sMethodName = null;
        sMethodArgs = null;
        sMaxScore = 0;
        sHint = null;
        sTestType = null;
        sDescription = null;
        sNote = null;
        sExceptionMessage = null;
        sExceptionType = null;
        sSeed = 0;
        sSeedIsSet = false;
        sStdInput = null;
        sIterableOrderMatters = true;
    }

    static void setStaticName(String name)                { sName = name; }
    static void setStaticClassName(String className)      { sClassName = className; }
    static void setStaticClassArgs(Object... args)        { sConstructorArgs = args; }
    static void setStaticMethodName(String methodName)    { sMethodName = methodName; }
    static void setStaticMethodArgs(Object... args)       { sMethodArgs = args; }
    static void setStaticMaxScore(double maxScore)        { sMaxScore = maxScore; }
    static void setStaticHint(String hint)                { sHint = hint; }
    static void setStaticDescription(String description)  { sDescription = description; }
    static void setStaticNote(String note)                { sNote = note; }
    static void setStaticExceptionMessage(String message) { sExceptionMessage = message; }
    static void setStaticTestingStdOut()                  { sTestType = TestType.STDOUT; }
    static void setStaticTestingReturnValue()             { sTestType = TestType.RETURN; }
    static void setStaticTestingIterator()                { sTestType = TestType.ITERATOR; }
    static void setStaticTestingFieldValue()              { sTestType = TestType.FIELD; }
    static void setStaticTestingNodeTree()                { sTestType = TestType.NODE_TREE; }
    static void setStaticStandardIn(String input)         { sStdInput = input; }
    static void setStaticIterableOrderMatters(boolean matters) {
        sIterableOrderMatters = matters;
    }
    static void setStaticTestingCornerCaseForConstructor() {
        sTestType = TestType.EXCEPTION_CONSTRUCTOR;
    }
    static void setStaticTestingCornerCaseForMethod() {
        sTestType = TestType.EXCEPTION_METHOD;
    }
    static void setStaticExceptionType(Class<? extends Throwable> exceptionType) {
        sExceptionType = exceptionType;
    }
    static void setStaticTestingMainMethod() {
        sTestType = TestType.MAIN;
        sMethodName = "main";
    }
    static void setRandomSeed(long s) {
        sSeedIsSet = true;
        sSeed = s;
    }

    static PreparedTest preparedTest() {
        return preparedTest(null);
    }

    static PreparedTest preparedTest(Double maxScore) throws UnpreparedTestException {
        if (sName == null || sClassName == null || sTestType == null
                || (maxScore == null && sMaxScore == 0)) {
            String msg = String.format("Additional test fields must be defined before a prepared "
                            + "test method can be called.\nMissing fields: [%s, %s, %s, %s]",
                    sName == null ? "name"     : "", sClassName == null ? "className" : "",
                    sMaxScore < 1 ? "maxScore" : "", sTestType  == null ? "testType"  : "");
            throw new UnpreparedTestException(msg);
        }
        TestBuilder b = new TestBuilder();

        b.setName(sName);
        b.setTestType(sTestType);
        b.setMaxScore(maxScore != null ? maxScore : sMaxScore);
        b.setClassName(sClassName);

        if (sConstructorArgs != null)   b.withConstructorArgs(sConstructorArgs);
        if (sMethodName != null)        b.forMethod(sMethodName);
        if (sMethodArgs != null)        b.withMethodArgs(sMethodArgs);
        if (sHint != null)              b.withHint(sHint);
        if (sDescription != null)       b.withDescription(sDescription);
        if (sNote != null)              b.addNote(sNote);
        if (sExceptionType != null)     b.forExceptionType(sExceptionType);
        if (sExceptionMessage != null)  b.withExceptionMessage(sExceptionMessage);
        // Randomizes the seed on each reuse
        long newSeed = sSeed + (long)(Math.random()*sSeed);
//        System.err.printf("New seed: %d", newSeed);
        if (sSeedIsSet)                 b.setSeed(newSeed);
        if (sStdInput != null)          b.usingStdInput(sStdInput);
        if (!sIterableOrderMatters)     b.iterableOrderDoesNotMatter();
        return b;
    }

    /* ************************************************************************************** */
    /* Instance fields and builder section                                                    */
    /* ************************************************************************************** */

    private final StringBuilder descriptionSB; // Used to generate test descriptions
    private final StringBuilder setupSB;       // Used to generate test setup descriptions
    private String name;                    // The display name to be used for this test
    private String className;               // The name of the class to be tested
    private Class<?> expectClass;           // The solution's class to be tested
    private Class<?> actualClass;           // The submission class to be tested
    private Object[] constructorArgs;       // Arguments for the class's constructor
    private Object expectClassInstance;     // The solution's instance of the test class
    private Object actualClassInstance;     // The submission instance of the test class
    private String methodName;              // The name of the method to be tested
    private Object[] methodArgs;            // Arguments for the method to be invoked
    private String description;             // The test description to be provided
    private String hint;                    // The hint to be provided if the test fails
    private TestType testType;              // What type of test will be done?
    private String failedMessage;           // The fail message to be provided
    private double maxScore;                // How many points is this test worth?
    private String tempMethodName;          // Used to call methods on class instances
    private String exceptionMessage;        // The message given when an exception is thrown
    private String fieldName;               // The name of the field being checked
    private boolean initialized;            // Used to track whether initialize() was called
    private String note;                    // The note to be added to the description
    private long seed;                      // The seed to use for RNG
    private boolean seedIsSet;              // Has the RNG seed been set?
    private int seedCounter;                // Added to the seed at each step. Helps sync RNG
    private String stdInput;                // File name to be used as standard input
    private boolean iterableOrderMatters;   // Does the order of a returned iterator matter?
    private Class<? extends Throwable> exceptionType; // The type of exception to be thrown

    static void createStyleTest(String className) {
        new TestBuilder().styleTest(className, 2).build();
    }

    static void createStyleTest(String className, double value) {
        new TestBuilder().styleTest(className, value).build();
    }

    static void createFileExistenceTest(String fileName) {
        new TestBuilder().fileExistenceTest(fileName).build();
    }

    public void build() throws UnpreparedTestException {
        if (testType == null)             throw new UnpreparedTestException("test type must be set");
        if (name == null)                 throw new UnpreparedTestException("name must be set");
        if (hint == null)                 throw new UnpreparedTestException("hint must be set");
        if (maxScore < 0)                 throw new UnpreparedTestException("maxScore must be set");
        if (testType == TestType.RETURN || testType == TestType.STDOUT) {
            if (methodName == null)       throw new UnpreparedTestException("method name must be set");
            if (methodArgs == null)       throw new UnpreparedTestException("method args must be set");
            if (constructorArgs == null)  throw new UnpreparedTestException("constructor args must be set");
        }
        if (testType == TestType.EXCEPTION_CONSTRUCTOR || testType == TestType.EXCEPTION_METHOD) {
            if (exceptionType == null)    throw new UnpreparedTestException("exception type must be set");
            if (exceptionMessage == null) throw new UnpreparedTestException("exception message must be set");
        }
        if (testType == TestType.FIELD || testType == TestType.NODE_TREE) {
            if (fieldName == null)        throw new UnpreparedTestException("field name must be set");
        }
        if (testType == TestType.MAIN) {
            if (methodArgs == null)       throw new UnpreparedTestException("method args must be set");
        }

        logger.fine(String.format("Building test for %s.%s", className, methodName));
        if (testType != TestType.STYLE) {
            if (!initialized) buildStepOne();
            buildStepTwo();
        }
        if (description == null) description = descriptionSB.toString();
        if (note != null)        description = note + "\n\n" + description;

        Test test = new Test(name, className, expectClass, actualClass, constructorArgs, fieldName,
                expectClassInstance, actualClassInstance, methodName, methodArgs, exceptionType,
                exceptionMessage, description, hint, testType, maxScore, failedMessage,
                seed, seedIsSet, stdInput, iterableOrderMatters);

        factory.addTest(test);
        logger.finer("Test built and added to factory.");
    }

    Object getExpectClassInstance() { return expectClassInstance; }
    Object getActualClassInstance() { return actualClassInstance; }
    void setExpectClassInstance(Object newInstance) {
        System.err.println("Replacing instance with " + newInstance);
        expectClassInstance = newInstance;
        expectClass = newInstance.getClass();
        className = expectClass.getSimpleName();
    }
    void setActualClassInstance(Object newInstance) {
        actualClassInstance = newInstance;
        if (newInstance == null) {
            setupSB.append("Test failed at this step of setup. See message below for more info\n");
            setFailed("\nField instance is null. Did you initialize it correctly?");
        } else {
            actualClass = newInstance.getClass();
            className = actualClass.getSimpleName();
        }
    }

    void setInstanceToIterator() {
        if (seedIsSet()) StdRandom.setSeed(seed + seedCounter);
        setExpectClassInstance(((Iterable<?>)expectClassInstance).iterator());
        if (seedIsSet()) StdRandom.setSeed(seed + seedCounter++);
        try {
            setActualClassInstance(((Iterable<?>)actualClassInstance).iterator());
        } catch (Exception e) {
            setupSB.append("Test failed at this step of setup. See message below for more info\n");
            setFailed("\n" + e.toString());
        }
    }

    void setFailed(String failedMessage) {
        testType = TestType.FAILED;
        this.failedMessage = failedMessage;
    }

    boolean hasFailed()  { return testType == TestType.FAILED; }
    boolean seedIsSet()  { return seedIsSet; }

    /* ************************************************************************************** */
    /* Helper methods */
    /* ************************************************************************************** */

    private TestBuilder styleTest(String className, double value) {
        testType = TestType.STYLE;
        this.className = className;
        methodName = "Checkstyle";
        name = "Checkstyle " + className;
        description = "Checkstyle " + className;
        hint = "";
        maxScore = value;
        return this;
    }

    private TestBuilder fileExistenceTest(String fileName) {
        testType = TestType.FILE_EXISTS;
        this.className = fileName;
        methodName = "File Existence";
        name = "File Existence (" + fileName + ")";
        description = "File Existence (" + fileName + ")";
        hint = "";
        maxScore = 0.01;
        return this;
    }

    private void setName(String name)           { this.name = name;           }
    private void setMaxScore(double maxScore)   { this.maxScore = maxScore;   }
    private void setTestType(TestType testType) { this.testType = testType;   }
    private void setClassName(String className) { this.className = className; }
    private void setSeed(long seed) {
        this.seed = seed;
        this.seedIsSet = true;
    }

    private void iterableOrderDoesNotMatter() {
        this.iterableOrderMatters = false;
    }

    private void buildStepOne() {
        if (testType == TestType.FILE_EXISTS) return;
        expectClass = TestUtils.getExpectClass(className);
        actualClass = TestUtils.getActualClass(className);
        if (actualClass == null) {
            descriptionSB.append("\nERR: Test failed due to a problem with the .class file\n");
            setFailed(TestUtils.getCompileError(className));
        }
        if (constructorArgs != null && constructorArgs.length > 0) {
            for (int i = 0; i < constructorArgs.length; i++) {
                if (constructorArgs[i] != null && constructorArgs[i].getClass().isAssignableFrom(ArgBuilder.class)) {
                    ArgBuilder currArg = (ArgBuilder) constructorArgs[i];
                    if (currArg.failed()) {
                        setFailed(currArg.getFailedMessage());
                    }
                }
            }
        }
        if (testType == TestType.FAILED || testType == TestType.EXCEPTION_CONSTRUCTOR
                || testType == TestType.MAIN || testType == TestType.STYLE) {
            // Don't need class instance
            return;
        }
        descriptionSB.append("Calling the <").append(className).append("> constructor");
        descriptionSB.append(" ").append(TestUtils.argArray2String(constructorArgs)).append("\n");
        if (seedIsSet()) StdRandom.setSeed(seed + seedCounter);
        if (stdInput != null) TestUtils.setIn(stdInput);
        Object[] expectArgs = TestUtils.parseExpectArgs(constructorArgs);
        Object[] actualArgs = TestUtils.parseActualArgs(constructorArgs);
        expectClassInstance = TestUtils.getClassInstance(expectClass, expectArgs);
        if (seedIsSet()) StdRandom.setSeed(seed + seedCounter++);
        if (stdInput != null) TestUtils.setIn(stdInput);
        try {
            actualClassInstance = TestUtils.getClassInstance(actualClass, actualArgs);
        } catch (Exception e) {
            e.printStackTrace();
            StringBuilder limitedTrace = new StringBuilder();
            for (StackTraceElement element : e.getStackTrace()) {
                String line = element.toString();
                if (line.contains("reflect")) break;
                else limitedTrace.append("  > ").append(line).append("\n");
            }
            System.err.println(limitedTrace.toString());
            String msg = String.format("\n%s was thrown, caused by:\n%s",
                    e.toString(), limitedTrace.toString());
            descriptionSB.append("\nERR: Test failed due to problem instantiating class\n");
            setFailed(msg);
        } catch (OutOfMemoryError e) {
            TestUtils.handleOutOfMemory(e);
            setFailed("JVM Out of memory. Attempting to dump standard out and resume test");
        }
    }

    private void buildStepTwo() {
        if (testType == TestType.FIELD) {
            descriptionSB.append("Then checking the value of <").append(fieldName).append(">\n");
        }
        if (testType == TestType.NODE_TREE) {
            descriptionSB.append("Then checking all values in the subtree rooted at <").append(fieldName).append(">\n");
        }
        if (testType == TestType.RETURN || testType == TestType.STDOUT) {
            boolean rtn = testType == TestType.RETURN;
            descriptionSB.append("Calling <").append(methodName).append("> ");
            descriptionSB.append(TestUtils.argArray2String(methodArgs)).append("\n");
            descriptionSB.append("Then checking ");
            descriptionSB.append(rtn ? "the returned value\n" : "standard output\n");
        }
        if (testType == TestType.EXCEPTION_METHOD) {
            descriptionSB.append("Calling <").append(methodName).append("> ");
            descriptionSB.append(TestUtils.argArray2String(methodArgs)).append("\n");
            descriptionSB.append("Then checking that a(n) ").append(exceptionType.getSimpleName());
            descriptionSB.append("\n is thrown with the message: \"").append(exceptionMessage);
            descriptionSB.append("\"\n");
        }
        if (testType == TestType.ITERATOR) {
            descriptionSB.append("Then comparing its iterator against the solution's iterator\n");
        }
        if (testType == TestType.MAIN) {
            descriptionSB.append("Calling the main method of <").append(className).append(">\n");
            descriptionSB.append(TestUtils.argArray2String(methodArgs)).append("\n");
            descriptionSB.append("Then checking standard output against the solution\n");
        }
        if (testType == TestType.EXCEPTION_CONSTRUCTOR) {
            descriptionSB.append("Calling the <").append(className).append("> constructor");
            descriptionSB.append(" ").append(TestUtils.argArray2String(constructorArgs)).append("\n");
            descriptionSB.append("Then checking that a(n) ").append(exceptionType.getSimpleName());
            descriptionSB.append("\n is thrown with the message: \"").append(exceptionMessage);
            descriptionSB.append("\"\n");
        }
    }

    /* ************************************************************************************** */
    /* PreparedTest builder methods */
    /* ************************************************************************************** */

    public PreparedTest usingStdInput(String stdInput) {
        descriptionSB.append(String.format("Using \"%s\" as standard input\n", stdInput));
        this.stdInput = stdInput;
        return this;
    }

    @Override
    public PreparedTest withConstructorArgs(Object... args) {
        constructorArgs = args;
        return this;
    }

    @Override
    public PreparedTest forField(String fieldName) {
        this.fieldName = fieldName;
        return this;
    }

    @Override
    public PreparedTest forMethod(String methodName) {
        this.methodName = methodName;
        return this;
    }

    @Override
    public PreparedTest withMethodArgs(Object... args) {
        methodArgs = args;
        return this;
    }

    @Override
    public PreparedTest withDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public PreparedTest addNote(String note) {
        this.note = note;
        return this;
    }

    @Override
    public PreparedTest withHint(String hint) {
        this.hint = "\n"+hint+"\n";
        return this;
    }

    @Override
    public ExceptionMessage forExceptionType(Class<? extends Throwable> type) {
        this.exceptionType = type;
        return this;
    }

    @Override
    public PreparedTest withExceptionMessage(String message) {
        this.exceptionMessage = message;
        return this;
    }

    /* ************************************************************************************** */
    /* Used in TestFactory.TestSetup                                                          */
    /* ************************************************************************************** */
    void callMethod(String methodName) {
        tempMethodName = methodName;
    }

    void withArgs(Object... args) {
        Object[] expectArgs = new Object[args.length];
        Object[] actualArgs = new Object[args.length];
        if (args.length == 1 && args[0] instanceof ArgObject) {
            expectArgs[0] = ((ArgObject)args[0]).getExpectInstance();
            actualArgs[0] = ((ArgObject)args[0]).getActualInstance();
        } else {
            System.arraycopy(args, 0, expectArgs, 0, args.length);
            System.arraycopy(args, 0, actualArgs, 0, args.length);
        }
        Method expect = TestUtils.getMethod(expectClass, tempMethodName, expectArgs);
        Method actual = TestUtils.getMethod(actualClass, tempMethodName, actualArgs);
        assert expect != null;
        assert actual != null;
        if (seedIsSet()) StdRandom.setSeed(seed + seedCounter);
        TestUtils.callMethodAndReturn(expectClassInstance, expect, expectArgs);
        if (seedIsSet()) StdRandom.setSeed(seed + seedCounter++);
        try {
            TestUtils.callMethodAndReturn(actualClassInstance, actual, actualArgs);
        } catch (Exception e) {
            setupSB.append("Test failed at this step of setup. See message below for more info\n");
            setFailed("\n" + e.toString());
        }
    }

    void setFieldValue(String fieldName, Object value) {
        if (hasFailed()) return;
        if (className.contains("Node")) {
            setupSB.append("(").append(value).append(")");
        } else {
            setupSB.append("   Setting <").append(className).append(".").append(fieldName);
            setupSB.append("> to <").append(value).append(">\n");
        }
        TestUtils.setFieldValue(expectClassInstance, fieldName, value);
        if (actualClassInstance != null) {
            TestUtils.setFieldValue(actualClassInstance, fieldName, value);
        }
    }

    /* ****************************************************************************************** */
    /* Two-step test creation builder methods (TestStartStep and TestFinishStep */
    /* ****************************************************************************************** */
    static TwoStepTestStart twoStepTestStart() {
        return twoStepTestStart(null);
    }
    static TwoStepTestStart twoStepTestStart(Double maxScore) {
        startedTest = (TestBuilder) preparedTest(maxScore);
        return startedTest;
    }

    @Override
    public TwoStepTestStart constructorArgs(Object... constructorArgs) {
        return (TwoStepTestStart) withConstructorArgs(constructorArgs);
    }

    @Override
    public TestFactory.TestSetup initialize() {
        buildStepOne();
        initialized = true;
        return TestFactory.setup(this, setupSB);
    }

    static TwoStepTestFinish twoStepTestFinish() {
        return new TestBuilder(startedTest);
    }

    @Override
    public TwoStepTestFinish field(String fieldName) {
        return (TwoStepTestFinish) forField(fieldName);
    }

    @Override
    public TwoStepTestFinish description(String description) {
        return (TwoStepTestFinish) withDescription(description);
    }

    @Override
    public TwoStepTestFinish note(String note) {
        return (TwoStepTestFinish) addNote(note);
    }

    @Override
    public TwoStepTestFinish method(String methodName) {
        return (TwoStepTestFinish) forMethod(methodName);
    }

    @Override
    public TwoStepTestFinish methodArgs(Object... methodArgs) {
        return (TwoStepTestFinish) withMethodArgs(methodArgs);
    }

    @Override
    public TwoStepTestFinish hint(String hint) {
        return (TwoStepTestFinish) withHint(hint);
    }
}
