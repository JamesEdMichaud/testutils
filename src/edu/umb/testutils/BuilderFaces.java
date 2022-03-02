package edu.umb.testutils;

public class BuilderFaces {
    public interface ArgClass {
        /**
         * Sets the name of the class that will be used as an argument.
         * 
         * @param className the name of the class to be used as an argument
         * @return the object to method chain using {@link ArgArgs#withArgs(Object...)}
         */
        ArgArgs ofClass(String className);
    }

    public interface ArgArgs {
        /**
         * Sets the constructor arguments to be used when instantiating the class object.
         * 
         * @param args the arguments to be used
         * @return the {@link ArgObject} containing both the student submission and solution objects
         */
        ArgObject withArgs(Object... args);
    }

    public interface ArgObject {
        /**
         * Returns the solution instance of the class object to be used as an argument.
         *
         * @return the solution class object to be used as an argument
         */
        Object getExpectInstance();

        /**
         * Returns the student submission instance of the class object to be used as an
         * argument.
         *
         * @return the student submission class object to be used as an argument
         */
        Object getActualInstance();

        /**
         * Returns the Class of the solution instance of the object to be used as an argument.
         *
         * @return the solution class of the object to be used as an argument
         */
        Class<?> getExpectClass();

        /**
         * Returns the Class of the student submission instance of the object to be used as an
         * argument. If there was a problem with compilation and the instance is null, this
         * method should return Object.class.
         *
         * @return the student submission class of the object to be used as an argument
         */
        Class<?> getActualClassOrElseObject();

        /**
         * Returns the value of the solution's field by the given field name.
         *
         * @param fieldName the name of the field
         * @return the value of the field by the given fieldName
         */
        Object getExpectFieldValue(String fieldName);

        /**
         * Returns the value of the student submission's field by the given field name.
         *
         * @param fieldName the name of the field
         * @return the value of the field by the given fieldName
         */
        Object getActualFieldValue(String fieldName);

        /**
         * Returns the description generated during the creation of this ArgObject.
         * @return the description generated during the creation of this ArgObject
         */
        String getDescription();

        /**
         * Sets the field by the given name to the given value. The value argument should be
         * constructed as an {@link ArgObject}, as this method should be used for
         * course class types. If the field is of a common data types or primitive, use the
         * {@link ArgObject#setFieldValue(String, Object)} method instead.
         *
         * @param fieldName the name of the field whose value to change
         * @param value the value to assign to the field by the given name
         */
        void setFieldValue(String fieldName, ArgObject value);

        /**
         * Sets the field by the given name to the given value. This method should be used for
         * common data types or primitives. If the field is of a course class type, use the
         * {@link ArgObject#setFieldValue(String, ArgObject)} method instead.
         *
         * @param fieldName the name of the field whose value to change
         * @param value the value to assign to the field by the given name
         */
        void setFieldValue(String fieldName, Object value);

        /**
         * Returns a {@link TestFactory.ArgSetup} object that is used to
         * initialize an argument object. Initialization typically involves calling some method
         * of the class being initialized or setting field values (for Nodes), thereby setting
         * the object up for use.
         *
         * This method call should be followed by a call to
         * {@link TestFactory.ArgSetup#setFieldValue(String, ArgObject)} or
         * {@link TestFactory.ArgSetup#setFieldValue(String, Object)}
         *
         * @return the {@link TestFactory.ArgSetup} object used to initialize the argument object
         * @see TestFactory.ArgSetup#setFieldValue(String, ArgObject)
         * @see TestFactory.ArgSetup#setFieldValue(String, Object)
         */
        TestFactory.ArgSetup initialize();
    }

    public interface PreparedTest {
        /**
         * Sets the arguments to be passed to the constructor of the class under test.
         *
         * @param args the arguments to be used
         * @return the {@link PreparedTest} builder object used to call this method
         */
        PreparedTest withConstructorArgs(Object... args);

        /**
         * Sets the name of the field to be tested.
         *
         * @param fieldName the name of the field to be tested
         * @return the builder object used to call this method
         */
        PreparedTest forField(String fieldName);

        /**
         * Sets the method name that will be called in this test. For corner case tests that
         * target the constructor, the method name may be "init" or "constructor".
         *
         * @param methodName the name of the method to be tested
         * @return the {@link PreparedTest} builder object used to call this method
         */
        PreparedTest forMethod(String methodName);

        /**
         * Sets the arguments to be passed to the method being tested.
         *
         * @param args the arguments to be used
         * @return the {@link PreparedTest} builder object used to call this method
         */
        PreparedTest withMethodArgs(Object... args);

        /**
         * Optionally sets the description to be displayed under this test. A description is
         * displayed regardless of whether the test passes or fails. If a description is not
         * set, an automatically generated description is used. Extra comments can be added
         * using the {@link PreparedTest#addNote(String)} method.
         *
         * @param description the description to be displayed
         * @return the {@link PreparedTest} builder object used to call this method
         */
        PreparedTest withDescription(String description);

