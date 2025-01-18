package com.github.codebase2prompt.storage;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@State(
    name = "FileSelectionStorage",
    storages = {@Storage("fileSelections.xml")}
)
public class FileSelectionStorage implements PersistentStateComponent<FileSelectionStorage.State> {
    private State myState = new State();
    private final Project project;

    public static class State {
        public List<FileSelection> selections = new ArrayList<>();
    }

    public FileSelectionStorage(Project project) {
        this.project = project;
    }

    public static FileSelectionStorage getInstance(Project project) {
        return project.getService(FileSelectionStorage.class);
    }

    @Override
    public @Nullable State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        myState = state;
    }

    public void saveSelection(String name, String description, List<String> filePaths) {
        FileSelection selection = new FileSelection();
        selection.setId(UUID.randomUUID().toString());
        selection.setName(name);
        selection.setDescription(description);
        selection.setFilePaths(filePaths);
        selection.setCreatedAt(System.currentTimeMillis());
        selection.setUpdatedAt(System.currentTimeMillis());

        myState.selections.add(selection);
    }

    public boolean isNameExists(String name) {
        return myState.selections.stream()
                .anyMatch(s -> s.getName().equalsIgnoreCase(name.trim()));
    }

    public List<FileSelection> getAllSelections() {
        return new ArrayList<>(myState.selections);
    }

    public static class FileSelection {
        private String id;
        private String name;
        private String description;
        private List<String> filePaths;
        private long createdAt;
        private long updatedAt;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public List<String> getFilePaths() { return filePaths; }
        public void setFilePaths(List<String> filePaths) { this.filePaths = filePaths; }
        
        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
        
        public long getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    }
} 