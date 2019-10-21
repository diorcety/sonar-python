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
package org.sonar.python.tree;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.plugins.python.api.tree.ComprehensionFor;
import org.sonar.plugins.python.api.tree.DictCompExpression;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.Token;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.tree.TreeVisitor;
import org.sonar.python.semantic.Symbol;

public class DictCompExpressionImpl extends PyTree implements DictCompExpression {

  private final Token openingBrace;
  private final Expression keyExpression;
  private final Token colon;
  private final Expression valueExpression;
  private final ComprehensionFor comprehensionFor;
  private final Token closingBrace;
  private Set<Symbol> symbols = new HashSet<>();

  public DictCompExpressionImpl(Token openingBrace, Expression keyExpression, Token colon, Expression valueExpression,
                                ComprehensionFor compFor, Token closingBrace) {
    this.openingBrace = openingBrace;
    this.keyExpression = keyExpression;
    this.colon = colon;
    this.valueExpression = valueExpression;
    this.comprehensionFor = compFor;
    this.closingBrace = closingBrace;
  }

  @Override
  public Expression keyExpression() {
    return keyExpression;
  }

  @Override
  public Token colonToken() {
    return colon;
  }

  @Override
  public Expression valueExpression() {
    return valueExpression;
  }

  @Override
  public ComprehensionFor comprehensionFor() {
    return comprehensionFor;
  }

  @Override
  public Set<Symbol> localVariables() {
    return symbols;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitDictCompExpression(this);
  }

  @Override
  public List<Tree> computeChildren() {
    return Stream.of(openingBrace, keyExpression, colon, valueExpression, comprehensionFor, closingBrace).filter(Objects::nonNull).collect(Collectors.toList());
  }

  @Override
  public Kind getKind() {
    return Kind.DICT_COMPREHENSION;
  }

  public void addLocalVariableSymbol(Symbol symbol) {
    symbols.add(symbol);
  }
}