        /**
         * Adds an extra message before the test description. This can be used to add an
         * additional note describing the test while still using the automatically generated
         * description.
         *
         * @param note the message to be added to the description
         * @return the {@link PreparedTest} builder object used to call this method
         */
        PreparedTest addNote(String note);

        /**
         * Sets the hint to be displayed to a student upon test failure. This hint is only
         * displayed if a test fails, so it should include advice on common errors related
         * to this test.
         *
         * @param hint the hint to be displayed upon test failure
         * @return the {@link PreparedTest} builder object used to call this method
         */
        PreparedTest withHint(String hint);

        /**
         * Sets the exception type that should be thrown in corner case tests.
         *
         * @param type the type of the exception to be thrown
         * @return an {@link ExceptionMessage} object used to set the expected message
         */
        ExceptionMessage forExceptionType(Class<? extends Throwable> type);

        /**
         * Builds a {@link Test} object using all previously set fields and values. After all
         * factory method chains are closed using this method, the {@link TestFactory#startTest()}
         * method may be called to begin test execution.
         *
         * This method throws an exception if any required fields are missing from the
         * {@link TestBuilder} object used to call this method.
         *
         * @throws UnpreparedTestException if any required values or fields are missing
         */
        void build() throws UnpreparedTestException;
    }

    public interface ExceptionMessage {
        /**
         * Sets the expected message to be included in the thrown exception specified by
         * {@link PreparedTest#forExceptionType(Class)}. If the student's message does not match
         * the given message, the test will fail.
         *
         * @param message the message required in the thrown exception
         * @return the {@link PreparedTest} builder object used in building this test
         */
        PreparedTest withExceptionMessage(String message);
    }

    public interface TwoStepTestStart {
        /**
         * Sets the constructor arguments for the class being tested. This method call
         * should be followed by a call to {@link TwoStepTestStart#initialize()} to
         * properly set up the class object.
         *
         * @param constructorArgs the arguments to be given to the class constructor
         * @return the builder object used for method chaining
         */
        TwoStepTestStart constructorArgs(Object... constructorArgs);

        /**
         * Returns a {@link TestFactory.TestSetup} object that is used to
         * initialize a class object. Initialization typically involves calling some method
         * of the class being tested, thereby setting the class up for testing.
         *
         * This method call should be followed by a call to
         * {@link TestFactory.TestSetup#using(TestFactory.SetupFunction, Object...)
         * using(SetupFunction, Object...)} or
         * {@link TestFactory.TestSetup#callMethod(String) callMethod(String)}
         *
         * @return the {@link TestFactory.TestSetup} object used to initialize the class object
         * @see TestFactory.TestSetup#using(TestFactory.SetupFunction, Object...)
         * @see TestFactory.TestSetup#callMethod(String)
         */
        TestFactory.TestSetup initialize();
    }

    public interface TwoStepTestFinish {
        /**
         * Sets the name of the method being tested.
         *
         * @param methodName the name of the method being tested
         * @return the builder object used to call this method
         */
        TwoStepTestFinish method(String methodName);

        /**
         * Sets the name of the field to be tested.
         * @param fieldName the name of the field to be tested
         * @return the builder object used to call this method
         */
        TwoStepTestFinish field(String fieldName);

        /**
         * Sets the arguments to be passed to the method being tested.
         *
         * @param methodArgs the method arguments
         * @return the builder object used to call this method
         */
        TwoStepTestFinish methodArgs(Object... methodArgs);

        /**
         * Sets the hint to be displayed to a student upon test failure. This hint is only
         * displayed if a test fails, so it should include advice on common errors related
         * to this test.
         *
         * @param hint the hint to be displayed upon test failure
         * @return the builder object used to call this method
         */
        TwoStepTestFinish hint(String hint);

        /**
         * Optionally sets the description to be displayed under this test. A description is
         * displayed regardless of whether the test passes or fails. If a description is not
         * set, an automatically generated description is used. Extra comments can be added
         * using the {@link TwoStepTestFinish#note(String)} method.
         *
         * @param description the description to be displayed
         * @return the {@link TwoStepTestFinish} builder object used to call this method
         */
        TwoStepTestFinish description(String description);

        /**
         * Adds an extra message before the test description. This can be used to add an
         * additional note describing the test while still using the automatically generated
         * description.
         *
         * @param note the message to be added to the description
         * @return the {@link TwoStepTestFinish} builder object used to call this method
         */
        TwoStepTestFinish note(String note);
        /**
         * Builds a {@link Test} object using all previously set fields and values. After all
         * factory method chains are closed using this method, the {@link TestFactory#startTest()}
         * method may be called to begin test execution.
         *
         * This method throws an exception if any required fields are missing from the
         * {@link TwoStepTestFinish} object used to call this method.
         *
         * @throws UnpreparedTestException if any required values or fields are missing
         */
        void build() throws UnpreparedTestException;
    }
}
