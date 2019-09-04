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

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import org.sonar.python.api.tree.PyStringElementTree;
import org.sonar.python.api.tree.PyStringLiteralTree;
import org.sonar.python.api.tree.PyTreeVisitor;
import org.sonar.python.api.tree.Tree;

public class PyStringLiteralTreeImpl extends PyTree implements PyStringLiteralTree {

  private final List<PyStringElementTree> stringElements;

  PyStringLiteralTreeImpl(AstNode node, List<PyStringElementTree> stringElements) {
    super(node);
    this.stringElements = stringElements;
  }

  @Override
  public Kind getKind() {
    return Kind.STRING_LITERAL;
  }

  @Override
  public void accept(PyTreeVisitor visitor) {
    visitor.visitStringLiteral(this);
  }

  @Override
  public List<Tree> children() {
    return Collections.unmodifiableList(stringElements);
  }

  @Override
  public List<PyStringElementTree> stringElements() {
    return stringElements;
  }
}