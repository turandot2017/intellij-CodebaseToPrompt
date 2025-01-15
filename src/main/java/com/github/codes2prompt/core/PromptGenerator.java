package com.github.codes2prompt.core;

import com.intellij.openapi.project.Project;
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
        
        // 1. 生成文件夹结构
        prompt.append(generateFolderStructure(selectedFiles));
        prompt.append("\n\n");
        
        // 2. 生成文件内容
        prompt.append(generateFileContents(selectedFiles));

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
        
        for (PsiFile file : files) {
            String relativePath = getRelativePath(file.getVirtualFile(), projectPath);
            contents.append("<document path=\"").append(relativePath).append("\">\n");
            
            try {
                String content = new String(file.getVirtualFile().contentsToByteArray(), StandardCharsets.UTF_8);
                contents.append(content);
                if (!content.endsWith("\n")) {
                    contents.append("\n");
                }
            } catch (IOException e) {
                contents.append("// Error reading file content: ").append(e.getMessage()).append("\n");
            }
            
            contents.append("</document>\n\n");
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