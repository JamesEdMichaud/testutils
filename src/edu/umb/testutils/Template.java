package edu.umb.testutils;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import stdlib.StdRandom;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.logging.Logger;

@ExtendWith(TestListener.class)
public class Template {
    private static final Logger logger = Logger.getLogger(Template.class.getName());

    static {
        logger.info(String.format("Template Logger online. level %s. Parent %s, level: %s",
                logger.getLevel(), logger.getParent(), logger.getParent().getLevel()));
    }

    @TestTemplate
    @ExtendWith(TemplateInvoker.class)
    void startTest(Test test) {
        String messageToListener = String.format("%s", test.getNumber());
        TestListener.receiveMessage(messageToListener);
        String methodName = test.getMethodName();

//        System.err.printf("Starting test %d on %s\n", test.getNumber(), methodName);
        logger.fine(String.format("Starting test %d on %s", test.getNumber(),
                methodName == null ? test.getTestType() : methodName));

        int seedMod = StdRandom.uniform(1, 100000);
        if (test.getTestType() == TestType.FAILED) {
            fail(test.getFailedMessage());
        } else if (test.getTestType() == TestType.STYLE) {
            TestUtils.testStyle(test.getClassName());
        } else if (test.getTestType() == TestType.FILE_EXISTS) {
            TestUtils.testFileExistence(test.getClassName());
        } else if (test.getTestType() == TestType.EXCEPTION_CONSTRUCTOR) {
            TestUtils.checkCornerCase(test.getExceptionType(),
                    () -> TestUtils.getClassInstanceForTest(
                            test.getActualClass(),
                            TestUtils.parseActualArgs(test.getConstructorArgs())),
                    test.getHint(), test.getExceptionMessage());
        } else if (test.getTestType() == TestType.MAIN) {
            String[] eArgs;
            String[] aArgs;
            if (test.getMethodArgs() == null || test.getMethodArgs().length == 0) {
                eArgs = new String[0];
                aArgs = new String[0];
            } else {
                String[] args = (String[]) test.getMethodArgs()[0];
                eArgs = new String[args.length];
                aArgs = new String[args.length];
                for (int i = 0; i < args.length; i++) {
                    eArgs[i] = args[i];
                    aArgs[i] = args[i];
                }
            }
            Method expectMethod = TestUtils.getMethod(test.getExpectClass(), "main", (Object) eArgs);
            Method actualMethod = TestUtils.getMethod(test.getActualClass(), "main", (Object) aArgs);
            assert expectMethod != null;
            assert actualMethod != null;
            if (test.seedIsSet()) StdRandom.setSeed(test.getSeed() + seedMod);
            if (test.getStdInput() != null) TestUtils.setIn(test.getStdInput());
            String expect = TestUtils.callMethodCaptureStdOut(null, expectMethod, (Object) eArgs);
            if (test.seedIsSet()) StdRandom.setSeed(test.getSeed() + seedMod);
            if (test.getStdInput() != null) TestUtils.setIn(test.getStdInput());
            String actual = TestUtils.callMethodCaptureStdOut(null, actualMethod, (Object) aArgs);
            if (expect.length() < 500) test.addResults(expect, actual);
            else test.addResults("results omitted", "to save space");
            TestUtils.compareStdOut(expect, actual, test.getHint());
        } else {
            Object expectObj = test.getExpectClassInstance();
            Object actualObj = test.getActualClassInstance();
            if (test.getTestType() == TestType.FIELD) {
                Object expect = TestUtils.getFieldValue(expectObj, test.getFieldName(), "Autograder error");
                Object actual = TestUtils.getFieldValue(actualObj, test.getFieldName(), "");
                test.addResults(expect, actual);
                TestUtils.compareValues(expect, actual, test.getHint());
            } else if (test.getTestType() == TestType.NODE_TREE) {
                Object expect = TestUtils.getFieldNode(expectObj, test.getFieldName(), "Autograder error");
                Object actual = TestUtils.getFieldNode(actualObj, test.getFieldName(), "");
                TestUtils.compareFields(expect, actual, test.getHint());
            } else if (test.getTestType() == TestType.ITERATOR) {
                logger.finer("Iterator test.");
                if (actualObj instanceof Iterable) {
                    if (test.seedIsSet()) StdRandom.setSeed(test.getSeed() + seedMod);
                    if (test.getStdInput() != null) TestUtils.setIn(test.getStdInput());
                    Iterator<?> expectIter = ((Iterable<?>) expectObj).iterator();
                    if (test.seedIsSet()) StdRandom.setSeed(test.getSeed() + seedMod);
                    if (test.getStdInput() != null) TestUtils.setIn(test.getStdInput());
                    logger.finer("Calling actualObj.iterator()");
                    Iterator<?> actualIter = ((Iterable<?>) actualObj).iterator();
                    TestUtils.compareIterators(expectIter, actualIter, test.getHint());
                } else {
                    fail(String.format("%s does not implement the Iterator interface",
                            actualObj.getClass().getSimpleName()));
                }
            } else {
                Object[] expectArgs = TestUtils.parseExpectArgs(test.getMethodArgs());
                Object[] actualArgs = TestUtils.parseActualArgs(test.getMethodArgs());
                Method expectMethod = TestUtils.getMethod(test.getExpectClass(), methodName, expectArgs);
                Method actualMethod = TestUtils.getMethod(test.getActualClass(), methodName, actualArgs);
                assert expectMethod != null;
                assert actualMethod != null;

                if (test.getTestType() == TestType.STDOUT) {
                    if (test.seedIsSet()) StdRandom.setSeed(test.getSeed() + seedMod);
                    if (test.getStdInput() != null) TestUtils.setIn(test.getStdInput());
                    String expect = TestUtils.callMethodCaptureStdOut(expectObj, expectMethod, expectArgs);
                    if (test.seedIsSet()) StdRandom.setSeed(test.getSeed() + seedMod);
                    if (test.getStdInput() != null) TestUtils.setIn(test.getStdInput());
                    String actual = TestUtils.callMethodCaptureStdOut(actualObj, actualMethod, actualArgs);
                    if (expect.length() < 100) test.addResults(expect, actual);
                    else test.addResults("results omitted", "to save space");
                    TestUtils.compareStdOut(expect, actual, test.getHint());
                } else if (test.getTestType() == TestType.RETURN) {
                    if (test.seedIsSet()) StdRandom.setSeed(test.getSeed() + seedMod);
                    if (test.getStdInput() != null) TestUtils.setIn(test.getStdInput());
                    Object expect = TestUtils.callMethodAndReturn(expectObj, expectMethod, expectArgs);
                    if (test.seedIsSet()) StdRandom.setSeed(test.getSeed() + seedMod);
                    if (test.getStdInput() != null) TestUtils.setIn(test.getStdInput());
                    Object actual = null;
                    try {
                        actual = TestUtils.callMethodAndReturn(actualObj, actualMethod, actualArgs);
                    } catch (Exception e) {
                        String msg = String.format("\nAn exception was thrown during while calling <%s>\n" +
                                        "%s\n%s", methodName, e, test.getHint());
                        fail(msg);
                    }
                    test.addResults(expect, actual);
                    if (!test.iterableOrderMatters()) {
                        TestUtils.compareValues(expect, actual, test.getHint(), false);
                    } else {
                        TestUtils.compareValues(expect, actual, test.getHint());
                    }
                } else if (test.getTestType() == TestType.EXCEPTION_METHOD) {
                        TestUtils.checkCornerCase(test.getExceptionType(), () ->
                                        TestUtils.callMethodAndReturn(test.getActualClassInstance(),
                                                actualMethod, actualArgs),
                                test.getHint(), test.getExceptionMessage());
                }
            }
        }
    }
}
