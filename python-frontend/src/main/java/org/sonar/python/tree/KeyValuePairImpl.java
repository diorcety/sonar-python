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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.KeyValuePair;
import org.sonar.plugins.python.api.tree.Token;
import org.sonar.plugins.python.api.tree.TreeVisitor;
import org.sonar.plugins.python.api.tree.Tree;

public class KeyValuePairImpl extends PyTree implements KeyValuePair {

  private final Token starStarToken;
  private final Expression expression;
  private final Expression key;
  private final Token colon;
  private final Expression value;

  public KeyValuePairImpl(Token starStarToken, Expression expression) {
    this.starStarToken = starStarToken;
    this.expression = expression;
    this.key = null;
    this.colon = null;
    this.value = null;
  }

  public KeyValuePairImpl(Expression key, Token colon, Expression value) {
    this.key = key;
    this.colon = colon;
    this.value = value;
    this.starStarToken = null;
    this.expression = null;
  }

  @CheckForNull
  @Override
  public Expression key() {
    return key;
  }

  @CheckForNull
  @Override
  public Token colon() {
    return colon;
  }

  @CheckForNull
  @Override
  public Expression value() {
    return value;
  }

  @CheckForNull
  @Override
  public Token starStarToken() {
    return starStarToken;
  }

  @CheckForNull
  @Override
  public Expression expression() {
    return expression;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitKeyValuePair(this);
  }

  @Override
  public List<Tree> computeChildren() {
    return Stream.of(starStarToken, expression, key, colon, value).filter(Objects::nonNull).collect(Collectors.toList());
  }

  @Override
  public Kind getKind() {
    return Kind.KEY_VALUE_PAIR;
  }
}
