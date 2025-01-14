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
package org.sonar.python.cfg;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.plugins.python.api.cfg.CfgBlock;
import org.sonar.plugins.python.api.cfg.ControlFlowGraph;
import org.sonar.plugins.python.api.tree.BreakStatement;
import org.sonar.plugins.python.api.tree.ClassDef;
import org.sonar.plugins.python.api.tree.ContinueStatement;
import org.sonar.plugins.python.api.tree.ElseClause;
import org.sonar.plugins.python.api.tree.ExceptClause;
import org.sonar.plugins.python.api.tree.FinallyClause;
import org.sonar.plugins.python.api.tree.ForStatement;
import org.sonar.plugins.python.api.tree.IfStatement;
import org.sonar.plugins.python.api.tree.RaiseStatement;
import org.sonar.plugins.python.api.tree.ReturnStatement;
import org.sonar.plugins.python.api.tree.Statement;
import org.sonar.plugins.python.api.tree.StatementList;
import org.sonar.plugins.python.api.tree.Token;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.tree.TryStatement;
import org.sonar.plugins.python.api.tree.WhileStatement;
import org.sonar.plugins.python.api.tree.WithStatement;
import org.sonar.python.tree.TreeUtils;

public class ControlFlowGraphBuilder {

  private PythonCfgBlock start;
  private final PythonCfgBlock end = new PythonCfgEndBlock();
  private final Set<PythonCfgBlock> blocks = new HashSet<>();
  private final Deque<Loop> loops = new ArrayDeque<>();
  private final Deque<PythonCfgBlock> exceptionTargets = new ArrayDeque<>();
  private final Deque<PythonCfgBlock> exitTargets = new ArrayDeque<>();

  public ControlFlowGraphBuilder(@Nullable StatementList statementList) {
    blocks.add(end);
    exceptionTargets.push(end);
    exitTargets.push(end);
    if (statementList != null) {
      start = build(statementList.statements(), createSimpleBlock(end));
    } else {
      start = end;
    }
    removeEmptyBlocks();
    computePredecessors();
  }

  private void computePredecessors() {
    for (PythonCfgBlock block : blocks) {
      for (CfgBlock successor : block.successors()) {
        ((PythonCfgBlock) successor).addPredecessor(block);
      }
    }
  }

  private void removeEmptyBlocks() {
    Map<PythonCfgBlock, PythonCfgBlock> emptyBlockReplacements = new HashMap<>();
    for (PythonCfgBlock block : blocks) {
      if (block.isEmptyBlock()) {
        PythonCfgBlock firstNonEmptySuccessor = block.firstNonEmptySuccessor();
        emptyBlockReplacements.put(block, firstNonEmptySuccessor);
      }
    }

    blocks.removeAll(emptyBlockReplacements.keySet());

    for (PythonCfgBlock block : blocks) {
      block.replaceSuccessors(emptyBlockReplacements);
    }

    start = emptyBlockReplacements.getOrDefault(start, start);
  }

  public ControlFlowGraph getCfg() {
    return new ControlFlowGraph(Collections.unmodifiableSet(blocks), start, end);
  }

  private PythonCfgSimpleBlock createSimpleBlock(CfgBlock successor) {
    PythonCfgSimpleBlock block = new PythonCfgSimpleBlock(successor);
    blocks.add(block);
    return block;
  }

  private PythonCfgBranchingBlock createBranchingBlock(Tree branchingTree, CfgBlock trueSuccessor, CfgBlock falseSuccessor) {
    PythonCfgBranchingBlock block = new PythonCfgBranchingBlock(branchingTree, trueSuccessor, falseSuccessor);
    blocks.add(block);
    return block;
  }

  private PythonCfgBranchingBlock createBranchingBlock(Tree branchingTree, CfgBlock falseSuccessor) {
    PythonCfgBranchingBlock block = new PythonCfgBranchingBlock(branchingTree, null, falseSuccessor);
    blocks.add(block);
    return block;
  }

  private PythonCfgBlock build(List<Statement> statements, PythonCfgBlock successor) {
    PythonCfgBlock currentBlock = successor;
    for (int i = statements.size() - 1; i >= 0; i--) {
      Statement statement = statements.get(i);
      currentBlock = build(statement, currentBlock);
    }
    return currentBlock;
  }

  private PythonCfgBlock build(Statement statement, PythonCfgBlock currentBlock) {
    switch (statement.getKind()) {
      case WITH_STMT:
        return buildWithStatement((WithStatement) statement, currentBlock);
      case CLASSDEF:
        return build(((ClassDef) statement).body().statements(), currentBlock);
      case RETURN_STMT:
        return buildReturnStatement((ReturnStatement) statement, currentBlock);
      case RAISE_STMT:
        return buildRaiseStatement((RaiseStatement) statement, currentBlock);
      case IF_STMT:
        return buildIfStatement(((IfStatement) statement), currentBlock);
      case WHILE_STMT:
        return buildWhileStatement(((WhileStatement) statement), currentBlock);
      case FOR_STMT:
        return buildForStatement(((ForStatement) statement), currentBlock);
      case CONTINUE_STMT:
        return buildContinueStatement(((ContinueStatement) statement), currentBlock);
      case TRY_STMT:
        return tryStatement(((TryStatement) statement), currentBlock);
      case BREAK_STMT:
        return buildBreakStatement((BreakStatement) statement, currentBlock);
      default:
        currentBlock.addElement(statement);
    }

    return currentBlock;
  }

