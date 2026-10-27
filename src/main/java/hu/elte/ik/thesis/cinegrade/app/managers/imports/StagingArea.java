package hu.elte.ik.thesis.cinegrade.app.managers.imports;

import hu.elte.ik.thesis.cinegrade.domain.editing.imports.StageEntry;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * View model of the import staging area.
 * Holds the staged entries, the filter/sort pipeline over them (name search, companion
 * and duplicate hiding), the derived counters shown by the UI, and the selection and
 * removal operations. It has no knowledge of background tasks or the database.
 */
@Getter
public class StagingArea {

    private static final Logger logger = LogManager.getLogger(StagingArea.class);

    // Look at those properties and fire a change when these change inside
    private final ObservableList<StageEntry> masterEntries = FXCollections.observableArrayList(e -> new Observable[]{e.getIsSelectedProperty(), e.getIsMaybeDuplicateProperty(), e.getIsCompanionProperty()});
    private final ObservableList<StageEntry> readOnlyEntries = FXCollections.unmodifiableObservableList(masterEntries);

    // Filter on different predicates
    private final FilteredList<StageEntry> filteredEntries = new FilteredList<>(masterEntries);
    // Sort on filenames
    private final SortedList<StageEntry> sortedEntries = new SortedList<>(filteredEntries);

    private final LongProperty stageSizeInBytes = new SimpleLongProperty(0);
    private final IntegerProperty selectedFilesProperty = new SimpleIntegerProperty(0);
    private final IntegerProperty stagedFilesProperty = new SimpleIntegerProperty(0);

    private final BooleanProperty autoPairProperty = new SimpleBooleanProperty(true);
    private final BooleanProperty hideDuplicatesProperty = new SimpleBooleanProperty(true);
    private final BooleanProperty stagingProperty = new SimpleBooleanProperty(false);
    private final StringProperty nameProperty = new SimpleStringProperty("");

    /**
     * Creates an empty staging area and wires the filter, sort and counter bindings.
     */
    public StagingArea() {
        filteredEntries.addListener((ListChangeListener<StageEntry>) change -> {
            logger.debug("FilteredEntries changed: count = {}, masterEntries count = {}", filteredEntries.size(), masterEntries.size());
            calculateStageSize();
            updateSelectedCount();
        });

        stagedFilesProperty.bind(Bindings.size(filteredEntries));
        sortedEntries.setComparator(Comparator.comparing(StageEntry::getFileNameWithoutExtension)
                .thenComparing((StageEntry e) -> e.isRaw ? 0 : 1));

        filteredEntries.predicateProperty().bind(Bindings.createObjectBinding(() -> {
            boolean hideCompanions = autoPairProperty.get();
            boolean hideDuplicates = hideDuplicatesProperty.get();
            String nameFilter = nameProperty.get();

            return (Predicate<StageEntry>) entry -> {
                boolean matchesName = nameFilter.isEmpty() || entry.getFile().getName().toLowerCase().contains(nameFilter.toLowerCase());
                boolean isCompanion = !hideCompanions || !entry.getIsCompanionProperty().get();
                boolean isDuplicate = !hideDuplicates || !entry.getIsMaybeDuplicateProperty().get();

                return matchesName && isCompanion && isDuplicate;
            };
        }, autoPairProperty, hideDuplicatesProperty, nameProperty));
    }

    /**
     * Adds newly staged entries to the area. Must be called on the FX thread.
     *
     * @param entries the entries to add; null or empty is ignored
     */
    public void addAll(List<StageEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        masterEntries.addAll(entries);
    }

    /**
     * Removes every staged entry.
     */
    public void clear() {
        logger.info("Clearing {} staged entries", masterEntries.size());
        masterEntries.clear();
    }

    /**
     * Returns the entries that are both visible under the current filter and selected.
     *
     * @return snapshot of the selected visible entries
     */
    public List<StageEntry> getSelectedEntries() {
        return filteredEntries.stream()
                .filter(entry -> entry.getIsSelectedProperty().get())
                .toList();
    }

    /**
     * Updates the selection state for every entry currently visible in the filtered list.
     *
     * @param isSelected true to select all visible staged entries, false to clear their selection
     */
    public void selectAll(boolean isSelected) {
        logger.info("Setting selection state for all {} staged entries to: {}", filteredEntries.size(), isSelected);
        filteredEntries.forEach(entry -> entry.getIsSelectedProperty().set(isSelected));
    }

    /**
     * Removes the specified staged entry, and its companion when auto pairing is on.
     *
     * @param stageEntry the staged entry to remove
     */
    public void removeStageEntry(StageEntry stageEntry) {
        stageEntry.getIsSelectedProperty().set(false);

        if (stageEntry.companion != null && autoPairProperty.get()) {
            stageEntry.companion.getIsSelectedProperty().set(false);
            masterEntries.remove(stageEntry.companion);
        }

        masterEntries.remove(stageEntry);
    }

    /**
     * Removes all selected stage entries from the import list.
     */
    public void removeSelection() {
        getSelectedEntries().forEach(this::removeStageEntry);
    }

    /**
     * Updates the count of selected files.
     */
    private void updateSelectedCount() {
        Platform.runLater(() -> selectedFilesProperty.set((int) filteredEntries.stream().filter(entry -> entry.getIsSelectedProperty().get()).count()));
    }

    /**
     * Recalculates the total size of all currently selected staged files in bytes.
     */
    private void calculateStageSize() {
        long byteSize = filteredEntries.stream().filter(entry -> entry.getIsSelectedProperty().get()).mapToLong(entry -> entry.size).sum();
        Platform.runLater(() -> stageSizeInBytes.set(byteSize));
        logger.debug("Stage size updated: {} bytes across {} staged entries", byteSize, masterEntries.size());
    }
}
