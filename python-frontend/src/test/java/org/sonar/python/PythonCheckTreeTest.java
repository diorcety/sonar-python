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
package org.sonar.python;

import java.io.File;
import java.util.List;
import org.junit.Test;
import org.sonar.plugins.python.api.PythonCheck;
import org.sonar.plugins.python.api.PythonCheckTree;
import org.sonar.plugins.python.api.PythonVisitorContext;
import org.sonar.plugins.python.api.PythonCheck.PreciseIssue;
import org.sonar.plugins.python.api.tree.FunctionDef;
import org.sonar.plugins.python.api.tree.Name;

import static org.assertj.core.api.Assertions.assertThat;

public class PythonCheckTreeTest {

  private static final File FILE = new File("src/test/resources/file.py");
  public static final String MESSAGE = "message";

  private static List<PreciseIssue> scanFileForIssues(File file, PythonCheck check) {
    PythonVisitorContext context = TestPythonVisitorRunner.createContext(file);
    check.scanFile(context);
    return context.getIssues();
  }

  @Test
  public void test() {
    TestPythonCheck check = new TestPythonCheck (){
      @Override
      public void visitFunctionDef(FunctionDef pyFunctionDefTree) {
        super.visitFunctionDef(pyFunctionDefTree);
        Name name = pyFunctionDefTree.name();
        addIssue(name, name.firstToken().value());
      }
    };

    List<PreciseIssue> issues = scanFileForIssues(FILE, check);

    assertThat(issues).hasSize(2);
    PreciseIssue firstIssue = issues.get(0);

    assertThat(firstIssue.cost()).isNull();
    assertThat(firstIssue.secondaryLocations()).isEmpty();

    IssueLocation primaryLocation = firstIssue.primaryLocation();
    assertThat(primaryLocation.message()).isEqualTo("hello");

    assertThat(primaryLocation.startLine()).isEqualTo(1);
    assertThat(primaryLocation.endLine()).isEqualTo(1);
    assertThat(primaryLocation.startLineOffset()).isEqualTo(4);
    assertThat(primaryLocation.endLineOffset()).isEqualTo(9);
  }

  @Test
  public void test_cost() {
    TestPythonCheck check = new TestPythonCheck (){
      @Override
      public void visitFunctionDef(FunctionDef pyFunctionDefTree) {
        super.visitFunctionDef(pyFunctionDefTree);
        Name name = pyFunctionDefTree.name();
        addIssue(name.firstToken(), MESSAGE).withCost(42);
      }
    };

    List<PreciseIssue> issues = scanFileForIssues(FILE, check);
    PreciseIssue firstIssue = issues.get(0);
    assertThat(firstIssue.cost()).isEqualTo(42);
  }

  private static class TestPythonCheck extends PythonCheckTree {

  }
}
