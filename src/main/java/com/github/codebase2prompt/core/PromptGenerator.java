package com.github.codebase2prompt.core;

import com.github.codebase2prompt.util.PerformanceLogger;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PromptGenerator {
    private final Project project;

    public PromptGenerator(Project project) {
        this.project = project;
    }

    public String generatePrompt(@NotNull List<PsiFile> selectedFiles) {
        if (selectedFiles.isEmpty()) {
            return "";
        }

        StringBuilder prompt = new StringBuilder();
        long t0 = System.currentTimeMillis();
        // 1. 生成文件夹结构
        prompt.append(generateFolderStructure(selectedFiles));
        prompt.append("\n\n");
        PerformanceLogger.logTime("generateFolderStructure", t0);
        
        // 2. 生成文件内容
        t0 = System.currentTimeMillis();
        prompt.append(generateFileContents(selectedFiles));
        PerformanceLogger.logTime("generateFileContents", t0);

        return prompt.toString();
    }

    private String generateFolderStructure(@NotNull List<PsiFile> files) {
        StringBuilder structure = new StringBuilder("<folder-structure>\n");
        String projectPath = project.getBasePath();
        
        // 构建目录树
        TreeNode root = new TreeNode("");
        for (PsiFile file : files) {
            String relativePath = getRelativePath(file.getVirtualFile(), projectPath);
            String[] parts = relativePath.split("/");
            
            TreeNode current = root;
            StringBuilder currentPath = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (currentPath.length() > 0) {
                    currentPath.append("/");
                }
                currentPath.append(parts[i]);
                
                TreeNode child = current.children.get(parts[i]);
                if (child == null) {
                    child = new TreeNode(currentPath.toString());
                    current.children.put(parts[i], child);
                }
                current = child;
            }
        }

        // 生成树形结构字符串
        structure.append(generateTreeString(root, "", "└─ ", "│  ", "├─ "));
        structure.append("</folder-structure>");
        
        return structure.toString();
    }

    private String generateFileContents(@NotNull List<PsiFile> files) {
        StringBuilder contents = new StringBuilder();
        String projectPath = project.getBasePath();
        
        long totalSize = 0;
        final long SIZE_LIMIT = 5 * 1024 * 1024; // 5MB limit
        List<String> processedFiles = new ArrayList<>();
        
        for (PsiFile file : files) {
            String relativePath = getRelativePath(file.getVirtualFile(), projectPath);
            
            try {
                byte[] fileContent = file.getVirtualFile().contentsToByteArray();
                totalSize += fileContent.length;
                
                if (totalSize > SIZE_LIMIT) {
                    // 显示警告对话框
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showWarningDialog(
                            project,
                            String.format("Total content size exceeds 5MB limit.\nProcessed %d of %d files.\nSkipped files starting from: %s",
                                processedFiles.size(),
                                files.size(),
                                relativePath),
                            "Content Size Warning"
                        );
                    });
                    break;
                }
                
                contents.append("<document path=\"").append(relativePath).append("\">\n");
                String content = new String(fileContent, StandardCharsets.UTF_8);
                contents.append(content);
                if (!content.endsWith("\n")) {
                    contents.append("\n");
                }
                contents.append("</document>\n\n");
                
                processedFiles.add(relativePath);
                
            } catch (IOException e) {
                contents.append("<document path=\"").append(relativePath).append("\">\n");
                contents.append("// Error reading file content: ").append(e.getMessage()).append("\n");
                contents.append("</document>\n\n");
            }
        }
        
        return contents.toString();
    }

    private String generateTreeString(TreeNode node, String prefix, String lastPrefix, 
                                    String indent, String middlePrefix) {
        StringBuilder result = new StringBuilder();
        
        List<Map.Entry<String, TreeNode>> sortedChildren = new ArrayList<>(node.children.entrySet());
        sortedChildren.sort(Map.Entry.comparingByKey());

        for (int i = 0; i < sortedChildren.size(); i++) {
            Map.Entry<String, TreeNode> entry = sortedChildren.get(i);
            boolean isLast = (i == sortedChildren.size() - 1);
            
            String nextPrefix = prefix + (isLast ? "   " : indent);
            String childPrefix = isLast ? lastPrefix : middlePrefix;
            
            result.append(prefix)
                  .append(childPrefix)
                  .append(entry.getKey())
                  .append("\n");
            
            result.append(generateTreeString(
                entry.getValue(),
                nextPrefix,
                lastPrefix,
                indent,
                middlePrefix
            ));
        }
        
        return result.toString();
    }

    private String getRelativePath(VirtualFile file, String basePath) {
        String filePath = file.getPath();
        if (basePath != null && filePath.startsWith(basePath)) {
            return filePath.substring(basePath.length() + 1);
        }
        return filePath;
    }

    private static class TreeNode {
        final String path;
        final Map<String, TreeNode> children = new TreeMap<>();

        TreeNode(String path) {
            this.path = path;
        }

        String getName() {
            int lastSlash = path.lastIndexOf('/');
            return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
        }
    }
} 