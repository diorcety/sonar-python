/*
 * SonarQube Python Plugin
 * Copyright (C) 2011-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.python.checks.utils;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.Ordering;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.sonar.python.IssueLocation;
import org.sonar.plugins.python.api.PythonCheck;
import org.sonar.plugins.python.api.PythonSubscriptionCheck;
import org.sonar.plugins.python.api.PythonVisitorContext;
import org.sonar.python.SubscriptionVisitor;
import org.sonar.python.TestPythonVisitorRunner;
import org.sonar.plugins.python.api.tree.Token;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.tree.Trivia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class PythonCheckVerifier extends PythonSubscriptionCheck {

  private List<TestIssue> expectedIssues = new ArrayList<>();

  @Override
  public void scanFile(PythonVisitorContext visitorContext) {
    SubscriptionVisitor.analyze(Collections.singletonList(this), visitorContext);
  }

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.TOKEN, ctx -> visitToken(((Token) ctx.syntaxNode())));
  }

  private static List<PreciseIssue> scanFileForIssues(PythonCheck check, PythonVisitorContext context) {
    check.scanFile(context);
    if (check instanceof PythonSubscriptionCheck) {
      SubscriptionVisitor.analyze(Collections.singletonList((PythonSubscriptionCheck) check), context);
    }
    return context.getIssues();
  }


  public static void verify(String path, PythonCheck check) {
    PythonCheckVerifier verifier = new PythonCheckVerifier();
    File file = new File(path);
    PythonVisitorContext pythonVisitorContext = TestPythonVisitorRunner.scanFile(file, verifier);

    Iterator<PreciseIssue> actualIssues = getActualIssues(check, pythonVisitorContext);
    List<TestIssue> expectedIssues = verifier.expectedIssues;

    for (TestIssue expected : expectedIssues) {
      if (actualIssues.hasNext()) {
        verifyIssue(expected, actualIssues.next());
      } else {
        throw new AssertionError("Missing issue at line " + expected.line());
      }
    }

    if (actualIssues.hasNext()) {
      PreciseIssue issue = actualIssues.next();
      throw new AssertionError("Unexpected issue at line " + line(issue) + ": \"" + issue.primaryLocation().message() + "\"");
    }

  }

  private static int line(PreciseIssue issue) {
    return issue.primaryLocation().startLine();
  }

  private static void verifyIssue(TestIssue expected, PreciseIssue actual) {
    if (line(actual) > expected.line()) {
      fail("Missing issue at line " + expected.line());
    }
    if (line(actual) < expected.line()) {
      fail("Unexpected issue at line " + line(actual) + ": \"" + actual.primaryLocation().message() + "\"");
    }
    if (expected.message() != null) {
      assertThat(actual.primaryLocation().message()).as("Bad message at line " + expected.line()).isEqualTo(expected.message());
    }
    if (expected.effortToFix() != null) {
      assertThat(actual.cost().intValue()).as("Bad effortToFix at line " + expected.line()).isEqualTo(expected.effortToFix());
    }
    if (expected.startColumn() != null) {
      assertThat(actual.primaryLocation().startLineOffset() + 1).as("Bad start column at line " + expected.line()).isEqualTo(expected.startColumn());
    }
    if (expected.endColumn() != null) {
      assertThat(actual.primaryLocation().endLineOffset() + 1).as("Bad end column at line " + expected.line()).isEqualTo(expected.endColumn());
    }
    if (expected.endLine() != null) {
      assertThat(actual.primaryLocation().endLine()).as("Bad end line at line " + expected.line()).isEqualTo(expected.endLine());
    }
    if (expected.secondaryLines() != null) {
      assertThat(secondary(actual)).as("Bad secondary locations at line " + expected.line()).isEqualTo(expected.secondaryLines());
    }
  }

  private static List<Integer> secondary(PreciseIssue issue) {
    List<Integer> result = new ArrayList<>();

    for (IssueLocation issueLocation : issue.secondaryLocations()) {
      result.add(issueLocation.startLine());
    }

    return Ordering.natural().sortedCopy(result);
  }

  private static Iterator<PreciseIssue> getActualIssues(PythonCheck check, PythonVisitorContext pythonVisitorContext) {
    List<PreciseIssue> issues = scanFileForIssues(check, pythonVisitorContext);
    List<PreciseIssue> sortedIssues = Ordering.natural().onResultOf(new IssueToLine()).sortedCopy(issues);
    return sortedIssues.iterator();
  }

  private static class IssueToLine implements Function<PreciseIssue, Integer> {
    @Override
    public Integer apply(PreciseIssue issue) {
      return line(issue);
    }
  }

  private void visitToken(Token token) {
    for (Trivia trivia : token.trivia()) {
      String text = trivia.token().value().substring(1).trim();
      String marker = "Noncompliant";

      if (text.startsWith(marker)) {
        int issueLine = trivia.token().line();
        String paramsAndMessage = text.substring(marker.length()).trim();
        expectedIssues.add(createIssue(issueLine, paramsAndMessage));
      } else if (text.startsWith("^")) {
        addPreciseLocation(trivia);
      }
    }
  }

  private static TestIssue createIssue(int issueLine, String paramsAndMessage) {
    if (paramsAndMessage.startsWith("@")) {
      String[] spaceSplit = paramsAndMessage.split("[\\s\\[{]", 2);
      issueLine += Integer.parseInt(spaceSplit[0].substring(1));
      paramsAndMessage = spaceSplit.length > 1 ? spaceSplit[1] : "";
    }

    TestIssue issue = TestIssue.create(null, issueLine);

    if (paramsAndMessage.startsWith("[[")) {
      int endIndex = paramsAndMessage.indexOf("]]");
      addParams(issue, paramsAndMessage.substring(2, endIndex));
      paramsAndMessage = paramsAndMessage.substring(endIndex + 2).trim();
    }

    if (paramsAndMessage.startsWith("{{")) {
      int endIndex = paramsAndMessage.indexOf("}}");
      String message = paramsAndMessage.substring(2, endIndex);
      issue.message(message);
    }
    return issue;
  }

  private static void addParams(TestIssue issue, String params) {
    for (String param : Splitter.on(';').split(params)) {
      int equalIndex = param.indexOf('=');
      if (equalIndex == -1) {
        throw new IllegalStateException("Invalid param at line 1: " + param);
      }
      String name = param.substring(0, equalIndex);
      String value = param.substring(equalIndex + 1);

      if ("effortToFix".equalsIgnoreCase(name)) {
        issue.effortToFix(Integer.parseInt(value));

      } else if ("sc".equalsIgnoreCase(name)) {
        issue.startColumn(Integer.parseInt(value));

      } else if ("ec".equalsIgnoreCase(name)) {
        issue.endColumn(Integer.parseInt(value));

      } else if ("el".equalsIgnoreCase(name)) {
        issue.endLine(lineValue(issue.line(), value));

      } else if ("secondary".equalsIgnoreCase(name)) {
        addSecondaryLines(issue, value);

      } else {
        throw new IllegalStateException("Invalid param at line 1: " + name);
      }
    }
  }

  private static void addSecondaryLines(TestIssue issue, String value) {
    List<Integer> secondaryLines = new ArrayList<>();
    if (!"".equals(value)) {
      for (String secondary : Splitter.on(',').split(value)) {
        secondaryLines.add(lineValue(issue.line(), secondary));
      }
    }
    issue.secondary(secondaryLines);
  }

  private static int lineValue(int baseLine, String shift) {
    if (shift.startsWith("+")) {
      return baseLine + Integer.parseInt(shift.substring(1));
    }
    if (shift.startsWith("-")) {
      return baseLine - Integer.parseInt(shift.substring(1));
    }
    return Integer.parseInt(shift);
  }

  private void addPreciseLocation(Trivia trivia) {
    Token token = trivia.token();
    int line = token.line();
    String text = token.value();
    if (token.column() > 1) {
      throw new IllegalStateException("Line " + line + ": comments asserting a precise location should start at column 1");
    }
    String missingAssertionMessage = String.format("Invalid test file: a precise location is provided at line %s but no issue is asserted at line %s", line, line - 1);
    if (expectedIssues.isEmpty()) {
      throw new IllegalStateException(missingAssertionMessage);
    }
    TestIssue issue = expectedIssues.get(expectedIssues.size() - 1);
    if (issue.line() != line - 1) {
      throw new IllegalStateException(missingAssertionMessage);
    }
    issue.endLine(issue.line());
    issue.startColumn(text.indexOf('^') + 1);
    issue.endColumn(text.lastIndexOf('^') + 2);
  }
}
