/*
 * SonarQube Python Plugin
 * Copyright (C) 2011 SonarSource and Waleri Enns
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.python.checks;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.AstAndTokenVisitor;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import com.sonar.sslr.impl.Parser;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.python.PythonConfiguration;
import org.sonar.python.parser.PythonParser;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;
import org.sonar.squidbridge.checks.SquidCheck;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;

@Rule(
    key = CommentedCodeCheck.CHECK_KEY,
    priority = Priority.MAJOR,
    name = "Sections of code should not be \"commented out\"",
    tags = {Tags.UNUSED, Tags.MISRA}
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("5min")
@ActivatedByDefault
public class CommentedCodeCheck extends SquidCheck<Grammar> implements AstAndTokenVisitor {
  public static final String CHECK_KEY = "S125";
  public static final String MESSAGE = "Remove this commented out code.";
  private static final Parser<Grammar> parser = PythonParser.create(new PythonConfiguration(Charsets.UTF_8));

  @Override
  public void visitToken(Token token) {
    List<List<Trivia>> groupedTrivias = groupTrivias(token);
    for (List<Trivia> triviaGroup : groupedTrivias) {
      checkTriviaGroup(triviaGroup);
    }
  }

  private void checkTriviaGroup(List<Trivia> triviaGroup) {
    String text = getTextForParsing(triviaGroup);
    if (isEmpty(text) || isOneWord(text)) {
      return;
    }
    if (isTextParsedAsCode(text)) {
      getContext().createLineViolation(this, MESSAGE, triviaGroup.get(0).getToken());
    }
  }

  private String getTextForParsing(List<Trivia> triviaGroup) {
    StringBuilder commentTextSB = new StringBuilder();
    for (Trivia trivia : triviaGroup) {
      String value = trivia.getToken().getValue();
      while (value.startsWith("#")) {
        value = value.substring(1);
      }
      if (value.startsWith(" ")) {
        value = value.substring(1);
      }
      if (!strContainsSpecialKeywords(value)) {
        commentTextSB.append(value);
        commentTextSB.append("\n");
      }
    }
    return commentTextSB.toString();
  }

  private boolean isOneWord(String text) {
    return text.matches("\\s*[\\w/\\-]+\\s*#*\n+");
  }

  private boolean isEmpty(String text) {
    return text.matches("\\s*");
  }

  private boolean strContainsSpecialKeywords(String text) {
    ImmutableList<String> list = ImmutableList.of(
        " in ",
        " is ",
        " and ",
        " or ",
        "not "
    );
    for (String keyword : list){
      if (text.contains(keyword)){
        return true;
      }
    }
    return false;
  }

  private boolean isTextParsedAsCode(String text) {
    try {
      parser.parse(text);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private List<List<Trivia>> groupTrivias(Token token) {
    List<List<Trivia>> result = new LinkedList<>();
    List<Trivia> currentGroup = null;
    for (Trivia trivia : token.getTrivia()) {
      currentGroup = handleOneLineComment(result, currentGroup, trivia);
    }
    if (currentGroup != null) {
      result.add(currentGroup);
    }
    return result;
  }

  private List<Trivia> handleOneLineComment(List<List<Trivia>> result, @Nullable List<Trivia> currentGroup, Trivia trivia) {
    List<Trivia> newTriviaGroup = currentGroup;
    if (currentGroup == null) {
      newTriviaGroup = new LinkedList<>();
      newTriviaGroup.add(trivia);
    } else if (currentGroup.get(currentGroup.size() - 1).getToken().getLine() + 1 == trivia.getToken().getLine()) {
      newTriviaGroup.add(trivia);
    } else {
      result.add(currentGroup);
      newTriviaGroup = new LinkedList<>();
      newTriviaGroup.add(trivia);
    }
    return newTriviaGroup;
  }
}
