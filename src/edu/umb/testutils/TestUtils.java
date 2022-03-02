package edu.umb.testutils;

import dsa.BasicST;
import dsa.OrderedST;
import dsa.RectHV;
import org.junit.jupiter.api.function.Executable;
import stdlib.In;
import stdlib.StdIn;
import stdlib.StdOut;

import java.io.*;
import java.lang.reflect.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.time.Duration;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public final class TestUtils {
    private static final URLClassLoader expectLoader;
    private static final URLClassLoader actualLoader;
    private static final String srcPath;
    private static final int timeout;

    private static ByteArrayOutputStream OUT;
    private static HashMap<Class<?>, Class<?>> primitivesMap;
    private static ByteArrayOutputStream tempOut;
    private static String packageName = "";
    private static final Logger logger = Logger.getLogger(TestUtils.class.getName());
    private static double maxTestScore;

    static {
        srcPath = "/autograder/submission/";
        timeout = 15;       // seconds
        maxTestScore = 60;  // Autograder: 60, report/code: 40
        URL expectOut;
        URL actualOut;
        try {
            expectOut = new File("expectOut/").toURI().toURL();
            actualOut = new File("actualOut/").toURI().toURL();
        } catch (MalformedURLException e) {
            // TODO: Cause this exception to be thrown, then build a handler method
            e.printStackTrace();
            expectOut = null;
            actualOut = null;
        }
        expectLoader = new URLClassLoader(new URL[]{expectOut},
                Thread.currentThread().getContextClassLoader());
        actualLoader = new URLClassLoader(new URL[]{actualOut},
                Thread.currentThread().getContextClassLoader());
        setUpPrimitivesMap();
        OUT = new ByteArrayOutputStream();
        System.setOut(new PrintStream(OUT));
        StdOut.resync();
        logger.info(String.format("TestUtils Logger online. level %s. Parent %s, level: %s",
                logger.getLevel(), logger.getParent(), logger.getParent().getLevel()));
    }

    private TestUtils() { /* no-op */ }

    public static void setPackageName(String newPackageName) {
        packageName = newPackageName.length() > 0 ? newPackageName+"." : newPackageName;
    }
    public static String getPackageName() { return packageName; }

    public static double getMaxTestScore() { return maxTestScore; }
    public static void setMaxTestScore(double newMax) {
        if (newMax < 0) {
            System.err.printf("Error setting max test score (%.2f). 0 <= newMax <= 100", newMax);
        }
        maxTestScore = newMax;
    }

    /* ---------------------------------------------------------------------- */
    /* ---------------------- New Helper Methods ---------------------------- */
    /* ---------------------------------------------------------------------- */

    static void testStyle(String className) {
        String fileName = className + (className.contains(".java") ? "" : ".java");
        // Set up the process and evaluation strings.
        ProcessBuilder pb = new ProcessBuilder();
        String cmd =  "check_style " + srcPath + fileName;
        pb.command("bash", "-c", cmd);
        String line;
        String prev = "";
        int lineCounter = 0;
        StringBuilder message = new StringBuilder();
        try {
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while((line = reader.readLine()) != null) {
                lineCounter++;
                // Keep track of prev to test "Audit done." at end.
                prev = line;
                // Also build the full output in case of exception.
                message.append(line).append("\n");
            }
        } catch(Exception e) {
            message.append(e.getMessage());
        }
        // if the last line is "Audit done.", style checker passes.
        String expect = "Audit done.";
        String actual = lineCounter < 4 ? prev : "See warnings for details.";
        assertEquals(expect, actual, message.toString());
    }

    static void testFileExistence(String fileName) {
        ProcessBuilder pb = new ProcessBuilder();
        String cmd =  "if test -f " +srcPath+fileName+ "; then echo \"exists\"; else echo \"missing\"; fi";
        pb.command("bash", "-c", cmd);
        try {
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            assertEquals("exists", line, fileName + " missing\n");
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    static Class<?> getExpectClass(String className) { return loadClass(className, expectLoader); }
    static Class<?> getActualClass(String className) { return loadClass(className, actualLoader); }

    static Class<?> loadClass(String className, URLClassLoader loader) {
        logger.finest(String.format("Loading %s using loader %s", className, loader));
        try {
            return loader.loadClass(packageName+className);
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            logger.warning(String.format("Loading class %s failed. Please debug me", className));
//            e.printStackTrace();
            return null;
        }
    }

    static String getCompileError(String className) {
        String msg;
        try {
            In in = new In("actualOut/"+className+".java.err");
            String msg1 = "Missing java file or compile error. "
                    + "Make sure your code compiles before uploading";
            msg = String.format("\n%s\nFirst 3 lines of error:\n%s\n%s\n%s\n",
                    msg1,
                    Objects.requireNonNullElse(in.readLine(), ""),
                    Objects.requireNonNullElse(in.readLine(), ""),
                    Objects.requireNonNullElse(in.readLine(), ""));
        } catch (Exception ex) {
            ex.printStackTrace();
            msg = "\nSomething went wrong during compilation, but a compile error was not saved.\n"
                    + "Please report this to the professor or a TA.";
        }
        return msg;
    }

    static String getInstantiateError(String className) {
        return "Error pending.";
    }

    static Object getClassInstance(Class<?> clazz, Object... args) {
        logger.finest(String.format("Getting instance of %s with constructor args %s",
                clazz.getSimpleName(), Arrays.toString(args)));
        Constructor<?> constructor = getConstructor(clazz, args);
        assert constructor != null;
        try {
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        } catch (Exception e) {
//            System.err.println("getClassInstance getRootCause(e).getMessage(): " + getRootCause(e).getMessage());
//            System.err.println("getRootCause(e): " + getRootCause(e).toString());
            throw (RuntimeException) getRootCause(e);
        }
    }

    static Object getClassInstanceForTest(Class<?> clazz, Object... args) {
        final Object[] answer = {null};
//        System.err.println("In getClassInstanceForTest");
        Constructor<?> constructor = getConstructor(clazz, args);
        assert constructor != null;
        assertTimeoutPreemptively(Duration.ofSeconds(timeout), () -> {
            String msg;
            try {
                constructor.setAccessible(true);
                answer[0] = constructor.newInstance(args);
            } catch (InvocationTargetException e) {
                throw (RuntimeException) getRootCause(e);
            } catch (IllegalArgumentException e) {
                msg = "Could not create class instance (IllegalArgumentException)\n" +
                        "Please report this error to the professor or TA\n";
                fail(msg);
            } catch (InstantiationException | IllegalAccessException e) {
                msg = "An unexpected exception was thrown. Please report this error to " +
                        "the professor or TA\n";
                fail(msg);
            } catch (OutOfMemoryError e) {
                handleOutOfMemory(e);
                fail("JVM Out of memory. Attempting to dump standard out and resume test");
            }
        }, "__TIMEOUT__\n");
        return answer[0];
    }

    private static Constructor<?> getConstructor(Class<?> clazz, Object... args) {
        if (clazz == null) return null;
        if (args == null) args = new Object[0];
        Class<?>[] argTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            argTypes[i] = args[i] == null ? null : args[i].getClass();
        }
//        if (Modifier.isStatic(clazz.getModifiers()) && argTypes.length > 0 && argTypes[0] != null
//                && clazz.getName().contains(argTypes[0].getSimpleName()+"$")) {
//            System.err.printf("clazzName: %s, arg0Name: %s\n", clazz.getName(), argTypes[0].getSimpleName());
//            argTypes = Arrays.copyOfRange(argTypes, 1, argTypes.length);
//        }
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            Class<?>[] constructorParamTypes =  constructor.getParameterTypes();
            if (constructor.getParameterTypes().length == argTypes.length) {
                boolean foundMatch = true;
                for (int i = 0; i < argTypes.length; i++) {
                    if (!typesMatch(constructorParamTypes[i], argTypes[i])) {
//                        if (clazz.getName().contains("SearchNode")) {
//                            System.err.printf("%s does not match %s\n", constructorParamTypes[i], argTypes[i]);
//                        }
                        foundMatch = false;
                        break;
                    }
                }
                if (foundMatch) return constructor;
            }
        }
        // Then relax the requirement and allow subclass matches
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            Class<?>[] constructorParamTypes =  constructor.getParameterTypes();
            System.err.println(String.format("Checking %s vs %s",
                    Arrays.toString(constructorParamTypes), Arrays.toString(argTypes)));
            if (constructor.getParameterTypes().length == argTypes.length) {
                boolean foundMatch = true;
                for (int i = 0; i < argTypes.length; i++) {
                    // TODO: Find a way to make this check more stable
                    if (!(typesLooselyMatch(constructorParamTypes[i], argTypes[i]))) {
                        System.err.println(String.format("%s doesn't match %s\n",
                                constructorParamTypes[i], argTypes[i]));
                        foundMatch = false;
                        break;
                    }
                }
                if (foundMatch) return constructor;
            }
        }
        fail("\n No suitable constructor found\n");
        return null;
    }

    static Object[] parseExpectArgs(Object... args) {
        return parseArgs(args, true);
    }

    static Object[] parseActualArgs(Object... args) {
        return parseArgs(args, false);
    }

    static Object[] parseArgs(Object[] args, boolean isExpect) {
        if (args != null && args.length > 0) {
            Object[] parsed = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null && args[i].getClass() == ArgBuilder.class) {
                    if (isExpect) parsed[i] = ((ArgBuilder)args[i]).getExpectInstance();
                    else          parsed[i] = ((ArgBuilder)args[i]).getActualInstance();
                } else {
                    parsed[i] = args[i];
                }
            }
            return parsed;
        }
        return args;
    }

    static Method getMethod(Class<?> clazz, String methodName, Object... args) {
        Class<?>[] parameterTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = args[i] == null ? null : args[i].getClass();
        }

        for (Method method : clazz.getDeclaredMethods()) {
            Class<?>[] methodParamTypes =  method.getParameterTypes();
            if (method.getName().equals(methodName) && methodParamTypes.length == parameterTypes.length) {
                method.setAccessible(true);
                boolean foundMatch = true;
                for (int i = 0; i < parameterTypes.length; i++) {
                    if (!typesMatch(methodParamTypes[i], parameterTypes[i])) {
                        foundMatch = false;
                        break;
                    }
                }
                if (foundMatch) return method;
            }
        }
        // Then relax the requirement and allow subclass matches
        for (Method method : clazz.getDeclaredMethods()) {
            Class<?>[] methodParamTypes =  method.getParameterTypes();
            if (method.getName().equals(methodName) && methodParamTypes.length == parameterTypes.length) {
                method.setAccessible(true);
                boolean foundMatch = true;
                for (int i = 0; i < parameterTypes.length; i++) {
                    // TODO: Find a way to make this check more stable
                    if (!(typesLooselyMatch(methodParamTypes[i], parameterTypes[i]))) {
                        foundMatch = false;
                        break;
                    }
                }
                if (foundMatch) return method;
            }
        }
        String msg = String.format("\n%s(%s) method not found\n",
                methodName, args2TypeString(args));
        fail(msg);
        return null;
    }

    private static String args2TypeString(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i].getClass()).append(i == args.length-1 ? "" : ", ");
        }
        return sb.toString();
    }

    static String callMethodCaptureStdOut(Object object, Method method, Object... args) {
        makeTempOut();
        assertTimeoutPreemptively(Duration.ofSeconds(timeout), () -> {
            try {
                method.invoke(object, args);
            } catch (IllegalAccessException e) {
                fail(String.format("\nMethod \"%s\" not accessible.\n", method.getName()));
            } catch (InvocationTargetException e) {
                throw (RuntimeException) getRootCause(e);
    //            fail(String.format("\nMethod \"%s\" threw an unexpected exception:\n%s\n",
    //                    method.getName(), getRootCause(e)));
            }
        }, "\n__TIMEOUT__\n");
        return getTempOut();
    }

    static Object callMethodAndReturn(Object object, Method method, Object... args) {
        final Object[] answer = {null};
        assertTimeoutPreemptively(Duration.ofSeconds(timeout), () -> {
            try {
                answer[0] = method.invoke(object, args);
            } catch (IllegalAccessException e) {
                fail(String.format("\nMethod \"%s\" not accessible.\n", method.getName()));
            } catch (InvocationTargetException e) {
                throw (RuntimeException) getRootCause(e);
//            fail(String.format("\nMethod \"%s\" threw an unexpected exception:\n%s\n",
//                    method.getName(), getRootCause(e)));
            } catch (OutOfMemoryError e) {
                handleOutOfMemory(e);
                fail("JVM Out of memory. Attempting to dump standard out and resume test");
            }
        }, "\n__TIMEOUT__\n");
        return answer[0];
    }

    static void handleOutOfMemory(OutOfMemoryError e) {
        System.out.close();
        OUT = new ByteArrayOutputStream();
        System.setOut(new PrintStream(OUT));
        StdOut.resync();
        String msg = "JVM Out of memory. Attempting to dump standard out and resume test";
        String output = "{" + String.join(",", new String[] {
                "\"score\": 0.0", "\"execution_time\": 0",
                String.format("\"tests\": [{\"name\": \"Error, out of memory\",\"score\": 0.0,"
                        + "\"visibility\": \"visible\",\"output\":\"%s\"}]", msg),
        }) + "}";

        try (PrintWriter tempOut = new PrintWriter("/autograder/results/results.json")) {
            tempOut.println(output);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static void setIn(String filename) {
        try {
            File file = new File(filename);
            System.setIn(new FileInputStream(file));
            StdIn.resync();
        } catch (FileNotFoundException e) {
//            System.err.println("Failed to load input from file. Using string as input");
            System.setIn(new ByteArrayInputStream(filename.getBytes()));
            StdIn.resync();
//            e.printStackTrace();
        }
    }

    static String obj2String(Object obj) {
        return    obj == null              ? "null"
                : obj.getClass().isArray() ? "[" + getArgString((Object[]) obj) + "]"
                : obj instanceof String    ? "\"" + obj + "\""
                : obj instanceof Character ? "'"  + obj + "'"
                : obj.toString();
    }

    static String obj2StringNoAdditions(Object obj) {
        return    obj == null              ? "null"
                : obj.getClass().isArray() ? "[" + getArgString(toObjectArray(obj)) + "]"
                : obj instanceof String    ? (String) obj
                : obj instanceof Character ? "'"  + obj + "'"
                : Iterable.class.isAssignableFrom(obj.getClass()) ?
                    "Iterable{\n" + iterable2String((Iterable<?>) obj) + "}"
                : obj.toString();
    }

    static Object[] toObjectArray(Object obj) {
        Object[] answer = new Object[Array.getLength(obj)];
        for (int i = 0; i < answer.length; i++) {
            answer[i] = Array.get(obj, i);
        }
        return answer;
    }

    static String argArray2String(Object[] array) {
        if (array != null && array.length > 0) {
            StringBuilder sb = new StringBuilder("with arguments:");
            String args = getArgString(array);
            if (args.length() < 45) sb.append(" (").append(args).append(")");
            else sb.append("\n  (").append(args).append(")");
            return sb.toString();
        } else {
            return "without arguments";
        }
    }

    static String iterable2String(Iterable<?> obj) {
        StringBuilder sb = new StringBuilder();
        for (Object o : obj) {
            sb.append("  ").append(obj2String(o)).append("\n");
        }
        return sb.toString();
    }

    static String getArgString(Object[] array) {
        StringBuilder sb = new StringBuilder();
        for (Object arg : array) {
            StringBuilder subSB = new StringBuilder();
            if (arg instanceof Object[]) {
                subSB.append("[").append(getArgString((Object[]) arg)).append("]");
            } else if (arg != null && arg.getClass().isArray()) {
                subSB.append("[").append(getArgString(prim2ObjArray(arg))).append("]");
            } else if (arg instanceof BuilderFaces.ArgObject) {
                subSB.append(getArgBuilderString(arg));
            } else {
                subSB.append(obj2String(arg));
            }
            int lastLineLength = subSB.length() - subSB.lastIndexOf("\n");
            subSB.append(lastLineLength > 55 ? ",\n  " : ", ");
            sb.append(subSB.length() > 600 ? "Argument omitted due to length, " : subSB.toString());
        }
        if (sb.length() > 1) {
            if (sb.charAt(sb.length()-3) == '\n') {
                sb.delete(sb.length()-4, sb.length());
            } else {
                sb.delete(sb.length()-2, sb.length());
            }
        }
        return sb.toString();
    }

    private static Object[] prim2ObjArray(Object arr) {
        List<Object> list = new ArrayList<Object>();
        if (arr instanceof int[]    ) for (int curr     : (int[])     arr) list.add(curr);
        if (arr instanceof double[] ) for (double curr  : (double[])  arr) list.add(curr);
        if (arr instanceof float[]  ) for (float curr   : (float[])   arr) list.add(curr);
        if (arr instanceof boolean[]) for (boolean curr : (boolean[]) arr) list.add(curr);
        if (arr instanceof long[]   ) for (long curr    : (long[])    arr) list.add(curr);
        if (arr instanceof byte[]   ) for (byte curr    : (byte[])    arr) list.add(curr);
        if (arr instanceof short[]  ) for (short curr   : (short[])   arr) list.add(curr);
        if (arr instanceof char[]   ) for (char curr    : (char[])    arr) list.add(curr);
        return list.toArray();
    }

    private static String getArgBuilderString(Object arg) {
        Object instance = ((ArgBuilder) arg).getExpectInstance();
        if (instance != null
                && instance.getClass().getSimpleName().contains("Node")
                && hasField(instance, "next")) {
            int count = 0;
            Object curr = instance;
            while ((curr = getFieldObject(curr, "next")) != null) {
                count++;
            }
            if (count > 10) {
                return String.format("Node list of %s values", count);
            } else {
                String valueVariable = hasField(instance, "item") ? "item"
                        : hasField(instance, "value") ? "value"
                        : hasField(instance, "val") ? "val" : null;
                if (valueVariable != null) {
                    StringBuilder sb = new StringBuilder("Node{");
                    curr = instance;
                    while (curr != null) {
                        sb.append(getFieldValue(curr, valueVariable, "error")).append("\u2b95 ");
                        curr = getFieldObject(curr, "next");
                    }
                    sb.delete(sb.length()-2, sb.length());
                    sb.append("}");
                    return sb.toString();
                } else {
                    return String.format("Node list of %s values", count);
                }
            }
        } else {
            assert instance != null;
            if (hasMethod(instance, "toString", (Class<?>[]) null)) {
                String str = instance.toString();
                str = str.contains("\n") ? "\n"+str+"\n" : str;
                return instance.getClass().getSimpleName() + "(" + str + ")";
            } else {
                if (instance instanceof Object[]) {
                    return "[" + getArgString((Object[]) instance) + "]";
                } else {
                    return instance.toString();
                }
            }
        }
    }

    static void checkCornerCase(Class<? extends Throwable> exceptionType,
                                Executable ex, String hint, String exceptionMessage) {
        String actualMessage = "blank";
        Throwable thrown = assertThrows(exceptionType, ex, hint);
        if (thrown != null) {
            actualMessage = thrown.getMessage();
        }
        assertEquals(exceptionMessage, actualMessage,
                "\n\nException message does not match the required string.\n" +
                        "Make sure the corner case check is the first operation of a method\n");
    }

    public static void makeTempOut() {
        tempOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(tempOut));
        StdOut.resync();
    }

    public static String getTempOut() {
        String output = tempOut.toString();
        System.out.close();
        System.setOut(new PrintStream(OUT));
        StdOut.resync();
        return output;
    }

    static boolean hasField(Object instance, String fieldName) {
        try {
            Field field = instance.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (field.get(instance) != null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return false;
        }
    }

    static boolean hasMethod(Object instance, String methodName,  Class<?>... parameterTypes) {
        try {
            Method method = instance.getClass().getDeclaredMethod(methodName, parameterTypes);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    static Object getFieldObject(Object instance, String fieldName) {
        try {
            Field field = instance.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(instance);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }

    static Object getFieldNode(Object instance, String fieldName, String msg) {
        return getFieldObject(instance, fieldName);
    }

    static Object getFieldValue(Object instance, String fieldName, String msg) {
        try {
//            System.err.printf("getting field: %s\n", fieldName);
            Field field = instance.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object gotten = field.get(instance);
            if (gotten != null && gotten.getClass().getSimpleName().contains("Node")) {
                String val;
                if (hasField(gotten, "item")) val = "item";
                else if (hasField(gotten, "value")) val = "value";
                else if (hasField(gotten, "val")) val = "val";
                else return gotten;
                return getFieldValue(gotten, val, msg);
            }
            return gotten;
        } catch (NoSuchFieldException e) {
            msg = String.format("\nField %s in %s not found. %s\n%s\n",
                    fieldName, instance.getClass(), "Do not change the name of any given fields", msg);
        } catch (IllegalAccessException e) {
            msg = String.format("IllegalAccessException trying to access field %s\n", fieldName);
        }
        fail(msg);
        throw new RuntimeException("Error getting field");
//        return null;
    }

    static void setFieldValue(Object instance, String fieldName, Object value) {
        try {
            Field field = instance.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(instance, value);
            return;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    static void checkFieldValue(Object expectObj, Object actualObj, String fieldName, String hint) {
        Object expect = getFieldValue(expectObj, fieldName, hint);
        Object actual = getFieldValue(actualObj, fieldName, hint);
        assertEquals(expect, actual, hint);
    }

    static void compareIterators(Iterator<?> expectIter, Iterator<?> actualIter, String hint) {
        StringBuilder expectSB = new StringBuilder();
        StringBuilder actualSB = new StringBuilder();

        logger.finer("Building expected iterator sequence");
        while (expectIter.hasNext()) {
            expectSB.append(expectIter.next()).append("\n");
        }
        try {
            logger.finer("Building actual iterator sequence");
            while (actualIter.hasNext()) {
                actualSB.append(actualIter.next()).append("\n");
            }
        } catch (OutOfMemoryError e) {
            fail("Out of memory. Your code may have an infinite loop");
        }
        String msg = String.format("\n%s%s\n%s",
                "Iterator produced unexpected sequence of values\n",
                "Values printed one per line, then compared against the solution\n", hint);
        logger.finer("Making comparison");
        TestUtils.compareStdOut(expectSB.toString(), actualSB.toString(), msg);

    }

    static void compareValues(Object expect, Object actual, String hint) {
        compareValues(expect, actual, hint, true);
    }

    static void compareValues(Object expect, Object actual, String hint, boolean iterableOrderMatters) {
        if (expect == null) {
            assertEquals(expect, actual, hint);
            return;
        }
        Class<?> expectClass = expect.getClass();
        Class<?> actualClass = actual.getClass();
        if (expectClass == String.class) {
            try {
                double expectD = Double.parseDouble((String) expect);
                double actualD = Double.parseDouble((String) actual);
//                System.err.println("Attempting to compare strings as doubles for class: " + expectClass);
                assertEquals(expectD, actualD, 0.005, hint);
                return;
            } catch (NumberFormatException e) {
                // do nothing
            }
        }
        if (expectClass == Double.class || expectClass == Float.class) {
            assertEquals((double)expect, (double)actual, 0.005, hint);

        } else if (isCommonType(expectClass)) {
            assertEquals(expect, actual, hint);

        } else if (expectClass.isArray()) {
            Object[] eArray = toObjectArray(expect);
            Object[] aArray = toObjectArray(actual);
            // Compare the string representations. If the elements are course class objects,
            // the solution object will not equal the submission object.
            if (eArray.length > 0 && eArray[0] != null
                    && hasMethod(eArray[0], "toString", (Class<?>[]) null)) {
                for (int i = 0; i < eArray.length; i++) {
                    String eString = eArray[i] == null ? "null" : eArray[i].toString();
                    String aString = aArray[i] == null ? "null" : aArray[i].toString();
                    assertEquals(eString, aString, String.format("\n" +
                            "Array contents differ at index [%d]\n%s\n", i, hint));
                }
            } else {
                assertArrayEquals((Object[]) expect, (Object[]) actual, hint);
            }

        // This next condition finds whether expect and actual have the same class name.
        // This happens when comparing a solution and student object - Example: class Rational
        } else if (!expectClass.equals(actualClass) &&
                expectClass.toString().equals(actualClass.toString())) {
            // We try to get the toString method, which is the best way to test equality
            try {
                expectClass.getDeclaredMethod("toString");
                hint = "\nThis test requires the toString method, so make sure it's working" + hint;
                assertEquals(expect.toString(), actual.toString(), hint);
            } catch (NoSuchMethodException e) {
                // If there's no defined toString method, check each of the object's fields
                compareFields(expect, actual, hint);
            }
        } else if (Iterable.class.isAssignableFrom(expectClass)) {
            if (iterableOrderMatters) {
                compareIterators(((Iterable<?>) expect).iterator(), ((Iterable<?>) actual).iterator(), hint);
            } else {
                LinkedList<String> expectList = iterableToStringList((Iterable<?>) expect);
                LinkedList<String> actualList = iterableToStringList((Iterable<?>) actual);
                String sideBySide = stackOutputSideBySide(expectList, actualList);
                while (!expectList.isEmpty()) {
                    String curr = expectList.removeFirst();
                    if (!actualList.remove(curr)) {
                        fail(String.format("Missing expected value:\n%s\n%s", curr, hint));
                    }
                }
                assertEquals(0, actualList.size(), "Your algorithm produces extra " +
                        "values.\nHere's the output stacked side-by-side\n" + sideBySide);
            }
        } else if (OrderedST.class.isAssignableFrom(expectClass)) {
            Iterable<?> expectKeys = ((OrderedST<?,?>) expect).keys();
            OrderedST expectST = (OrderedST) expect;
            OrderedST actualST = (OrderedST) actual;

            assertEquals(expectST.size(), actualST.size(), String.format(
                    "ST sizes do not match\n%s", hint));
            for (Object key : expectKeys) {
                if (!actualST.contains((Comparable) key)) {
                    fail(String.format("ST is missing the key: %s\n%s", key, hint));
                } else {
                    compareValues(expectST.get((Comparable) key), actualST.get((Comparable) key),
                            String.format("Incorrect value for key: %s\n%s", key, hint));
                }
            }
        } else if (BasicST.class.isAssignableFrom(expectClass)) {
            Iterable<?> expectKeys = ((BasicST<?,?>) expect).keys();
            BasicST expectST = (BasicST) expect;
            BasicST actualST = (BasicST) actual;

            assertEquals(expectST.size(), actualST.size(), String.format(
                    "ST sizes do not match\n%s", hint));
            for (Object key : expectKeys) {
                if (!actualST.contains((Comparable) key)) {
                    fail(String.format("ST is missing the key: %s\n%s", key, hint));
                } else {
                    compareValues(expectST.get((Comparable) key), actualST.get((Comparable) key),
                            String.format("Incorrect value for key: %s\n%s", key, hint));
                }
            }
        } else {
//            System.err.println("!expectClass.equals(actualClass) && expectClass.toString().equals(actualClass.toString()):\n"
//                    + !expectClass.equals(actualClass) + " && " + expectClass.toString().equals(actualClass.toString()));
            if (expectClass.toString().contains("RectHV")) {
                RectHV e = (RectHV) expect;
                RectHV a = (RectHV) actual;
                boolean truth = expect.equals(actual);
                System.err.printf("expect.equals(actual) returns %s. Checking their values...\n" +
                        "Here is each as a String:\n" +
                        "  expect: %s\n" +
                        "  actual: %s\n" +
                        "RectHV e = (RectHV) expect\nRectHV a = (RectHV) actual\n" +
                        "e.xMin() == a.xMin() ? %s\n" +
                        "e.yMin() == a.yMin() ? %s\n" +
                        "e.xMax() == a.xMax() ? %s\n" +
                        "e.yMax() == a.yMax() ? %s\n", truth, expect, actual,
                        e.xMin() == a.xMin(), e.yMin() == a.yMin(),
                        e.xMax() == a.xMax(), e.yMax() == a.yMax());
                if (!expect.equals(actual)) {
                    System.err.printf("If not equal, are their .getClass() values equal?\n" +
                                    "%s.equals(%s): %s\n", expectClass, actualClass,
                            expectClass.equals(actualClass));
                }
            }
            assertEquals(expect, actual, hint);
        }
    }

    static void compareFields(Object expect, Object actual, String hint) {
        StringBuilder sb = new StringBuilder("0:Start");
        compareFields(expect, actual, hint, sb, 0);
    }

    // TODO: Clean this up, and/or consolidate it with other methods
    static void compareFields(Object expect, Object actual, String hint, StringBuilder sb, int level) {
        Class<?> expectClass = expect.getClass();
        Class<?> actualClass = actual.getClass();
        System.err.printf("Comparing fields for %s\n", expectClass.getSimpleName());

        for (Field expectField : expectClass.getDeclaredFields()) {
            // Inner classes have a reference to the outer class. Avoid circular reference.
            if (expectField.getName().equals("this$0")) continue;
            System.err.printf("  checking %s\n", expectField.getName());
            try {
                Field actualField = actualClass.getDeclaredField(expectField.getName());
                expectField.setAccessible(true);
                actualField.setAccessible(true);
                Class<?> expectFieldType = expectField.getType();
                Class<?> actualFieldType = actualField.getType();
                Object expectFieldObj = expectField.get(expect);
                Object actualFieldObj = actualField.get(actual);
                String msg2 = sb.length() > 10 ? String.format("Route so far: %s\n", sb.toString()) : "";
                String msg = String.format("\nFailed when comparing the <%s> field of <%s> at " +
                                "level %d (if applicable)\n%s%s\n",
                        expectField.getName(), expectClass.getSimpleName(), level, msg2, hint);
                if (expectFieldObj != null && actualFieldObj != null &&
                        !expectFieldType.equals(actualFieldType) &&
                        expectFieldType.toString().equals(actualFieldType.toString())) {
                    try {
                        expectFieldType.getDeclaredMethod("toString");
                        msg = "\nUsing the toString() method to compare objects." + msg;
                        assertEquals(expectFieldObj.toString(), actualFieldObj.toString(), msg);
                    } catch (NoSuchMethodException e) {
                        // If there's no defined toString method, check each of the object's fields
                        sb.append("\u2193").append(expectField.getName()).append(level+1).append(":");
                        compareFields(expectFieldObj, actualFieldObj, hint, sb, level +1);
                        sb.append("\u2191").append(level).append(":");
                    }
                } else if (expectFieldObj != null && actualFieldObj != null &&
                        Iterable.class.isAssignableFrom(expectFieldType)) {
                    compareIterators(
                            ((Iterable<?>)expectFieldObj).iterator(),
                            ((Iterable<?>)actualFieldObj).iterator(), msg);
                } else {
                    try {
                        expectFieldType.getDeclaredMethod("toString");
                        msg = "\nUsing the toString() method to compare objects." + msg;
                        assertEquals(expectFieldObj.toString(), actualFieldObj.toString(), msg);
                    } catch (NoSuchMethodException e) {
                        assertEquals(expectFieldObj, actualFieldObj, msg);
                    }
                }
            } catch (NoSuchFieldException e) {
                String msg = String.format("\nFailed when comparing the <%s> field of <%s>\n" +
                                "Field missing: %s\n%s\n",
                        expectField.getName(), expectClass.getSimpleName(),
                        expectField.getName(), hint);
                fail(msg);
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                fail("Error accessing field. (TestUtils#compareFields -> IllegalAccessException)");
                e.printStackTrace();
            }
        }
    }



    public static LinkedList<String> iterableToStringList(Iterable<?> it) {
        LinkedList<String> list = new LinkedList<>();
        it.forEach((item) -> list.add(Objects.requireNonNullElse(item, "null").toString()));
        return list;
    }

    private static boolean isCommonType(Class<?> clazz) {
        return clazz == Integer.class || clazz == Float.class || clazz == String.class
            || clazz == Boolean.class || clazz == Short.class || clazz == Double.class
            || clazz == Long.class    || clazz == Byte.class  || clazz == Character.class;
    }

    static void compareStdOut(String expect, String actual, String hint) {
        if (expect.length() < 100) {
            if (expect.length() <= 25) {
                try {
                    double expectD = Double.parseDouble(expect);
                    double actualD = Double.parseDouble(actual);
//                    System.err.println("Attempting to compare strings as doubles");
                    assertEquals(expectD, actualD, 0.005, hint);
                    return;
                } catch (NumberFormatException e) {
                    // do nothing
                }
            }
            assertEquals("\n"+expect+"\n", "\n"+actual+"\n", hint);
        } else {
            LinkedList<String> expectList = string2LineList(expect);
            LinkedList<String> actualList = string2LineList(actual);

            String sideBySide = stackOutputSideBySide(expectList, actualList);
            int i = 0;
            String feedback = "\n%s\nFailed on line %d of output.\n" +
                    "Here's a side-by-side of the output:\n%s\n";
            while (!expectList.isEmpty() && !actualList.isEmpty()) {
                String expectCurrent = "\n"+expectList.removeFirst()+"\n";
                String actualCurrent = "\n"+actualList.removeFirst()+"\n";
                String failMessage = String.format(feedback, hint, i++, sideBySide);
                assertEquals(expectCurrent, actualCurrent, failMessage);
            }
            feedback = "%s\nYour solution produced output of unexpected length\n" +
                    "Review this side-by-side of output and adjust your solution:\n%s";
            assertEquals(expectList.size(), actualList.size(),
                    String.format(feedback, hint, sideBySide));
        }
    }

    static LinkedList<String> string2LineList(String str) {
        LinkedList<String> list = new LinkedList<>();
        BufferedReader reader = new BufferedReader(new StringReader(str));
        reader.lines().forEach(list::add);
        return list;
    }

    public static String stackOutputSideBySide(List<String> expectList, List<String> actualList) {
        String expectHeader = "Expected Output ";
        String actualHeader = "Actual Output ";

        // Find the max width of any line in expectList.
        int maxExpectLine = 0;
        for (String line : expectList) {
            int tabs = (int) line.chars().filter(ch -> ch == '\t').count();
            int length = line.length() + 3*tabs;
            if (length > maxExpectLine) {
                maxExpectLine = length;
            }
        }
        int max = Math.max(maxExpectLine, expectHeader.length());

        // headerPad2 contains the odd space, if present
        String headerPad1 = " ".repeat(                (max - expectHeader.length()) / 2);
        String headerPad2 = " ".repeat((int) Math.ceil((max - expectHeader.length()) / 2.0));
        String headerPad3 = " ".repeat(                (max - actualHeader.length()) / 2);

        StringBuilder sb = new StringBuilder();
        Iterator<String> expectIter = expectList.iterator();
        Iterator<String> actualIter = actualList.iterator();

        // Append the header
        sb.append("\n").append("Line | ")
                .append(headerPad1).append(expectHeader).append(headerPad2).append(" | ")
                .append(headerPad3).append(actualHeader).append("\n");
        int line = 0;
        while (expectIter.hasNext() || actualIter.hasNext()) {
            String expectCurrent = expectIter.hasNext() ? expectIter.next() : "";
            String actualCurrent = actualIter.hasNext() ? actualIter.next() : "";
            int tabs = (int) expectCurrent.chars().filter(ch -> ch == '\t').count();
            String expectPadding = " ".repeat(max - expectCurrent.length() - tabs*3);
            sb.append(String.format("%4d | %s%s | %s\n", line++, expectCurrent, expectPadding,
                    actualCurrent));
        }
        return sb.toString();
    }

    /**
     * Gets the root cause of a thrown exception.
     * Credit to user Legna from StackOverflow.
     *
     * @param ex the exception you want the root cause for
     * @return the root cause of ex
     */
    protected static Throwable getRootCause(Throwable ex) {
        Throwable cause;
        Throwable result = ex;
        while(null != (cause = result.getCause()) && (result != cause) ) {
            result = cause;
        }
        return result;
    }

    private static boolean typesMatch(Class<?> a, Class<?> b) {
        if (!a.isPrimitive() && b == null) return true;
        if (a.isPrimitive() && b == null) return false;
        if (a.equals(b)) return true;
        if (a.equals(primitivesMap.get(b))) return true;
        if (b.equals(primitivesMap.get(a))) return true;
        HashSet<Class<?>> doubleCanTake = new HashSet<>();
        doubleCanTake.add(Integer.class);
        doubleCanTake.add(int.class);
        doubleCanTake.add(Float.class);
        doubleCanTake.add(float.class);
        doubleCanTake.add(Short.class);
        doubleCanTake.add(short.class);
        doubleCanTake.add(Long.class);
        doubleCanTake.add(long.class);
        if (a.equals(Double.class) || a.equals(double.class)) {
            return doubleCanTake.contains(b);
        }
        return false;
    }

    private static boolean typesLooselyMatch(Class<?> a, Class<?> b) {
        if (a.isPrimitive() && b == null) return false;
        return typesMatch(a, b) || a.isAssignableFrom(b) || b.isAssignableFrom(a);
    }

    private static void setUpPrimitivesMap() {
        primitivesMap = new HashMap<Class<?>, Class<?>>();
        primitivesMap.put(Double.class, double.class);
        primitivesMap.put(Integer.class, int.class);
        primitivesMap.put(Float.class, float.class);
        primitivesMap.put(Boolean.class, boolean.class);
        primitivesMap.put(Long.class, long.class);
        primitivesMap.put(Byte.class, byte.class);
        primitivesMap.put(Short.class, short.class);
        primitivesMap.put(Character.class, char.class);
        primitivesMap.put(double.class, Double.class);
        primitivesMap.put(int.class, Integer.class);
        primitivesMap.put(float.class, Float.class);
        primitivesMap.put(boolean.class, Boolean.class);
        primitivesMap.put(long.class, Long.class);
        primitivesMap.put(byte.class, Byte.class);
        primitivesMap.put(short.class, Short.class);
        primitivesMap.put(char.class, Character.class);
    }
}
