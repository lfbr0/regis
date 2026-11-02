package com.github.lfbr0.engine;

import com.github.lfbr0.move.Move;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 11/2/26
 * @project regis
 */

/**
 * Really just a wrapper for a Move and its evaluation
 */
@AllArgsConstructor
@Getter
@ToString
public class EngineMove {
    private final Move move;
    private final double evaluation;

    public Move move() {
        return move;
    }

    public double evaluation() {
        return evaluation;
    }
}
