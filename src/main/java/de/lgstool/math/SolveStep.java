package de.lgstool.math;

/** One recorded elimination step: a human description plus the matrix snapshot afterwards. */
public record SolveStep(String description, FractionMatrix snapshot) {
}
