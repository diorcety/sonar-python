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
package org.sonar.plugins.python.api.cfg;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class CfgValidator {

  private static final String DEBUG_MESSAGE_TEMPLATE;

  static {
    StringJoiner stringJoiner = new StringJoiner(System.lineSeparator());
    stringJoiner.add("Not expected CFG structure. Hint: %s for '%s'.");
    DEBUG_MESSAGE_TEMPLATE = stringJoiner.toString();
  }

  private final ExpectedCfgStructure expectedCfg;

  CfgValidator(ExpectedCfgStructure expectedCfg) {
    this.expectedCfg = expectedCfg;
  }

  static void assertCfgStructure(ControlFlowGraph actualCfg) {
    new CfgValidator(ExpectedCfgStructure.parse(actualCfg.blocks())).assertCfg(actualCfg);
  }

  void assertCfg(ControlFlowGraph actualCfg) {
    assertThat(actualCfg.end().successors())
      .withFailMessage("END block should not have successors")
      .isEmpty();
    assertThat(actualCfg.end().elements())
      .withFailMessage("END block should not have elements")
      .isEmpty();
    assertThat(actualCfg.blocks())
      .withFailMessage(buildDebugMessage(format("size expected: %d actual: %d", expectedCfg.size(), actualCfg.blocks().size()), "CFG"))
      .hasSize(expectedCfg.size());

    for (CfgBlock actualBlock : actualCfg.blocks()) {
      if (actualBlock.equals(actualCfg.end())) {
        continue;
      }
      if (actualBlock.elements().isEmpty()) {
        assertEmptyBlock(actualBlock);
        continue;
      }

      String blockTestId = expectedCfg.testId(actualBlock);
      assertSuccessors(actualBlock);
      assertPredecessors(actualBlock, blockTestId);
      assertElements(actualBlock, blockTestId);
      assertSyntacticSuccessor(actualBlock);
    }
  }

  private void assertPredecessors(CfgBlock actualBlock, String blockTestId) {
    if (expectedCfg.hasNonEmptyPredecessors()) {
      List<String> expectedPred = expectedCfg.expectedPred(actualBlock);
      assertThat(expectedCfg.blockIds(actualBlock.predecessors()))
        .withFailMessage(buildDebugMessage("predecessors", blockTestId))
        .containsOnlyElementsOf(expectedPred);
    }
  }

  private void assertElements(CfgBlock actualBlock, String blockTestId) {
    if (expectedCfg.hasNonEmptyElementNumbers()) {
      int actualElementNumber = actualBlock.elements().size();
      int expectedElementNumber = expectedCfg.expectedNumberOfElements(actualBlock);
      String message = format("Expecting %d elements instead of %d for '%s'",
        expectedElementNumber, actualElementNumber, blockTestId);
      assertThat(actualBlock.elements().size())
        .withFailMessage(message)
        .isEqualTo(expectedCfg.expectedNumberOfElements(actualBlock));
    }
  }

  private void assertEmptyBlock(CfgBlock emptyBlock) {
    List<ExpectedCfgStructure.BlockExpectation> matchedExpectations = expectedCfg.emptyBlockExpectations.stream()
      .filter(blockExpectation -> blockExpectation.matchesBlock(emptyBlock))
      .collect(Collectors.toList());
    assertThat(matchedExpectations)
      .withFailMessage(format("Failed to assert empty block succ=%s, pred=%s. Matched expectations: %s",
        expectedCfg.blockIds(emptyBlock.successors()),
        expectedCfg.blockIds(emptyBlock.predecessors()),
        matchedExpectations))
      .hasSize(1);
    // remove the expectation we've just asserted so it is not used for another empty block
    expectedCfg.emptyBlockExpectations.removeAll(matchedExpectations);
  }

  private void assertSuccessors(CfgBlock actualBlock) {
    String blockTestId = expectedCfg.testId(actualBlock);

    List<String> expectedSucc = expectedCfg.expectedSucc(actualBlock);
    List<String> actual = expectedCfg.blockIds(actualBlock.successors());
    if (actualBlock instanceof CfgBranchingBlock) {
      CfgBranchingBlock branchingBlock = (CfgBranchingBlock) actualBlock;
      actual = expectedCfg.blockIds(Arrays.asList(branchingBlock.trueSuccessor(), branchingBlock.falseSuccessor()));
    }

    assertThat(actual)
      .withFailMessage(buildDebugMessage(format("successors actual: %s expected %s", actual, expectedSucc), blockTestId))
      .isEqualTo(expectedSucc);
  }

  private void assertSyntacticSuccessor(CfgBlock actualBlock) {
    String blockTestId = expectedCfg.testId(actualBlock);
    String expectedSyntSucc = expectedCfg.expectedSyntSucc(actualBlock);

    if (expectedSyntSucc != null) {
      if (expectedSyntSucc.equals(ExpectedCfgStructure.EMPTY)) {
        assertThat(actualBlock.syntacticSuccessor().elements())
          .withFailMessage("syntactic successor should be _empty", blockTestId)
          .isEmpty();
      } else {
        assertThat(actualBlock.syntacticSuccessor())
          .withFailMessage(buildDebugMessage("syntactic successor", blockTestId))
          .isEqualTo(expectedCfg.cfgBlock(expectedSyntSucc));
      }
    } else {
      assertThat(actualBlock.syntacticSuccessor()).withFailMessage(buildDebugMessage("syntactic successor", blockTestId)).isNull();
    }
  }


  private String buildDebugMessage(String hint, String blockId) {
    return format(DEBUG_MESSAGE_TEMPLATE, hint, blockId, null);
  }

}
