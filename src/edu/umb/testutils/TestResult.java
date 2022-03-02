package edu.umb.testutils;

public class TestResult implements Comparable<TestResult> {
    private final String className;
    private final String methodName;
    private final String description;
    private final String hint;
    private final int number;
    private final double maxScore;
    private double score;
    private String name;
    private String visibility;

    /* outputSB builds any text to be given to the student. */
    private StringBuilder sb;

    public TestResult(int number, Test test) {
        this.number = number;
        name = test.getName();
        className = test.getClassName();
        methodName = test.getMethodName();
        description = test.getDescription();
        hint = test.getHint();
        maxScore = test.getMaxScore();
        score = maxScore;
        sb = new StringBuilder();
        visibility = "visible";
    }

    void setName(String name)   { this.name = name; }
    void setScore(double score) { this.score = score; }

    double getScore()    { return score; }
    double getMaxScore() { return maxScore; }
    String getName()     { return name; }

    public void insertOutput(int offset, String x)  { sb.insert(offset, x); }
    public void addOutput(String x)                 { sb.append(x); }

    /* Return in JSON format.
     * TODO: Need to escape newlines and possibly other characters. */
    public String toJSON() {
        String output = description + "\n" + sb.toString();
        if (output.contains("illegal character: '\\'")) {
            output = "Stray backslash in your code caused it not to compile.\n"
                    + "Please review your code and upload again\n";
        }
        String noBackslashes = output.replace("'\\'", "'\\\\'");
        String noWindowsNewLines = noBackslashes.replace("\r\n", "\\n");
        String noWeirdNewLines = noWindowsNewLines.replace("\r", "\\n");
        String noLinuxNewLines = noWeirdNewLines.replace("\n", "\\n");
        String noTabs = noLinuxNewLines.replace("\t", "    ");
        String noQuotes = noTabs.replace("\"", "\\\"");

        return "{" + String.join(",", new String[] {
                String.format("\"%s\": \"%s\"", "name", name),
                String.format("\"%s\": \"%s\"", "number", number),
                String.format("\"%s\": %.2f", "score", score),
                String.format("\"%s\": %.2f", "max_score", maxScore),
                String.format("\"%s\": \"%s\"", "visibility", visibility),
                String.format("\"%s\": \"%s\"", "output", noQuotes)
        }) + "}";
    }

    /* For debugging only. */
    public String toString() {
        String str = "name: %s, number: %s, score: %.2f, maxScore: %.2f,\n" +
                "className: %s, methodName: %s\nDescription: %s\nHint: %s\n" +
                "detailed output if any (on next line): \n%s";
        return(String.format(str, name, number, score, maxScore, className, methodName,
                description, hint, sb.toString()));
    }

    public boolean equals(Object that) {
        if (this == that) return true;
        if (that instanceof TestResult) {
            TestResult thatTestResult = (TestResult) that;
            return this.name.equals(thatTestResult.name) && this.number == thatTestResult.number;
        }
        return false;
    }

    @Override
    public int compareTo(TestResult other) {
        return Integer.compare(this.number, other.number);
    }
}

