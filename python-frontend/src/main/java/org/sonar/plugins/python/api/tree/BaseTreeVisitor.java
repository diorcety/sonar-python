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
package org.sonar.plugins.python.api.tree;

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.python.tree.DictCompExpressionImpl;

/**
 * Default implementation of {@link TreeVisitor}.
 */
public class BaseTreeVisitor implements TreeVisitor {

  protected void scan(@Nullable Tree tree) {
    if (tree != null) {
      tree.accept(this);
    }
  }

  protected void scan(@Nullable List<? extends Tree> trees) {
    if (trees != null) {
      for (Tree tree : trees) {
        scan(tree);
      }
    }
  }

  @Override
  public void visitFileInput(FileInput fileInput) {
    scan(fileInput.statements());
  }

  @Override
  public void visitStatementList(StatementList statementList) {
    scan(statementList.statements());
  }

  @Override
  public void visitIfStatement(IfStatement pyIfStatementTree) {
    scan(pyIfStatementTree.condition());
    scan(pyIfStatementTree.body());
    scan(pyIfStatementTree.elifBranches());
    scan(pyIfStatementTree.elseBranch());
  }

  @Override
  public void visitElseClause(ElseClause pyElseClauseTree) {
    scan(pyElseClauseTree.body());
  }

  @Override
  public void visitExecStatement(ExecStatement pyExecStatementTree) {
    scan(pyExecStatementTree.expression());
    scan(pyExecStatementTree.globalsExpression());
    scan(pyExecStatementTree.localsExpression());
  }

  @Override
  public void visitAssertStatement(AssertStatement pyAssertStatementTree) {
    scan(pyAssertStatementTree.condition());
    scan(pyAssertStatementTree.message());
  }

  @Override
  public void visitDelStatement(DelStatement pyDelStatementTree) {
    scan(pyDelStatementTree.expressions());
  }

  @Override
  public void visitPassStatement(PassStatement pyPassStatementTree) {
    // nothing to visit for pass statement
  }

  @Override
  public void visitPrintStatement(PrintStatement pyPrintStatementTree) {
    scan(pyPrintStatementTree.expressions());
  }

  @Override
  public void visitReturnStatement(ReturnStatement pyReturnStatementTree) {
    scan(pyReturnStatementTree.expressions());
  }

  @Override
  public void visitYieldStatement(YieldStatement pyYieldStatementTree) {
    scan(pyYieldStatementTree.yieldExpression());
  }

  @Override
  public void visitYieldExpression(YieldExpression pyYieldExpressionTree) {
    scan(pyYieldExpressionTree.expressions());
  }

  @Override
  public void visitRaiseStatement(RaiseStatement pyRaiseStatementTree) {
    scan(pyRaiseStatementTree.expressions());
    scan(pyRaiseStatementTree.fromExpression());
  }

  @Override
  public void visitBreakStatement(BreakStatement pyBreakStatementTree) {
    // nothing to visit for break statement
  }

  @Override
  public void visitContinueStatement(ContinueStatement pyContinueStatementTree) {
    // nothing to visit for continue statement
  }

  @Override
  public void visitFunctionDef(FunctionDef pyFunctionDefTree) {
    scan(pyFunctionDefTree.decorators());
    scan(pyFunctionDefTree.name());
    scan(pyFunctionDefTree.parameters());
    scan(pyFunctionDefTree.returnTypeAnnotation());
    scan(pyFunctionDefTree.body());
  }

  @Override
  public void visitName(Name pyNameTree) {
    // nothing to scan on a name
  }

  @Override
  public void visitClassDef(ClassDef pyClassDefTree) {
    scan(pyClassDefTree.name());
    scan(pyClassDefTree.args());
    scan(pyClassDefTree.body());
  }

  @Override
  public void visitAliasedName(AliasedName aliasedName) {
    scan(aliasedName.dottedName());
    scan(aliasedName.alias());
  }

  @Override
  public void visitDottedName(DottedName dottedName) {
    scan(dottedName.names());
  }

  @Override
  public void visitImportFrom(ImportFrom pyImportFromTree) {
    scan(pyImportFromTree.module());
    scan(pyImportFromTree.importedNames());
  }