  private PythonCfgBlock buildWithStatement(WithStatement withStatement, PythonCfgBlock successor) {
    PythonCfgBlock withBodyBlock = build(withStatement.statements().statements(), createSimpleBlock(successor));
    // exceptions may be raised inside with block and be caught by context manager
    // see https://docs.python.org/3/reference/compound_stmts.html#the-with-statement
    return createBranchingBlock(withStatement, withBodyBlock, successor);
  }

  private PythonCfgBlock tryStatement(TryStatement tryStatement, PythonCfgBlock successor) {
    PythonCfgBlock finallyOrAfterTryBlock = successor;
    FinallyClause finallyClause = tryStatement.finallyClause();
    PythonCfgBlock finallyBlock = null;
    if (finallyClause != null) {
      finallyOrAfterTryBlock = build(finallyClause.body().statements(), createBranchingBlock(finallyClause, successor, exitTargets.peek()));
      finallyBlock = finallyOrAfterTryBlock;
      exitTargets.push(finallyBlock);
      loops.push(new Loop(finallyBlock, finallyBlock));
    }
    PythonCfgBlock firstExceptClauseBlock = exceptClauses(tryStatement, finallyOrAfterTryBlock, finallyBlock);
    ElseClause elseClause = tryStatement.elseClause();
    PythonCfgBlock tryBlockSuccessor = finallyOrAfterTryBlock;
    if (elseClause != null) {
      tryBlockSuccessor = build(elseClause.body().statements(), createSimpleBlock(finallyOrAfterTryBlock));
    }
    if (finallyClause != null) {
      exitTargets.pop();
      loops.pop();
    }
    exceptionTargets.push(firstExceptClauseBlock);
    exitTargets.push(firstExceptClauseBlock);
    loops.push(new Loop(firstExceptClauseBlock, firstExceptClauseBlock));
    PythonCfgBlock firstTryBlock = build(tryStatement.body().statements(), createBranchingBlock(tryStatement, tryBlockSuccessor, firstExceptClauseBlock));
    exceptionTargets.pop();
    exitTargets.pop();
    loops.pop();
    return createSimpleBlock(firstTryBlock);
  }

  private PythonCfgBlock exceptClauses(TryStatement tryStatement, PythonCfgBlock finallyOrAfterTryBlock, @Nullable PythonCfgBlock finallyBlock) {
    PythonCfgBlock falseSuccessor = finallyBlock == null ? exceptionTargets.peek() : finallyBlock;
    List<ExceptClause> exceptClauses = tryStatement.exceptClauses();
    for (int i = exceptClauses.size() - 1; i >= 0; i--) {
      ExceptClause exceptClause = exceptClauses.get(i);
      PythonCfgBlock exceptBlock = build(exceptClause.body().statements(), createSimpleBlock(finallyOrAfterTryBlock));
      PythonCfgBlock exceptCondition = createBranchingBlock(exceptClause, exceptBlock, falseSuccessor);
      exceptCondition.addElement(exceptClause);
      falseSuccessor = exceptCondition;
    }
    return falseSuccessor;
  }

  private Loop currentLoop(Tree tree) {
    Loop loop = loops.peek();
    if (loop == null) {
      Token token = tree.firstToken();
      throw new IllegalStateException("Invalid \"" + token.value() + "\" outside loop at line " + token.line());
    }
    return loop;
  }

  private PythonCfgBlock buildBreakStatement(BreakStatement breakStatement, PythonCfgBlock syntacticSuccessor) {
    PythonCfgSimpleBlock block = createSimpleBlock(currentLoop(breakStatement).breakTarget);
    block.setSyntacticSuccessor(syntacticSuccessor);
    block.addElement(breakStatement);
    return block;
  }

  private PythonCfgBlock buildContinueStatement(ContinueStatement continueStatement, PythonCfgBlock syntacticSuccessor) {
    PythonCfgSimpleBlock block = createSimpleBlock(currentLoop(continueStatement).continueTarget);
    block.setSyntacticSuccessor(syntacticSuccessor);
    block.addElement(continueStatement);
    return block;
  }

