package edu.umb.testutils;

import org.junit.jupiter.api.extension.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class TemplateInvoker implements TestTemplateInvocationContextProvider {
    private final Collection<Test> tests;
    public TemplateInvoker() {
        tests = TestFactory.getTests();
    }

    @Override
    public boolean supportsTestTemplate(ExtensionContext extensionContext) {
        return true;
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
            ExtensionContext extensionContext) {
        return tests.stream().map(this::invocationContext);
    }

    private TestTemplateInvocationContext invocationContext(Test test) {
        return new TestTemplateInvocationContext() {
            @Override
            public String getDisplayName(int invocationIndex) {
                return test.getName() + " - " + test.getMethodName();
            }

            @Override
            public List<Extension> getAdditionalExtensions() {
                return Collections.singletonList(new ParameterResolver() {
                    @Override
                    public boolean supportsParameter(ParameterContext parameterContext,
                                                     ExtensionContext extensionContext) {
                        return parameterContext.getParameter().getType().equals(Test.class);
                    }

                    @Override
                    public Object resolveParameter(ParameterContext parameterContext,
                                                   ExtensionContext extensionContext) {
                        return test;
                    }
                });
            }
        };
    }
}
