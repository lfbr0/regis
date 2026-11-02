package com.github.lfbr0.engine;

import com.github.lfbr0.move.Move;
import lombok.Data;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 11/2/26
 * @project regis
 */

/**
 * Really just a wrapper for a Move and its evaluation
 */
public record EngineMove(Move move,
                         double evaluation) {
}