  @Override
  public void visitForStatement(ForStatement pyForStatementTree) {
    scan(pyForStatementTree.expressions());
    scan(pyForStatementTree.testExpressions());
    scan(pyForStatementTree.body());
    scan(pyForStatementTree.elseClause());
  }

  @Override
  public void visitImportName(ImportName pyImportNameTree) {
    scan(pyImportNameTree.modules());
  }

  @Override
  public void visitGlobalStatement(GlobalStatement pyGlobalStatementTree) {
    scan(pyGlobalStatementTree.variables());
  }

  @Override
  public void visitNonlocalStatement(NonlocalStatement pyNonlocalStatementTree) {
    scan(pyNonlocalStatementTree.variables());
  }

  @Override
  public void visitWhileStatement(WhileStatement pyWhileStatementTree) {
    scan(pyWhileStatementTree.condition());
    scan(pyWhileStatementTree.body());
    scan(pyWhileStatementTree.elseClause());
  }

  @Override
  public void visitExpressionStatement(ExpressionStatement pyExpressionStatementTree) {
    scan(pyExpressionStatementTree.expressions());
  }

  @Override
  public void visitTryStatement(TryStatement pyTryStatementTree) {
    scan(pyTryStatementTree.body());
    scan(pyTryStatementTree.exceptClauses());
    scan(pyTryStatementTree.finallyClause());
    scan(pyTryStatementTree.elseClause());
  }

  @Override
  public void visitFinallyClause(FinallyClause finallyClause) {
    scan(finallyClause.body());
  }

  @Override
  public void visitExceptClause(ExceptClause exceptClause) {
    scan(exceptClause.exception());
    scan(exceptClause.exceptionInstance());
    scan(exceptClause.body());
  }

  @Override
  public void visitWithStatement(WithStatement pyWithStatementTree) {
    scan(pyWithStatementTree.withItems());
    scan(pyWithStatementTree.statements());
  }

  @Override
  public void visitWithItem(WithItem withItem) {
    scan(withItem.test());
    scan(withItem.expression());
  }

  @Override
  public void visitQualifiedExpression(QualifiedExpression pyQualifiedExpressionTree) {
    scan(pyQualifiedExpressionTree.qualifier());
    scan(pyQualifiedExpressionTree.name());
  }

  @Override
  public void visitCallExpression(CallExpression pyCallExpressionTree) {
    scan(pyCallExpressionTree.callee());
    scan(pyCallExpressionTree.argumentList());
  }

  @Override
  public void visitArgumentList(ArgList argList) {
    scan(argList.arguments());
  }

  @Override
  public void visitArgument(Argument pyArgumentTree) {
    scan(pyArgumentTree.keywordArgument());
    scan(pyArgumentTree.expression());
  }

  @Override
  public void visitAssignmentStatement(AssignmentStatement pyAssignmentStatementTree) {
    scan(pyAssignmentStatementTree.lhsExpressions());
    scan(pyAssignmentStatementTree.assignedValue());
  }

  @Override
  public void visitExpressionList(ExpressionList pyExpressionListTree) {
    scan(pyExpressionListTree.expressions());
  }

  @Override
  public void visitBinaryExpression(BinaryExpression pyBinaryExpressionTree) {
    scan(pyBinaryExpressionTree.leftOperand());
    scan(pyBinaryExpressionTree.rightOperand());
  }

  @Override
  public void visitLambda(LambdaExpression pyLambdaExpressionTree) {
    scan(pyLambdaExpressionTree.parameters());
    scan(pyLambdaExpressionTree.expression());
  }

  @Override
  public void visitParameterList(ParameterList parameterList) {
    scan(parameterList.all());
  }

  @Override
  public void visitTupleParameter(TupleParameter tree) {
    scan(tree.parameters());
  }

  @Override
  public void visitParameter(Parameter tree) {
    scan(tree.name());
    scan(tree.typeAnnotation());
    scan(tree.defaultValue());
  }

  @Override
  public void visitTypeAnnotation(TypeAnnotation tree) {
    scan(tree.expression());
  }

  @Override
  public void visitNumericLiteral(NumericLiteral pyNumericLiteralTree) {
    // noop
  }

  @Override
  public void visitStringLiteral(StringLiteral pyStringLiteralTree) {
    scan(pyStringLiteralTree.stringElements());
  }

  @Override
  public void visitStringElement(StringElement tree) {
    // noop
  }