  private PythonCfgBlock buildLoop(Tree branchingTree, Tree conditionElement, StatementList body, @Nullable ElseClause elseClause, PythonCfgBlock successor) {
    PythonCfgBlock afterLoopBlock = successor;
    if (elseClause != null) {
      afterLoopBlock = build(elseClause.body().statements(), createSimpleBlock(successor));
    }
    PythonCfgBranchingBlock conditionBlock = createBranchingBlock(branchingTree, afterLoopBlock);
    conditionBlock.addElement(conditionElement);
    loops.push(new Loop(successor, conditionBlock));
    PythonCfgBlock loopBodyBlock = build(body.statements(), createSimpleBlock(conditionBlock));
    loops.pop();
    conditionBlock.setTrueSuccessor(loopBodyBlock);
    return createSimpleBlock(conditionBlock);
  }

  private PythonCfgBlock buildForStatement(ForStatement forStatement, PythonCfgBlock successor) {
    PythonCfgBlock beforeForStmt = buildLoop(forStatement, forStatement, forStatement.body(), forStatement.elseClause(), successor);
    forStatement.testExpressions().forEach(beforeForStmt::addElement);
    return beforeForStmt;
  }

  private PythonCfgBlock buildWhileStatement(WhileStatement whileStatement, PythonCfgBlock currentBlock) {
    return buildLoop(whileStatement, whileStatement.condition(), whileStatement.body(), whileStatement.elseClause(), currentBlock);
  }

  /**
   * CFG for if-elif-else statement:
   *
   *                +-----------+
   *       +--------+ before_if +-------+
   *       |        +-----------+       |
   *       |                            |
   * +-----v----+                +------v-----+
   * | if_body  |          +-----+ elif_cond  +-----+
   * +----+-----+          |     +------------+     |
   *      |                |                        |
   *      |          +-----v-----+            +-----v-----+
   *      |          | elif_body |            | else_body |
   *      |          +-----+-----+            +-----+-----+
   *      |                |                        |
   *      |        +-------v-----+                  |
   *      +-------->  after_if   <------------------+
   *               +-------------+
   */
  private PythonCfgBlock buildIfStatement(IfStatement ifStatement, PythonCfgBlock afterBlock) {
    PythonCfgBlock ifBodyBlock = createSimpleBlock(afterBlock);
    ifBodyBlock = build(ifStatement.body().statements(), ifBodyBlock);
    ElseClause elseClause = ifStatement.elseBranch();
    PythonCfgBlock falseSuccessor = afterBlock;
    if (elseClause != null) {
      PythonCfgBlock elseBodyBlock = createSimpleBlock(afterBlock);
      elseBodyBlock = build(elseClause.body().statements(), elseBodyBlock);
      falseSuccessor = elseBodyBlock;
    }
    falseSuccessor = buildElifClauses(afterBlock, falseSuccessor, ifStatement.elifBranches());
    PythonCfgBlock beforeIfBlock = createBranchingBlock(ifStatement, ifBodyBlock, falseSuccessor);
    beforeIfBlock.addElement(ifStatement.condition());
    return beforeIfBlock;
  }

  private PythonCfgBlock buildElifClauses(PythonCfgBlock currentBlock, PythonCfgBlock falseSuccessor, List<IfStatement> elifBranches) {
    for (int i = elifBranches.size() - 1; i >= 0; i--) {
      IfStatement elifStatement = elifBranches.get(i);
      PythonCfgBlock elifBodyBlock = createSimpleBlock(currentBlock);
      elifBodyBlock = build(elifStatement.body().statements(), elifBodyBlock);
      PythonCfgBlock beforeElifBlock = createBranchingBlock(elifStatement, elifBodyBlock, falseSuccessor);
      beforeElifBlock.addElement(elifStatement.condition());
      falseSuccessor = beforeElifBlock;
    }
    return falseSuccessor;
  }

  private PythonCfgBlock buildReturnStatement(ReturnStatement statement, PythonCfgBlock syntacticSuccessor) {
    if (TreeUtils.firstAncestorOfKind(statement, Tree.Kind.FUNCDEF) == null || isStatementAtClassLevel(statement)) {
      throw new IllegalStateException("Invalid return outside of a function");
    }
    PythonCfgSimpleBlock block = createSimpleBlock(exitTargets.peek());
    block.setSyntacticSuccessor(syntacticSuccessor);
    block.addElement(statement);
    return block;
  }

  // assumption: parent of return statement is always a statementList, which, in turn, has always a parent
  private static boolean isStatementAtClassLevel(ReturnStatement statement) {
    return statement.parent().parent().is(Tree.Kind.CLASSDEF);
  }

  private PythonCfgBlock buildRaiseStatement(RaiseStatement statement, PythonCfgBlock syntacticSuccessor) {
    PythonCfgSimpleBlock block = createSimpleBlock(exceptionTargets.peek());
    block.setSyntacticSuccessor(syntacticSuccessor);
    block.addElement(statement);
    return block;
  }

  private static class Loop {

    final PythonCfgBlock breakTarget;
    final PythonCfgBlock continueTarget;

    private Loop(PythonCfgBlock breakTarget, PythonCfgBlock continueTarget) {
      this.breakTarget = breakTarget;
      this.continueTarget = continueTarget;
    }
  }
}
