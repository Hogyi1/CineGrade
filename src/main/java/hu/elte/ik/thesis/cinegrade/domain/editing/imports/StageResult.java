package hu.elte.ik.thesis.cinegrade.domain.editing.imports;

import java.util.List;

public record StageResult(List<StageEntry> entries, List<Runnable> fxUpdates) {}