  @Override
  public void visitListLiteral(ListLiteral pyListLiteralTree) {
    scan(pyListLiteralTree.elements());
  }

  @Override
  public void visitUnaryExpression(UnaryExpression pyUnaryExpressionTree) {
    scan(pyUnaryExpressionTree.expression());
  }

  @Override
  public void visitStarredExpression(StarredExpression pyStarredExpressionTree) {
    scan(pyStarredExpressionTree.expression());
  }

  @Override
  public void visitAwaitExpression(AwaitExpression pyAwaitExpressionTree) {
    scan(pyAwaitExpressionTree.expression());
  }

  @Override
  public void visitSliceExpression(SliceExpression pySliceExpressionTree) {
    scan(pySliceExpressionTree.object());
    scan(pySliceExpressionTree.sliceList());
  }

  @Override
  public void visitSliceList(SliceList sliceList) {
    scan(sliceList.slices());
  }

  @Override
  public void visitSliceItem(SliceItem sliceItem) {
    scan(sliceItem.lowerBound());
    scan(sliceItem.upperBound());
    scan(sliceItem.stride());
  }

  @Override
  public void visitSubscriptionExpression(SubscriptionExpression pySubscriptionExpressionTree) {
    scan(pySubscriptionExpressionTree.object());
    scan(pySubscriptionExpressionTree.subscripts());
  }

  @Override
  public void visitParenthesizedExpression(ParenthesizedExpression pyParenthesizedExpressionTree) {
    scan(pyParenthesizedExpressionTree.expression());
  }

  @Override
  public void visitTuple(Tuple pyTupleTree) {
    scan(pyTupleTree.elements());
  }

  @Override
  public void visitConditionalExpression(ConditionalExpression pyConditionalExpressionTree) {
    scan(pyConditionalExpressionTree.condition());
    scan(pyConditionalExpressionTree.trueExpression());
    scan(pyConditionalExpressionTree.falseExpression());
  }

  @Override
  public void visitPyListOrSetCompExpression(ComprehensionExpression tree) {
    scan(tree.resultExpression());
    scan(tree.comprehensionFor());
  }

  @Override
  public void visitComprehensionFor(ComprehensionFor tree) {
    scan(tree.loopExpression());
    scan(tree.iterable());
    scan(tree.nestedClause());
  }

  @Override
  public void visitComprehensionIf(ComprehensionIf tree) {
    scan(tree.condition());
    scan(tree.nestedClause());
  }

  @Override
  public void visitDictionaryLiteral(DictionaryLiteral pyDictionaryLiteralTree) {
    scan(pyDictionaryLiteralTree.elements());
  }

  @Override
  public void visitSetLiteral(SetLiteral pySetLiteralTree) {
    scan((pySetLiteralTree.elements()));
  }

  @Override
  public void visitKeyValuePair(KeyValuePair keyValuePair) {
    scan(keyValuePair.expression());
    scan(keyValuePair.key());
    scan(keyValuePair.value());
  }

  @Override
  public void visitDictCompExpression(DictCompExpressionImpl tree) {
    scan(tree.keyExpression());
    scan(tree.valueExpression());
    scan(tree.comprehensionFor());
  }

  @Override
  public void visitCompoundAssignment(CompoundAssignmentStatement pyCompoundAssignmentStatementTree) {
    scan(pyCompoundAssignmentStatementTree.lhsExpression());
    scan(pyCompoundAssignmentStatementTree.rhsExpression());
  }

  @Override
  public void visitAnnotatedAssignment(AnnotatedAssignment annotatedAssignment) {
    scan(annotatedAssignment.variable());
    scan(annotatedAssignment.annotation());
    scan(annotatedAssignment.assignedValue());
  }

  @Override
  public void visitNone(NoneExpression pyNoneExpressionTree) {
    // noop
  }

  @Override
  public void visitRepr(ReprExpression pyReprExpressionTree) {
    scan(pyReprExpressionTree.expressionList());
  }

  @Override
  public void visitEllipsis(EllipsisExpression pyEllipsisExpressionTree) {
    // noop
  }

  @Override
  public void visitDecorator(Decorator decorator) {
    scan(decorator.name());
    scan(decorator.arguments());
  }

  public void visitToken(Token token) {
    // noop
  }
}
