package com.github.codebase2prompt.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;

import java.util.*;

@State(
    name = "FileSelectionStorage",
    storages = {@Storage("fileSelections.xml")}
)
public class FileSelectionStorage {
    private List<FileSelection> selections = new ArrayList<>();
    private final Project project;

    public FileSelectionStorage(Project project) {
        this.project = project;
    }

    public static FileSelectionStorage getInstance(Project project) {
        return ServiceManager.getService(project, FileSelectionStorage.class);
    }

    public void saveSelection(String name, String description, List<String> filePaths) {
        FileSelection selection = new FileSelection();
        selection.setId(UUID.randomUUID().toString());
        selection.setName(name);
        selection.setDescription(description);
        selection.setFilePaths(filePaths);
        selection.setCreatedAt(System.currentTimeMillis());
        selection.setUpdatedAt(System.currentTimeMillis());

        selections.add(selection);
    }

    public boolean isNameExists(String name) {
        return selections.stream()
                .anyMatch(s -> s.getName().equalsIgnoreCase(name.trim()));
    }

    public List<FileSelection> getAllSelections() {
        return new ArrayList<>(selections);
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