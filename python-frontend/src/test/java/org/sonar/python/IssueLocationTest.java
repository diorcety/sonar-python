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

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.impl.Parser;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.sonar.python.api.PythonPunctuator;
import org.sonar.python.api.PythonTokenType;
import org.sonar.python.parser.PythonParser;
import org.sonar.python.tree.TokenImpl;

import static org.assertj.core.api.Assertions.assertThat;

public class IssueLocationTest {

  private static final String MESSAGE = "message";

  private Parser<Grammar> parser = PythonParser.create(new PythonConfiguration(StandardCharsets.UTF_8));

  @Test
  public void file_level() {
    IssueLocation issueLocation = IssueLocation.atFileLevel(MESSAGE);
    assertThat(issueLocation.message()).isEqualTo(MESSAGE);
    assertThat(issueLocation.startLine()).isEqualTo(IssueLocation.UNDEFINED_LINE);
    assertThat(issueLocation.endLine()).isEqualTo(IssueLocation.UNDEFINED_LINE);
    assertThat(issueLocation.startLineOffset()).isEqualTo(IssueLocation.UNDEFINED_OFFSET);
    assertThat(issueLocation.endLineOffset()).isEqualTo(IssueLocation.UNDEFINED_OFFSET);
  }

  @Test
  public void line_level() {
    IssueLocation issueLocation = IssueLocation.atLineLevel(MESSAGE, 42);
    assertThat(issueLocation.message()).isEqualTo(MESSAGE);
    assertThat(issueLocation.startLine()).isEqualTo(42);
    assertThat(issueLocation.endLine()).isEqualTo(42);
    assertThat(issueLocation.startLineOffset()).isEqualTo(IssueLocation.UNDEFINED_OFFSET);
    assertThat(issueLocation.endLineOffset()).isEqualTo(IssueLocation.UNDEFINED_OFFSET);
  }

  @Test
  public void tokens() {
    AstNode root = parser.parse("\n\nfoo(42 + y) + 2");
    AstNode firstNode = root.getFirstDescendant(PythonTokenType.NUMBER);
    AstNode lastNode = root.getFirstDescendant(PythonPunctuator.RPARENTHESIS);
    IssueLocation issueLocation = IssueLocation.preciseLocation(new TokenImpl(firstNode.getToken()), new TokenImpl(lastNode.getToken()), MESSAGE);
    assertThat(issueLocation.message()).isEqualTo(MESSAGE);
    assertThat(issueLocation.startLine()).isEqualTo(3);
    assertThat(issueLocation.endLine()).isEqualTo(3);
    assertThat(issueLocation.startLineOffset()).isEqualTo(4);
    assertThat(issueLocation.endLineOffset()).isEqualTo(11);
  }
}
