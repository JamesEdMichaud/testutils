package edu.umb.testutils;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.LinkedList;

public final class Test {
    private final String name;                    // The display name to be used for this test
    private final String className;               // The name of the class to be tested
    private final Class<?> expectClass;           // The solution's class to be tested
    private final Class<?> actualClass;           // The submission class to be tested
    private final Object[] constructorArgs;       // Arguments for the class's constructor
    private final Object expectClassInstance;     // The solution's instance of the test class
    private final Object actualClassInstance;     // The solution's instance of the test class
    private final String methodName;              // The name of the method to be tested
    private final Object[] methodArgs;            // Arguments for the method to be invoked
    private final String exceptionMessage;        // The message given when an exception is thrown
    private final String hint;                    // The hint to be provided if the test fails
    private final TestType testType;              // The type of test (see Testing enum)
    private final String failedMessage;           // The message upon immediate failure
    private final double maxScore;                // How many points is this test worth?
    private final String fieldName;               // The name of the field being checked
    private final long seed;                      // The seed to use for RNG
    private final boolean seedIsSet;              // Indicates whether the RNG seed has been set
    private final String stdInput;                // File to be used as standard input
    private final Class<? extends Throwable> exceptionType; // The type of exception to be thrown
    private final boolean iterableOrderMatters;

    private String description;                   // The test description to be provided
    private int number;                           // The number of this test

    Test(String name, String className, Class<?> expectClass, Class<?> actualClass,
         Object[] constructorArgs, String fieldName, Object expectClassInstance,
         Object actualClassInstance,
         String methodName, Object[] methodArgs,
         Class<? extends Throwable> exceptionType,
         String exceptionMessage, String description, String hint,
         TestType type, double maxScore, String failedMessage, long seed, boolean seedIsSet,
         String stdInput, boolean iterableOrderMatters) {
        this.name = name;
        this.className = className;
        this.expectClass = expectClass;
        this.actualClass = actualClass;
        this.constructorArgs = constructorArgs;
        this.fieldName = fieldName;
        this.expectClassInstance = expectClassInstance;
        this.actualClassInstance = actualClassInstance;
        this.methodName = methodName;
        this.methodArgs = methodArgs;
        this.exceptionType = exceptionType;
        this.exceptionMessage = exceptionMessage;
        this.description = description;
        this.hint = hint;
        this.testType = type;
        this.maxScore = maxScore;
        this.failedMessage = failedMessage;
        this.seed = seed;
        this.seedIsSet = seedIsSet;
        this.stdInput = stdInput;
        this.iterableOrderMatters = iterableOrderMatters;
    }

    /* ****************************************************************************************** */
    /* Getters and test number setter */
    /* ****************************************************************************************** */

    public void setNumber(int number) { this.number = number; }

    public String   getName()                { return name;                  }
    public String   getClassName()           { return className;             }
    public Class<?> getExpectClass()         { return expectClass;           }
    public Class<?> getActualClass()         { return actualClass;           }
    public Object[] getConstructorArgs()     { return constructorArgs;       }
    public String   getFieldName()           { return fieldName;             }
    public Object   getExpectClassInstance() { return expectClassInstance;   }
    public Object   getActualClassInstance() { return actualClassInstance;   }
    public String   getMethodName()          { return methodName;            }
    public Object[] getMethodArgs()          { return methodArgs;            }
    public String   getExceptionMessage()    { return exceptionMessage;      }
    public String   getDescription()         { return description;           }
    public String   getHint()                { return hint;                  }
    public TestType getTestType()            { return testType;              }
    public double   getMaxScore()            { return maxScore;              }
    public int      getNumber()              { return number;                }
    public String   getFailedMessage()       { return failedMessage;         }
    public long     getSeed()                { return seed;                  }
    public boolean  seedIsSet()              { return seedIsSet;             }
    public String   getStdInput()            { return stdInput;              }
    public boolean  iterableOrderMatters()   { return iterableOrderMatters;  }
    public Class<? extends Throwable> getExceptionType() { return exceptionType; }

    void addResults(Object expect, Object actual) {
        String eString = TestUtils.obj2StringNoAdditions(expect);
        String aString = TestUtils.obj2StringNoAdditions(actual);
        if (eString.length() > 3000) eString = "Omitted due to length";
        if (aString.length() > 3000) aString = "Omitted due to length";
        long newlines = eString.chars().filter(ch -> ch == '\n').count();
        long spaces = eString.chars().filter(ch -> ch == ' ').count();
        long maxLine = Arrays.stream(eString.split("\n")).map(String::length)
                .max(Integer::compareTo).orElse(0);
        if ((newlines > 3 && (eString.length()/newlines < 4 || newlines > spaces/2 || maxLine < 35))
                || (expect != null && Iterable.class.isAssignableFrom(expect.getClass()))) {
            LinkedList<String> expectList = TestUtils.string2LineList(eString);
            LinkedList<String> actualList = TestUtils.string2LineList(aString);
            description = String.format("%s\n%s", description,
                    TestUtils.stackOutputSideBySide(expectList, actualList));
//        } else if (expect != null && Iterable.class.isAssignableFrom(expect.getClass())) {
//            // Do nothing (for now)
        } else {
            description = String.format("%s\nExpected value: %s\n  Actual value: %s\n",
                    description, quoteString(expect, eString), quoteString(actual, aString));
        }
    }

    static String quoteString(Object original, String str) {
        if (original instanceof String) {
            return String.format("\"%s%s\"",
                    str.length() > 90 || str.contains("\n") ? "\n" : "",
                    str.trim());
        } else {
            return str;
        }
    }
}
