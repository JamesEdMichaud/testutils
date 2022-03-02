package edu.umb.testutils;

/**
 * An UnpreparedTestException is thrown when a user attempts to call factory.preparedTest()
 * or factory.twoStepTestStart() before sufficient static fields have been populated.
 *
 * Minimum required fields: name, className, testType, maxScore. If the user wishes to use
 * an automatically generated test description, that must be specified before calling either
 * method as well.
 *
 * @author James Michaud
 */
class UnpreparedTestException extends RuntimeException {
    UnpreparedTestException(String message) {
        super(message);
    }
}
