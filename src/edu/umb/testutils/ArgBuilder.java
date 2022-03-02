package edu.umb.testutils;

import edu.umb.testutils.BuilderFaces.*;

public class ArgBuilder implements ArgClass, ArgArgs, ArgObject {
    private Class<?> expectClazz;           // The solution's class to be instantiated
    private Class<?> actualClazz;           // The submission class to be instantiated
    private Object expectClassInstance;     // The solution's instance
    private Object actualClassInstance;     // The submission instance
    private Object[] args;                  // The constructor args used to create this object
    private String className;               // The name of the class being used as an argument
    private StringBuilder sb;               // Used to produce a description of resulting ArgObject
    private boolean failed;                 // Used to notify TestBuilder that something went wrong
    private String failedMessage;           // A message about what went wrong

    private ArgBuilder() {
        sb = new StringBuilder();
    }

    static ArgClass createObjectArg() { return new ArgBuilder(); }

    static ArgObject createObjectArg(Object expectArg, Object actualArg) {
        return new ArgBuilder().setup(expectArg, actualArg);
    }

    private ArgObject setup(Object expectArg, Object actualArg) {
        this.expectClazz = expectArg.getClass();
        this.actualClazz = actualArg == null ? null : actualArg.getClass();
        this.expectClassInstance = expectArg;
        this.actualClassInstance = actualArg;
        this.className = expectClazz.getSimpleName();
        return this;
    }

    @Override
    public ArgArgs ofClass(String className) {
        this.className = className;
        if (className.contains("Node")) {
            sb.append(className.substring(className.lastIndexOf('$')));
        } else {
            sb.append("Creating argument of class <").append(className).append("> ");
        }
        expectClazz = TestUtils.getExpectClass(className);
        actualClazz = TestUtils.getActualClass(className);
//        assert expectClazz != null;
//        assert actualClazz != null;
//        System.err.printf("actualClazz == null?: %s\n", actualClazz == null);
        return this;
    }

    public boolean failed() { return failed; }
    public String getFailedMessage() { return failedMessage; }

    @Override
    public ArgObject withArgs(Object... args) {
        this.args = args;
        String argString = TestUtils.argArray2String(args);
        if (!(className.contains("Node") && argString.contains("without arguments"))) {
            sb.append(argString).append("\n");
        }
//        Object[] expectClassArgs = new Object[args.length];
//        Object[] actualClassArgs = new Object[args.length];
//        System.arraycopy(args, 0, expectClassArgs, 0, args.length);
//        System.arraycopy(args, 0, actualClassArgs, 0, args.length);
        Object[] expectArgs = TestUtils.parseExpectArgs(args);
        Object[] actualArgs = TestUtils.parseActualArgs(args);
        expectClassInstance = TestUtils.getClassInstance(expectClazz, expectArgs);
        if (actualClazz != null) {
            try {
                actualClassInstance = TestUtils.getClassInstance(actualClazz, actualArgs);
            } catch (Exception e) {
                StringBuilder limitedTrace = new StringBuilder();
                for (StackTraceElement element : e.getStackTrace()) {
                    String line = element.toString();
                    if (line.contains("reflect")) break;
                    else limitedTrace.append("  > ").append(line).append("\n");
                }
                System.err.println(limitedTrace.toString());
                String msg = String.format("\nTest failed because an exception was thrown while instantiating %s.\n" +
                        "Exception: %s\nLimited stack trace:\n%s", className, e.toString(), limitedTrace.toString());
                setFailed(msg);
            }
        } else {
            setFailed("Test failed because " + className + " could not be instantiated.");
        }
        return this;
    }

    private void setFailed(String message) {
        this.failed = true;
        this.failedMessage = message;
    }

    @Override
    public Object getExpectInstance() { return expectClassInstance; }
    @Override
    public Object getActualInstance() { return actualClassInstance; }

    @Override
    public Class<?> getExpectClass() {
        return expectClassInstance == null ? null : expectClassInstance.getClass();
    }

    @Override
    public Class<?> getActualClassOrElseObject() {
        return actualClassInstance == null ? Object.class : actualClassInstance.getClass();
    }

    @Override
    public Object getExpectFieldValue(String fieldName) {
        return TestUtils.getFieldValue(expectClassInstance, fieldName,
                "Error getting expect field <" + fieldName + ">. ");
    }

    @Override
    public Object getActualFieldValue(String fieldName) {
        return TestUtils.getFieldValue(actualClassInstance, fieldName,
                "Error getting actual field <" + fieldName + ">. ");
    }

    @Override
    public String getDescription() {
        return sb.toString();
    }

    @Override
    public void setFieldValue(String fieldName, ArgObject value) {
        if (className.contains("Node")) {
            if (!fieldName.contains("next")) sb.append("(").append(value).append(")");
        } else {
            sb.append("Setting <").append(className).append(".").append(fieldName);
            sb.append("> to <").append(value).append(">\n");
        }
        TestUtils.setFieldValue(expectClassInstance, fieldName, value == null ? null : value.getExpectInstance());
        if (actualClassInstance != null) {
            TestUtils.setFieldValue(actualClassInstance, fieldName, value == null ? null : value.getActualInstance());
        }
    }

    @Override
    public void setFieldValue(String fieldName, Object value) {
        if (className.contains("Node")) {
            sb.append("(").append(value).append(")");
        } else {
            sb.append("Setting <").append(className).append(".").append(fieldName);
            sb.append("> to <").append(value).append(">\n");
        }
        TestUtils.setFieldValue(expectClassInstance, fieldName, value);
        if (actualClassInstance != null) {
            TestUtils.setFieldValue(actualClassInstance, fieldName, value);
        }
    }

    @Override
    public TestFactory.ArgSetup initialize() {
        return TestFactory.setup(this);
    }

    public String toString() {
        return String.format("%s(%s)", className, TestUtils.argArray2String(args));
    }
}
